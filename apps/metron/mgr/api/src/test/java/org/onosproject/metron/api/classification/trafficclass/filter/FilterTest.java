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

import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.common.Constants;

import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for Filter.
 */
public class FilterTest {

    private final Logger log = getLogger(getClass());

    private static final short MAX_HEADER_POS = 299;
    private static final short MAX_HEADER_LEN = 63;

    private Filter filterA;
    private Filter filterB;
    private Filter filterC;

    private String pattern;

    /**
     * Method to set up the test environment by creating various segment lists.
     */
    @Before
    public void setUp() {
        filterA = new Filter();
        filterB = new Filter();
        filterC = new Filter();
    }

    /**
     * Checks whether the filter constructors work as expected.
     *
     * @throws IllegalArgumentException if any of the
     *         filters is not built properly
     */
    @Test
    public void filterConstruction() throws IllegalArgumentException {
        // Filter A
        assertThat(filterA, notNullValue());
        assertTrue(filterA.headerField() == HeaderField.UNKNOWN);
        assertTrue(filterA.fieldType() == TrafficClassType.NEUTRAL);

        // Filter B
        filterB = new Filter(HeaderField.TCP_OPT);
        assertThat(filterB, notNullValue());
        assertFalse(filterB.filter().isEmpty());
        assertTrue(filterB.fieldType() == TrafficClassType.TCP);

        filterB = new Filter(HeaderField.UDP_LEN, 23456);
        assertThat(filterB, notNullValue());
        assertFalse(filterB.filter().isEmpty());
        assertTrue(filterB.fieldType() == TrafficClassType.UDP);

        filterB = new Filter(HeaderField.IP_SRC, 124, 23456);
        assertThat(filterB, notNullValue());
        assertFalse(filterB.filter().isEmpty());
        assertTrue(filterB.fieldType() == TrafficClassType.IP);

        filterB = new Filter(HeaderField.AMBIGUOUS, (short) 60, (short) 32);
        assertThat(filterB, notNullValue());
        assertTrue(filterB.filter().isEmpty());
        assertTrue(filterB.filterToSubtract().isEmpty());
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);
    }

    /**
     * Checks whether the filter constructors assert
     * specific violations.
     *
     * @throws Exception if any of the violations is not detected
     * @throws IllegalArgumentException if any of the
     *         filters is not built properly
     */
    @Test
    public void invalidFilterDetection() throws IllegalArgumentException {
        /**
         * Invalid lower limit.
         */
        filterB = null;
        try {
            filterB = new Filter(HeaderField.IP_SRC, -1, 1789);
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught filter with invalid lower limit"
            );
        }
        assertThat(filterB, nullValue());

        /**
         * Invalid upper limit.
         */
        filterB = null;
        try {
            filterB = new Filter(HeaderField.IP_SRC, 0, Constants.MAX_LONG);
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught filter with invalid upper limit"
            );
        }
        assertThat(filterB, nullValue());

        /**
         * Invalid header position.
         */
        filterB = null;
        try {
            filterB = new Filter(
                HeaderField.AMBIGUOUS, (short) (MAX_HEADER_POS + 1), (short) MAX_HEADER_LEN
            );
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught ambiguous filter with invalid header position"
            );
        }
        assertThat(filterB, nullValue());

        /**
         * Invalid header length.
         */
        filterB = null;
        try {
            filterB = new Filter(
                HeaderField.AMBIGUOUS, (short) MAX_HEADER_POS, (short) (MAX_HEADER_LEN + 1)
            );
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught ambiguous filter with invalid header length"
            );
        }
        assertThat(filterB, nullValue());
    }

    /**
     * Stress the functionality of the fromEthernetAddress
     * filter generator.
     *
     * @throws IllegalArgumentException if any of the
     *         filters is not built properly
     */
    @Test
    public void filterFromEthernetAddress() throws IllegalArgumentException {
        /**
         * Try to create an Ethernet filter using wrong header field.
         */
        pattern = new String("00:00:00:00:00:00");
        filterB = null;
        try {
            filterB = Filter.fromEthernetAddress(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught attempt to create Ethernet filter with invalid header field"
            );
        }

        assertThat(filterB, nullValue());

        /**
         * Create an Ethernet filter using the lowest MAC address value.
         */
        pattern = new String("00:00:00:00:00:00");

        try {
            filterB = Filter.fromEthernetAddress(HeaderField.ETHER_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB.toString(), notNullValue());
        // filterB has the minimum value
        assertTrue(filterB.match((long) 0));
        assertTrue(filterB.fieldType() == TrafficClassType.ETHERNET);

        /**
         * Create an Ethernet filter using the highest MAC address value.
         */
        pattern = new String("FF:FF:FF:FF:FF:FF");

        try {
            filterB = Filter.fromEthernetAddress(HeaderField.ETHER_DST, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB.toString(), notNullValue());
        // filterB must match this MAC address
        assertTrue(filterB.match((long) MacAddress.valueOf(pattern).toLong()));
        assertTrue(filterB.fieldType() == TrafficClassType.ETHERNET);

        /**
         * Create an Ethernet filter using the highest MAC address value,
         * but a different constructor.
         */
        filterA = new Filter(HeaderField.ETHER_DST, (long) MacAddress.valueOf(pattern).toLong());

        // These 2 filters must be identical
        assertTrue(filterB.equals(filterA));
        assertTrue(filterB.fieldType() == filterA.fieldType());
    }

    /**
     * Stress the functionality of the fromIpv4PrefixStr
     * filter generator.
     *
     * @throws IllegalArgumentException if any of the
     *         filters is not built properly
     */
    @Test
    public void filterFromIpv4Prefix() throws IllegalArgumentException {
        /**
         * Try to create an IP filter using wrong header field.
         */
        pattern = new String("10.0.0.4/24");
        filterB = null;
        try {
            filterB = Filter.fromIpv4PrefixStr(HeaderField.ETHER_DST, pattern);
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught attempt to create IP filter with invalid header field"
            );
        }

        assertThat(filterB, nullValue());

        /**
         * Create an IP filter using a valid IP prefix.
         */
        pattern = new String("10.0.0.4/24");

        try {
            filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB.toString(), notNullValue());
        assertTrue(filterB.fieldType() == TrafficClassType.IP);

        /**
         * Create an IP filter using a valid and small IP prefix,
         */
        pattern = new String("255.255.255.255/0");

        try {
            filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_DST, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB.toString(), notNullValue());
        assertTrue(filterB.fieldType() == TrafficClassType.IP);

        /**
         * Create an IP filter using a valid and large IP prefix,
         */
        pattern = new String("0.0.0.0/0");

        try {
            filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_DST, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB.toString(), notNullValue());
        assertTrue(filterB.fieldType() == TrafficClassType.IP);
    }

    /**
     * Stress the functionality of fromIpClassifierPattern
     * filter generator.
     *
     * @throws IllegalArgumentException if any of the
     *         filters is not built properly
     */
    @Test
    public void filterFromIpClassifierPattern() throws IllegalArgumentException {
        /**
         * Single port.
         */
        pattern = new String(Long.toString(TpPort.MAX_PORT));

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(TpPort.MAX_PORT));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        /**
         * Single IP address.
         */
        pattern = new String("10.0.0.4");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.IP_DST, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(Common.stringOrIpToUnsignedInt(pattern)));
        assertTrue(filterB.fieldType() == TrafficClassType.IP);

        /**
         * Port range.
         */
        pattern = new String("1479-65457");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(10909));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        /**
         * Operator > on port.
         */
        pattern = new String("> 80");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(81));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        /**
         * Operator >= on port.
         */
        pattern = new String(">= 4000");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(TpPort.MAX_PORT));
        assertFalse(filterB.match(0));
        assertFalse(filterB.match(3999));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        /**
         * Operator < on port.
         */
        pattern = new String("< 12986");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(TpPort.MIN_PORT));
        assertFalse(filterB.match(12986));
        assertFalse(filterB.match(TpPort.MAX_PORT));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        /**
         * Operator <= on port.
         */
        pattern = new String("<= " + TpPort.MAX_PORT);

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.match(TpPort.MIN_PORT));
        assertTrue(filterB.match(TpPort.MAX_PORT));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        /**
         * Operator != on port.
         */
        pattern = new String("!= 1000");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertFalse(filterB.filterToSubtract().isEmpty());
        assertTrue(filterB.match(999));
        assertFalse(filterB.match(1000));
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);
    }

    /**
     * Stress the functionality of the filter intersection
     * using header fields of the same type.
     *
     * @throws IllegalArgumentException if the filter
     * is not properly intersected
     */
    @Test
    public void intersectionTest() throws IllegalArgumentException {
        pattern = new String("10.0.0.0/24");

        try {
            filterA = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertTrue(filterA.fieldType() == TrafficClassType.IP);

        pattern = new String("192.168.0.1/24");

        try {
            filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertTrue(filterB.fieldType() == TrafficClassType.IP);

        // Intersect the 2 filters
        filterA = filterA.intersect(filterB);

        // The outcome is definitelly not NULL
        assertThat(filterA, notNullValue());
        // ...but it is empty!
        assertTrue(filterA.isNone());
    }

    /**
     * Stress the functionality of the filter unite function
     * using header fields of different types (L2 and L3).
     *
     * @throws IllegalArgumentException if the filters
     * are not properly united
     */
    @Test
    public void uniteL2L3Test() throws IllegalArgumentException {
        // Ethernet field
        pattern = new String("FF:FF:FF:FF:FF:FF");
        filterA = null;
        try {
            filterA = Filter.fromEthernetAddress(HeaderField.ETHER_DST, pattern);
        } catch (IllegalArgumentException iaEx) {
            log.info(
                "Successfully caught attempt to create IP filter with invalid header field"
            );
        }

        assertThat(filterA, notNullValue());
        assertTrue(filterA.fieldType() == TrafficClassType.ETHERNET);

        // IP field
        pattern = new String("10.0.0.0/24");

        try {
            filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.fieldType() == TrafficClassType.IP);

        // Unite the 2 filters
        filterA = filterA.unite(filterB);

        // The outcome is definitelly not NULL
        assertThat(filterA, notNullValue());
        // ... and non-empty!
        assertFalse(filterA.isNone());
        // ... also ambiguous because of filterB
        assertTrue(filterA.fieldType() == TrafficClassType.IP);
    }

    /**
     * Stress the functionality of the filter unite function
     * using header fields of different types (L3 and L4).
     *
     * @throws IllegalArgumentException if the filters
     * are not properly united
     */
    @Test
    public void uniteL3L4Test() throws IllegalArgumentException {
        // IP field
        pattern = new String("10.0.0.0/24");

        try {
            filterA = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterA, notNullValue());
        assertTrue(filterA.fieldType() == TrafficClassType.IP);

        // Transport field
        pattern = new String("== 1000");

        try {
            filterB = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterB, notNullValue());
        assertTrue(filterB.fieldType() == TrafficClassType.AMBIGUOUS);

        // Unite the 2 filters
        filterA = filterA.unite(filterB);

        // The outcome is definitelly not NULL
        assertThat(filterA, notNullValue());
        // ... and non-empty!
        assertFalse(filterA.isNone());
        // ... also ambiguous because of filterB
        assertTrue(filterA.fieldType() == TrafficClassType.AMBIGUOUS);
    }

    /**
     * Stress the functionality of the filter unite function
     * using header field with neutral type.
     *
     * @throws IllegalArgumentException if the filters
     * are not properly united
     */
    @Test
    public void uniteWithNeutralTest() throws IllegalArgumentException {
        // IP field
        pattern = new String("10.0.0.0/24");

        try {
            filterA = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, pattern);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        assertThat(filterA, notNullValue());
        assertTrue(filterA.fieldType() == TrafficClassType.IP);

        // Transport field
        filterB = new Filter();

        assertThat(filterB, notNullValue());
        assertTrue(filterB.fieldType() == TrafficClassType.NEUTRAL);

        // Unite the 2 filters
        filterA = filterA.unite(filterB);

        // The outcome is definitelly not NULL
        assertThat(filterA, notNullValue());
        // ... and non-empty!
        assertFalse(filterA.isNone());
        // ... also ambiguous because of filterB
        assertTrue(filterA.fieldType() == TrafficClassType.IP);
    }

}
