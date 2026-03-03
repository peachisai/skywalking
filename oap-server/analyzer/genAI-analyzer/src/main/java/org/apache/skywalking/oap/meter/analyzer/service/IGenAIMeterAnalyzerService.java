package org.apache.skywalking.oap.meter.analyzer.service;

import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.oap.server.library.module.Service;

public interface IGenAIMeterAnalyzerService extends Service {

    void doTraceAnalysis(SpanObject span, SegmentObject segment);

}
