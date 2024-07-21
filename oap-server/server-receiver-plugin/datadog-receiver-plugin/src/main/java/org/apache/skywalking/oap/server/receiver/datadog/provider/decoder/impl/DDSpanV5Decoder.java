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

package org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.DDSpanDecoder;
import org.apache.skywalking.oap.server.receiver.datadog.provider.entity.DDSpan;
import org.msgpack.core.MessageIntegerOverflowException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class DDSpanV5Decoder implements DDSpanDecoder {

    @Override
    public List<DDSpan> deserializeMsgPack(byte[] bytes) {

        try (MessageUnpacker unPacker = MessagePack.newDefaultUnpacker(bytes)) {
            //
            int headerFlag = unPacker.unpackArrayHeader();
            if (headerFlag != 2) {
                return null;
            }

            int size = unPacker.unpackArrayHeader();
            if (size < 0) {

            }
            String[] spanArray = new String[size];
            for (int i = 0; i < size; i++) {
                spanArray[i] = unPacker.unpackString();
            }
            unPacker.unpackArrayHeader();
            int spanSize = unPacker.unpackArrayHeader();
            List<DDSpan> ddSpanList = new ArrayList<>();
            for (int i = 0; i < spanSize; i++) {
                DDSpan ddSpan = getDDSpan(unPacker, spanArray);
                if (!Objects.isNull(ddSpan)) {
                    ddSpanList.add(ddSpan);
                }
            }
            return ddSpanList;
        } catch (Exception e) {
            return null;
        }
    }

    private DDSpan getDDSpan(MessageUnpacker unPacker, String[] spanArray) {
        try {
            int elementSize = unPacker.unpackArrayHeader();
            if (elementSize != 12) {
                throw new IllegalArgumentException(
                        "Wrong span element array size " + elementSize + ". Expected 12.");
            }

            DDSpan ddSpan = new DDSpan();
            ddSpan.setService(unpackString(unPacker, spanArray));
            ddSpan.setName(unpackString(unPacker, spanArray));
            ddSpan.setResource(unpackString(unPacker, spanArray));
            ddSpan.setTraceID(unPacker.unpackLong());
            ddSpan.setSpanID(unPacker.unpackLong());
            ddSpan.setParentID(unPacker.unpackLong());
            ddSpan.setStart(unPacker.unpackLong());
            ddSpan.setDuration(unPacker.unpackLong());
            ddSpan.setError(unPacker.unpackInt());
            ddSpan.setMeta(unpackStringMap(unPacker, spanArray));
            ddSpan.setMetrics(unpackNumberMap(unPacker, spanArray));
            ddSpan.setType(unpackString(unPacker, spanArray));

            return ddSpan;
        } catch (Throwable t) {
            return null;
        }
    }

    private String unpackString(MessageUnpacker unPacker, String[] dataArray)
            throws IOException {
        return dataArray[unPacker.unpackInt()];
    }

    private Number unpackNumber(MessageUnpacker unPacker) {
        Number result = null;
        try {
            ValueType valueType = unPacker.getNextFormat().getValueType();
            switch (valueType) {
                case INTEGER:
                    try {
                        result = unPacker.unpackInt();
                    } catch (MessageIntegerOverflowException e) {
                        result = unPacker.unpackLong();
                    }
                    break;
                case FLOAT:
                    result = unPacker.unpackDouble();
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Failed to decode number. Unexpected value type " + valueType);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode number.", e);
        }
        return result;
    }

    private Map<String, String> unpackStringMap(MessageUnpacker unPacker, String[] spanArray) {
        try {
            int metaSize = unPacker.unpackMapHeader();
            if (metaSize < 0) {
                return null;
            }
            Map<String, String> meta = new HashMap<>(metaSize);
            for (int i = 0; i < metaSize; i++) {
                meta.put(unpackString(unPacker, spanArray), unpackString(unPacker, spanArray));
            }
            return meta;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Number> unpackNumberMap(MessageUnpacker unPacker, String[] spanArray) {
        try {
            int metricsSize = unPacker.unpackMapHeader();
            if (metricsSize < 0) {
                return null;
            }
            Map<String, Number> metrics = new HashMap<>(metricsSize);
            for (int i = 0; i < metricsSize; i++) {
                metrics.put(unpackString(unPacker, spanArray), unpackNumber(unPacker));
            }
            return metrics;
        } catch (Exception e) {
            return null;
        }
    }
}
