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

import java.util.List;
import java.util.Map;

@Data
public class DDSpan {
    // service is the name of the service with which this span is associated.
    private String service;
    // name is the operation name of this span.
    private String name;
    // resource is the resource name of this span, also sometimes called the endpoint (for web spans).
    private String resource;
    // traceID is the ID of the trace to which this span belongs.
    private long traceID;
    // spanID is the ID of this span.
    private long spanID;
    // parentID is the ID of this span's parent, or zero if this span has no parent.
    private long parentID;
    // start is the number of nanoseconds between the Unix epoch and the beginning of this span.
    private long start;
    // duration is the time length of this span in nanoseconds.
    private long duration;
    // error is 1 if there is an error associated with this span, or 0 if there is not.
    private int error;
    // meta is a mapping from tag name to tag value for string-valued tags.
    private Map<String, String> meta;
    // metrics is a mapping from tag name to tag value for numeric-valued tags.
    private Map<String, Double> metrics;
    // type is the type of the service with which this span is associated. Example values: web, db, lambda.
    private String type;
    // meta_struct is a registry of structured "other" data used by, e.g., AppSec.
    private Map<String, byte[]> metaStruct;
    // span_links represents a collection of links, where each link defines a causal relationship between two spans.
    private List<DDSpanLink> spanLinks;
}
