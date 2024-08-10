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

package org.apache.skywalking.oap.server.receiver.datadog.provider.decoder;

import org.apache.skywalking.oap.server.receiver.datadog.provider.constants.DDSpanVersion;
import org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.impl.DDSpanV4Decoder;
import org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.impl.DDSpanV5Decoder;

import java.util.EnumMap;
import java.util.Map;

public class DDSpanDecoderFactory {

    private static final Map<DDSpanVersion, DDSpanDecoder> DECODER_MAP = new EnumMap<>(DDSpanVersion.class);

    static {
        DECODER_MAP.put(DDSpanVersion.V4, new DDSpanV4Decoder());
        DECODER_MAP.put(DDSpanVersion.V5, new DDSpanV5Decoder());
    }

    public static DDSpanDecoder getDecoder(String uri) {
        DDSpanVersion ddSpanVersion = DDSpanVersion.getVersion(uri);
        if (ddSpanVersion == null) {
            return null;
        }
        return DECODER_MAP.get(ddSpanVersion);
    }

    private DDSpanDecoderFactory() {
    }
}
