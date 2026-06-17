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

package org.apache.skywalking.oap.server.ai.evalution.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.apache.skywalking.oap.server.ai.evalution.AIEvalutionContext;
import org.apache.skywalking.oap.server.ai.evalution.AIEvalutionModule;
import org.apache.skywalking.oap.server.ai.evalution.GenAISemanticAttributes;
import org.apache.skywalking.oap.server.ai.evalution.service.IAIEvalutionService;
import org.apache.skywalking.oap.server.core.trace.OTLPSpanReader;
import org.apache.skywalking.oap.server.core.trace.SpanListener;
import org.apache.skywalking.oap.server.core.trace.SpanListenerResult;
import org.apache.skywalking.oap.server.core.zipkin.source.ZipkinSpan;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

public class AIEvalutionSpanListener implements SpanListener {
    private IAIEvalutionService evalutionService;

    @Override
    public String[] requiredModules() {
        return new String[] {
            AIEvalutionModule.NAME
        };
    }

    @Override
    public void init(final ModuleManager moduleManager) {
        evalutionService = moduleManager.find(AIEvalutionModule.NAME)
                                        .provider()
                                        .getService(IAIEvalutionService.class);
    }

    @Override
    public SpanListenerResult onOTLPSpan(final OTLPSpanReader span,
                                         final Map<String, String> resourceAttributes,
                                         final String scopeName,
                                         final String scopeVersion) {
        final Map<String, String> tags = new HashMap<>(resourceAttributes);
        putIfNotEmpty(tags, GenAISemanticAttributes.OPERATION_NAME,
                      span.getAttribute(GenAISemanticAttributes.OPERATION_NAME));
        putIfNotEmpty(tags, GenAISemanticAttributes.RESPONSE_MODEL,
                      span.getAttribute(GenAISemanticAttributes.RESPONSE_MODEL));
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

        if (!hasGenAITag(tags)) {
            return SpanListenerResult.CONTINUE;
        }

        evalutionService.sample(AIEvalutionContext.builder()
                                                     .source(AIEvalutionContext.SpanSource.OTLP)
                                                     .traceId(span.traceId())
                                                     .spanId(span.spanId())
                                                     .serviceName(resourceAttributes.get("service.name"))
                                                     .operationName(span.spanName())
                                                     .startTimeMillis(span.startTimeNanos() / 1_000_000L)
                                                     .endTimeMillis(span.endTimeNanos() / 1_000_000L)
                                                     .tags(tags)
                                                     .build());
        return SpanListenerResult.CONTINUE;
    }

    @Override
    public SpanListenerResult onZipkinSpan(final ZipkinSpan span) {
        final Map<String, String> tags = toMap(span.getTags());
        if (!hasGenAITag(tags)) {
            return SpanListenerResult.CONTINUE;
        }

        evalutionService.sample(AIEvalutionContext.builder()
                                                     .source(AIEvalutionContext.SpanSource.ZIPKIN)
                                                     .traceId(span.getTraceId())
                                                     .spanId(span.getSpanId())
                                                     .serviceName(span.getLocalEndpointServiceName())
                                                     .operationName(span.getName())
                                                     .startTimeMillis(span.getTimestampMillis())
                                                     .endTimeMillis(
                                                         span.getTimestampMillis() + span.getDuration() / 1000
                                                     )
                                                     .error("true".equalsIgnoreCase(tags.get("error")))
                                                     .tags(tags)
                                                     .build());
        return SpanListenerResult.CONTINUE;
    }

    private static void putIfNotEmpty(final Map<String, String> tags, final String key, final String value) {
        if (value != null && !value.isEmpty()) {
            tags.put(key, value);
        }
    }

    private static boolean hasGenAITag(final Map<String, String> tags) {
        return tags.keySet().stream().anyMatch(key -> key.startsWith(GenAISemanticAttributes.PREFIX));
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
