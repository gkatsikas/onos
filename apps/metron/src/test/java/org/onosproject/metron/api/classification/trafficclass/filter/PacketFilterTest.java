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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for PacketFilter.
 */
public class PacketFilterTest {

    private final Logger log = getLogger(getClass());

    private Filter filter;
    private Filter filterA;
    private Filter filterB;
    private Filter filterC;
    private Filter filterConfl;

    private PacketFilter pktFilter;
    private PacketFilter pktFilterA;
    private PacketFilter pktFilterB;
    private PacketFilter pktFilterC;
    private PacketFilter pktFilterConfl;

    /**
     * Method to set up the test environment by creating various segment lists.
     */
    @Before
    public void setUp() {
        // L2-L4 traffic class
        filterA = Filter.fromEthernetAddress(HeaderField.ETHER_DST, "ec:f4:bb:d5:ff:0c");
        filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, "10.0.0.1/24");
        filterC = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, "80");
        filterConfl = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, "<= 80");

        // Initialize
        pktFilter  = new PacketFilter();
        pktFilterA = new PacketFilter(filterA);
        pktFilterB = new PacketFilter(filterB);
        pktFilterC = new PacketFilter(filterC);
        pktFilterConfl = new PacketFilter(filterConfl);
    }

    /**
     * Checks whether the default constructor of a
     * packet filter works as expected.
     *
     * @throws IllegalArgumentException if the
     *         packet filter is not empty
     */
    @Test
    public void defaultConstructorCheck() throws IllegalArgumentException {
        // Must be empty
        assertTrue(pktFilter.isEmpty());
    }

    /**
     * Checks whether the copy constructor of a
     * packet filter works as expected.
     *
     * @throws IllegalArgumentException if a
     *         packet filter violates some properties
     */
    @Test
    public void copyConstructorCheck() throws IllegalArgumentException {
        // None of the following packet filters are NULL or empty
        assertThat(pktFilterA, notNullValue());
        assertFalse(pktFilterA.isEmpty());

        assertThat(pktFilterB, notNullValue());
        assertFalse(pktFilterB.isEmpty());

        assertThat(pktFilterC, notNullValue());
        assertFalse(pktFilterC.isEmpty());
    }

    /**
     * Checks whether packet filter's key operations
     * (e.g., composition, confict detection, coverity)
     * work as expected.
     *
     * @throws IllegalArgumentException if a
     *         packet filter's function fails
     */
    @Test
    public void functionalCheck() throws IllegalArgumentException {
        pktFilter.addPacketFilter(pktFilterA);
        assertTrue(pktFilter.covers(pktFilterA));

        pktFilter.addPacketFilter(pktFilterB);
        assertTrue(pktFilter.covers(pktFilterB));

        // No conflict on port 80
        assertFalse(pktFilter.conflictsWith(pktFilterConfl));

        pktFilter.addPacketFilter(pktFilterC);
        assertTrue(pktFilter.covers(pktFilterC));

        // Conflict on port 80
        assertTrue(pktFilter.conflictsWith(pktFilterConfl));
    }

}
