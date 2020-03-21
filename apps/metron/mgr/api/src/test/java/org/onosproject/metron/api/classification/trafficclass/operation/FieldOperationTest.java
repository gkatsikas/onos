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

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for FieldOperation.
 */
public class FieldOperationTest {

    private final Logger log = getLogger(getClass());

    private FieldOperation defaultFieldOperation;
    private FieldOperation fieldOperationA;
    private FieldOperation fieldOperationB;

    private HeaderField    headerField;
    private OperationType  operationType;
    private OperationValue operationValue;

    /**
     * Method to set up the test environment.
     */
    @Before
    public void setUp() {
        defaultFieldOperation = new FieldOperation();
    }

    /**
     * Checks whether valid field operations can be
     * successfully constructed.
     *
     * @throws IllegalArgumentException if any of the valid
     *         field operations does not build properly
     */
    @Test
    public void validFieldOperations() throws IllegalArgumentException {

        assertEquals(defaultFieldOperation, new FieldOperation());

        // Monitor operation with valid stateless value
        headerField    = HeaderField.IP_PROTO;
        operationType  = OperationType.MONITOR;
        operationValue = new StatelessOperationValue(0);

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }


        // Stateless operation with valid stateless value
        headerField    = HeaderField.UDP_CHS;
        operationType  = OperationType.WRITE_STATELESS;
        operationValue = new StatelessOperationValue(1000);

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }


        // Stateful operation with valid stateful values
        headerField    = HeaderField.IP_DSCP;
        operationType  = OperationType.WRITE_STATELESS;
        operationValue = new StatefulOperationValue(5, 8);

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }


        // Stateful set operation with valid stateful set value
        headerField    = HeaderField.IP_DSCP;
        operationType  = OperationType.WRITE_STATELESS;
        operationValue = new StatefulSetOperationValue(
            new HashSet<Long>(
                Arrays.asList(
                    (long) 2467, (long) 48345, (long) 65565
                )
            )
        );

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                operationValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }
    }

    /**
     * Checks whether the composition of stateless field
     * operator works or not.
     *
     * @throws Exception if the composition does not succeed
     * @throws IllegalArgumentException if the composition/comparison throw exception
     */
    @Test
    public void validStatelessComposition() throws Exception, IllegalArgumentException {
        headerField   = HeaderField.IP_PROTO;
        operationType = OperationType.WRITE_STATELESS;
        StatelessOperationValue opValue = new StatelessOperationValue(0);

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        headerField   = HeaderField.IP_PROTO;
        operationType = OperationType.WRITE_STATELESS;
        opValue = new StatelessOperationValue(5);

        try {
            fieldOperationB = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // At this point the two operations must have different values
        if (fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that two field operations with different values are not the same"
            );
        }

        // Compose B from A
        try {
            fieldOperationB.compose(fieldOperationA);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Verify that the individual components of the two operations agree
        OperationType opA = fieldOperationA.operationType();
        OperationType opB = fieldOperationB.operationType();

        assertEquals(opA, opB);

        StatelessOperationValue valA = (StatelessOperationValue) fieldOperationA.operationValue();
        StatelessOperationValue valB = (StatelessOperationValue) fieldOperationB.operationValue();

        assertEquals(
            valA.statelessValue(),
            valB.statelessValue()
        );

        /**
         * After the composition, the two field operations must have the same value,
         * unless we have a translate operation (not tested here).
         */
        if (!fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that a composed field operation does not agree with the composer"
            );
        }
    }

    /**
     * Checks whether the composition of stateful field
     * operator works or not.
     *
     * @throws Exception if the composition does not succeed
     * @throws IllegalArgumentException if the composition/comparison throw exception
     */
    @Test
    public void validStatefulComposition() throws Exception, IllegalArgumentException {
        headerField   = HeaderField.IP_PROTO;
        operationType = OperationType.WRITE_STATEFUL;
        StatefulOperationValue opValue = new StatefulOperationValue(0, Constants.MAX_UINT);

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        headerField   = HeaderField.IP_PROTO;
        operationType = OperationType.WRITE_ROUNDROBIN;
        opValue = new StatefulOperationValue(48, 54099);

        try {
            fieldOperationB = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // At this point the two operations must have different values
        if (fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that two field operations with different values are not the same"
            );
        }

        // Compose B from A
        try {
            fieldOperationB.compose(fieldOperationA);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Verify that the individual components of the two operations agree
        OperationType opA = fieldOperationA.operationType();
        OperationType opB = fieldOperationB.operationType();

        assertEquals(opA, opB);

        StatefulOperationValue valA = (StatefulOperationValue)
            fieldOperationA.operationValue();
        StatefulOperationValue valB = (StatefulOperationValue)
            fieldOperationB.operationValue();

        assertEquals(
            valA.statefulValue(),
            valB.statefulValue()
        );

        /**
         * After the composition, the two field operations must have the same value,
         * unless we have a translate operation (not tested here).
         */
        if (!fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that a composed field operation does not agree with the composer"
            );
        }
    }

    /**
     * Checks whether the composition of stateful set
     * field operator works or not.
     *
     * @throws Exception if the composition does not succeed
     * @throws IllegalArgumentException if the composition/comparison throw exception
     */
    @Test
    public void validStatefulSetComposition() throws Exception, IllegalArgumentException {
        headerField   = HeaderField.IP_SRC;
        operationType = OperationType.WRITE_LB;
        StatefulSetOperationValue opValue = new StatefulSetOperationValue(
            new HashSet<Long>(
                Arrays.asList(
                    (long) 67, (long) 345, (long) 655
                )
            )
        );

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        headerField   = HeaderField.IP_SRC;
        operationType = OperationType.WRITE_LB;
        opValue = new StatefulSetOperationValue(
            new HashSet<Long>(
                Arrays.asList(
                    (long) 0, (long) 10000, (long) Constants.MAX_UINT
                )
            )
        );

        try {
            fieldOperationB = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // At this point the two operations must have different values
        if (fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that two field operations with different values are not the same"
            );
        }

        // Compose B from A
        try {
            fieldOperationB.compose(fieldOperationA);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Verify that the individual components of the two operations agree
        OperationType opA = fieldOperationA.operationType();
        OperationType opB = fieldOperationB.operationType();

        assertEquals(opA, opB);

        StatefulSetOperationValue valA = (StatefulSetOperationValue)
            fieldOperationA.operationValue();
        StatefulSetOperationValue valB = (StatefulSetOperationValue)
            fieldOperationB.operationValue();

        assertEquals(
            valA.statefulSetValue(),
            valB.statefulSetValue()
        );

        /**
         * After the composition, the two field operations must have the same value,
         * unless we have a translate operation (not tested here).
         */
        if (!fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that a composed field operation does not agree with the composer"
            );
        }
    }

    /**
     * Checks whether the composition of stateless field
     * operator of type TRANSLATE works as expected.
     *
     * @throws Exception if the composition does not succeed
     * @throws IllegalArgumentException if the composition/comparison throw exception
     */
    @Test
    public void validTranslateComposition() throws Exception, IllegalArgumentException {
        final int statelessValueA = 15;
        final int statelessValueB = 230;

        headerField   = HeaderField.IP_TTL;
        operationType = OperationType.TRANSLATE;
        StatelessOperationValue opValue = new StatelessOperationValue(statelessValueA);

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        headerField   = HeaderField.IP_TTL;
        operationType = OperationType.TRANSLATE;
        opValue = new StatelessOperationValue(statelessValueB);

        try {
            fieldOperationB = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // At this point the two operations must have different values
        if (fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that two field operations with different values are not the same"
            );
        }

        // Compose B from A
        try {
            fieldOperationB.compose(fieldOperationA);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Verify that the individual components of the two operations agree
        OperationType opA = fieldOperationA.operationType();
        OperationType opB = fieldOperationB.operationType();

        assertEquals(opA, opB);

        StatelessOperationValue valA = (StatelessOperationValue) fieldOperationA.operationValue();
        StatelessOperationValue valB = (StatelessOperationValue) fieldOperationB.operationValue();

        // This is what translaiton is supposed to do
        assertEquals(
            statelessValueA,
            valA.statelessValue()
        );

        // Field operation B carries A
        assertEquals(
            statelessValueA + statelessValueB,
            valB.statelessValue()
        );

        /**
         * After the composition, the two field operations must have the same value,
         * unless we have a translate operation (not tested here).
         */
        if (fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "A translated field operation must never agree with the composer"
            );
        }
    }

    /**
     * Checks whether an invalid composition of an empty field is detected or not.
     *
     * @throws Exception if the composition succeeds
     * @throws IllegalArgumentException if the composition throws exception
     */
    @Test
    public void invalidNoOperationComposition() throws Exception, IllegalArgumentException {
        headerField   = HeaderField.IP_VERS;
        operationType = OperationType.NO_OPERATION;
        NoOperationValue opValue = new NoOperationValue();

        try {
            fieldOperationA = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        headerField   = HeaderField.IP_VERS;
        operationType = OperationType.NO_OPERATION;
        opValue = new NoOperationValue();

        try {
            fieldOperationB = new FieldOperation(
                headerField,
                operationType,
                opValue
            );
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // At this point the two operations must have the same value
        if (!fieldOperationB.hasSameValue(fieldOperationA)) {
            throw new Exception(
                "Unable to verify that two empty field operations do not have the same value"
            );
        }

        // Compose B from A
        try {
            fieldOperationB.compose(fieldOperationA);
        } catch (IllegalArgumentException iaEx) {
            log.info("Successfully caught an attempt to compose empty operations");
            return;
        }

        throw new Exception("Composition of empty field operation succeeded");
    }

    /**
     * Checks whether invalid field operations can be
     * successfully detected.
     *
     * @throws Exception if any of the invalid field operations is not detected
     * @throws IllegalArgumentException if any of the field operations is not built
     */
    @Test
    public void invalidFieldOperations() throws Exception, IllegalArgumentException {

        headerField   = HeaderField.IP_PROTO;
        operationType = OperationType.MONITOR;

        try {
            operationValue = new StatelessOperationValue(-1);
        } catch (IllegalArgumentException iaEx) {
            log.info("Successfully caught invalid stateless operation value");
            return;
        }

        try {
            operationValue = new StatefulOperationValue(5, -4);
        } catch (IllegalArgumentException iaEx) {
            log.info("Successfully caught invalid stateful operation value");
            return;
        }

        try {
            operationValue = new StatefulSetOperationValue((Set<Long>) null);
        } catch (IllegalArgumentException iaEx) {
            log.info("Successfully caught invalid stateful set operation value");
            return;
        }

        throw new Exception("Invalid operation value not caught");
    }

}
