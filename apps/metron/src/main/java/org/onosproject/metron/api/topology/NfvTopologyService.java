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

package org.onosproject.metron.api.topology;

import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainScope;

import org.onosproject.drivers.server.stats.MonitoringStatistics;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.protocol.rest.RestSBDevice;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Metorn's topology service; extends ONOS topology service.
 */
public interface NfvTopologyService {

    /**
     * Returns the current topology graph.
     *
     * @return topology graph
     */
    TopologyGraph topologyGraph();

    /**
     * Returns the current set of clusters of the topology.
     *
     * @return set of topology clusters
     */
    Set<TopologyCluster> topologyClusters();

    /**
     * Checks whether the underlying topology exists or not.
     *
     * @return boolean topology status
     */
    boolean exists();

    /**
     * Returns the number of devices in the topology.
     *
     * @return integer number of devices in the topology
     */
    public int devicesNumber();

    /**
     * Returns the number of links in the topology.
     *
     * @return integer number of links in the topology
     */
    public int linksNumber();

    /**
     * Returns whether a device ID belongs to
     * an active server or not.
     *
     * @param deviceId device to check
     * @return boolean existence of device
     */
    boolean deviceExists(DeviceId deviceId);

    /**
     * Returns a device with a given ID.
     *
     * @param deviceId the device ID to be retrieved
     * @return a RestSBDevice device or null
     */
    RestSBDevice getDevice(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading to the specified device.
     *
     * @param deviceId device identifier
     * @return set of device ingress links
     */
    Set<Link> getDeviceIngressLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading to the specified device.
     *
     * @param deviceId device identifier
     * @param portId device port identifier
     * @return set of device ingress links
     */
    Set<Link> getDeviceIngressLinksWithPort(DeviceId deviceId, long portId);

    /**
     * Returns set of all infrastructure links from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device egress links
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links from the specified device.
     *
     * @param deviceId device identifier
     * @param portId device port identifier
     * @return set of device egress links
     */
    Set<Link> getDeviceEgressLinksWithPort(DeviceId deviceId, long portId);

    /**
     * Returns whether a certain device appears in a link.
     *
     * @param link link to be checked
     * @param deviceId device ID
     * @return boolean existence of device in link
     */
    boolean linkHasDevice(Link link, DeviceId deviceId);

    /**
     * Returns whether a certain port number appears in a link.
     *
     * @param link link to be checked
     * @param port port number
     * @return boolean existence of port number in link
     */
    boolean linkHasPort(Link link, long port);

    /**
     * Returns whether the topology contains any switches or not.
     * In particular, this method discovers OpenFlow switches.
     *
     * @return boolean existence of switches in the topology
     */
    boolean hasSwitches();

    /**
     * Returns whether the topology contains any servers or not.
     *
     * @return boolean existence of servers in the topology
     */
    boolean hasServers();

    /**
     * Returns a set of `k` shortest paths between a given
     * source and destination pair of devices.
     *
     * @param src the device ID of the source
     * @param dst the device ID of the destination
     * @param maxPaths the maximum number of paths to be computed
     * @return set of shortest paths
     */
    Set<Path> getKShortestPathsBasedOnHopCount(
        DeviceId src, DeviceId dst, int maxPaths
    );

    /**
     * Returns a map between the input server device IDs and their objects.
     *
     * @return map with server devices
     */
    Map<DeviceId, RestSBDevice> getServers();

    /**
     * Returns the least overloaded device out of two random choices.
     *
     * @param numberOfCores declare the core requirements
     * @param numberOfNics declare the NIC requirements
     * @return NFV device that exhibits the lowest load
     *         and meets the requirements
     */
    RestSBDevice getLeastOverloadedServerPowerOfTwoChoices(
        int numberOfCores, int numberOfNics
    );

    /**
     * Checks whether a device is SDN-based or not.
     * This is done by checking the first letters of its name.
     * OpenFlow devices start with a certain prefix.
     *
     * @param deviceId the ID of the device to be checked
     * @return boolean identity (SDN or not)
     */
    boolean isSdnDevice(DeviceId deviceId);

    /**
     * Checks whether a device is a server or not.
     * This is done by checking the first letters of its name.
     * Servers start with a certain prefix which is the prefix
     * that characterizes REST devices.
     *
     * @param deviceId the ID of the device to be checked
     * @return boolean identity (NFV or not)
     */
    boolean isServer(DeviceId deviceId);

    /***************************** Relayed Services to ServerManager. **************************/

    /**
     * Instructs the device to deploy a traffic class of a service chain.
     *
     * @param deviceId the server where the deployment takes place
     * @param scId the ID of the service chain
     * @param tcId the ID of the service chain's traffic class
     * @param scScope the scope of the service chain
     * @param configurationType the type of the target packet processing element
     *        Can be Click-based (click) or a blackbox NF (standalone)
     * @param configuration the packet processing instructions as a string
     * @param newCpuSet the set of CPU cores to be used for this traffic class
     * @param maxNumberOfCores estimation of the maximum the number of CPUs you might need
     * @param nicIds the IDs of the NICs that participate in the processing
     * @param autoscale allows the agent to handle load imbalances autonomously
     * @return TrafficClassRuntimeInfo a description of the runtime information
     *         of this traffic class
     */
    TrafficClassRuntimeInfo deployTrafficClassOfServiceChain(
        DeviceId          deviceId,
        ServiceChainId    scId,
        URI               tcId,
        ServiceChainScope scScope,
        String            configurationType,
        String            configuration,
        Set<Integer>      newCpuSet,
        int               maxNumberOfCores,
        Set<String>       nicIds,
        boolean           autoscale
    );

    /**
     * Instructs the device to reconfigure a traffic class of a service chain.
     *
     * @param deviceId the server where the reconfiguration takes place
     * @param scId the ID of the service chain
     * @param tcId the ID of the service chain's traffic class
     * @param configurationType the type of the target packet processing element
     *        Can be Click-based (click) or a blackbox NF (standalone)
     * @param configuration the packet processing instructions as a string
     * @param newCpuSet the new set of CPU cores to be used for this traffic class
     * @param maxNumberOfCores the new maximum number of CPUs you need
     * @return boolean reconfiguration status
     */
    boolean reconfigureTrafficClassOfServiceChain(
        DeviceId       deviceId,
        ServiceChainId scId,
        URI            tcId,
        String         configurationType,
        String         configuration,
        Set<Integer>   newCpuSet,
        int            maxNumberOfCores
    );

    /**
     * Updates the runtime information related to a traffic class.
     *
     * @param deviceId the server to be queried
     * @param scId     the ID of the service chain to be queried
     * @param tcId     the ID of the service chain's traffic class
     * @param tcInfo   the runtime information object to be updated
     * @return updated traffic class runtime information
     */
    TrafficClassRuntimeInfo updateTrafficClassRuntimeInfo(
        DeviceId                deviceId,
        ServiceChainId          scId,
        URI                     tcId,
        TrafficClassRuntimeInfo tcInfo
    );

    /**
     * Asks for service chain-specific monitoring data
     * from an NFV device.
     *
     * @param deviceId the server to be monitored
     * @param scId     the ID of the service chain to be monitored
     * @param tcIds    the set of traffic class IDs of the service chain
     * @return set of monitoring statistics per traffic class of the service chain
     */
    Set<MonitoringStatistics> getServiceChainMonitoringStats(
        DeviceId deviceId, ServiceChainId scId, Set<URI> tcIds
    );

    /**
     * Asks for service chain-specific monitoring data
     * from an NFV device regarding a single traffic class.
     *
     * @param deviceId the server to be monitored
     * @param scId     the ID of the service chain to be monitored
     * @param tcId     the ID of the traffic class to be monitored
     * @return monitoring statistics of a traffic class of the service chain
     */
    MonitoringStatistics getTrafficClassMonitoringStats(
        DeviceId deviceId, ServiceChainId scId, URI tcId
    );

    /**
     * Asks for global monitoring data from an NFV device.
     *
     * @param deviceId the server to be monitored
     * @return monitoring statistics
     */
    MonitoringStatistics getGlobalMonitoringStats(DeviceId deviceId);

    /**
     * Deletes an entire service chain from an NFV device.
     *
     * @param deviceId the server that hosts the service chain
     * @param scId     the ID of the service chain to be deleted
     * @param tcIds    the set of traffic class IDs of the service chain
     * @return boolean deletion status
     */
    boolean deleteServiceChain(
        DeviceId deviceId, ServiceChainId scId, Set<URI> tcIds
    );

    /**
     * Deletes a service chain from an NFV device.
     *
     * @param deviceId the server that hosts the service chain
     * @param scId     the ID of the service chain to be deleted
     * @param tcId     the ID of the traffic class to be deleted
     * @return boolean deletion status
     */
    boolean deleteTrafficClassOfServiceChain(
        DeviceId deviceId, ServiceChainId scId, URI tcId
    );

    /**
     * Builds a runtime information object for a just deployed service chain.
     *
     * @param deviceId the server where the deployment takes place
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class of this service chain
     * @param primaryNic the ID of traffic class's primary NIC
     * @param configurationType the type of the target packet processing element
     *        Can be Click-based (click) or a blackbox NF (standalone)
     * @param configuration the packet processing instructions as a string
     * @param newCpuSet the set of CPU cores to be used for this traffic class
     * @param maxNumberOfCores estimation of the maximum the number of CPUs you might need
     * @param nicIds the IDs of the NICs that participate in the processing
     * @param rxFilterMethodStr tagging method supported by the NIC of this service chain
     * @return TrafficClassRuntimeInfo a description of the runtime information
     *         of this service chain
     */
    TrafficClassRuntimeInfo buildRuntimeInformation(
            DeviceId       deviceId,
            ServiceChainId scId,
            URI            tcId,
            String         primaryNic,
            String         configurationType,
            String         configuration,
            Set<Integer>   newCpuSet,
            int            maxNumberOfCores,
            Set<String>    nicIds,
            String         rxFilterMethodStr
    );

}
