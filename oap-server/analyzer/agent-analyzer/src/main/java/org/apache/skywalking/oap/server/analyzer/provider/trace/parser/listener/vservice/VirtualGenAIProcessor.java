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

import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanLayer;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanType;
import org.apache.skywalking.oap.server.core.source.Source;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VirtualGenAIProcessor implements VirtualServiceProcessor {

    @Override
    public void prepareVSIfNecessary(SpanObject span, SegmentObject segmentObject) {
        if (span.getSpanLayer() != SpanLayer.GenAI) {
            return;
        }

        // 处理tool
        int componentId = span.getComponentId();

        Map<String, String> tags = span.getTagsList().stream()
                .collect(
                        Collectors.toMap(KeyStringValuePair::getKey, KeyStringValuePair::getValue));
        tags.put("",);

        // 获取provider

        // 获取model

        // 生成tool 次数指标

        // 生成调用LLM latency指标

        // 生成input token, output token, total token 指标




        tags.get("provider");


        // 处理token

        //

    }

    @Override
    public void emitTo(Consumer<Source> consumer) {

    }
}
