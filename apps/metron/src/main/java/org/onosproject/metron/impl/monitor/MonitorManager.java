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

package org.onosproject.metron.impl.monitor;

// Metron libraries
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.structures.LruCache;

// ONOS libraries
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.drivers.server.behavior.CpuStatisticsDiscovery;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onlab.util.SharedScheduledExecutors;
import org.onlab.util.SharedScheduledExecutorService;

// Apache libraries
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Other libraries
import org.slf4j.Logger;

// Java libraries
import java.net.URI;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A service that keeps monitoring information about the running service chains.
 */
@Component(immediate = true)
@Service
public final class MonitorManager implements MonitorService {

    private static final Logger log = getLogger(MonitorManager.class);

    /**
     * The label of this component.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".monitor";
    private static final String COMPONET_LABEL = "Monitor Manager";

    /**
     * The application ID of this component.
     */
    private ApplicationId appId = null;

    /**
     * An upper bound of monitoring entries that we keep in memory.
     * After this bound is met, we replace an entry using an LRU policy.
     */
    private static final int MAX_DELAY_ENTRIES = 500;
    private static final int MAX_LOAD_ENTRIES  = 500;
    private static final float MINIMUM_MILLI_SEC_DELAY = (float) 0.5;

    /**
     * Frequency at which we poll the NFV servers for CPU statistics.
     */
    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 2;

    /**
     * Schedulers.
     */
    private ScheduledFuture<?> scheduledTask;

    private final SharedScheduledExecutorService cpuStatisticsExecutor =
            SharedScheduledExecutors.getPoolThreadExecutor();

    /******************************* Monitoring data structures. *****************************/

    /**
     * Measure the time it takes to compute the traffic classes of a service chain.
     * The ganularity is at the level of a service chain.
     */
    private Map<ServiceChainId, Float> synthesisDelayMap  = null;
    /**
     * Measure the time it takes to retrieve monitoring statistics
     * from each traffic class of each service chain.
     * This measures the delay between the controller and the NFV agent.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Float>>> monitoringDelayMap = null;
    /**
     * Measure the time it takes to translate each traffic class of each service chain
     * into hardware (i.e., OpenFlow) rules.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Float>>> offloadingComputationDelayMap = null;
    /**
     * Measure the time it takes to install the computed hardware (i.e., OpenFlow) rules.
     * The granularity here is at level of a service chain (not traffic class.)
     */
    private Map<ServiceChainId, LruCache<Float>> offloadingInstallationDelayMap = null;
    /**
     * Measure the time it takes to launch a service chain at the dataplane.
     * This is the time it takes to the NFV agent to spawn a new Metron slave.
     */
    private Map<ServiceChainId, Map<URI, Long>> launchDelayMap = null;
    /**
     * Measure the time it takes to reconfigure one or more
     * traffic classes of each service chain using a fast data plane strategy.
     * This measures the reaction time of an NFV agent in case of a load imbalance.
     * The reaction is simple as it only involves adding/removing CPU cores.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Float>>> agentReconfigurationDelayMap = null;
    /**
     * Measure the time it takes to reconfigure one or more
     * traffic classes of each service chain using a fine-graied control plane strategy.
     * This measures the reaction time of an NFV agent in case of a load imbalance, when the
     * controller is involved by splitting/merging traffic classes of a service chain.
     */
    private Map<ServiceChainId, LruCache<Float>> globalReconfigurationDelayMap = null;
    /**
     * Measure the time it takes to enforce a reconfiguration to the dataplane.
     * This is also part of the reaction time in case of a loab imbalance.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Float>>> enforcementDelayMap = null;
    /**
     * Measure the CPU utilization of each traffic class of each service chain.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Float>>> cpuLoadMap = null;
    /**
     * Measure the per CPU core number of cache misses of each traffic class of each service chain.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Integer>>> cacheMissesMap = null;
    /**
     * Measure the number of dropped packets of each traffic class of each service chain.
     */
    private Map<ServiceChainId, Map<URI, LruCache<Integer>>> droppedPacketsMap;
    /**
     * Report the number of active CPU cores per device.
     */
    private Map<DeviceId, LruCache<Integer>> activeCoresPerDeviceMap;
    /**
     * Report the CPU load per core per device.
     */
    private Map<DeviceId, Map<Integer, LruCache<Float>>> cpuLoadPerDeviceMap;

    /*************************** End of Monitoring data structures. **************************/

    /**
     * Interact with ONOS core.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    public MonitorManager() {
        this.synthesisDelayMap  = new ConcurrentHashMap<ServiceChainId, Float>();
        this.monitoringDelayMap = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Float>>>();
        this.offloadingComputationDelayMap  = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Float>>>();
        this.offloadingInstallationDelayMap = new ConcurrentHashMap<ServiceChainId, LruCache<Float>>();
        this.launchDelayMap = new ConcurrentHashMap<ServiceChainId, Map<URI, Long>>();
        this.agentReconfigurationDelayMap = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Float>>>();
        this.globalReconfigurationDelayMap = new ConcurrentHashMap<ServiceChainId, LruCache<Float>>();
        this.enforcementDelayMap = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Float>>>();

        this.cpuLoadMap = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Float>>>();
        this.cacheMissesMap = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Integer>>>();
        this.droppedPacketsMap = new ConcurrentHashMap<ServiceChainId, Map<URI, LruCache<Integer>>>();

        this.activeCoresPerDeviceMap = new ConcurrentHashMap<DeviceId, LruCache<Integer>>();
        this.cpuLoadPerDeviceMap = new ConcurrentHashMap<DeviceId, Map<Integer, LruCache<Float>>>();
    }

    @Activate
    protected void activate() {
        // Register the Monitor with the core.
        this.appId = coreService.registerApplication(APP_NAME);

        // Schedule a polling thread to retrieve CPU statistics
        scheduledTask = scheduleCpuStatsPolling();

        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        scheduledTask.cancel(true);

        log.info("[{}] Stopped", this.label());
    }

    private ScheduledFuture scheduleCpuStatsPolling() {
        return cpuStatisticsExecutor.scheduleAtFixedRate(this::executeCpuStatisticsUpdate,
                                                          DEFAULT_POLL_FREQUENCY_SECONDS / 2,
                                                          DEFAULT_POLL_FREQUENCY_SECONDS,
                                                          TimeUnit.SECONDS);
    }

    private void executeCpuStatisticsUpdate() {
        for (Device device : this.deviceService.getDevices()) {
            this.updateCpuStatistics(device);
        }
    }

    private void updateCpuStatistics(Device device) {
        checkNotNull(device, "Device cannot be null");
        DeviceId deviceId = device.id();

        if (device.is(CpuStatisticsDiscovery.class)) {
            CpuStatisticsDiscovery cpuStatisticsDiscovery = device.as(CpuStatisticsDiscovery.class);
            Collection<CpuStatistics> cpuStatistics = cpuStatisticsDiscovery.discoverCpuStatistics();
            if (cpuStatistics != null && !cpuStatistics.isEmpty()) {
                for (CpuStatistics cs : cpuStatistics) {
                    this.updateCpuLoadOfDeviceOfCore(deviceId, cs.id(), cs.load());
                }
            }
        } else {
            log.debug("No port statistics getter behaviour for device {}", deviceId);
        }
    }

    /*********************************** Synthesis Delay. **********************************/

    @Override
    public Map<ServiceChainId, Float> synthesisDelayMap() {
        return this.synthesisDelayMap;
    }

    @Override
    public float synthesisDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.synthesisDelayMap.containsKey(scId)) {
            return -1;
        }

        return this.synthesisDelayMap.get(scId).longValue();
    }

    @Override
    public void addSynthesisDelayOfServiceChain(ServiceChainId scId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative synthesis delay");

        if (delay == 0) {
            // Since there is no sub-millisecond granularity, we consider this delay as a middleground.
            delay = MINIMUM_MILLI_SEC_DELAY;
        }

        this.synthesisDelayMap.put(scId, new Float(delay));
    }

    /********************************** Monitoring Delay. **********************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Float>>> monitoringDelayMap() {
        return this.monitoringDelayMap;
    }

    @Override
    public Map<URI, LruCache<Float>> monitoringDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.monitoringDelayMap.containsKey(scId)) {
            return null;
        }

        return this.monitoringDelayMap.get(scId);
    }

    @Override
    public LruCache<Float> monitoringDelayOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.monitoringDelayMap.containsKey(scId)) {
            return null;
        }

        if (!this.monitoringDelayMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.monitoringDelayMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Float> monitoringDelayOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.monitoringDelayMap.keySet()) {
            // Not here
            if (!this.monitoringDelayMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.monitoringDelayMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateMonitoringDelayOfTrafficClass(ServiceChainId scId, URI tcId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative monitoring delay");

        if (delay == 0) {
            // Since there is no sub-millisecond granularity, we consider this delay as a middleground.
            delay = MINIMUM_MILLI_SEC_DELAY;
        }

        Map<URI, LruCache<Float>> scDelay = null;
        if (!this.monitoringDelayMap.containsKey(scId)) {
            scDelay = new ConcurrentHashMap<URI, LruCache<Float>>();
        } else {
            scDelay = this.monitoringDelayMap.get(scId);
        }

        LruCache<Float> delayList = null;
        if (!scDelay.containsKey(tcId)) {
            delayList = new LruCache<Float>(MAX_DELAY_ENTRIES);
        } else {
            delayList = scDelay.get(tcId);
        }

        // Add this entry to the LRU
        delayList.add(new Float(delay));

        // Map the list to its traffic class
        scDelay.put(tcId, delayList);

        // Finally, update the entry for this service chain
        this.monitoringDelayMap.put(scId, scDelay);
    }

    /**************************** Offloading Computation Delay. ****************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Float>>> offloadingComputationDelayMap() {
        return this.offloadingComputationDelayMap;
    }

    @Override
    public Map<URI, LruCache<Float>> offloadingComputationDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.offloadingComputationDelayMap.containsKey(scId)) {
            return null;
        }

        return this.offloadingComputationDelayMap.get(scId);
    }

    @Override
    public LruCache<Float> offloadingComputationDelayOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.offloadingComputationDelayMap.containsKey(scId)) {
            return null;
        }

        if (!this.offloadingComputationDelayMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.offloadingComputationDelayMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Float> offloadingComputationDelayOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.offloadingComputationDelayMap.keySet()) {
            // Not here
            if (!this.offloadingComputationDelayMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.offloadingComputationDelayMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateOffloadingComputationDelayOfTrafficClass(
            ServiceChainId scId, URI tcId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative computation delay");

        if (delay == 0) {
            // Since there is no sub-millisecond granularity, we consider this delay as a middleground.
            delay = MINIMUM_MILLI_SEC_DELAY;
        }

        Map<URI, LruCache<Float>> scDelay = null;
        if (!this.offloadingComputationDelayMap.containsKey(scId)) {
            scDelay = new ConcurrentHashMap<URI, LruCache<Float>>();
        } else {
            scDelay = this.offloadingComputationDelayMap.get(scId);
        }

        LruCache<Float> delayList = null;
        if (!scDelay.containsKey(tcId)) {
            delayList = new LruCache<Float>(MAX_DELAY_ENTRIES);
        } else {
            delayList = scDelay.get(tcId);
        }

        // Add this entry to the LRU
        delayList.add(new Float(delay));

        // Map the list to its traffic class
        scDelay.put(tcId, delayList);

        // Finally, update the entry for this service chain
        this.offloadingComputationDelayMap.put(scId, scDelay);
    }

    /**************************** Offloading Installation Delay. ***************************/

    @Override
    public Map<ServiceChainId, LruCache<Float>> offloadingInstallationDelayMap() {
        return this.offloadingInstallationDelayMap;
    }

    @Override
    public LruCache<Float> offloadingInstallationDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.offloadingInstallationDelayMap.containsKey(scId)) {
            return null;
        }

        return this.offloadingInstallationDelayMap.get(scId);
    }

    @Override
    public void updateOffloadingInstallationDelay(ServiceChainId scId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative installation delay");

        if (delay == 0) {
            // Since there is no sub-millisecond granularity, we consider this delay as a middleground.
            delay = MINIMUM_MILLI_SEC_DELAY;
        }

        LruCache<Float> delayList = null;
        if (!this.offloadingInstallationDelayMap.containsKey(scId)) {
            delayList = new LruCache<Float>(MAX_DELAY_ENTRIES);
        } else {
            delayList = this.offloadingInstallationDelayMap.get(scId);
        }

        // Add this entry to the LRU
        delayList.add(new Float(delay));

        // Finally, update the entry for this service chain
        this.offloadingInstallationDelayMap.put(scId, delayList);
    }

    /************************************ Launch Delay. ************************************/

    @Override
    public Map<ServiceChainId, Map<URI, Long>> launchDelayMap() {
        return this.launchDelayMap;
    }

    @Override
    public Map<URI, Long> launchDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.launchDelayMap.containsKey(scId)) {
            return null;
        }

        return this.launchDelayMap.get(scId);
    }

    @Override
    public long launchDelayOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.launchDelayMap.containsKey(scId)) {
            return -1;
        }

        if (!this.launchDelayMap.get(scId).containsKey(tcId)) {
            return -1;
        }

        return this.launchDelayMap.get(scId).get(tcId).longValue();
    }

    @Override
    public long launchDelayOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.launchDelayMap.keySet()) {
            // Not here
            if (!this.launchDelayMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.launchDelayMap.get(scId).get(tcId).longValue();
        }

        return -1;
    }

    @Override
    public void updateLaunchDelayOfTrafficClass(ServiceChainId scId, URI tcId, long delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative delay to launch traffic class " + tcId);

        Map<URI, Long> scDelay = null;
        if (!this.launchDelayMap.containsKey(scId)) {
            scDelay = new ConcurrentHashMap<URI, Long>();
        } else {
            scDelay = this.launchDelayMap.get(scId);
        }

        // Add this delay
        scDelay.put(tcId, new Long(delay));

        // Finally, update the entry for this service chain
        this.launchDelayMap.put(scId, scDelay);
    }

    /************************** Data plane Reconfiguration Delay. **************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Float>>> agentReconfigurationDelayMap() {
        return this.agentReconfigurationDelayMap;
    }

    @Override
    public Map<URI, LruCache<Float>> agentReconfigurationDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.agentReconfigurationDelayMap.containsKey(scId)) {
            return null;
        }

        return this.agentReconfigurationDelayMap.get(scId);
    }

    @Override
    public LruCache<Float> agentReconfigurationDelayOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.agentReconfigurationDelayMap.containsKey(scId)) {
            return null;
        }

        if (!this.agentReconfigurationDelayMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.agentReconfigurationDelayMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Float> agentReconfigurationDelayOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.agentReconfigurationDelayMap.keySet()) {
            // Not here
            if (!this.agentReconfigurationDelayMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.agentReconfigurationDelayMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateAgentReconfigurationDelayOfTrafficClass(ServiceChainId scId, URI tcId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative local reconfiguration delay");

        Map<URI, LruCache<Float>> scDelay = null;
        if (!this.agentReconfigurationDelayMap.containsKey(scId)) {
            scDelay = new ConcurrentHashMap<URI, LruCache<Float>>();
        } else {
            scDelay = this.agentReconfigurationDelayMap.get(scId);
        }

        LruCache<Float> delayList = null;
        if (!scDelay.containsKey(tcId)) {
            delayList = new LruCache<Float>(MAX_DELAY_ENTRIES);
        } else {
            delayList = scDelay.get(tcId);
        }

        // Add this entry to the LRU
        delayList.add(new Float(delay));

        // Map the list to its traffic class
        scDelay.put(tcId, delayList);

        // Finally, update the entry for this service chain
        this.agentReconfigurationDelayMap.put(scId, scDelay);
    }

    /**************************** Global Reconfiguration Delay. ****************************/

    @Override
    public Map<ServiceChainId, LruCache<Float>> globalReconfigurationDelayMap() {
        return this.globalReconfigurationDelayMap;
    }

    @Override
    public LruCache<Float> globalReconfigurationDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.globalReconfigurationDelayMap.containsKey(scId)) {
            return null;
        }

        return this.globalReconfigurationDelayMap.get(scId);
    }

    @Override
    public void updateGlobalReconfigurationDelayOfServiceChain(ServiceChainId scId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative global reconfiguration delay");

        LruCache<Float> delayList = null;
        if (!this.globalReconfigurationDelayMap.containsKey(scId)) {
            delayList = new LruCache<Float>(MAX_DELAY_ENTRIES);
        } else {
            delayList = this.globalReconfigurationDelayMap.get(scId);
        }

        // Add this entry to the LRU
        delayList.add(new Float(delay));

        // Finally, update the entry for this service chain
        this.globalReconfigurationDelayMap.put(scId, delayList);
    }

    /********************************* Enforcement Delay. **********************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Float>>> enforcementDelayMap() {
        return this.enforcementDelayMap;
    }

    @Override
    public Map<URI, LruCache<Float>> enforcementDelayOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.enforcementDelayMap.containsKey(scId)) {
            return null;
        }

        return this.enforcementDelayMap.get(scId);
    }

    @Override
    public LruCache<Float> enforcementDelayOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.enforcementDelayMap.containsKey(scId)) {
            return null;
        }

        if (!this.enforcementDelayMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.enforcementDelayMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Float> enforcementDelayOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.enforcementDelayMap.keySet()) {
            // Not here
            if (!this.enforcementDelayMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.enforcementDelayMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateEnforcementDelayOfTrafficClass(ServiceChainId scId, URI tcId, float delay) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(delay >= 0, "[" + this.label() + "] Negative enforcement delay");

        Map<URI, LruCache<Float>> scDelay = null;
        if (!this.enforcementDelayMap.containsKey(scId)) {
            scDelay = new ConcurrentHashMap<URI, LruCache<Float>>();
        } else {
            scDelay = this.enforcementDelayMap.get(scId);
        }

        LruCache<Float> delayList = null;
        if (!scDelay.containsKey(tcId)) {
            delayList = new LruCache<Float>(MAX_DELAY_ENTRIES);
        } else {
            delayList = scDelay.get(tcId);
        }

        // Add this entry to the LRU
        delayList.add(new Float(delay));

        // Map the list to its traffic class
        scDelay.put(tcId, delayList);

        // Finally, update the entry for this service chain
        this.enforcementDelayMap.put(scId, scDelay);
    }

    /************************** CPU Utilization per Service Chain. *************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Float>>> cpuLoadMap() {
        return this.cpuLoadMap;
    }

    @Override
    public Map<URI, LruCache<Float>> cpuLoadOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.cpuLoadMap.containsKey(scId)) {
            return null;
        }

        return this.cpuLoadMap.get(scId);
    }

    @Override
    public LruCache<Float> cpuLoadOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.cpuLoadMap.containsKey(scId)) {
            return null;
        }

        if (!this.cpuLoadMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.cpuLoadMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Float> cpuLoadOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.cpuLoadMap.keySet()) {
            // Not here
            if (!this.cpuLoadMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.cpuLoadMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateCpuLoadOfTrafficClass(ServiceChainId scId, URI tcId, float load) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(load >= 0, "[" + this.label() + "] Negative CPU utilization");

        Map<URI, LruCache<Float>> scLoad = null;
        if (!this.cpuLoadMap.containsKey(scId)) {
            scLoad = new ConcurrentHashMap<URI, LruCache<Float>>();
        } else {
            scLoad = this.cpuLoadMap.get(scId);
        }

        LruCache<Float> loadList = null;
        if (!scLoad.containsKey(tcId)) {
            loadList = new LruCache<Float>(MAX_LOAD_ENTRIES);
        } else {
            loadList = scLoad.get(tcId);
        }

        // Add this entry to the LRU
        // loadList.put(new Integer(loadList.size()), new Float(load));
        loadList.add(new Float(load));

        // Map the list to its traffic class
        scLoad.put(tcId, loadList);

        // Finally, update the entry for this service chain
        this.cpuLoadMap.put(scId, scLoad);
    }

    /******************************** CPU Cache Utilization. *******************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Integer>>> cacheMissesMap() {
        return this.cacheMissesMap;
    }

    @Override
    public Map<URI, LruCache<Integer>> cacheMissesOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.cacheMissesMap.containsKey(scId)) {
            return null;
        }

        return this.cacheMissesMap.get(scId);
    }

    @Override
    public LruCache<Integer> cacheMissesOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.cacheMissesMap.containsKey(scId)) {
            return null;
        }

        if (!this.cacheMissesMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.cacheMissesMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Integer> cacheMissesOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.cacheMissesMap.keySet()) {
            // Not here
            if (!this.cacheMissesMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.cacheMissesMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateCacheMissesOfTrafficClass(ServiceChainId scId, URI tcId, int misses) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(misses >= 0, "[" + this.label() + "] Negative number of CPU cache misses");

        Map<URI, LruCache<Integer>> scMisses = null;
        if (!this.cacheMissesMap.containsKey(scId)) {
            scMisses = new ConcurrentHashMap<URI, LruCache<Integer>>();
        } else {
            scMisses = this.cacheMissesMap.get(scId);
        }

        LruCache<Integer> missesList = null;
        if (!scMisses.containsKey(tcId)) {
            missesList = new LruCache<Integer>(MAX_LOAD_ENTRIES);
        } else {
            missesList = scMisses.get(tcId);
        }

        // Add this entry to the LRU
        missesList.add(new Integer(misses));

        // Map the list to its traffic class
        scMisses.put(tcId, missesList);

        // Finally, update the entry for this service chain
        this.cacheMissesMap.put(scId, scMisses);
    }

    /************************************ Packet Drops. ************************************/

    @Override
    public Map<ServiceChainId, Map<URI, LruCache<Integer>>> droppedPacketsMap() {
        return this.droppedPacketsMap;
    }

    @Override
    public Map<URI, LruCache<Integer>> droppedPacketsOfServiceChain(ServiceChainId scId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");

        if (!this.droppedPacketsMap.containsKey(scId)) {
            return null;
        }

        return this.droppedPacketsMap.get(scId);
    }

    @Override
    public LruCache<Integer> droppedPacketsOfTrafficClass(ServiceChainId scId, URI tcId) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        if (!this.droppedPacketsMap.containsKey(scId)) {
            return null;
        }

        if (!this.droppedPacketsMap.get(scId).containsKey(tcId)) {
            return null;
        }

        return this.droppedPacketsMap.get(scId).get(tcId);
    }

    @Override
    public LruCache<Integer> droppedPacketsOfTrafficClass(URI tcId) {
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");

        // The service chain is not specified, we need to search all of them
        for (ServiceChainId scId : this.droppedPacketsMap.keySet()) {
            // Not here
            if (!this.droppedPacketsMap.get(scId).containsKey(tcId)) {
                continue;
            }

            // Got it!
            return this.droppedPacketsMap.get(scId).get(tcId);
        }

        return null;
    }

    @Override
    public void updateDroppedPacketsOfTrafficClass(ServiceChainId scId, URI tcId, int drops) {
        checkNotNull(scId, "[" + this.label() + "] NULL service chain ID");
        checkNotNull(tcId, "[" + this.label() + "] NULL traffic class ID");
        checkArgument(drops >= 0, "[" + this.label() + "] Negative number of packet drops");

        Map<URI, LruCache<Integer>> scDrops = null;
        if (!this.droppedPacketsMap.containsKey(scId)) {
            scDrops = new ConcurrentHashMap<URI, LruCache<Integer>>();
        } else {
            scDrops = this.droppedPacketsMap.get(scId);
        }

        LruCache<Integer> dropsList = null;
        if (!scDrops.containsKey(tcId)) {
            dropsList = new LruCache<Integer>(MAX_LOAD_ENTRIES);
        } else {
            dropsList = scDrops.get(tcId);
        }

        // Add this entry to the LRU
        dropsList.add(new Integer(drops));

        // Map the list to its traffic class
        scDrops.put(tcId, dropsList);

        // Finally, update the entry for this service chain
        this.droppedPacketsMap.put(scId, scDrops);
    }

    /******************************* Active CPU Cores/Device. ******************************/

    @Override
    public Map<DeviceId, LruCache<Integer>> activeCoresPerDeviceMap() {
        return this.activeCoresPerDeviceMap;
    }

    @Override
    public LruCache<Integer> activeCoresOfDevice(DeviceId deviceId) {
        checkNotNull(deviceId, "[" + this.label() + "] NULL device ID");

        if (!this.activeCoresPerDeviceMap.containsKey(deviceId)) {
            return null;
        }

        return this.activeCoresPerDeviceMap.get(deviceId);
    }

    @Override
    public void addActiveCoresToDevice(DeviceId deviceId, int cores) {
        checkNotNull(deviceId, "[" + this.label() + "] NULL device ID");
        checkArgument(cores >= 0, "[" + this.label() + "] Negative number of active CPU cores");

        LruCache<Integer> scCores = null;

        // First entry
        if (!this.activeCoresPerDeviceMap.containsKey(deviceId)) {
            scCores = new LruCache<Integer>(MAX_LOAD_ENTRIES);

            // Add this first entry to the LRU
            scCores.add(new Integer(cores));
        } else {
            // Retrieve the LRU
            scCores = this.activeCoresPerDeviceMap.get(deviceId);
            // Get the current number of active CPU cores
            int currCores = scCores.getLastValue().intValue();
            // Update
            scCores.add(new Integer(currCores + cores));
        }

        // Finally, update the entry for this service chain
        this.activeCoresPerDeviceMap.put(deviceId, scCores);
    }

    @Override
    public void removeActiveCoresFromDevice(DeviceId deviceId, int cores) {
        checkNotNull(deviceId, "[" + this.label() + "] NULL device ID");
        checkArgument(cores >= 0, "[" + this.label() + "] Negative number of active CPU cores");

        LruCache<Integer> scCores = null;

        // First entry
        if (!this.activeCoresPerDeviceMap.containsKey(deviceId)) {
            log.error("Cannot remove active CPU cores from device {} as it has no active CPU cores.", deviceId);
            return;
        }

        // Retrieve the LRU
        scCores = this.activeCoresPerDeviceMap.get(deviceId);
        // Get the current number of active CPU cores
        int currCores = scCores.getLastValue().intValue();

        if (cores > currCores) {
            log.error(
                "Cannot remove {} active CPU cores from device {} as it has only {} CPU cores.",
                cores, deviceId, currCores
            );
            return;
        }

        // Update
        scCores.add(new Integer(currCores - cores));

        // Finally, update the entry for this service chain
        this.activeCoresPerDeviceMap.put(deviceId, scCores);
    }

    /****************************** CPU Utilization per Device. ****************************/

    @Override
    public Map<DeviceId, Map<Integer, LruCache<Float>>> cpuLoadPerDeviceMap() {
        return this.cpuLoadPerDeviceMap;
    }

    @Override
    public Map<Integer, LruCache<Float>> cpuLoadOfDevice(DeviceId deviceId) {
        checkNotNull(deviceId, "[" + this.label() + "] NULL device ID");

        if (!this.cpuLoadPerDeviceMap.containsKey(deviceId)) {
            return null;
        }

        return this.cpuLoadPerDeviceMap.get(deviceId);
    }

    @Override
    public LruCache<Float> cpuLoadOfDeviceOfCore(DeviceId deviceId, int cpuId) {
        checkNotNull(deviceId, "[" + this.label() + "] NULL device ID");
        checkArgument(cpuId >= 0, "[" + this.label() + "] Negative CPU ID");

        if (!this.cpuLoadPerDeviceMap.containsKey(deviceId)) {
            return null;
        }

        if (!this.cpuLoadPerDeviceMap.get(deviceId).containsKey(cpuId)) {
            return null;
        }

        return this.cpuLoadPerDeviceMap.get(deviceId).get(new Integer(cpuId));
    }

    @Override
    public void updateCpuLoadOfDeviceOfCore(DeviceId deviceId, int cpuId, float load) {
        checkNotNull(deviceId, "[" + this.label() + "] NULL device ID");
        checkArgument(cpuId >= 0, "[" + this.label() + "] Negative CPU ID");
        checkArgument(load >= 0, "[" + this.label() + "] Negative CPU utilization");

        Integer cpuI = new Integer(cpuId);

        Map<Integer, LruCache<Float>> scLoad = null;
        if (!this.cpuLoadPerDeviceMap.containsKey(deviceId)) {
            scLoad = new ConcurrentHashMap<Integer, LruCache<Float>>();
        } else {
            scLoad = this.cpuLoadPerDeviceMap.get(deviceId);
        }

        LruCache<Float> loadList = null;
        if (!scLoad.containsKey(cpuI)) {
            loadList = new LruCache<Float>(MAX_LOAD_ENTRIES);
        } else {
            loadList = scLoad.get(cpuI);
        }

        // Add this entry to the LRU
        loadList.add(new Float(load));

        // Map the list to its traffic class
        scLoad.put(cpuI, loadList);

        // Finally, update the entry for this device
        this.cpuLoadPerDeviceMap.put(deviceId, scLoad);
    }

    /********************************* End of Monitoring. ***********************************/

    /**
     * Returns a label with the module's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}
