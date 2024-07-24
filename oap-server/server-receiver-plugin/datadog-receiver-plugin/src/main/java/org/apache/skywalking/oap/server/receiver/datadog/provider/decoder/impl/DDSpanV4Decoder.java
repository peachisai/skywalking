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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.DDSpanDecoder;
import org.apache.skywalking.oap.server.receiver.datadog.provider.entity.DDSpan;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DDSpanV4Decoder implements DDSpanDecoder {

    @Override
    public List<DDSpan> deserializeMsgPack(byte[] bytes) {
        try (MessageUnpacker messageUnpacker = MessagePack.newDefaultUnpacker(bytes)) {
            ImmutableValue immutableValue = messageUnpacker.unpackValue();
            String json = immutableValue.toJson();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
            CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DDSpan.class));
            List<List<DDSpan>> list = objectMapper.readValue(json, collectionType);
            return list.stream().flatMap(List::stream).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}