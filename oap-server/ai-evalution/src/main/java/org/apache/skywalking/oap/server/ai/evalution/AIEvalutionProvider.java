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

package org.apache.skywalking.oap.server.ai.evalution;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationInputExtractor;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationPlanner;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationPromptBuilder;
import org.apache.skywalking.oap.server.ai.evalution.plan.EvaluationResultParser;
import org.apache.skywalking.oap.server.ai.evalution.task.EvaluationTaskRegistry;
import org.apache.skywalking.oap.server.ai.evalution.judge.JudgeModelProvider;
import org.apache.skywalking.oap.server.ai.evalution.judge.provider.OpenAICompatibleProvider;
import org.apache.skywalking.oap.server.ai.evalution.service.AIEvalutionService;
import org.apache.skywalking.oap.server.ai.evalution.service.sample.DefaultAIEvalutionSamplingPolicy;
import org.apache.skywalking.oap.server.ai.evalution.service.IAIEvalutionService;
import org.apache.skywalking.oap.server.ai.evalution.service.strategy.AIEvalutionStrategy;
import org.apache.skywalking.oap.server.ai.evalution.service.strategy.span.SpanAIEvalutionStrategy;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;

public class AIEvalutionProvider extends ModuleProvider {
    private static final int MAX_SAMPLE_RATE = 1_000_000;
    private AIEvalutionConfig config = new AIEvalutionConfig();

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return AIEvalutionModule.class;
    }

    @Override
    public ConfigCreator<? extends ModuleConfig> newConfigCreator() {
        return new ConfigCreator<AIEvalutionConfig>() {
            @Override
            public Class<AIEvalutionConfig> type() {
                return AIEvalutionConfig.class;
            }

            @Override
            public void onInitialized(final AIEvalutionConfig initialized) {
                config = initialized;
            }
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        final int sampleRate = config.getSampleRate();
        config = new AIEvalutionConfigLoader().load();
        config.setSampleRate(sampleRate);
        validateConfig(config);
        if (config.getSampleRate() < 0 || config.getSampleRate() > MAX_SAMPLE_RATE) {
            throw new IllegalArgumentException(
                "sampleRate: " + config.getSampleRate() + ", should be between 0 and " + MAX_SAMPLE_RATE);
        }
        registerServiceImplementation(
            IAIEvalutionService.class,
            new AIEvalutionService(
                new DefaultAIEvalutionSamplingPolicy(config.getSampleRate()),
                createJudgeProvider(),
                createStrategies()
            )
        );
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
    }

    @Override
    public String[] requiredModules() {
        return new String[0];
    }

    private JudgeModelProvider createJudgeProvider() throws ModuleStartException {
        final AIEvalutionConfig.Judge judge = buildJudgeConfig(config.getJudge());
        if ("openai".equalsIgnoreCase(judge.getProvider())) {
            return new OpenAICompatibleProvider(judge);
        }
        throw new ModuleStartException("Unsupported AI evalution judge provider: " + judge.getProvider());
    }

    private List<AIEvalutionStrategy> createStrategies() {
        final EvaluationTaskRegistry taskRegistry = new EvaluationTaskRegistry(config.getTasks());
        final EvaluationInputExtractor inputExtractor = new EvaluationInputExtractor();
        return Collections.singletonList(new SpanAIEvalutionStrategy(
            taskRegistry,
            new EvaluationPlanner(inputExtractor),
            new EvaluationPromptBuilder(config.getSystemPrompt()),
            new EvaluationResultParser()
        ));
    }

    private static AIEvalutionConfig.Judge buildJudgeConfig(final Properties properties) {
        final AIEvalutionConfig.Judge judge = new AIEvalutionConfig.Judge();
        judge.setProvider(getString(properties, "provider"));
        judge.setBaseUrl(getString(properties, "base-url"));
        judge.setModel(getString(properties, "model"));
        judge.setApiKey(getString(properties, "api-key"));
        return judge;
    }

    private static void validateConfig(final AIEvalutionConfig config) throws ModuleStartException {
        final Properties judge = config.getJudge();
        validateJudgeProperty(judge, "provider");
        validateJudgeProperty(judge, "base-url");
        validateJudgeProperty(judge, "model");
        validateJudgeProperty(judge, "api-key");
        if (isBlank(config.getSystemPrompt())) {
            throw new ModuleStartException("AI evalution system-prompt is required.");
        }
    }

    private static void validateJudgeProperty(final Properties judge,
                                              final String key) throws ModuleStartException {
        if (judge == null || isBlank(getString(judge, key))) {
            throw new ModuleStartException("AI evalution judge config [" + key + "] is required.");
        }
    }

    private static String getString(final Properties properties, final String key) {
        if (properties == null) {
            return null;
        }
        final Object value = properties.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
