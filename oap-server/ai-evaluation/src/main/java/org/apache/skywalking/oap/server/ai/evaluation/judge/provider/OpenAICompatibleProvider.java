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

package org.apache.skywalking.oap.server.ai.evaluation.judge.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.apache.skywalking.oap.server.ai.evaluation.AIEvaluationConfig;
import org.apache.skywalking.oap.server.ai.evaluation.judge.JudgeModelProvider;
import org.apache.skywalking.oap.server.ai.evaluation.judge.JudgeModelRequest;
import org.apache.skywalking.oap.server.ai.evaluation.judge.JudgeModelResponse;

public class OpenAICompatibleProvider implements JudgeModelProvider {
    private static final Gson GSON = new Gson();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final String model;

    public OpenAICompatibleProvider(final AIEvaluationConfig.Judge config) {
        this(HttpClient.newHttpClient(), config);
    }

    OpenAICompatibleProvider(final HttpClient httpClient, final AIEvaluationConfig.Judge config) {
        this.httpClient = httpClient;
        this.endpoint = normalizeEndpoint(config.getBaseUrl());
        this.apiKey = config.getApiKey();
        this.model = config.getModel();
    }

    @Override
    public Optional<JudgeModelResponse> judge(final JudgeModelRequest request)
        throws IOException, InterruptedException {
        if (!isConfigured()) {
            return Optional.empty();
        }

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                                                   .uri(URI.create(endpoint))
                                                   .timeout(REQUEST_TIMEOUT)
                                                   .header("Authorization", "Bearer " + apiKey)
                                                   .header("Content-Type", "application/json")
                                                   .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                                                   .build();
        final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI compatible judge API request failed, status: " + response.statusCode());
        }
        final JudgeModelResponse judgeResponse = parseResponse(response.body());
        if (isEmpty(judgeResponse.getContent())) {
            throw new IOException("OpenAI compatible judge API response has no completion content.");
        }
        return Optional.of(judgeResponse);
    }

    @Override
    public String model() {
        return model;
    }

    private boolean isConfigured() {
        return !isEmpty(endpoint) && !isEmpty(apiKey) && !isEmpty(model);
    }

    private String buildRequestBody(final JudgeModelRequest request) {
        final JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", false);

        final JsonArray messages = new JsonArray();
        addMessage(messages, "system", request.getSystemPrompt());
        addMessage(messages, "user", request.getUserPrompt());
        body.add("messages", messages);
        return GSON.toJson(body);
    }

    private static void addMessage(final JsonArray messages, final String role, final String content) {
        if (isEmpty(content)) {
            return;
        }
        final JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        messages.add(message);
    }

    private static JudgeModelResponse parseResponse(final String body) {
        final JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        final JsonArray choices = root.getAsJsonArray("choices");
        String content = "";
        if (choices != null && choices.size() > 0) {
            final JsonObject choice = choices.get(0).getAsJsonObject();
            final JsonObject message = choice.getAsJsonObject("message");
            if (message != null) {
                content = getAsString(message, "content");
            }
        }

        final JsonObject usage = root.getAsJsonObject("usage");
        return JudgeModelResponse.builder()
                                 .content(content)
                                 .promptTokens(getAsInt(usage, "prompt_tokens"))
                                 .completionTokens(getAsInt(usage, "completion_tokens"))
                                 .totalTokens(getAsInt(usage, "total_tokens"))
                                 .build();
    }

    private static String normalizeEndpoint(final String baseUrl) {
        if (isEmpty(baseUrl)) {
            return "";
        }
        final String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed + "/chat/completions";
    }

    private static String getAsString(final JsonObject object, final String memberName) {
        if (object == null) {
            return "";
        }
        final JsonElement element = object.get(memberName);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static int getAsInt(final JsonObject object, final String memberName) {
        if (object == null) {
            return 0;
        }
        final JsonElement element = object.get(memberName);
        return element == null || element.isJsonNull() ? 0 : element.getAsInt();
    }

    private static boolean isEmpty(final String value) {
        return value == null || value.isEmpty();
    }
}
