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

package org.onosproject.metron.impl.dataplane;

import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.classification.trafficclass.outputclass.OutputClass;
import org.onosproject.metron.api.classification.trafficclass.parser.IpFilterParser;
import org.onosproject.metron.api.classification.trafficclass.operation.FieldOperation;
import org.onosproject.metron.api.classification.trafficclass.operation.OperationType;
import org.onosproject.metron.api.classification.trafficclass.operation.StatelessOperationValue;
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.exceptions.ParseException;
import org.onosproject.metron.api.exceptions.SynthesisException;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;
import org.onosproject.metron.api.structures.Pair;

import org.onosproject.metron.impl.processing.blocks.Discard;
import org.onosproject.metron.impl.processing.blocks.IpRewriter;

import org.onlab.packet.IpAddress;

import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * An NFV packet processing block meant for the Metron dataplane.
 */
public class NfvDataplaneBlock implements NfvDataplaneBlockInterface {

    private static final Logger log = getLogger(NfvDataplaneBlock.class);

    /**
     * Pointer to the network function that owns this block.
     */
    private NetworkFunctionInterface networkFunction;
    /**
     * The packet processing logic of this dataplane block.
     */
    private ProcessingBlockInterface processor;
    /**
     * The list of output classes associated with this dataplane block.
     */
    private List<OutputClass> outputClasses;

    /**
     * Auxiliary information derived by the members above.
     */
    private String               name;
    private ProcessingBlockClass blockClass;
    private int                  portsNumber;
    private String               basicConfiguration;
    private String               networkFunctionOfOutIface;

    /**
     * Global counter that allows us to create
     * Discard blocks with unique IDs at will.
     */
    private static int newDiscardInstanceIndex = 0;

    public NfvDataplaneBlock(
            NetworkFunctionInterface networkFunction,
            ProcessingBlockInterface block) {
        this(networkFunction, block, 0);
    }

    public NfvDataplaneBlock(
            NetworkFunctionInterface networkFunction,
            ProcessingBlockInterface block,
            int inputPort) {
        checkNotNull(
            networkFunction,
            "NFV dataplane block cannot be constructed out of a NULL network function"
        );

        checkNotNull(
            block,
            "NFV dataplane block cannot be constructed out of a NULL processing element"
        );

        checkArgument(
            inputPort >= 0,
            "NFV dataplane block " + this.name +
            " cannot have negative input port"
        );

        this.networkFunction           = networkFunction;
        this.processor                 = block;
        this.name                      = this.processor.id();
        this.blockClass                = this.processor.processingBlockClass();
        this.portsNumber               = this.processor.ports().size();
        this.basicConfiguration        = this.processor.configuration();
        this.networkFunctionOfOutIface = networkFunction.name();
        this.outputClasses             = new ArrayList<OutputClass>();

        this.parseConfiguration(inputPort);
    }

    public NfvDataplaneBlock(ProcessingBlockInterface block) {
        checkNotNull(
            block,
            "NFV dataplane block cannot be constructed out of a NULL processing element"
        );

        this.networkFunction           = null;
        this.processor                 = block;
        this.name                      = this.processor.id();
        this.blockClass                = this.processor.processingBlockClass();
        this.portsNumber               = this.processor.ports().size();
        this.basicConfiguration        = this.processor.configuration();
        this.networkFunctionOfOutIface = "";
        this.outputClasses             = new ArrayList<OutputClass>();

        this.parseConfiguration(0);
    }

    public static NfvDataplaneBlock createDiscardBlock() {
        return new NfvDataplaneBlock(
            new Discard(
                "Discard:" + Integer.toString(newDiscardInstanceIndex++),
                "", ""
            )
        );
    }

    @Override
    public NetworkFunctionInterface networkFunction() {
        return this.networkFunction;
    }

    @Override
    public void setNetworkFunction(NetworkFunctionInterface nf) {
        this.networkFunction = nf;
    }

    @Override
    public ProcessingBlockInterface processor() {
        return this.processor;
    }

    @Override
    public void setProcessor(ProcessingBlockInterface processor) {
        this.processor = processor;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ProcessingBlockClass blockClass() {
        return this.blockClass;
    }

    @Override
    public int portsNumber() {
        return this.portsNumber;
    }

    @Override
    public void setPortsNumber(int portsNumber) {
        checkArgument(
            portsNumber >= 0,
            "NFV dataplane block " + this.name +
            " cannot have negative number of ports"
        );

        this.portsNumber = portsNumber;
    }

    @Override
    public String basicConfiguration() {
        return this.basicConfiguration;
    }

    @Override
    public String networkFunctionOfOutIface() {
        return this.networkFunctionOfOutIface;
    }

    @Override
    public List<OutputClass> outputClasses() {
        return this.outputClasses;
    }

    @Override
    public boolean isLeaf() {
        return this.portsNumber == 0;
    }

    @Override
    public void setChild(NfvDataplaneBlockInterface child, int port, int nextInputPort) {
        checkArgument(
            port >= 0,
            "Cannot set child " + child.blockClass() + " for block " +
            this.blockClass() + " on a negative port number"
        );

        for (OutputClass outClass : this.outputClasses) {
            if (outClass.portNumber() == port) {
                log.debug(
                    "[{}] \t\t\tBlock {} added {} as a child on output port {}",
                    this.label(),
                    this.blockClass().toString(),
                    child.blockClass().toString(),
                    port
                );
                outClass.setChild(child);
                outClass.setNextInputPort(nextInputPort);
            }
        }
    }

    /**
     * Adds a new child to the output classes of this block.
     * Assumes that next input port is 0.
     *
     * @param child a child NfvDataplaneBlock
     * @param port the port number of the output class that gets this child
     */
    public void setChild(NfvDataplaneBlock child, int port) {
        this.setChild(child, port, 0);
    }

    /**
     * Adds a new output class to this block.
     *
     * @param outputClass the output class to be added
     */
    public void addOutputClass(OutputClass outputClass) {
        checkNotNull(
            outputClass,
            "Cannot add NULL output class to NFV dataplane block " +
            processor.id() + " of NF {}" + networkFunction.name()
        );

        this.outputClasses.add(outputClass);

        int outputClassPort = outputClass.portNumber() + 1;

        if (outputClassPort > this.portsNumber()) {
            this.portsNumber = outputClassPort;
        }
    }

    /**
     * Print the output classes of this block.
     */
    public void printOutputClasses() {
        if (this.outputClasses.size() == 0) {
            return;
        }

        String printedConf = new String(this.basicConfiguration);
        int confSize = printedConf.length();
        if (confSize > 30) {
            printedConf = printedConf.substring(0, 30) + "...";
        }

        log.debug(
            "[{}] \t\t##############################################################################",
            this.label()
        );
        log.debug(
            "[{}] \t\tOutput classes of {} [{}({})]",
            this.label(), this.name, this.blockClass, printedConf
        );

        for (OutputClass outClass : this.outputClasses) {
            String outClassStr = outClass.toString();
            if (!outClassStr.isEmpty()) {
                log.debug("[{}] \t\t{}", this.label(), outClassStr);
            }
        }

        log.debug(
            "[{}] \t\t##############################################################################",
            this.label()
        );
        log.debug("");
    }

    /**
     * Parse the configuration of this block according to its type.
     */
    private void parseConfiguration(int inputPort) {
        // Currently ARP responses are not supported
        if (this.blockClass == ProcessingBlockClass.ARP_RESPONDER) {
            log.error(
                "[{}] \t\t {} is not currently supported but it is not fatal.",
                this.label(), this.blockClass
            );
            this.blockClass = ProcessingBlockClass.DISCARD;
        // All these blocks have a single output port
        } else if ((this.blockClass == ProcessingBlockClass.ARP_QUERIER) ||
                   (this.blockClass == ProcessingBlockClass.AVERAGE_COUNTER) ||
                   (this.blockClass == ProcessingBlockClass.AVERAGE_COUNTER_MP) ||
                   (this.blockClass == ProcessingBlockClass.CHECK_ICMP_HEADER) ||
                   (this.blockClass == ProcessingBlockClass.CHECK_IP_HEADER) ||
                   (this.blockClass == ProcessingBlockClass.CHECK_TCP_HEADER) ||
                   (this.blockClass == ProcessingBlockClass.CHECK_UDP_HEADER) ||
                   (this.blockClass == ProcessingBlockClass.COUNTER) ||
                   (this.blockClass == ProcessingBlockClass.COUNTER_MP) ||
                   (this.blockClass == ProcessingBlockClass.ETHER_ENCAP) ||
                   (this.blockClass == ProcessingBlockClass.ETHER_REWRITE) ||
                   (this.blockClass == ProcessingBlockClass.ETHER_MIRROR) ||
                   (this.blockClass == ProcessingBlockClass.DROP_BROADCASTS) ||
                   (this.blockClass == ProcessingBlockClass.FROM_BLACKBOX_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.FROM_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.FROM_DPDK_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.FROM_NET_FRONT) ||
                   (this.blockClass == ProcessingBlockClass.FROM_SNORT_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.GET_IP_ADDRESS) ||
                   (this.blockClass == ProcessingBlockClass.IP_GW_OPTIONS) ||
                   (this.blockClass == ProcessingBlockClass.IP_PRINT) ||
                   (this.blockClass == ProcessingBlockClass.MARK_IP_HEADER) ||
                   (this.blockClass == ProcessingBlockClass.PAINT) ||
                   (this.blockClass == ProcessingBlockClass.POLL_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.PRINT) ||
                   (this.blockClass == ProcessingBlockClass.QUEUE) ||
                   (this.blockClass == ProcessingBlockClass.PRINT) ||
                   (this.blockClass == ProcessingBlockClass.STORE_ETHER_ADDRESS) ||
                   (this.blockClass == ProcessingBlockClass.STRIP) ||
                   (this.blockClass == ProcessingBlockClass.TO_BLACKBOX_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.TO_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.TO_DPDK_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.TO_NET_FRONT) ||
                   (this.blockClass == ProcessingBlockClass.TO_SNORT_DEVICE) ||
                   (this.blockClass == ProcessingBlockClass.UNSTRIP)) {
            OutputClass port = new OutputClass(0);
            this.addOutputClass(port);
        } else if (this.blockClass == ProcessingBlockClass.CLASSIFIER) {
            this.parseClassifier();
        } else if (this.blockClass == ProcessingBlockClass.DEC_IP_TTL) {
            this.parseDecIpTtl();
        } else if ((this.blockClass == ProcessingBlockClass.DIRECT_IP_LOOKUP)   ||
                   (this.blockClass == ProcessingBlockClass.LINEAR_IP_LOOKUP)   ||
                   (this.blockClass == ProcessingBlockClass.LOOKUP_IP_ROUTE_MP) ||
                   (this.blockClass == ProcessingBlockClass.RADIX_IP_LOOKUP)    ||
                   (this.blockClass == ProcessingBlockClass.RANGE_IP_LOOKUP)    ||
                   (this.blockClass == ProcessingBlockClass.SORTED_IP_LOOKUP)   ||
                   (this.blockClass == ProcessingBlockClass.STATIC_IP_LOOKUP)) {
            this.parseLookupFilter();
        } else if (this.blockClass == ProcessingBlockClass.DISCARD) {
            // Do nothing
            return;
        } else if (this.blockClass == ProcessingBlockClass.FIX_IP_SRC) {
            this.parseFixIpSrc();
        } else if (this.blockClass == ProcessingBlockClass.IP_ADDR_PAIR_REWRITER) {
            this.parseIpAddrPairRewriter(inputPort);
        } else if (this.blockClass == ProcessingBlockClass.IP_ADDR_REWRITER) {
            this.parseIpAddrRewriter(inputPort);
        } else if (this.blockClass == ProcessingBlockClass.IP_CLASSIFIER) {
            this.parseIpClassifier();
        } else if (this.blockClass == ProcessingBlockClass.IP_FILTER) {
            this.parseIpFilter();
        } else if (this.blockClass == ProcessingBlockClass.IP_FRAGMENTER) {
            this.parseIpFragmenter();
        } else if (this.blockClass == ProcessingBlockClass.IP_OUTPUT_COMBO) {
            throw new ParseException(this.blockClass + " is not currently supported.");
        } else if (this.blockClass == ProcessingBlockClass.IP_REWRITER) {
            this.parseIpRewriter(inputPort);
        } else if (this.blockClass == ProcessingBlockClass.OPENFLOW_CLASSIFIER) {
            throw new ParseException(this.blockClass + " is totally offloaded to the network.");
        } else if (this.blockClass == ProcessingBlockClass.PAINT_TEE) {
            throw new ParseException(this.blockClass + " is not currently supported.");
        } else if (this.blockClass == ProcessingBlockClass.ROUND_ROBIN_IP_MAPPER) {
            this.parseRoundRobinIpMapper();
        } else if (this.blockClass == ProcessingBlockClass.SET_VLAN_ANNO) {
            throw new ParseException(this.blockClass + " is not currently supported.");
        } else if (this.blockClass == ProcessingBlockClass.SIMPLE_ETHERNET_CLASSIFIER) {
            this.parseSimpleEthernetClassifier();
        } else if (this.blockClass == ProcessingBlockClass.TCP_REWRITER) {
            this.parseTcpRewriter(inputPort);
        } else if (this.blockClass == ProcessingBlockClass.UDP_REWRITER) {
            this.parseUdpRewriter(inputPort);
        } else if (this.blockClass == ProcessingBlockClass.VLAN_ENCAP) {
            this.parseVlanEncap();
        } else if (this.blockClass == ProcessingBlockClass.VLAN_DECAP) {
            this.parseVlanDecap();
        } else {
            throw new SynthesisException(
                "Processing block " + this.blockClass + " is not currently supported"
            );
        }

        this.printOutputClasses();
    }

    /**
     * Parse the configuration of a Classifier block.
     * This is a low-level (hex-based) L2-L7 classifier.
     */
    private void parseClassifier() {
        // TODO
        return;
    }

    /**
     * Parse the configuration of a SimpleEthernetClassifier block.
     * This is a high-level equivalent of the Classifier above.
     * However, it does not cover classification beyond L2.
     */
    private void parseSimpleEthernetClassifier() {
        // TODO
        return;
    }

    /**
     * Parse the configuration of a VLANEncap block.
     * Adds VLAN information to Ethernet packets.
     */
    private void parseVlanEncap() {
        int pos = this.basicConfiguration.indexOf(" ");
        if (pos < 0) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Expected keyword and got: " + this.basicConfiguration
            );
        }

        String keyword = this.basicConfiguration.substring(0, pos);
        int pcp = 0;
        int dei = 2;
        int vid = 0;

        if (keyword.equals("VLAN_TCI")) {
            int value  = Integer.parseInt(
                this.basicConfiguration.substring(pos + 1, this.basicConfiguration.length() - pos - 1)
            );
            pcp = value >> 13;                        // Removes 13 last bits
            dei = (value >> 12) & (0xffffffff << 1);  // Gets 12th bit from smaller endian
            vid = value & (0xffffffff << 12);         // Gets 12 last bits
        } else {
            while (pos >= 0) {
                if (keyword.equals("VLAN_PCP")) {
                    pcp  = Integer.parseInt(
                        this.basicConfiguration.substring(pos + 1, this.basicConfiguration.length() - pos - 1)
                    );
                } else if (keyword.equals("VLAN_ID")) {
                    vid = Integer.parseInt(
                        this.basicConfiguration.substring(pos + 1, this.basicConfiguration.length() - pos - 1)
                    );
                } else {
                    throw new ParseException(
                        "[" + this.name + " (" + this.blockClass + ")] " +
                        "Unknown keyword in VLANEncap: " + keyword
                    );
                }

                int start = Common.findFirstNotOf(
                    this.basicConfiguration, " ,", this.basicConfiguration.indexOf(",", pos)
                );
                pos = this.basicConfiguration.indexOf(" ", start);
            }
        }

        OutputClass port = new OutputClass(0);

        // Create a header field operation for PCP
        StatelessOperationValue pcpVal = new StatelessOperationValue(pcp);
        FieldOperation pcpOp = new FieldOperation(
            HeaderField.VLAN_PCP,
            OperationType.WRITE_STATELESS,
            pcpVal
        );
        port.addFieldOperation(pcpOp);

        // Create a header field operation for VID
        StatelessOperationValue vidVal = new StatelessOperationValue(vid);
        FieldOperation vidOp = new FieldOperation(
            HeaderField.VLAN_VID,
            OperationType.WRITE_STATELESS,
            pcpVal
        );
        port.addFieldOperation(vidOp);

        if (dei < 2) {
            // Create a header field operation for DEI
            StatelessOperationValue deiVal = new StatelessOperationValue(dei);
            FieldOperation deiOp = new FieldOperation(
                HeaderField.VLAN_DEI,
                OperationType.WRITE_STATELESS,
                deiVal
            );
            port.addFieldOperation(deiOp);
        }

        this.addOutputClass(port);
    }

    /**
     * Parse the configuration of a VLANDecap block.
     * Strips VLAN information from Ethernet packets.
     */
    private void parseVlanDecap() {
        // TODO: do we handle ANNO and if yes how?
        if (!this.basicConfiguration.isEmpty()) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "VLAN annotation not implemented yet"
            );
        }

        OutputClass port = new OutputClass(0);

        // Create a header field operation for PCP
        StatelessOperationValue pcpVal = new StatelessOperationValue(Constants.MAX_UINT);
        FieldOperation pcpOp = new FieldOperation(
            HeaderField.VLAN_PCP,
            OperationType.WRITE_STATELESS,
            pcpVal
        );
        port.addFieldOperation(pcpOp);

        // Create a header field operation for VID
        StatelessOperationValue vidVal = new StatelessOperationValue(Constants.MAX_UINT);
        FieldOperation vidOp = new FieldOperation(
            HeaderField.VLAN_VID,
            OperationType.WRITE_STATELESS,
            pcpVal
        );
        port.addFieldOperation(vidOp);

        // Create a header field operation for DEI
        StatelessOperationValue deiVal = new StatelessOperationValue(Constants.MAX_UINT);
        FieldOperation deiOp = new FieldOperation(
            HeaderField.VLAN_DEI,
            OperationType.WRITE_STATELESS,
            deiVal
        );
        port.addFieldOperation(deiOp);

        this.addOutputClass(port);
    }

    /**
     * Parse the configuration of an IPFilter block.
     * This offers L3-L4 IP filtering.
     */
    private void parseIpFilter() {
        List<String> rules = Common.separateArguments(this.basicConfiguration);
        if (rules.isEmpty()) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Failed to parse the following rules: " + this.basicConfiguration
            );
        }

        List<PacketFilter> toDiscard = new ArrayList<PacketFilter>();

        for (String rule : rules) {
            if (rule.isEmpty()) {
                throw new ParseException(
                    "[" + this.name + " (" + this.blockClass + ")] " +
                    "Empty classification rule"
                );
            }

            rule = rule.trim();

            int output = -1;
            int firstSpace = rule.indexOf(" ");
            String behaviour = rule.substring(0, firstSpace);

            // This is a rule with action allow
            if (behaviour.equals("allow")) {
                output = 0;
            } else if (Common.findFirstNotOf(behaviour, "0123456789", 0) < 0) {
                output = Integer.parseInt(behaviour);
            } else if (!behaviour.equals("deny") && !behaviour.equals("drop")) {
                throw new ParseException(
                    "[" + this.name + " (" + this.blockClass + ")] " +
                    "Unknown action " + behaviour
                );
            }

            rule = rule.replace(behaviour, "").trim();
            log.debug("[{}] \t\t\tRule: {} --> {}", this.label(), rule, output);

            List<PacketFilter> packetFilters = IpFilterParser.filtersFromIpFilterLine(rule);

            if (output == -1) {
                toDiscard.addAll(packetFilters);
            } else {
                for (PacketFilter pf : packetFilters) {
                    OutputClass port = new OutputClass(output);
                    port.setPacketFilter(pf);
                    this.addOutputClass(port);
                }
            }
        }

        long discardPort = this.portsNumber;

        for (PacketFilter pf : toDiscard) {
            OutputClass port = new OutputClass((int) discardPort);
            port.setChild(createDiscardBlock());
            port.setPacketFilter(pf);
            this.addOutputClass(port);
        }
    }

    /**
     * Parse the configuration of an IPClassifier block.
     * This is equivalent to IPFilter, but follows slightly different structure.
     */
    private void parseIpClassifier() {
        List<String> rules = Common.separateArguments(this.basicConfiguration);
        if (rules.isEmpty()) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Failed to parse the following rules: " + this.basicConfiguration
            );
        }

        int ruleNb = 0;
        for (String rule : rules) {

            if (rule.isEmpty()) {
                throw new ParseException(
                    "[" + this.name + " (" + this.blockClass + ")] " +
                    "Empty classification rule"
                );
            }

            rule = rule.trim();
            log.debug("[{}] \t\t\tRule: {}", this.label(), rule);

            List<PacketFilter> packetFilters = IpFilterParser.filtersFromIpFilterLine(rule);

            for (PacketFilter pf : packetFilters) {
                OutputClass port = new OutputClass(ruleNb);
                port.setPacketFilter(pf);
                this.addOutputClass(port);
            }

            ruleNb++;
        }
    }

    /**
     * Parse the configuration of an IPRouteTable block.
     * A variety of blocks are implemented on top of this one:
     * |-> DirectIPLookup
     * |-> LinearIPLookup
     * |-> LookupIPRouteMP
     * |-> RadixIPLookup
     * |-> RangeIPLookup
     * |-> SortedIPLookup
     * |-> StaticIPLookup
     */
    private void parseLookupFilter() {
        List<String> rules = Common.separateArguments(this.basicConfiguration);
        if (rules.isEmpty()) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Failed to parse the following rules: " + this.basicConfiguration
            );
        }

        Filter parsedPrefixes = new Filter(HeaderField.IP_DST);
        parsedPrefixes.makeNone();

        for (String rule : rules) {
            OutputClass port = OutputClass.fromLookupRule(rule, parsedPrefixes);
            this.addOutputClass(port);
        }
    }

    /**
     * Parse the configuration of an IPAddrRewriter block.
     * This is a stateful block that keeps track of modifications
     * on src IP addresses.
     *
     * @param inputPort the input port indicates which line
     *        of the configuration we parse
     */
    private void parseIpAddrRewriter(int inputPort) {
        this.parseIpRewriter(inputPort);
    }

    /**
     * Parse the configuration of an IPAddrPairRewriter block.
     * This is a stateful block that keeps track of modifications
     * on src and/or dst IP addresses.
     *
     * @param inputPort the input port indicates which line
     *        of the configuration we parse
     */
    private void parseIpAddrPairRewriter(int inputPort) {
        this.parseIpRewriter(inputPort);
    }

    /**
     * Parse the configuration of an IPRewriter block.
     * This is a stateful block that keeps track of modifications
     * on src/dst IP addresses and/or src/dst ports.
     *
     * @param inputPort the input port indicates which line
     *        of the configuration we parse
     */
    private void parseIpRewriter(int inputPort) {
        List<String> rules = null;

        // Round-robin load balancer is an exceptional case
        if (this.basicConfiguration.startsWith(
                ProcessingBlockClass.ROUND_ROBIN_IP_MAPPER.toString())) {
            rules = new ArrayList<String>();
            int endOfMapper = this.basicConfiguration.indexOf(")") + 1;
            // First rule is the whole RoundRobinIPMapper(...)
            rules.add(this.basicConfiguration.substring(0, endOfMapper));
            // Then add the rest as usual
            rules.addAll(
                Common.separateArguments(this.basicConfiguration.substring(endOfMapper + 1))
            );
        } else {
            rules = Common.separateArguments(this.basicConfiguration);
        }

        if (rules.isEmpty()) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Failed to parse the following rules: " + this.basicConfiguration
            );
        }

        // Detect aggregate parameter
        for (String rule : rules) {
            if (rule.contains(IpRewriter.AGGREGATE)) {
                String[] tokens = rule.split(IpRewriter.AGGREGATE);
                if (tokens.length != 2) {
                    continue;
                }

                IpRewriter rw = (IpRewriter) processor;
                rw.setAggregate(Boolean.parseBoolean(tokens[1].toLowerCase().trim()));

                break;
            }
        }

        // Keep only the configuration of the rule associated with the input port
        String confLine = rules.get(inputPort);
        if (confLine.startsWith(ProcessingBlockClass.ROUND_ROBIN_IP_MAPPER.toString())) {
            this.parseRoundRobinIpMapper(confLine);
            return;
        }
        String[] splitLine = confLine.split("\\s+");

        // Drop pattern
        if (splitLine.length == 1) {
            if (confLine.equals("drop") || confLine.equals("discard")) {
                OutputClass discard = new OutputClass(0);
                discard.setChild(createDiscardBlock());
                this.addOutputClass(discard);
            } else {
                throw new ParseException(
                    "[" + this.name + " (" + this.blockClass + ")] " +
                    "Expected a drop/discard action. Instead, I got " + confLine
                );
            }
        // IPAddr(Pair)Rewriter pattern
        } else if (splitLine.length == 5) {
            if (!(this.blockClass == ProcessingBlockClass.IP_ADDR_REWRITER) &&
                !(this.blockClass == ProcessingBlockClass.IP_ADDR_PAIR_REWRITER)) {
                throw new ParseException(
                    "[" + this.name + " (" + this.blockClass + ")] " +
                    "Patterns with 5 tokens are only present in IPAddr(Pair)Rewriters"
                );
            }

            if (this.blockClass == ProcessingBlockClass.IP_ADDR_REWRITER) {
                if (!splitLine[2].equals("-")) {
                    throw new ParseException(
                        "[" + this.name + " (" + this.blockClass + ")] " +
                        "If you want to modify destination IP addresses, " +
                        "use an IPAddrPairRewriter"
                    );
                }
            }

            /**
             * Convert to an IPRewriter pattern.
             * This requires to add dashes where IPRewriter expects ports.
             */
            String[] newLine = new String[7];
            newLine[0] = splitLine[0];
            newLine[1] = splitLine[1];
            newLine[2] = "-";
            newLine[3] = splitLine[2];
            newLine[4] = "-";
            newLine[5] = splitLine[3];
            newLine[6] = splitLine[4];

            // Do it!
            Pair<OutputClass, OutputClass> ports = OutputClass.fromPattern(newLine);
            this.addOutputClass(ports.getKey());
        // Regular IPRewriter pattern
        } else if (splitLine.length == 7) {
            Pair<OutputClass, OutputClass> ports = OutputClass.fromPattern(splitLine);
            this.addOutputClass(ports.getKey());
        } else {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Failed to parse line: " + confLine + ". " +
                "IPRewriter expects patterns first and then any other configuration."
            );
        }

        return;
    }

    /**
     * Parse the configuration of a RoundRobinIPMapper block.
     * This is a block that performs round-robin load balancing.
     *
     * @param config the configuration of the RoundRobinIPMapper element
     */
    private void parseRoundRobinIpMapper(String config) {
        // Remove the initial element name
        String[] tokens = config.split(ProcessingBlockClass.ROUND_ROBIN_IP_MAPPER.toString());
        checkArgument(
            tokens[0].trim().isEmpty() && tokens[1] != null,
            "Invalid RoundRobinIPMapper configuration: " + config);

        // Keep what is enclosed between the parentheses
        config = tokens[1].substring(tokens[1].indexOf("(") + 1, tokens[1].lastIndexOf(")"));

        // Tokenize the arguments
        List<String> rules = Common.separateArguments(config);

        // Get the Round-Robin output port
        this.addOutputClass(OutputClass.fromIpMapper(rules));

        return;
    }

    /**
     * Parse the configuration of a TCPRewriter block.
     * This is a stateful block that keeps track of modifications
     * on src/dst IP addresses, src/dst ports, and TCP seq of TCP packets.
     *
     * @param inputPort the input port indicates which line
     *        of the configuration we parse
     */
    private void parseTcpRewriter(int inputPort) {
        this.parseIpRewriter(inputPort);
    }

    /**
     * Parse the configuration of a UDPRewriter block.
     * This is a stateful block that keeps track of modifications
     * on src/dst IP addresses and/or src/dst ports of UDP packets.
     *
     * @param inputPort the input port indicates which line
     *        of the configuration we parse
     */
    private void parseUdpRewriter(int inputPort) {
        this.parseIpRewriter(inputPort);
    }

    /**
     * Parse the configuration of a DecIPTTL block.
     * Decrements the IP Time to Live field.
     */
    private void parseDecIpTtl() {
        String conf = this.basicConfiguration;

        if (conf.length() != 0) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Unable to parse configuration"
            );
        }

        // This is the minimum valid TTL value
        StatelessOperationValue stlVal = new StatelessOperationValue(1);
        // Create a header field operation
        FieldOperation ttlOp = new FieldOperation(
            HeaderField.IP_TTL,
            OperationType.TRANSLATE,
            stlVal
        );

        // This is the port where the element sends valid packets
        OutputClass port = new OutputClass(0);
        // The filter goes from 1 to the maximum allowed TTL value
        Filter validTtl  = new Filter(
            HeaderField.IP_TTL,
            (long) 2,
            Constants.HEADER_FIELD_UPPER_BOUND.get(HeaderField.IP_TTL).longValue()
        );
        port.addFieldOperation(ttlOp);
        port.addFilter(HeaderField.IP_TTL, validTtl);

        // Add this valid output class
        this.addOutputClass(port);

        // This is the port where the element sends invalid packets
        OutputClass port1 = new OutputClass(1);
        // Invalid packets have expired TTL (zero)
        Filter zeroTtl = new Filter(
            HeaderField.IP_TTL, (long) 0, (long) 1
        );
        port1.addFilter(HeaderField.IP_TTL, zeroTtl);
        port1.setChild(createDiscardBlock());

        // Add this output class to this block
        this.addOutputClass(port1);
    }

    /**
     * Parse the configuration of a FixIPSrc block.
     * Modifies the source IP address of a packet.
     */
    private void parseFixIpSrc() {
        String[] splitConf = this.basicConfiguration.split("\\s+");

        String ipStr = "";
        IpAddress ip = null;

        // FixIPSrc(x.x.x.x)
        if (splitConf.length == 1) {
            ipStr = splitConf[0];
        // FixIPSrc(IPADDR x.x.x.x)
        } else if (splitConf.length == 2) {
            if (!splitConf[0].equals("IPADDR")) {
                throw new ParseException(
                    "[" + this.name + " (" + this.blockClass + ")] " +
                    "Invalid configuration " + this.basicConfiguration
                );
            }
            ipStr = splitConf[1];
        }

        // Check if the given IP is valid
        try {
            ip = IpAddress.valueOf(ipStr);
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                "[" + this.name + " (" + this.blockClass + ")] " +
                "Invalid IP address " + ipStr
            );
        }

        // Convert the string-based IP into integer
        long newIpValue = Common.stringIpToInt(ipStr);
        // And not make it a stateless value
        StatelessOperationValue stlVal = new StatelessOperationValue(newIpValue);

        // Create a header field operation
        FieldOperation fixIpSrcOp = new FieldOperation(
            HeaderField.IP_SRC,
            OperationType.WRITE_STATELESS,
            stlVal
        );

        // ..and an ouput class that gets this operation
        OutputClass port = new OutputClass(0);
        port.addFieldOperation(fixIpSrcOp);

        // Add this output class to this block
        this.addOutputClass(port);
    }

    /**
     * Parse the configuration of an IPFragmenter block.
     */
    private void parseIpFragmenter() {
        // TODO
        // this.addOutputClass(0);
        return;
    }

    /**
     * Parse the configuration of an RoundRobinIPMapper block.
     * Works in tandem with IPRewriter to provide round-robin rewriting.
     */
    private void parseRoundRobinIpMapper() {
        // TODO
        return;
    }

    /**
     * Returns a label with the NFV dataplane block's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return "NFV Dataplane Block";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.networkFunction,
            this.processor,
            this.name,
            this.blockClass,
            this.portsNumber,
            this.basicConfiguration,
            this.networkFunctionOfOutIface
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof NfvDataplaneBlock))) {
            return false;
        }

        NfvDataplaneBlock other = (NfvDataplaneBlock) obj;

        return  Objects.equals(this.networkFunction, other.networkFunction) &&
                Objects.equals(this.processor,  other.processor) &&
                Objects.equals(this.blockClass, other.blockClass) &&
                this.name.equals(other.name) &&
                this.portsNumber == other.portsNumber &&
                this.basicConfiguration.equals(other.basicConfiguration) &&
                this.networkFunctionOfOutIface.equals(other.networkFunctionOfOutIface);
    }

    @Override
    public String toString() {
        String output = "\n";

        output += "===============================================================\n";
        output += "NFV Dataplane Block " + this.name + " of type " + this.blockClass + "\n";
        output += "===============================================================\n";

        if (this.networkFunction != null) {
            output += "Belongs to network function " + this.networkFunction.id() + "\n";
        }

        output += "Has " + this.portsNumber + " ports\n";
        output += "Has configuration: " + this.basicConfiguration + "\n";
        output += "Has output classes: \n";

        int outClassNb = 0;
        for (OutputClass oc : this.outputClasses) {
            output +=   "Output class " + String.valueOf(++outClassNb) +
                        ": " + oc.toString() + "\n";
        }

        output += "===============================================================\n";

        return output;
    }

}
