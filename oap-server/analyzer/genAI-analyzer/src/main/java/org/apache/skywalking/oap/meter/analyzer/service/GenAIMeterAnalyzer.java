package org.apache.skywalking.oap.meter.analyzer.service;

import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.oap.meter.analyzer.matcher.GenAIProviderPrefixMatcher;
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
    private static final String TAG_TOTAL_TOKENS = "gen_ai.client.token.usage";

    @Override
    public void doTraceAnalysis(SpanObject span, SegmentObject segment) {
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
            return;
        }

        String provider = matcher.findProvider(modelName);

        int inputTokens = parseSafeInt(tags.get(TAG_INPUT_TOKENS));
        int outputTokens = parseSafeInt(tags.get(TAG_OUTPUT_TOKENS));
        int totalTokens = parseSafeInt(tags.get(TAG_TOTAL_TOKENS));

        if (totalTokens <= 0) {
            totalTokens = inputTokens + outputTokens;
        }


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
