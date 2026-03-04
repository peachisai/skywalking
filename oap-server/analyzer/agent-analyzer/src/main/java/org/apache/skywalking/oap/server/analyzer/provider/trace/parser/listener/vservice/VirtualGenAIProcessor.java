/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.oap.server.analyzer.provider.trace.parser.listener.vservice;

import lombok.RequiredArgsConstructor;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanLayer;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.oap.meter.analyzer.service.IGenAIMeterAnalyzerService;
import org.apache.skywalking.oap.server.core.source.Source;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class VirtualGenAIProcessor implements VirtualServiceProcessor {

    private final IGenAIMeterAnalyzerService meterAnalyzerService;

    @Override
    public void prepareVSIfNecessary(SpanObject span, SegmentObject segmentObject) {
        // Only process spans related to Generative AI
        if (span.getSpanLayer() != SpanLayer.GenAI) {
            return;
        }

        meterAnalyzerService.doTraceAnalysis(span,segmentObject);
    }

    @Override
    public void emitTo(Consumer<Source> consumer) {

    }
}
