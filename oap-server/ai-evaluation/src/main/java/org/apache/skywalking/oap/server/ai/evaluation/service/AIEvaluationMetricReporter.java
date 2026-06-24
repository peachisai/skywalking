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

package org.apache.skywalking.oap.server.ai.evaluation.service;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.meter.analyzer.v2.MetricConvert;
import org.apache.skywalking.oap.meter.analyzer.v2.dsl.Sample;
import org.apache.skywalking.oap.meter.analyzer.v2.dsl.SampleFamily;
import org.apache.skywalking.oap.meter.analyzer.v2.dsl.SampleFamilyBuilder;
import org.apache.skywalking.oap.server.ai.evaluation.context.AIEvaluationContext;
import org.apache.skywalking.oap.server.ai.evaluation.plan.EvaluationResult;

@Slf4j
public class AIEvaluationMetricReporter {
    public static final String RULE_CATALOG = "ai-evaluation-rules";
    public static final String RULE_NAME = "default";
    public static final String SAMPLE_SCORE_NAME = "gen_ai_evaluation_score_ppm";
    private static final double SCORE_SCALE = 1_000_000D;

    private final List<MetricConvert> metricConverts;

    public AIEvaluationMetricReporter(final List<MetricConvert> metricConverts) {
        this.metricConverts = metricConverts;
    }

    public void reportScore(final AIEvaluationContext context,
                            final EvaluationResult result,
                            final long evaluationTime) {
        final double score;
        try {
            score = Double.parseDouble(result.getValue());
        } catch (NumberFormatException e) {
            log.warn("Skip AI evaluation score metric, invalid score: {}", result.getValue(), e);
            return;
        }

        final Sample sample = Sample.builder()
                .name(SAMPLE_SCORE_NAME)
                .timestamp(evaluationTime)
                .value(score * SCORE_SCALE)
                .labels(ImmutableMap.copyOf(labels(context, result)))
                .build();
        final ImmutableMap<String, SampleFamily> sampleFamilies = ImmutableMap.of(
                SAMPLE_SCORE_NAME,
                SampleFamilyBuilder.newBuilder(sample).build()
        );
        metricConverts.forEach(convert -> convert.toMeter(sampleFamilies));
    }

    private static Map<String, String> labels(final AIEvaluationContext context,
                                              final EvaluationResult result) {
        return ImmutableMap.of(
                "provider_name", defaultString(context.getProviderName()),
                "model_name", defaultString(context.getModelName()),
                "task_name", defaultString(result.getName())
        );
    }

    private static String defaultString(final String value) {
        return value == null ? "" : value;
    }
}
