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

package org.apache.skywalking.e2e.controller;

import lombok.RequiredArgsConstructor;
import org.apache.skywalking.e2e.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/genai")
@RequiredArgsConstructor
public class GenAICallController {

    private final WeatherTool weatherTool;
    private final ChatClient chatClient;

    @GetMapping("/testSpringAI")
    public String testCase() throws Exception {

        String systemPrompt = "You are a professional technical assistant.\n" +
                "Strictly use the provided context to answer questions.\n" +
                "If the information is not in the context, say: \"I'm sorry, I don't have that information in my knowledge base.\"\n" +
                "Do not use outside knowledge. Be concise.\n";

        chatClient
                .prompt("What's the weather in New York?")
                .system(systemPrompt)
                .tools(weatherTool)
                .call()
                .content();

        chatClient
                .prompt("What's the weather in New York?")
                .system(systemPrompt)
                .tools(weatherTool)
                .stream()
                .content()
                .doOnNext(System.out::println)
                .blockLast();

        return "success";
    }
}
