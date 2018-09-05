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

package org.onosproject.metron.impl.orchestrator;

// Metron libraries
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.config.TrafficPoint;
import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.dataplane.PathEstablisherInterface;
import org.onosproject.metron.api.dataplane.TagService;
import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.monitor.WallClockNanoTimestamp;
import org.onosproject.metron.api.orchestrator.DeploymentService;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainService;
import org.onosproject.metron.api.servicechain.ServiceChainListenerInterface;
import org.onosproject.metron.api.topology.NfvTopologyService;

// ONOS libraries
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyVertex;
import org.osgi.service.component.ComponentContext;

import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

// Apache libraries
import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Guava
import com.google.common.collect.Sets;

// Other libraries
import org.slf4j.Logger;

// Java libraries
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;

import static org.onosproject.metron.api.config.TrafficPoint.TrafficPointType.PROCESSING;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.util.Tools.groupedThreads;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * A service that undertakes to deploy Metron service chains.
 */
@Component(immediate = true)
@Service
public final class DeploymentManager implements DeploymentService {

    private static final Logger log = getLogger(DeploymentManager.class);

    /**
     * Application information for the Metron Deployer.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".deployer";
    private static final String COMPONET_LABEL = "Metron Deployer";

    /**
     * Constants.
     */
    // Default number of requested paths from a switch to a server
    private static final int  DEF_NUMBER_OF_SW_TO_SRV_PATHS = 8;
    // Indicates any port
    private static final long ANY_PORT = -1;

    /**
     * Members:
     * |-> An application ID is necessary to register with the core.
     * |-> A set of service chains (fetched by the Service Chain Manager) ready to be deployed.
     * |-> A set of already deployed service chains (active).
     * |-> A map between deployed service chains and their runtime information.
     * |-> A map between active servers and their rules.
     */
    private ApplicationId appId = null;
    private Set<ServiceChainInterface> serviceChainsToDeploy = null;
    private Set<ServiceChainInterface> deployedServiceChains = null;
    private Map<ServiceChainId, Map<URI, TrafficClassRuntimeInfo>> runtimeInfo = null;
    private Map<RestServerSBDevice, Set<FlowRule>> serverToNetworkRules = null;

    /**
     * Listen to events dispatched by the Service Chain Manager.
     * These events are related to the state of the service chains
     * that reside in the Service Chain Store.
     */
    private final ServiceChainListenerInterface serviceChainListener = new InternalServiceChainListener();

    /**
     * Component properties to be adjusted by the operator.
     * The operator can select whether the synthesizer will be
     * involved in the formation of the traffic classes or not.
     * By default the synthesizer is enabled, so every CONSTRUCTED
     * service chain will be translated into a highly optimized
     * equivalent, before it becomes READY.
     */
    private static final String ENABLE_HW_OFFLOADING = "enableHwOffloading";
    private static final boolean DEF_HW_OFFLOADING = true;
    @Property(name = ENABLE_HW_OFFLOADING, boolValue = DEF_HW_OFFLOADING,
             label = "Enable the hardware offloading features of Metron; default is true")
    private boolean enableHwOffloading = DEF_HW_OFFLOADING;

    /**
     * Variable that determines the scaling policy.
     * If true, the data plane undertakes scaling, otherwise
     * the controller performs scaling.
     */
    private static final String ENABLE_AUTOSCALE = "enableAutoScale";
    public  static final boolean DEF_AUTOSCALE = false;
    @Property(name = ENABLE_AUTOSCALE, boolValue = DEF_AUTOSCALE,
             label = "Allow the data plane to undertake scaling in case of load imbalances; default is false")
    private boolean enableAutoScale = DEF_AUTOSCALE;

    /**
     * A dedicated thread pool to deploy Metron service chains.
     */
    private static final int DEPLOYER_THREADS_NO = 1;
    private final ExecutorService deployerExecutor = newFixedThreadPool(DEPLOYER_THREADS_NO,
        groupedThreads(this.getClass().getSimpleName(), "sc-deployer", log));

    /**
     * The Metron Deployer requires:
     * 1) the ONOS core service to register,
     * 2) the ONOS configuration service to configure whether HW offloading will be active or not
     * 3) the Service Chain Manager to fetch the list of ready to deploy Metron service chains.
     *    (Such service chains must be in the READY state).
     * 4) the Topology Manager to identify the devices that will participate in the processing.
     * 5) the ONOS flow rule service to interact (install/remove rules) with the network elements.
     * 6) the Monitoring service to update the system's runtime information.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ServiceChainService serviceChainService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NfvTopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MonitorService monitoringService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TagService taggingService;

    public DeploymentManager() {
        this.serviceChainsToDeploy = Sets.<ServiceChainInterface>newConcurrentHashSet();
        this.deployedServiceChains = Sets.<ServiceChainInterface>newConcurrentHashSet();
        this.runtimeInfo = new ConcurrentHashMap<ServiceChainId, Map<URI, TrafficClassRuntimeInfo>>();
        this.serverToNetworkRules = new ConcurrentHashMap<RestServerSBDevice, Set<FlowRule>>();
    }

    @Activate
    protected void activate(ComponentContext context) {
        // Register the Metron Deployer with the core.
        this.appId = coreService.registerApplication(APP_NAME);

        // Register the component configuration
        cfgService.registerProperties(getClass());

        // Catch events coming from the service chain manager.
        serviceChainService.addListener(serviceChainListener);

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        // Unregister the component configuration
        cfgService.unregisterProperties(getClass(), false);

        // Remove the listener for the events coming from the service chain manager.
        serviceChainService.removeListener(serviceChainListener);

        // Take care of the thread pool
        this.deployerExecutor.shutdown();

        // Remove all the flow rules installed by the Metron Deployer
        flowRuleService.removeFlowRulesById(this.appId);

        // Tear down all the deployed service chains
        this.tearDownServiceChains();

        // Clean up your memory
        this.serviceChainsToDeploy.clear();
        this.deployedServiceChains.clear();
        this.runtimeInfo.clear();
        this.serverToNetworkRules.clear();

        log.info("[{}] Stopped", label());
    }

    @Modified
    public void modified(ComponentContext context) {
        this.readComponentConfiguration(context);
    }

    @Override
    public ApplicationId applicationId() {
        return this.appId;
    }

    @Override
    public void sendServiceChainToDeployer(ServiceChainInterface sc) {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be deployed");

        this.serviceChainsToDeploy.add(sc);
    }

    @Override
    public Set<ServiceChainInterface> readyToDeployServiceChains() {
        return this.serviceChainsToDeploy;
    }

    @Override
    public void deploy() {
        // No service chains, no party
        if (!this.hasServiceChainsToDeploy()) {
            log.debug("[{}] No service chains to deploy; cannot make deployment decisions", label());
            return;
        }

        log.info(Constants.STDOUT_BARS);
        log.info("Service Chain Deployer");
        log.info(Constants.STDOUT_BARS);

        // No topology, no party
        if (!this.topologyExists()) {
            log.warn("[{}] Topology unavailable; cannot make deployment decisions", label());
            log.warn("{}", Constants.STDOUT_BARS);
            return;
        }

        // Go through the service chains ready to be deployed
        Iterator<ServiceChainInterface> scIterator = this.serviceChainsToDeploy.iterator();
        while (scIterator.hasNext()) {
            ServiceChainInterface sc = scIterator.next();
            ServiceChainId scId = sc.id();
            ServiceChainState scState = sc.state();
            checkArgument(scState == ServiceChainState.READY,
                "Only service chains in READY state are handled by the Deployment Manager");

            boolean correctDeployment = true;

            // Get the runnable dataplane tree of this service chain
            Map<String, NfvDataplaneTreeInterface> dpTrees = serviceChainService.runnableServiceChains(scId);
            checkNotNull(dpTrees, "Runnable dataplane trees for service chain " + scId + " are NULL");

            for (Map.Entry<String, NfvDataplaneTreeInterface> entry : dpTrees.entrySet()) {
                String iface = entry.getKey();
                NfvDataplaneTreeInterface dpTree = entry.getValue();

                // Build the binary tree of this service chain and compress it
                dpTree.buildAndCompressBinaryTree();

                // Check the possibilty to offload (parts of) the service chain into the network
                boolean networkOffloadable = this.canDoNetworkOffloading(sc);
                // Can this be done without involving a server?
                boolean isTotallyOffloadable = dpTree.isTotallyOffloadable();

                /**
                 * *********************************************************************************
                 * ***************************  A. Complete Offloading  ****************************
                 * *********************************************************************************
                 * If network elements are available for offloading this service chain and
                 * the service chain can be completely offloaded, we should not involve any server.
                 * try to offload parts of the service chains that are ready to be deployed.
                 */
                if (networkOffloadable && isTotallyOffloadable) {
                    try {
                        if (!this.networkPlacer(sc, dpTree, null)) {
                            correctDeployment = false;
                            break;
                        } else {
                            // No need to continue, this service chain is fully offloadable.
                            continue;
                        }
                    } catch (DeploymentException depEx) {
                        throw depEx;
                    }
                }

                // Now let's see if the server-level deployment supports offloading
                boolean isOffloadable = sc.isHardwareBased();

                /**
                 * *********************************************************************************
                 * ****************************  B. Partial Offloading  ****************************
                 * *********************************************************************************
                 * *** B1. Software Part
                 * First find a candidate Metron server that can host this service chain, and then
                 * deploy the service chain there.
                 * The Metron agent of the server will give you the tags associated with this
                 * service chain, such that you can instruct the network elements accordingly.
                 */
                RestServerSBDevice server = null;
                try {
                    server = this.serverPlacer(sc, iface, dpTree, isOffloadable);
                } catch (DeploymentException depEx) {
                    correctDeployment = false;
                    break;
                }

                if (server == null) {
                    throw new DeploymentException("[" + label() +
                        "] Failed to instantiate the software part of service chain: " + scId);
                }

                // Now let's see if the deployment requires both network and server
               boolean serverAndNetworkCooperation = sc.isNetworkWide();

                /**
                 * *** B2. Hardware Part
                 * Use the tags above to mark the traffic classes of this service chain.
                 * Each traffic class consists of a set of SDN rules installed to one or
                 * more switches along the path between ingress and the target Metron server.
                 */
                if (serverAndNetworkCooperation) {
                    try {
                        if (!this.networkPlacer(sc, dpTree, server)) {
                            /**
                             * This can happen if e.g., the target switch is not connected with a Metron server.
                             * This failure results in tearing down the software part of this service chain.
                             */
                            correctDeployment = false;
                        }
                    } catch (DeploymentException depEx) {
                        throw depEx;
                    }
                }
            }

            // At this point, we can mark this service chain as DEPLOYED
            if (correctDeployment) {
                this.markServiceChainAsDeployed(sc, scIterator);
            // Or retract the deployment due to error
            } else {
                this.markServiceChainAsUndeployed(sc, scIterator);
            }
        }

        log.info(Constants.STDOUT_BARS);
    }

    /**
     * Fetch the topology from the Topology Manager and check
     * if there are available network elements. If so, try to
     * offload parts of the given service chain.
     *
     * @param sc     the service chain to be offloaded
     * @param dpTree the dataplane configuration of this service chain
     * @param server the Metron device where the software part of the
     *        service chain is offloaded
     *        (null means that the service chain is fully offloaded)
     * @return boolean status
     * @throws DeploymentException if network placement fails
     */
    private boolean networkPlacer(
            ServiceChainInterface sc, NfvDataplaneTreeInterface dpTree, RestServerSBDevice server)
            throws DeploymentException {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be placed in the network");
        checkNotNull(dpTree, "[" + label() + "] NULL dataplane configuration cannot be placed in the network");

        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] Offloading Metron traffic classes to hardware", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);

        // Obtain the ID of the service chain
        ServiceChainId scId = sc.id();

        // Scaling ability for this service chain
        boolean scale = sc.scale();

        // The number of CPU cores at the server side
        int numberOfCores = scale ? sc.cpuCores() : sc.maxCpuCores();

        // and the number of NICs required
        int numberOfNics = dpTree.numberOfNics();

        // Indicates successful outcome
        boolean offloaded = false;

        log.info("[{}] Hardware Offloader for service chain {}", label(), scId);

        for (TopologyCluster cluster : this.topologyClusters()) {
            // This is where the traffic enters
            TopologyVertex device = cluster.root();

            // We only care about network devices, not servers
            if (topologyService.isServer(device.deviceId())) {
                continue;
            }

            // This is a network device, keep its ID
            DeviceId coreSwitchId = device.deviceId();

            // The target switch must be an ingress point
            if (!sc.isIngressPoint(coreSwitchId)) {
                continue;
            }

            // Totally offloaded service chain is redirected towards an egress point
            if (server == null) {
                return placeTotallyOffloadedServiceChain(sc, dpTree, coreSwitchId);
            }

            // Port towards the server
            long coreSwitchMetronPort = ANY_PORT;
            // TODO: Eliminate lists?
            // Get the port where traffic is expected to show up
            TrafficPoint ingressPoint = sc.ingressPointOfDevice(coreSwitchId);
            long   coreSwitchIngrPort = ingressPoint.portIds().get(0).toLong();
            // Get the port where traffic is expected to leave
            TrafficPoint egressPoint  = sc.egressPointOfDevice(coreSwitchId);
            long   coreSwitchEgrPort  = egressPoint.portIds().get(0).toLong();

            // This is the destination we want to reach
            DeviceId serverId = server.deviceId();
            // TODO: Always 0? numberOfNics?
            List<Link> serverIngLinks = new ArrayList<Link>(
                topologyService.getDeviceIngressLinksWithPort(serverId, 0)
            );
            List<Link> serverEgrLinks = new ArrayList<Link>(
                topologyService.getDeviceEgressLinksWithPort(serverId, numberOfNics)
            );
            checkArgument(
                (serverIngLinks != null) && !serverIngLinks.isEmpty() &&
                (serverEgrLinks != null) && !serverEgrLinks.isEmpty(),
                "Empty list of server ingress links. Check your topology"
            );
            // TODO: How to make it more explicit?
            long serverIngrPort = serverIngLinks.get(0).dst().port().toLong();
            long serverEgrPort  = serverEgrLinks.get(0).src().port().toLong();

            if (this.hasPortConflicts(
                    serverIngLinks, coreSwitchId, coreSwitchIngrPort, coreSwitchEgrPort
                ) ||
                this.hasPortConflicts(
                    serverEgrLinks, coreSwitchId, coreSwitchIngrPort, coreSwitchEgrPort
                )) {
                log.error("[{}] Conflicts detected between {} and {} ingress/egress ports. " +
                    "Please check your JSON configuration", label(), coreSwitchId, serverId);
                return false;
            }

            log.info("[{}] \t Partial offloading of service chain {}", label(), scId);

            // Add the switch and the server to the set of processing points of this service chain
            addProcessingPoint(sc, coreSwitchId, coreSwitchIngrPort, coreSwitchEgrPort);
            addProcessingPoint(sc, serverId, serverIngrPort, serverEgrPort);

            // Get the shortest path between the target switch and the target server
            Path selectedFwdPath = this.selectShortestPath(coreSwitchId, ANY_PORT, serverId, serverIngrPort);
            // And the reverse path
            Path selectedBwdPath = this.selectShortestPath(
                serverId, serverEgrPort, coreSwitchId, ANY_PORT
            );
            if ((selectedFwdPath == null) || (selectedBwdPath == null)) {
                log.error("[{}] \t Could not establish forward/backward paths towards {} for service chain",
                    label(), serverId, scId);
                return false;
            }

            // Build a forward and a reverse path
            PathEstablisherInterface pathEstablisher = dpTree.createPathEstablisher(
                selectedFwdPath, selectedBwdPath, coreSwitchIngrPort, coreSwitchEgrPort, true
            );

            // Verify
            coreSwitchId = pathEstablisher.offloaderSwitchId().equals(coreSwitchId) ?
                coreSwitchId : null;
            coreSwitchMetronPort = pathEstablisher.offloaderSwitchMetronPort() > 0 ?
                pathEstablisher.offloaderSwitchMetronPort() : ANY_PORT;
            coreSwitchIngrPort = pathEstablisher.ingressPort() == coreSwitchIngrPort ?
                coreSwitchIngrPort : ANY_PORT;
            coreSwitchEgrPort  = pathEstablisher.egressPort()  == coreSwitchEgrPort ?
                coreSwitchEgrPort : ANY_PORT;
            serverId = pathEstablisher.serverId().equals(serverId) ? serverId : null;
            serverIngrPort = pathEstablisher.serverInressPort() == serverIngrPort ? serverIngrPort : ANY_PORT;
            serverEgrPort  = pathEstablisher.serverEgressPort() == serverEgrPort  ? serverEgrPort  : ANY_PORT;
            final String msg = String.format(
                "[%s] \t Core switch %s IN:%d/EG:%d -> Server %s IN:%d/EG:%d", label(),
                coreSwitchId, coreSwitchIngrPort, coreSwitchEgrPort, serverId, serverIngrPort, serverEgrPort
            );
            checkArgument(
                (coreSwitchId         != null)     &&
                (coreSwitchMetronPort != ANY_PORT) &&
                (coreSwitchIngrPort   != ANY_PORT) && (coreSwitchEgrPort != ANY_PORT) &&
                (serverId             != null)     &&
                (serverIngrPort       != ANY_PORT) && (serverEgrPort     != ANY_PORT),
                "Network path from " + device.deviceId() + " towards " + server.deviceId() + " is wrong. " + msg
            );

            // We might have a leaf switch, potentially different from the core one
            DeviceId  leafSwitchId = pathEstablisher.leafSwitchId();
            long leafSwitchEgrPort = pathEstablisher.leafSwitchEgressPort();
            boolean coreAndLeafMatch = leafSwitchId.equals(coreSwitchId);

            // Establish the path between server and egress point
            if (!serverToNetworkRules.containsKey(server)) {
                Set<FlowRule> bwdPathRules = pathEstablisher.egressRules(this.appId);
                if (this.installRules(scId, bwdPathRules, false)) {
                    // Add the set of rules to the map
                    serverToNetworkRules.put(server, bwdPathRules);
                }
            }

            /**
             * Generate the hardware configuration of this service chain.
             * Tagging flag is true to indicate partial offloading with tagging.
             */
            Map<URI, RxFilterValue> tcTags = this.createRules(
                scId, dpTree, coreSwitchId, numberOfCores, -1, -1, coreSwitchEgrPort, true, false
            );

            // Get the set of OpenFlow rules that comprise the hardware configuration
            Set<FlowRule> offloadingRules = dpTree.hardwareConfigurationToSet();

            // Push them to the switch
            this.installRules(scId, offloadingRules, false);

            offloaded = true;

            /**
             * Now establish the path between the ingress switch and the server.
             * This is only required when 1 or more switches lie between the
             * ingress switch and the Metron server.
             */
            if (!coreAndLeafMatch) {
                for (Map.Entry<URI, RxFilterValue> entry : tcTags.entrySet()) {
                    URI tcGroupId = entry.getKey();
                    RxFilterValue tag = entry.getValue();

                    Set<FlowRule> fwdPathRules = pathEstablisher.ingressRules(this.appId, tag);
                    this.installRules(scId, fwdPathRules, false);
                }
            }

            break;
        }

        log.info("[{}] {}\n", label(), Constants.STDOUT_BARS_SUB);

        return offloaded;
    }

   /**
     * Fetch the topology from the Topology Manager and check
     * if there are available Metron servers. If so, try to
     * (fully or partially) instantiate the given service chain.
     * Full or partial deployment depends upon the last argument.
     *
     * @param sc     the service chain to be offloaded
     * @param iface  the input interface of the configuration
     * @param dpTree the dataplane configuration of this service chain
     * @param isOffloadable a hardware offloader will be involved right
     *        after we place the service chain to the appropriate server.
     *        This offloader can either be a NIC or a programmable switch
     * @return RestServerSBDevice the selected server
     * @throws DeploymentException if network placement fails
     */
    private RestServerSBDevice serverPlacer(
            ServiceChainInterface sc, String iface, NfvDataplaneTreeInterface dpTree, boolean isOffloadable)
            throws DeploymentException {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be placed in a server");
        checkNotNull(dpTree, "[" + label() + "] NULL dataplane configuration cannot be placed in a server");

        // Obtain the ID of the service chain
        ServiceChainId scId = sc.id();

        boolean inFlowDirMode = sc.isServerLevel() && sc.isHardwareBased();
        boolean inRssMode = sc.isServerLevel() && sc.isSoftwareBased();

        // Scaling ability for this service chain
        boolean scale = sc.scale();
        // Autoscale must be asked by the user and approved by this module
        boolean autoScale = hasAutoScale(scId, sc.autoScale());
        // This is an upper limit of the cores you can use
        int userRequestedMaxCpus = sc.maxCpuCores();
        // If scaling is disabled, start with the maximum CPU cores
        int userRequestedCpus = scale ? sc.cpuCores() : userRequestedMaxCpus;
        // The required number of NICs
        int userRequestedNics = sc.nics();

        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] Instantiating server-level Metron traffic classes", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);

        // Pick a server with enough resources
        RestServerSBDevice server = null;
        if (sc.targetDevice() == null) {
            server = this.getLeastOverloadedServer(userRequestedMaxCpus, userRequestedNics);
        } else {
            server = this.getDesiredServer(sc.targetDevice(), userRequestedMaxCpus, userRequestedNics);
        }

        if (server == null) {
            if (sc.targetDevice() == null) {
                log.error(
                    "[{}] \t Service chain with ID {} cannot be deployed. " +
                    "Either no servers are available or none of the available servers " +
                    "complies with the processing requirements (i.e., {} NICs and up to {} CPU cores) " +
                    "of this service chain.",
                    label(), scId, userRequestedNics, userRequestedMaxCpus
                );
            } else {
                log.error(
                    "[{}] \t Service chain with ID {} cannot be deployed. " +
                    "The desired server with ID {} is either unavailable or " +
                    "it does not comply with the processing requirements (i.e., " +
                    "{} NICs and up to {} CPU cores) of this service chain.",
                    label(), scId, sc.targetDevice(), userRequestedNics, userRequestedMaxCpus
                );
            }

            return null;
        }

        DeviceId deviceId = server.deviceId();
        int serverNics = server.numberOfNics();

        // Requires rule installation in the server's NIC
        TrafficPoint ingressPoint = sc.ingressPointOfDevice(deviceId);
        checkNotNull(ingressPoint, "Cannot generate NIC flow rules without ingress point information");
        PortNumber serverIngPortNum = ingressPoint.portIds().get(0);
        long serverIngPort = serverIngPortNum.toLong();

        TrafficPoint egressPoint = sc.egressPointOfDevice(deviceId);
        checkNotNull(egressPoint, "Cannot generate NIC flow rules without egress point information");
        PortNumber serverEgrPortNum = egressPoint.portIds().get(0);
        long serverEgrPort = serverEgrPortNum.toLong();

        // In Flow Director or RSS modes there is no network placement, thus path must be built here
        if (inFlowDirMode || inRssMode) {
            dpTree.createPathEstablisher(
                new ConnectPoint(deviceId, serverIngPortNum),
                new ConnectPoint(deviceId, serverEgrPortNum),
                serverIngPort, serverEgrPort, true
            );

            addProcessingPoint(sc, deviceId, serverIngPort, serverEgrPort);
        }

        // Get the configuration
        String confType = dpTree.type().toString();
        checkNotNull(confType, "[" + label() + "] Service chain configuration type is NULL");
        Map<Integer, String> serviceChainConf = dpTree.softwareConfiguration(server, userRequestedCpus, isOffloadable);
        checkNotNull(serviceChainConf, "[" + label() + "] Service chain configuration is NULL");

        /**
         * This is the number of cores required for this service chain.
         * To avoid quickly exhausting the available CPUs by allocating
         * one core/traffic class, dpTree.softwareConfiguration performs
         * traffic class grouping.
         * If this grouping appears to be performing poorly, then a
         * traffic class deflation is triggered, splitting the group
         * into two sub-groups until load gets balanced.
         */
        int neededCpus = serviceChainConf.size();
        checkArgument(neededCpus > 0,
            "[" + label() + "] No server-level CPU configuration for service chain " +
                sc.name() + "; nothing to instantiate");
        checkArgument(neededCpus <= userRequestedMaxCpus,
            "[" + label() + "] Service chain " + sc.name() + " requires " + neededCpus +
            "CPU cores, but user has requested only " + userRequestedMaxCpus + " CPU cores");

        // This is the number of NICs required for this service chain
        int neededNics = dpTree.numberOfNics();
        checkArgument(serverNics >= neededNics,
            "Metron agent " + deviceId + " has " +
            Integer.toString(serverNics) + " NICs, but " +
            Integer.toString(neededNics) + " are required for this deployment"
        );
        checkArgument(userRequestedNics >= neededNics,
            "User defined ingress/egress point indicate the need for " +
            Integer.toString(userRequestedNics) + " NICs, " +
            "but the required number of NICs according to the Click elements is " +
            Integer.toString(neededNics)
        );

        Set<String> nics = new ConcurrentSkipListSet<String>();
        for (int i = 0; i < neededNics; i++) {
            NicDevice nic = (NicDevice) server.nics().toArray()[i];
            nics.add(nic.name());
        }

        // Runtime information per traffic class
        Set<TrafficClassRuntimeInfo> serviceChainRuntimeInfo =
                Sets.<TrafficClassRuntimeInfo>newConcurrentHashSet();

        // Deploy the traffic classes one by one
        for (Map.Entry<Integer, String> entry : serviceChainConf.entrySet()) {
            int core = entry.getKey().intValue();
            String conf = entry.getValue();

            // Scale=false means that we create 'userRequestedMaxCpus' groups of independent traffic classes
            // Each group is assigned to 1 CPU core and the maximum CPU cores is also 1 (thus never scales).
            int cores = scale ? sc.cpuCores() : 1;
            int maxCores = scale ? userRequestedMaxCpus : 1;

            // Fetch the ID of the (grouped) traffic class of this service chain
            URI tcId = dpTree.groupTrafficClassIdOnCore(core);

            // Ask from the topology manager to deploy this traffic class
            TrafficClassRuntimeInfo tcRuntimeInfo =
                topologyService.deployTrafficClassOfServiceChain(
                    deviceId, scId, tcId, sc.scope(), confType, conf, cores, maxCores, nics, autoScale);

            if (tcRuntimeInfo == null) {
                throw new DeploymentException("[" + label() + "] Failed to deploy traffic class " + tcId +
                    " of service chain " + scId);
            }

            log.debug("Runtime info: {}", tcRuntimeInfo.toString());

            // Add this traffic class to the service chain's set of traffic classes
            serviceChainRuntimeInfo.add(tcRuntimeInfo);

            // Update the monitoring service
            monitoringService.addActiveCoresToDevice(deviceId, cores);
        }

        // Push this information to the distributed store
        serviceChainService.addRuntimeInformationToServiceChain(scId, serviceChainRuntimeInfo);

        // Now the traffic class is deployed; ask Metron agent to provide deployment information
        if (!this.askForRuntimeInfo(scId, iface)) {
            throw new DeploymentException("\t Failed to retrieve runtime information for service chain " + scId);
        }

        // Perform rule installation in the server's NIC
        if (inFlowDirMode) {
            long numberOfQueues = (long) userRequestedCpus;

            // Generate the hardware configuration for this service chain
            Map<URI, RxFilterValue> tcTags = this.createRules(
                scId, dpTree, deviceId, userRequestedCpus, serverIngPort, numberOfQueues, serverEgrPort, true, false
            );

            // Get the set of rules that comprise the hardware configuration
            Set<FlowRule> rules = dpTree.hardwareConfigurationToSet();

            // Push them to the server
            this.installRules(scId, rules, false);
        }

        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("");

        return server;
    }

    /**
     * Handles the placement of a service chain that is totally
     * offloadable.
     *
     * @param sc the service chain to be offloaded
     * @param dpTree the dataplane configuration of this service chain
     * @param switchId the device ID of the switch that will host this chain
     * @return boolean status
     */
    private boolean placeTotallyOffloadedServiceChain(
            ServiceChainInterface sc, NfvDataplaneTreeInterface dpTree, DeviceId switchId) {
        ServiceChainId scId = sc.id();
        log.info(
            "[{}] \t Total offloading of service chain {} --> Switch {}",
            label(), scId, switchId
        );

        TrafficPoint ingressPoint = sc.ingressPointOfDevice(switchId);
        checkNotNull(ingressPoint,
            "Invalid ingress device to redirect traffic. Check your service chain configuration");
        PortNumber ingressPortNum = ingressPoint.portIds().get(0);
        long ingressPort = ingressPortNum.toLong();
        checkArgument(ingressPort >= 0, "Invalid ingress port to redirect traffic");

        TrafficPoint egressPoint = sc.egressPointOfDevice(switchId);
        checkNotNull(egressPoint,
            "Invalid egress device to redirect traffic. Check your service chain configuration");
        PortNumber egressPortNum = egressPoint.portIds().get(0);
        long egressPort = egressPortNum.toLong();
        checkArgument(egressPort >= 0, "Invalid egress port to redirect traffic");

        // Add this switch to the set of processing points of this service chain
        addProcessingPoint(sc, switchId, ingressPort, egressPort);

        /**
         * Generate the hardware configuration of this service chain.
         * Number of CPU cores is irrelevant as traffic will never reach a server,
         * therefore argument no4 is set to 1.
         * Tagging flag is false to indicate total offloading.
         */
        Map<URI, RxFilterValue> tcTags = this.createRules(
            scId, dpTree, switchId, 1, -1, -1, egressPort, false, true
        );

        // Get the set of OpenFlow rules that comprise the hardware configuration
        Set<FlowRule> rules = dpTree.hardwareConfigurationToSet();

        // Push them to the switch
        this.installRules(scId, rules, false);

        // Build the (short) path of this service chain
        PathEstablisherInterface pathEstablisher = dpTree.createPathEstablisher(
            new ConnectPoint(switchId, ingressPortNum),
            new ConnectPoint(switchId, egressPortNum),
            ingressPort, egressPort, false
        );

        return true;
    }

    @Override
    public void markServiceChainAsDeployed(
            ServiceChainInterface sc, Iterator<ServiceChainInterface> scIterator) {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be marked as deployed");

        // Update the state to DEPLOYED
        serviceChainService.processDeployedState(sc);

        // ...and add it to the set of "deployed" service chains
        this.deployedServiceChains.add(sc);

        // Remove it from the set of "ready to deploy" service chains
        scIterator.remove();
    }

    @Override
    public Set<ServiceChainInterface> deployedServiceChains() {
        return this.deployedServiceChains;
    }

    @Override
    public void markServiceChainAsUndeployed(
            ServiceChainInterface sc, Iterator<ServiceChainInterface> scIterator) {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be marked as undeployed");

        // Tear this service chain down
        this.tearDownServiceChain(sc);

        // Remove its run-time information from the store
        serviceChainService.deleteRuntimeInformationForServiceChain(sc.id());

        // Make this service chain non-runnable
        serviceChainService.deleteRunnableServiceChain(sc.id());

        // Unregister from the core
        serviceChainService.unregisterServiceChain(sc);

        // Remove this service chain from the list of deployed service chains
        if (scIterator != null) {
            scIterator.remove();
        } else {
            // Called by external module, remove without iterator
            this.deployedServiceChains.remove(sc);
        }
    }

    @Override
    public boolean installRules(ServiceChainId scId, Set<FlowRule> rules, boolean update) {
        if ((rules == null) || (rules.isEmpty())) {
            log.warn("[{}] \t Cannot install rules, NULL or empty set is given", label());
            return false;
        }

        // A rule operations object undertakes to install a batch of rules
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        // Populate rules in this object
        rules.stream()
            .filter(Objects::nonNull)
            .forEach(flowOpsBuilder::add);

        // For printing purposes
        final String operation;
        final String operationInf;

        // Update existing rules
        if (update) {
            operation    = "updated";
            operationInf = "update";
        // Add new rules
        } else {
            operation    = "installed";
            operationInf = "install";
        }

        // Close stages
        flowOpsBuilder.newStage();

        // START
        WallClockNanoTimestamp startInstMon = new WallClockNanoTimestamp();

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                // STOP
                WallClockNanoTimestamp endInstMon = new WallClockNanoTimestamp();

                // This is the time to install the rules
                float ruleInstDelay = (float) (endInstMon.unixTimestamp() - startInstMon.unixTimestamp());

                // Store it
                monitoringService.updateOffloadingInstallationDelay(scId, ruleInstDelay);

                log.info("[{}] \t Successfully {} a batch of {} rules", label(), operation, rules.size());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.error("[{}] \t Failed to {} a batch of {} rules", label(), operationInf, rules.size());

                // If rule installation/update fails, we will completely tear down the service chain
                ServiceChainInterface sc = serviceChainService.serviceChain(scId);
                markServiceChainAsUndeployed(sc, getServiceChainIterator(scId));
            }
        }));

        return true;
    }

    /**
     * Creates a set a service chain's hardware configuration for a given device.
     *
     * @param scId the target service chain
     * @param dpTree the data plane tree of the service chain
     * @param deviceId the device where hardware configuration will be applied
     * @param coresNumber the number of cores to spread the traffic classes across
     * @param ingressPort the ingress port where input traffic appears
     * @param queuesNumber the number of input queues to spread the traffic across
     * @param egressPort the egress port where matched traffic will be redirected
     * @param tagging boolean indicator for tagging
     * @param withRuntimeBuild boolean indicator for building traffic classes' run-time info
     * @return a map of traffic class IDs to their Rx filters
     */
    private Map<URI, RxFilterValue> createRules(
            ServiceChainId            scId,
            NfvDataplaneTreeInterface dpTree,
            DeviceId                  deviceId,
            int                       coresNumber,
            long                      ingressPort,
            long                      queuesNumber,
            long                      egressPort,
            boolean                   tagging,
            boolean                   withRuntimeBuild) {
        // Maps traffic classes to tag values
        Map<URI, RxFilterValue> tcTags = null;
        // Measures the delay to computate the rules
        Map<URI, Float> tcCompDelays = new ConcurrentHashMap<URI, Float>();
        try {
            tcTags = dpTree.generateHardwareConfiguration(
                scId, this.appId, deviceId, coresNumber, ingressPort, queuesNumber, egressPort, tagging, tcCompDelays
            );
        } catch (DeploymentException depEx) {
            log.error("[{}] {}", label(), depEx.toString());
            return null;
        }
        checkNotNull(tcTags, "Failed to generate hardware configuration for service chain " + scId);
        checkNotNull(tcCompDelays, "Failed to measure hardware configuration for service chain " + scId);

        // Report the time it took to do so
        this.reportRuleComputationDelays(scId, tcCompDelays);

        // Build runtime information objects for each traffic class
        if (withRuntimeBuild) {
            this.buildRuntimeInfoForHwBasedTrafficClass(
                scId, tcCompDelays.keySet(), deviceId
            );
        }

        return tcTags;
    }

    /**
     * Adds a device to the processing traffic points of a service chain.
     *
     * @param sc the target service chain
     * @param deviceId the device ID of the processing point
     * @param ingrPort the ingress port of the processing point
     * @param egrPort the egress port of the processing point
     */
    private void addProcessingPoint(ServiceChainInterface sc, DeviceId deviceId, long ingrPort, long egrPort) {
        checkNotNull(sc, "Cannot add processing point to null service chain");
        TrafficPoint.Builder procTrafficPointBuilder = TrafficPoint.builder()
                .type(PROCESSING.toString())
                .deviceId(deviceId.toString())
                .portIds(Sets.newHashSet(ingrPort, egrPort));
        sc.addProcessingPoint(procTrafficPointBuilder.build());
    }

    @Override
    public boolean updateRules(ServiceChainId scId, Set<FlowRule> rules) {
        return this.installRules(scId, rules, true);
    }

    @Override
    public boolean removeRules(ServiceChainId scId, Set<FlowRule> rules) {
        if (rules == null) {
            log.warn("[{}] \t Cannot remove rules, empty set is given", label());
            return false;
        }

        flowRuleService.removeFlowRules(rules.toArray(new FlowRule[]{}));
        log.info("[{}] \t {} rules are removed", label(), rules.size());

        return true;
    }

    @Override
    public boolean hasAutoScale(ServiceChainId scId, boolean userRequestedAutoScale) {
        if (userRequestedAutoScale && !this.enableAutoScale) {
            log.warn("Service chain {} requested autoscale but network operator has disabled this feature", scId);
        }
        return this.enableAutoScale && userRequestedAutoScale;
    }

    @Override
    public boolean hwOffloadingEnabled() {
        return this.enableHwOffloading;
    }

    /***************************** Relayed Services to TopologyManager. **************************/

    /**
     * Gets the current set of clusters of the topology from the TopologyManager.
     *
     * @return set of topology clusters
     */
    private Set<TopologyCluster> topologyClusters() {
        return topologyService.topologyClusters();
    }

    /**
     * Checks whether the underlying backbone topology exists or not.
     * By backbone we mean the infrastructure of switches/routers.
     *
     * @return boolean backbone topology status
     */
    private boolean topologyExists() {
        return topologyService.exists();
    }

    /************************** End of Relayed Services to TopologyManager. **********************/


    /************************************** Internal services. ***********************************/

    /**
     * Return a deployed service chain with a given ID.
     *
     * @param scId the service chain ID
     * @return service chain instance with this ID or null
     */
    private ServiceChainInterface deployedServiceChain(ServiceChainId scId) {
        for (ServiceChainInterface sc : this.deployedServiceChains) {
            if (sc.id().equals(scId)) {
                return sc;
            }
        }

        return null;
    }

    /**
     * Ask for the least overloaded device that meets the number
     * of CPU cores and NICs we want to use.
     *
     * @param numberOfCores number of CPU cores we want to use
     * @param numberOfNics number of NICs we want to use
     */
    private RestServerSBDevice getLeastOverloadedServer(int numberOfCores, int numberOfNics) {
        return (RestServerSBDevice) topologyService.getLeastOverloadedServerPowerOfTwoChoices(
            numberOfCores, numberOfNics
        );
    }

    /**
     * Ask for a desired device, provided that it meets the number
     * of CPU cores and NICs we want to use.
     *
     * @param targetDevId target Device ID
     * @param numberOfCores number of CPU cores we want to use
     * @param numberOfNics number of NICs we want to use
     */
    private RestServerSBDevice getDesiredServer(DeviceId targetDevId, int numberOfCores, int numberOfNics) {
        RestServerSBDevice server = (RestServerSBDevice) topologyService.getDevice(targetDevId);

        // No such server
        if (server == null) {
            return null;
        }

        // Check if it meets the resource criteria
        if ((server.numberOfNics() < numberOfNics) || (server.numberOfCpus() < numberOfCores)) {
            return null;
        }

        return server;
    }

    /**
     * Check if a switche's ingress/egress ports are contained into the
     * respective server ports. This indicates a confict as the same port
     * cannot be used both as an NFV server prt and as a traffic src/sink.
     *
     * @param serverLinks a list of links that end up/begin from a server
     * @param coreSwitchId the ID of the switch
     * @param coreSwitchIngrPort the ingress port of the switch
     * @param coreSwitchEgrPort the egress port of the switch
     * @return boolean conflict status
     */
    private boolean hasPortConflicts(
            List<Link> serverLinks, DeviceId coreSwitchId,
            long coreSwitchIngrPort, long coreSwitchEgrPort) {
        log.debug("[{}] \t Checking for topology conflicts", label());
        log.debug("[{}] \t\t Switch Ingress  Port {}", label(), coreSwitchIngrPort);
        log.debug("[{}] \t\t Switch  Egress  Port {}", label(), coreSwitchEgrPort);
        log.debug("[{}] \t\t Server  links {}", label(), serverLinks);

        for (Link link : serverLinks) {
            if (topologyService.linkHasDevice(link, coreSwitchId) &&
               (topologyService.linkHasPort(link, coreSwitchIngrPort) ||
                topologyService.linkHasPort(link, coreSwitchEgrPort))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Given a source and destination device in the network, find a set of
     * shortest paths and return one that also satisfies their ingress ports.
     *
     * @param srcDeviceId the ID of the source network element
     * @param srcPort the port of the source network element
     *        This argument can also be ANY_PORT
     * @param dstDeviceId the ID of the destination network element
     * @param dstPort the port of the destination network element
     *        This argument can also be ANY_PORT
     * @return the shortest path based on hop count
     */
    private Path selectShortestPath(
            DeviceId srcDeviceId, long srcPort,
            DeviceId dstDeviceId, long dstPort) {

        boolean onlySrcCheck = (srcPort >= 0) && (dstPort == ANY_PORT);
        boolean onlyDstCheck = (dstPort >= 0) && (srcPort == ANY_PORT);
        boolean noPortCheck  = (srcPort == ANY_PORT) && (dstPort == ANY_PORT);
        String srcPortStr = (srcPort == ANY_PORT) ? "ANY" : String.valueOf(srcPort);
        String dstPortStr = (dstPort == ANY_PORT) ? "ANY" : String.valueOf(dstPort);

        log.debug("Looking for shortest path between {}/{} and {}/{}",
            srcDeviceId, srcPortStr, dstDeviceId, dstPortStr);

        // Find a set of shortest paths between the core switch and the server
        Set<Path> shortestPaths = topologyService.getKShortestPathsBasedOnHopCount(
            srcDeviceId, dstDeviceId, DEF_NUMBER_OF_SW_TO_SRV_PATHS);

        // This is really bad!
        if ((shortestPaths == null) || (shortestPaths.size() == 0)) {
            throw new DeploymentException("[" + label() + "] Failed to retrieve a path between " +
                srcDeviceId + " and " + dstDeviceId);
        }

        // Get a path that satisfies the port requirements
        for (Path path : shortestPaths) {
            Link firstLink = path.links().get(0);
            Link lastLink = path.links().get(path.links().size() - 1);

            if (noPortCheck) {
                return path;
            }

            long srcIngrPort = firstLink.src().port().toLong();
            long dstIngrPort = lastLink.dst().port().toLong();

            if ((onlySrcCheck && topologyService.linkHasPort(firstLink, srcPort)) ||
                (onlyDstCheck && topologyService.linkHasPort(lastLink,  dstPort)) ||
                (topologyService.linkHasPort(firstLink, srcPort) &&
                 topologyService.linkHasPort(lastLink, dstPort))) {
                log.info("[{}] \t Retrieved a path between {}/{} and {}/{}",
                    label(), srcDeviceId, srcPortStr, dstDeviceId, dstPortStr);
                return path;
            }
        }

        log.error("[{}] \t Failed to retrieve a path between {}/{} and {}/{}",
            label(), srcDeviceId, srcPortStr, dstDeviceId, dstPortStr);

        return null;
    }

    /**
     * A totally offloaded service chain requires a runtime information object although
     * it does not run on any server.
     * This method constructs such an object for each traffic class of such a service chain.
     *
     * @param scId the ID of the service chain to be queried
     * @param tcIds the set of traffic classes that compise this service chain
     * @param deviceId the device where this service chain is offloaded
     */
    private void buildRuntimeInfoForHwBasedTrafficClass(ServiceChainId scId, Set<URI> tcIds, DeviceId deviceId) {
        // Runtime information per traffic class
        Set<TrafficClassRuntimeInfo> serviceChainRuntimeInfo = Sets.<TrafficClassRuntimeInfo>newConcurrentHashSet();

        // Deploy the traffic classes one by one
        for (URI tcId : tcIds) {
            // Ask from the topology manager to deploy this traffic class
            TrafficClassRuntimeInfo tcRuntimeInfo =
                topologyService.buildRuntimeInformation(
                    deviceId, scId, tcId, "", "", "", 0, 0, new ConcurrentSkipListSet<String>(), ""
                );

            if (tcRuntimeInfo == null) {
                throw new DeploymentException(
                    "[" + label() + "] Failed to deploy traffic class " + tcId + " of service chain " + scId);
            }

            log.debug("Runtime info: {}", tcRuntimeInfo.toString());

            // Add this traffic class to the service chain's set of traffic classes
            serviceChainRuntimeInfo.add(tcRuntimeInfo);

            // Update the monitoring service
            monitoringService.addActiveCoresToDevice(deviceId, 0);
        }

        // Push this information to the distributed store
        serviceChainService.addRuntimeInformationToServiceChain(scId, serviceChainRuntimeInfo);

    }

    /**
     * After a service chain is deployed, a runtime information object
     * must be constructed per traffic class. This object must be updated
     * by the devices that execute the service chain. The updates mostly
     * include the Rx filters (tags) used by the devices to ensure core
     * affinity and the CPU core numbers that participate in the processing.
     *
     * @param scId the ID of the service chain to be queried
     * @param iface the input interface of the service chain's configuration
     * @return boolean status
     */
    private boolean askForRuntimeInfo(ServiceChainId scId, String iface) {
        // Get the runnable service chain
        NfvDataplaneTreeInterface dpTree = serviceChainService.runnableServiceChainOfIface(scId, iface);

        // Iterate through the grouped traffic classes
        for (URI tcGroupId : dpTree.groupedTrafficClasses().keySet()) {
            // Unresponsive traffic class means trouble
            if (!this.askForRuntimeInfoOfTrafficClass(scId, tcGroupId, dpTree)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fetches and updates the runtime information for a traffic class.
     *
     * @param scId the ID of the service chain to be queried
     * @param tcId the ID of the traffic class to be queried
     * @param dpTree the dataplane processing tree of this traffic class
     * @return boolean status
     */
    private boolean askForRuntimeInfoOfTrafficClass(
            ServiceChainId scId, URI tcId, NfvDataplaneTreeInterface dpTree) {
        // Fetch the runtime information of this service chain from the store
        TrafficClassRuntimeInfo initRuntimeInfo = serviceChainService
            .runtimeInformationForTrafficClassOfServiceChain(scId, tcId);

        if (initRuntimeInfo == null) {
            log.error("[{}] Runtime information for traffic class {} is NULL", label(), tcId);
            return false;
        }

        // This will host the updated runtime information
        TrafficClassRuntimeInfo runtimeInfo = null;

        // Update across all the devices that might host this traffic class
        for (DeviceId deviceId : initRuntimeInfo.devices()) {
            // Ask from the Metron agent to update the information
            runtimeInfo = topologyService.updateTrafficClassRuntimeInfo(
                deviceId, scId, tcId, initRuntimeInfo
            );

            if (runtimeInfo == null) {
                log.error("[{}] Device {} provided no runtime information for traffic class {}.",
                    label(), deviceId, tcId);
                return false;
            }

            // Update our local cache
            this.addTrafficClassRuntimeInfo(scId, tcId, runtimeInfo);
            this.updateTaggingInformation(scId, tcId, dpTree, deviceId, runtimeInfo);
        }

        log.debug("[{}] Updated runtime information for traffic class {}", label(), tcId);
        log.debug("[{}] \t {}", label(), runtimeInfo.toString());

        return true;
    }

    /**
     * Update the specific local memory that keeps the tagging information
     * for each traffic class and for each service chain.
     *
     * @param scId the ID of the service chain to be updated
     * @param tcId the ID of the traffic class to be updated
     * @param dpTree the dataplane processing tree of this traffic class
     * @param deviceId the ID of the device that runs this traffic class
     * @param runtimeInfo the object that contains the newly retrieved
     *        runtime information for this traffic class
     * @return boolean status
     */
    private void updateTaggingInformation(
            ServiceChainId scId, URI tcId,
            NfvDataplaneTreeInterface dpTree,
            DeviceId deviceId, TrafficClassRuntimeInfo runtimeInfo) {

        String nic = runtimeInfo.primaryNic();

        // Get the group of traffic classes that correspond to this ID
        Set<TrafficClassInterface> tcGroup = dpTree.getGroupedTrafficClassesWithID(tcId);

        // Fetch the tagging information from the grand object
        RxFilter rxFilter = runtimeInfo.rxFilterMethodOfDeviceOfNic(deviceId, nic);
        Set<RxFilterValue> rxFilterValues = runtimeInfo.rxFiltersOfDeviceOfNic(deviceId, nic);

        // Verify that there is no memory error
        checkNotNull(rxFilter,
            "[" + label() + "] NULL Rx mechanism for traffic class " + tcId + " of service chain " + scId);

        // Provide relevant information for this group of traffic classes to the Tag Manager
        if (rxFilterValues != null) {
            taggingService.establishTaggingForTrafficClassGroup(tcId, tcGroup, rxFilter, rxFilterValues);
        }

        return;
    }

    /**
     * Request from the Metron agents of the devices that host a service chain
     * to tear it down and release the resources.
     *
     * @param devId the ID of the device that hosts the service chain
     * @param scId the ID of the service chain to be deleted
     * @param tcId the ID of the traffic class to be deleted
     * @return boolean status
     */
    private boolean deleteTrafficClassOfServiceChain(DeviceId devId, ServiceChainId scId, URI tcId) {
        // Delete
        if (!topologyService.deleteTrafficClassOfServiceChain(devId, scId, tcId)) {
            log.error("[{}] Failed to delete traffic class {} of service chain {}", tcId, scId);
            return false;
        }

        return true;
    }

    /**
     * Get runtime information for all the traffic classs of a service chain
     * from our local cache.
     *
     * @param scId the ID of the service chain to be queried
     * @return map of traffic class ID to information objects
     */
    private Map<URI, TrafficClassRuntimeInfo> getServiceChainRuntimeInfo(ServiceChainId scId) {
        if (!this.runtimeInfo.containsKey(scId)) {
            return null;
        }

        return this.runtimeInfo.get(scId);
    }

    /**
     * Get runtime information for a traffic class of a service chain
     * from our local cache.
     *
     * @param scId the ID of the service chain to be queried
     * @param tcId the ID of the traffic class to be queried
     * @return info the information object of this traffic class
     */
    private TrafficClassRuntimeInfo getTrafficClassRuntimeInfo(ServiceChainId scId, URI tcId) {
        if (!this.runtimeInfo.containsKey(scId)) {
            return null;
        }

        return this.runtimeInfo.get(scId).get(tcId);
    }

    /**
     * Add runtime information for a traffic class of a service chain
     * int our local cache.
     *
     * @param scId the ID of the service chain to be added
     * @param tcId the ID of the traffic class to be added
     * @param info the information object of this traffic class
     */
    private void addTrafficClassRuntimeInfo(ServiceChainId scId, URI tcId, TrafficClassRuntimeInfo info) {
        Map<URI, TrafficClassRuntimeInfo> scInfo = null;

        if (this.runtimeInfo.containsKey(scId)) {
            scInfo = this.runtimeInfo.get(scId);
        } else {
            scInfo = new ConcurrentHashMap<URI, TrafficClassRuntimeInfo>();
        }

        scInfo.put(tcId, info);

        this.runtimeInfo.put(scId, scInfo);
    }

    /**
     * Report the time it took to compute the OpenFlow rules for each traffic class
     * of the given service chain.
     *
     * @param scId the ID of the service chain that requires these rules
     * @param tcCompDelays map of traffic class IDs to their computaitonal delay
     */
    private void reportRuleComputationDelays(ServiceChainId scId, Map<URI, Float> tcCompDelays) {
        if (tcCompDelays == null) {
            return;
        }

        for (Map.Entry<URI, Float> entry : tcCompDelays.entrySet()) {
            URI tcId = entry.getKey();
            float compDelay = entry.getValue().floatValue();

            // Store it
            monitoringService.updateOffloadingComputationDelayOfTrafficClass(scId, tcId, compDelay);
        }
    }

    /**
     * Updates the 'launch' delay a service chain's traffic classes, by adding
     * a component that corresponds to the HW delay to instal the OpenFlow rules.
     *
     * @param scId the ID of the service chainto be updated
     * @param ruleInstDelay the delay to install OpenFlow rules for this service chain
     */
    private void updateLaunchTimeOfServiceChain(ServiceChainId scId, float ruleInstDelay) {
        // The 'launch' time for this service chain is the time to install the rules
        Map<String, NfvDataplaneTreeInterface> dpTrees = serviceChainService.runnableServiceChains(scId);

        if (dpTrees.size() == 0) {
            throw new DeploymentException(
                "[" + label() + "] No dataplane configuration is found for service chain " + scId);
        }

        // The delay to install the rules is first divided among the dataplane trees of this service chain
        long perTreeDelay = (long) ruleInstDelay / dpTrees.size();

        for (NfvDataplaneTreeInterface dpTree : dpTrees.values()) {
            int tcNb = dpTree.groupedTrafficClasses().size();

            if (tcNb == 0) {
                continue;
            }

            // Now we further divide this time among the traffic classes of this tree
            long tcDelay = perTreeDelay / tcNb;

            for (Map.Entry<URI, Set<TrafficClassInterface>> entry :
                    dpTree.groupedTrafficClasses().entrySet()) {
                URI tcGroupId = entry.getKey();

                /**
                 * This delay is positive when a traffic class has a SW-based part (already deployed on a server).
                 * In this case we have to add up the HW-based delay.
                 * If a traffic class in purely implemented in HW, then currLaunchTcDelay is negative,
                 * hence the only `launch` delay is the one to install OF rules.
                 */
                long currLaunchTcDelay = monitoringService.launchDelayOfTrafficClass(scId, tcGroupId);

                // Add the SW-side component of the launch delay to the HW-side one.
                if (currLaunchTcDelay < 0) {
                    tcDelay += currLaunchTcDelay;
                }

                // Update at the traffic class level
                monitoringService.updateLaunchDelayOfTrafficClass(scId, tcGroupId, tcDelay);
            }
        }
    }

    /**
     * Go through the deployed service chains and ask from their Metron agents
     * to undeploy them.
     */
    private void tearDownServiceChains() {
        // No deployed service chains..
        if (this.deployedServiceChains().size() == 0) {
            return;
        }

        // Go though the deployed chains
        Iterator<ServiceChainInterface> scIterator = this.deployedServiceChains().iterator();
        while (scIterator.hasNext()) {
            ServiceChainInterface sc = scIterator.next();
            // Tear this service chain down
            this.markServiceChainAsUndeployed(sc, scIterator);
        }

        checkArgument(
            this.serviceChainsToDeploy.size() == 0,
            "[" + label() + "] Memory of `ready to deploy` service chains is not really empty; " +
            this.serviceChainsToDeploy.size() + " found."
        );

        checkArgument(
            this.deployedServiceChains.size() == 0,
            "[" + label() + "] Memory of `deployed` service chains is not really empty; " +
            this.deployedServiceChains.size() + " found."
        );
    }

    /**
     * Upon a state change to DESTROYED, a service chain needs to
     * be undeployed from the dataplane.
     *
     * @param sc the service chain to be undeployed
     */
    private void tearDownServiceChain(ServiceChainInterface sc) {
        ServiceChainId scId = sc.id();

        // Remove any rules, potentially installed by this service chain
        this.removeRulesOfServiceChain(scId);

        // Go through the traffic classes of this service chain
        Set<TrafficClassRuntimeInfo> tcSetInfo =
            serviceChainService.runtimeInformationForServiceChain(scId);
        if (tcSetInfo == null) {
            return;
        }

        Iterator<TrafficClassRuntimeInfo> tcIterator = tcSetInfo.iterator();
        if (tcIterator == null) {
            log.error("[{}] Failed to delete service chain {}", label(), scId);
            return;
        }

        while (tcIterator.hasNext()) {
            TrafficClassRuntimeInfo tcInfo = tcIterator.next();

            // Fetch the ID of the traffic class of this service chain
            URI tcId = tcInfo.trafficClassId();

            // And the devices where it is deployed
            for (DeviceId deviceId : tcInfo.devices()) {
                // Only for servers
                if (!topologyService.deviceExists(deviceId) ||
                    !topologyService.isServer(deviceId)) {
                    continue;
                }

                // Ask the Metron agent to delete it
                if (!topologyService.deleteTrafficClassOfServiceChain(deviceId, scId, tcId)) {
                    log.error("[{}] Failed to delete traffic class {} of service chain {}",
                        label(), tcId, scId);
                } else {
                    log.info("[{}] Successfully deleted traffic class {} of service chain {}",
                        label(), tcId, scId);
                    tcIterator.remove();
                    serviceChainService.deleteRuntimeInformationForTrafficClassOfServiceChain(scId, tcInfo);
                    break;
                }
            }
        }
    }

    /**
     * Removes any rules installed by a service chain.
     *
     * @param scId the ID of the service chain
     */
    private void removeRulesOfServiceChain(ServiceChainId scId) {
        Map<String, NfvDataplaneTreeInterface> dpTrees = serviceChainService.runnableServiceChains(scId);

        if ((dpTrees == null) || (dpTrees.values() == null)) {
            log.warn("[{}] Service chain with ID {} is not runnable; no rules for removal",
                label(), scId);
            return;
        }

        Set<FlowRule> rules = Sets.<FlowRule>newConcurrentHashSet();

        for (NfvDataplaneTreeInterface dpTree : dpTrees.values()) {
            // Get the set of OpenFlow rules that comprise the hardware configuration
            rules.addAll(dpTree.hardwareConfigurationToSet());
        }

        // Remove these rules
        this.removeRules(scId, rules);
    }

    /**
     * Prints the "ready to deploy" service chains.
     */
    private void printReadyToDeployServiceChains() {
        for (ServiceChainInterface sc : this.serviceChainsToDeploy) {
            log.info("[{}] Service chain with ID {} is ready to be deployed", label(), sc.id());
        }
    }

    /**
     * Prints the deployed service chains.
     */
    private void printDeployedServiceChains() {
        for (ServiceChainInterface sc : this.deployedServiceChains) {
            log.info("[{}] Service chain with ID {} is deployed", label(), sc.id());
        }
    }

    /**
     * Returns whether there are service chains to be deployed or not.
     *
     * @return boolean existence of service chains to be deployed
     */
    private boolean hasServiceChainsToDeploy() {
        return this.serviceChainsToDeploy.size() > 0;
    }

    /**
     * Returns whether there are network elements present to
     * perform the HW offloading.
     *
     * @param sc the service chain that requires hardware offloading
     * @return boolean status of the HW offloading process
     */
    private boolean canDoNetworkOffloading(ServiceChainInterface sc) {
        // Server-level deployment implies offloading into local NICs
        if (sc.isServerLevel()) {
            return false;
        }

        checkArgument(
            this.hwOffloadingEnabled(),
            "Network-wide service chain deployment requires to enable Metron's hardware offloading. " +
            "Set configuration property '" + ENABLE_HW_OFFLOADING + " = true'"
        );

        checkArgument(
            topologyService.hasSwitches(),
            "Network-wide service chain deployment requires the presence of programmable switches. " +
            "If no programmable switches exist, use scope='server-rules' or scope='server-rss'"
        );

        return true;
    }

    /**
     * Extracts properties from the component configuration context
     * and updates local parameters accordingly.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();

        // Property for hardware offloading is given
        if (Tools.isPropertyEnabled(properties, ENABLE_HW_OFFLOADING) != null) {
            boolean previousEnableHwOffloading = this.enableHwOffloading;
            this.enableHwOffloading = Tools.isPropertyEnabled(properties, ENABLE_HW_OFFLOADING, DEF_HW_OFFLOADING);

            if (this.enableHwOffloading != previousEnableHwOffloading) {
                log.info("Configured! Hardware offloading is now {}", this.enableHwOffloading ? "enabled" : "disabled");
            } else {
                log.info("Hardware offloading remains {}", this.enableHwOffloading ? "enabled" : "disabled");
            }
        }

        // Property for autoscale is given
        if (Tools.isPropertyEnabled(properties, ENABLE_AUTOSCALE) != null) {
            boolean previousEnableAutoScale = this.enableAutoScale;
            this.enableAutoScale = Tools.isPropertyEnabled(properties, ENABLE_AUTOSCALE, DEF_AUTOSCALE);

            if (this.enableAutoScale != previousEnableAutoScale) {
                log.info("Configured! Autoscale is now {}", this.enableAutoScale ? "enabled" : "disabled");
            } else {
                log.info("Autoscale remains {}", this.enableAutoScale ? "enabled" : "disabled");
            }
        }
    }

    /**
     * Returns an iterator object for a particular active service chain.
     *
     * @param scId the Id of the service chain
     * @return iterator of this service chain
     */
    private Iterator<ServiceChainInterface> getServiceChainIterator(ServiceChainId scId) {
        // Go though the deployed chains
        Iterator<ServiceChainInterface> scIterator = this.deployedServiceChains().iterator();
        while (scIterator.hasNext()) {
            ServiceChainInterface sc = scIterator.next();

            if (sc.id().equals(scId)) {
                return scIterator;
            }
        }

        return null;
    }

    /**
     * Returns a label with the Metron Deployer's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

    /*********************************** End of internal services. *******************************/

    /**
     * Handles events related to the service chains that reside in the service chain store.
     * The Metron Deployer cares about service chains that are in state READY.
     * This state comes after CONSTRUCTED and/or SYNTHESIZED.
     */
    protected class InternalServiceChainListener implements ServiceChainListenerInterface {
        @Override
        public void event(ServiceChainEvent event) {
            // Parse the event to identify the service chain and its state
            ServiceChainState state  = event.type();
            ServiceChainInterface sc = event.subject();

            // Filter out the events we do care about.
            if (this.isReady(state)) {
                log.info("");
                log.info("[{}] Service chain with ID {} is in state {}", label(), sc.id(), state);

                // Add this service chain to the list of "ready to deploy" service chains
                sendServiceChainToDeployer(sc);

                // Perform the deployment
                deployerExecutor.execute(() -> {
                    try {
                        deploy();
                    } catch (DeploymentException depEx) {
                        throw depEx;
                    }
                });
            }
        }

        /**
         * Service chain events must strictly exhibit states
         * specified in ServiceChainState.
         *
         * @param state the state of the service chain
         * @return boolean validity
         */
        private boolean isValidState(ServiceChainState state) {
            return ArrayUtils.contains(ServiceChainState.values(), state);
        }
        /**
         * Returns whether a service chain is in state READY or not.
         * Such a service chain might either come from the application
         * (after being constructed) or from the synthesizer (after
         * transforming the application's service chain to an optimized one).
         * The Metron Deployer undertakes to deploy this service chain.
         *
         * @param state the state of the service chain
         * @return boolean is ready or not
         */
        private boolean isReady(ServiceChainState state) {
            return isValidState(state) && (state == ServiceChainState.READY);
        }
        /**
         * Returns whether a service chain is in state SUSPENDED or not.
         *
         * @param state the state of the service chain
         * @return boolean is suspended or not
         */
        private boolean isSuspended(ServiceChainState state) {
            return isValidState(state) && (state == ServiceChainState.SUSPENDED);
        }
    }

}
