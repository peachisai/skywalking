package org.apache.skywalking.oap.server.core.analysis.manual.database;

import org.apache.skywalking.oap.server.core.analysis.SourceDispatcher;
import org.apache.skywalking.oap.server.core.analysis.worker.TopNStreamProcessor;
import org.apache.skywalking.oap.server.core.source.ServiceDatabaseSlowStatement;

public class ServiceDatabaseSlowStatementDispatcher implements SourceDispatcher<ServiceDatabaseSlowStatement> {
    @Override
    public void dispatch(ServiceDatabaseSlowStatement source) {
        TopNServiceDatabaseStatement statement = new TopNServiceDatabaseStatement();
        statement.setId(source.getId());
        statement.setEntityId(source.getServiceId());
        statement.setLatency(source.getLatency());
        statement.setStatement(source.getStatement());
        statement.setTimeBucket(source.getTimeBucket());
        statement.setTraceId(source.getTraceId());
        statement.setTimestamp(source.getTimestamp());

        TopNStreamProcessor.getInstance().in(statement);
    }
}
