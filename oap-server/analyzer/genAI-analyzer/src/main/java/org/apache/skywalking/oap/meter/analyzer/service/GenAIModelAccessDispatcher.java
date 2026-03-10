package org.apache.skywalking.oap.meter.analyzer.service;

import org.apache.skywalking.oap.server.core.analysis.SourceDispatcher;
import org.apache.skywalking.oap.server.core.analysis.manual.instance.InstanceTraffic;
import org.apache.skywalking.oap.server.core.analysis.worker.MetricsStreamProcessor;
import org.apache.skywalking.oap.server.core.source.GenAIModelAccess;

public class GenAIModelAccessDispatcher implements SourceDispatcher<GenAIModelAccess> {

    @Override
    public void dispatch(GenAIModelAccess source) {
        InstanceTraffic traffic = new InstanceTraffic();
        traffic.setTimeBucket(source.getTimeBucket());
        traffic.setName(source.getModelName());
        traffic.setServiceId(source.getServiceId());
        traffic.setLastPingTimestamp(source.getTimeBucket());
        MetricsStreamProcessor.getInstance().in(traffic);
    }
}
