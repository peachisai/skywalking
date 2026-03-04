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

package org.apache.skywalking.oap.meter.analyzer.source;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.analysis.Layer;
import org.apache.skywalking.oap.server.core.source.ScopeDeclaration;
import org.apache.skywalking.oap.server.core.source.ScopeDefaultColumn;
import org.apache.skywalking.oap.server.core.source.Source;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.GEN_AI_MODEL_ACCESS;
import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.SERVICE_INSTANCE_CATALOG_NAME;

@ScopeDeclaration(id = GEN_AI_MODEL_ACCESS, name = "GenAIModelAccess ", catalog = SERVICE_INSTANCE_CATALOG_NAME)
@ScopeDefaultColumn.VirtualColumnDefinition(fieldName = "entityId", columnName = "entity_id", isID = true, type = String.class)
public class GenAIModelAccess extends Source {

    @Override
    public int scope() {
        return GEN_AI_MODEL_ACCESS;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }

    private String entityId;

    @Getter
    @Setter
    @ScopeDefaultColumn.DefinedByField(columnName = "service_id")
    private String serviceId;

    @Getter
    @Setter
    @ScopeDefaultColumn.DefinedByField(columnName = "provider")
    private String provider;

    @Getter
    @Setter
    @ScopeDefaultColumn.DefinedByField(columnName = "model_name")
    private String modelName;

    @Getter
    @Setter
    private long tokenUsage;

    @Getter
    @Setter
    private long timeToFirstToken;

    @Getter
    @Setter
    private long latency;

    @Override
    public void prepare() {
        serviceId = IDManager.ServiceID.buildId(provider, Layer.VIRTUAL_GENAI.isNormal());
        entityId = IDManager.ServiceInstanceID.buildId(serviceId, modelName);
    }
}
