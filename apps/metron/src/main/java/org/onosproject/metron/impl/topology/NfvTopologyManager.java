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

package org.onosproject.metron.impl.topology;

// Metron libraries
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.server.ServerService;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainScope;
import org.onosproject.metron.api.topology.NfvTopologyService;

import org.onosproject.metron.api.structures.Pair;

// ONOS libraries
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.Path;
import org.onosproject.net.topology.HopCountLinkWeigher;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.protocol.rest.RestSBDevice;

// Apache libraries
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Other libraries
import org.slf4j.Logger;

// Guava libraries
import com.google.common.collect.Sets;

// Java libraries
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collection;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of Metron's Topology Manager component.
 */
@Component(immediate = true)
@Service
public class NfvTopologyManager implements NfvTopologyService {

    private static final Logger log = getLogger(NfvTopologyManager.class);

    /**
     * Application ID for the Topology Manager.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".topology.manager";
    private static final String COMPONET_LABEL = "Topology Manager";

    /**
     * Key device identifiers.
     */
    private static final String SERVER_LABEL_PREFIX = "rest:";
    private static final String OPENFLOW_SWITCH_LABEL_PREFIX = "of:";

    /**
     * Link weight based on the hop count.
     */
    private static final LinkWeigher HOP_COUNT_LINK_WEIGHER = new HopCountLinkWeigher();

    /**
     * An application ID is necessary to register with the core.
     */
    private ApplicationId appId = null;

    /**
     * The topology graph fetched by the ONOS TopologyService.
     */
    private TopologyGraph topologyGraph = null;

    /**
     * The topology clusters fetched by the ONOS TopologyService.
     */
    private Set<TopologyCluster> topologyClusters = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ServerService serverService;

    public NfvTopologyManager() {
    }

    @Activate
    public void activate() {
        this.appId = coreService.registerApplication(APP_NAME);
        log.info("[{}] Started", label());
    }

    @Deactivate
    public void deactivate() {
        log.info("[{}] Stopped", label());
    }

    @Override
    public TopologyGraph topologyGraph() {
        if (topologyService == null) {
            return null;
        }

        // Fetch a fresh view of the topology
        TopologyGraph newTopologyGraph = topologyService.getGraph(topologyService.currentTopology());

        // First occurence
        if (this.topologyGraph == null) {
            this.topologyGraph = newTopologyGraph;
        }

        // If the topology has changed, update our object
        if ((newTopologyGraph.getVertexes().size() > 0) && !newTopologyGraph.equals(this.topologyGraph)) {
            this.topologyGraph = newTopologyGraph;
        }

        return this.topologyGraph;
    }

    @Override
    public Set<TopologyCluster> topologyClusters() {
        if (topologyService == null) {
            return null;
        }

        this.topologyClusters = topologyService.getClusters(
            topologyService.currentTopology()
        );

        return this.topologyClusters;
    }

    @Override
    public boolean exists() {
        if (this.topologyGraph() == null) {
            return false;
        }

        return this.topologyGraph.getVertexes().size() > 0;
    }

    @Override
    public int devicesNumber() {
        return this.topologyGraph.getVertexes().size();
    }

    @Override
    public int linksNumber() {
        return this.topologyGraph.getEdges().size();
    }

    @Override
    public boolean deviceExists(DeviceId deviceId) {
        return serverService.deviceExists(deviceId);
    }

    @Override
    public RestSBDevice getDevice(DeviceId deviceId) {
        return serverService.getDevice(deviceId);
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        return linkService.getDeviceIngressLinks(deviceId);
    }

    @Override
    public Set<Link> getDeviceIngressLinksWithPort(DeviceId deviceId, long portId) {
        Set<Link> links = Sets.<Link>newConcurrentHashSet();
        for (Link link : linkService.getDeviceIngressLinks(deviceId)) {
            DeviceId dev = link.src().deviceId();
            long port = link.src().port().toLong();
            // if (dev.equals(deviceId) && (port == portId)) {
            if (port == portId) {
                links.add(link);
            }
        }

        return links;
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        return linkService.getDeviceEgressLinks(deviceId);
    }

    @Override
    public Set<Link> getDeviceEgressLinksWithPort(DeviceId deviceId, long portId) {
        Set<Link> links = Sets.<Link>newConcurrentHashSet();
        for (Link link : linkService.getDeviceEgressLinks(deviceId)) {
            DeviceId dev = link.src().deviceId();
            long port = link.dst().port().toLong();
            // if (dev.equals(deviceId) && (port == portId)) {
            if (port == portId) {
                links.add(link);
            }
        }

        return links;
    }

    @Override
    public boolean linkHasDevice(Link link, DeviceId deviceId) {
        checkNotNull(link, "Cannot check NULL link");
        checkNotNull(deviceId, "Cannot check NULL device");

        if (link.src().deviceId().equals(deviceId) ||
            link.dst().deviceId().equals(deviceId)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean linkHasPort(Link link, long port) {
        checkNotNull(link, "Cannot check NULL link");
        checkArgument(port > 0, "Cannot check negative port");

        if ((link.src().port().toLong() == port) ||
            (link.dst().port().toLong() == port)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean hasSwitches() {
        for (TopologyVertex topoVertex : this.topologyGraph().getVertexes()) {
            DeviceId devId = topoVertex.deviceId();

            // The device ID has a characteristic OpenFlow mark
            if (devId.toString().contains(OPENFLOW_SWITCH_LABEL_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasServers() {
        for (TopologyVertex topoVertex : this.topologyGraph().getVertexes()) {
            DeviceId devId = topoVertex.deviceId();

            // Whatever is not OpenFlow, it is an NFV server
            if (!devId.toString().contains(OPENFLOW_SWITCH_LABEL_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<Path> getKShortestPathsBasedOnHopCount(DeviceId src, DeviceId dst, int maxPaths) {
        checkNotNull(src,
            "[" + label() + "] Unable to compute shortest paths; Source network element is NULL");

        checkNotNull(dst,
            "[" + label() + "] Unable to compute shortest paths; Destination network element is NULL");

        checkArgument(maxPaths > 0,
            "[" + label() + "] Number of shortest paths requested is non-positive: " + maxPaths);

        return topologyService.getKShortestPaths(
            topologyService.currentTopology(), src, dst, HOP_COUNT_LINK_WEIGHER, maxPaths);
    }

    @Override
    public Map<DeviceId, RestSBDevice> getServers() {
        Map<DeviceId, RestSBDevice> deviceMap = new ConcurrentHashMap<DeviceId, RestSBDevice>();

        for (TopologyVertex topoVertex : this.topologyGraph().getVertexes()) {
            DeviceId deviceId = topoVertex.deviceId();

            // Already here, skip
            if (deviceMap.containsKey(deviceId)) {
                continue;
            }

            // Non-NFV devices are discovered by ONOS
            if (!this.isServer(deviceId)) {
                continue;
            }

            // Here we discover NFV servers
            RestServerSBDevice device = (RestServerSBDevice) serverService.getDevice(deviceId);
            if (device != null) {
                deviceMap.put(deviceId, device);
            }
        }

        return deviceMap;
    }

    @Override
    public RestSBDevice getLeastOverloadedServerPowerOfTwoChoices(int numberOfCores, int numberOfNics) {
        checkArgument(numberOfCores > 0,
            "[" + label() + "] Invalid request for the least overloaded server. " +
            "The requested number of cores is invalid: " + numberOfCores);

        checkArgument(numberOfNics > 0,
            "[" + label() + "] Invalid request for the least overloaded server. " +
            "The requested number of NICs is invalid: " + numberOfNics);

        Map<DeviceId, RestSBDevice> deviceMap = this.getServers();

        // No devices available
        if (deviceMap.size() == 0) {
            return null;
        }

        /**
         * To reduce the number of queries to different servers,
         * pick a random key from the device map.
         */
        List<DeviceId> origDeviceIds = new ArrayList<DeviceId>(deviceMap.keySet());
        List<DeviceId> currDeviceIds = new ArrayList<DeviceId>(deviceMap.keySet());

        // Here we will shortly store the target server.
        RestServerSBDevice selectedDev = null;
        // The ID of the target device
        DeviceId selectedDevId  = null;

        // As long as there are servers.
        while (this.hasServersLeft(currDeviceIds)) {
            // Pick two random servers
            Pair<Integer, Integer> twoChoices = this.twoRandomChoices(currDeviceIds);

            int firstChoice  = twoChoices.getKey().intValue();
            int secondChoice = twoChoices.getValue().intValue();

            // ..and the devices that correponds to these servers
            DeviceId firstDevId  = origDeviceIds.get(firstChoice);
            DeviceId secondDevId = origDeviceIds.get(secondChoice);

            // Now ask for monitoring data
            MonitoringStatistics firstStats  = null;
            MonitoringStatistics secondStats = null;
            Pair<DeviceId, MonitoringStatistics> bestChoice = null;

            // Ask for monitoring data from the first device
            firstStats = serverService.getGlobalMonitoringStats(firstDevId);

            // There is only one device, so this is the best choice
            if (firstChoice == secondChoice) {
                bestChoice = new Pair<DeviceId, MonitoringStatistics>(firstDevId, firstStats);

            } else {
                // Get monitoring data from the second device too
                secondStats = serverService.getGlobalMonitoringStats(secondDevId);
                // Find which of the two devices has the largest amount fo capacity
                bestChoice = this.bestServerChoice(
                    firstDevId, firstStats, secondDevId, secondStats
                );
            }

            DeviceId bestDevId = bestChoice.getKey();
            RestServerSBDevice bestDev = (RestServerSBDevice) deviceMap.get(bestDevId);
            MonitoringStatistics bestStats = bestChoice.getValue();

            // Fetch the CPU cores' stata of this device
            int busyCores = 0;
            Collection<CpuStatistics> cpuStats = bestStats.cpuStatisticsAll();
            for (CpuStatistics cs : cpuStats) {
                if (cs.busy()) {
                    busyCores++;
                }
            }

            int freeCores = cpuStats.size() - busyCores;
            int totalCores = freeCores + busyCores;

            // This server has sufficient resources. let's go!
            if ((freeCores > 0) && (freeCores >= numberOfCores)) {
                selectedDev = bestDev;
                selectedDevId = bestDevId;

                log.info(
                    "[{}] \t Server {} found with {}/{} free CPU cores",
                    label(), selectedDevId, freeCores, totalCores
                );

                break;
            // Not enough resources
            } else {
                // Nullify these indices to be avoided in the future
                currDeviceIds.set(currDeviceIds.indexOf(firstDevId),  null);

                if (firstChoice == secondChoice) {
                    log.info("[{}] \t Bad random choice of server {}", label(), firstDevId);
                } else {
                    log.info("[{}] \t Bad random choice of servers {} and {}",
                        label(), firstDevId, secondDevId);

                    // The second index must be also nullified
                    currDeviceIds.set(currDeviceIds.indexOf(secondDevId), null);
                }

                continue;
            }
        }

        if (selectedDev == null) {
            log.error("[{}] No server is available to provide {} CPU cores", label(), numberOfCores);
            return null;
        }

        return selectedDev;
    }

    /**
     * Checks whether a list of devices still contains non-NULL devices.
     *
     * @param deviceList the list of devices to check
     * @return boolean device availability
     */
    private boolean hasServersLeft(List<DeviceId> deviceList) {
        for (DeviceId devId : deviceList) {
            if (devId != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Make two random server choices out of a set of available servers.
     *
     * @param deviceIds the list of servers available
     * @return a pair of indices that indicate the two choices
     */
    private Pair<Integer, Integer> twoRandomChoices(List<DeviceId> deviceIds) {
        Random random = new Random();

        // Desired range is [0, servers-1]
        int min = 0;
        int max = deviceIds.size() - 1;

        int firstChoice = -1;
        DeviceId firstDev = null;
        while (firstDev == null) {
            firstChoice = min + random.nextInt((max - min) + 1);
            firstDev = deviceIds.get(firstChoice);
        }

        checkArgument((firstChoice >= min) && (firstChoice <= max), "Wrong server index is randomly chosen");

        // There is only one device available
        if (max == 0) {
            return new Pair<Integer, Integer>(new Integer(firstChoice), new Integer(firstChoice));
        }

        int tries = 0;
        int secondChoice = firstChoice;
        DeviceId secondDev = null;

        while (((secondChoice == firstChoice) || (secondDev == null)) && (tries < 5 * max)) {
            secondChoice = min + random.nextInt((max - min) + 1);
            secondDev = deviceIds.get(secondChoice);
            tries++;
        }

        checkArgument((secondChoice >= min) && (secondChoice <= max), "Wrong server index is randomly chosen");

        return new Pair<Integer, Integer>(new Integer(firstChoice), new Integer(secondChoice));
    }

    /**
     * Returns the device with the most available capacity out of the two input devices.
     *
     * @param firstDevId the ID of the first device
     * @param firstStats monitoring statistics from the first device
     * @param secondDevId the ID of the second device
     * @param secondStats monitoring statistics from the second device
     * @return pair of device ID with statistics for the device with the largest amount of capacity
     */
    private Pair<DeviceId, MonitoringStatistics> bestServerChoice(
            DeviceId firstDevId,  MonitoringStatistics firstStats,
            DeviceId secondDevId, MonitoringStatistics secondStats) {
        int firstFreeCores = -1;
        if (firstStats != null) {
            Collection<CpuStatistics> firstCpuStats = firstStats.cpuStatisticsAll();
            for (CpuStatistics cs : firstCpuStats) {
                if (!cs.busy()) {
                    firstFreeCores++;
                }
            }
        }

        int secondFreeCores = -1;
        if (secondStats != null) {
            Collection<CpuStatistics> secondCpuStats = secondStats.cpuStatisticsAll();
            for (CpuStatistics cs : secondCpuStats) {
                if (!cs.busy()) {
                    secondFreeCores++;
                }
            }
        }

        if (firstFreeCores >= secondFreeCores) {
            return new Pair<DeviceId, MonitoringStatistics>(firstDevId, firstStats);
        }

        return new Pair<DeviceId, MonitoringStatistics>(secondDevId, secondStats);
    }

    @Override
    public boolean isSdnDevice(DeviceId deviceId) {
        if (deviceId.toString().startsWith(OPENFLOW_SWITCH_LABEL_PREFIX)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isServer(DeviceId deviceId) {
        if (deviceId.toString().startsWith(SERVER_LABEL_PREFIX)) {
            return true;
        }

        return false;
    }

    /***************************** Relayed Services to ServerManager. **************************/

    @Override
    public TrafficClassRuntimeInfo deployTrafficClassOfServiceChain(
            DeviceId          deviceId,
            ServiceChainId    scId,
            URI               tcId,
            ServiceChainScope scScope,
            String            configurationType,
            String            configuration,
            int               numberOfCores,
            int               maxNumberOfCores,
            Set<String>       nicIds,
            boolean           autoscale) {
        return serverService.deployTrafficClassOfServiceChain(
            deviceId, scId, tcId, scScope,
            configurationType, configuration,
            numberOfCores, maxNumberOfCores,
            nicIds, autoscale
        );
    }

    @Override
    public boolean reconfigureTrafficClassOfServiceChain(
            DeviceId       deviceId,
            ServiceChainId scId,
            URI            tcId,
            String         configurationType,
            String         configuration,
            int            numberOfCores,
            int            maxNumberOfCores) {
        return serverService.reconfigureTrafficClassOfServiceChain(
            deviceId, scId, tcId,
            configurationType, configuration,
            numberOfCores, maxNumberOfCores
        );
    }

    @Override
    public TrafficClassRuntimeInfo updateTrafficClassRuntimeInfo(
            DeviceId                deviceId,
            ServiceChainId          scId,
            URI                     tcId,
            TrafficClassRuntimeInfo tcInfo) {
        return serverService.updateTrafficClassRuntimeInfo(
            deviceId, scId, tcId, tcInfo
        );
    }

    @Override
    public Set<MonitoringStatistics> getServiceChainMonitoringStats(
            DeviceId deviceId, ServiceChainId scId, Set<URI> tcIds) {
        return serverService.getServiceChainMonitoringStats(deviceId, scId, tcIds);
    }

    @Override
    public MonitoringStatistics getTrafficClassMonitoringStats(
            DeviceId deviceId, ServiceChainId scId, URI tcId) {
        return serverService.getTrafficClassMonitoringStats(deviceId, scId, tcId);
    }

    @Override
    public MonitoringStatistics getGlobalMonitoringStats(DeviceId deviceId) {
        return serverService.getGlobalMonitoringStats(deviceId);
    }

    @Override
    public boolean deleteServiceChain(DeviceId deviceId, ServiceChainId scId, Set<URI> tcIds) {
        return serverService.deleteServiceChain(deviceId, scId, tcIds);
    }

    @Override
    public boolean deleteTrafficClassOfServiceChain(
            DeviceId deviceId, ServiceChainId scId, URI tcId) {
        return serverService.deleteTrafficClassOfServiceChain(deviceId, scId, tcId);
    }

    @Override
    public TrafficClassRuntimeInfo buildRuntimeInformation(
            DeviceId       deviceId,
            ServiceChainId scId,
            URI            tcId,
            String         primaryNic,
            String         configurationType,
            String         configuration,
            int            numberOfCores,
            int            maxNumberOfCores,
            Set<String>    nicIds,
            String         rxFilterMethodStr) {
        return serverService.buildRuntimeInformation(
            deviceId, scId, tcId, primaryNic,
            configurationType, configuration,
            numberOfCores, maxNumberOfCores,
            nicIds, rxFilterMethodStr
        );
    }

    /************************ End of Relayed Services to ServerManager. ************************/

    /**
     * Returns a label with the Topology Manager's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}
