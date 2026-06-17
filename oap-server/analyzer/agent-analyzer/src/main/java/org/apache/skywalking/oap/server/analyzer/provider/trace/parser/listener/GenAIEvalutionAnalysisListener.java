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

package org.apache.skywalking.oap.server.analyzer.provider.trace.parser.listener;

import lombok.RequiredArgsConstructor;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanLayer;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.oap.server.ai.evalution.AIEvalutionContext;
import org.apache.skywalking.oap.server.ai.evalution.AIEvalutionModule;
import org.apache.skywalking.oap.server.ai.evalution.service.IAIEvalutionService;
import org.apache.skywalking.oap.server.analyzer.provider.AnalyzerModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GenAIEvalutionAnalysisListener implements EntryAnalysisListener, ExitAnalysisListener, LocalAnalysisListener {
    private final IAIEvalutionService evalutionService;

    @Override
    public void parseEntry(final SpanObject span, final SegmentObject segmentObject) {
        sample(span, segmentObject);
    }

    @Override
    public void parseExit(final SpanObject span, final SegmentObject segmentObject) {
        sample(span, segmentObject);
    }

    @Override
    public void parseLocal(final SpanObject span, final SegmentObject segmentObject) {
        sample(span, segmentObject);
    }

    @Override
    public void build() {
    }

    @Override
    public boolean containsPoint(final Point point) {
        return point == Point.Entry || point == Point.Exit || point == Point.Local;
    }

    private void sample(final SpanObject span, final SegmentObject segmentObject) {
        if (span.getSkipAnalysis() || span.getSpanLayer() != SpanLayer.GenAI) {
            return;
        }

        final Map<String, String> tags = span.getTagsList().stream()
                                             .collect(Collectors.toMap(
                                                 KeyStringValuePair::getKey,
                                                 KeyStringValuePair::getValue,
                                                 (left, right) -> left
                                             ));

        evalutionService.sample(AIEvalutionContext.builder()
                                                     .source(AIEvalutionContext.SpanSource.SW)
                                                     .traceId(segmentObject.getTraceId())
                                                     .spanId(String.valueOf(span.getSpanId()))
                                                     .serviceName(segmentObject.getService())
                                                     .serviceInstanceName(segmentObject.getServiceInstance())
                                                     .operationName(span.getOperationName())
                                                     .startTimeMillis(span.getStartTime())
                                                     .endTimeMillis(span.getEndTime())
                                                     .error(span.getIsError())
                                                     .tags(tags)
                                                     .build());
    }

    public static class Factory implements AnalysisListenerFactory {
        private final IAIEvalutionService evalutionService;

        public Factory(final ModuleManager moduleManager) {
            this.evalutionService = moduleManager.find(AIEvalutionModule.NAME)
                                                .provider()
                                                .getService(IAIEvalutionService.class);
        }

        @Override
        public AnalysisListener create(final ModuleManager moduleManager, final AnalyzerModuleConfig config) {
            return new GenAIEvalutionAnalysisListener(evalutionService);
        }
    }
}
