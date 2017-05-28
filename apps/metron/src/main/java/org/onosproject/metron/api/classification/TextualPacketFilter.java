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

package org.onosproject.metron.api.classification;

import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;
import org.onosproject.metron.api.exceptions.ParseException;

import org.onlab.packet.TpPort;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;

import org.slf4j.Logger;

import com.google.common.collect.Sets;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A packet filter is a header field associated with
 * an operator and a header value. This class is a
 * primitive object used to construct a traffic class.
 */
public final class TextualPacketFilter {

    private static final Logger log = getLogger(TextualPacketFilter.class);

    private String  headerField;
    private String  operator;
    private String  headerValue;
    private boolean isNegated;
    private boolean isAllowed;
    private TrafficClassType type;

    public TextualPacketFilter(
            String  headerField,
            String  operator,
            String  headerValue) {
        this(headerField, operator, headerValue, false, true);
    }

    public TextualPacketFilter(
            String  headerField,
            String  operator,
            String  headerValue,
            boolean isNegated,
            boolean isAllowed) {
        this.headerField = headerField;
        this.operator    = operator;
        this.headerValue = headerValue;
        this.isNegated   = isNegated;
        this.isAllowed   = isAllowed;

        this.type = this.findType();
    }

    /**
     * Returns the header field of this packet filter.
     *
     * @return header field as a string
     */
    public String headerField() {
        return this.headerField;
    }

    /**
     * Returns the operator of this packet filter.
     *
     * @return operator as a string
     */
    public String operator() {
        return this.operator;
    }

    /**
     * Returns the header value of this packet filter.
     *
     * @return header value as a string
     */
    public String headerValue() {
        return this.headerValue;
    }

    /**
     * Returns true if this packet filter is negated.
     *
     * @return boolean status
     */
    public boolean isNegated() {
        return this.isNegated;
    }

    /**
     * Returns true if this packet filter is allowed.
     *
     * @return boolean status
     */
    public boolean isAllowed() {
        return this.isAllowed;
    }

    /**
     * Returns the 'post IP' protocol type of this packet filter.
     *
     * @return packet filter type
     */
    public TrafficClassType type() {
        return this.type;
    }

    /**
     * Checks whether this set of packet filters can be implemented in hardware.
     *
     * @param packetFilters to be checked for hardware compliance
     * @return boolean status
     */
    public static boolean isHardwareCompliant(Set<TextualPacketFilter> packetFilters) {
        for (TextualPacketFilter pf : packetFilters) {
            if (!pf.isHardwareCompliant()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether this packet filter can be implemented in hardware.
     *
     * @return boolean status
     */
    public boolean isHardwareCompliant() {
        return  !this.isNegated() &&
                this.linkLayerHwCompliance() &&
                this.networkLayerHwCompliance() &&
                this.transportLayerHwCompliance();
    }

    /**
     * Checks whether this packet filter contains ambiguous header fields.
     * For example, dst port 145 corresponds to (dst tcp port 145 OR dst udp port 145).
     *
     * @return boolean ambiguity status
     */
    public boolean isAmbiguous() {
        if (this.headerField().equals(ClassificationSyntax.PATTERN_SRC_PORT) ||
            this.headerField().equals(ClassificationSyntax.PATTERN_DST_PORT)) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter contains ambiguous header fields, given an
     * input set of packet filters.
     *
     * @param setToCompare set of packet filters to compare this one against and then decide
     * @return boolean ambiguity status
     */
    public boolean isAmbiguousGivenSet(Set<TextualPacketFilter> setToCompare) {
        boolean isLikely = false;

        for (TextualPacketFilter pf : setToCompare) {
            if (this.headerField().equals(ClassificationSyntax.PATTERN_SRC_PORT) ||
                this.headerField().equals(ClassificationSyntax.PATTERN_DST_PORT)) {

                // Verify that the given set does not contain any IP protocol specific filters
                if (!pf.headerField().equals(this.headerField()) && (pf.isUdp() || pf.isTcp())) {
                    return false;
                } else {
                    isLikely = true;
                }
            }
        }

        if (isLikely) {
            return true;
        }

        return false;
    }

    /**
     * An ambiguous packet filter is translated into a set of packet filters.
     * For example, dst port 145 corresponds to (dst tcp port 145 OR dst udp port 145).
     *
     * @return set of packet filters that comprise the original, ambiguous one
     */
    public Set<TextualPacketFilter> augmentAmbiguousPacketFilter() {
        if (!this.isAmbiguous()) {
            return null;
        }

        Set<TextualPacketFilter> newPacketFilters = Sets.<TextualPacketFilter>newConcurrentHashSet();

        if (this.headerField().equals(ClassificationSyntax.PATTERN_SRC_PORT)) {
            TextualPacketFilter udpPf = new TextualPacketFilter(
                ClassificationSyntax.PATTERN_SRC_UDP_PORT,
                this.operator,
                this.headerValue
            );
            newPacketFilters.add(udpPf);

            TextualPacketFilter tcpPf = new TextualPacketFilter(
                ClassificationSyntax.PATTERN_SRC_TCP_PORT,
                this.operator,
                this.headerValue
            );
            newPacketFilters.add(tcpPf);
        } else if (this.headerField().equals(ClassificationSyntax.PATTERN_DST_PORT)) {
            TextualPacketFilter udpPf = new TextualPacketFilter(
                ClassificationSyntax.PATTERN_DST_UDP_PORT,
                this.operator,
                this.headerValue
            );
            newPacketFilters.add(udpPf);

            TextualPacketFilter tcpPf = new TextualPacketFilter(
                ClassificationSyntax.PATTERN_DST_TCP_PORT,
                this.operator,
                this.headerValue
            );
            newPacketFilters.add(tcpPf);
        }

        return newPacketFilters;
    }

    /**
     * Checks whether this packet filter is compliant with the ones present in the input set.
     * Incompliance might occur as follows:
     * Set contains dst tcp port X and this packet filter is src udp port Y.
     * The source of incompliance is the protocol type in this case.
     *
     * @param packetFilterSet the packet filter set to be checked against this one
     * @return boolean compliance status
     */
    public boolean compliesWithPacketFilterSet(Set<TextualPacketFilter> packetFilterSet) {
        // Neutral packet filters comply by default
        if (this.type == TrafficClassType.NEUTRAL) {
            return true;
        }

        // Let's examine
        for (TextualPacketFilter pf : packetFilterSet) {
            // These are easy going filters
            if (pf.isNeutral()) {
                continue;
            }

            if (pf.isEthernet() && (this.type != TrafficClassType.ETHERNET)) {
                return false;
            }

            if (pf.isArp() && (this.type != TrafficClassType.ARP)) {
                return false;
            }

            if (pf.isVlan() && (this.type != TrafficClassType.VLAN)) {
                return false;
            }

            if (pf.isTcp() && (this.type != TrafficClassType.TCP)) {
                return false;
            }

            if (pf.isUdp() && (this.type != TrafficClassType.UDP)) {
                return false;
            }

            if (pf.isIcmp() && (this.type != TrafficClassType.ICMP)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether this packet filter is Ethernet-based or not.
     *
     * @return boolean Ethernet or not
     */
    public boolean isEthernet() {
        if (ClassificationSyntax.ETHERNET_PATTERNS.contains(this.headerField)) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter is ARP-based or not.
     *
     * @return boolean ARP or not
     */
    public boolean isArp() {
        if (ClassificationSyntax.ARP_PATTERNS.contains(this.headerField)) {
            return true;
        }

        if (this.headerField.equals(ClassificationSyntax.PATTERN_ETHER_PROTO) &&
           (this.headerValue.equals(ClassificationSyntax.ETHER_PROTO_ARP))) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter is VLAN-based or not.
     *
     * @return boolean VLAN or not
     */
    public boolean isVlan() {
        if (this.headerField.equals(ClassificationSyntax.PATTERN_ETHER_PROTO) &&
           (this.headerValue.equals(ClassificationSyntax.ETHER_PROTO_VLAN))) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter is IPv4-based or not.
     *
     * @return boolean IP or not
     */
    public boolean isIp() {
        if (ClassificationSyntax.IP_PATTERNS.contains(this.headerField)) {
            return true;
        }

        if (this.isTcp() || this.isUdp() || this.isIcmp()) {
            return true;
        }

        if (this.headerField.equals(ClassificationSyntax.PATTERN_ETHER_PROTO) &&
           (this.headerValue.equals(ClassificationSyntax.ETHER_PROTO_IPV4))) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter is TCP-based or not.
     *
     * @return boolean TCP or not
     */
    public boolean isTcp() {
        if (ClassificationSyntax.TCP_PATTERNS.contains(this.headerField)) {
            return true;
        }

        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_PROTO) &&
           (this.headerValue.equals(ClassificationSyntax.IPV4_PROTO_TCP) ||
            this.headerValue.equals(Long.toString(ClassificationSyntax.VALUE_PROTO_TCP)))) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter is UDP-based or not.
     *
     * @return boolean UDP or not
     */
    public boolean isUdp() {
        if (ClassificationSyntax.UDP_PATTERNS.contains(this.headerField)) {
            return true;
        }

        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_PROTO) &&
           (this.headerValue.equals(ClassificationSyntax.IPV4_PROTO_UDP) ||
            this.headerValue.equals(Long.toString(ClassificationSyntax.VALUE_PROTO_UDP)))) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter is ICMP-based or not.
     *
     * @return boolean ICMP or not
     */
    public boolean isIcmp() {
        if (ClassificationSyntax.ICMP_PATTERNS.contains(this.headerField)) {
            return true;
        }

        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_PROTO) &&
           (this.headerValue.equals(ClassificationSyntax.IPV4_PROTO_ICMP) ||
            this.headerValue.equals(Long.toString(ClassificationSyntax.VALUE_PROTO_ICMP)))) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether this packet filter depends on any 'post IP' protocol or not.
     *
     * @return boolean neutral or not
     */
    public boolean isNeutral() {
        if (this.isIcmp() || this.isTcp() || this.isUdp()) {
            return false;
        }

        return true;
    }

    /**
     * Looks for the (protocol) type of this packet filter according to its header field.
     *
     * @return traffic class type
     */
    private TrafficClassType findType() {
        if (this.isAmbiguous()) {
            return TrafficClassType.AMBIGUOUS;
        } else if (this.isIcmp()) {
            return TrafficClassType.ICMP;
        } else if (this.isTcp()) {
            return TrafficClassType.TCP;
        } else if (this.isUdp()) {
            return TrafficClassType.UDP;
        }

        return TrafficClassType.NEUTRAL;
    }

    /**
     * Checks whether the link layer operations of this packet filter
     * can be implemented in hardware.
     *
     * @return boolean status
     */
    private boolean linkLayerHwCompliance() {
        return true;
    }

    /**
     * Checks whether the network layer operations of this packet filter
     * can be implemented in hardware.
     *
     * @return boolean status
     */
    private boolean networkLayerHwCompliance() {
        // IP version
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_VERS)) {
            return false;
        }

        // IP header length
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_HDR_LEN)) {
            return false;
        }

        // IP identifier
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_ID)) {
            return false;
        }

        // IP type of service (ToS)
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_TOS)) {
            return false;
        }

        // IP time to live (TTL)
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_TTL)) {
            /**
             * TODO: We don't know whether this packet filter comes from
             * a rule of a decrement IP TTL operation..
             */
            // return false;
        }

        // IP fragmentation
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_FRAG)) {
            return false;
        }

        // IP de-fragmentation
        if (this.headerField.equals(ClassificationSyntax.PATTERN_IP_UNFRAG)) {
            return false;
        }

        return true;
    }

    /**
     * Checks whether the transport layer operations of this packet filter
     * can be implemented in hardware.
     *
     * @return boolean status
     */
    private boolean transportLayerHwCompliance() {
        // TCP window
        if (this.headerField.equals(ClassificationSyntax.PATTERN_TCP_WIN)) {
            return false;
        }

        return true;
    }

    /**
     * Integrates a packet filter into a traffic selector object.
     *
     * @param selector a traffic selector to integrate the packet filters into
     * @param relevantFilters relevant packet filters in the same rule that might affect this selector
     */
    public void updateTrafficSelector(
            TrafficSelector.Builder selector, Set<TextualPacketFilter> relevantFilters) {
        // Link layer rule construction
        this.updateLinkLayerSelector(selector);
        // Network layer rule construction
        this.updateNetworkLayerSelector(selector);
        // Transport layer rule construction
        this.updateTransportLayerSelector(selector, relevantFilters);
    }

    /**
     * Integrates protocol-specific packet filters into a traffic selector object.
     * These fields are prerequisites when building selectors deeper in the header.
     *
     * @param selector a traffic selector to integrate the protocol packet filters into
     * @param relevantFilters relevant packet filters in the same rule that might affect this selector
     */
    public static void updateAbstractSelector(
            TrafficSelector.Builder selector, Set<TextualPacketFilter> relevantFilters) {
        for (TextualPacketFilter pf : relevantFilters) {
            // Cannot have IP matches without EthType = 0x800 ;)
            if (pf.isIp()) {
                short ethType =
                    ClassificationSyntax.ETHER_PROTO_MAP.get(ClassificationSyntax.ETHER_PROTO_IPV4).shortValue();
                selector.matchEthType(ethType);
                break;
            // Cannot have UDP matches without IP proto = 0x11 ;)
            } else if (pf.isUdp()) {
                byte ipProto = ClassificationSyntax.IPV4_PROTO_MAP.get(ClassificationSyntax.IPV4_PROTO_UDP);
                selector.matchIPProtocol(ipProto);
                break;
            // Cannot have TCP matches without IP proto = 0x06 ;)
            } else if (pf.isTcp()) {
                byte ipProto = ClassificationSyntax.IPV4_PROTO_MAP.get(ClassificationSyntax.IPV4_PROTO_TCP);
                selector.matchIPProtocol(ipProto);
                break;
            // Cannot have ICMP matches without IP proto = 0x01 ;)
            } else if (pf.isIcmp()) {
                byte ipProto = ClassificationSyntax.IPV4_PROTO_MAP.get(ClassificationSyntax.IPV4_PROTO_ICMP);
                selector.matchIPProtocol(ipProto);
                break;
            }
        }
    }

    /**
     * Integrates a link layer packet filter into a traffic selector object.
     *
     * @param selector a traffic selector to integrate the link layer packet filter into
     */
    private void updateLinkLayerSelector(TrafficSelector.Builder selector) {
        // Ethernet protocol
        if (this.headerField.equals(ClassificationSyntax.PATTERN_ETHER_PROTO)) {
            short proto = ClassificationSyntax.ETHER_PROTO_MAP.get(this.headerValue).shortValue();
            if (proto == ClassificationSyntax.ETHER_PROTO_WILDCARD.shortValue()) {
                selector.add(Criteria.dummy());
                log.debug("ETH Proto: DUMMY");
            } else {
                selector.matchEthType(proto);
                log.debug("ETH Proto: {}", proto);
            }
        }

        // ARP type
        if (this.headerField.equals(ClassificationSyntax.PATTERN_ARP_TYPE)) {
            int arpType = ClassificationSyntax.ARP_TYPE_MAP.get(this.headerValue).intValue();
            selector.matchArpOp(arpType);
            log.debug("ARP Type: {}", arpType);
        }

        // VLAN type
        if (this.headerField.equals(ClassificationSyntax.PATTERN_VLAN_TYPE)) {
            short vlanType = ClassificationSyntax.VLAN_TYPE_MAP.get(this.headerValue).shortValue();
            // TODO: Check correctness
            selector.matchEthType(vlanType);
            log.debug("VLAN Type: {}", vlanType);
        }

        // Ethernet addresses
        if (ClassificationSyntax.MAC_ADDR_PATTERNS.contains(this.headerField)) {
            MacAddress mac = MacAddress.valueOf(this.headerValue);

            if (this.headerField.equals(ClassificationSyntax.PATTERN_SRC_ETHER_HOST)) {
                log.debug("Src MAC address: {}", mac.toString());
                selector.matchEthSrc(mac);
            } else if (this.headerField.equals(ClassificationSyntax.PATTERN_DST_ETHER_HOST)) {
                log.debug("Dst MAC address: {}", mac.toString());
                selector.matchEthDst(mac);
            }
        }
    }

    /**
     * Integrates a network layer packet filter into a traffic selector object.
     *
     * @param selector a traffic selector to integrate the network layer packet filter into
     */
    private void updateNetworkLayerSelector(TrafficSelector.Builder selector) {
        // IP addresses
        if (ClassificationSyntax.IP_ADDR_PATTERNS.contains(this.headerField)) {
            // For IP addresses without prefix, append a host prefix
            if (!this.headerValue.contains("/")) {
                this.headerValue += "/32";
            }

            // This value is already verified
            IpPrefix ip = IpPrefix.valueOf(this.headerValue);
            log.debug("IP address: {}", ip.toString());

            // Src
            if (this.headerField.equals(ClassificationSyntax.PATTERN_SRC_HOST) ||
                this.headerField.equals(ClassificationSyntax.PATTERN_SRC_NET)) {
                selector.matchIPSrc(ip);
            // Dst
            // TODO: Handle gateway
            } else if (
                this.headerField.equals(ClassificationSyntax.PATTERN_DST_HOST) ||
                this.headerField.equals(ClassificationSyntax.PATTERN_DST_NET)) {
                // this.headerField.equals(ClassificationSyntax.PATTERN_GW)) {
                selector.matchIPDst(ip);
            }
        }

        // IP differentiated services code point (DSCP)
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_DSCP)) {
            byte ipDscp = Byte.valueOf(this.headerValue);
            selector.matchIPDscp(ipDscp);
            log.debug("IP DSCP: {}", ipDscp);
        }

        // IP ECN Capable Transport (ECT)
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_ECT)) {
            selector.matchIPEcn(ClassificationSyntax.IPV4_ECT_CT);
            log.debug("IP ECN CT: {}", ClassificationSyntax.IPV4_ECT_CT);
        }

        // IP ECN Congestion Experienced (CE)
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_CE)) {
            selector.matchIPEcn(ClassificationSyntax.IPV4_ECT_CE);
            log.debug("IP ECN CE: {}", ClassificationSyntax.IPV4_ECT_CE);
        }

        // IP protocol
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_PROTO)) {
            // Convert a string-based protocol name into a byte
            byte ipProto = ClassificationSyntax.IPV4_PROTO_MAP.get(this.headerValue);
            selector.matchIPProtocol(ipProto);
            log.debug("IP Proto: {}", ipProto);
        }

        // ICMP type
        if (headerField.equals(ClassificationSyntax.PATTERN_ICMP_TYPE)) {
            byte icmpType = Byte.valueOf(this.headerValue);
            selector.matchIcmpType(icmpType);
            log.debug("ICMP type: {}", icmpType);
        }
    }

    /**
     * Integrates a transport layer packet filter into a traffic selector object.
     *
     * @param selector a traffic selector to integrate the transport layer packet filter into
     * @param relevantFilters relevant packet filters in the same rule that might affect this selector
     */
    private void updateTransportLayerSelector(
            TrafficSelector.Builder selector, Set<TextualPacketFilter> relevantFilters) {
        if (ClassificationSyntax.PORT_PATTERNS.contains(this.headerField)) {
            TpPort port = TpPort.tpPort(Integer.parseInt(this.headerValue));
            log.debug("Transport port number: {}", port.toString());

            if (this.headerField.equals(ClassificationSyntax.PATTERN_SRC_PORT) ||
                this.headerField.equals(ClassificationSyntax.PATTERN_DST_PORT)) {

                /**
                 * Check if the other packet filters of the same rule can make
                 * this header field more specific.
                 */
                String ipProto = "";
                for (TextualPacketFilter pf : relevantFilters) {
                    if (pf.isUdp()) {
                        ipProto = "udp";
                        break;
                    } else if (pf.isTcp()) {
                        ipProto = "tcp";
                        break;
                    }
                }

                // No chance
                if (ipProto.isEmpty()) {
                    throw new ParseException(
                        "OpenFlow requires explicit protocol when matching transport layer ports. " +
                        "Use patters [src/dst] [tcp/udp] port"
                    );
                }

                // Translate this header field into a more specific one
                if (ipProto.equals("udp") &&
                    this.headerField.equals(ClassificationSyntax.PATTERN_SRC_PORT)) {
                    this.headerField = ClassificationSyntax.PATTERN_SRC_UDP_PORT;
                } else if (ipProto.equals("udp") &&
                    this.headerField.equals(ClassificationSyntax.PATTERN_DST_PORT)) {
                    this.headerField = ClassificationSyntax.PATTERN_DST_UDP_PORT;
                } else if (ipProto.equals("tcp") &&
                    this.headerField.equals(ClassificationSyntax.PATTERN_SRC_PORT)) {
                    this.headerField = ClassificationSyntax.PATTERN_SRC_TCP_PORT;
                } else {
                    this.headerField = ClassificationSyntax.PATTERN_DST_TCP_PORT;
                }
            }

            // UDP
            if (this.headerField.equals(ClassificationSyntax.PATTERN_SRC_UDP_PORT) ||
                this.headerField.equals(ClassificationSyntax.PATTERN_DST_UDP_PORT)) {
                byte ipProto = ClassificationSyntax.IPV4_PROTO_MAP.get(ClassificationSyntax.IPV4_PROTO_UDP);
                selector.matchIPProtocol(ipProto);

                if (this.headerField.equals(ClassificationSyntax.PATTERN_SRC_UDP_PORT)) {
                    selector.matchUdpSrc(port);
                } else if (this.headerField.equals(ClassificationSyntax.PATTERN_DST_UDP_PORT)) {
                    selector.matchUdpDst(port);
                }
            // TCP
            } else {
                byte ipProto = ClassificationSyntax.IPV4_PROTO_MAP.get(ClassificationSyntax.IPV4_PROTO_TCP);
                selector.matchIPProtocol(ipProto);

                if (this.headerField.equals(ClassificationSyntax.PATTERN_SRC_TCP_PORT)) {
                    selector.matchTcpSrc(port);
                } else if (this.headerField.equals(ClassificationSyntax.PATTERN_DST_TCP_PORT)) {
                    selector.matchTcpDst(port);
                }
            }
        }
    }

    public String filtersToString() {
        String negation = this.isNegated ? "!" : "";
        return  negation + this.headerField + " " + this.operator + " " +
                this.headerValue;
    }

    public String actionToString() {
        return this.isAllowed ? "allow" : "drop";
    }

    @Override
    public String toString() {
        return filtersToString() + " --> " + actionToString();
    }

}
