/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;

@Schema(allOf = { PolicyTO.class },
        subTypes = { InboundPolicyTO.class, PushPolicyTO.class },
        discriminatorProperty = "_class")
public abstract class ProvisioningPolicyTO extends PolicyTO {

    private static final long serialVersionUID = -3786048942148269602L;

    private ConflictResolutionAction conflictResolutionAction;

    private final Map<String, String> correlationRules = new HashMap<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.policy.ProvisioningPolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public ConflictResolutionAction getConflictResolutionAction() {
        return Optional.ofNullable(conflictResolutionAction).orElse(ConflictResolutionAction.IGNORE);
    }

    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    public Map<String, String> getCorrelationRules() {
        return correlationRules;
    }
}
