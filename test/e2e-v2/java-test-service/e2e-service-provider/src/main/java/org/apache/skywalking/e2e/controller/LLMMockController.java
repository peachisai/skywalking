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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/llm")
public class LLMMockController {
    @PostMapping("/v1/chat/completions")
    public Object completions(@RequestBody JSONObject request) {

        String jokeResponse = String.format(
                "{\n" +
                        "    \"id\": \"chatcmpl-simple-mock-001\",\n" +
                        "    \"object\": \"chat.completion\",\n" +
                        "    \"created\": %d,\n" +
                        "    \"model\": \"gpt-4.1-mini\",\n" +
                        "    \"choices\": [\n" +
                        "        {\n" +
                        "            \"index\": 0,\n" +
                        "            \"message\": {\n" +
                        "                \"role\": \"assistant\",\n" +
                        "                \"content\": \"Why did the scarecrow win an award? Because he was outstanding in his field!\"\n" +
                        "            },\n" +
                        "            \"finish_reason\": \"stop\"\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"usage\": {\n" +
                        "        \"prompt_tokens\": 5,\n" +
                        "        \"completion_tokens\": 15,\n" +
                        "        \"total_tokens\": 20\n" +
                        "    }\n" +
                        "}", Instant.now().getEpochSecond());

        return JSON.parseObject(jokeResponse);
    }
}
