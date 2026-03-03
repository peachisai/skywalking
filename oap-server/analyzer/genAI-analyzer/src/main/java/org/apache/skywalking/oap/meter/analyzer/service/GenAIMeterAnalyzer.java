package org.apache.skywalking.oap.meter.analyzer.service;

import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.oap.meter.analyzer.matcher.GenAIProviderPrefixMatcher;

import java.util.Map;
import java.util.stream.Collectors;

public class GenAIMeterAnalyzer implements IGenAIMeterAnalyzerService {

    private final GenAIProviderPrefixMatcher matcher;

    public GenAIMeterAnalyzer(GenAIProviderPrefixMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void doTraceAnalysis(SpanObject span, SegmentObject segment) {
        Map<String, String> tags = span.getTagsList().stream()
                .collect(
                        Collectors.toMap(KeyStringValuePair::getKey, KeyStringValuePair::getValue));

        // Get the AI service provider (e.g., OpenAI, Anthropic)
        String provider = tags.get("gen_ai.provider.name");
        String requestModel = tags.get("gen_ai.request.model");
        matcher.findProvider(requestModel);

        // Get the specific model name (e.g., gpt-4, claude-3)
        // String model = tags.get("model");

        // Generate metrics for tool invocation counts

        // Generate metrics for LLM invocation latency

        // Generate metrics for input, output, and total token usage

        // Process token-specific logic

        //
    }
}
