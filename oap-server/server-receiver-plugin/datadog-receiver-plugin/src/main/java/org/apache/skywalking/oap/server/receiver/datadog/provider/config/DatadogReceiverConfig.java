package org.apache.skywalking.oap.server.receiver.datadog.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;

@Getter
@Setter
public class DatadogReceiverConfig extends ModuleConfig {

    public int port = 8126;

    public int maxThread = 200;

    public int sampleRate = 10000;

    public String contextPath = "";
}
