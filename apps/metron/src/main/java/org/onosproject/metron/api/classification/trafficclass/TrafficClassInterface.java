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

import org.onosproject.metron.api.classification.ClassificationTreeInterface;
import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.trafficclass.condition.ConditionMap;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.classification.trafficclass.operation.Operation;
import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingLayer;

import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;

import java.net.URI;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * Interface that represents a Metron traffic class.
 */
public interface TrafficClassInterface {

    /**
     * Returns the ID of this traffic class.
     *
     * @return ID of this traffic class
     */
    URI id();

    /**
     * Sets the ID of this traffic class.
     *
     * @param tcId ID of this traffic class
     */
    void setId(URI tcId);

    /**
     * Returns the type of this traffic class.
     * The type depends on the protocol type
     * of its rules (e.g., ICMP, TCP, UDP).
     *
     * @return type of this traffic class
     */
    TrafficClassType type();

    /**
     * Returns the processing layer on which this
     * traffic class operates.
     *
     * @return ProcessingLayer processing layer
     *         of this traffic class
     */
    ProcessingLayer processingLayer();

    /**
     * Sets the processing layer on which this
     * traffic class operates.
     *
     * @param procLayer ProcessingLayer processing layer
     *        of this traffic class
     */
    void setProcessingLayer(ProcessingLayer procLayer);

    /**
     * Returns the packet filter of this traffic class.
     *
     * @return packet filter of this traffic class
     */
    PacketFilter packetFilter();

    /**
     * Adds a packet filter to this traffic class.
     *
     * @param pf packet filter to be added to this traffic class
     */
    void addPacketFilter(PacketFilter pf);

    /**
     * Returns the condition map of this traffic class.
     *
     * @return condition map of this traffic class
     */
    ConditionMap conditionMap();

    /**
     * Adds a condition map to this traffic class.
     *
     * @param cm condition map to be added to this traffic class
     */
    void addConditionMap(ConditionMap cm);

    /**
     * Returns the operation of this traffic class.
     *
     * @return operation of this traffic class
     */
    Operation operation();

    /**
     * Returns the post-routing operations of this traffic class.
     *
     * @return list of post-routing operations of this traffic class
     */
    List<ProcessingBlockClass> postOperations();

    /**
     * Returns the list of blocks of this traffic class.
     *
     * @return list of blocks of this traffic class
     */
    List<NfvDataplaneBlockInterface> blockPath();

    /**
     * Returns the input interface of this traffic class.
     *
     * @return input interface of this traffic class
     */
    String inputInterface();

    /**
     * Sets the input interface of this traffic class.
     *
     * @param inputInterface input interface of this traffic class
     */
    void setInputInterface(String inputInterface);

    /**
     * Returns the name of the network function that holds
     * the input interface of this traffic class.
     *
     * @return name of the network function that holds
     *         the input interface of this traffic class
     */
    String networkFunctionOfInIface();

    /**
     * Returns the output interface of this traffic class.
     *
     * @return output interface of this traffic class
     */
    String outputInterface();

    /**
     * Sets the output interface of this traffic class.
     *
     * @param outputInterface output interface of this traffic class
     */
    void setOutputInterface(String outputInterface);

    /**
     * Returns the configuration of this
     * traffic class's output interface.
     *
     * @return configuration of this traffic
     *         class's output interface
     */
    String outputInterfaceConf();

    /**
     * Returns the name of the network function that holds
     * the output interface of this traffic class.
     *
     * @return name of the network function that holds
     *         the output interface of this traffic class
     */
    String networkFunctionOfOutIface();

    /**
     * Returns the stateful input port of this traffic class.
     *
     * @return stateful input port of this traffic class
     */
    int statefulInputPort();

    /**
     * Returns the chain of elements that follow this traffic class.
     *
     * @return chain of elements that follow this traffic class
     */
    String followers();

    /**
     * Returns whether this traffic class
     * requires checksum calculation.
     *
     * @return boolean checksum calculation requirement
     */
    boolean calculateChecksum();

    /**
     * Returns the Ethernet encapsulation information
     * of this traffic class.
     *
     * @return string Ehternet encapsulation information
     *         of this traffic class
     */
    String ethernetEncapConfiguration();

    /**
     * Returns the blackbox configuration of this
     * traffic class. Indicates the interaction between
     * Click elements and blackbox NFs.
     *
     * @return string Blackbox configuration of this traffic class
     */
    String blackboxConfiguration();

    /**
     * Returns whether this traffic class goes through any blackbox NF.
     *
     * @return boolean has blackbox configuration not not
     */
    boolean hasBlackboxConfiguration();

    /**
     * Returns the monitor configuration of this
     * traffic class. Indicates the existence of
     * (Average)Counter Click elements.
     *
     * @return string monitor configuration of this traffic class
     */
    String monitorConfiguration();

    /**
     * Returns whether this traffic class goes through any monitoring element.
     *
     * @return boolean has monitor configuration not not
     */
    boolean hasMonitorConfiguration();

    /**
     * Returns whether this traffic class requires its
     * IP TTL field to be decremented.
     *
     * @return boolean decrement IP TTL not not
     */
    boolean hasDecrementIpTtl();

    /**
     * Returns whether this traffic class can be
     * completely offloaded to the network.
     *
     * @return boolean total offloadability
     */
    boolean isTotallyOffloadable();

    /**
     * Returns whether this traffic class can be
     * partially offloaded to the network.
     *
     * @return boolean partial offloadability
     */
    boolean isPartiallyOffloadable();

    /**
     * Returns whether this traffic class contains
     * read operations that cannot be offloaded.
     *
     * @return boolean software-based reads
     */
    boolean hasSoftwareRules();

    /**
     * Sets the offloadability of  this traffic class.
     *
     * @param offloadable boolean offloadability
     */
    void setTotallyOffloadable(boolean offloadable);

    /**
     * Returns the input operations of this traffic class.
     *
     * @return input operations as a string
     */
    String inputOperationsAsString();

    /**
     * Sets the input operations of this traffic class.
     *
     * @param inputOps input operations as a string
     */
    void setInputOperationsAsString(String inputOps);

    /**
     * Returns only the software-based read operations
     * of this traffic class.
     *
     * @return software read operations as a string
     */
    String softReadOperationsAsString();

    /**
     * Returns the read operations of this traffic class.
     *
     * @return read operations as a string
     */
    String readOperationsAsString();

    /**
     * Returns the write operations of this traffic class.
     *
     * @return write operations as a string
     */
    String writeOperationsAsString();

    /**
     * Returns the blackbox operations of this traffic class.
     * This indicates the interaction between Click elements
     * and blackbox NFs.
     *
     * @return blackbox operations as a string
     */
    String blackboxOperationsAsString();

    /**
     * Sets the blackbox operations of this traffic class.
     *
     * @param blackboxOps blackbox operations as a string
     */
    void setBlackboxOperationsAsString(String blackboxOps);

    /**
     * Returns the monitor operations of this traffic class.
     *
     * @return monitor operations as a string
     */
    String monitorOperationsAsString();

    /**
     * Sets the monitor operations of this traffic class.
     *
     * @param monitorOps monitor operations as a string
     */
    void setMonitorOperationsAsString(String monitorOps);

    /**
     * Returns the output operations of this traffic class.
     *
     * @return output operations as a string
     */
    String outputOperationsAsString();

    /**
     * Sets the output operations of this traffic class.
     *
     * @param outputOps output operations as a string
     */
    void setOutputOperationsAsString(String outputOps);

    /**
     * Returns the application ID of this traffic class.
     *
     * @return application ID
     */
    ApplicationId applicationId();

    /**
     * Returns the device ID of this traffic class.
     *
     * @return device ID
     */
    DeviceId deviceId();

    /**
     * Returns the binary classification tree that encodes
     * this traffic class.
     *
     * @return binary classification tree
     */
    ClassificationTreeInterface binaryTree();

    /**
     * Returns whether this traffic class
     * contains stateful operations or not.
     *
     * @return boolean statefulness status
     */
    boolean isStateful();

    /**
     * Returns whether this traffic class leads
     * to a drop operation or not.
     *
     * @return boolean drop status
     */
    boolean isDiscarded();

    /**
     * Returns whether this traffic class has traversed
     * a stateful NAPT operation.
     *
     * @return boolean NAPT traversal status
     */
    boolean isSourceNapted();

    /**
     * Returns whether this traffic class contains any
     * operations or not.
     * By operations we mean:
     * \--> Packet filters
     * \--> Conditions or
     * \--> Blackbox configuration
     *
     * @return boolean emptiness status
     */
    boolean isEmpty();

    /**
     * Returns whether this traffic class solely
     * belongs to a blackbox NF or not.
     *
     * @return boolean blackbox ownership status
     */
    boolean isSolelyOwnedByBlackbox();

    /**
     * Checks whether one traffic class fully
     * covers another traffic class.
     *
     * @param other traffic class to be checked against this one
     * @return boolean coverage status
     */
    boolean covers(TrafficClassInterface other);

    /**
     * Checks whether two traffic classes have
     * conflicting packet filters or not.
     *
     * @param other traffic class to be checked against this one
     * @return boolean conflict status
     */
    boolean conflictsWith(TrafficClassInterface other);

    /**
     * Returns the pipeline of blocks that
     * follow the stateful operations of
     * this traffic class, before we arrive to an output block.
     *
     * @return pipeline of blocks as a string
     */
    String postRoutingPipeline();

    /**
     * Populates the input operations of
     * this traffic class into a string.
     *
     * @param port the device where traffic enters
     */
    void computeInputOperations(long port);

    /**
     * Populates the read operations of this
     * traffic class into a string.
     *
     * @param all computes all read operations,
     *        otherwise only the software-based ones
     */
    void computeReadOperations(boolean all);

    /**
     * Populates the write operations of this
     * traffic class into a string.
     */
    void computeWriteOperations();

    /**
     * Populates the blackbox operations of this
     * traffic class into a string.
     */
    void computeBlackboxOperations();

    /**
     * Populates the monitor operations of this
     * traffic class into a string.
     */
    void computeMonitorOperations();

    /**
     * Populates the output operations of this
     * traffic class into a string.
     *
     * @param port the device where traffic exits
     */
    void computeOutputOperations(long port);

    /**
     * Intersects a new condition (filter + operation)
     * with the condition map of this traffic class.
     *
     * @param block to be added in the path of blocks
     *        of this traffic class
     * @param outputPort output port of this block
     * @param operationNo number of operations of this block
     * @return boolean status
     */
    boolean addBlock(
        NfvDataplaneBlockInterface block,
        int outputPort,
        Integer operationNo
    );

    /**
     * Returns the input interface associated with this
     * traffic class.
     *
     * @return input interface as a string
     */
    String findInputInterface();

    /**
     * Returns the output interface associated with this
     * traffic class.
     *
     * @return output interface as a string
     */
    String findOutputInterface();

    /**
     * Converts this traffic class into a
     * binary classification tree.
     */
    void buildBinaryTree();

    /**
     * Converts this traffic class into a set of software
     * and/or hardware-based packet filters.
     * This method is executed after a traffic class is built
     * with 'buildBinaryTree' and after the compression scheme
     * has potentially modified some of the traffic classes.
     */
    void generatePacketFilters();

    /**
     * Returns the map between traffic class patterns and
     * their list of software packet filters.
     *
     * @return map between traffic class patterns and
     *         their list of software packet filters
     */
    Map<String, List<Set<TextualPacketFilter>>> swPacketFiltersMap();

    /**
     * Returns the map between traffic class patterns and
     * their list of hardware packet filters.
     * These filters are offloadable to OpenFlow hardware.
     *
     * @return map between traffic class patterns and
     *         their list of hardware packet filters
     */
    Map<String, List<Set<TextualPacketFilter>>> hwPacketFiltersMap();

    /**
     * Returns the map between traffic class patterns and
     * their target deployment (HW or SW).
     *
     * @return map between traffic class patterns and
     *         their target deployment
     */
    Map<String, String> packetFiltersTargetMap();

    /**
     * Returns the read operations of this traffic class
     * that cannot be offloaded, thus run in software.
     *
     * @return a set of rules as a string
     */
    String onlySoftwareRules();

    /**
     * Converts the read and stateless write operations
     * of this traffic class into OpenFlow rules.
     * Conversion is not always possible, hence this
     * method might return an empty set.
     *
     * @param applicationId the application ID that
     *        demands this rule
     * @param deviceId the device where the rule
     *        will be installed
     * @param tcId the traffic class ID where the rule
     *        belongs to
     * @param inputPort the input port where input
     *        packet arrived
     * @param queueIndex the NIC queue where input
     *        packet will be sent
     * @param outputPort the port of the device where
     *        the rule will be sent out
     * @param rxFilter the Rx filter mechanism
     *        (i.e., mac, vlan, mpls, etc.)
     *        used by this traffic class
     * @param rxFilterValue the actual value of the
     *        above filter
     * @param tagging indicates that tagging needs
     *        to be associated with the generated rules;
     *        tagging information is stored above
     * @return set of ONOS FlowRule objects
     * @throws DeploymentException if the translation
     *         into OpenFlow rules fails
     */
    Set<FlowRule> toOpenFlowRules(
        ApplicationId applicationId,
        DeviceId      deviceId,
        URI           tcId,
        long          inputPort,
        long          queueIndex,
        long          outputPort,
        RxFilter      rxFilter,
        RxFilterValue rxFilterValue,
        boolean       tagging
    ) throws DeploymentException;

}
