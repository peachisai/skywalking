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

@Slf4j
public class DDSpanV4Decoder implements DDSpanDecoder {

    @Override
    public List<DDSpan> deserializeMsgPack(byte[] bytes) {

        try (MessageUnpacker unPacker = MessagePack.newDefaultUnpacker(bytes)) {
            //
            int spanSize = unPacker.unpackArrayHeader();
            if (spanSize < 0) {
                return null;
            }

            List<DDSpan> ddSpanList = new ArrayList<>(spanSize);
            for (int i = 0; i < spanSize; i++) {

            }

            return ddSpanList;
        } catch (Exception e) {
            return null;
        }
    }

    private DDSpan getDDSpan(MessageUnpacker unPacker, String[] dataArray) {
        try {
            int elementSize = unPacker.unpackArrayHeader();
            if (elementSize != 12) {
                throw new IllegalArgumentException(
                        "Wrong span element array size " + elementSize + ". Expected 12.");
            }

            DDSpan ddSpan = new DDSpan();
            ddSpan.setService(unpackString(unPacker, dataArray));
            ddSpan.setName(unpackString(unPacker, dataArray));
            ddSpan.setResource(unpackString(unPacker, dataArray));
            ddSpan.setTraceID(unPacker.unpackLong());
            ddSpan.setSpanID(unPacker.unpackLong());
            ddSpan.setParentID(unPacker.unpackLong());
            ddSpan.setStart(unPacker.unpackLong());
            ddSpan.setDuration(unPacker.unpackLong());
            ddSpan.setError(unPacker.unpackInt());
            ddSpan.setMeta(unpackStringMap(unPacker, dataArray));
            ddSpan.setMetrics(unpackNumberMap(unPacker, dataArray));
            ddSpan.setType(unpackString(unPacker, dataArray));

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

    private Map<String, String> unpackStringMap(MessageUnpacker unPacker, String[] dataArray) {
        try {
            int metaSize = unPacker.unpackMapHeader();
            if (metaSize < 0) {
                return null;
            }
            Map<String, String> meta = new HashMap<>(metaSize);
            for (int i = 0; i < metaSize; i++) {
                meta.put(unpackString(unPacker, dataArray), unpackString(unPacker, dataArray));
            }
            return meta;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Number> unpackNumberMap(MessageUnpacker unPacker, String[] dataArray) {
        try {
            int metricsSize = unPacker.unpackMapHeader();
            if (metricsSize < 0) {
                return null;
            }
            Map<String, Number> metrics = new HashMap<>(metricsSize);
            for (int i = 0; i < metricsSize; i++) {
                metrics.put(unpackString(unPacker, dataArray), unpackNumber(unPacker));
            }
            return metrics;
        } catch (Exception e) {
            return null;
        }
    }
}
