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

package org.onosproject.metron.api.classification.trafficclass.filter;

import org.onosproject.metron.api.common.Constants;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for DisjointSegmentList.
 */
public class DisjointSegmentListTest {

    private final Logger log = getLogger(getClass());

    private static final long SEG_A_LOWER = 2;
    private static final long SEG_A_UPPER = 20;

    private static final long SEG_B_LOWER = 30;
    private static final long SEG_B_UPPER = 43;

    private static final long MIN_SEG_UPPER = 0;
    private static final long MAX_SEG_UPPER = Constants.MAX_LONG;

    private static final long SEG_C_LOWER = MIN_SEG_UPPER;
    private static final long SEG_C_UPPER = MAX_SEG_UPPER;

    private DisjointSegmentList segmentListA;
    private DisjointSegmentList segmentListB;
    private DisjointSegmentList segmentListC;

    /**
     * Method to set up the test environment by creating various segment lists.
     */
    @Before
    public void setUp() {
        segmentListA = new DisjointSegmentList();
        segmentListB = new DisjointSegmentList();
        segmentListC = new DisjointSegmentList();

        segmentListA.addSegment(SEG_A_LOWER, SEG_A_UPPER);
        segmentListB.addSegment(SEG_B_LOWER, SEG_B_UPPER);
        segmentListC.addSegment(SEG_C_LOWER, SEG_C_UPPER);
    }

    /**
     * Checks whether the segment lists can detect
     * segments that are within their range.
     *
     * @throws Exception if any of the segment lists
     *         does not contain these segments
     */
    @Test
    public void validContainQueries() throws Exception {
        long testLower = SEG_A_LOWER + 3;
        long testUpper = SEG_A_UPPER - 5;

        // Segment list A
        if (
                !segmentListA.containsSegment(testLower, testUpper) &&
                (testLower >= SEG_A_LOWER) && (testUpper <= SEG_A_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = SEG_A_LOWER;
        testUpper = SEG_A_UPPER;
        if (
                !segmentListA.containsSegment(testLower, testUpper) &&
                (testLower >= SEG_A_LOWER) && (testUpper <= SEG_A_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        // Segment list B
        testLower = SEG_B_LOWER + 1;
        testUpper = SEG_B_UPPER - 1;
        if (
                !segmentListB.containsSegment(testLower, testUpper) &&
                (testLower >= SEG_B_LOWER) && (testUpper <= SEG_B_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = SEG_B_LOWER;
        testUpper = SEG_B_UPPER;
        if (
                !segmentListB.containsSegment(testLower, testUpper) &&
                (testLower >= SEG_B_LOWER) && (testUpper <= SEG_B_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        // Segment list C
        testLower = MIN_SEG_UPPER + 10;
        testUpper = MAX_SEG_UPPER - 4;
        if (
                !segmentListC.containsSegment(testLower, testUpper) &&
                (testLower >= MIN_SEG_UPPER) && (testUpper <= MAX_SEG_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = MIN_SEG_UPPER;
        testUpper = MAX_SEG_UPPER;
        if (
                !segmentListC.containsSegment(testLower, testUpper) &&
                (testLower >= MIN_SEG_UPPER) && (testUpper <= MAX_SEG_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }
    }

    /**
     * Checks whether the segment lists can detect
     * segments that are not within their range.
     *
     * @throws Exception if any of the segment lists
     *         contains these segments
     */
    @Test
    public void invalidContainQueries() throws Exception {
        long testLower = SEG_A_LOWER;
        long testUpper = SEG_A_UPPER + 5;

        // Segment list A
        if (
                segmentListA.containsSegment(testLower, testUpper) &&
                (testLower >= SEG_A_LOWER) && (testUpper > SEG_A_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = SEG_A_LOWER - 1;
        testUpper = SEG_A_UPPER;
        if (
                segmentListA.containsSegment(testLower, testUpper) &&
                (testLower < SEG_A_LOWER) && (testUpper <= SEG_A_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = SEG_A_LOWER - 1;
        testUpper = SEG_A_UPPER + 1;
        if (
                segmentListA.containsSegment(testLower, testUpper) &&
                (testLower < SEG_A_LOWER) && (testUpper > SEG_A_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        // Segment list B
        testLower = SEG_B_LOWER + 1;
        testUpper = SEG_B_UPPER + 1;
        if (
                segmentListB.containsSegment(testLower, testUpper) &&
                (testLower >= SEG_B_LOWER) && (testUpper > SEG_B_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = SEG_B_LOWER - 1;
        testUpper = SEG_B_UPPER;
        if (
                segmentListB.containsSegment(testLower, testUpper) &&
                (testLower < SEG_B_LOWER) && (testUpper <= SEG_B_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }

        testLower = SEG_B_LOWER - 1;
        testUpper = SEG_B_UPPER + 1;
        if (
                segmentListB.containsSegment(testLower, testUpper) &&
                (testLower < SEG_B_LOWER) && (testUpper > SEG_B_UPPER) &&
                (testLower <= testUpper)
        ) {
            throw new Exception("Method containsSegment is buggy");
        }
    }

    /**
     * Checks whether intersection works or not.
     *
     * @throws Exception if any of the segment lists
     *         are not properly intersected
     */
    @Test
    public void intersectionTest() throws Exception {
        segmentListA = new DisjointSegmentList();
        segmentListB = new DisjointSegmentList();

        // This can be IP proto = TCP
        segmentListA.addSegment(6, 6);
        // This can be IP proto = UDP
        segmentListB.addSegment(17, 17);

        // Each segment has some content
        assertFalse(segmentListA.isEmpty());
        assertFalse(segmentListB.isEmpty());

        // There is no intersection between these segments
        segmentListA.intersectSegmentList(segmentListB);

        // The outcome is not NULL
        assertThat(segmentListA, notNullValue());

        // ... but has to be an empty segment
        assertTrue(segmentListA.isEmpty());
    }

    /**
     * Checks whether unification works or not.
     *
     * @throws Exception if any of the segment lists
     *         are not properly unified
     */
    @Test
    public void unificationTest() throws Exception {
        segmentListA = new DisjointSegmentList();
        segmentListB = new DisjointSegmentList();

        // This can be IP proto = TCP
        segmentListA.addSegment(6, 6);
        // This can be IP proto = UDP
        segmentListB.addSegment(17, 17);

        // Each segment has some content
        assertFalse(segmentListA.isEmpty());
        assertFalse(segmentListB.isEmpty());

        // Take the union of these segments
        SegmentNode unified = segmentListA.unify(segmentListA.head(), segmentListB.head());

        // The outcome is not NULL
        assertThat(unified, notNullValue());

        // the lower bounds starts at 6
        assertTrue(segmentListA.head().lowerLimit() == 6);
        assertTrue(segmentListA.contains((long) 6));
        assertTrue(segmentListA.containsSegment((long) 6,  (long) 6));

        // while the upper goes up to 17
        assertTrue(segmentListA.contains((long) 17));
        assertTrue(segmentListA.containsSegment((long) 17, (long) 17));
    }

    /**
     * Checks whether subtraction works or not.
     *
     * @throws Exception if any of the segment lists
     *         are not properly subtracted
     */
    @Test
    public void subtractTest() throws Exception {
        segmentListA = new DisjointSegmentList();
        segmentListB = new DisjointSegmentList();

        // This can be IP proto = TCP
        segmentListA.addSegment(6, 6);
        // This can be IP proto = UDP
        segmentListB.addSegment(17, 17);

        // Take the union of these segments
        SegmentNode unified = segmentListA.unify(segmentListA.head(), segmentListB.head());
        assertTrue(segmentListA.containsSegment((long) 6, (long) 6));

        // Remove segment A
        segmentListA.subtractSegment(6, 6);
        // Verify that it is not there
        assertFalse(segmentListA.containsSegment((long) 6, (long) 6));

        // Add it back
        segmentListA.addSegment(6, 6);
        // Create a clone
        DisjointSegmentList segmentListC = new DisjointSegmentList();
        segmentListC.addSegment(6, 6);

        // Now subtract the two segments lists
        segmentListA.subtractSegmentList(segmentListC);
        // It should not be there again
        assertFalse(segmentListA.containsSegment((long) 6, (long) 6));
    }

    /**
     * Checks whether the a segment node can detect lower or
     * upper values that exceed the ranges.
     *
     * @throws IllegalArgumentException if the limit violations
     *         are correctly detected
     * @throws Exception if the limit violations are not detected
     */
    @Test
    public void invalidLimits() throws IllegalArgumentException, Exception {
        long testLower = MIN_SEG_UPPER - 1;
        long testUpper = SEG_A_UPPER;

        try {
            segmentListA.containsSegment(testLower, testUpper);
        } catch (IllegalArgumentException iaEx) {
            log.info("A negative low bound is successfully caught by the SegmentNode");
        }

        testLower = SEG_B_LOWER;
        long testUpperLong = MAX_SEG_UPPER + 1;
        try {
            segmentListA.containsSegment(testLower, testUpperLong);
            segmentListB.containsSegment(testLower, testUpperLong);
            segmentListC.containsSegment(testLower, testUpperLong);
        } catch (IllegalArgumentException iaEx) {
            log.info("An upper bound beyong the integer limits is successfully caught by the SegmentNode");
            return;
        }

        throw new Exception("Segment node does not flag the out of range segment values");
    }

}
