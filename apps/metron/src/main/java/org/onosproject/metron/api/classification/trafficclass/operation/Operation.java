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

import org.onosproject.metron.api.classification.trafficclass.HeaderField;

import org.onlab.packet.IpAddress;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A traffic class operation associated with
 * a set of header fields and values.
 */
public class Operation {

    private static final Logger log = getLogger(Operation.class);

    /**
     * A composite operation that contains multiple header fields,
     * mapped to different operation types and values.
     * This is the key logic of write operation synthesis.
     */
    private Map<HeaderField, FieldOperation> operationMap;

    // Keeps the list of monitors that this class goes through.
    private List<StatelessOperationValue> monitors;

    // Flag that charactrizes the state of this operation.
    private boolean isStateful = false;

    public Operation() {
        this.operationMap = new ConcurrentHashMap<HeaderField, FieldOperation>();
        this.monitors = new ArrayList<StatelessOperationValue>();
    }

    public Operation(FieldOperation fieldOperation) {
        checkNotNull(
            fieldOperation,
            "Cannot construct Operation out of a NULL FieldOperation"
        );

        this.operationMap = new ConcurrentHashMap<HeaderField, FieldOperation>();
        this.monitors = new ArrayList<StatelessOperationValue>();

        this.addFieldOperation(fieldOperation);
    }

    /**
     * Constructs an operation out of another one.
     *
     * @param other operation to be copied to this object
     */
    public Operation(Operation other) {
        checkNotNull(
            other,
            "Cannot construct Operation out of a NULL one"
        );

        // TODO: Iterate and copy
        this.operationMap = new ConcurrentHashMap<HeaderField, FieldOperation>(other.operationMap);
        this.monitors = new ArrayList<StatelessOperationValue>(other.monitors);

        this.compose(other);
    }

    /**
     * Returns the operation map of this operation.
     *
     * @return operation map
     */
    public Map<HeaderField, FieldOperation> operationMap() {
        return this.operationMap;
    }

    /**
     * Returns the monitors' list of this operation.
     *
     * @return list of monitors
     */
    public List<StatelessOperationValue> monitors() {
        return this.monitors;
    }

    /**
     * Returns whether the operation map contains
     * an operation for the given header field.
     *
     * @param headerField header field
     * @return boolean status
     */
    public boolean hasFieldOperation(HeaderField headerField) {
        return this.operationMap.containsKey(headerField);
    }

    /**
     * Returns whether the operations' map is empty or not.
     *
     * @return boolean status of the operations' map
     */
    public boolean hasOperations() {
        return !this.operationMap.isEmpty();
    }

    /**
     * Returns whether the monitors' list is empty or not.
     *
     * @return boolean status of the monitors' list
     */
    public boolean hasMonitors() {
        return !this.monitors.isEmpty();
    }

    /**
     * Returns whether the operation is empty.
     *
     * @return boolean status of the operation
     */
    public boolean isEmpty() {
        return !this.hasOperations() && !this.hasMonitors();
    }

    /**
     * Returns whether this operation is stateful or not.
     *
     * @return boolean statefulness status
     */
    public boolean isStateful() {
        if (this.isEmpty()) {
            return false;
        }

        return this.isStateful;
    }


    /**
     * Returns the header field operation of the given header field.
     *
     * @param headerField header field
     * @return header field operation
     * @throws IllegalArgumentException if the header field is invalid
     */
    public FieldOperation fieldOperation(HeaderField headerField) {
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        // Nothing in the map
        if (!this.hasFieldOperation(headerField)) {
            return null;
        }

        return this.operationMap.get(headerField);
    }

    /**
     * Adds a header field operation to the map.
     * |-> If this is the first occurence of this header field,
     *     we just add the new operation.
     * |-> Otherwise, the existing operation gets composed.
     *
     * @param fieldOperation header field operation
     * @throws IllegalArgumentException if the addition fails
     */
    public void addFieldOperation(FieldOperation fieldOperation)
            throws IllegalArgumentException {
        checkNotNull(
            fieldOperation,
            "Cannot add NULL field operation"
        );

        OperationType newOperationType = fieldOperation.operationType();

        if (newOperationType == OperationType.MONITOR) {
            StatelessOperationValue val = (StatelessOperationValue)
                fieldOperation.operationValue();
            this.monitors.add(val);
            return;
        } else if (newOperationType == OperationType.NO_OPERATION) {
            return;
        } else if (OperationType.isStateful(newOperationType)) {
            this.isStateful = true;
        }

        // All write and translate operations are handled below
        HeaderField newHeaderField = fieldOperation.headerField();

        // This is the first operation on this field
        if (!this.hasFieldOperation(newHeaderField)) {
            this.operationMap.put(newHeaderField, fieldOperation);
            return;
        }

        // If not, compose!
        FieldOperation thisOp = this.fieldOperation(newHeaderField);
        try {
            thisOp.compose(fieldOperation);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        this.operationMap.put(newHeaderField, thisOp);

        return;
    }

    /**
     * Composes a composite operation out of another one.
     *
     * @param other operation
     * @throws IllegalArgumentException if the composition fails
     */
    public void compose(Operation other) throws IllegalArgumentException {
        checkNotNull(
            other,
            "Cannot compose operation using another NULL operation"
        );

        // Parse the other operation add add its components to this one
        for (Map.Entry<HeaderField, FieldOperation> entry :
                other.operationMap.entrySet()) {
            FieldOperation otherOp = entry.getValue();

            try {
                this.addFieldOperation(otherOp);
            } catch (IllegalArgumentException iaEx) {
                throw iaEx;
            }
        }

        return;
    }

    /**
     * Translates a composite operation to an IPRewriter configuration.
     *
     * @return textual IPRewriter configuration
     * @throws IllegalArgumentException if the translation fails
     */
    public String toIPRewriterConfig() {
        String ipSrc = "";
        String ipDst = "";
        String tpSrc = "";
        String tpDst = "";

        // IP source
        HeaderField ipSrcfield = HeaderField.IP_SRC;

        if (this.hasFieldOperation(ipSrcfield)) {
            FieldOperation ipSrcOp = this.fieldOperation(ipSrcfield);
            if (ipSrcOp.operationType() == OperationType.WRITE_STATELESS) {
                StatelessOperationValue stlVal = (StatelessOperationValue)
                    ipSrcOp.operationValue();
                IpAddress ip = IpAddress.valueOf((int) stlVal.statelessValue());
                ipSrc = ip.toString();
            } else {
                throw new IllegalArgumentException(
                    "Unexpected write operation on " + ipSrcfield.toString()
                );
            }
        } else {
            ipSrc = "-";
        }

        // Source port
        tpSrc = this.getTransportPortValue(HeaderField.TP_SRC_PORT);

        // Destination port
        tpDst = this.getTransportPortValue(HeaderField.TP_DST_PORT);

        // Destination IP
        HeaderField ipDstfield = HeaderField.IP_DST;

        if (this.hasFieldOperation(ipDstfield)) {
            FieldOperation ipDstOp = this.fieldOperation(ipDstfield);

            if (ipDstOp.operationType() == OperationType.WRITE_STATELESS) {
                StatelessOperationValue stlVal = (StatelessOperationValue)
                    ipDstOp.operationValue();
                IpAddress ip = IpAddress.valueOf((int) stlVal.statelessValue());

                ipDst = ip.toString();
            } else if (ipDstOp.operationType() == OperationType.WRITE_LB) {
                StatefulSetOperationValue stfVal = (StatefulSetOperationValue)
                    ipDstOp.operationValue();

                ipDst = "RoundRobinIPMapper(";
                for (Long lbVal : stfVal.statefulSetValue()) {
                    IpAddress ip = IpAddress.valueOf(lbVal.intValue());
                    ipDst += ip.toString() + ", ";
                }

                // Cut the last space and comma
                ipDst = ipDst.substring(0, ipDst.length() - 2);
                ipDst += ")";
            } else {
                throw new IllegalArgumentException(
                    "Unexpected write operation on " + ipDstfield.toString()
                );
            }
        } else {
            ipDst = "-";
        }

        if (ipSrc.equals("-") && tpSrc.equals("-") &&
            ipDst.equals("-") && tpDst.equals("-")) {
            return "";
        }

        // TODO: Fix this
        return "pattern " + ipSrc + " " + tpSrc + " " + ipDst + " " + tpDst + " 0 0";
    }

    /**
     * Get the value of the transport header field of this operation.
     *
     * @param portField the header field type (src or dst)
     * @return the value of this header field is returned
     */
    private String getTransportPortValue(HeaderField portField) {
        if ((portField != HeaderField.TP_SRC_PORT) && (portField != HeaderField.TP_DST_PORT)) {
            throw new IllegalArgumentException(
                "Transport port (src or dst) header field was expected"
            );
        }

        // Default configuration
        String tpPort = "-";

        if (this.hasFieldOperation(portField)) {
            FieldOperation portOp = this.fieldOperation(portField);

            if (portOp.operationType() == OperationType.WRITE_STATELESS) {
                StatelessOperationValue stlVal = (StatelessOperationValue)
                    portOp.operationValue();

                tpPort = String.valueOf(stlVal.statelessValue());
            } else if (portOp.operationType() == OperationType.WRITE_STATEFUL) {
                StatefulOperationValue stfVal = (StatefulOperationValue)
                    portOp.operationValue();

                tpPort = String.valueOf(stfVal.statelessValue()) + "-" +
                         String.valueOf(stfVal.statefulValue());
            } else if (portOp.operationType() == OperationType.WRITE_ROUNDROBIN) {
                StatefulOperationValue stfVal = (StatefulOperationValue)
                    portOp.operationValue();

                tpPort = String.valueOf(stfVal.statelessValue()) + "-" +
                         String.valueOf(stfVal.statefulValue()) + "#";
            } else if (portOp.operationType() == OperationType.WRITE_RANDOM) {
                StatefulOperationValue stfVal = (StatefulOperationValue)
                    portOp.operationValue();

                tpPort = String.valueOf(stfVal.statelessValue()) + "-" +
                         String.valueOf(stfVal.statefulValue()) + "?";
            } else {
                throw new IllegalArgumentException(
                    "Unexpected write operation on " + portField.toString()
                );
            }
        }

        return tpPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.operationMap, this.monitors);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof Operation))) {
            return false;
        }

        Operation other = (Operation) obj;

        Map<HeaderField, FieldOperation> outerMap = null;
        Map<HeaderField, FieldOperation> innerMap = null;
        if (this.operationMap.size() >= other.operationMap.size()) {
            innerMap = other.operationMap;
            outerMap = this.operationMap;
        } else {
            innerMap = this.operationMap;
            outerMap = other.operationMap;
        }

        for (Map.Entry<HeaderField, FieldOperation> entry : outerMap.entrySet()) {
            HeaderField outerField = entry.getKey();

            if (!innerMap.containsKey(outerField)) {
                return false;
            }

            FieldOperation outerOp = entry.getValue();
            FieldOperation innerOp = innerMap.get(outerField);

            if (innerOp == null) {
                return false;
            }

            if (!outerOp.equals(innerOp)) {
                return false;
            }
        }

        if (!this.monitors().equals(other.monitors())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        String result = "Operation: \n";

        for (Map.Entry<HeaderField, FieldOperation> entry :
                this.operationMap.entrySet()) {
            FieldOperation thisOp = entry.getValue();

            result += "\t" + thisOp.toString() + "\n";
        }

        return result;
    }

}
