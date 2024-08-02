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

package org.apache.skywalking.oap.server.receiver.datadog.provider.entity;

import lombok.Data;

import java.util.Map;

@Data
public class DDSpanLink {

    private long traceID;

    // TraceIDHigh is optional. The high 64 bits of a referenced trace id.
    private long traceIDHigh;

    private long spanID;

    // Attributes is optional. Simple mapping of keys to string values.
    private Map<String, String> attributes;

    // Tracestate is optional. W3C tracestate.
    private String tracestate;

    // Flags is optional. W3C trace flags. If set, the high bit (bit 31) must be set.
    private int flags;
}
