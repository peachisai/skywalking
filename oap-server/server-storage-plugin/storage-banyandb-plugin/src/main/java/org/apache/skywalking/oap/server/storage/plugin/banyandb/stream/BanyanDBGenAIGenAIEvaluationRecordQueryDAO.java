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

package org.apache.skywalking.oap.server.storage.plugin.banyandb.stream;

import com.google.common.collect.ImmutableSet;
import org.apache.skywalking.library.banyandb.v1.client.AbstractQuery;
import org.apache.skywalking.library.banyandb.v1.client.RowEntity;
import org.apache.skywalking.library.banyandb.v1.client.StreamQuery;
import org.apache.skywalking.library.banyandb.v1.client.StreamQueryResponse;
import org.apache.skywalking.oap.server.core.analysis.manual.genai.GenAIEvaluationResultRecord;
import org.apache.skywalking.oap.server.core.analysis.manual.log.LogRecord;
import org.apache.skywalking.oap.server.core.analysis.manual.searchtag.Tag;
import org.apache.skywalking.oap.server.core.query.enumeration.Order;
import org.apache.skywalking.oap.server.core.query.input.Duration;
import org.apache.skywalking.oap.server.core.query.input.TraceScopeCondition;
import org.apache.skywalking.oap.server.core.query.type.ContentType;
import org.apache.skywalking.oap.server.core.query.type.KeyValue;
import org.apache.skywalking.oap.server.core.query.type.Log;
import org.apache.skywalking.oap.server.core.query.type.Logs;
import org.apache.skywalking.oap.server.core.storage.query.IGenAIEvaluationRecordQueryDAO;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.storage.plugin.banyandb.BanyanDBStorageClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link org.apache.skywalking.oap.server.core.analysis.manual.log.LogRecord} is a gen-ai evaluation result
 */
public class BanyanDBGenAIGenAIEvaluationRecordQueryDAO extends AbstractBanyanDBDAO implements IGenAIEvaluationRecordQueryDAO {
    private static final Set<String> TAGS = ImmutableSet.of(
        GenAIEvaluationResultRecord.TRACE_ID,
        GenAIEvaluationResultRecord.SEGMENT_ID,
        GenAIEvaluationResultRecord.SPAN_ID,
        GenAIEvaluationResultRecord.SPAN_TYPE,
        GenAIEvaluationResultRecord.PROVIDER_NAME,
        GenAIEvaluationResultRecord.MODEL_NAME,
        GenAIEvaluationResultRecord.TASK_NAME,
        GenAIEvaluationResultRecord.VALUE_TYPE,
        GenAIEvaluationResultRecord.VALUE,
        GenAIEvaluationResultRecord.REASON,
        GenAIEvaluationResultRecord.JUDGE_MODEL,
        GenAIEvaluationResultRecord.EVALUATION_TIME
    );

    public BanyanDBGenAIGenAIEvaluationRecordQueryDAO(BanyanDBStorageClient client) {
        super(client);
    }

    @Override
    public Logs queryGenAIEvaluationRecord(String serviceId, String serviceInstanceId, String endpointId,
                                           TraceScopeCondition relatedTrace, Order queryOrder, int from, int limit,
                                           Duration duration, List<Tag> tags, List<String> keywordsOfContent,
                                           List<String> excludingKeywordsOfContent) throws IOException {
        final boolean isColdStage = duration != null && duration.isColdStage();
        final QueryBuilder<StreamQuery> query = new QueryBuilder<StreamQuery>() {
            @Override
            public void apply(StreamQuery query) {
//                if (StringUtil.isNotEmpty(serviceId)) {
//                    query.and(eq(AbstractLogRecord.SERVICE_ID, serviceId));
//                }
//
//                if (StringUtil.isNotEmpty(serviceInstanceId)) {
//                    if (StringUtil.isEmpty(serviceId)) {
//                        IDManager.ServiceInstanceID.InstanceIDDefinition instanceIDDefinition = IDManager.ServiceInstanceID.analysisId(
//                            serviceInstanceId);
//                        query.and(eq(AbstractLogRecord.SERVICE_ID, instanceIDDefinition.getServiceId()));
//                    }
//                    query.and(eq(AbstractLogRecord.SERVICE_INSTANCE_ID, serviceInstanceId));
//                }
//                if (StringUtil.isNotEmpty(endpointId)) {
//                    if (StringUtil.isEmpty(serviceId)) {
//                        IDManager.EndpointID.EndpointIDDefinition endpointIDDefinition = IDManager.EndpointID.analysisId(
//                            endpointId);
//                        query.and(eq(AbstractLogRecord.SERVICE_ID, endpointIDDefinition.getServiceId()));
//                    }
//                    query.and(eq(AbstractLogRecord.ENDPOINT_ID, endpointId));
//                }
//                if (Objects.nonNull(relatedTrace)) {
//                    if (StringUtil.isNotEmpty(relatedTrace.getTraceId())) {
//                        query.and(eq(AbstractLogRecord.TRACE_ID, relatedTrace.getTraceId()));
//                    }
//                    if (StringUtil.isNotEmpty(relatedTrace.getSegmentId())) {
//                        query.and(eq(AbstractLogRecord.TRACE_SEGMENT_ID, relatedTrace.getSegmentId()));
//                    }
//                    if (Objects.nonNull(relatedTrace.getSpanId())) {
//                        query.and(eq(AbstractLogRecord.SPAN_ID, (long) relatedTrace.getSpanId()));
//                    }
//                }

                if (CollectionUtils.isNotEmpty(tags)) {
                    List<String> tagsConditions = new ArrayList<>(tags.size());
                    for (final Tag tag : tags) {
                        tagsConditions.add(tag.toString());
                    }
                    query.and(having(LogRecord.TAGS, tagsConditions));
                }
                if (queryOrder == Order.ASC) {
                    query.setOrderBy(
                        new AbstractQuery.OrderBy(AbstractQuery.Sort.ASC));
                } else {
                    query.setOrderBy(
                        new AbstractQuery.OrderBy(AbstractQuery.Sort.DESC));
                }
                query.setLimit(limit);
                query.setOffset(from);
            }
        };

        StreamQueryResponse resp = queryDebuggable(isColdStage, GenAIEvaluationResultRecord.INDEX_NAME, TAGS, getTimestampRange(duration), query);

        Logs logs = new Logs();

        for (final RowEntity rowEntity : resp.getElements()) {
            Log log = new Log();
            log.setTraceId(rowEntity.getTagValue(GenAIEvaluationResultRecord.TRACE_ID));
            log.setTimestamp(((Number) rowEntity.getTagValue(GenAIEvaluationResultRecord.EVALUATION_TIME)).longValue());
            log.setContentType(ContentType.TEXT);
            log.setContent(rowEntity.getTagValue(GenAIEvaluationResultRecord.VALUE));
            appendTag(log, GenAIEvaluationResultRecord.SEGMENT_ID, rowEntity.getTagValue(GenAIEvaluationResultRecord.SEGMENT_ID));
            appendTag(log, GenAIEvaluationResultRecord.SPAN_ID, rowEntity.getTagValue(GenAIEvaluationResultRecord.SPAN_ID));
            appendTag(log, GenAIEvaluationResultRecord.SPAN_TYPE, rowEntity.getTagValue(GenAIEvaluationResultRecord.SPAN_TYPE));
            appendTag(log, GenAIEvaluationResultRecord.PROVIDER_NAME, rowEntity.getTagValue(GenAIEvaluationResultRecord.PROVIDER_NAME));
            appendTag(log, GenAIEvaluationResultRecord.MODEL_NAME, rowEntity.getTagValue(GenAIEvaluationResultRecord.MODEL_NAME));
            appendTag(log, GenAIEvaluationResultRecord.TASK_NAME, rowEntity.getTagValue(GenAIEvaluationResultRecord.TASK_NAME));
            appendTag(log, GenAIEvaluationResultRecord.VALUE_TYPE, rowEntity.getTagValue(GenAIEvaluationResultRecord.VALUE_TYPE));
            appendTag(log, GenAIEvaluationResultRecord.REASON, rowEntity.getTagValue(GenAIEvaluationResultRecord.REASON));
            appendTag(log, GenAIEvaluationResultRecord.JUDGE_MODEL, rowEntity.getTagValue(GenAIEvaluationResultRecord.JUDGE_MODEL));
            logs.getLogs().add(log);
        }
        return logs;
    }

    private void appendTag(final Log log, final String key, final Object value) {
        if (value != null) {
            log.getTags().add(new KeyValue(key, String.valueOf(value)));
        }
    }
}
