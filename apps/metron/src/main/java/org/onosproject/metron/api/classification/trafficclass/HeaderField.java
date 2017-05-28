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

/**
 * Header fields understood by Metron.
 * These headers fields are solely related to what
 * an NFV server can parse in L2-L4.
 * Metron goes beyond this by combining NFV with
 * the SDN logic of ONOS and by augmenting the NFV
 * library with deep packet inspection, provided by
 * blackbox NFs (e.g., Snort).
 */
public enum HeaderField {

    AMBIGUOUS("AMBIGUOUS"),
    ARP_REQ("ARP REQ"),
    ARP_RES("ARP RES"),
    ETHER_DST("ETHER DST"),
    ETHER_SRC("ETHER SRC"),
    ETHER_TYPE("ETHER TYPE"),
    ICMP_CHS("ICMP CHECKSUM"),
    ICMP_CODE("ICMP CODE"),
    ICMP_ID("ICMP ID"),
    ICMP_SEQ("ICMP SEQ"),
    ICMP_TYPE("ICMP TYPE"),
    IP_CE("IP CE"),
    IP_CHS("IP CHECKSUM"),
    IP_DSCP("IP DSCP"),
    IP_DST("IP DST"),
    IP_ECT("IP ECT"),
    IP_ID("IP ID"),
    IP_IHL("IP HEADER LENGTH"),
    IP_PROTO("IP PROTO"),
    IP_SRC("IP SRC"),
    IP_TL("IP TOTAL LENGTH"),
    IP_TTL("IP TTL"),
    IP_VERS("IP VERSION"),
    MTU("MTU"),
    PAYLOAD("PAYLOAD"),
    TCP_ACK("TCP ACK"),
    TCP_CHS("TCP CHECKSUM"),
    TCP_FLAGS("TCP FLAGS"),
    TCP_FLAGS_ACK("TCP FLAG ACK"),
    TCP_FLAGS_FIN("TCP FLAG FIN"),
    TCP_FLAGS_PSH("TCP FLAG PSH"),
    TCP_FLAGS_RST("TCP FLAG RST"),
    TCP_FLAGS_SYN("TCP FLAG SYN"),
    TCP_FLAGS_URG("TCP FLAG URG"),
    TCP_OFF_RES("TCP OFF RES"),
    TCP_OPT("TCP OPT"),
    TCP_SEQ("TCP SEQ"),
    TCP_URG("TCP URG"),
    TCP_WIN("TCP WIN"),
    TP_DST_PORT("TRANSPORT DST PORT"),
    TP_SRC_PORT("TRANSPORT SRC PORT"),
    UDP_CHS("UDP CHECKSUM"),
    UDP_LEN("UDP LENGTH"),
    UNKNOWN("UNKNOWN"),
    VLAN_DEI("VLAN DEI"),
    VLAN_PCP("VLAN PCP"),
    VLAN_VID("VLAN ID");

    private String headerField;

    private HeaderField(String headerField) {
        this.headerField = headerField;
    }

    /**
     * Check whether a header field is an Ethernet field or not.
     *
     * @param headerField the header field to be checked
     * @return boolean Ethernet field or not
     */
    public static boolean isEthernet(HeaderField headerField) {
        return (headerField == ETHER_DST) ||
               (headerField == ETHER_SRC) ||
               (headerField == ETHER_TYPE);
    }

    /**
     * Check whether a header field is an Ethernet address or not.
     *
     * @param headerField the header field to be checked
     * @return boolean Ethernet address or not
     */
    public static boolean isEthernetAddress(HeaderField headerField) {
        return (headerField == ETHER_DST) ||
               (headerField == ETHER_SRC);
    }

    /**
     * Check whether a header field is an ARP field or not.
     *
     * @param headerField the header field to be checked
     * @return boolean ARP field or not
     */
    public static boolean isArp(HeaderField headerField) {
        return (headerField == ARP_REQ) ||
               (headerField == ARP_RES);
    }

    /**
     * Check whether a header field is a VLAN field or not.
     *
     * @param headerField the header field to be checked
     * @return boolean VLAN field or not
     */
    public static boolean isVlan(HeaderField headerField) {
        return (headerField == VLAN_DEI) ||
               (headerField == VLAN_PCP) ||
               (headerField == VLAN_VID);
    }

    /**
     * Check whether a header field is IPv4-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean IPv4 or not
     */
    public static boolean isIp(HeaderField headerField) {
        return (headerField == IP_CE)  ||
               (headerField == IP_CHS) ||
               (headerField == IP_DSCP)   ||
               (headerField == IP_DST)  ||
               (headerField == IP_ECT)  ||
               (headerField == IP_ID)  ||
               (headerField == IP_IHL)  ||
               (headerField == IP_PROTO)  ||
               (headerField == IP_SRC)  ||
               (headerField == IP_TL)  ||
               (headerField == IP_TTL)  ||
               (headerField == IP_VERS);
    }

    /**
     * Check whether a header field is an IP address or not.
     *
     * @param headerField the header field to be checked
     * @return boolean IP address or not
     */
    public static boolean isIpAddress(HeaderField headerField) {
        return (headerField == IP_DST)  ||
               (headerField == IP_SRC);
    }

    /**
     * Check whether a header field is TCP-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean TCP or not
     */
    public static boolean isTcp(HeaderField headerField) {
        return (headerField == TCP_ACK)       ||
               (headerField == TCP_CHS)       ||
               (headerField == TCP_FLAGS_ACK) ||
               (headerField == TCP_FLAGS_FIN) ||
               (headerField == TCP_FLAGS_PSH) ||
               (headerField == TCP_FLAGS_RST) ||
               (headerField == TCP_FLAGS_SYN) ||
               (headerField == TCP_FLAGS_URG) ||
               (headerField == TCP_OFF_RES)   ||
               (headerField == TCP_OPT)       ||
               (headerField == TCP_SEQ)       ||
               (headerField == TCP_URG)       ||
               (headerField == TCP_WIN)       ||
               (headerField == TCP_SEQ);
    }

    /**
     * Check whether a header field is UDP-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean UDP or not
     */
    public static boolean isUdp(HeaderField headerField) {
        return (headerField == UDP_CHS) ||
               (headerField == UDP_LEN);
    }

    /**
     * Check whether a header field is a transport port or not.
     *
     * @param headerField the header field to be checked
     * @return boolean transport port or not
     */
    public static boolean isTransportPort(HeaderField headerField) {
        return (headerField == TP_DST_PORT) ||
               (headerField == TP_SRC_PORT);
    }

    /**
     * Check whether a header field is ICMP-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean ICMP or not
     */
    public static boolean isIcmp(HeaderField headerField) {
        return (headerField == ICMP_CHS)  ||
               (headerField == ICMP_CODE) ||
               (headerField == ICMP_ID)   ||
               (headerField == ICMP_SEQ)  ||
               (headerField == ICMP_TYPE);
    }

    /**
     * Check whether a header field is not a header field (thus payload).
     *
     * @param headerField the header field to be checked
     * @return boolean payload or not
     */
    public static boolean isPayload(HeaderField headerField) {
        return (headerField == PAYLOAD);
    }

    /**
     * Check whether a header field is an ambiguous field or not.
     * Transport port fields are ambiguous because we don't know
     * if it is a TCP or UDP transport port.
     *
     * @param headerField the header field to be checked
     * @return boolean ambiguous field or not
     */
    public static boolean isAmbiguous(HeaderField headerField) {
        return isTransportPort(headerField) ||
               (headerField == AMBIGUOUS);
    }

    @Override
    public String toString() {
        return headerField;
    }

}
