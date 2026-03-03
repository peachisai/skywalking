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

package org.apache.skywalking.oap.meter.analyzer.config;

import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public class GenAIConfigLoader {

    private final GenAIConfig config;
    private final Yaml yaml;

    public GenAIConfigLoader(GenAIConfig config, Yaml yaml) {
        this.config = config;
        this.yaml = yaml;
    }

    public GenAIConfig loadConfig() throws ModuleStartException {
        Reader applicationReader;
        try {
            applicationReader = ResourceUtils.read("gen-ai-config.yml");
        } catch (FileNotFoundException e) {
            throw new ModuleStartException("Cannot find the GenAI configuration file [gen-ai-config.yml].", e);
        }

        Map<String, List<Map<String, Object>>> configMap = yaml.loadAs(applicationReader, Map.class);
        if (configMap == null || !configMap.containsKey("providers")) {
            return config;
        }

        List<Map<String, Object>> providersConfig = configMap.get("providers");
        for (Map<String, Object> providerMap : providersConfig) {
            GenAIConfig.Provider provider = new GenAIConfig.Provider();

            Object name = providerMap.get("provider");
            if (name == null) {
                throw new ModuleStartException("Provider name is missing in [gen-ai-config.yml].");
            }
            provider.setProvider(name.toString());

            Object baseUrl = providerMap.get("base-url");
            if (baseUrl != null) {
                provider.setBaseUrl(baseUrl.toString());
            }

            Object prefixMatch = providerMap.get("prefix-match");
            if (prefixMatch instanceof List) {
                provider.getPrefixMatch().addAll((List<String>) prefixMatch);
            } else if (prefixMatch != null) {
                throw new ModuleStartException("prefix-match must be a list in [gen-ai-config.yml] for provider: " + name);
            }

            config.getProviders().add(provider);
        }

        return config;
    }
}
