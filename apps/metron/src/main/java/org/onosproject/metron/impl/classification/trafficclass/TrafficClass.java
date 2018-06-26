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

package org.onosproject.metron.impl.classification.trafficclass;

import org.onosproject.metron.api.exceptions.SynthesisException;
import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.classification.ClassificationTreeInterface;
import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;
import org.onosproject.metron.api.classification.trafficclass.condition.Condition;
import org.onosproject.metron.api.classification.trafficclass.condition.ConditionMap;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.classification.trafficclass.operation.Operation;
import org.onosproject.metron.api.classification.trafficclass.operation.FieldOperation;
import org.onosproject.metron.api.classification.trafficclass.operation.OperationType;
import org.onosproject.metron.api.classification.trafficclass.operation.StatelessOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatefulOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatefulSetOperationValue;
import org.onosproject.metron.api.classification.trafficclass.outputclass.OutputClass;
import org.onosproject.metron.api.net.ClickFlowRuleAction;
import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingLayer;

import org.onosproject.metron.impl.classification.ClassificationTree;
import org.onosproject.metron.impl.processing.blocks.CheckIpHeader;
import org.onosproject.metron.impl.processing.blocks.Device;
import org.onosproject.metron.impl.processing.blocks.EtherEncap;
import org.onosproject.metron.impl.processing.blocks.FromBlackboxDevice;
import org.onosproject.metron.impl.processing.blocks.FromSnortDevice;
import org.onosproject.metron.impl.processing.blocks.MarkIpHeader;
import org.onosproject.metron.impl.processing.blocks.Strip;
import org.onosproject.metron.impl.processing.blocks.StoreEtherAddress;
import org.onosproject.metron.impl.processing.blocks.ToBlackboxDevice;
import org.onosproject.metron.impl.processing.blocks.ToSnortDevice;
import org.onosproject.metron.impl.processing.blocks.Unstrip;

import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;

import org.onosproject.core.ApplicationId;
import org.onosproject.drivers.server.devices.nic.NicFlowRule;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.MacRxFilterValue;
import org.onosproject.drivers.server.devices.nic.MplsRxFilterValue;
import org.onosproject.drivers.server.devices.nic.VlanRxFilterValue;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;
import org.onosproject.drivers.server.devices.nic.DefaultDpdkNicFlowRule;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;

import org.slf4j.Logger;
import com.google.common.collect.Sets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.metron.api.net.ClickFlowRuleAction.ALLOW;
import static org.onosproject.metron.api.net.ClickFlowRuleAction.DROP;

/**
 * A Metron traffic class implementation.
 */
public class TrafficClass implements TrafficClassInterface {

    private static final Logger log = getLogger(TrafficClass.class);

    // Default priority for the rules of this traffic class
    public static final int DEFAULT_PRIORITY = 1000;
    // A dummy port number that serves for building the binary tree of this traffic class
    private static final int DUMMY_OUTPUT_PORT = 1;
    // Indicates no input port setting
    public static final int NO_INPUT_PORT = -1;

    // A unique ID for this traffic class
    private URI id = null;

    /**
     * The type of a traffic class depends on the type of its
     * rules. If all rules belong to a specific protocol (e.g., TCP)
     * this traffic class has type TCP.
     */
    private TrafficClassType type = null;

    /**
     * The layer on which this traffic class operates.
     */
    private ProcessingLayer layer;

    /**
     * Primitive components of a traffic class.
     */
    private PacketFilter packetFilter;
    private ConditionMap conditionMap;
    private Operation    operation;
    private List<ProcessingBlockClass> postOperations;
    private List<NfvDataplaneBlockInterface> blockPath;

    /**
     * Auxiliary components that contribute to
     * the composition of the final configuration.
     */
    private String  inputInterface;             // The output interface
    private String  networkFunctionOfInIface;   // The NF that holds the input interface
    private String  outputInterface;            // The output interface
    private String  outputInterfaceConf;        // The configuration of the output interface
    private String  networkFunctionOfOutIface;  // The NF that holds the output interface
    private int     statefulInputPort;          // The input port
    private String  followers;                  // Elements that follow the write operations
    private boolean calculateChecksum;          // Indicates that this traffic class modifies
    private String  ethernetEncapConfiguration; // The configuration of the Ethernet encapsulator
    private String  blackboxConfiguration;      // The configuration of an integrated Blackbox NF

    /**
     * Flags that indicat the state of this traffic class.
     */
    private boolean hasSoftwareRules;           // Contains non-offloadable read operations
    private boolean totallyOffloadable;         // It can be completely offloaded to the network
    private boolean solelyOwnedByBlackbox;      // It is only owned by a blackbox NFs

    /**
     * A summary of the final configuration.
     */
    private String  inputOperationsAsString     = ""; //         Input operations as a string
    private String  softReadOperationsAsString  = ""; // Software read operations as a string
    private String  readOperationsAsString      = ""; //          Read operations as a string
    private String  writeOperationsAsString     = ""; //         Write operations as a string
    private String  blackboxOperationsAsString  = ""; //      Blackbox operations as a string
    private String  outputOperationsAsString    = ""; //        Output operations as a string

    private ApplicationId applicationId = null;
    private DeviceId deviceId = null;

    /**
     * A binary tree the encodes the packet filters of this traffic class.
     */
    private ClassificationTreeInterface binaryTree = null;

    /**
     * Each traffic class of the binary tree is either translated into:
     * |-> a set of software packet filters or
     * |-> a set of packet filters that can be offloaded into hardware.
     *
     * The target of this packet filter can then be either HW or SW.
     */
    private Map<String, List<Set<TextualPacketFilter>>> swPacketFiltersMap = null;
    private Map<String, List<Set<TextualPacketFilter>>> hwPacketFiltersMap = null;
    private Map<String, String> packetFiltersTargetMap = null;

    private static final String TARGET_HW = "Hardware";
    private static final String TARGET_SW = "Software";

    public TrafficClass() {
        try {
            this.id = new URI(UUID.randomUUID().toString());
        } catch (URISyntaxException sEx) {
            throw new SynthesisException(
                "Failed to create a unique traffic class ID"
            );
        }

        this.type           = TrafficClassType.NEUTRAL;
        this.layer          = ProcessingLayer.LINK;

        this.packetFilter   = new PacketFilter();
        this.conditionMap   = new ConditionMap();
        this.operation      = new Operation();
        this.postOperations = new ArrayList<ProcessingBlockClass>();
        this.blockPath      = new ArrayList<NfvDataplaneBlockInterface>();

        this.inputInterface             = "";
        this.networkFunctionOfInIface   = "";
        this.outputInterface            = "";
        this.outputInterfaceConf        = "";
        this.networkFunctionOfOutIface  = "";
        this.statefulInputPort          = NO_INPUT_PORT;
        this.followers                  = "";
        this.calculateChecksum          = false;
        this.ethernetEncapConfiguration = "";
        this.blackboxConfiguration      = "";

        this.hasSoftwareRules           = false;
        this.totallyOffloadable         = true;
        this.solelyOwnedByBlackbox      = false;

        this.swPacketFiltersMap =
            new ConcurrentHashMap<String, List<Set<TextualPacketFilter>>>();
        this.hwPacketFiltersMap =
            new ConcurrentHashMap<String, List<Set<TextualPacketFilter>>>();
        this.packetFiltersTargetMap = new ConcurrentHashMap<String, String>();
    }

    public TrafficClass(TrafficClassInterface other) {
        this.fromTrafficClass(other);
    }

    /**
     * Copy constructor for traffic class.
     * Note that packet filters, conditions, and other
     * similar in principle fields are actually extended
     * (i.e., might contain a mix of the 2 classes).
     *
     * @param other the traffic class to copy from
     * @throws SynthesisException upon failure to create UUID
     */
    public void fromTrafficClass(TrafficClassInterface other) {
        try {
            this.id = new URI(UUID.randomUUID().toString());
        } catch (URISyntaxException sEx) {
            throw new SynthesisException(
                "Failed to create a unique traffic class ID"
            );
        }

        this.packetFilter   = new PacketFilter();
        this.packetFilter.addPacketFilter(other.packetFilter());

        this.conditionMap   = new ConditionMap();
        this.conditionMap.addConditionMap(other.conditionMap());

        this.operation      = new Operation(other.operation());
        this.postOperations = new ArrayList<ProcessingBlockClass>(other.postOperations());
        this.blockPath      = new ArrayList<NfvDataplaneBlockInterface>(other.blockPath());

        this.inputInterface             = other.inputInterface();
        this.networkFunctionOfInIface   = other.networkFunctionOfInIface();
        this.outputInterface            = other.outputInterface();
        this.outputInterfaceConf        = other.outputInterfaceConf();
        this.networkFunctionOfOutIface  = other.networkFunctionOfOutIface();
        this.statefulInputPort          = other.statefulInputPort();
        this.followers                  = other.followers();
        this.calculateChecksum          = other.calculateChecksum();
        this.ethernetEncapConfiguration = other.ethernetEncapConfiguration();
        this.blackboxConfiguration      = other.blackboxConfiguration();

        this.hasSoftwareRules           = other.hasSoftwareRules();
        this.totallyOffloadable         = other.isTotallyOffloadable();
        this.solelyOwnedByBlackbox      = other.isSolelyOwnedByBlackbox();

        if (!other.inputOperationsAsString().isEmpty()) {
            this.inputOperationsAsString = other.inputOperationsAsString();
        }
        if (!other.softReadOperationsAsString().isEmpty()) {
            this.softReadOperationsAsString = other.softReadOperationsAsString();
        }
        if (!other.readOperationsAsString().isEmpty()) {
            this.readOperationsAsString = other.readOperationsAsString();
        }
        if (!other.writeOperationsAsString().isEmpty()) {
            this.writeOperationsAsString = other.writeOperationsAsString();
        }
        if (!other.blackboxOperationsAsString().isEmpty()) {
            this.blackboxOperationsAsString = other.blackboxOperationsAsString();
        }
        if (!other.outputOperationsAsString().isEmpty()) {
            this.outputOperationsAsString = other.outputOperationsAsString();
        }

        this.swPacketFiltersMap = other.swPacketFiltersMap();
        this.hwPacketFiltersMap = other.hwPacketFiltersMap();
        this.packetFiltersTargetMap = other.packetFiltersTargetMap();
    }

    @Override
    public URI id() {
        return this.id;
    }

    @Override
    public void setId(URI tcId) {
        this.id = tcId;
    }

    @Override
    public TrafficClassType type() {
        return this.type;
    }

    @Override
    public ProcessingLayer processingLayer() {
        return this.layer;
    }

    @Override
    public void setProcessingLayer(ProcessingLayer layer) {
        this.layer = layer;
    }

    @Override
    public PacketFilter packetFilter() {
        return this.packetFilter;
    }

    @Override
    public void addPacketFilter(PacketFilter pf) {
        if ((pf == null) || (pf.isEmpty())) {
            return;
        }

        for (Map.Entry<HeaderField, Filter> entry : pf.entrySet()) {
            HeaderField newHeaderField = entry.getKey();
            Filter newFilter = entry.getValue();
            TrafficClassType newType = newFilter.fieldType();
            ProcessingLayer newLayer = newFilter.fieldLayer();

            if (!this.packetFilter.containsKey(newHeaderField)) {
                this.packetFilter.put(newHeaderField, new Filter(newFilter));
                // Update the traffic class type by combining both types
                this.type = TrafficClassType.updateType(this.type, newType);
                // Update also the processing layer in a similar fashion
                this.layer = ProcessingLayer.updateLayer(this.layer, newLayer);
            } else {
                Filter f = this.packetFilter.get(newHeaderField);
                f.intersect(newFilter);
            }
        }

        return;
    }

    @Override
    public ConditionMap conditionMap() {
        return this.conditionMap;
    }

    @Override
    public void addConditionMap(ConditionMap cm) {
        if ((cm == null) || (cm.isEmpty())) {
            return;
        }

        for (Map.Entry<HeaderField, List<Condition>> entry : cm.entrySet()) {
            HeaderField headerField = entry.getKey();
            List<Condition> conditions = entry.getValue();

            if (!this.conditionMap.containsKey(headerField)) {
                this.conditionMap.put(headerField, new ArrayList<Condition>(conditions));
            } else {
                List<Condition> cond = this.conditionMap.get(headerField);
                cond.addAll(conditions);
            }
        }

        return;
    }

    @Override
    public Operation operation() {
        return this.operation;
    }

    @Override
    public List<ProcessingBlockClass> postOperations() {
        return this.postOperations;
    }

    @Override
    public List<NfvDataplaneBlockInterface> blockPath() {
        return this.blockPath;
    }

    @Override
    public String inputInterface() {
        return this.inputInterface;
    }

    @Override
    public String networkFunctionOfInIface() {
        return this.networkFunctionOfInIface;
    }

    @Override
    public String outputInterface() {
        return this.outputInterface;
    }

    @Override
    public String outputInterfaceConf() {
        return this.outputInterfaceConf;
    }

    @Override
    public String networkFunctionOfOutIface() {
        return this.networkFunctionOfOutIface;
    }

    @Override
    public int statefulInputPort() {
        return this.statefulInputPort;
    }

    @Override
    public String followers() {
        return this.followers;
    }

    @Override
    public boolean calculateChecksum() {
        return this.calculateChecksum;
    }

    @Override
    public String ethernetEncapConfiguration() {
        return this.ethernetEncapConfiguration;
    }

    @Override
    public String blackboxConfiguration() {
        return this.blackboxConfiguration;
    }

    @Override
    public boolean hasBlackboxConfiguration() {
        return !this.blackboxConfiguration.isEmpty();
    }

    @Override
    public boolean hasDecrementIpTtl() {
        for (ProcessingBlockClass block : this.postOperations()) {
            if (block == ProcessingBlockClass.DEC_IP_TTL) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isTotallyOffloadable() {
        if (this.writeOperationsAsString == null) {
            this.computeWriteOperations();
        }

        if (this.blackboxOperationsAsString == null) {
            this.computeBlackboxOperations();
        }

        // By default
        this.totallyOffloadable = true;

        // ... unless stateful (and not discarded) operations are present
        if (this.isStateful() && !this.isDiscarded()) {
            this.totallyOffloadable = false;
        }

        if (!this.writeOperationsAsString.isEmpty()) {
            this.totallyOffloadable = false;
        }

        if (!this.blackboxOperationsAsString.isEmpty()) {
            this.totallyOffloadable = false;
        }

        return this.totallyOffloadable;
    }

    @Override
    public boolean isPartiallyOffloadable() {
        return !this.isTotallyOffloadable();
    }

    @Override
    public boolean hasSoftwareRules() {
        return this.hasSoftwareRules;
    }

    @Override
    public void setTotallyOffloadable(boolean offloadable) {
        this.totallyOffloadable = offloadable;
    }

    @Override
    public String inputOperationsAsString() {
        return this.inputOperationsAsString;
    }

    @Override
    public void setInputOperationsAsString(String inputOps) {
        this.inputOperationsAsString = inputOps;
    }

    @Override
    public String softReadOperationsAsString() {
        this.computeReadOperations(false);
        return this.softReadOperationsAsString;
    }

    @Override
    public String readOperationsAsString() {
        this.computeReadOperations(true);
        return this.readOperationsAsString;
    }

    @Override
    public String writeOperationsAsString() {
        this.computeWriteOperations();
        return this.writeOperationsAsString;
    }

    @Override
    public String blackboxOperationsAsString() {
        this.computeBlackboxOperations();
        return this.blackboxOperationsAsString;
    }

    @Override
    public void setBlackboxOperationsAsString(String blackboxOps) {
        this.blackboxOperationsAsString = blackboxOps;
    }

    @Override
    public String outputOperationsAsString() {
        return this.outputOperationsAsString;
    }

    @Override
    public void setOutputOperationsAsString(String outputOps) {
        this.outputOperationsAsString = outputOps;
    }

    @Override
    public ApplicationId applicationId() {
        return this.applicationId;
    }

    @Override
    public DeviceId deviceId() {
        return this.deviceId;
    }

    @Override
    public ClassificationTreeInterface binaryTree() {
        return this.binaryTree;
    }

    @Override
    public boolean isStateful() {
        return this.operation.isStateful();
    }

    @Override
    public boolean isDiscarded() {
        // There is no block yet
        if (this.blockPath.size() == 0) {
            return false;
        }

        // Check the class of the last block
        ProcessingBlockClass lastBlockClass =
            this.blockPath.get(this.blockPath.size() - 1).blockClass();

        return lastBlockClass == ProcessingBlockClass.DISCARD;
    }

    @Override
    public boolean isSourceNapted() {
        FieldOperation srcPort = this.operation.fieldOperation(HeaderField.TP_SRC_PORT);
        if (srcPort == null) {
            return false;
        }

        return srcPort.operationType() == OperationType.WRITE_STATEFUL;
    }

    @Override
    public boolean isEmpty() {
        /**
         * A traffic class with packet filters is not empty.
         */
        for (Map.Entry<HeaderField, Filter> entry : this.packetFilter().entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter filter = entry.getValue();

            // At least one non-empty filter
            if (!filter.isNone()) {
                return false;
            }
        }

        /**
         * A traffic class with conditions is not empty.
         */
        for (Map.Entry<HeaderField, List<Condition>> entry : this.conditionMap().entrySet()) {
            for (Condition condition : entry.getValue()) {
                // At least one non-empty condition
                if (!condition.isNone()) {
                    return false;
                }
            }
        }

        /**
         * No filters and conditions, but some blackbox configuration instead!
         */
        if (this.hasBlackboxConfiguration()) {
            // Mark this
            this.solelyOwnedByBlackbox = true;
            return false;
        }

        return true;
    }

    @Override
    public boolean isSolelyOwnedByBlackbox() {
        return this.solelyOwnedByBlackbox;
    }

    @Override
    public boolean covers(TrafficClassInterface other) {
        return this.packetFilter.covers(other.packetFilter());
    }

    @Override
    public boolean conflictsWith(TrafficClassInterface other) {
        return this.packetFilter.conflictsWith(other.packetFilter());
    }

    @Override
    public String postRoutingPipeline() {
        String pipeline = "";

        for (ProcessingBlockClass block : this.postOperations()) {
            String blockStr = block.toString() + "(";

            // TODO: Handle checksum as per SNF
            // Avoid checksum recomputation by passing one extra argument
            // if ((block == ProcessingBlockClass.DEC_IP_TTL) &&
            //     !this.writeOperationsAsString.isEmpty()) {
            //     blockStr += "CALC_CHECKSUM false";
            // }

            pipeline += blockStr + ") -> ";
        }

        // Update the responsible member
        this.followers = pipeline;

        return pipeline;
    }

    @Override
    public void computeInputOperations(short port) {
        // Begin the journey from a device (input configuration)
        String inputConf = "input[" + port + "] -> ";

        // Get into to the right processing layer
        if (this.processingLayer() == ProcessingLayer.NETWORK) {
            inputConf += "MarkIPHeader(OFFSET 14) -> ";
        }

        this.inputOperationsAsString = inputConf;
    }

    @Override
    public void computeReadOperations(boolean all) {
        if (all) {
            this.readOperationsAsString = this.toIpClassifierPattern();
        } else {
            this.softReadOperationsAsString = this.onlySoftwareRules();
        }

        return;
    }

    @Override
    public void computeWriteOperations() {
        String ipRw = this.operation.toIPRewriterConfig();

        this.writeOperationsAsString = "";

        if (!ipRw.isEmpty()) {
            this.writeOperationsAsString += "IPRewriter(" + ipRw + ") -> ";
        }
    }

    @Override
    public void computeBlackboxOperations() {
        this.blackboxOperationsAsString = "";

        // No configuration available
        if (this.blackboxConfiguration.isEmpty()) {
            return;
        }

        // A standalone NF does not need the Click wrapper
        if (!this.isSolelyOwnedByBlackbox()) {
            this.blackboxOperationsAsString += "BlackboxNF(";
        }

        // ... only the content
        this.blackboxOperationsAsString += this.blackboxConfiguration;

        if (!this.isSolelyOwnedByBlackbox()) {
            this.blackboxOperationsAsString += ") -> ";
        }
    }

    @Override
    public void computeOutputOperations(short port) {
        String outputConf = "";

        // The output part starts from the Ethernet layer
        if (!this.ethernetEncapConfiguration.isEmpty()) {
            outputConf += "StoreEtherAddress(" + this.ethernetEncapConfiguration() + ", dst) -> ";
        }

        // Potential Blackbox NF integration: Click pipeline meets with a blackbox NF
        outputConf += this.blackboxOperationsAsString();

        // TODO: What if a subsequent traffic class uses the idle part??
        if (!this.ethernetEncapConfiguration.isEmpty()) {
            outputConf += "[" + port + "]output;";
        }

        this.outputOperationsAsString = outputConf;
    }

    @Override
    public boolean addBlock(NfvDataplaneBlockInterface block, int outputPort, Integer operationNo) {
        if (block == null) {
            throw new SynthesisException("Cannot add a NULL dataplane block to traffic class");
        }

        int noneFiltersNo = 0;

        // Add this block in the list
        this.blockPath.add(block);

        ProcessingBlockClass blockClass = block.blockClass();

        // Post-routing operations are kept in a map
        if ((blockClass == ProcessingBlockClass.DROP_BROADCASTS)  ||
            (blockClass == ProcessingBlockClass.IP_GW_OPTIONS)    ||
            (blockClass == ProcessingBlockClass.FIX_IP_SRC)       ||
            (blockClass == ProcessingBlockClass.DEC_IP_TTL)       ||
            (blockClass == ProcessingBlockClass.IP_OUTPUT_COMBO)  ||
            (blockClass == ProcessingBlockClass.IP_FRAGMENTER)) {
            this.addPostRoutingOperation(blockClass);
        // Strip will likely change the processing layer
        } else if (blockClass == ProcessingBlockClass.STRIP) {
            Strip stripBlock = (Strip) block.processor();
            if (stripBlock.length() == Strip.IP_LENGTH) {
                this.setProcessingLayer(ProcessingLayer.NETWORK);
            }
        // Unstrip changes the processing layer
        } else if (blockClass == ProcessingBlockClass.UNSTRIP) {
            Unstrip unstripBlock = (Unstrip) block.processor();
            if (unstripBlock.length() == Unstrip.IP_LENGTH) {
                this.setProcessingLayer(ProcessingLayer.LINK);
            }
        // CheckIPHeader will likely change the processing layer
        } else if (blockClass == ProcessingBlockClass.CHECK_IP_HEADER) {
            CheckIpHeader checkIpHdrBlock = (CheckIpHeader) block.processor();
            if (checkIpHdrBlock.offset() == CheckIpHeader.IP_OFFSET) {
                this.setProcessingLayer(ProcessingLayer.NETWORK);
            }
        // MarkIPHeader will likely change the processing layer
        } else if (blockClass == ProcessingBlockClass.MARK_IP_HEADER) {
            MarkIpHeader markIpHdrBlock = (MarkIpHeader) block.processor();
            if (markIpHdrBlock.offset() == MarkIpHeader.IP_OFFSET) {
                this.setProcessingLayer(ProcessingLayer.NETWORK);
            }
        // EtherEncap will give us useful information to output this traffic class
        } else if (blockClass == ProcessingBlockClass.ETHER_ENCAP) {
            // Interface configuration from EtherEncap
            EtherEncap etherEncapBlock = (EtherEncap) block.processor();
            this.ethernetEncapConfiguration = etherEncapBlock.dstMacStr();
        // StoreEtherAddress will give us useful information to output this traffic class
        } else if (blockClass == ProcessingBlockClass.STORE_ETHER_ADDRESS) {
            // Interface configuration from StoreEtherAddress
            StoreEtherAddress storeEtherAddrBlock = (StoreEtherAddress) block.processor();
            // We care about elements that set the destination
            if (storeEtherAddrBlock.offset().equals(StoreEtherAddress.DST_OFFSET)) {
                this.ethernetEncapConfiguration = storeEtherAddrBlock.macStr();
            }
        // TODO: Add support for EtherMirror
        } else if (blockClass == ProcessingBlockClass.ETHER_MIRROR) {
            // this.ethernetEncapConfiguration =
        // Support for Blackbox NFs
        } else if (blockClass == ProcessingBlockClass.TO_BLACKBOX_DEVICE) {
            ToBlackboxDevice toBlackboxBlock = (ToBlackboxDevice) block.processor();
            FromBlackboxDevice fromBlackboxBlock = toBlackboxBlock.peerDevice();
            this.blackboxConfiguration = FromBlackboxDevice.EXEC + " " + fromBlackboxBlock.executable() + ", " +
                                         FromBlackboxDevice.ARGS + " " + fromBlackboxBlock.arguments()  + "";
        } else if (blockClass == ProcessingBlockClass.TO_SNORT_DEVICE) {
            ToSnortDevice toSnortBlock = (ToSnortDevice) block.processor();
            FromSnortDevice fromSnortBlock = (FromSnortDevice) toSnortBlock.peerDevice();
            this.blackboxConfiguration = FromSnortDevice.EXEC + " " + fromSnortBlock.executable() + ", " +
                                         FromSnortDevice.ARGS + " " + fromSnortBlock.arguments()  + ", " +
                                         FromSnortDevice.FROM_RING + " " + fromSnortBlock.fromRing() + ", " +
                                         FromSnortDevice.TO_RING   + " " + fromSnortBlock.toRing()   + ", " +
                                         FromSnortDevice.TO_REVERSE_RING + " " + fromSnortBlock.toReverseRing();
        }

        // Last element of the chain -> no children
        if (outputPort < 0) {
            return true;
        }

        OutputClass outClass = block.outputClasses().get(outputPort);
        if (outClass == null) {
            log.error("\t\tNo output class found for {} at port {}", block.name(), outputPort);
            return false;
        }

        log.debug("\t\tBlock {} with output class: {}", block.blockClass(), outClass.toString());

        PacketFilter pf = outClass.packetFilter();

        if (pf == null) {
            log.error("\t\tNo packet filters found for {} at port {}", block.name(), outputPort);
            return false;
        }

        for (Map.Entry<HeaderField, Filter> entry : pf.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter filter = entry.getValue();

            log.debug("\t\t\tFilter to add: {}", filter.toString());

            FieldOperation fieldOp = this.operation.fieldOperation(headerField);

            if (fieldOp != null) {
                if ((fieldOp.operationType() == OperationType.WRITE_STATELESS) ||
                    (fieldOp.operationType() == OperationType.TRANSLATE)) {
                    StatelessOperationValue stlVal = (StatelessOperationValue) fieldOp.operationValue();

                    if (fieldOp.operationType() == OperationType.WRITE_STATELESS) {
                        if (!filter.match(stlVal.statelessValue())) {
                            if (this.packetFilter.get(headerField) != null) {
                                this.packetFilter.get(headerField).makeNone();
                                noneFiltersNo++;
                            }
                        }

                        if (operationNo.intValue() > 0) {
                            log.debug("\t\t\t------> Write Stateless");
                            operationNo.sum(operationNo.intValue(), 1);
                        }
                    } else {
                        Filter translatedFilter = filter;
                        if (stlVal.statelessValue() > 0) {
                            translatedFilter.translate(stlVal.statelessValue(), true);
                        }

                        if (!this.intersectFilter(translatedFilter)) {
                            noneFiltersNo++;
                        }

                        log.debug("\t\t\t------> Translate");
                    }
                } else if ((fieldOp.operationType() == OperationType.WRITE_STATEFUL) ||
                           (fieldOp.operationType() == OperationType.WRITE_ROUNDROBIN) ||
                           (fieldOp.operationType() == OperationType.WRITE_RANDOM)) {
                    StatefulOperationValue stfVal = (StatefulOperationValue) fieldOp.operationValue();

                    Filter writeCondition = new Filter(headerField, stfVal.statelessValue(), stfVal.statefulValue());
                    if (!filter.contains(writeCondition)) {
                        writeCondition = writeCondition.intersect(filter);
                        // FIXME: what if I have successive range writes?
                        if (!this.intersectFilterOperationInCondition(writeCondition, fieldOp)) {
                            noneFiltersNo++;
                        }
                    }

                    if (operationNo.intValue() > 0) {
                        log.debug("\t\t\t------>Write Stateful");
                        operationNo.sum(operationNo.intValue(), 1);
                    }

                    // Implies that there is stateful operation that cannot be offloaded
                    this.totallyOffloadable = false;
                } else if (fieldOp.operationType() == OperationType.WRITE_LB) {
                    StatefulSetOperationValue stfsVal = (StatefulSetOperationValue) fieldOp.operationValue();

                    Filter writeCondition = new Filter(headerField);

                    for (Long value : stfsVal.statefulSetValue()) {
                        writeCondition = writeCondition.unite(
                            new Filter(headerField, value.longValue())
                        );
                    }

                    if (!filter.contains(writeCondition)) {
                        writeCondition = writeCondition.intersect(filter);
                        // FIXME: what if I have successive range writes?
                        if (!this.intersectFilterOperationInCondition(writeCondition, fieldOp)) {
                            noneFiltersNo++;
                        }
                    }

                    if (operationNo.intValue() > 0) {
                        log.debug("\t\t\t------>Write LB");
                        operationNo.sum(operationNo.intValue(), 1);
                    }

                    // Implies that there is stateful operation that cannot be offloaded
                    this.totallyOffloadable = false;
                } else {
                    throw new SynthesisException(
                        "Non-modifying operation requested to add block to a traffic class"
                    );
                }
            } else {
                if (!this.intersectFilter(filter)) {
                    noneFiltersNo++;
                }
                log.debug("\t\t\t------>No operation");
            }
        }

        this.operation.compose(outClass.operation());

        return true;
    }

    @Override
    public String findInputInterface() {
        if (!this.isDiscarded()) {
            if (this.blockPath.size() > 1) {
                NfvDataplaneBlockInterface fromDev = null;

                // Get the very first element. This must be an input element.
                NfvDataplaneBlockInterface bl = this.blockPath.get(0);
                if (!ProcessingBlockClass.isInput(bl.blockClass().toString())) {
                    throw new SynthesisException(
                        "Failed to identify the input interface of a traffic class. " +
                        "The first element of this traffic class is not an input element"
                    );
                }

                fromDev = bl;

                // Get the device
                Device devBlock = (Device) fromDev.processor();

                // Update the input NF
                this.networkFunctionOfInIface = fromDev.networkFunctionOfOutIface();

                // Return the device's name
                return devBlock.devName();
            }
        }

        return "";
    }

    @Override
    public String findOutputInterface() {
        if (!this.isDiscarded()) {
            if (this.blockPath.size() > 1) {
                NfvDataplaneBlockInterface toDev = null;

                // Search backwards for the last output element
                for (int i = this.blockPath.size() - 1; i >= 0; --i) {
                    NfvDataplaneBlockInterface bl = this.blockPath.get(i);
                    if (ProcessingBlockClass.isOutput(bl.blockClass().toString())) {
                        toDev = bl;
                        break;
                    }
                }

                // Something is not right here!
                if (toDev == null) {
                    throw new SynthesisException(
                        "Failed to identify the output interface of a traffic class. " +
                        "The last element of this traffic class is not an output element"
                    );
                }

                // Get the device
                Device devBlock = (Device) toDev.processor();

                // Update the output NF
                this.networkFunctionOfOutIface = toDev.networkFunctionOfOutIface();

                // Return the device's name
                return devBlock.devName();
            }
        }

        return "";
    }

    @Override
    public void buildBinaryTree() {
        int priority   = DEFAULT_PRIORITY;
        int outputPort = DUMMY_OUTPUT_PORT;
        ClickFlowRuleAction action = this.isDiscarded() ? DROP : ALLOW;

        // Retrieve the read operations of this traffic class
        String classifierConf = this.readOperationsAsString();

        // Build a binary classification tree for this traffic class
        this.binaryTree = this.buildClassificationTree(
            classifierConf, outputPort, priority, action
        );
    }

    @Override
    public void generatePacketFilters() {
        int priority = DEFAULT_PRIORITY;
        int outputPort = DUMMY_OUTPUT_PORT;
        ClickFlowRuleAction action = this.isDiscarded() ? DROP : ALLOW;

        // Retrieve the read operations of this traffic class
        String classifierConf = this.readOperationsAsString();

        // Build a binary classification tree for this traffic class
        this.binaryTree = this.buildClassificationTree(
            classifierConf, outputPort, priority, action
        );

        // For each datapath component of this traffic class
        for (String tc : this.binaryTree.datapathTrafficClasses()) {
            log.info("\t Traffic class filters: {}", tc);

            // Get the packet filters
            Set<TextualPacketFilter> packetFilters = this.binaryTree.getPacketFiltersOf(tc);

            // A packet filter might contain ambiguous header fields; store them here
            Set<TextualPacketFilter> ambiguousPacketFilters =
                    Sets.<TextualPacketFilter>newConcurrentHashSet();

            // Go through the packet filters of this component to identify the ambiguous ones
            Iterator<TextualPacketFilter> iterator = packetFilters.iterator();
            while (iterator.hasNext()) {
                TextualPacketFilter pf = iterator.next();

                // Find the type of this traffic class judging by its packet filters
                if (this.type == null) {
                    this.type = pf.type();
                } else {
                    if ((this.type == TrafficClassType.NEUTRAL) && (this.type != pf.type())) {
                        this.type = pf.type();
                    } else if ((this.type != pf.type()) && (pf.type() != TrafficClassType.NEUTRAL)) {
                        this.type = TrafficClassType.AMBIGUOUS;
                    }
                }

                // Ambiguous packet filters are temporarily removed from this traffic class
                if (pf.isAmbiguousGivenSet(packetFilters)) {
                    log.debug("\t\t Ambiguous packet filter {}", pf);
                    ambiguousPacketFilters.add(pf);
                    iterator.remove();
                }
            }

            if (this.type == null) {
                this.type = TrafficClassType.NEUTRAL;
            }

            // One traffic class might correspond to more than one rules (a list)
            List<Set<TextualPacketFilter>> targetPacketFilters = null;

            /**
             * The existence of ambiguous packet filters implies that this datapath component
             * needs to be split into more than one hardware rules.
             * Let's find out how many rules this component requires.
             */
            if (ambiguousPacketFilters.size() > 0) {
                targetPacketFilters = this.trafficClassToMultiplePacketFilters(
                    packetFilters, ambiguousPacketFilters
                );
            // No need for extra rules (1 traffic class == 1 rule)
            } else {
                targetPacketFilters = new ArrayList<Set<TextualPacketFilter>>();
                targetPacketFilters.add(packetFilters);
            }
            checkNotNull(
                targetPacketFilters,
                "Target-specific packet filters for traffic class pattern " + tc + "are NULL"
            );

            /**
             * Go through the list and check which of them can be translated into OpenFlow rules.
             */
            boolean isHwCompliant = true;
            for (Set<TextualPacketFilter> pfs : targetPacketFilters) {
                // This rule is not hardware-compliant
                if (!TextualPacketFilter.isHardwareCompliant(packetFilters)) {
                    log.warn(
                        "\t\t Cannot be translated into OpenFlow rules --> software-based traffic class"
                    );
                    isHwCompliant = false;
                    break;
                }
            }

            this.packetFiltersTargetMap.put(tc, isHwCompliant ? TARGET_HW : TARGET_SW);

            // Keep these filters in the appropriate map for future usage
            if (isHwCompliant) {
                this.hwPacketFiltersMap.put(tc, targetPacketFilters);
            } else {
                this.swPacketFiltersMap.put(tc, targetPacketFilters);
                this.hasSoftwareRules = true;
            }
        }

        // printPacketFilters();

        return;
    }

    @Override
    public Map<String, List<Set<TextualPacketFilter>>> hwPacketFiltersMap() {
        return this.hwPacketFiltersMap;
    }

    @Override
    public Map<String, List<Set<TextualPacketFilter>>> swPacketFiltersMap() {
        return this.swPacketFiltersMap;
    }

    @Override
    public Map<String, String> packetFiltersTargetMap() {
        return this.packetFiltersTargetMap;
    }

    @Override
    public Set<FlowRule> toOpenFlowRules(
            ApplicationId applicationId,
            DeviceId      deviceId,
            URI           tcId,
            long          inputPort,
            long          queueIndex,
            long          outputPort,
            RxFilter      rxFilter,
            RxFilterValue rxFilterValue,
            boolean       tagging) {
        // Memory to host the generated OpenFlow rules
        Set<FlowRule> rules = Sets.<FlowRule>newConcurrentHashSet();

        checkNotNull(applicationId, "Application ID for traffic class " + this.id() + " is NULL");
        checkNotNull(deviceId, "Device ID for traffic class " + this.id() + " is NULL");
        checkNotNull(this.binaryTree, "Binary classification tree for traffic class " + this.id() + " is NULL");

        log.debug("Filter  Type: {}", rxFilter);
        log.debug("Filter Value: {}", rxFilterValue);

        if (tagging && ((rxFilter == null) || (rxFilterValue == null))) {
            throw new DeploymentException(
                "Insufficient tagging information; cannot generate hardware rules"
            );
        }

        // Update the application and device ID members of this traffic class
        this.applicationId = applicationId;
        this.deviceId = deviceId;
        int priority = DEFAULT_PRIORITY;
        ClickFlowRuleAction action = this.isDiscarded() ? DROP : ALLOW;
        boolean isServerDevice = this.deviceId.toString().contains("rest:");

        // For each datapath component of this traffic class
        for (String tc : this.binaryTree.datapathTrafficClasses()) {

            // Denotes whether this is a software or hardware-based traffic class
            String target = this.packetFiltersTargetMap.get(tc);

            // This is a software-based traffic class, skip
            if ((target == null) || target.equals(TARGET_SW)) {
                continue;
            }

            // Get the hardware packet filters of this traffic class
            List<Set<TextualPacketFilter>> targetPacketFilters = this.hwPacketFiltersMap.get(tc);
            checkNotNull(
                targetPacketFilters,
                "Hardware packet filters for traffic class pattern '" + tc + "' are NULL"
            );

            // Go through the list and translate each item into an OpenFlow rule
            for (Set<TextualPacketFilter> pfs : targetPacketFilters) {
                FlowRule rule = this.packetFiltersToOpenFlowRule(
                    applicationId, deviceId, tcId, pfs,
                    inputPort, queueIndex, outputPort,
                    priority, rxFilter, rxFilterValue,
                    action, isServerDevice
                );
                rules.add(rule);
            }

            log.debug("");
        }

        return rules;
    }

    @Override
    public String onlySoftwareRules() {
        String rules = "";

        if (this.binaryTree == null) {
            return rules;
        }

        // For each datapath component of this traffic class
        for (String tc : this.binaryTree.datapathTrafficClasses()) {
            List<Set<TextualPacketFilter>> targetPacketFilters;

            // Denotes whether this is a software or hardware-based traffic class
            String target = this.packetFiltersTargetMap.get(tc);

            // This is a hardware-based traffic class, skip
            if ((target == null) || target.equals(TARGET_HW)) {
                continue;
            }

            targetPacketFilters = this.swPacketFiltersMap.get(tc);

            boolean atLeastOne = false;
            for (Set<TextualPacketFilter> pfs : targetPacketFilters) {
                String rule = this.packetFiltersToSoftwareRule(pfs);
                if (rule.isEmpty()) {
                    continue;
                }
                atLeastOne = true;
                rules += rule + ", ";
            }

            // Removes trailing  ", "
            if (atLeastOne) {
                rules = rules.substring(0, rules.length() - 2);
            }
        }

        return rules;
    }

    @Override
    public String toString() {
        String output = "\n================= Begin Traffic Class =================\n";
        output += "ID: " + id() + "\n";
        output += "Filters:\n";
        output += this.readOperationsAsString();

        output += "\n\nConditions on Write operations:\n";
        for (Map.Entry<HeaderField, List<Condition>> entry : this.conditionMap.entrySet()) {
            for (Condition c : entry.getValue()) {
                output += "\t" + c.toString() + "\n";
            }
        }

        output += this.operation.toString();

        output += "\nPassed elements: \n\t";
        for (NfvDataplaneBlockInterface block : this.blockPath) {
            output += block.blockClass().toString();
            if (block.isLeaf()) {
                output += "\n";
            } else {
                output += "->";
            }
        }

        output += "=================  End Traffic Class  =================\n";

        return output;
    }

    /**
     * Encodes a traffic class into a binary classification tree.
     *
     * @param a traffic class
     * @param the output port of this traffic class
     * @param the priority of this traffic class
     * @param the action of this traffic class
     * @throws RuntimeException if the verification of the tree fails
     * @return a binary classification tree that encodes this traffic class
     */
    private ClassificationTreeInterface buildClassificationTree(
        String trafficClass, int outputPort, int priority, ClickFlowRuleAction action) {
        /**
         * Post-process the traffic class to simplify the parsing operations
         * Otherwise, it requires regular expressions, such as
         * "\\s*[\\(\\)]+\\s*" to split the tokens
         */
        trafficClass = "(" + trafficClass + ")";
        trafficClass = trafficClass.replace("(", " ( ");
        trafficClass = trafficClass.replace(")", " ) ");
        trafficClass = trafficClass.replace("!", " ! ");

        // Build the classification tree
        ClassificationTreeInterface tree = new ClassificationTree(trafficClass, outputPort, priority, action);
        try {
            tree.buildTree();
        } catch (RuntimeException rtEx) {
            throw rtEx;
        }

        // Fetch the datapath traffic classes that derive from the classification tree.
        List<String> dpTrafficClasses = tree.toHardwareRules();

        // Verify that the constructed tree complies to the syntax
        tree.verifyTree();

        return tree;
    }

    /**
     * Translates a set of packet filters that comprise a traffic class
     * into a single OpenFlow rule.
     *
     * @param applicationId the application that contains
     *        this traffic class
     * @param deviceId the device where this traffic class will
     *        be installed
     * @param tcId the traffic class ID where the rule
     *        belongs to
     * @param packetFilters set of packet filters that comprise
     *        a traffic class
     * @param inputPort the input port where packet arrived
     * @param queueIndex the NIC queue where input packet will be sent
     * @param outputPort the output port to be chosen for this
     *        traffic class (if action is ALLOW)
     * @param priority the priority of this traffic class
     * @param rxFilter the type of tagging supported by this device
     * @param rxFilterValue the actual tag to be used
     * @param action the action of this traffic class (ALLOW or DROP)
     * @param isServerDevice indicates whether the target device is a server
     * @return a FlowRule object that encodes the traffic class in hardware
     */
    private FlowRule packetFiltersToOpenFlowRule(
            ApplicationId            applicationId,
            DeviceId                 deviceId,
            URI                      tcId,
            Set<TextualPacketFilter> packetFilters,
            long                     inputPort,
            long                     queueIndex,
            long                     outputPort,
            int                      priority,
            RxFilter                 rxFilter,
            RxFilterValue            rxFilterValue,
            ClickFlowRuleAction      action,
            boolean                  isServerDevice) {

        // Rule builder
        FlowRule.Builder ruleBuilder = new DefaultFlowRule.Builder();

        ruleBuilder.forDevice(deviceId);
        ruleBuilder.fromApp(applicationId);
        ruleBuilder.withPriority(priority);
        ruleBuilder.makePermanent();

        /**
         * MATCH
         */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        // Integrate all the textual packet filters into the traffic selector
        TextualPacketFilter.updateAbstractSelector(selector, packetFilters);
        for (TextualPacketFilter pf : packetFilters) {
            pf.updateTrafficSelector(selector, packetFilters);
        }
        ruleBuilder.withSelector(selector.build());

        /**
         * ACTION
         */
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        // Drop rule
        if (action == ClickFlowRuleAction.DROP) {
            treatment.drop();
        // Allow rule
        } else {
            // Might or might not have filtering
            if (rxFilter != null) {
                // Verify that the Rx filter is supported
                if (!RxFilter.isSupported(rxFilter)) {
                    throw new DeploymentException(
                        "Unsupported tagging mechanism " + rxFilter
                    );
                }

                // Use a desired header field as a tag
                if (rxFilter == RxFilter.VLAN) {
                    VlanId tag = ((VlanRxFilterValue) rxFilterValue).value();
                    log.debug("VLAN tagging using {}", tag);
                    treatment.pushVlan();
                    treatment.setVlanId(tag);
                } else if (rxFilter == RxFilter.MAC) {
                    MacAddress tag = ((MacRxFilterValue) rxFilterValue).value();
                    log.debug("MAC tagging using {}", tag);
                    treatment.setEthDst(tag);
                } else if (rxFilter == RxFilter.MPLS) {
                    MplsLabel tag = ((MplsRxFilterValue) rxFilterValue).value();
                    log.debug("MPLS tagging using {}", tag);
                    treatment.pushMpls();
                    treatment.setMpls(tag);
                }
            }

            // Decrement IP TTL is requested
            if (this.hasDecrementIpTtl()) {
                treatment.decNwTtl();
            }

            // This rule contains input dispatching information
            if ((inputPort >= 0) && (queueIndex >= 0)) {
                treatment.add(
                    Instructions.setQueue(queueIndex, PortNumber.portNumber(inputPort))
                );
            }

            // This rule contains output information
            if (outputPort >= 0) {
                treatment.add(
                    Instructions.createOutput(PortNumber.portNumber(outputPort))
                );
            }
        }
        ruleBuilder.withTreatment(treatment.build());

        FlowRule rule = ruleBuilder.build();

        // A server device's NIC rule requires a few more things
        if (isServerDevice) {
            NicFlowRule.Builder serverRuleBuilder = new DefaultDpdkNicFlowRule.Builder();

            serverRuleBuilder.fromFlowRule(rule);
            serverRuleBuilder.withTrafficClassId(tcId.toString());
            // TODO: Pass the interface name as announced by the agent (e.g., fd0)
            serverRuleBuilder.withInterfaceName(this.inputInterface());
            serverRuleBuilder.assignedToCpuCore(queueIndex);

            return serverRuleBuilder.build();
        }

        return rule;
    }

    /**
     * Translates a set of packet filters that comprise a traffic class
     * into a software-based rule.
     *
     * @param packetFilters set of packet filters that comprise a traffic class
     * @return a string-based rule that encodes the traffic class in software
     */
    private String packetFiltersToSoftwareRule(Set<TextualPacketFilter> packetFilters) {
        String rule = "";

        // Integrate all packet filters into a string-based rule
        boolean atLeastOne = false;
        for (TextualPacketFilter pf : packetFilters) {
            String filterStr = pf.filtersToString();
            if (filterStr.isEmpty()) {
                continue;
            }

            rule += "(" + filterStr + ") && ";
            atLeastOne = true;
        }

        // Removes trailing  " && "
        if (atLeastOne) {
            rule = rule.substring(0, rule.length() - 4);
        }

        return rule;
    }

    /**
     * Prints the software and hardware-based packet filters of this traffic class.
     */
    private void printPacketFilters() {
        // For each datapath component of this traffic class
        for (String tc : this.binaryTree.datapathTrafficClasses()) {
            List<Set<TextualPacketFilter>> targetPacketFilters;

            // Denotes whether this is a software or hardware-based traffic class
            String target = this.packetFiltersTargetMap.get(tc);

            // This is a software-based traffic class
            boolean isHwCompliant = false;
            if (target.equals(TARGET_SW)) {
                targetPacketFilters = this.swPacketFiltersMap.get(tc);
            } else {
                isHwCompliant = true;
                targetPacketFilters = this.hwPacketFiltersMap.get(tc);
            }

            log.info("Traffic class '{}' is {}", tc, isHwCompliant ? "HW-based" : "SW-based");
        }
    }

    /**
     * Translates a set of packet filters that comprise a traffic class into a
     * list of individual packet filter sets that can be translated into hardware
     * rules.
     *
     * @param packetFilters set of packet filters that comprise the traffic class
     * @param ambiguousPacketFilters subset of the above packet filters that
     *        are ambiguous (will be translated into more than one sets)
     * @return list of individual packet filter sets
     */
    private List<Set<TextualPacketFilter>> trafficClassToMultiplePacketFilters(
            Set<TextualPacketFilter> packetFilters,
            Set<TextualPacketFilter> ambiguousPacketFilters) {
        List<Set<TextualPacketFilter>> hwPacketFilters = new ArrayList<Set<TextualPacketFilter>>();

        // Create the new list of sets, where in each list one the augmented packet filters will exist
        for (TextualPacketFilter ambPf : ambiguousPacketFilters) {
            // Turn an ambiguous packet filter into a set of individual ones
            Set<TextualPacketFilter> augmPfs = ambPf.augmentAmbiguousPacketFilter();

            if (hwPacketFilters.size() == 0) {
                for (TextualPacketFilter augmPf : augmPfs) {
                    Set<TextualPacketFilter> newPacketFilters =
                            Sets.<TextualPacketFilter>newConcurrentHashSet();
                    newPacketFilters.add(augmPf);
                    hwPacketFilters.add(newPacketFilters);
                }
            } else {
                for (Set<TextualPacketFilter> exPfs : hwPacketFilters) {
                    for (TextualPacketFilter augmPf : augmPfs) {
                        /**
                         * We add this packet filter only if it complies with the existing ones.
                         * Incompliance might occur if the new packet filter is e.g.,
                         * dst tcp port 80 while the set contains UDP-based filters.
                         */
                        if (augmPf.compliesWithPacketFilterSet(exPfs)) {
                            exPfs.add(augmPf);
                        }
                    }
                }
            }
        }

        // Now, append the original set of packet filters to each entry of the list
        for (Set<TextualPacketFilter> exPfs : hwPacketFilters) {
            exPfs.addAll(packetFilters);
        }

        return hwPacketFilters;
    }

    /**
     * Intersects a new filter with the filters of this traffic class.
     *
     * @param newFilter to be intersected with the
     *        filters of this traffic class
     * @return boolean intersection status
     */
    private boolean intersectFilter(Filter newFilter) {
        HeaderField newHeaderField = newFilter.headerField();

        if (!this.packetFilter.containsKey(newHeaderField)) {
            this.packetFilter.put(newHeaderField, newFilter);
        } else {
            Filter f = this.packetFilter.get(newHeaderField);
            f.intersect(newFilter);
        }

        return !this.packetFilter.get(newHeaderField).isNone();
    }

    /**
     * Intersects a new condition (filter + operation) with the
     * condition map this traffic class.
     *
     * @param filter to be intersected with the condition map of this traffic class
     * @param operation to be intersected with the condition map of this traffic class
     * @return boolean intersection status
     */
    private boolean intersectFilterOperationInCondition(Filter filter, FieldOperation operation) {
        HeaderField condHeaderfield = filter.headerField();

        // Does not exist, add it
        if (!this.conditionMap.containsKey(condHeaderfield)) {
            Condition condition = new Condition(
                condHeaderfield,
                filter,
                operation,
                this.blockPath.get(this.blockPath.size() - 1)
            );

            this.conditionMap.addCondition(condHeaderfield, condition);
        } else {
            List<Condition> conditions = this.conditionMap.get(condHeaderfield);

            Condition condition = conditions.get(conditions.size() - 1);

            if (!condition.intersect(filter)) {
                throw new SynthesisException(
                    "Failed to intersect filter " + filter.toString() +
                    " with the condition map of a traffic class"
                );
            }
        }

        return true;
    }

    /**
     * Add a post-routing element to the list of elements to be synthesized.
     *
     * @param blockClass block class where the operation will be added
     */
    private void addPostRoutingOperation(ProcessingBlockClass blockClass) {
        /**
         * These elements modify part of the header, hence
         * checksum(s) need(s) to be re-calculated.
         */
        if (ProcessingBlockClass.isPostRouting(blockClass.toString()) &&
            !this.calculateChecksum) {
            this.calculateChecksum = true;
        }

        if (!this.hasPostRoutingOperation(blockClass)) {
            this.postOperations.add(blockClass);
        }
    }

    /**
     * Checks whether the input block class has post-routing operations.
     *
     * @param blockClass block class to be checked
     * @return boolean possesion status
     */
    private boolean hasPostRoutingOperation(ProcessingBlockClass blockClass) {
        return this.postOperations.contains(blockClass);
    }

    /**
     * Returns the configuration of the output interface
     * associated with this traffic class.
     *
     * @return output interface configuration as a string
     */
    public String findOutputInterfaceConf() {
        if (!this.isDiscarded()) {
            if (this.blockPath.size() > 1) {
                NfvDataplaneBlockInterface toDev = null;

                // Search backwards for the last output element
                for (int i = this.blockPath.size() - 1; i >= 0; --i) {
                    NfvDataplaneBlockInterface bl = this.blockPath.get(i);
                    if (ProcessingBlockClass.isOutput(bl.blockClass().toString())) {
                        toDev = bl;
                        break;
                    }
                }

                // Something is not right here!
                if (toDev == null) {
                    return "";
                }

                // Configuration contains interface name and other parameters
                String[] conf = toDev.basicConfiguration().split(",");

                // Turn it to a string excluding the interface name
                this.outputInterfaceConf = String.join(
                    ",", Arrays.copyOfRange(conf, 1, conf.length)
                );

                return this.outputInterfaceConf;
            }
        }

        return "";
    }

    /**
     * Translates the traffic class into a format understandable by
     * an IPClassifier element.
     *
     * @return string-based IPClassifier traffic class representation
     */
    private String toIpClassifierPattern() {
        String output = "";

        boolean atLeastOne = false;
        for (Map.Entry<HeaderField, Filter> entry : this.packetFilter.entrySet()) {
            Filter filter = entry.getValue();
            String filterStr = filter.toIpClassifierPattern();

            if (!filterStr.isEmpty()) {
                atLeastOne = true;
                output += "(" + filterStr + ") && ";
            }
        }

        // Remove trailing " && "
        if (atLeastOne) {
            output = output.substring(0, output.length() - 4);
        }

        return output;
    }

}
