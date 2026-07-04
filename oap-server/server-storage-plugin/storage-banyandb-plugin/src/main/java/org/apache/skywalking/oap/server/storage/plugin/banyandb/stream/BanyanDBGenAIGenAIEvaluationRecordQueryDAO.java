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
import org.apache.skywalking.oap.server.core.analysis.manual.genai.GenAIEvaluationRecord;
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
import org.apache.skywalking.oap.server.library.util.StringUtil;
import org.apache.skywalking.oap.server.storage.plugin.banyandb.BanyanDBStorageClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@link org.apache.skywalking.oap.server.core.analysis.manual.log.LogRecord} is a gen-ai evaluation result
 */
public class BanyanDBGenAIGenAIEvaluationRecordQueryDAO extends AbstractBanyanDBDAO implements IGenAIEvaluationRecordQueryDAO {
    private static final Set<String> TAGS = ImmutableSet.of(
            GenAIEvaluationRecord.TRACE_ID,
            GenAIEvaluationRecord.SERVICE_ID,
            GenAIEvaluationRecord.SERVICE_INSTANCE_ID,
            GenAIEvaluationRecord.SEGMENT_ID,
            GenAIEvaluationRecord.SPAN_ID,
            GenAIEvaluationRecord.SPAN_TYPE,
            GenAIEvaluationRecord.TASK_NAME,
            GenAIEvaluationRecord.VALUE_TYPE,
            GenAIEvaluationRecord.VALUE,
            GenAIEvaluationRecord.REASON,
            GenAIEvaluationRecord.JUDGE_MODEL,
            GenAIEvaluationRecord.EVALUATION_TIME
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
        final QueryBuilder<StreamQuery> query = new QueryBuilder<>() {
            @Override
            public void apply(StreamQuery query) {
                if (StringUtil.isNotEmpty(serviceId)) {
                    query.and(eq(GenAIEvaluationRecord.SERVICE_ID, serviceId));
                }
                if (StringUtil.isNotEmpty(serviceInstanceId)) {
                    query.and(eq(GenAIEvaluationRecord.SERVICE_INSTANCE_ID, serviceInstanceId));
                }

                if (Objects.nonNull(relatedTrace)) {
                    if (StringUtil.isNotEmpty(relatedTrace.getTraceId())) {
                        query.and(eq(GenAIEvaluationRecord.TRACE_ID, relatedTrace.getTraceId()));
                    }
                    if (StringUtil.isNotEmpty(relatedTrace.getSegmentId())) {
                        query.and(eq(GenAIEvaluationRecord.SEGMENT_ID, relatedTrace.getSegmentId()));
                    }
                    if (Objects.nonNull(relatedTrace.getSpanId())) {
                        query.and(eq(GenAIEvaluationRecord.SPAN_ID, (long) relatedTrace.getSpanId()));
                    }
                }

                if (CollectionUtils.isNotEmpty(tags)) {
                    for (final Tag tag : tags) {
                        if (StringUtil.isNotEmpty(tag.getKey()) && StringUtil.isNotEmpty(tag.getValue())) {
                            query.and(eq(tag.getKey(), tag.getValue()));
                        }
                    }
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

        StreamQueryResponse resp = queryDebuggable(isColdStage, GenAIEvaluationRecord.INDEX_NAME, TAGS, getTimestampRange(duration), query);

        Logs logs = new Logs();

        for (final RowEntity rowEntity : resp.getElements()) {
            Log log = new Log();
            log.setTraceId(rowEntity.getTagValue(GenAIEvaluationRecord.TRACE_ID));
            log.setTimestamp(((Number) rowEntity.getTagValue(GenAIEvaluationRecord.EVALUATION_TIME)).longValue());
            log.setContentType(ContentType.TEXT);
            log.setContent(rowEntity.getTagValue(GenAIEvaluationRecord.VALUE));
            appendTag(log, GenAIEvaluationRecord.SERVICE_ID, rowEntity.getTagValue(GenAIEvaluationRecord.SERVICE_ID));
            appendTag(log, GenAIEvaluationRecord.SERVICE_INSTANCE_ID, rowEntity.getTagValue(GenAIEvaluationRecord.SERVICE_INSTANCE_ID));
            appendTag(log, GenAIEvaluationRecord.SEGMENT_ID, rowEntity.getTagValue(GenAIEvaluationRecord.SEGMENT_ID));
            appendTag(log, GenAIEvaluationRecord.SPAN_ID, rowEntity.getTagValue(GenAIEvaluationRecord.SPAN_ID));
            appendTag(log, GenAIEvaluationRecord.SPAN_TYPE, rowEntity.getTagValue(GenAIEvaluationRecord.SPAN_TYPE));
            appendTag(log, GenAIEvaluationRecord.TASK_NAME, rowEntity.getTagValue(GenAIEvaluationRecord.TASK_NAME));
            appendTag(log, GenAIEvaluationRecord.VALUE_TYPE, rowEntity.getTagValue(GenAIEvaluationRecord.VALUE_TYPE));
            appendTag(log, GenAIEvaluationRecord.REASON, rowEntity.getTagValue(GenAIEvaluationRecord.REASON));
            appendTag(log, GenAIEvaluationRecord.JUDGE_MODEL, rowEntity.getTagValue(GenAIEvaluationRecord.JUDGE_MODEL));
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
