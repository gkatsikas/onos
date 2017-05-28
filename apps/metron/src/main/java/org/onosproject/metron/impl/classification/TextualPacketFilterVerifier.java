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

package org.onosproject.metron.impl.classification;

import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.ClassificationSyntax;
import org.onosproject.metron.api.exceptions.ParseException;

import org.onlab.packet.TpPort;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Verifies packet classification patterns.
 */
public final class TextualPacketFilterVerifier {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private TextualPacketFilter tuple = null;

    public TextualPacketFilterVerifier(TextualPacketFilter tuple) {
        this.tuple = tuple;
    }

    /**
     * Verifies that the operator and value of a certain packet filter are valid.
     */
    public void verify() {
        if (this.tuple == null) {
            throw new ParseException(
                "NULL expression is given to the classifier."
            );
        }

        String headerField = this.tuple.headerField();
        String operator    = this.tuple.operator();
        String headerValue = this.tuple.headerValue();

        String expression = headerField + " " + operator + " " + headerValue;

        // Verify the operator
        if (!ClassificationSyntax.PATTERN_OPERATORS.contains(operator)) {
            throw new ParseException(
                "Invalid operator \"" + operator + "\" in expression: " + expression
            );
        }

        // TODO: Fix this
        if (ClassificationSyntax.UNSUPPORTED_PATTERN_OPERATORS.contains(operator) &&
            !headerField.equals(ClassificationSyntax.PATTERN_IP_TTL)) {
            throw new ParseException(
                "Range matches using " + operator + " are not currently supported. " +
                "Use only: " + String.join(",", ClassificationSyntax.SUPPORTED_PATTERN_OPERATORS)
            );
        }

        // An empty header value is expected only for certain header fields (e.g., ip ce)
        if (headerValue.isEmpty() && ClassificationSyntax.PATTERN_MATCH_INFO.get(headerField).intValue() > 0) {
            throw new ParseException(
                "Empty header value in expression: " + expression
            );
        }

        /**
         * In some cases, the value belongs to a rather limited set of possible values.
         * Check if the value does actually belong to this set.
         */
        if (ClassificationSyntax.PATTERN_MATCH_MAP.containsKey(headerField)) {
            String[] validValues = ClassificationSyntax.PATTERN_MATCH_MAP.get(headerField);
            if (!Arrays.asList(validValues).contains(headerValue)) {
                throw new ParseException(
                    "Invalid value \"" + headerValue + "\" in expression: " + expression
                );
            }
        }

        // Verify the different layers of the protocol stack of this packet filter
        if (!this.verifyLinkLayerPacketFilter(headerField, headerValue)) {
            throw new ParseException(
                "Unable to verify the link layer patterns of the expression: " + expression
            );
        }

        if (!this.verifyNetworkLayerPacketFilter(headerField, headerValue)) {
            throw new ParseException(
                "Unable to verify the network layer patterns of the expression: " + expression
            );
        }

        if (!this.verifyTransportLayerPacketFilter(headerField, headerValue)) {
            throw new ParseException(
                "Unable to verify the transport layer patterns of the expression: " + expression
            );
        }
    }

    /**
     * Verifies that a link layer (L2) packet filter is correct.
     *
     * @param the matched header field
     * @param the value of the header field
     * @return boolean status
     */
    private boolean verifyLinkLayerPacketFilter(String headerField, String headerValue) {
        // Search for packet filter related to MAC addresses
        if (ClassificationSyntax.MAC_ADDR_PATTERNS.contains(headerField)) {
            // Verify that the header value is a MAC address
            MacAddress mac = null;
            try {
                if (headerValue.contains(":")) {
                    mac = MacAddress.valueOf(headerValue);
                } else {
                    mac = MacAddress.valueOf(Long.parseLong(headerValue));
                }
            } catch (IllegalArgumentException iaEx) {
                log.error("Invalid MAC address {} with error: {}", headerValue, iaEx.toString());
                return false;
            }
            log.debug("Valid MAC address: {}", mac.toString());
        }

        return true;
    }

    /**
     * Verifies that a network layer (L3) packet filter is correct.
     *
     * @param the matched header field
     * @param the value of the header field
     * @return boolean status
     */
    private boolean verifyNetworkLayerPacketFilter(String headerField, String headerValue) {
        // Search for packet filter related to IP addresses
        if (ClassificationSyntax.IP_ADDR_PATTERNS.contains(headerField)) {
            // For IP addresses without prefix, append a host prefix
            if (!headerValue.contains("/")) {
                headerValue += "/32";
            }

            // Verify that the header value is a valid IP prefix
            IpPrefix ip = null;
            try {
                ip = IpPrefix.valueOf(headerValue);
            } catch (IllegalArgumentException iaEx) {
                log.error("Invalid IP address {} with error: {}", headerValue, iaEx.toString());
                return false;
            }
            log.debug("Valid IP address: {}", ip.toString());
        }

        // IP version
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_VERS)) {
            int ipVer = Integer.parseInt(headerValue);
            if ((ipVer < 0) || (ipVer > ClassificationSyntax.VALUE_MAX_IP_VERS)) {
                log.error("Invalid IP version: {}", ipVer);
                return false;
            }
            log.debug("Valid IP version: {}", ipVer);
        }

        // IP header length
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_HDR_LEN)) {
            int ipHdrLen = Integer.parseInt(headerValue);
            if ((ipHdrLen < 0) || (ipHdrLen > ClassificationSyntax.VALUE_MAX_HDR_LEN)) {
                log.error("Invalid IP header length: {}", ipHdrLen);
                return false;
            }
            log.debug("Valid IP header length: {}", ipHdrLen);
        }

        // IP identifier
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_ID)) {
            int ipId = Integer.parseInt(headerValue);
            if ((ipId < 0) || (ipId > ClassificationSyntax.VALUE_MAX_IP_ID)) {
                log.error("Invalid IP identifier: {}", ipId);
                return false;
            }
            log.debug("Valid IP identifier: {}", ipId);
        }

        // IP type of service (ToS)
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_TOS)) {
            int ipTos = Integer.parseInt(headerValue);
            if ((ipTos < 0) || (ipTos > ClassificationSyntax.VALUE_MAX_IP_TOS)) {
                log.error("Invalid IP ToS: {}", ipTos);
                return false;
            }
            log.debug("Valid IP ToS: {}", ipTos);
        }

        // IP differentiated services code point (DSCP)
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_DSCP)) {
            int ipDscp = Integer.parseInt(headerValue);
            if ((ipDscp < 0) || (ipDscp > ClassificationSyntax.VALUE_MAX_IP_DSCP)) {
                log.error("Invalid IP DSCP: {}", ipDscp);
                return false;
            }
            log.debug("Valid IP DSCP: {}", ipDscp);
        }

        // IP differentiated services code point (DSCP)
        if (headerField.equals(ClassificationSyntax.PATTERN_IP_TTL)) {
            int ipTtl = Integer.parseInt(headerValue);
            if ((ipTtl < 0) || (ipTtl > ClassificationSyntax.VALUE_MAX_IP_TTL)) {
                log.error("Invalid IP TTL: {}", ipTtl);
                return false;
            }
            log.debug("Valid IP TTL: {}", ipTtl);
        }

        // ICMP type
        if (headerField.equals(ClassificationSyntax.PATTERN_ICMP_TYPE)) {
            int icmpType = Integer.parseInt(headerValue);
            if ((icmpType < 0) || (icmpType > ClassificationSyntax.VALUE_MAX_ICMP_TYPE)) {
                log.error("Invalid ICMP type: {}", icmpType);
                return false;
            }
            log.debug("Valid ICMP type: {}", icmpType);
        }

        return true;
    }

    /**
     * Verifies that a transport layer (L4) packet filter is correct.
     *
     * @param the matched header field
     * @param the value of the header field
     * @return boolean status
     */
    private boolean verifyTransportLayerPacketFilter(String headerField, String headerValue) {
        // Search for packet filter related to transport ports
        if (ClassificationSyntax.PORT_PATTERNS.contains(headerField)) {
            // Verify that the header value is a valid port
            int port = Integer.parseInt(headerValue);
            if ((port <= TpPort.MIN_PORT) || (port > TpPort.MAX_PORT)) {
                log.error("Invalid transport port number: {}", port);
                return false;
            }
            log.debug("Valid transport port number: {}", port);
        }

        // TCP window
        if (headerField.equals(ClassificationSyntax.PATTERN_TCP_WIN)) {
            int tcpWin = Integer.parseInt(headerValue);
            if ((tcpWin <= 0) || (tcpWin > ClassificationSyntax.VALUE_MAX_TCP_WINDOW)) {
                log.error("Invalid TCP window: {}", tcpWin);
                return false;
            }
            log.debug("Valid TCP window: {}", tcpWin);
        }

        return true;
    }

}
