/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.metron.api.net;

/**
 * Action of a Click flow rule.
 */
public enum ClickFlowRuleAction {

    /**
     * The packet should be sent out on a port.
     */
    ALLOW("allow"),
    /**
     * The packet will be dropped.
     */
    DENY("deny"),
    DROP("drop");

    private String clickActionType;

    private ClickFlowRuleAction(String clickActionType) {
        this.clickActionType = clickActionType.toLowerCase();
    }

    @Override
    public String toString() {
        return clickActionType;
    }

}