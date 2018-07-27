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

package org.onosproject.metron.api.processing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Different classes of packet proccessing blocks.
 */
public enum ProcessingBlockClass {

    ARP_QUERIER("ARPQuerier"),
    ARP_RESPONDER("ARPResponder"),
    AVERAGE_COUNTER("AverageCounter"),
    AVERAGE_COUNTER_MP("AverageCounterMP"),
    CHECK_ICMP_HEADER("CheckICMPHeader"),
    CHECK_IP_HEADER("CheckIPHeader"),
    CHECK_TCP_HEADER("CheckTCPHeader"),
    CHECK_UDP_HEADER("CheckUDPHeader"),
    CLASSIFIER("Classifier"),
    COUNTER("Counter"),
    COUNTER_MP("CounterMP"),
    DEC_IP_TTL("DecIPTTL"),
    DIRECT_IP_LOOKUP("DirectIPLookup"),
    DISCARD("Discard"),
    DROP_BROADCASTS("DropBroadcasts"),
    ETHER_ENCAP("EtherEncap"),
    ETHER_MIRROR("EtherMirror"),
    ETHER_REWRITE("EtherRewrite"),
    FIX_IP_SRC("FixIPSrc"),
    FROM_BLACKBOX_DEVICE("FromBlackboxDevice"),
    FROM_DEVICE("FromDevice"),
    FROM_DPDK_DEVICE("FromDPDKDevice"),
    FROM_NET_FRONT("FromNetFront"),
    FROM_SNORT_DEVICE("FromSnortDevice"),
    GET_IP_ADDRESS("GetIPAddress"),
    IP_ADDR_PAIR_REWRITER("IPAddrPairRewriter"),
    IP_ADDR_REWRITER("IPAddrRewriter"),
    IP_CLASSIFIER("IPClassifier"),
    IP_FILTER("IPFilter"),
    IP_FRAGMENTER("IPFragmenter"),
    IP_GW_OPTIONS("IPGWOptions"),
    IP_OUTPUT_COMBO("IPOutputCombo"),
    IP_PRINT("IPPrint"),
    IP_REWRITER("IPRewriter"),
    LINEAR_IP_LOOKUP("LinearIPLookup"),
    LOOKUP_IP_ROUTE_MP("LookupIPRouteMP"),
    MARK_IP_HEADER("MarkIPHeader"),
    OPENFLOW_CLASSIFIER("OpenFlowClassifier"),
    PAINT("Paint"),
    PAINT_TEE("PaintTee"),
    POLL_DEVICE("PollDevice"),
    PRINT("Print"),
    QUEUE("Queue"),
    RADIX_IP_LOOKUP("RadixIPLookup"),
    RANGE_IP_LOOKUP("RangeIPLookup"),
    ROUND_ROBIN_IP_MAPPER("RoundRobinIPMapper"),
    SET_VLAN_ANNO("SetVLANAnno"),
    SIMPLE_ETHERNET_CLASSIFIER("SimpleEthernetClassifier"),
    SORTED_IP_LOOKUP("SortedIPLookup"),
    STATIC_IP_LOOKUP("StaticIPLookup"),
    STORE_ETHER_ADDRESS("StoreEtherAddress"),
    STRIP("Strip"),
    TCP_REWRITER("TCPRewriter"),
    TO_BLACKBOX_DEVICE("ToBlackboxDevice"),
    TO_DEVICE("ToDevice"),
    TO_DPDK_DEVICE("ToDPDKDevice"),
    TO_NET_FRONT("ToNetFront"),
    TO_SNORT_DEVICE("ToSnortDevice"),
    UDP_REWRITER("UDPRewriter"),
    UNSTRIP("Unstrip"),
    VLAN_ENCAP("VLANEncap"),
    VLAN_DECAP("VLANDecap");

    private String processingBlockClass;

    // Statically maps names with enum types
    private static final Map<String, ProcessingBlockClass> MAP =
        new ConcurrentHashMap<String, ProcessingBlockClass>();
    static {
        for (ProcessingBlockClass bl : ProcessingBlockClass.values()) {
            MAP.put(bl.toString(), bl);
        }
    }

    private ProcessingBlockClass(String processingBlockClass) {
        this.processingBlockClass = processingBlockClass;
    }

    /**
     * Returns a ProcessingBlockClass object created by an
     * input processing block name.
     *
     * @param nameStr the name of the block to be created
     * @return ProcessingBlockClass object
     */
    public static ProcessingBlockClass getByName(String nameStr) {
        return MAP.get(nameStr);
    }

    /**
     * Classifies the input block name to assess whether
     * it is an input processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean input or non-input
     */
    public static boolean isInput(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == FROM_BLACKBOX_DEVICE) ||
            (blockClass == FROM_DEVICE) ||
            (blockClass == FROM_DPDK_DEVICE) ||
            (blockClass == FROM_NET_FRONT) ||
            (blockClass == FROM_SNORT_DEVICE) ||
            (blockClass == POLL_DEVICE)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies the input block name to assess whether
     * it is an output processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean output or non-output
     */
    public static boolean isOutput(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == TO_BLACKBOX_DEVICE) ||
            (blockClass == TO_DEVICE) ||
            (blockClass == TO_DPDK_DEVICE) ||
            (blockClass == TO_NET_FRONT) ||
            (blockClass == TO_SNORT_DEVICE)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies the input block name to assess whether
     * it is a classifier processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean classifier or non-classifier
     */
    public static boolean isClassifier(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == CLASSIFIER) ||
            (blockClass == DIRECT_IP_LOOKUP) ||
            (blockClass == IP_CLASSIFIER) ||
            (blockClass == IP_FILTER) ||
            (blockClass == LINEAR_IP_LOOKUP) ||
            (blockClass == LOOKUP_IP_ROUTE_MP) ||
            (blockClass == OPENFLOW_CLASSIFIER) ||
            (blockClass == RADIX_IP_LOOKUP) ||
            (blockClass == RANGE_IP_LOOKUP) ||
            (blockClass == SIMPLE_ETHERNET_CLASSIFIER) ||
            (blockClass == SORTED_IP_LOOKUP) ||
            (blockClass == STATIC_IP_LOOKUP)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies the input block name to assess whether
     * it is a rewriter processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean rewriter or non-rewriter
     */
    public static boolean isRewriter(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == IP_ADDR_PAIR_REWRITER) ||
            (blockClass == IP_ADDR_REWRITER) ||
            (blockClass == IP_REWRITER) ||
            (blockClass == TCP_REWRITER) ||
            (blockClass == UDP_REWRITER)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies the input block name to assess whether
     * it is a blackbox processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean blackbox or non-blackbox
     */
    public static boolean isBlackbox(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == FROM_BLACKBOX_DEVICE) ||
            (blockClass == FROM_SNORT_DEVICE) ||
            (blockClass == TO_BLACKBOX_DEVICE) ||
            (blockClass == TO_SNORT_DEVICE)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies the input block name to assess whether
     * it is a post-routing processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean post-routing or non-post-routing
     */
    public static boolean isPostRouting(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == DEC_IP_TTL) ||
            (blockClass == DROP_BROADCASTS) ||
            (blockClass == FIX_IP_SRC) ||
            (blockClass == IP_FRAGMENTER) ||
            (blockClass == IP_GW_OPTIONS) ||
            (blockClass == IP_OUTPUT_COMBO)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies the input block name to assess whether
     * it is a counter processing block.
     *
     * @param blockClass the class of the block to be checked
     * @return boolean counter or non-counter
     */
    public static boolean isCounter(ProcessingBlockClass blockClass) {
        if (blockClass == null) {
            return false;
        }

        if ((blockClass == AVERAGE_COUNTER) ||
            (blockClass == AVERAGE_COUNTER_MP) ||
            (blockClass == COUNTER) ||
            (blockClass == COUNTER_MP)) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return this.processingBlockClass;
    }

}
