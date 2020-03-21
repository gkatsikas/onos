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

import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashSet;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for Operation.
 */
public class OperationTest {

    private final Logger log = getLogger(getClass());

    private Operation operationA;
    private Operation operationB;
    private Operation operationC;

    private HeaderField    headerField;
    private OperationType  operationType;
    private OperationValue operationValue;
    private FieldOperation fieldOperation;

    /**
     * Method to set up the test environment.
     */
    @Before
    public void setUp() {
        operationA = new Operation();
        operationB = new Operation();
        operationC = new Operation();
    }

    /**
     * Checks whether various valid operations can be
     * successfully constructed.
     *
     * @throws IllegalArgumentException if any of the valid
     *         operations does not build properly
     */
    @Test
    public void differentOperations() throws IllegalArgumentException {
        /**
         * Stateless Write
         */
        headerField    = HeaderField.IP_PROTO;
        operationType  = OperationType.WRITE_STATELESS;
        operationValue = new StatelessOperationValue(6);

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Operation A is empty, it should not contain anything
        assertThat(operationA.fieldOperation(headerField), nullValue());

        // Check also that the hasFieldOperation works
        assertFalse(operationA.hasFieldOperation(headerField));

        // Now we insert this field operation
        operationA.addFieldOperation(fieldOperation);

        // Let's check that it is there
        assertThat(operationA.fieldOperation(headerField), notNullValue());

        assertThat(operationA.toString(), notNullValue());

        /**
         * Stateful Write
         */
        headerField    = HeaderField.IP_SRC;
        operationType  = OperationType.WRITE_STATEFUL;
        operationValue = new StatefulOperationValue(2000, 10000);

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Operation B is empty, it should not contain anything
        assertThat(operationB.fieldOperation(headerField), nullValue());

        // Check also that the hasFieldOperation works
        assertFalse(operationB.hasFieldOperation(headerField));

        // Now we insert this field operation
        operationB.addFieldOperation(fieldOperation);

        // Let's check that it is there
        assertThat(operationB.fieldOperation(headerField), notNullValue());

        // Let's also verify that Operations A and B are not equal
        assertFalse(operationA.equals(operationB));

        assertThat(operationB.toString(), notNullValue());


        /**
         * Stateful Set Write
         */
        headerField    = HeaderField.IP_DST;
        operationType  = OperationType.WRITE_LB;
        operationValue = new StatefulSetOperationValue(
            new HashSet<Long>(
                Arrays.asList(
                    (long) 0, (long) 10000, (long) Constants.MAX_UINT
                )
            )
        );

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Operation C is empty, it should not contain anything
        assertThat(operationC.fieldOperation(headerField), nullValue());

        // Check also that the hasFieldOperation works
        assertFalse(operationC.hasFieldOperation(headerField));

        // Now we insert this field operation
        operationC.addFieldOperation(fieldOperation);

        // Let's check that it is there
        assertThat(operationC.fieldOperation(headerField), notNullValue());

        // Let's also verify that Operations C and B/A are not equal
        assertFalse(operationC.equals(operationB));
        assertFalse(operationC.equals(operationA));

        assertThat(operationB.toString(), notNullValue());
    }

    /**
     * Stress the operations' composition.
     *
     * @throws IllegalArgumentException if any of the
     *         compositions results in malformed operations
     */
    @Test
    public void composition() throws IllegalArgumentException {
        /**
         * Stateless Write
         */
        headerField    = HeaderField.IP_PROTO;
        operationType  = OperationType.WRITE_STATELESS;
        operationValue = new StatelessOperationValue(6);

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Now we insert this field operation
        operationA.addFieldOperation(fieldOperation);

        /**
         * Stateful Write
         */
        headerField    = HeaderField.IP_SRC;
        operationType  = OperationType.WRITE_STATEFUL;
        operationValue = new StatefulOperationValue(2, 10000);

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Now we insert this field operation
        operationB.addFieldOperation(fieldOperation);

        /**
         * Stateful Set Write
         */
        headerField    = HeaderField.IP_DST;
        operationType  = OperationType.WRITE_LB;
        operationValue = new StatefulSetOperationValue(
            new HashSet<Long>(
                Arrays.asList(
                    (long) 0, (long) 10000, (long) Constants.MAX_UINT
                )
            )
        );

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Now we insert this field operation
        operationC.addFieldOperation(fieldOperation);

        // Keep a copy of the current operations
        Operation prevOpA = new Operation(operationA);
        Operation prevOpB = new Operation(operationB);
        Operation prevOpC = new Operation(operationC);

        /**
         * Compositions
         */
        try {
            // Compose A such that it includes B
            operationA.compose(operationB);
        } catch (IllegalArgumentException iaEx) {
            log.error("Exception caught during compose");
            throw iaEx;
        }

        // The new operation A now contains both A and B
        assertFalse(operationA.equals(prevOpA));
        assertFalse(operationA.equals(prevOpB));


        try {
            // Compose B from B (identity)
            operationB.compose(operationB);
        } catch (IllegalArgumentException iaEx) {
            log.error("Exception caught during compose");
            throw iaEx;
        }

        // The new operation B has to be equal to the old one
        assertTrue(operationB.equals(prevOpB));


        try {
            // Compose C from B (identity)
            operationC.compose(operationB);
        } catch (IllegalArgumentException iaEx) {
            log.error("Exception caught during compose");
            throw iaEx;
        }

        // The new operation C now contains both C and B
        assertFalse(operationC.equals(prevOpC));
        assertFalse(operationC.equals(prevOpB));
    }

    /**
     * Checks whether valid but transparent operations can be
     * successfully constructed.
     *
     * @throws IllegalArgumentException if any of the valid
     *         operations does not build properly
     */
    @Test
    public void transparentOperations() throws Exception {
        headerField    = HeaderField.IP_PROTO;
        operationType  = OperationType.MONITOR;
        operationValue = new StatelessOperationValue(0);

        try {
            fieldOperation = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // An empty operation should not contain anything
        assertThat(operationA.fieldOperation(headerField), nullValue());

        // Check also that the hasFieldOperation works
        assertFalse(operationA.hasFieldOperation(headerField));

        /**
         * Now we inserted this field operation.
         * Howerver, this is a monitor operation, so the operation map does not get anything..
         */
        operationA.addFieldOperation(fieldOperation);

        // Let's verify that the map does not have an entry
        assertThat(operationA.fieldOperation(headerField), nullValue());
    }

}
