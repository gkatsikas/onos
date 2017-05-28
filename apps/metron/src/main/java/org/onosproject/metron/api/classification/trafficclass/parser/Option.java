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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parsing options for IPFilter.
 */
public enum Option {

    ARP_HW_ADD_LEN("ARP HW ADD LEN"),
    ARP_PT_ADD_LEN("ARP PT ADD LEN"),
    ARP_HW_TYPE("ARP HW TYPE"),
    ARP_PT_TYPE("ARP PT TYPE"),
    ARP_OP("ARP OP"),
    ARP_SDR_HW("ARP SDR HW"),
    ARP_SDR_IP("ARP SDR IP"),
    ARP_TRG_HW("ARP TRG HW"),
    ARP_TRG_IP("ARP TRG IP"),
    DST_ETHER_HOST("DST ETHER HOST"),
    DST_HOST("DST HOST"),
    DST_NET("DST NET"),
    DST_PORT("DST PORT"),
    DST_TCP_PORT("DST TCP PORT"),
    DST_UDP_PORT("DST UDP PORT"),
    ETH_CRC("ETH CRC"),
    ETH_DST("ETH DST"),
    ETH_SRC("ETH SRC"),
    ETH_TYPE("ETH TYPE"),
    ICMP_CHS("ICMP CHS"),
    ICMP_CODE("ICMP CODE"),
    ICMP_ID("ICMP ID"),
    ICMP_SEQ("ICMP SEQ"),
    ICMP_TYPE("ICMP TYPE"),
    IP_CE("IP CE"),
    IP_CHS("IP CHS"),
    IP_DSCP("IP DSCP"),
    IP_DST("IP DST"),
    IP_ECT("IP ECT"),
    IP_FLAGS("IP FLAGS"),
    IP_FRAG("IP FRAG"),
    IP_IHL("IP HL"),
    IP_ID("IP ID"),
    IP_PROTO("IP PROTO"),
    IP_SRC("IP SRC"),
    IP_TL("IP TL"),
    IP_TOS("IP TOS"),
    IP_TTL("IP TTL"),
    IP_UNFRAG("IP UNFRAG"),
    IP_VERS("IP VERS"),
    IP_VERS_HL("IP VERS HL"),
    SRC_ETHER_HOST("SRC ETHER HOST"),
    SRC_HOST("SRC HOST"),
    SRC_NET("SRC NET"),
    SRC_PORT("SRC PORT"),
    SRC_TCP_PORT("SRC TCP PORT"),
    SRC_UDP_PORT("SRC UDP PORT"),
    TCP_ACK("TCP ACK"),
    TCP_CHS("TCP CHS"),
    TCP_FLAGS("TCP FLAGS"),
    TCP_OFF_RES("TCP OFF RES"),
    TCP_OPT("TCP OPT"),
    TCP_SEQ("TCP SEQ"),
    TCP_URG("TCP URG"),
    TCP_WIN("TCP WIN"),
    UDP_CHS("UDP CHS"),
    UDP_LEN("UDP LEN"),
    UNDEFINED("UNDEFINED");

    private String option;

    // Statically maps options to enum types
    private static final Map<String, Option> MAP =
        new ConcurrentHashMap<String, Option>();

    static {
        for (Option opt : Option.values()) {
            MAP.put(opt.toString(), opt);
        }
    }

    private Option(String option) {
        this.option = option;
    }

    /**
     * Returns an Option object created by a string-based option name.
     *
     * @param optStr string-based option
     * @return Option object
     */
    public static Option getByName(String optStr) {
        return MAP.get(optStr.toUpperCase());
    }

    /**
     * Returns whether an option contains no value or not.
     *
     * @param opt option
     * @return boolean has or does not have value
     */
    public static boolean hasNoValue(Option opt) {
        if ((opt == IP_ECT)  || (opt == IP_CE) ||
            (opt == IP_FRAG) || (opt == IP_UNFRAG)) {
            return true;
        }

        return false;
    }

    /**
     * Returns whether an option is Ethernet-based or not.
     *
     * @param opt option
     * @return boolean is Ethernet-based or not
     */
    public static boolean isEthernet(Option opt) {
        if ((opt == SRC_ETHER_HOST) || (opt == DST_ETHER_HOST)) {
            return true;
        }

        return false;
    }

    /**
     * Returns whether an option is network-based or not.
     *
     * @param opt option
     * @return boolean is network-based or not
     */
    public static boolean isNet(Option opt) {
        if ((opt == SRC_NET) || (opt == DST_NET)) {
            return true;
        }

        return false;
    }

    /**
     * Returns whether an option is port-based or not.
     *
     * @param opt option
     * @return boolean is port-based or not
     */
    public static boolean isPort(Option opt) {
        if ((opt == SRC_TCP_PORT) || (opt == DST_TCP_PORT) ||
            (opt == SRC_UDP_PORT) || (opt == DST_UDP_PORT)) {
            return true;
        }

        return false;
    }

    /**
     * Returns whether an option is undefined or not.
     *
     * @param opt option
     * @return boolean is undefined or not
     */
    public static boolean isUndefinedOption(Option opt) {
        return opt == UNDEFINED;
    }

    @Override
    public String toString() {
        return this.option;
    }

}
