/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.ai.evaluation.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import org.apache.skywalking.oap.server.ai.evaluation.context.AIEvaluationContext;
import org.apache.skywalking.oap.server.ai.evaluation.AIEvaluationModule;
import org.apache.skywalking.oap.server.ai.evaluation.context.GenAIContextResolver;
import org.apache.skywalking.oap.server.ai.evaluation.context.GenAISemanticAttributes;
import org.apache.skywalking.oap.server.ai.evaluation.service.IAIEvaluationService;
import org.apache.skywalking.oap.server.core.trace.OTLPSpanReader;
import org.apache.skywalking.oap.server.core.trace.SpanListener;
import org.apache.skywalking.oap.server.core.trace.SpanListenerResult;
import org.apache.skywalking.oap.server.core.zipkin.source.ZipkinSpan;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.util.StringUtil;

public class AIEvaluationSpanListener implements SpanListener {
    private static final String SERVICE_NAME = "service.name";
    private static final String ERROR_TAG = "error";
    private static final long MILLIS_PER_NANO = 1_000_000L;
    private static final long MILLIS_PER_MICRO = 1_000L;

    private IAIEvaluationService evaluationService;

    @Override
    public String[] requiredModules() {
        return new String[]{
                AIEvaluationModule.NAME
        };
    }

    @Override
    public void init(final ModuleManager moduleManager) {
        evaluationService = moduleManager.find(AIEvaluationModule.NAME)
                .provider()
                .getService(IAIEvaluationService.class);
    }

    @Override
    public SpanListenerResult onOTLPSpan(final OTLPSpanReader span,
                                         final Map<String, String> resourceAttributes,
                                         final String scopeName,
                                         final String scopeVersion) {
        final Map<String, String> tags = new HashMap<>(resourceAttributes);
        putIfNotEmpty(tags, GenAISemanticAttributes.RESPONSE_MODEL,
                span.getAttribute(GenAISemanticAttributes.RESPONSE_MODEL));
        if (!isGenAISpan(tags)) {
            return SpanListenerResult.CONTINUE;
        }
        if (!shouldSample(span.traceId())) {
            return SpanListenerResult.CONTINUE;
        }
        putIfNotEmpty(tags, GenAISemanticAttributes.OPERATION_NAME,
                span.getAttribute(GenAISemanticAttributes.OPERATION_NAME));
        putIfNotEmpty(tags, GenAISemanticAttributes.PROVIDER_NAME,
                span.getAttribute(GenAISemanticAttributes.PROVIDER_NAME));
        putIfNotEmpty(tags, GenAISemanticAttributes.SYSTEM,
                span.getAttribute(GenAISemanticAttributes.SYSTEM));
        putIfNotEmpty(tags, GenAISemanticAttributes.USAGE_INPUT_TOKENS,
                span.getAttribute(GenAISemanticAttributes.USAGE_INPUT_TOKENS));
        putIfNotEmpty(tags, GenAISemanticAttributes.USAGE_OUTPUT_TOKENS,
                span.getAttribute(GenAISemanticAttributes.USAGE_OUTPUT_TOKENS));
        putIfNotEmpty(tags, GenAISemanticAttributes.SERVER_TIME_TO_FIRST_TOKEN,
                span.getAttribute(GenAISemanticAttributes.SERVER_TIME_TO_FIRST_TOKEN));
        sample(AIEvaluationContext.SpanSource.OTLP,
                span.traceId(),
                span.spanId(),
                resourceAttributes.get(SERVICE_NAME),
                span.spanName(),
                span.startTimeNanos() / MILLIS_PER_NANO,
                span.endTimeNanos() / MILLIS_PER_NANO,
                tags,
                false);
        return SpanListenerResult.CONTINUE;
    }

    @Override
    public SpanListenerResult onZipkinSpan(final ZipkinSpan span) {
        final Map<String, String> tags = toMap(span.getTags());
        if (!isGenAISpan(tags)) {
            return SpanListenerResult.CONTINUE;
        }
        if (!shouldSample(span.getTraceId())) {
            return SpanListenerResult.CONTINUE;
        }
        sample(AIEvaluationContext.SpanSource.ZIPKIN,
                span.getTraceId(),
                span.getSpanId(),
                span.getLocalEndpointServiceName(),
                span.getName(),
                span.getTimestampMillis(),
                span.getTimestampMillis() + span.getDuration() / MILLIS_PER_MICRO,
                tags,
                "true".equalsIgnoreCase(tags.get(ERROR_TAG)));
        return SpanListenerResult.CONTINUE;
    }

    private void sample(final AIEvaluationContext.SpanSource source,
                        final String traceId,
                        final String spanId,
                        final String serviceName,
                        final String operationName,
                        final long startTimeMillis,
                        final long endTimeMillis,
                        final Map<String, String> tags,
                        final boolean error) {
        final GenAIContextResolver.Result genAIContext = GenAIContextResolver.resolve(tags);
        evaluationService.sample(AIEvaluationContext.builder()
                .source(source)
                .traceId(traceId)
                .spanId(spanId)
                .serviceName(serviceName)
                .operationName(operationName)
                .providerName(genAIContext.getProviderName())
                .modelName(genAIContext.getModelName())
                .startTimeMillis(startTimeMillis)
                .endTimeMillis(endTimeMillis)
                .error(error)
                .tags(tags)
                .build());
    }

    private boolean shouldSample(final String traceId) {
        return evaluationService.shouldSample(traceId);
    }

    private static boolean isGenAISpan(final Map<String, String> tags) {
        return StringUtil.isNotBlank(tags.get(GenAISemanticAttributes.RESPONSE_MODEL));
    }

    private static void putIfNotEmpty(final Map<String, String> tags, final String key, final String value) {
        if (value != null && !value.isEmpty()) {
            tags.put(key, value);
        }
    }

    private static Map<String, String> toMap(final JsonObject tags) {
        final Map<String, String> result = new HashMap<>();
        if (tags == null) {
            return result;
        }
        for (Map.Entry<String, JsonElement> entry : tags.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }
}
