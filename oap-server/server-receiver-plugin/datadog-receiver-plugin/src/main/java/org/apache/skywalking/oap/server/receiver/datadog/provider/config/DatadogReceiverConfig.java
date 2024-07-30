package org.apache.skywalking.oap.server.receiver.datadog.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;

@Getter
@Setter
public class DatadogReceiverConfig extends ModuleConfig {

    private int port = 8126;

    private int maxThread = 200;

    private String contextPath = "";
}
