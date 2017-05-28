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

package org.onosproject.metron.api.classification.trafficclass;

import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.processing.ProcessingLayer;

import org.onosproject.metron.impl.classification.trafficclass.TrafficClass;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for TrafficClass.
 */
public class TrafficClassTest {

    private final Logger log = getLogger(getClass());

    private TrafficClass tcA;
    private TrafficClass tcB;

    private Filter filter;
    private Filter filterA;
    private Filter filterB;
    private Filter filterC;

    private PacketFilter pktFilter;
    private PacketFilter pktFilterA;
    private PacketFilter pktFilterB;
    private PacketFilter pktFilterC;

    private String pattern;

    /**
     * Method to set up the test environment by creating various segment lists.
     */
    @Before
    public void setUp() {
        tcA = new TrafficClass();

        // L2-L4 traffic class
        filterA = Filter.fromEthernetAddress(HeaderField.ETHER_DST, "ec:f4:bb:d5:ff:0c");
        filterB = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, "10.0.0.1/24");
        filterC = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, "80");

        // Compose it
        pktFilterA = new PacketFilter(filterA);
        pktFilterB = new PacketFilter(filterB);
        pktFilterC = new PacketFilter(filterC);
        pktFilter  = new PacketFilter();
        pktFilter.addPacketFilter(pktFilterA);
        pktFilter.addPacketFilter(pktFilterB);
        pktFilter.addPacketFilter(pktFilterC);

        tcB = new TrafficClass();
    }

    /**
     * Checks whether the default constructor of a
     * traffic class works as expected.
     *
     * @throws IllegalArgumentException if the
     *         traffic class is not built properly
     */
    @Test
    public void defaultConstructorCheck() throws IllegalArgumentException {
        // A default traffic class must always have an ID
        assertThat(tcA.id(), notNullValue());
        // Must be empty
        assertTrue(tcA.isEmpty());
        // Must have a neutral type
        assertTrue(tcA.type() == TrafficClassType.NEUTRAL);
        // Empty packet filters
        assertTrue(tcA.packetFilter().isEmpty());
        // Empty condition map
        assertTrue(tcA.conditionMap().isEmpty());
        // No operations
        assertTrue(tcA.operation().isEmpty());
        // No post routing operations
        assertTrue(tcA.postOperations().isEmpty());
        // No processing blocks in its list
        assertTrue(tcA.blockPath().isEmpty());

        // No input interface
        assertTrue(tcA.inputInterface().isEmpty());
        // No input NF
        assertTrue(tcA.networkFunctionOfInIface().isEmpty());
        // No output interface
        assertTrue(tcA.outputInterface().isEmpty());
        // No output interface configuration
        assertTrue(tcA.outputInterfaceConf().isEmpty());
        // No output NF
        assertTrue(tcA.networkFunctionOfOutIface().isEmpty());
        // No stateful input port
        assertTrue(tcA.statefulInputPort() == TrafficClass.NO_INPUT_PORT);
        // No followers
        assertTrue(tcA.followers().isEmpty());
        // Does not calculate checksum
        assertFalse(tcA.calculateChecksum());
        // No Ethernet configuration
        assertTrue(tcA.ethernetEncapConfiguration().isEmpty());
        // No Blackbox configuration
        assertTrue(tcA.blackboxConfiguration().isEmpty());
        // It is totally offloadable, since it is empty :p
        assertTrue(tcA.isTotallyOffloadable());
        // It is not solely owned by a Blackbox NF
        assertFalse(tcA.isSolelyOwnedByBlackbox());
        // Processing layer is L2
        assertTrue(tcA.processingLayer() == ProcessingLayer.LINK);

        // No operations
        assertTrue(tcA.inputOperationsAsString().isEmpty());
        assertTrue(tcA.readOperationsAsString().isEmpty());
        assertTrue(tcA.writeOperationsAsString().isEmpty());
        assertTrue(tcA.outputOperationsAsString().isEmpty());
    }

    /**
     * Checks whether the copy constructor of
     * a traffic class works as expected.
     *
     * @throws IllegalArgumentException if the
     *         traffic class violates some properties
     */
    @Test
    public void copyConstructorCheck() throws IllegalArgumentException {
        // A traffic class must always have an ID
        assertThat(tcB.id(), notNullValue());
        // Currently, it is empty
        assertTrue(tcB.isEmpty());
        // With neutral type
        assertTrue(tcB.type() == TrafficClassType.NEUTRAL);
        // And link-level processing layer (this is where frames belong)
        assertTrue(tcB.processingLayer() == ProcessingLayer.LINK);

        // Let's now add some filters
        tcB.addPacketFilter(pktFilter);
        // Now, this traffic class must not be empty
        assertFalse(tcB.isEmpty());
        // Because it has some packet filters
        assertFalse(tcB.packetFilter().isEmpty());
        // After the updates the type has changed (NEUTRAL + AMBIGUOUS = AMBIGUOUS)
        assertTrue(tcB.type() == TrafficClassType.AMBIGUOUS);
        // And link-level processing layer (LINK + TRANSPORT = LINK)
        assertTrue(tcB.processingLayer() == ProcessingLayer.LINK);

        // It is totally offloadable
        assertTrue(tcB.isTotallyOffloadable());
        // It is not solely owned by a Blackbox NF
        assertFalse(tcB.isSolelyOwnedByBlackbox());
    }

}
