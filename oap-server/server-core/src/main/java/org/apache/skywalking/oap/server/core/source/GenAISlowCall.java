package org.apache.skywalking.oap.server.core.source;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.topn.TopN;
import org.apache.skywalking.oap.server.core.analysis.worker.TopNStreamProcessor;
import org.apache.skywalking.oap.server.core.storage.StorageID;
import org.apache.skywalking.oap.server.core.storage.annotation.BanyanDB;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Entity;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Storage;
import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;

import java.util.Objects;

@Stream(name = GenAISlowCall.INDEX_NAME, scopeId = DefaultScopeDefine.GEN_AI_MODEL_ACCESS, builder = GenAISlowCall.Builder.class, processor = TopNStreamProcessor.class)
@BanyanDB.TimestampColumn(TopN.TIMESTAMP)
@BanyanDB.Group(streamGroup = BanyanDB.StreamGroup.RECORDS)
public class GenAISlowCall extends TopN {

    public static final String INDEX_NAME = "top_n_service_gen_ai";

    @Setter
    private String id;
    @Getter
    @Setter
    @Column(name = STATEMENT, length = 2000, storageOnly = true)
    private String statement;

    @Override
    public StorageID id() {
        return new StorageID().append(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GenAISlowCall statement = (GenAISlowCall) o;
        return Objects.equals(getEntityId(), statement.getEntityId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntityId());
    }

    public static class Builder implements StorageBuilder<GenAISlowCall> {
        @Override
        public GenAISlowCall storage2Entity(final Convert2Entity converter) {
            GenAISlowCall statement = new GenAISlowCall();
            statement.setStatement((String) converter.get(STATEMENT));
            statement.setTraceId((String) converter.get(TRACE_ID));
            statement.setLatency(((Number) converter.get(LATENCY)).longValue());
            statement.setEntityId((String) converter.get(ENTITY_ID));
            statement.setTimeBucket(((Number) converter.get(TIME_BUCKET)).longValue());
            statement.setTimestamp(((Number) converter.get(TIMESTAMP)).longValue());
            return statement;
        }

        @Override
        public void entity2Storage(final GenAISlowCall storageData, final Convert2Storage converter) {
            converter.accept(STATEMENT, storageData.getStatement());
            converter.accept(TRACE_ID, storageData.getTraceId());
            converter.accept(LATENCY, storageData.getLatency());
            converter.accept(ENTITY_ID, storageData.getEntityId());
            converter.accept(TIME_BUCKET, storageData.getTimeBucket());
            converter.accept(TIMESTAMP, storageData.getTimestamp());
        }
    }
}
