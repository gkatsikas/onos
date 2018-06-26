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
import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.dataplane.TagService;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.monitor.WallClockNanoTimestamp;
import org.onosproject.metron.api.orchestrator.DeploymentService;
import org.onosproject.metron.api.orchestrator.OrchestrationService;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainService;
import org.onosproject.metron.api.servicechain.ServiceChainListenerInterface;
import org.onosproject.metron.api.structures.Pair;
import org.onosproject.metron.api.topology.NfvTopologyService;

import org.onosproject.metron.impl.dataplane.NfvDataplaneTree;

// ONOS libraries
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.store.service.WallClockTimestamp;

import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;

// Apache libraries
import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Guava
import com.google.common.collect.Sets;

// Other libraries
import org.slf4j.Logger;

// Java libraries
import java.net.URI;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.util.Tools.groupedThreads;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * A service that undertakes to deploy and manage Metron service chains.
 */
@Component(immediate = true)
@Service
public final class OrchestrationManager
        implements OrchestrationService {

    private static final Logger log = getLogger(OrchestrationManager.class);

    /**
     * Application ID for the Metron Orchestrator.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".orchestrator";
    private static final String COMPONET_LABEL = "Metron Orchestrator";

    /**
     * Some management constants.
     */
    // The frequency of the Orchestrator's monitor in milliseconds
    private static final int   MONITORING_PERIOD_MS = 500;
    // The CPU thresholds used for load balancing
    private static final float ZERO_CPU_LOAD        = (float) 0.0;
    private static final float CPU_OVERLOAD_LIMIT   = (float) 0.75;
    private static final float CPU_UNDERLOAD_LIMIT  = (float) 0.15;

    /**
     * Members:
     * |-> An application ID is necessary to register with the core.
     * |-> A set of already deployed service chains (active).
     * |-> A set of suspended service chains (undeployed due to some issue).
     * |-> A map between service chains and their runtime information.
     */
    private ApplicationId appId = null;
    private boolean autoScale   = DeploymentManager.DEF_AUTOSCALE;
    private Set<ServiceChainInterface> activeServiceChains    = null;
    private Set<ServiceChainInterface> suspendedServiceChains = null;

    /**
     * Listen to events dispatched by the Service Chain Manager.
     * These events are related to the state of the service chains
     * that reside in the Service Chain Store.
     */
    private final ServiceChainListenerInterface serviceChainListener = new InternalServiceChainListener();

    /**
     * A dedicated thread pool to orchestrate Metron service chains at runtime.
     */
    private static final int MANAGER_THREADS_NO = 3;
    private final ExecutorService managerExecutor = newFixedThreadPool(
        MANAGER_THREADS_NO,
        groupedThreads(this.getClass().getSimpleName(), "sc-orchestrator", log)
    );

    /**
     * The Metron Orchestrator requires:
     * 1) the ONOS core service to register,
     * 2) the Service Chain Manager to fetch information about Metron service chains.
     * 3) the Topology Manager to interact with the devices.
     * 4) the Monitoring service to update the system's runtime information.
     * 5) the Deployer's services to fetch the list of active Metron service chains.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ServiceChainService serviceChainService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NfvTopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MonitorService monitoringService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeploymentService deployerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TagService taggingService;

    public OrchestrationManager() {
        this.activeServiceChains    = Sets.<ServiceChainInterface>newConcurrentHashSet();
        this.suspendedServiceChains = Sets.<ServiceChainInterface>newConcurrentHashSet();
    }

    @Activate
    protected void activate() {
        // Register the Metron Orchestrator with the core.
        this.appId = coreService.registerApplication(APP_NAME);

        // Catch events coming from the service chain manager.
        serviceChainService.addListener(serviceChainListener);

        // Get this from the deployer
        this.autoScale = deployerService.hasAutoScale();

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        // Remove the listener for the events coming from the service chain manager.
        serviceChainService.removeListener(serviceChainListener);

        // Take care of the thread pool
        this.managerExecutor.shutdown();

        // Clean up your memory
        this.activeServiceChains.clear();
        this.suspendedServiceChains.clear();

        log.info("[{}] Stopped", label());
    }

    @Override
    public void sendServiceChainToOrchestrator(ServiceChainInterface sc) {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be managed");

        this.activeServiceChains.add(sc);
    }

    @Override
    public Set<ServiceChainInterface> activeServiceChains() {
        return this.activeServiceChains;
    }

    @Override
    public ServiceChainInterface activeServiceChainWithId(ServiceChainId scId) {
        checkNotNull(scId, "[" + label() + "] NULL service chain ID");

        for (ServiceChainInterface sc : this.activeServiceChains) {
            if (sc.id().equals(scId)) {
                return sc;
            }
        }
        return null;
    }

    @Override
    public void markServiceChainAsUndeployed(
            ServiceChainInterface sc, Iterator<ServiceChainInterface> scIterator) {
        checkNotNull(sc, "[" + label() + "] NULL service chain cannot be marked as undeployed");

        // Notify the deployer
        deployerService.markServiceChainAsUndeployed(sc, null);

        // ...and add it to the set of suspended service chains
        this.suspendedServiceChains.add(sc);

        // Remove this service chain from the list of active service chains
        scIterator.remove();
    }

    @Override
    public void manage() {
        log.debug(Constants.STDOUT_BARS);
        log.debug("Service Chains' Manager");
        log.debug(Constants.STDOUT_BARS);

        Map<ServiceChainId, Map<URI, AtomicReference<Float>>> previousLoad =
            new ConcurrentHashMap<ServiceChainId, Map<URI, AtomicReference<Float>>>();

        Map<ServiceChainId, Map<URI, AtomicBoolean>> isRebalanced =
            new ConcurrentHashMap<ServiceChainId, Map<URI, AtomicBoolean>>();

        while (true) {
            // Periodic monitoring every MONITORING_PERIOD_MS milliseconds
            try {
                Thread.sleep(MONITORING_PERIOD_MS);
            } catch (InterruptedException intEx) {
                continue;
            }

            MonitoringStatistics previousStats = null;

            // Go though the active chain, monitor them, and make load balancing decisions
            Iterator<ServiceChainInterface> scIterator = this.activeServiceChains.iterator();
            while (scIterator.hasNext()) {
                ServiceChainInterface sc = scIterator.next();
                ServiceChainId scId = sc.id();

                log.debug("[{}] Monitoring active service chain {}", label(), scId);

                // Allocate local resources at the service chain level
                if (!previousLoad.containsKey(scId)) {
                    previousLoad.put(scId, new ConcurrentHashMap<URI, AtomicReference<Float>>());
                }
                if (!isRebalanced.containsKey(scId)) {
                    isRebalanced.put(scId, new ConcurrentHashMap<URI, AtomicBoolean>());
                }

                // Fetch the runtime info for this service chain
                Set<TrafficClassRuntimeInfo> scInfo = serviceChainService.runtimeInformationForServiceChain(scId);

                // Prevent a crash
                if (scInfo == null) {
                    continue;
                }

                Iterator<TrafficClassRuntimeInfo> tcIterator = scInfo.iterator();

                while (tcIterator.hasNext()) {
                    TrafficClassRuntimeInfo tcInfo = tcIterator.next();

                    // Fetch the ID of the traffic class of this service chain
                    URI tcId = tcInfo.trafficClassId();

                    // And the maximum number of CPUs that it can currently have (we can ask for more)
                    int maxCpus = tcInfo.maxNumberOfCpus();

                    // Allocate local resources at the traffic class level
                    if (!previousLoad.get(scId).containsKey(tcId)) {
                        previousLoad.get(scId).put(tcId, new AtomicReference<Float>(new Float(0)));
                    }
                    if (!isRebalanced.get(scId).containsKey(tcId)) {
                        isRebalanced.get(scId).put(tcId, new AtomicBoolean(false));
                    }

                    for (DeviceId deviceId : tcInfo.devices()) {

                        if (!topologyService.isNfvDevice(deviceId)) {
                            continue;
                        }

                        // Get monitoring statistics from this service chain
                        MonitoringStatistics stats = this.doMonitoring(deviceId, scId, tcId);

                        // This device seems to be down or a problem suddenly occured
                        if (stats == null) {
                            // There is something wrong with this service chain
                            if (previousStats == null) {
                                // Undeploy it
                                this.markServiceChainAsUndeployed(sc, scIterator);
                            }

                            continue;
                        } else {
                            previousStats = stats;
                        }

                        AtomicReference<Float> previousLoadTc = previousLoad.get(scId).get(tcId);
                        AtomicBoolean isRebalancedTc = isRebalanced.get(scId).get(tcId);

                        // Check for potential load imbalance
                        this.checkForLoadImbalance(
                            sc,
                            tcId,
                            deviceId,
                            stats,
                            maxCpus,
                            previousLoadTc,
                            isRebalancedTc
                        );
                    }
                }
            }

            // No need to monitor an `empty` system
            if (!this.hasActiveServiceChains()) {
                break;
            }
        }

        log.debug(Constants.STDOUT_BARS);
    }

    /**
     * Given the statistics obtained for a traffic class, check whether
     * this traffic class exhibits overload and act accordingly.
     *
     * @param sc the overloaded service chain
     * @param tcId the ID of the overloaded traffic class
     * @param deviceId the ID of the overloaded device
     * @param stats monitoring statistics for a given traffic class
     * @param maxCpus the new maximum number of CPUs you can currently have
     * @param previousLoad the load level of this traffic class in the previous iteration
     * @param isRebalanced indicates that this traffic class is just rebalanced
     */
    private void checkForLoadImbalance(
            ServiceChainInterface  sc,
            URI                    tcId,
            DeviceId               deviceId,
            MonitoringStatistics   stats,
            int                    maxCpus,
            AtomicReference<Float> previousLoad,
            AtomicBoolean          isRebalanced) {
        // Skip the check and reset the flag
        if (isRebalanced.get()) {
            log.debug("[{}] \t Skipping rebalance check for traffic class {}", label(), tcId);
            isRebalanced.set(false);
            return;
        }

        ServiceChainId scId = sc.id();

        // Fetch the dataplane tree of this service chain
        NfvDataplaneTreeInterface tree = serviceChainService.runnableServiceChainWithTrafficClass(
            scId, tcId
        );

        // Get the list of CPU cores with non-zero load
        Collection<CpuStatistics> cpuStats = stats.cpuStatisticsAll();

        Map<Integer, Float> loadedCores = this.getCoresWithLoadGreaterThan(cpuStats, ZERO_CPU_LOAD);

        // Get the number of CPU cores that exhbit some load
        int loadedCpusOfThisTc = loadedCores.size();

        // No need to react
        if (loadedCpusOfThisTc == 0) {
            // Store it so that we can compare in the future
            monitoringService.updateCpuLoadOfTrafficClass(scId, tcId, (float) 0);

            log.debug("[{}] \t Traffic class {}: 0 overloaded cores", label(), tcId);
            return;
        }

        for (Map.Entry<Integer, Float> entry : loadedCores.entrySet()) {
            // The physical CPU core number that exhibits some load
            int cpu = entry.getKey().intValue();
            // The amount of load of this CPU core
            float load = entry.getValue().floatValue();

            log.info(
                "[{}] \t Traffic class {}: CPU Core {} ---> Load {}%",
                label(), tcId, cpu, (load * 100)
            );

            // Update with the new one
            monitoringService.updateCpuLoadOfTrafficClass(scId, tcId, load);

            // We let the agent handle imbalances
            if (this.autoScale || sc.isSoftwareBased()) {
                continue;
            }

            // An overload has been detected for more than one iterations
            if (load >= CPU_OVERLOAD_LIMIT) {
                // Scale out
                this.deflateLoad(
                    scId,
                    tcId,
                    deviceId,
                    cpu,
                    maxCpus
                );

                previousLoad.set(load);
                isRebalanced.set(true);

                return;
            // We are at a low load level for the first time
            } else if ((load <= CPU_UNDERLOAD_LIMIT) &&
                       (previousLoad.get() >= CPU_UNDERLOAD_LIMIT)) {
                // Scale in
                this.inflateLoad(
                    scId,
                    tcId,
                    deviceId,
                    cpu,
                    maxCpus
                );

                previousLoad.set(load);
                isRebalanced.set(true);

                return;
            }

            // Update the previous load any way
            previousLoad.set(load);
        }
    }

    @Override
    public void deflateLoad(
            ServiceChainId scId,
            URI            tcId,
            DeviceId       deviceId,
            int            overLoadedCpu,
            int            maxCpus) {
        log.info("");
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] \t Deflate load on traffic class group {}: Core {}", label(), tcId, overLoadedCpu);

        // We want one additional CPU core
        int initialCores = taggingService.getNumberOfActiveCoresOfTrafficClassGroup(tcId);
        int cpuCoresToAllocate = initialCores + 1;
        if (cpuCoresToAllocate > maxCpus) {
            log.warn("[{}] \t Not enough cores to further deflate this traffic class group", label());
            log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
            log.info("");
            return;
        }

        log.info(
            "[{}] \t Initial # of CPU Cores {} --> New # of CPU cores {}",
            label(), initialCores, cpuCoresToAllocate
        );

        /**
         * Measure the time it takes to compute the reconfiguration.
         */
        // START
        WallClockNanoTimestamp startReconf = new WallClockNanoTimestamp();

        // Fetch the overloaded device object
        RestServerSBDevice device = (RestServerSBDevice) topologyService.getDevice(deviceId);

        // Fetch the dataplane tree of this service chain
        NfvDataplaneTreeInterface tree = serviceChainService.runnableServiceChainWithTrafficClass(
            scId, tcId
        );

        // Perform the deflation
        Pair<RxFilterValue, Set<TrafficClassInterface>> changes = taggingService.deflateTrafficClassGroup(tcId);

        // Check what is sent back
        if (changes == null) {
            log.warn("[{}] \t {}", label(), taggingService.getLbStatusOfTrafficClassGroup(tcId));
            return;
        }

        // The Tag Manager sent us the affected traffic classes along with their new tag
        RxFilterValue changedTag = changes.getKey();
        Set<TrafficClassInterface> changedTcs = changes.getValue();
        RxFilter filterMechanism = taggingService.getTaggingMechanismOfTrafficClassGroup(tcId);

        // Get useful information about the switch that will host the new rules
        DeviceId  swId = tree.pathEstablisher().offloaderSwitchId();
        long swOutPort = tree.pathEstablisher().offloaderSwitchMetronPort();

        // Compute the new rules
        Set<FlowRule> newRules = NfvDataplaneTree.convertTrafficClassSetToOpenFlowRules(
            changedTcs, tcId, deployerService.applicationId(), swId,
            (long) -1, (long) -1, swOutPort,
            filterMechanism, changedTag, true
        );

        // STOP
        WallClockNanoTimestamp endReconf = new WallClockNanoTimestamp();

        // Send the reconfiguration delay to the monitoring service
        float reconfDelay = (float) (endReconf.unixTimestamp() - startReconf.unixTimestamp());
        monitoringService.updateGlobalReconfigurationDelayOfServiceChain(scId, reconfDelay);

        // Update the number of active CPU cores of the device
        monitoringService.addActiveCoresToDevice(deviceId, 1);

        // Reconfigure the Metron agent and the necessary network elements
        this.doReconfiguration(deviceId, scId, tcId, newRules, cpuCoresToAllocate);

        // Update the rules of this traffic class
        this.updateRulesOfTrafficClass(tree, tcId, newRules);

        log.info("[{}] \t Done", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("");
    }

    @Override
    public void inflateLoad(
            ServiceChainId scId,
            URI            tcId,
            DeviceId       deviceId,
            int            underLoadedCpu,
            int            maxCpus) {
        log.info("");
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] \t Inflate load on traffic class group {}: Core {}", label(), tcId, underLoadedCpu);

        // We want one less CPU core
        int initialCores = taggingService.getNumberOfActiveCoresOfTrafficClassGroup(tcId);
        int cpuCoresToAllocate = initialCores - 1;
        if (cpuCoresToAllocate <= 0) {
            log.warn("[{}] \t Only once CPU core is left, cannot inflate this traffic class group", label());
            log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
            log.info("");
            return;
        }

        log.info(
            "[{}] \t Initial # of CPU Cores {} --> New # of CPU cores {}",
            label(), initialCores, cpuCoresToAllocate
        );

        /**
         * Measure the time it takes to compute the reconfiguration.
         */
        // START
        WallClockNanoTimestamp startReconf = new WallClockNanoTimestamp();

        // Fetch the underloaded device object
        RestServerSBDevice device = (RestServerSBDevice) topologyService.getDevice(deviceId);

        // Fetch the dataplane tree of this service chain
        NfvDataplaneTreeInterface tree = serviceChainService.runnableServiceChainWithTrafficClass(
            scId, tcId
        );

        // Perform the inflation
        Pair<RxFilterValue, Set<TrafficClassInterface>> changes = taggingService.inflateTrafficClassGroup(tcId);

        // Check what is sent back
        if (changes == null) {
            log.warn("[{}] \t {}", label(), taggingService.getLbStatusOfTrafficClassGroup(tcId));
            return;
        }

        // The Tag Manager sent us the affected traffic classes along with their new tag
        RxFilterValue changedTag = changes.getKey();
        Set<TrafficClassInterface> changedTcs = changes.getValue();
        RxFilter filterMechanism = taggingService.getTaggingMechanismOfTrafficClassGroup(tcId);

        // Get useful information about the switch that will host the new rules
        DeviceId  swId = tree.pathEstablisher().offloaderSwitchId();
        long swOutPort = tree.pathEstablisher().offloaderSwitchMetronPort();

        // Compute the new rules
        Set<FlowRule> newRules = NfvDataplaneTree.convertTrafficClassSetToOpenFlowRules(
            changedTcs, tcId, deployerService.applicationId(), swId,
            (long) -1, (long) -1, swOutPort,
            filterMechanism, changedTag, true
        );

        // STOP
        WallClockNanoTimestamp endReconf = new WallClockNanoTimestamp();

        // Send the reconfiguration delay to the monitoring service
        float reconfDelay = (float) (endReconf.unixTimestamp() - startReconf.unixTimestamp());
        monitoringService.updateGlobalReconfigurationDelayOfServiceChain(scId, reconfDelay);

        // Update the number of active CPU cores of the device
        monitoringService.removeActiveCoresFromDevice(deviceId, 1);

        // Reconfigure the Metron agent and the necessary network elements
        this.doReconfiguration(deviceId, scId, tcId, newRules, cpuCoresToAllocate);

        // Update the rules of this traffic class
        this.updateRulesOfTrafficClass(tree, tcId, newRules);

        log.info("[{}] \t Done", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("");
    }

    /**
     * Parse the collected CPU statistics and report the CPU cores that
     * exhibit load greater or equal than a certain threshold.
     *
     * @param cpuStats the CPU statistics to be parsed
     * @param loadThreshold the load threshold we are looking for
     * @return a map of cores with the desired load pattern
     */
    private Map<Integer, Float> getCoresWithLoadGreaterThan(
            Collection<CpuStatistics> cpuStats, float loadThreshold) {
        Map<Integer, Float> cores = new ConcurrentHashMap<Integer, Float>();

        for (CpuStatistics cs : cpuStats) {
            if (cs.load() > loadThreshold) {
                cores.put(new Integer(cs.id()), new Float(cs.load()));
            }
        }

        return cores;
    }

    /**
     * Parse the collected CPU statistics and report the CPU cores that
     * exhibit load less or equal than a certain threshold.
     *
     * @param cpuStats the CPU statistics to be parsed
     * @param loadThreshold the load threshold we are looking for
     * @return a map of cores with the desired load pattern
     */
    private Map<Integer, Float> getCoresWithLoadLessThan(
            Collection<CpuStatistics> cpuStats, float loadThreshold) {
        Map<Integer, Float> cores = new ConcurrentHashMap<Integer, Float>();

        for (CpuStatistics cs : cpuStats) {
            if (cs.load() < loadThreshold) {
                cores.put(new Integer(cs.id()), new Float(cs.load()));
            }
        }

        return cores;
    }

    /**
     * Perform a monitoring operation for a given traffic class.
     *
     * @param deviceId the device which hosts the traffic class
     * @param scId the service chain where the traffic class belongs to
     * @param tcId the traffic class to monitor
     * @return monitoring statistics for this traffic class
     */
    private MonitoringStatistics doMonitoring(DeviceId deviceId, ServiceChainId scId, URI tcId) {
        /**
         * Measure the time it takes to retrieve the monitoring.
         */
        // START
        WallClockTimestamp startMon = new WallClockTimestamp();

        // Get the statistics from this device
        MonitoringStatistics stats = topologyService.getTrafficClassMonitoringStats(
            deviceId, scId, tcId
        );

        // STOP
        WallClockTimestamp endMon = new WallClockTimestamp();

        // Compute the time difference
        float monitoringDelay = (float) (endMon.unixTimestamp() - startMon.unixTimestamp());

        // Store it
        if (monitoringService != null) {
            monitoringService.updateMonitoringDelayOfTrafficClass(scId, tcId, monitoringDelay);
        }

        return stats;
    }

    /**
     * Perform a reconfiguration of the Metron agent and the data plane for a given traffic class.
     * Update also the monitoring service.
     *
     * @param deviceId the device which hosts the traffic class
     * @param scId the service chain where the traffic class belongs to
     * @param tcId the traffic class to monitor
     * @param newRules the set of new rulues to update the data plane
     * @param cpuCoresToAllocate the new number of CPU cores to allocate
     */
    private void doReconfiguration(
            DeviceId deviceId, ServiceChainId scId, URI tcId,
            Set<FlowRule> newRules, int cpuCoresToAllocate) {
        /**
         * Measure the time it takes to enforce the decision.
         */
        WallClockNanoTimestamp startEnforc = new WallClockNanoTimestamp();

        // Ask from the topology manager to reconfigure this traffic class
        boolean status = topologyService.reconfigureTrafficClassOfServiceChain(
            deviceId, scId, tcId, null, null, cpuCoresToAllocate, -1
        );

        // Push the rules to the switch
        deployerService.updateRules(scId, newRules);

        // STOP
        WallClockNanoTimestamp endEnforc = new WallClockNanoTimestamp();

        // Compute the time difference
        float enforcDelay = (float) (endEnforc.unixTimestamp() - startEnforc.unixTimestamp());

        // Store the enforcement delay
        if (monitoringService != null) {
            monitoringService.updateEnforcementDelayOfTrafficClass(scId, tcId, enforcDelay);
        }
    }

    /**
     * Updates the rules of a service chain after a load balancing decision.
     *
     * @param dpTree the data plane tree of a service chain
     * @param tcId the ID of the traffic class to update
     * @param newRules the set of rules to be updated
     */
    private void updateRulesOfTrafficClass(NfvDataplaneTreeInterface dpTree, URI tcId, Set<FlowRule> newRules) {
        Set<FlowRule> oldRules = dpTree.hardwareConfigurationOfTrafficClass(tcId);

        Iterator<FlowRule> ruleIterator = oldRules.iterator();
        while (ruleIterator.hasNext()) {
            FlowRule rule = ruleIterator.next();

            if (newRules.contains(rule)) {
                ruleIterator.remove();
            }
        }
        oldRules.addAll(newRules);

        // Push to memory
        dpTree.setHardwareConfigurationOfTrafficClass(tcId, oldRules);
    }

    /**
     * Returns whether there are active service chains or not.
     *
     * @return boolean existence of active service chains
     */
    private boolean hasActiveServiceChains() {
        return this.activeServiceChains.size() > 0;
    }

    /**
     * Prints the active service chains.
     */
    private void printActiveServiceChains() {
        for (ServiceChainInterface sc : this.activeServiceChains) {
            log.info(
                "[{}] Service chain with ID {} is active",
                label(),
                sc.id()
            );
        }
    }

    /**
     * Returns a label with the Metron Orchestrator's identity.
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
     * The Metron Orchestrator orchestrates service chains that are in state DEPLOYED and
     * undeployes service chains in state DESTROYED.
     */
    protected class InternalServiceChainListener implements ServiceChainListenerInterface {
        @Override
        public void event(ServiceChainEvent event) {
            // Parse the event to identify the service chain and its state
            ServiceChainState state  = event.type();
            ServiceChainInterface sc = event.subject();

            // Filter out the events we do care about.
            if (this.isDeployed(state)) {
                // Add this service chain to the list of "active" service chains
                sendServiceChainToOrchestrator(sc);

                // Perform the real time management
                managerExecutor.execute(() -> {
                    manage();
                });
            } else if (this.isDestroyed(state)) {
                // A request for service chain removal has arrived
                Iterator<ServiceChainInterface> scIterator = activeServiceChains().iterator();
                while (scIterator.hasNext()) {
                    ServiceChainInterface activeSc = scIterator.next();
                    if (!activeSc.id().equals(sc.id())) {
                        continue;
                    }

                    // Initiate tear down process
                    markServiceChainAsUndeployed(sc, scIterator);
                }
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
         * Returns whether a service chain is in state DEPLOYED or not.
         *
         * @param state the state of the service chain
         * @return boolean is deployed or not
         */
        private boolean isDeployed(ServiceChainState state) {
            return isValidState(state) && (state == ServiceChainState.DEPLOYED);
        }
        /**
         * Returns whether a service chain is in state DESTROYED or not.
         *
         * @param state the state of the service chain
         * @return boolean is destroyed or not
         */
        private boolean isDestroyed(ServiceChainState state) {
            return isValidState(state) && (state == ServiceChainState.DESTROYED);
        }
    }
}
