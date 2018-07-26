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

package org.onosproject.metron.api.classification.trafficclass.outputclass;

import org.onosproject.metron.api.exceptions.SynthesisException;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.classification.trafficclass.operation.FieldOperation;
import org.onosproject.metron.api.classification.trafficclass.operation.Operation;
import org.onosproject.metron.api.classification.trafficclass.operation.OperationType;
import org.onosproject.metron.api.classification.trafficclass.operation.StatelessOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatefulOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatefulSetOperationValue;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.structures.Pair;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A traffic class's output maps traffic class
 * filters to composite packet operations.
 */
public class OutputClass {

    private static final Logger log = getLogger(OutputClass.class);

    private static final int MAX_OUTPUT_PORT = 255;
    private static final int IPMAPPER_PATTERN_LENGTH = 6;
    private static final int IPREWRITER_PATTERN_LENGTH = 7;

    private int portNumber;
    private int nextInputPort;
    private NfvDataplaneBlockInterface child;

    private Operation    operation    = new Operation();
    private PacketFilter packetFilter = new PacketFilter();

    public OutputClass(int portNumber) {
        checkArgument(
            portNumber >= 0,
            "Invalid output port for output class"
        );

        this.portNumber    = portNumber;
        this.nextInputPort = 0;
        this.child = null;
    }

    /**
     * Returns the port number of this output class.
     *
     * @return port number of this output class
     */
    public int portNumber() {
        return this.portNumber;
    }

    /**
     * Sets the port number of this output class.
     *
     * @param portNumber port number of this output class
     */
    public void setPortNumber(int portNumber) {
        checkArgument(
            (portNumber >= 0) && (portNumber <= MAX_OUTPUT_PORT),
            "Invalid output port for output class"
        );

        this.portNumber = portNumber;
    }

    /**
     * Returns the next input port number of this output class.
     *
     * @return next input port number of this output class
     */
    public int nextInputPort() {
        return this.nextInputPort;
    }

    /**
     * Sets the next input port number of this output class.
     *
     * @param nextInputPort next input port number of this output class
     */
    public void setNextInputPort(int nextInputPort) {
        checkArgument(
            (nextInputPort >= 0) && (nextInputPort <= MAX_OUTPUT_PORT),
            "Invalid next input port for output class"
        );

        this.nextInputPort = nextInputPort;
    }

    /**
     * Returns the dataplane block associated with this output class.
     *
     * @return NfvDataplaneBlockInterface associated with this output class
     */
    public NfvDataplaneBlockInterface child() {
        return this.child;
    }

    /**
     * Sets the dataplane block associated with this output class.
     *
     * @param child NfvDataplaneBlockInterface associated with this output class
     */
    public void setChild(NfvDataplaneBlockInterface child) {
        checkNotNull(
            child,
            "Cannot assign NULL NFV Dataplane Block to output class"
        );

        this.child = child;
    }

    /**
     * Returns the field operation of this output class.
     *
     * @return Operation of this output class
     */
    public Operation operation() {
        return this.operation;
    }

    /**
     * Adds a new field operation to the operations of this output class.
     *
     * @param fieldOperation the field operation to be added
     */
    public void addFieldOperation(FieldOperation fieldOperation) {
        this.operation.addFieldOperation(fieldOperation);
    }

    /**
     * Returns the map of filters of this output class.
     *
     * @return the packet filter map of this output class
     */
    public PacketFilter packetFilter() {
        return this.packetFilter;
    }

    /**
     * Sets the map of filters of this output class.
     *
     * @param pf the packet filter map of this output class
     */
    public void setPacketFilter(PacketFilter pf) {
        checkNotNull(
            pf,
            "Cannot assign NULL packet filter to output class"
        );

        this.packetFilter = pf;
    }

    /**
     * Adds a new filter to the map of filters of this output class.
     * This filter is associated with the input header field.
     *
     * @param headerField where the filter applies to
     * @param filter to be applied
     */
    public void addFilter(HeaderField headerField, Filter filter) {
        if (this.packetFilter.containsKey(headerField)) {
            throw new SynthesisException(
                "Trying to add filter " + headerField +
                " on already filtered field in OutputClass"
            );
        }

        this.packetFilter.put(headerField, filter);
    }

    /**
     * Returns whether this output class contains any packet filters or not.
     *
     * @return boolean existence of packet filters
     */
    public boolean hasPacketFilters() {
        return !this.packetFilter.isEmpty();
    }

    /**
     * Returns whether this output class contains any operations or not.
     *
     * @return boolean existence of operations
     */
    public boolean hasOperations() {
        return this.operation.hasOperations() || this.operation.hasMonitors();
    }

    /**
     * Creates an output class out of a lookup rule.
     *
     * @param rule the lookup rule to be translated
     * @param parsedRules the existing filter of this rule
     * @return OutputClass an output class
     */
    public static OutputClass fromLookupRule(String rule, Filter parsedRules) {
        // Existing rules must only be on the destination IP address
        if (parsedRules.headerField() != HeaderField.IP_DST) {
            throw new SynthesisException(
                "Existing IPv4 prefixes point to invalid header field: " +
                parsedRules.headerField().toString() + ". Destination IP was expected");
        }

        // TODO: Check also for \t \n
        String[] decomposedRule = rule.split("\\s+");
        int argsNumber = decomposedRule.length;

        if ((argsNumber > 3) || (argsNumber < 2)) {
            throw new SynthesisException("Wrong lookup format: " + rule);
        }

        int portNumber = Integer.parseInt(decomposedRule[argsNumber - 1]);
        String[] addressAndMask = decomposedRule[0].split("/");
        Filter f;
        switch (addressAndMask.length) {
            case 1:
                f = new Filter(HeaderField.IP_DST, Common.stringIpToInt(decomposedRule[0]));
                break;
            case 2:
                f = Filter.fromIpv4Prefix(
                    HeaderField.IP_DST,
                    Common.stringIpToInt(addressAndMask[0]),
                    Integer.parseInt(addressAndMask[1])
                );
                break;
            default:
                throw new SynthesisException("Wrong lookup format: " + rule);
        }

        log.debug("From lookup rule {} --> Filter: {}", rule, f.toString());

        f = f.differentiate(parsedRules);

        log.debug("Differentiated Filter: {}", f.toString());

        parsedRules = parsedRules.unite(f);

        log.debug("United Parsed Filters: {}", parsedRules.toString());

        OutputClass port = new OutputClass(portNumber);
        port.addFilter(HeaderField.IP_DST, f);

        return port;
    }

    /**
     * Creates an output class out of an IPRewriter pattern.
     * Format: pattern SADDR SPORT DADDR DPORT FOUTPUT ROUTPUT
     *
     * @param pattern the pattern to be translated
     * @return OutputClass an output class
     */
    public static Pair<OutputClass, OutputClass> fromPattern(String[] pattern) {
        if (pattern.length != IPREWRITER_PATTERN_LENGTH) {
            throw new SynthesisException("Incorrect IPRewriter pattern size");
        }

        int unmodifiedPortNumber = Integer.parseInt(pattern[6]);
        int modifiedPortNumber   = Integer.parseInt(pattern[5]);

        OutputClass foutput = new OutputClass(modifiedPortNumber);

        // Source IP address
        if (!pattern[1].equals("-")) {
            long ipSrc = Common.stringIpToInt(pattern[1]);
            if (ipSrc < 0) {
                throw new SynthesisException(
                    "Invalid source IP address in IPRewriter pattern: " +
                    String.join(" ", pattern)
                );
            }

            StatelessOperationValue stlOp = new StatelessOperationValue(ipSrc);

            foutput.addFieldOperation(
                new FieldOperation(
                    HeaderField.IP_SRC,
                    OperationType.WRITE_STATELESS,
                    stlOp
                )
            );
        }

        // Source transport port
        if (!pattern[2].equals("-")) {
            String[] splitPortPattern = pattern[2].split("-");
            if (splitPortPattern.length == 1) {
                StatelessOperationValue stlOp = new StatelessOperationValue(
                    Long.parseLong(pattern[2])
                );

                foutput.addFieldOperation(
                    new FieldOperation(
                        HeaderField.TP_SRC_PORT,
                        OperationType.WRITE_STATELESS,
                        stlOp
                    )
                );
            } else if (splitPortPattern.length == 2) {
                OperationType opType = OperationType.WRITE_STATEFUL;

                switch (splitPortPattern[1].charAt(splitPortPattern[1].length() - 1)) {
                    case '#':
                        opType = OperationType.WRITE_ROUNDROBIN;
                        // splitPortPattern[1].pop_back();
                        break;
                    case '?':
                        opType = OperationType.WRITE_RANDOM;
                        // splitPortPattern[1].pop_back();
                        break;
                    default:
                        break;
                }

                StatefulOperationValue stfOp = new StatefulOperationValue(
                    Long.parseLong(splitPortPattern[0]),
                    Long.parseLong(splitPortPattern[1])
                );

                FieldOperation fieldOp = new FieldOperation(
                    HeaderField.TP_SRC_PORT, opType, stfOp
                );

                foutput.addFieldOperation(fieldOp);
            } else {
                throw new SynthesisException(
                    "Incorrect source port pattern in IPRewriter pattern: " +
                    String.join(" ", pattern)
                );
            }
        }

        // Destination IP address
        if (!pattern[3].equals("-")) {
            long ipDst = Common.stringIpToInt(pattern[3]);
            if (ipDst < 0) {
                throw new SynthesisException(
                    "Invalid destination IP address in IPRewriter pattern: " +
                    String.join(" ", pattern)
                );
            }

            StatelessOperationValue stlOp = new StatelessOperationValue(ipDst);

            foutput.addFieldOperation(
                new FieldOperation(
                    HeaderField.IP_DST,
                    OperationType.WRITE_STATELESS,
                    stlOp
                )
            );
        }

        // Destination transport port
        if (!pattern[4].equals("-")) {
            String[] splitPortPattern = pattern[4].split("-");
            if (splitPortPattern.length == 1) {
                StatelessOperationValue stlOp = new StatelessOperationValue(
                    Long.parseLong(pattern[4])
                );

                foutput.addFieldOperation(
                    new FieldOperation(
                        HeaderField.TP_DST_PORT,
                        OperationType.WRITE_STATELESS,
                        stlOp
                    )
                );
            } else if (splitPortPattern.length == 2) {
                OperationType opType = OperationType.WRITE_STATEFUL;

                switch (splitPortPattern[1].charAt(splitPortPattern[1].length() - 1)) {
                    case '#':
                        opType = OperationType.WRITE_ROUNDROBIN;
                        // splitPortPattern[1].pop_back();
                        break;
                    case '?':
                        opType = OperationType.WRITE_RANDOM;
                        // splitPortPattern[1].pop_back();
                        break;
                    default:
                        break;
                }

                StatefulOperationValue stfOp = new StatefulOperationValue(
                    Long.parseLong(splitPortPattern[0]),
                    Long.parseLong(splitPortPattern[1])
                );

                FieldOperation fieldOp = new FieldOperation(
                    HeaderField.TP_DST_PORT, opType, stfOp
                );

                foutput.addFieldOperation(fieldOp);
            } else {
                throw new SynthesisException(
                    "Incorrect destination port pattern in IPRewriter pattern: " +
                    String.join(" ", pattern)
                );
            }
        }

        log.debug("Created output class: " + foutput.toString());

        return new Pair<OutputClass, OutputClass>(foutput, new OutputClass(unmodifiedPortNumber));
    }

    /**
     * Creates an output class out of an IPMapper pattern.
     * Format: SADDR SPORT DADDR DPORT FOUTPUT ROUTPUT
     *
     * @param patterns a list of IPMapper patterns to be translated
     * @return OutputClass an output class
     */
    public static OutputClass fromIpMapper(List<String> patterns) {
        OutputClass foutput = null;
        Set<Long> statefulSetValues = new HashSet<Long>();

        for (String pattern : patterns) {
            String[] token = pattern.split("\\s+");
            checkArgument(
                token.length == IPMAPPER_PATTERN_LENGTH,
                "Failed to parse line: " + pattern + ". " +
                "IPMapper element expects rules in the form: - - DSTIP - FOUTPUT BOUTPUT"
            );

            int bwdPort = Integer.parseInt(token[5]);
            int fwdPort = Integer.parseInt(token[4]);

            if (foutput == null) {
                foutput = new OutputClass(fwdPort);
            }

            if (!token[0].equals("-") || !token[1].equals("-") || !token[3].equals("-")) {
                log.warn("Limited support for pattern: " + pattern + ". " +
                         "RoundRobinIPMapper support is currently limited to destination IP addresses");
            }

            long ipDst = Common.stringIpToInt(token[2]);
            statefulSetValues.add(ipDst);
        }

        checkArgument(!statefulSetValues.isEmpty(), "No IPMapper operations out of patterns: " + patterns);
        StatefulSetOperationValue stfOp = new StatefulSetOperationValue(statefulSetValues);

        foutput.addFieldOperation(
            new FieldOperation(
                HeaderField.IP_DST,
                OperationType.WRITE_LB,
                stfOp
            )
        );

        log.debug("Created output class: " + foutput.toString());

        return foutput;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.portNumber,
            this.nextInputPort,
            this.child,
            this.operation,
            this.packetFilter
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof OutputClass))) {
            return false;
        }

        OutputClass that = (OutputClass) obj;

        if (Objects.equals(this.portNumber,    that.portNumber) &&
            Objects.equals(this.nextInputPort, that.nextInputPort) &&
            Objects.equals(this.child,         that.child) &&
            Objects.equals(this.operation,     that.operation) &&
            Objects.equals(this.packetFilter,  that.packetFilter)) {
            return true;
        }

        return true;
    }

    @Override
    public String toString() {
        if (!this.hasPacketFilters() && !this.hasOperations()) {
            return "";
        }

        String output = "\n======== Begin Output Class ========\n";

        output +=   "Output port " + this.portNumber +
                    " ---> Next input port " + this.nextInputPort + "\n";
        output +=   "Filters:\n";

        for (Map.Entry<HeaderField, Filter> filter : this.packetFilter.entrySet()) {
            output += ("\t" + filter.getValue().toString() + "\n");
        }
        output += this.operation.toString();

        output += "========= End   Output Class =========\n";

        return output;
    }

}
