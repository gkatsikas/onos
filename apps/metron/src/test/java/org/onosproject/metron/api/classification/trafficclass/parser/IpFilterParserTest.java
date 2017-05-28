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

package org.onosproject.metron.api.classification.trafficclass.parser;

import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.ArrayList;

import static org.slf4j.LoggerFactory.getLogger;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for IpFilterParser.
 */
public class IpFilterParserTest {

    private final Logger log = getLogger(getClass());

    private static final short MAX_HEADER_POS = 299;
    private static final short MAX_HEADER_LEN = 63;

    private Filter filterA;
    private Filter filterB;

    private PacketFilter packetFilterA;
    private PacketFilter packetFilterB;

    private List<PacketFilter> pfListA;
    private List<PacketFilter> pfListB;

    private String pattern;

    /**
     * Method to set up the test environment by creating various segment lists.
     */
    @Before
    public void setUp() {
        pfListA = new ArrayList<PacketFilter>();
        pfListB = new ArrayList<PacketFilter>();
    }

    /**
     * Checks whether the merge packet filters operation works or not.
     *
     * @throws IllegalArgumentException if any of the
     *         merge operations do not produce the expected results
     */
    @Test
    public void mergeTest() throws IllegalArgumentException {
        pattern = "10.0.0.4/24";
        filterA = Filter.fromIpClassifierPattern(HeaderField.IP_DST, pattern);

        pattern = "192.168.0.1/16";
        filterB = Filter.fromIpClassifierPattern(HeaderField.IP_SRC, pattern);

        packetFilterA = new PacketFilter(HeaderField.IP_DST, filterA);
        packetFilterB = new PacketFilter(HeaderField.IP_SRC, filterB);

        assertFalse(packetFilterA.size() == 0);
        assertFalse(packetFilterB.size() == 0);
        assertFalse(packetFilterA.equals(packetFilterB));

        // Construct the lists
        pfListA.add(packetFilterA);
        pfListB.add(packetFilterB);

        assertFalse(pfListA.size() == 0);
        assertFalse(pfListB.size() == 0);

        // Merge
        List<PacketFilter> pfListMerged = IpFilterParser.mergePacketFilterLists(pfListA, pfListB);

        assertFalse(pfListMerged.size() == 0);
        assertTrue(pfListMerged.size()  == 1);
        assertThat(IpFilterParser.packetFilterListToString(pfListMerged), notNullValue());
    }

    /**
     * Checks whether the negate packet filters operation works or not.
     *
     * @throws IllegalArgumentException if any of the
     *         negated operations does not produce the
     *         expected results
     */
    @Test
    public void negateTest() throws IllegalArgumentException {
        pattern = "10.0.0.4/24";
        filterA = Filter.fromIpClassifierPattern(HeaderField.IP_DST, pattern);

        pattern = "192.168.0.1/16";
        filterB = Filter.fromIpClassifierPattern(HeaderField.IP_SRC, pattern);

        packetFilterA = new PacketFilter(HeaderField.IP_DST, filterA);
        packetFilterB = new PacketFilter(HeaderField.IP_SRC, filterB);

        assertFalse(packetFilterA.size() == 0);
        assertFalse(packetFilterB.size() == 0);
        assertFalse(packetFilterA.equals(packetFilterB));

        // Construct the lists
        pfListA.add(packetFilterA);
        pfListB.add(packetFilterB);

        assertFalse(pfListA.size() == 0);
        assertFalse(pfListB.size() == 0);

        // Negate using packet filter
        List<PacketFilter> pfListNegatedSin = IpFilterParser.negatePacketFilter(packetFilterA);
        assertFalse(pfListNegatedSin.size() == 0);
        assertThat(IpFilterParser.packetFilterListToString(pfListNegatedSin), notNullValue());

        // Negate using packet filter list
        List<PacketFilter> pfListNegated = IpFilterParser.negatePacketFilterList(pfListA);
        assertFalse(pfListNegated.size() == 0);
        assertThat(IpFilterParser.packetFilterListToString(pfListNegated), notNullValue());

        // Both results must agree as they are based on the same packet filter
        for (PacketFilter p1 : pfListNegatedSin) {
            for (PacketFilter p2 : pfListNegated) {
                if (!p1.equals(p2)) {
                    throw new IllegalArgumentException("Negations disagree");
                }
            }
        }
    }

}
