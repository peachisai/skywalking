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

package org.apache.skywalking.oap.server.ai.evaluation.storage;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.record.Record;
import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.source.ScopeDeclaration;
import org.apache.skywalking.oap.server.core.storage.StorageID;
import org.apache.skywalking.oap.server.core.storage.annotation.BanyanDB;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;
import org.apache.skywalking.oap.server.core.storage.annotation.ElasticSearch;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Entity;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Storage;
import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;

@Getter
@Setter
@ScopeDeclaration(id = DefaultScopeDefine.AI_EVALUATION_RESULT, name = "AIEvaluationResult")
@Stream(name = AIEvaluationResultRecord.INDEX_NAME, scopeId = DefaultScopeDefine.AI_EVALUATION_RESULT,
        builder = AIEvaluationResultRecord.Builder.class, processor = RecordStreamProcessor.class)
@BanyanDB.TimestampColumn(AIEvaluationResultRecord.EVALUATION_TIME)
@BanyanDB.Group(streamGroup = BanyanDB.StreamGroup.RECORDS)
public class AIEvaluationResultRecord extends Record {

    public static final String INDEX_NAME = "ai_evaluation_result";
    public static final String TRACE_ID = "trace_id";
    public static final String SEGMENT_ID = "segment_id";
    public static final String SPAN_ID = "span_id";
    public static final String SPAN_TYPE = "span_type";
    public static final String TASK_NAME = "task_name";
    public static final String VALUE_TYPE = "value_type";
    public static final String VALUE = "value";
    public static final String REASON = "reason";
    public static final String JUDGE_MODEL = "judge_model";
    public static final String EVALUATION_TIME = "evaluation_time";

    @Column(name = TRACE_ID, length = 150, storageOnly = true)
    @BanyanDB.SeriesID(index = 0)
    private String traceId;

    @Column(name = SEGMENT_ID, length = 150, storageOnly = true)
    private String segmentId;

    @Column(name = SPAN_ID, length = 150, storageOnly = true)
    private String spanId;

    @Column(name = SPAN_TYPE, length = 64)
    private String spanType;

    @Column(name = TASK_NAME, length = 512)
    private String taskName;

    @Column(name = VALUE_TYPE, length = 64)
    private String valueType;

    @Column(name = VALUE, length = 4096)
    private String value;

    @Column(name = REASON, length = 4096)
    private String reason;

    @Column(name = JUDGE_MODEL, length = 256)
    private String judgeModel;

    @ElasticSearch.EnableDocValues
    @Column(name = EVALUATION_TIME)
    private long evaluationTime;

    @Override
    public StorageID id() {
        return new StorageID()
                .append(TRACE_ID, traceId)
                .append(SPAN_ID, spanId)
                .append(SPAN_TYPE, spanType)
                .append(TASK_NAME, taskName)
                .append(EVALUATION_TIME, evaluationTime);
    }

    public static class Builder implements StorageBuilder<AIEvaluationResultRecord> {
        @Override
        public AIEvaluationResultRecord storage2Entity(final Convert2Entity converter) {
            final AIEvaluationResultRecord record = new AIEvaluationResultRecord();
            record.setTraceId((String) converter.get(TRACE_ID));
            record.setSegmentId((String) converter.get(SEGMENT_ID));
            record.setSpanId((String) converter.get(SPAN_ID));
            record.setSpanType((String) converter.get(SPAN_TYPE));
            record.setTaskName((String) converter.get(TASK_NAME));
            record.setValueType((String) converter.get(VALUE_TYPE));
            record.setValue((String) converter.get(VALUE));
            record.setReason((String) converter.get(REASON));
            record.setJudgeModel((String) converter.get(JUDGE_MODEL));
            record.setEvaluationTime(((Number) converter.get(EVALUATION_TIME)).longValue());
            record.setTimeBucket(((Number) converter.get(TIME_BUCKET)).longValue());
            return record;
        }

        @Override
        public void entity2Storage(final AIEvaluationResultRecord storageData, final Convert2Storage converter) {
            converter.accept(TRACE_ID, storageData.getTraceId());
            converter.accept(SEGMENT_ID, storageData.getSegmentId());
            converter.accept(SPAN_ID, storageData.getSpanId());
            converter.accept(SPAN_TYPE, storageData.getSpanType());
            converter.accept(TASK_NAME, storageData.getTaskName());
            converter.accept(VALUE_TYPE, storageData.getValueType());
            converter.accept(VALUE, storageData.getValue());
            converter.accept(REASON, storageData.getReason());
            converter.accept(JUDGE_MODEL, storageData.getJudgeModel());
            converter.accept(EVALUATION_TIME, storageData.getEvaluationTime());
            converter.accept(TIME_BUCKET, storageData.getTimeBucket());
        }
    }
}
