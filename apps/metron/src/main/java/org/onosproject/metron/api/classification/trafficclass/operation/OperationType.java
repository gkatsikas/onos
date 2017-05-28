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

package org.onosproject.metron.api.classification.trafficclass.operation;

/**
 * Encodes the packet/flow operation type of a traffic class.
 */
public enum OperationType {

    /**
     * Transparent operation for monitoring purposes.
     */
    MONITOR("Monitor"),
    /**
     * Translation.
     */
    TRANSLATE("Translate"),
    /**
     * Stateless write operation.
     */
    WRITE_STATELESS("WriteStateless"),
    /**
     * Round-robin value selection.
     */
    WRITE_ROUNDROBIN("WriteRoundRobin"),
    /**
     * Random value selection.
     */
    WRITE_RANDOM("WriteRandom"),
    /**
     * Write from stateful map.
     */
    WRITE_STATEFUL("WriteStateful"),
    /**
     * Manual fix for load-balancing.
     * TODO: switch FieldOperation.m_value to SegmentList object.
     */
    WRITE_LB("WriteLB"),
    /**
     * No operation is applied.
     */
    NO_OPERATION("NoOperation");

    private String operationType;

    private OperationType(String operationType) {
        this.operationType = operationType;
    }

    public static boolean isStateful(OperationType op) {
        return (op == WRITE_ROUNDROBIN) ||
               (op == WRITE_RANDOM)     ||
               (op == WRITE_STATEFUL)   ||
               (op == WRITE_LB);
    }

    @Override
    public String toString() {
        return operationType;
    }

}
