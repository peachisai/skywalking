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

package org.apache.skywalking.oap.server.ai.evalution.service.strategy.span;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.ai.evalution.AIEvalutionContext;
import org.apache.skywalking.oap.server.ai.evalution.GenAISemanticAttributes;
import org.apache.skywalking.oap.server.ai.evalution.judge.JudgeModelProvider;
import org.apache.skywalking.oap.server.ai.evalution.judge.JudgeModelResponse;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationPlan;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationPlanner;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationPromptBuilder;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationResult;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationResultParser;
import org.apache.skywalking.oap.server.ai.evalution.storage.AIEvalutionResultRecord;
import org.apache.skywalking.oap.server.ai.evalution.service.strategy.AIEvalutionStrategy;
import org.apache.skywalking.oap.server.ai.evalution.task.EvaluationTaskRegistry;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;

@Slf4j
public class SpanAIEvalutionStrategy implements AIEvalutionStrategy {
    private static final String CHAT_OPERATION = "chat";

    private final EvaluationTaskRegistry taskRegistry;
    private final EvaluationPlanner evaluationPlanner;
    private final EvaluationPromptBuilder promptBuilder;
    private final EvaluationResultParser resultParser;

    public SpanAIEvalutionStrategy(final EvaluationTaskRegistry taskRegistry,
                                   final EvaluationPlanner evaluationPlanner,
                                   final EvaluationPromptBuilder promptBuilder,
                                   final EvaluationResultParser resultParser) {
        this.taskRegistry = taskRegistry;
        this.evaluationPlanner = evaluationPlanner;
        this.promptBuilder = promptBuilder;
        this.resultParser = resultParser;
    }

    @Override
    public boolean support(final AIEvalutionContext context) {
        return context != null && !isEmpty(context.getTraceId()) && !isEmpty(context.getSpanId());
    }

    @Override
    public String taskId(final AIEvalutionContext context) {
        return context.getTraceId() + "/" + context.getSpanId();
    }

    @Override
    public void evaluate(final AIEvalutionContext context,
                         final JudgeModelProvider judgeModelProvider) throws IOException, InterruptedException {
        if (taskRegistry.isEmpty()) {
            log.debug("Skip GenAI span evalution, no evaluation task configured, taskId: {}", taskId(context));
            return;
        }

        if (isLLMCallSpan(context)) {
            log.debug(
                    "Skip GenAI span evalution, unsupported operation: {}, taskId: {}",
                    operationName(context),
                    taskId(context)
            );
            evaluateLLMCallSpan(context, judgeModelProvider);
        }
    }

    private void evaluateLLMCallSpan(final AIEvalutionContext context,
                                     final JudgeModelProvider judgeModelProvider)
            throws IOException, InterruptedException {
        final String judgeModel = judgeModelProvider.model();
        final List<EvaluationPlan> plans = evaluationPlanner.plan(context, taskRegistry.tasks());
        for (EvaluationPlan plan : plans) {
            final Optional<JudgeModelResponse> response = judgeModelProvider.judge(promptBuilder.build(plan));
            if (!response.isPresent()) {
                continue;
            }

            final JudgeModelResponse judgeResponse = response.get();
            final List<EvaluationResult> results = resultParser.parse(plan, judgeResponse.getContent());
            persistResults(context, plan, results, judgeModel);
            log.info(
                    "GenAI LLM call span evalution result, taskId: {}, spanType: {}, resultCount: {}, results: {}",
                    taskId(context),
                    plan.getSpanType(),
                    results.size(),
                    results
            );
        }
    }

    private static void persistResults(final AIEvalutionContext context,
                                       final EvaluationPlan plan,
                                       final List<EvaluationResult> results,
                                       final String judgeModel) {
        final long evaluationTime = System.currentTimeMillis();
        final String segmentId = "";
        for (EvaluationResult result : results) {
            final AIEvalutionResultRecord record = new AIEvalutionResultRecord();
            record.setTraceId(context.getTraceId());
            record.setSegmentId(segmentId);
            record.setSpanId(context.getSpanId());
            record.setSpanType(plan.getSpanType() == null ? "" : plan.getSpanType().name());
            record.setTaskName(result.getName());
            record.setValueType(result.getValueType() == null ? "" : result.getValueType().name());
            record.setValue(result.getValue());
            record.setReason(result.getReason());
            record.setJudgeModel(judgeModel);
            record.setEvaluationTime(evaluationTime);
            record.setTimeBucket(TimeBucket.getRecordTimeBucket(evaluationTime));
            RecordStreamProcessor.getInstance().in(record);
        }
    }

    private static boolean isLLMCallSpan(final AIEvalutionContext context) {
        return CHAT_OPERATION.equalsIgnoreCase(operationName(context));
    }

    private static String operationName(final AIEvalutionContext context) {
        return context.getTags().get(GenAISemanticAttributes.OPERATION_NAME);
    }

    private static boolean isEmpty(final String value) {
        return value == null || value.isEmpty();
    }
}
