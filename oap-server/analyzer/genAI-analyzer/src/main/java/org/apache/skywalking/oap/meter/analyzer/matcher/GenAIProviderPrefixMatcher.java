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

package org.apache.skywalking.oap.meter.analyzer.matcher;

import org.apache.skywalking.oap.meter.analyzer.config.GenAIConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenAIProviderPrefixMatcher {
    private static final String UNKNOWN = "unknown";
    private final TrieNode root;

    private GenAIProviderPrefixMatcher(TrieNode root) {
        this.root = root;
    }

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        String providerName;
    }

    public static GenAIProviderPrefixMatcher build(GenAIConfig config) {
        TrieNode root = new TrieNode();

        for (GenAIConfig.Provider p : config.getProviders()) {
            List<String> prefixes = p.getPrefixMatch();
            if (prefixes == null) continue;

            for (String prefix : prefixes) {
                if (prefix == null || prefix.isEmpty()) continue;

                TrieNode current = root;
                for (int i = 0; i < prefix.length(); i++) {
                    char c = prefix.charAt(i);
                    current = current.children
                            .computeIfAbsent(c, k -> new TrieNode());
                }
                current.providerName = p.getProvider();
            }
        }

        return new GenAIProviderPrefixMatcher(root);
    }

    public String findProvider(String modelName) {

        TrieNode current = root;
        String matched = null;

        for (int i = 0; i < modelName.length(); i++) {
            current = current.children.get(modelName.charAt(i));
            if (current == null) break;
            if (current.providerName != null) {
                matched = current.providerName;
            }
        }
        return matched != null ? matched : UNKNOWN;
    }
}
