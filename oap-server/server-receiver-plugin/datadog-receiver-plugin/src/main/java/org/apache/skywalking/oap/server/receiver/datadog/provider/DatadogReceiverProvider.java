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

package org.apache.skywalking.oap.server.receiver.datadog.provider;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.receiver.datadog.module.DatadogReceiverModule;
import org.apache.skywalking.oap.server.receiver.datadog.provider.config.DatadogReceiverConfig;
import org.apache.skywalking.oap.server.receiver.datadog.provider.server.DatadogReceiverServer;

@Slf4j
public class DatadogReceiverProvider extends ModuleProvider {

    private DatadogReceiverConfig config;

    private DatadogReceiverServer server;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return DatadogReceiverModule.class;
    }

    @Override
    public ConfigCreator<? extends ModuleConfig> newConfigCreator() {

        return new ConfigCreator<DatadogReceiverConfig>() {
            @Override
            public Class<DatadogReceiverConfig> type() {
                return DatadogReceiverConfig.class;
            }

            @Override
            public void onInitialized(DatadogReceiverConfig initialized) {
                config = initialized;
            }
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        server = new DatadogReceiverServer(config, getManager());
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {

        checkConfig();

        try {
            server.start();
        } catch (InterruptedException e) {
            log.error("Datadog receiver server start fail at port:{}", config.getPort(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public String[] requiredModules() {
        return new String[]{
                CoreModule.NAME
        };
    }

    private void checkConfig() {

        if (config.getMaxThread() < 0) {
            throw new IllegalArgumentException(
                    "maxThread: " + config.getMaxThread() + ", should be greater than 0");
        }
    }
}
