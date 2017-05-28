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

package org.onosproject.metron.api.common;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Unit test for Common.
 */
public class CommonTest {

    private final Logger log = getLogger(getClass());

    private String ipStrA;
    private String ipStrB;
    private String ipStrC;

    private long ipIntA;
    private long ipIntB;
    private long ipIntC;

    /**
     * Method to set up the test environment.
     */
    @Before
    public void setUp() {
        ipStrA = null;
        ipStrB = null;
        ipStrC = null;

        ipIntA = 0;
        ipIntB = 0;
        ipIntC = 0;
    }

    /**
     * Stresses the string -> int -> string conversion.
     *
     * @throws IllegalArgumentException if any of the
     *         conversions is not done properly
     */
    @Test
    public void strToIntAndBack() throws IllegalArgumentException {
        /**
         * IP address A
         */
        ipStrA = "0.0.0.0";

        // Convert into unsigned integer
        ipIntA = Common.stringIpToInt(ipStrA);

        // Should produce zero
        assertTrue(
            ipIntA == 0
        );

        // Convert back to string
        String convertedIpStrA = Common.intIpToString(ipIntA);

        // The string must be valid
        assertFalse(
            convertedIpStrA.isEmpty() ||
            convertedIpStrA == null
        );

        // Should result in the same string
        assertTrue(
            ipStrA.equals(convertedIpStrA)
        );

        /**
         * IP address B
         */
        ipStrB = "192.168.45.67";

        // Convert into unsigned integer
        ipIntB = Common.stringIpToInt(ipStrB);

        // Should produce a positive integer
        assertTrue(
            (ipIntB > 0) && (ipIntB < Constants.MAX_UINT)
        );

        // Convert back to string
        String convertedIpStrB = Common.intIpToString(ipIntB);

        // The string must be valid
        assertFalse(
            convertedIpStrB.isEmpty() ||
            convertedIpStrB == null
        );

        // Should result in the same string
        assertTrue(
            ipStrB.equals(convertedIpStrB)
        );

        /**
         * IP address C
         */
        ipStrC = "255.255.255.255";

        // Convert into unsigned integer
        ipIntC = Common.stringIpToInt(ipStrC);

        // Should produce the maximum unsigned integer
        assertTrue(
            ipIntC == Constants.MAX_UINT
        );

        // Convert back to string
        String convertedIpStrC = Common.intIpToString(ipIntC);

        // The string must be valid
        assertFalse(
            convertedIpStrC.isEmpty() ||
            convertedIpStrC == null
        );

        // Should result in the same string
        assertTrue(
            ipStrC.equals(convertedIpStrC)
        );
    }

    /**
     * Stresses the int -> string -> int conversion.
     *
     * @throws IllegalArgumentException if any of the
     *         conversions is not done properly
     */
    @Test
    public void intToStrAndBack() throws IllegalArgumentException {
        /**
         * IP address B
         */
        ipIntA = 0;

        // Convert into string
        ipStrA = Common.intIpToString(ipIntA);

        // The string must be valid
        assertFalse(
            ipStrA.isEmpty() ||
            ipStrA == null
        );

        // Convert back to int
        long convertedIpIntA = Common.stringIpToInt(ipStrA);

        // Should produce a positive integer
        assertTrue(
            convertedIpIntA == 0
        );

        // Should result in the same integer
        assertTrue(
            ipIntA == convertedIpIntA
        );

        /**
         * IP address B
         */
        ipIntB = 348597044;

        // Convert into string
        ipStrB = Common.intIpToString(ipIntB);

        // The string must be valid
        assertFalse(
            ipStrB.isEmpty() ||
            ipStrB == null
        );

        // Convert back to int
        long convertedIpIntB = Common.stringIpToInt(ipStrB);

        // Should produce a positive integer
        assertTrue(
            (convertedIpIntB >= 0) && (convertedIpIntB < Constants.MAX_UINT)
        );

        // Should result in the same integer
        assertTrue(
            ipIntB == convertedIpIntB
        );

        /**
         * IP address C
         */
        ipIntC = Constants.MAX_UINT;

        // Convert into string
        ipStrC = Common.intIpToString(ipIntC);

        // The string must be valid
        assertFalse(
            ipStrC.isEmpty() ||
            ipStrC == null
        );

        // Convert back to int
        long convertedIpIntC = Common.stringIpToInt(ipStrC);

        // Should produce a positive integer
        assertTrue(
            convertedIpIntC == Constants.MAX_UINT
        );

        // Should result in the same integer
        assertTrue(
            ipIntC == convertedIpIntC
        );
    }

    /**
     * Stresses the int -> string -> int conversion
     * with random numbers.
     *
     * @throws IllegalArgumentException if any of the
     *         conversions is not done properly
     */
    @Test
    public void randomTest() throws IllegalArgumentException {
        long randomIpInt = generateRandomIntInRange(0, Constants.MAX_UINT);

        // Convert into string
        ipStrA = Common.intIpToString(randomIpInt);

        // The string must be valid
        assertFalse(
            ipStrA.isEmpty() ||
            ipStrA == null
        );

        // Convert back to int
        long convertedIpIntA = Common.stringIpToInt(ipStrA);

        // Should produce a positive integer
        assertTrue(
            (convertedIpIntA >= 0) && (convertedIpIntA <= Constants.MAX_UINT)
        );

        // Should result in the same integer
        assertTrue(
            randomIpInt == convertedIpIntA
        );
    }

    /**
     * Generates random integers in a range.
     *
     * @param min the lower bound of the range
     * @param max the upper bound of the range
     * @return the randomly generated number
     */
    private long generateRandomIntInRange(long min, long max) {
        return min + (long) (Math.random() * ((max - min) + 1));
    }

}
