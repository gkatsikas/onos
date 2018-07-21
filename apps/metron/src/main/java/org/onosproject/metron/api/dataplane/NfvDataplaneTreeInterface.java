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

package org.onosproject.metron.api.dataplane;

import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.networkfunction.NetworkFunctionType;
import org.onosproject.metron.api.servicechain.ServiceChainId;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Interface that represents an NFV dataplane tree.
 */
public interface NfvDataplaneTreeInterface {

    /**
     * Returns the root node of this NFV dataplane tree.
     *
     * @return NfvDataplaneNodeInterface root node
     */
    NfvDataplaneNodeInterface root();

    /**
     * Returns the type of this NFV dataplane tree.
     *
     * @return NetworkFunctionType NF dataplane tree type
     */
    NetworkFunctionType type();

    /**
     * Sets the root node of this NFV dataplane tree.
     *
     * @param root NfvDataplaneNodeInterface root node
     */
    void setRoot(NfvDataplaneNodeInterface root);

    /**
     * Create an entity for managing the paths of this tree.
     *
     * @param fwdPath the forward path towards this tree
     * @param bwdPath the backward path from this tree
     * @param ingressPort the ingress port towards this tree
     * @param egressPort the egress port after leaving this tree
     * @param withServer indicates whether this path leads to a server
     * @return a path establisher
     */
    PathEstablisherInterface createPathEstablisher(
        Path fwdPath, Path bwdPath,
        long ingressPort, long egressPort,
        boolean withServer
    );

    /**
     * Create an entity for managing the paths of this tree.
     *
     * @param point1 the first point of the path
     * @param point2 the second point of the path
     * @param ingressPort the ingress port towards this tree
     * @param egressPort the egress port after leaving this tree
     * @param withServer indicates whether this path leads to a server
     * @return a path establisher
     */
    PathEstablisherInterface createPathEstablisher(
        ConnectPoint point1, ConnectPoint point2,
        long ingressPort, long egressPort,
        boolean withServer
    );

    /**
     * Returns the entity for managing the paths of this tree.
     *
     * @return a path establisher
     */
    PathEstablisherInterface pathEstablisher();

    /**
     * Returns the name of the input network interface of this tree.
     *
     * @return name of the input network interface
     */
    String inputInterfaceName();

    /**
     * Sets the name of the input network interface of this tree.
     *
     * @param inputInterfaceName name of the input network interface
     */
    void setInputInterfaceName(String inputInterfaceName);

    /**
     * Returns the map of traffic classes
     * associated with logical core numbers.
     *
     * @return set of traffic classes associated with logicla cores
     */
    Map<Integer, TrafficClassInterface> trafficClasses();

    /**
     * Returns the traffic class associated with a logical core.
     *
     * @param core the logical core associated with the traffic class
     * @return set of traffic classes associated with this tree
     */
    TrafficClassInterface trafficClassOnCore(int core);

    /**
     * Adds a new traffic class to the map of traffic classes of this tree.
     *
     * @param core         the logical core associated with the traffic class
     * @param trafficClass the traffic class to be added
     */
    void addTrafficClassOnCore(int core, TrafficClassInterface trafficClass);

    /**
     * Groups together traffic classes with the same write operations.
     * A virtual traffic class ID is assigned per group.
     *
     * @return map of grouped traffic classes to group IDs
     */
    Map<URI, Set<TrafficClassInterface>> groupedTrafficClasses();

    /**
     * Returns the set of traffic classes that belong to a certain group.
     *
     * @param groupId the ID of this group of traffic classes
     * @return set of grouped traffic classes with specific group ID
     */
    Set<TrafficClassInterface> getGroupedTrafficClassesWithID(URI groupId);

    /**
     * Returns the mapping between the IDs of grouped traffic classes and
     * their logical CPU cores.
     *
     * @return map of grouped traffic class IDs to logical cores
     */
    Map<URI, Integer> groupTrafficClassToCoreMap();

    /**
     * Associates a group of traffic classes with a logical CPU core.
     *
     * @param groupId the ID of this group of traffic classes
     * @param core the logical core
     */
    void pinGroupTrafficClassToCore(URI groupId, int core);

    /**
     * Returns the ID of the group of traffic classes
     * associated with the given logical core.
     *
     * @param core the logical core associated with the traffic class
     * @return set of traffic classes associated with this logical core
     */
    URI groupTrafficClassIdOnCore(int core);

    /**
     * Checks whether the given traffic class ID is part of this tree.
     *
     * @param tcId the traffic class ID to be checked
     * @return boolean exists or not
     */
    boolean hasTrafficClass(URI tcId);

    /**
     * Returns the idle interfaces' configuration
     * of this traffic class.
     *
     * @return idle interface configuration as a string
     */
    String idleInterfaceConfiguration();

    /**
     * Returns the number of NICs required by this traffic class.
     *
     * @return number of NICs required by this traffic class
     */
    int numberOfNics();

    /**
     * Sets the number of NICs required by this traffic class.
     *
     * @param nics number of NICs required by this traffic class
     */
    void setNumberOfNics(int nics);

    /**
     * Returns whether this NFV dataplane tree can be
     * completely offloaded to the network.
     *
     * @return boolean total offloadability
     */
    boolean isTotallyOffloadable();

    /**
     * Returns whether this NFV dataplane tree can be
     * partially offloaded to the network.
     *
     * @return boolean partial offloadability
     */
    boolean isPartiallyOffloadable();

    /**
     * Returns a map of the software configuration
     * of this traffic class per CPU core.
     *
     * @param server device to get configuration from
     * @param withHwOffloading if true the generated software
     *        configuration contains only part of the entire
     *        service chain
     * @return software configuration map of this traffic class
     */
    Map<Integer, String> softwareConfiguration(
        RestServerSBDevice server, boolean withHwOffloading
    );

    /**
     * Returns the software configuration of this traffic class
     * on a particular CPU core.
     *
     * @param core the CPU core that executes this configuration
     * @return software configuration of this traffic class on a core
     */
    String softwareConfigurationOnCore(int core);

    /**
     * Sets the software configuration of this traffic class
     * on a particular CPU core.
     *
     * @param core the CPU core that executes this configuration
     * @param conf the configuration we want to set
     */
    void setSoftwareConfigurationOnCore(int core, String conf);

    /**
     * Returns a set of hardware rules that correspond to the
     * service chain's configuration.
     * Requires generateHardwareConfiguration to be executed first.
     *
     * @return map of traffic class IDs to their hardware rules
     */
    Map<URI, Set<FlowRule>> hardwareConfiguration();

    /**
     * Returns a set of hardware rules that correspond to the
     * service chain's configuration as a set.
     * Requires generateHardwareConfiguration to be executed first.
     *
     * @return set of hardware rules for this service chain
     */
    Set<FlowRule> hardwareConfigurationToSet();

    /**
     * Returns a set of hardware rules that correspond to the
     * traffic class's configuration.
     * Requires generateHardwareConfiguration to be executed first.
     *
     * @param tcId the ID of this traffic class
     * @return set of hardware rules for this traffic class
     */
    Set<FlowRule> hardwareConfigurationOfTrafficClass(URI tcId);

    /**
     * Sets the hardware configuration of this traffic class.
     *
     * @param tcId the ID of this traffic class
     * @param rules set of hardware rules for this traffic class
     */
    void setHardwareConfigurationOfTrafficClass(URI tcId, Set<FlowRule> rules);

    /**
     * Converts the entire service chain into a binary classification tree
     * and compresses its service chain-level traffic classes.
     */
    void buildAndCompressBinaryTree();

    /**
     * Generates the configuration of each traffic class
     * as if it has to be executed entirely or partially
     * in software.
     *
     * @param server device to configure
     * @param withHwOffloading if true the generated software
     *        configuration contains only part of the entire
     *        service chain
     * @throws DeploymentException if the generation fails
     */
    void generateSoftwareConfiguration(
        RestServerSBDevice server, boolean withHwOffloading
    ) throws DeploymentException;

    /**
     * Generates the configuration of each traffic class
     * as if it can be (partially) offloaded in hardware.
     *
     * @param scId the service chain ID that demands
     *        this hardware configuration
     * @param appId the application ID that demands
     *        this hardware configuration
     * @param deviceId the device where the hardware
     *        configuration will be installed
     * @param inputPort the input port where hardware
     *        configuration is applied
     * @param queuesNumber the number of input queues to
     *        spread the traffic across
     * @param outputPort the port of the device where the
     *        hardware configuration will be sent out
     * @param tagging indicates that tagging needs to be associated
     *        with the generated rules
     * @param tcCompDelay map with latencies to compute each
     *        traffic class's rules
     * @return map of traffic classes to tags
     * @throws DeploymentException if the generation fails
     */
    Map<URI, RxFilterValue> generateHardwareConfiguration(
        ServiceChainId  scId,
        ApplicationId   appId,
        DeviceId        deviceId,
        long            inputPort,
        long            queuesNumber,
        long            outputPort,
        boolean         tagging,
        Map<URI, Float> tcCompDelay
    ) throws DeploymentException;

    /**
     * Returns the tag service of this dataplane tree.
     *
     * @return object that manages the tags for this dataplane tree
     */
    TagService tagService();

}
