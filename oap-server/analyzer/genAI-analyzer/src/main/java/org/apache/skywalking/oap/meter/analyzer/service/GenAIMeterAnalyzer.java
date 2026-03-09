package org.apache.skywalking.oap.meter.analyzer.service;

import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.oap.meter.analyzer.config.GenAIConfig;
import org.apache.skywalking.oap.meter.analyzer.matcher.GenAIProviderPrefixMatcher;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.source.GenAIMetrics;
import org.apache.skywalking.oap.server.library.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class GenAIMeterAnalyzer implements IGenAIMeterAnalyzerService {

    private static final Logger LOG = LoggerFactory.getLogger(GenAIMeterAnalyzer.class);

    private final GenAIProviderPrefixMatcher matcher;

    public GenAIMeterAnalyzer(GenAIProviderPrefixMatcher matcher) {
        this.matcher = matcher;
    }

    private static final String TAG_MODEL = "gen_ai.response.model";
    private static final String TAG_INPUT_TOKENS = "gen_ai.usage.input_tokens";
    private static final String TAG_OUTPUT_TOKENS = "gen_ai.usage.output_tokens";
    private static final String TAG_TTFT = "gen_ai.usage.ttft";

    @Override
    public GenAIMetrics extractMetricsFromSWSpan(SpanObject span, SegmentObject segment) {
        Map<String, String> tags = span.getTagsList().stream()
                .collect(toMap(
                        KeyStringValuePair::getKey,
                        KeyStringValuePair::getValue,
                        (v1, v2) -> v1
                ));

        String modelName = tags.get(TAG_MODEL);

        if (StringUtil.isBlank(modelName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Model name is missing in span [{}], skipping GenAI analysis", span.getOperationName());
            }
            return null;
        }

        GenAIProviderPrefixMatcher.MatchResult matchResult = matcher.match(modelName);
        String provider = matchResult.getProvider();
        GenAIConfig.Model modelConfig = matchResult.getModelConfig();

        long inputTokens = parseSafeInt(tags.get(TAG_INPUT_TOKENS));
        long outputTokens = parseSafeInt(tags.get(TAG_OUTPUT_TOKENS));

        // calculate the total cost by the cost configs
        double totalCost = 0.0;
        if (modelConfig != null) {
            if (modelConfig.getInputCostPerM() > 0) {
                totalCost += (inputTokens / 1_000_000.0) * modelConfig.getInputCostPerM();
            }
            if (modelConfig.getOutputCostPerM() > 0) {
                totalCost += (outputTokens / 1_000_000.0) * modelConfig.getOutputCostPerM();
            }
        }

        tags.put("gen_ai.usage.total_cost", String.valueOf(totalCost));

        GenAIMetrics metrics = new GenAIMetrics();

        metrics.setProvider(provider);
        metrics.setModel(modelName);
        metrics.setInputTokens(inputTokens);
        metrics.setOutputTokens(outputTokens);
        metrics.setTotalCost(totalCost);

        long latency = span.getEndTime() - span.getStartTime();
        metrics.setLatency(latency);
        metrics.setStatus(!span.getIsError());
        metrics.setTimeBucket(TimeBucket.getMinuteTimeBucket(span.getStartTime()));

        return metrics;
    }

    private int parseSafeInt(String value) {
        if (StringUtil.isEmpty(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse token count: {}", value);
            return 0;
        }
    }
}
