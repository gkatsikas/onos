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

import org.onosproject.metron.api.classification.trafficclass.HeaderField;

import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class with useful constants for:
 * |-> Exception handling,
 * |-> User output,
 * |-> Lower and upper bounds.
 */
public final class Constants {

    private Constants() {}

    public static final int INPUT_CONFIGURATION_ERROR = 1;
    public static final int NETWORK_FUNCTION_ERROR    = 2;
    public static final int SERVICE_CHAIN_ERROR       = 3;
    public static final int SYNTHESIS_ERROR           = 4;
    public static final int PLACEMENT_ERROR           = 5;
    public static final int DEPLOYMENT_ERROR          = 6;
    public static final int ILLEGAL_ARGUMENT_ERROR    = 7;

    public static final long MAX_UINT = 0xFFFFFFFFL;
    public static final long MAX_LONG = Long.MAX_VALUE;
    public static final long MAX_PIPELINE_PORT_NB = 255;

    public static final String SYSTEM_PREFIX     = "org.onosproject.metron";
    public static final String SYSTEM_APP_PREFIX = SYSTEM_PREFIX + ".apps";

    public static final String STDOUT_BARS =
        "===========================================================" +
        "===========================================================";

    public static final String STDOUT_BARS_SUB =
        "==============================================================================================";

    /**
     * Maps a header field with the number of bits it occupies in the header.
     */
    public static final Map<HeaderField, Integer> HEADER_FIELD_BITS = Collections.unmodifiableMap(
        new ConcurrentHashMap<HeaderField, Integer>() {
            {
                put(HeaderField.ETHER_DST,     new Integer(48));
                put(HeaderField.ETHER_SRC,     new Integer(48));
                put(HeaderField.ICMP_CHS,      new Integer(16));
                put(HeaderField.ICMP_CODE,     new Integer(8));
                put(HeaderField.ICMP_ID,       new Integer(16));
                put(HeaderField.ICMP_SEQ,      new Integer(16));
                put(HeaderField.ICMP_TYPE,     new Integer(8));
                put(HeaderField.IP_CE,         new Integer(2));
                put(HeaderField.IP_CHS,        new Integer(16));
                put(HeaderField.IP_ECT,        new Integer(2));
                put(HeaderField.IP_DSCP,       new Integer(6));
                put(HeaderField.IP_DST,        new Integer(32));
                put(HeaderField.IP_ID,         new Integer(16));
                put(HeaderField.IP_IHL,        new Integer(4));
                put(HeaderField.IP_PROTO,      new Integer(8));
                put(HeaderField.IP_SRC,        new Integer(32));
                put(HeaderField.IP_TL,         new Integer(16));
                put(HeaderField.IP_TTL,        new Integer(8));
                put(HeaderField.IP_VERS,       new Integer(4));
                put(HeaderField.MTU,           new Integer(16));
                put(HeaderField.TCP_ACK,       new Integer(32));
                put(HeaderField.TCP_CHS,       new Integer(16));
                put(HeaderField.TCP_FLAGS,     new Integer(9));
                put(HeaderField.TCP_FLAGS_ACK, new Integer(1));
                put(HeaderField.TCP_FLAGS_FIN, new Integer(1));
                put(HeaderField.TCP_FLAGS_PSH, new Integer(1));
                put(HeaderField.TCP_FLAGS_RST, new Integer(1));
                put(HeaderField.TCP_FLAGS_SYN, new Integer(1));
                put(HeaderField.TCP_FLAGS_URG, new Integer(1));
                put(HeaderField.TCP_OFF_RES,   new Integer(7));
                put(HeaderField.TCP_OPT,       new Integer(32));
                put(HeaderField.TCP_SEQ,       new Integer(32));
                put(HeaderField.TCP_URG,       new Integer(16));
                put(HeaderField.TCP_WIN,       new Integer(16));
                put(HeaderField.TP_DST_PORT,   new Integer(16));
                put(HeaderField.TP_SRC_PORT,   new Integer(16));
                put(HeaderField.UDP_CHS,       new Integer(16));
                put(HeaderField.UDP_LEN,       new Integer(16));
                put(HeaderField.UNKNOWN,       new Integer(32));
                put(HeaderField.VLAN_DEI,      new Integer(1));
                put(HeaderField.VLAN_PCP,      new Integer(3));
                put(HeaderField.VLAN_VID,      new Integer(12));
            }
        }
    );

    /**
     * Maps a header field with the maximum value it can take.
     */
    public static final Map<HeaderField, Long> HEADER_FIELD_UPPER_BOUND = Collections.unmodifiableMap(
        new ConcurrentHashMap<HeaderField, Long>() {
            {
                put(HeaderField.ETHER_DST,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ETHER_DST)) - 1)
                );
                put(HeaderField.ETHER_SRC,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ETHER_SRC)) - 1)
                );
                put(HeaderField.ICMP_CHS,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ICMP_CHS)) - 1)
                );
                put(HeaderField.ICMP_CODE,     new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ICMP_CODE)) - 1)
                );
                put(HeaderField.ICMP_ID,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ICMP_ID)) - 1)
                );
                put(HeaderField.ICMP_SEQ,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ICMP_SEQ)) - 1)
                );
                put(HeaderField.ICMP_TYPE,     new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.ICMP_TYPE)) - 1)
                );
                put(HeaderField.IP_CE,         new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_CE)) - 1)
                );
                put(HeaderField.IP_CHS,        new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_CHS)) - 1)
                );
                put(HeaderField.IP_ECT,        new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_ECT)) - 1)
                );
                put(HeaderField.IP_DSCP,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_DSCP)) - 1)
                );
                put(HeaderField.IP_DST,        new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_DST)) - 1)
                );
                put(HeaderField.IP_ID,         new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_ID)) - 1)
                );
                put(HeaderField.IP_IHL,        new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_IHL)) - 1)
                );
                put(HeaderField.IP_PROTO,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_PROTO)) - 1)
                );
                put(HeaderField.IP_SRC,        new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_SRC)) - 1)
                );
                put(HeaderField.IP_TL,         new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_TL)) - 1)
                );
                put(HeaderField.IP_TTL,        new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_TTL)) - 1)
                );
                put(HeaderField.IP_VERS,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.IP_VERS)) - 1)
                );
                put(HeaderField.MTU,           new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.MTU)) - 1)
                );
                put(HeaderField.TCP_ACK,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_ACK)) - 1)
                );
                put(HeaderField.TCP_CHS,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_CHS)) - 1)
                );
                put(HeaderField.TCP_FLAGS,     new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS)) - 1)
                );
                put(HeaderField.TCP_FLAGS_ACK, new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS_ACK)) - 1)
                );
                put(HeaderField.TCP_FLAGS_FIN, new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS_FIN)) - 1)
                );
                put(HeaderField.TCP_FLAGS_PSH, new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS_PSH)) - 1)
                );
                put(HeaderField.TCP_FLAGS_RST, new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS_RST)) - 1)
                );
                put(HeaderField.TCP_FLAGS_SYN, new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS_SYN)) - 1)
                );
                put(HeaderField.TCP_FLAGS_URG, new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_FLAGS_URG)) - 1)
                );
                put(HeaderField.TCP_OFF_RES,   new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_OFF_RES)) - 1)
                );
                put(HeaderField.TCP_OPT,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_OPT)) - 1)
                );
                put(HeaderField.TCP_SEQ,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_SEQ)) - 1)
                );
                put(HeaderField.TCP_URG,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_URG)) - 1)
                );
                put(HeaderField.TCP_WIN,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TCP_WIN)) - 1)
                );
                put(HeaderField.TP_DST_PORT,   new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TP_DST_PORT)) - 1)
                );
                put(HeaderField.TP_SRC_PORT,   new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.TP_SRC_PORT)) - 1)
                );
                put(HeaderField.UDP_CHS,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.UDP_CHS)) - 1)
                );
                put(HeaderField.UDP_LEN,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.UDP_LEN)) - 1)
                );
                put(HeaderField.UNKNOWN,       new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.UNKNOWN)) - 1)
                );
                put(HeaderField.VLAN_DEI,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.VLAN_DEI)) - 1)
                );
                put(HeaderField.VLAN_PCP,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.VLAN_PCP)) - 1)
                );
                put(HeaderField.VLAN_VID,      new Long(
                    (long) Math.pow(2, HEADER_FIELD_BITS.get(HeaderField.VLAN_VID)) - 1)
                );
            }
        }
    );

}
