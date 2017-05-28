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

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A packet/flow operation associated with a header field and a value.
 */
public class FieldOperation {

    private static final Logger log = getLogger(FieldOperation.class);

    HeaderField   headerField;
    OperationType operationType;

    /**
     * Abstract value class that depends on the operation.
     */
    OperationValue operationValue = null;

    public FieldOperation() {
        this.headerField   = HeaderField.UNKNOWN;
        this.operationType = OperationType.NO_OPERATION;
    }

    public FieldOperation(
            HeaderField headerField,
            OperationType operationType,
            OperationValue operationValue) {
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        if (!ArrayUtils.contains(OperationType.values(), operationType)) {
            throw new IllegalArgumentException(String.valueOf(operationType));
        }

        checkNotNull(
            operationValue,
            "Field operation's value must not be NULL"
        );

        this.checkValidity(operationType, operationValue);

        this.headerField    = headerField;
        this.operationType  = operationType;
        this.operationValue = operationValue;
    }

    /**
     * Returns the header field of this field operation.
     *
     * @return header field
     */
    public HeaderField headerField() {
        return this.headerField;
    }

    /**
     * Sets the header field of this field operation.
     *
     * @param headerField header field
     */
    public void setHeaderField(HeaderField headerField) {
        this.headerField = headerField;
    }

    /**
     * Returns the operation type of this field operation.
     *
     * @return operation type
     */
    public OperationType operationType() {
        return this.operationType;
    }

    /**
     * Sets the operation type of this field operation.
     *
     * @param operationType operation type
     */
    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    /**
     * Returns the operation value of this field operation.
     *
     * @return operation value
     */
    public OperationValue operationValue() {
        return this.operationValue;
    }

    /**
     * Sets the operation value of this field operation.
     *
     * @param operationValue operation value
     */
    public void setOperationValue(OperationValue operationValue) {
        this.operationValue = operationValue;
    }

    /**
     * Returns whether this field operation is stateful or not.
     *
     * @return boolean statefulness
     */
    public boolean isStateful() {
        return OperationType.isStateful(this.operationType);
    }

    /**
     * Checks whether an operation's type and value agree.
     *
     * @param operationType operation type
     * @param operationValue operation value
     * @throws IllegalArgumentException if not
     */
    private void checkValidity(OperationType operationType, OperationValue operationValue) {
        if ((operationType == OperationType.TRANSLATE) ||
            (operationType == OperationType.MONITOR) ||
            (operationType == OperationType.WRITE_STATELESS)) {
            checkArgument(
                operationValue instanceof StatelessOperationValue,
                "A stateless operation requires stateless value."
            );
        } else if ((operationType == OperationType.WRITE_ROUNDROBIN) ||
                   (operationType == OperationType.WRITE_STATEFUL) ||
                   (operationType == OperationType.WRITE_RANDOM)) {
            checkArgument(
                operationValue instanceof StatefulOperationValue,
                "A stateful operation requires stateful value."
            );
        } else if (operationType == OperationType.WRITE_LB) {
            checkArgument(
                operationValue instanceof StatefulSetOperationValue,
                "A stateful set operation requires stateful set value."
            );
        } else if (operationType == OperationType.NO_OPERATION) {
            checkArgument(
                operationValue instanceof NoOperationValue,
                "A no operation type requires no value."
            );
        }
    }

    /**
     * Composes a field operation out of a similar one.
     *
     * @param other field operation
     * @throws IllegalArgumentException if the operation type is unsupported
     *         or the composition fails
     */
    public void compose(FieldOperation other) throws IllegalArgumentException {
        if (this.headerField != other.headerField) {
            log.warn("Trying to compose FieldOperation on different fields.");
        }

        /**
         * These operation types are not composable.
         * Monitors are kept in a different data structure, while
         * no-operations are transparent.
         */
        if ((other.operationType == OperationType.MONITOR) ||
            (other.operationType == OperationType.NO_OPERATION)) {
            throw new IllegalArgumentException(
                 "Only Write and Translate field operations are composable"
            );
        // Transparent is not strictly bound to a type, unlike the writes
        } else if (other.operationType != OperationType.TRANSLATE) {
            this.operationType = other.operationType;
        }

        this.changeValueType(other);

        return;
    }

    /**
     * Changes the operation value of this field operation,
     * according to the input field operation.
     *
     * @param other field operation
     */
    private void changeValueType(FieldOperation other) {
        if (other.operationType() == OperationType.WRITE_STATELESS) {
            StatelessOperationValue otherVal = (StatelessOperationValue) other.operationValue;
            this.operationValue = new StatelessOperationValue(otherVal);
        } else if ((other.operationType() == OperationType.WRITE_ROUNDROBIN) ||
                   (other.operationType() == OperationType.WRITE_STATEFUL) ||
                   (other.operationType() == OperationType.WRITE_RANDOM)) {
            StatefulOperationValue otherVal = (StatefulOperationValue) other.operationValue;
            this.operationValue = new StatefulOperationValue(otherVal);
        } else if (other.operationType() == OperationType.WRITE_LB) {
            StatefulSetOperationValue otherVal = (StatefulSetOperationValue) other.operationValue;
            this.operationValue = new StatefulSetOperationValue(otherVal);
        } else if (other.operationType() == OperationType.TRANSLATE) {
            StatelessOperationValue thisVal  = (StatelessOperationValue) this.operationValue;
            StatelessOperationValue otherVal = (StatelessOperationValue) other.operationValue;
            this.operationValue = new StatelessOperationValue(
                thisVal.statelessValue() + otherVal.statelessValue()
            );
        }

        return;
    }

    /**
     * Returns whether two field operations are of the same field or not.
     *
     * @param other field operation
     * @return boolean header field equality status
     */
    public boolean hasSameField(FieldOperation other) {
        return (this.headerField == other.headerField);
    }

    /**
     * Returns whether two field operations are of the same type or not.
     *
     * @param other field operation
     * @return boolean type equality status
     */
    public boolean hasSameType(FieldOperation other) {
        return (this.operationType == other.operationType);
    }

    /**
     * Returns whether two field operations have the same value or not.
     *
     * @param other field operation
     * @return boolean value equality status
     */
    public boolean hasSameValue(FieldOperation other) {
        boolean result = true;

        if ((this.operationType == OperationType.WRITE_STATELESS) ||
            (this.operationType == OperationType.TRANSLATE)) {

            StatelessOperationValue thisVal  = (StatelessOperationValue) this.operationValue;
            StatelessOperationValue otherVal = (StatelessOperationValue) other.operationValue;

            result = (result && thisVal.equals(otherVal));
        } else if ((this.operationType == OperationType.WRITE_ROUNDROBIN) ||
                   (this.operationType == OperationType.WRITE_RANDOM) ||
                   (this.operationType == OperationType.WRITE_STATEFUL)) {

            StatefulOperationValue thisVal  = (StatefulOperationValue) this.operationValue;
            StatefulOperationValue otherVal = (StatefulOperationValue) other.operationValue;

            result = (result && thisVal.equals(otherVal));
        } else if (this.operationType == OperationType.WRITE_LB) {

            StatefulSetOperationValue thisVal  = (StatefulSetOperationValue) this.operationValue;
            StatefulSetOperationValue otherVal = (StatefulSetOperationValue) other.operationValue;

            result = (result && thisVal.equals(otherVal));
        } else if (this.operationType == OperationType.NO_OPERATION) {
            // These are equal by construction
        }

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.headerField, this.operationType, this.operationValue
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof FieldOperation))) {
            return false;
        }

        FieldOperation other = (FieldOperation) obj;

        return this.hasSameField(other) &&
               this.hasSameType(other) &&
               this.hasSameValue(other);
    }

    @Override
    public String toString() {
        String result = this.headerField.toString();

        result += ": " + this.operationType.toString() + "(";

        // Stateless
        if ((this.operationType == OperationType.WRITE_STATELESS) ||
            (this.operationType == OperationType.TRANSLATE)) {
            // The stateless value of this field
            StatelessOperationValue stlVal = (StatelessOperationValue) this.operationValue;

            // IP addresses get special treatment
            if ((this.headerField == HeaderField.IP_SRC) ||
                (this.headerField == HeaderField.IP_DST)) {
                // Translate the value into an IP address
                IpAddress ip = IpAddress.valueOf((int) stlVal.statelessValue());

                result += ip.toString() + ")";
            } else {
                result += String.valueOf(stlVal.statelessValue()) + ")";
            }
        } else if ((this.operationType == OperationType.WRITE_ROUNDROBIN) ||
                   (this.operationType == OperationType.WRITE_RANDOM) ||
                   (this.operationType == OperationType.WRITE_STATEFUL)) {

            StatefulOperationValue stfVal = (StatefulOperationValue) this.operationValue;

            // IP addresses get special treatment
            if ((this.headerField == HeaderField.IP_SRC) ||
                (this.headerField == HeaderField.IP_DST)) {
                // Translate the values into IP addresses
                IpAddress ipStl = IpAddress.valueOf((int) stfVal.statelessValue());
                IpAddress ipStf = IpAddress.valueOf((int) stfVal.statefulValue());

                result += ipStl.toString() + ", " + ipStf.toString() + ")";
            } else {
                result += String.valueOf(stfVal.statelessValue()) + ", " +
                          String.valueOf(stfVal.statefulValue()) + ")";
            }
        } else if (this.operationType == OperationType.WRITE_LB) {

            StatefulSetOperationValue stfVal = (StatefulSetOperationValue) this.operationValue;

            for (Long intVal : stfVal.statefulSetValue()) {
                IpAddress ip = IpAddress.valueOf(intVal.intValue());
                result += ip.toString() + ", ";
            }

            // Cut the last space and comma
            result = result.substring(0, result.length() - 2);
        } else if ((this.operationType == OperationType.MONITOR) ||
                   (this.operationType == OperationType.NO_OPERATION)) {
            result += ")";
        }

        return result;
    }

}
