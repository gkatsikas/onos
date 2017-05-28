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

import org.onlab.packet.Ethernet;
import org.onlab.packet.ARP;
import org.onlab.packet.IPv4;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.ListUtils;

/**
 * Syntactic sugar for a binary classification tree.
 */
public final class ClassificationSyntax {

    /////////////////////////////////////////////////////////////////////////////////////
    // Syntax to formulate an operator that glues patterns together
    /////////////////////////////////////////////////////////////////////////////////////
    public static final String LOGICAL_OR  = "or";
    public static final String LOGICAL_AND = "and";
    public static final String LOGICAL_NOT = "not";

    public static final List<String> LOGICAL_OPERATORS = Arrays.asList(
        "and", "&&", "or", "||", "not", "!"
    );

    public static final List<String> PARENTHESES = Arrays.asList(
        "(", ")"
    );

    public static final List<String> SYMBOLS = ListUtils.union(
        LOGICAL_OPERATORS, PARENTHESES
    );
    /////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////////////
    // Syntax to formulate a pattern
    /////////////////////////////////////////////////////////////////////////////////////
    public static final String PATTERN_ETHER_PROTO    = "ether proto";
    public static final String PATTERN_IP_PROTO       = "ip proto";
    public static final String PATTERN_IP_POS         = "ip";

    public static final String PATTERN_ARP_TYPE       = "arp type";
    public static final String PATTERN_VLAN_TYPE      = "vlan type";

    public static final String PATTERN_SRC_ETHER_HOST = "src ether host";
    public static final String PATTERN_DST_ETHER_HOST = "dst ether host";
    public static final String PATTERN_SRC_HOST       = "src host";
    public static final String PATTERN_DST_HOST       = "dst host";

    public static final String PATTERN_SRC_NET        = "src net";
    public static final String PATTERN_DST_NET        = "dst net";
    public static final String PATTERN_GW             = "gw";

    public static final String PATTERN_ARP_REQ        = "arp req";
    public static final String PATTERN_ARP_RES        = "arp res";

    public static final String PATTERN_IP_VERS        = "ip vers";
    public static final String PATTERN_IP_HDR_LEN     = "ip hl";
    public static final String PATTERN_IP_ID          = "ip id";
    public static final String PATTERN_IP_TOS         = "ip tos";
    public static final String PATTERN_IP_DSCP        = "ip dscp";
    public static final String PATTERN_IP_ECT         = "ip ect";
    public static final String PATTERN_IP_CE          = "ip ce";
    public static final String PATTERN_IP_TTL         = "ip ttl";
    public static final String PATTERN_IP_FRAG        = "ip frag";
    public static final String PATTERN_IP_UNFRAG      = "ip unfrag";

    public static final String PATTERN_ICMP_TYPE      = "icmp type";

    public static final String PATTERN_SRC_PORT       = "src port";
    public static final String PATTERN_DST_PORT       = "dst port";
    public static final String PATTERN_SRC_UDP_PORT   = "src udp port";
    public static final String PATTERN_DST_UDP_PORT   = "dst udp port";
    public static final String PATTERN_SRC_TCP_PORT   = "src tcp port";
    public static final String PATTERN_DST_TCP_PORT   = "dst tcp port";

    public static final String PATTERN_TCP_OPT        = "tcp opt";
    public static final String PATTERN_TCP_WIN        = "tcp win";

    public static final String PATTERN_TRUE           = "true";
    public static final String PATTERN_FALSE          = "false";
    public static final String PATTERN_DENY_ALL       = "deny all";
    public static final String PATTERN_DROP_ALL       = "drop all";

    // Operators within patterns
    public static final String PATTERN_OPERATOR_DEF   = "";
    public static final String PATTERN_OPERATOR_EQ    = "==";
    public static final String PATTERN_OPERATOR_NE    = "!=";
    public static final String PATTERN_OPERATOR_LT    = "<";
    public static final String PATTERN_OPERATOR_LE    = "<=";
    public static final String PATTERN_OPERATOR_GT    = ">";
    public static final String PATTERN_OPERATOR_GE    = ">=";

    // Values for different patterns
    public static final String ETHER_PROTO_ARP    = "arp";
    public static final String ETHER_PROTO_VLAN   = "vlan";
    public static final String ETHER_PROTO_MPLS   = "mpls";
    public static final String ETHER_PROTO_IPV4   = "ipv4";
    public static final String ETHER_PROTO_IPV6   = "ipv6";
    public static final String ETHER_PROTO_ANY    = "any";

    public static final String ARP_PROTO_REQ      = "req";
    public static final String ARP_PROTO_RES      = "res";

    public static final String VLAN_TYPE_IPV4     = "ipv4";
    public static final String VLAN_TYPE_IPV6     = "ipv6";

    public static final String IPV4_PROTO_ICMP    = "icmp";
    public static final String IPV4_PROTO_IGMP    = "igmp";
    public static final String IPV4_PROTO_TCP     = "tcp";
    public static final String IPV4_PROTO_UDP     = "udp";
    public static final String IPV4_PROTO_PIM     = "pim";

    public static final byte   IPV4_ECT_CT        = 0x1;
    public static final byte   IPV4_ECT_CE        = 0x3;

    public static final String TCP_OPT_ACK        = "ack";
    public static final String TCP_OPT_FIN        = "fin";
    public static final String TCP_OPT_PSH        = "psh";
    public static final String TCP_OPT_RST        = "rst";
    public static final String TCP_OPT_SYN        = "syn";
    public static final String TCP_OPT_URG        = "urg";

    public static final String[] VALUE_ETHER_PROTO = {
        ETHER_PROTO_ARP,  ETHER_PROTO_VLAN, ETHER_PROTO_MPLS,
        ETHER_PROTO_IPV4, ETHER_PROTO_IPV6, ETHER_PROTO_ANY
    };
    public static final String[] VALUE_ARP_TYPE = {
        ARP_PROTO_REQ, ARP_PROTO_RES
    };
    public static final String[] VALUE_VLAN_TYPE = {
        VLAN_TYPE_IPV4, VLAN_TYPE_IPV6
    };
    public static final String[] VALUE_IP_PROTO = {
        IPV4_PROTO_ICMP, String.valueOf(IPv4.PROTOCOL_ICMP),
        IPV4_PROTO_UDP,  String.valueOf(IPv4.PROTOCOL_UDP),
        IPV4_PROTO_TCP,  String.valueOf(IPv4.PROTOCOL_TCP)
    };
    public static final String[] VALUE_TCP_OPT = {
        TCP_OPT_ACK, TCP_OPT_FIN, TCP_OPT_PSH,
        TCP_OPT_RST, TCP_OPT_SYN, TCP_OPT_URG
    };

    public static final Short ETHER_PROTO_WILDCARD = new Short((short) -1);

    public static final long VALUE_PROTO_ICMP    = 1;
    public static final long VALUE_PROTO_TCP     = 6;
    public static final long VALUE_PROTO_UDP     = 17;

    public static final int VALUE_MAX_IP_VERS    = 15;
    public static final int VALUE_MAX_HDR_LEN    = 15;
    public static final int VALUE_MAX_IP_ID      = (int) Math.pow(2, 16) - 1;
    public static final int VALUE_MAX_IP_TOS     = 255;
    public static final int VALUE_MAX_IP_DSCP    = (int) Math.pow(2,  6) - 1;
    public static final int VALUE_MAX_IP_TTL     = 255;
    public static final int VALUE_MAX_ICMP_TYPE  = 255;
    public static final int VALUE_MAX_TCP_WINDOW = (int) Math.pow(2, 16) - 1;
    /////////////////////////////////////////////////////////////////////////////////////

    // Index all valid patterns for operands
    public static final List<String> PATTERNS = Arrays.asList(
        PATTERN_ETHER_PROTO, PATTERN_IP_PROTO,
        PATTERN_ARP_TYPE, PATTERN_VLAN_TYPE,
        PATTERN_SRC_ETHER_HOST, PATTERN_DST_ETHER_HOST, PATTERN_SRC_HOST, PATTERN_DST_HOST,
        PATTERN_SRC_NET, PATTERN_DST_NET, PATTERN_GW,
        PATTERN_IP_VERS, PATTERN_IP_HDR_LEN, PATTERN_IP_ID, PATTERN_IP_TOS, PATTERN_IP_DSCP,
        PATTERN_IP_ECT, PATTERN_IP_CE, PATTERN_IP_TTL, PATTERN_IP_FRAG, PATTERN_IP_UNFRAG,
        PATTERN_ICMP_TYPE,
        PATTERN_SRC_PORT, PATTERN_DST_PORT,
        PATTERN_SRC_UDP_PORT, PATTERN_DST_UDP_PORT, PATTERN_SRC_TCP_PORT, PATTERN_DST_TCP_PORT,
        PATTERN_TCP_OPT, PATTERN_TCP_WIN,
        PATTERN_TRUE, PATTERN_FALSE, PATTERN_DENY_ALL, PATTERN_DROP_ALL
    );

    // Index all arithmetic operators
    public static final List<String> PATTERN_OPERATORS = Arrays.asList(
        PATTERN_OPERATOR_DEF,
        PATTERN_OPERATOR_EQ, PATTERN_OPERATOR_NE,
        PATTERN_OPERATOR_LT, PATTERN_OPERATOR_LE,
        PATTERN_OPERATOR_GT, PATTERN_OPERATOR_GE
    );

    // Supported operators
    public static final List<String> SUPPORTED_PATTERN_OPERATORS = Arrays.asList(
        PATTERN_OPERATOR_DEF, PATTERN_OPERATOR_EQ
    );

    // TODO: Provide support for these operators
    public static final List<String> UNSUPPORTED_PATTERN_OPERATORS = Arrays.asList(
        PATTERN_OPERATOR_NE,
        PATTERN_OPERATOR_LT, PATTERN_OPERATOR_LE,
        PATTERN_OPERATOR_GT, PATTERN_OPERATOR_GE
    );

    // Index the patterns related to MAC addresses
    public static final List<String> MAC_ADDR_PATTERNS = Arrays.asList(
        PATTERN_SRC_ETHER_HOST, PATTERN_DST_ETHER_HOST
    );

    // Index the patterns related to IP addresses
    public static final List<String> IP_ADDR_PATTERNS = Arrays.asList(
        PATTERN_SRC_HOST,
        PATTERN_DST_HOST,
        PATTERN_SRC_NET,
        PATTERN_DST_NET,
        PATTERN_GW
    );

    // Index the patterns related to transport ports
    public static final List<String> PORT_PATTERNS = Arrays.asList(
        PATTERN_SRC_PORT,     PATTERN_DST_PORT,
        PATTERN_SRC_UDP_PORT, PATTERN_DST_UDP_PORT,
        PATTERN_SRC_TCP_PORT, PATTERN_DST_TCP_PORT
    );

    // Index the patterns related to the Ethernet protocol
    public static final List<String> ETHERNET_PATTERNS = Arrays.asList(
        PATTERN_ETHER_PROTO, PATTERN_SRC_ETHER_HOST, PATTERN_DST_ETHER_HOST
    );

    // Index the patterns related to the ARP protocol
    public static final List<String> ARP_PATTERNS = Arrays.asList(
        PATTERN_ARP_REQ, PATTERN_ARP_RES
    );

    // Index the patterns related to the IP protocol
    public static final List<String> IP_PATTERNS = Arrays.asList(
        PATTERN_IP_VERS, PATTERN_IP_HDR_LEN, PATTERN_IP_ID,
        PATTERN_IP_TOS, PATTERN_IP_DSCP, PATTERN_IP_ECT,
        PATTERN_IP_CE, PATTERN_IP_TTL, PATTERN_IP_PROTO,
        PATTERN_IP_FRAG, PATTERN_IP_UNFRAG,
        PATTERN_SRC_HOST, PATTERN_DST_HOST,
        PATTERN_SRC_NET, PATTERN_DST_NET, PATTERN_GW
    );

    // Index the patterns related to the TCP protocol
    public static final List<String> TCP_PATTERNS = Arrays.asList(
        PATTERN_SRC_TCP_PORT, PATTERN_DST_TCP_PORT,
        PATTERN_TCP_OPT, PATTERN_TCP_WIN
    );

    // Index the patterns related to the UDP protocol
    public static final List<String> UDP_PATTERNS = Arrays.asList(
        PATTERN_SRC_UDP_PORT, PATTERN_DST_UDP_PORT
    );

    // Index the patterns related to the ICMP protocol
    public static final List<String> ICMP_PATTERNS = Arrays.asList(
        PATTERN_ICMP_TYPE
    );

    /**
     * Shows how many arguments (at maximum) are expected after each pattern.
     * For example, `ip proto == tcp` contains 2 strings after `ip proto`,
     * but it can also be expressed as `ip proto tcp` (== is implied).
     */
    public static final Map<String, Integer> PATTERN_MATCH_INFO = Collections.unmodifiableMap(
        new ConcurrentHashMap<String, Integer>() {
            {
                put(PATTERN_ETHER_PROTO,    new Integer(2));
                put(PATTERN_IP_PROTO,       new Integer(2));
                put(PATTERN_ARP_TYPE,       new Integer(2));
                put(PATTERN_VLAN_TYPE,      new Integer(2));
                put(PATTERN_SRC_ETHER_HOST, new Integer(2));
                put(PATTERN_DST_ETHER_HOST, new Integer(2));
                put(PATTERN_SRC_HOST,       new Integer(2));
                put(PATTERN_DST_HOST,       new Integer(2));
                put(PATTERN_SRC_NET,        new Integer(2));
                put(PATTERN_DST_NET,        new Integer(2));
                put(PATTERN_GW,             new Integer(2));
                put(PATTERN_IP_VERS,        new Integer(2));
                put(PATTERN_IP_HDR_LEN,     new Integer(2));
                put(PATTERN_IP_ID,          new Integer(2));
                put(PATTERN_IP_TOS,         new Integer(2));
                put(PATTERN_IP_DSCP,        new Integer(2));
                put(PATTERN_IP_ECT,         new Integer(0));
                put(PATTERN_IP_CE,          new Integer(0));
                put(PATTERN_IP_TTL,         new Integer(2));
                put(PATTERN_IP_FRAG,        new Integer(0));
                put(PATTERN_IP_UNFRAG,      new Integer(0));
                put(PATTERN_ICMP_TYPE,      new Integer(2));
                put(PATTERN_SRC_PORT,       new Integer(2));
                put(PATTERN_DST_PORT,       new Integer(2));
                put(PATTERN_SRC_UDP_PORT,   new Integer(2));
                put(PATTERN_DST_UDP_PORT,   new Integer(2));
                put(PATTERN_SRC_TCP_PORT,   new Integer(2));
                put(PATTERN_DST_TCP_PORT,   new Integer(2));
                put(PATTERN_TCP_OPT,        new Integer(2));
                put(PATTERN_TCP_WIN,        new Integer(2));
                put(PATTERN_TRUE,           new Integer(0));
                put(PATTERN_FALSE,          new Integer(0));
                put(PATTERN_DENY_ALL,       new Integer(0));
                put(PATTERN_DROP_ALL,       new Integer(0));
            }
        }
    );

    // Maps certain header fields to a list of candidate values
    public static final Map<String, String[]> PATTERN_MATCH_MAP = Collections.unmodifiableMap(
        new ConcurrentHashMap<String, String[]>() {
            {
                put(PATTERN_ETHER_PROTO, VALUE_ETHER_PROTO);
                put(PATTERN_IP_PROTO,    VALUE_IP_PROTO);
                put(PATTERN_ARP_TYPE,    VALUE_ARP_TYPE);
                put(PATTERN_VLAN_TYPE,   VALUE_VLAN_TYPE);
                put(PATTERN_TCP_OPT,     VALUE_TCP_OPT);
            }
        }
    );

    // Maps Ethernet protocol types to their values
    public static final Map<String, Short> ETHER_PROTO_MAP  = Collections.unmodifiableMap(
        new ConcurrentHashMap<String, Short>() {
            {
                put(ETHER_PROTO_ARP,  new Short(Ethernet.TYPE_ARP));
                put(ETHER_PROTO_VLAN, new Short(Ethernet.TYPE_VLAN));
                put(ETHER_PROTO_MPLS, new Short(Ethernet.MPLS_UNICAST));
                put(ETHER_PROTO_IPV4, new Short(Ethernet.TYPE_IPV4));
                put(ETHER_PROTO_IPV6, new Short(Ethernet.TYPE_IPV6));
                put(ETHER_PROTO_ANY,  ETHER_PROTO_WILDCARD);
            }
        }
    );

    // Maps ARP types to their values
    public static final Map<String, Integer> ARP_TYPE_MAP = Collections.unmodifiableMap(
        new ConcurrentHashMap<String, Integer>() {
            {
                put(ARP_PROTO_REQ, new Integer(ARP.OP_REQUEST));
                put(ARP_PROTO_RES, new Integer(ARP.OP_REPLY));
            }
        }
    );

    // Maps VLAN types to their values
    public static final Map<String, Short> VLAN_TYPE_MAP = Collections.unmodifiableMap(
        new ConcurrentHashMap<String, Short>() {
            {
                put(VLAN_TYPE_IPV4, new Short(Ethernet.TYPE_IPV4));
                put(VLAN_TYPE_IPV6, new Short(Ethernet.TYPE_IPV6));
            }
        }
    );

    // Maps IPv4 protocol types to their values
    public static final Map<String, Byte> IPV4_PROTO_MAP = Collections.unmodifiableMap(
        new ConcurrentHashMap<String, Byte>() {
            {
                // String-based format
                put(IPV4_PROTO_ICMP, IPv4.PROTOCOL_ICMP);
                put(IPV4_PROTO_IGMP, IPv4.PROTOCOL_IGMP);
                put(IPV4_PROTO_TCP,  IPv4.PROTOCOL_TCP);
                put(IPV4_PROTO_UDP,  IPv4.PROTOCOL_UDP);
                put(IPV4_PROTO_PIM,  IPv4.PROTOCOL_PIM);
                // Numerical format
                put(String.valueOf(IPv4.PROTOCOL_ICMP), IPv4.PROTOCOL_ICMP);
                put(String.valueOf(IPv4.PROTOCOL_IGMP), IPv4.PROTOCOL_IGMP);
                put(String.valueOf(IPv4.PROTOCOL_TCP),  IPv4.PROTOCOL_TCP);
                put(String.valueOf(IPv4.PROTOCOL_UDP),  IPv4.PROTOCOL_UDP);
                put(String.valueOf(IPv4.PROTOCOL_PIM),  IPv4.PROTOCOL_PIM);
            }
        }
    );

    /**
     * Utility class with private constructor.
     */
    private ClassificationSyntax() {}

}
