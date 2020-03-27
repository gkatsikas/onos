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

package org.onosproject.metron.orchestrator;

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

import org.onosproject.metron.dataplane.NfvDataplaneTree;

// ONOS libraries
import org.onosproject.cfg.ComponentConfigService;
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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onlab.util.Tools;
import org.osgi.service.component.ComponentContext;

// Guava
import com.google.common.collect.Sets;

// Other libraries
import org.slf4j.Logger;

// Java libraries
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.max;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.util.Tools.groupedThreads;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * A service that undertakes to deploy and manage Metron service chains.
 */
@Component(immediate = true)
@Service
public final class OrchestrationManager implements OrchestrationService {

    /**
     * Record the last time a CPU core was checked for load imbalance.
     */
    private class CpuRuntimeInfo {
        public CpuRuntimeInfo() {
            lastResched = null;
        }

        WallClockTimestamp lastResched;
        ReentrantLock lock;
    }

    private static final Logger log = getLogger(OrchestrationManager.class);

    /**
     * Application ID for the Metron Orchestrator.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".orchestrator";
    private static final String COMPONET_LABEL = "Metron Orchestrator";

    /**
     * Some CPU thresholds used for load balancing.
     */
    private static final float CPU_LOAD_ZERO = (float) 0.0;
    private static final float CPU_MINIMUM_PRINTABLE_LIMIT = (float) 0.01;
    private static final int CPU_MIN_RESCHED_MS = 1000;

    /**
     * Members:
     * |-> An application ID is necessary to register with the core.
     * |-> A set of already deployed service chains (active).
     * |-> A set of suspended service chains (undeployed due to some issue).
     */
    private ApplicationId appId = null;
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
    private static final int MANAGER_THREADS_NO = 8;
    private final ExecutorService managerExecutor = newFixedThreadPool(MANAGER_THREADS_NO,
        groupedThreads(this.getClass().getSimpleName(), "sc-orchestrator", log));

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
    protected ComponentConfigService cfgService;

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

    /** The CPU utilization threshold to perform scale down. */
    private static final String SCALE_DOWN_LOAD_THRESHOLD = "scaleDownLoadThreshold";
    private static final float SCALE_DOWN_LOAD_THRESHOLD_DEFAULT = 0.25f;
    @Property(name = SCALE_DOWN_LOAD_THRESHOLD, floatValue = SCALE_DOWN_LOAD_THRESHOLD_DEFAULT,
             label = "Configure the amount of CPU load to trigger scale down events; " +
                    "default is 25% CPU core utilization")
    private float scaleDownLoadThreshold = SCALE_DOWN_LOAD_THRESHOLD_DEFAULT;

    /** The CPU utilization threshold to perform scale up. */
    private static final String SCALE_UP_LOAD_THRESHOLD = "scaleUpLoadThreshold";
    private static final float SCALE_UP_LOAD_THRESHOLD_DEFAULT = 0.75f;
    @Property(name = SCALE_UP_LOAD_THRESHOLD, floatValue = SCALE_UP_LOAD_THRESHOLD_DEFAULT,
             label = "Configure the amount of CPU load to trigger scale up events; " +
                    "default is 75% CPU core utilization")
    private float scaleUpLoadThreshold = SCALE_UP_LOAD_THRESHOLD_DEFAULT;

    /** The frequency of the Orchestrator's monitoring in milliseconds. */
    private static final String MONITORING_PERIOD_MS = "monitoringPeriodMilli";
    private static final int MONITORING_PERIOD_MS_DEFAULT = 100;
    @Property(name = MONITORING_PERIOD_MS, intValue = MONITORING_PERIOD_MS_DEFAULT,
             label = "Configure the data plane monitoring frequency (in milliseconds); " +
                    "default is 100 ms")
    private int monitoringPeriodMilli = MONITORING_PERIOD_MS_DEFAULT;

    public OrchestrationManager() {
        this.activeServiceChains    = Sets.<ServiceChainInterface>newConcurrentHashSet();
        this.suspendedServiceChains = Sets.<ServiceChainInterface>newConcurrentHashSet();
    }

    @Activate
    protected void activate() {
        // Register the Metron Orchestrator with the core
        this.appId = coreService.registerApplication(APP_NAME);

        // Catch events coming from the service chain manager
        serviceChainService.addListener(serviceChainListener);

        // Configuration service is up
        cfgService.registerProperties(getClass());

        log.info("[{}] Started", label());
    }

    @Modified
    public void modified(ComponentContext context) {
        this.readComponentConfiguration(context);
    }

    @Deactivate
    protected void deactivate() {
        // Remove the listener for the events coming from the service chain manager
        serviceChainService.removeListener(serviceChainListener);

        // Disable configuration service
        cfgService.unregisterProperties(getClass(), false);

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

        // Not already undeployed
        if (!this.suspendedServiceChains.contains(sc)) {
            // ...and add it to the set of suspended service chains
            this.suspendedServiceChains.add(sc);

            // Notify the deployer
            deployerService.markServiceChainAsUndeployed(sc, null);
        }

        // Remove this service chain from the list of active service chains
        if (scIterator != null) {
            scIterator.remove();
        }
    }

    @Override
    public void manage() {
        log.debug(Constants.STDOUT_BARS);
        log.debug("Service Chains Orchestrator - Runtime Load Balancing");
        log.debug(Constants.STDOUT_BARS);

        Map<ServiceChainId, Map<URI, Map<Integer, Float>>> previousLoad =
            new ConcurrentHashMap<ServiceChainId, Map<URI, Map<Integer, Float>>>();

        Map<ServiceChainId, Map<URI, Map<Integer, CpuRuntimeInfo>>> isRebalanced =
            new ConcurrentHashMap<>();

        boolean didSomething = false;

        while (true) {
            int sleepTime = monitoringPeriodMilli;

            MonitoringStatistics previousStats = null;
            AtomicBoolean quit = new AtomicBoolean(false);

            // Go though the active chain, monitor them, and make load balancing decisions
            Iterator<ServiceChainInterface> scIterator = this.activeServiceChains.iterator();
            while (scIterator.hasNext()) {
                ServiceChainInterface sc = scIterator.next();
                ServiceChainId scId = sc.id();

                quit.set(false);

                log.debug("[{}] Managing active service chain {}", label(), scId);

                // Allocate local resources at the service chain level
                if (!previousLoad.containsKey(scId)) {
                    previousLoad.put(scId, new ConcurrentHashMap<URI, Map<Integer, Float>>());
                }
                if (!isRebalanced.containsKey(scId)) {
                    isRebalanced.put(scId, new ConcurrentHashMap<>());
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
                    long lastResched = new WallClockTimestamp().msSince(tcInfo.lastImbalanceCheck());
                    if (lastResched < monitoringPeriodMilli) {
                        sleepTime = Math.min(monitoringPeriodMilli - (int) lastResched, sleepTime);
                    }
                    if (!tcInfo.LOCK.tryLock()) {
                        continue;
                    }
                    try {
                        // Fetch the ID of the traffic class of this service chain
                        URI tcId = tcInfo.trafficClassId();

                        // And the maximum number of CPUs that it can currently have (we can ask for more)
                        int maxCpus = tcInfo.maxNumberOfCpus();

                        // Allocate local resources at the traffic class level
                        if (!previousLoad.get(scId).containsKey(tcId)) {
                            previousLoad.get(scId).put(tcId, new ConcurrentHashMap<Integer, Float>());
                        }
                        if (!isRebalanced.get(scId).containsKey(tcId)) {
                            isRebalanced.get(scId).put(tcId, new ConcurrentHashMap<>());
                        }

                        for (DeviceId deviceId : tcInfo.devices()) {
                            if (!topologyService.isServer(deviceId)) {
                                continue;
                            }

                            if (!topologyService.getDevice(deviceId).isActive()) {
                                quit.set(true);
                                break;
                            }

                            // Get monitoring statistics from this service chain
                            MonitoringStatistics stats = this.doMonitoring(deviceId, scId, tcId);

                            // This device seems to be down or a problem suddenly occured
                            if (stats == null) {
                                quit.set(true);
                                break;
                            } else {
                                previousStats = stats;
                            }

                            Map<Integer, Float> previousLoadTc = previousLoad.get(scId).get(tcId);
                            Map<Integer, CpuRuntimeInfo> isRebalancedTc = isRebalanced.get(scId).get(tcId);

                            // Check for potential load imbalance
                            if (this.checkForLoadImbalance(
                                    sc, tcInfo, deviceId, stats, maxCpus, previousLoadTc, isRebalancedTc)) {
                                didSomething = true; //Be sure to reschedule right away
                            } else {
                                // Remember that we did the check for this TC; repeat only after some time
                                tcInfo.imbalanceCheckDone();
                            }
                        }

                        // Most likely a dead agent
                        if (quit.get()) {
                            break;
                        }
                    } finally {
                        tcInfo.LOCK.unlock();
                    }
                }

                if (quit.get()) {
                    this.markServiceChainAsUndeployed(sc, scIterator);
                }
            }

            // No need to monitor an `empty` system
            if (!this.hasActiveServiceChains()) {
                break;
            }

            // Periodic monitoring every monitoringPeriodMilli milliseconds
            try {
                if (!didSomething) {
                    Thread.sleep(sleepTime);
                }
                didSomething = false;
            } catch (InterruptedException intEx) {
                continue;
            }
        }

        log.debug(Constants.STDOUT_BARS);
    }

    private Map<Integer, Float> getActiveCores(Collection<CpuStatistics> cpuStats) {
        Map<Integer, Float> cores = new ConcurrentHashMap<Integer, Float>();

        for (CpuStatistics cs : cpuStats) {
            if (cs.busy()) {
                cores.put(cs.id(), cs.load());
            }
        }

        return cores;
    }


    /**
     * Given the statistics obtained for a traffic class, check whether
     * this traffic class exhibits overload and act accordingly.
     *
     * @param sc the overloaded service chain
     * @param tc the ID of the overloaded traffic class
     * @param deviceId the ID of the overloaded device
     * @param stats monitoring statistics for a given traffic class
     * @param maxCpus the new maximum number of CPUs you can currently have
     * @param previousLoad the load level of this traffic class's CPU cores in the previous iteration
     * @param isRebalanced indicates that this traffic class is just rebalanced
     */
    private boolean checkForLoadImbalance(
            ServiceChainInterface        sc,
            TrafficClassRuntimeInfo      tc,
            DeviceId                     deviceId,
            MonitoringStatistics         stats,
            int                          maxCpus,
            Map<Integer, Float>          previousLoad,
            Map<Integer, CpuRuntimeInfo> isRebalanced) {
        // Service chain object might be deleted anytime due to e.g., dead agent
        if ((sc == null) || (stats == null)) {
            return false;
        }

        ServiceChainId scId = sc.id();
        URI tcId = tc.trafficClassId();
        boolean autoScale = deployerService.hasAutoScale(scId, sc.autoScale());
        boolean withLimitedReconfiguration = sc.isSoftwareBased() ? true : false;

        // Get the list of CPU cores with non-zero load
        Collection<CpuStatistics> cpuStats = stats.cpuStatisticsAll();
        Map<Integer, Float> activeCores = this.getActiveCores(cpuStats);
        // Get the number of CPU cores that exhibit some load
        int activeCoresOfThisTc = activeCores.size();

        // No need to react
        if (activeCoresOfThisTc == 0) {
            // Store it so that we can compare in the future
            monitoringService.updateCpuLoadOfTrafficClass(scId, tcId, CPU_LOAD_ZERO);
            log.debug("[{}] \t Traffic class {}: 0 overloaded cores", label(), tcId);
            return false;
        }

        float totalLoad = 0;
        for (Map.Entry<Integer, Float> entry : activeCores.entrySet()) {
            float load = entry.getValue();
            totalLoad += load;
        }

        float highScaleUpLoadThreshold = (scaleUpLoadThreshold + 1.0f) / 2.0f;
        Set<Integer> overloaded = new ConcurrentSkipListSet<Integer>();
        Set<Integer> underloaded = new ConcurrentSkipListSet<Integer>();

        for (Map.Entry<Integer, Float> entry : activeCores.entrySet()) {
            // The physical CPU core number that exhibits some load
            int cpu = entry.getKey();
            // The amount of load on this CPU core
            float load = entry.getValue();

            if (!previousLoad.containsKey(cpu)) {
                previousLoad.put(cpu, CPU_LOAD_ZERO);
            }

            float pastLoad = previousLoad.get(cpu);

            // Update the previous load any way. Keep an exponential smoothed average to have a trend
            previousLoad.put(cpu, (float) (pastLoad * 0.67 + 0.33 * load));

            float futureLoad = (load - pastLoad) + load;
            float highestLoad = max(max(load, pastLoad), futureLoad);

            CpuStatistics cpuStat = null;
            for (CpuStatistics cs : cpuStats) {
                if (cs.id() == cpu) {
                    cpuStat = cs;
                }
            }
            if (cpuStat == null) {
                continue;
            }

            // Print load only if there is some
            if (load >= CPU_MINIMUM_PRINTABLE_LIMIT) {
                log.info(
                    "[{}] \t Traffic class {}: CPU Core {} ---> Load {}%, previous {}%, future {}%, busy since {}",
                    label(), tcId, cpu, load * 100, pastLoad * 100, futureLoad * 100, cpuStat.busySince()
                );
            }

            // Update with the new one
            monitoringService.updateCpuLoadOfTrafficClass(scId, tcId, load);

            // We let the agent handle imbalances by auto scaling
            if (autoScale) {
                continue;
            }

            // Keep track of when a core has seen a change in activity.
            // We only set the time when the change occurs.
            if (isRebalanced.containsKey(cpu)) {
                if (isRebalanced.get(cpu).lastResched == null) {
                    log.debug("[{}] \t Uninitialized CPU {} busy {}", label(), cpu, cpuStat.busy());
                    if ((cpuStat.busy() && (load > 0)) || (!cpuStat.busy() && (load == 0))) {
                        log.info("[{}] \t Initializing CPU {} with load {}", label(), cpu, load);
                        isRebalanced.get(cpu).lastResched = new WallClockTimestamp();
                    }
                    continue;
                } else {
                    long diff = new WallClockTimestamp().unixTimestamp() -
                                isRebalanced.get(cpu).lastResched.unixTimestamp();
                    if ((diff < CPU_MIN_RESCHED_MS) && (load < highScaleUpLoadThreshold)) {
                        log.info(
                            "[{}] \t Skipping rebalance check for traffic class {} core {} as delta is {}",
                            label(), tcId, cpu, diff
                        );
                        continue;
                    } else {
                        log.debug(
                            "[{}] \t Doing rebalance check for traffic class {} core {} as delta is {}",
                            label(), tcId, cpu, diff
                        );
                    }
                }
            } else {
                isRebalanced.put(cpu, new CpuRuntimeInfo()); // Assume it was just rescheduled
                continue;
            }

            // An overload has been detected for more than one iterations
            if ((load >= scaleUpLoadThreshold) || (futureLoad >= scaleUpLoadThreshold)) {
                // Avoid overprovisioning by scaling up only if the total free cpu is smaller than 2 * scaleUp,
                // except if the load of this cpu is getting too high (half the threshold set)
                if ((((float) activeCoresOfThisTc - totalLoad < 3 * scaleUpLoadThreshold) ||
                    (load > highScaleUpLoadThreshold)) && (pastLoad >= scaleUpLoadThreshold)) {
                    overloaded.add(cpu);
                }
            // Load has decreased to the desired level for scale down
            // We will not scale if the scaling itself (assuming the core will take 1.8 times its current load)
            // will cause a scaleUp directly after
            } else if ((highestLoad <= scaleDownLoadThreshold) && ((highestLoad * 1.8) <= scaleUpLoadThreshold)) {
                // Avoid underprovisioning by scaling down only if total load is lower than 2 * scaleDown,
                // except if the load of this cpu is getting too low (half the threshold set)
                if (((totalLoad < (3 * scaleDownLoadThreshold * activeCoresOfThisTc)) ||
                    (load < (scaleDownLoadThreshold / 3)))) {
                    // Scale down
                    underloaded.add(cpu);
                }
            }
        }

        boolean didSomething = false;

        // Comparator that allows to select the CPU to inflate first, by order of last rescheduling then per load
        Comparator<Integer> loadComparator = (c1, c2) -> {
            int c;
            if (isRebalanced.get(c1).lastResched != null) {     // c1 ok
                if (isRebalanced.get(c2).lastResched == null) { // c2 ok
                    return 1; //c1 preferred
                }
                c = Comparator.<Long>naturalOrder().compare(
                    isRebalanced.get(c1).lastResched.unixTimestamp() / 1000,
                    isRebalanced.get(c2).lastResched.unixTimestamp() / 1000
                );
            } else if (isRebalanced.get(c2).lastResched != null) { // c1 ok, c2 ok continue
                return -1; // Prefer c2
            } else {       // c1 ok, c2 ok
                c = 0;
            }
            if (c != 0) {
                return c;
            }
            return Comparator.<Float>naturalOrder().compare(activeCores.get(c1), activeCores.get(c2));
        };

        log.debug("[{}] \t Deflating selected CPU", label());
        while (overloaded.size() > 0) {
            Integer cpu = Collections.max(overloaded, loadComparator);
            overloaded.remove(cpu);

            int newCpu = this.deflateLoad(sc, tc, deviceId, cpu, maxCpus, withLimitedReconfiguration);
            if (newCpu >= 0) {
                checkArgument(newCpu != cpu, "deflated to the same core !");
                if (!isRebalanced.containsKey(newCpu)) {
                    isRebalanced.put(newCpu, new CpuRuntimeInfo());
                }
                if (underloaded.contains(newCpu)) {
                   log.error("[{}] \t Inflated in CPU {} which was selected for deflation", label(), newCpu);
                   underloaded.remove(newCpu);
                }
                isRebalanced.get(cpu).lastResched = null;
                isRebalanced.get(newCpu).lastResched = null;
                didSomething = true;
            }
        }

        log.debug("[{}] \t Inflating selected CPU", label());
        while (underloaded.size() > 1) { //A core must inflate on another one
            Integer cpu = Collections.min(underloaded, loadComparator);
            underloaded.remove(cpu);

            int newCpu = this.inflateLoad(sc, tc, deviceId, cpu, maxCpus, withLimitedReconfiguration, underloaded);
            if (newCpu >= 0) {
                underloaded.remove(cpu);
                if (underloaded.contains(newCpu)) {
                    underloaded.remove(newCpu);
                }
                checkArgument(newCpu != cpu);
                isRebalanced.get(cpu).lastResched = null;
                isRebalanced.get(newCpu).lastResched = null;
                didSomething = true;
            }
        }

        return didSomething;
    }

    @Override
    public int deflateLoad(
            ServiceChainInterface    sc,
            TrafficClassRuntimeInfo  tc,
            DeviceId       deviceId,
            int            overLoadedCpu,
            int            maxCpus,
            boolean        limitedReconfiguration) {
        ServiceChainId scId = sc.id();
        URI tcId = tc.trafficClassId();
        log.info("");
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] \t Deflate load on traffic class group {}: Core {}", label(), tcId, overLoadedCpu);

        int initialCores = taggingService.getNumberOfActiveCoresOfTrafficClassGroup(tcId);
        // TEMP test
        checkArgument(
            initialCores == tc.coresOfDevice(deviceId).size(),
            "Initial number of cores %d is different than core map size %d!",
            initialCores, tc.coresOfDevice(deviceId).size()
        );

        // We want to increase the number of CPU cores by one
        int cpuCoresToAllocate = initialCores + 1;
        if (cpuCoresToAllocate > maxCpus) {
            log.warn("[{}] \t Not enough cores to deflate this traffic class group", label());
            log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
            log.info("");
            return -1;
        }

        log.info("[{}] \t Initial # of CPU Cores {} --> New # of CPU cores {}",
                label(), initialCores, cpuCoresToAllocate);

        /**
         * Measure the time it takes to compute the reconfiguration.
         */
        // START
        WallClockNanoTimestamp startReconf = new WallClockNanoTimestamp();

        // Fetch the overloaded device object
        RestServerSBDevice device = (RestServerSBDevice) topologyService.getDevice(deviceId);

        // Fetch the dataplane tree of this service chain
        NfvDataplaneTreeInterface tree = serviceChainService.runnableServiceChainWithTrafficClass(scId, tcId);

        Set<FlowRule> newRules = null;

        // Compute the deflated traffic classes
        Pair<RxFilterValue, Set<TrafficClassInterface>> changes =
            taggingService.deflateTrafficClassGroup(tcId, overLoadedCpu);

        // Check what is sent back
        int newCpu;
        if (changes == null) {
            log.warn("[{}] \t {}", label(), taggingService.getLbStatusOfTrafficClassGroup(tcId));
            return -1;
        } else {
            newCpu = changes.getKey().cpuId();
        }

        checkArgument(overLoadedCpu != newCpu, "Scheduling a CPU to itself!");

        // Proper way of deflating
        if (!limitedReconfiguration) {
            // The Tag Manager sent us the affected traffic classes along with their new tag
            RxFilterValue changedTag = changes.getKey();
            Set<TrafficClassInterface> changedTcs = changes.getValue();
            RxFilter filterMechanism = taggingService.getTaggingMechanismOfTrafficClassGroup(tcId);

            checkNotNull(tree.pathEstablisher(), "No path established for service chain " + scId);

            // Get useful information about the device that will host the new rules
            DeviceId  offloaderId = tree.pathEstablisher().offloaderSwitchId();
            long  offloaderInPort = tree.pathEstablisher().serverInressPort();
            long offloaderOutPort = tree.pathEstablisher().offloaderSwitchToServerPort();

            // Compute the new rules
            newRules = NfvDataplaneTree.convertTrafficClassSetToOpenFlowRules(
                changedTcs, tcId, deployerService.applicationId(), offloaderId,
                offloaderInPort, (long) cpuCoresToAllocate, offloaderOutPort,
                filterMechanism, changedTag, true
            );
        }

        // STOP
        WallClockNanoTimestamp endReconf = new WallClockNanoTimestamp();

        // Send the reconfiguration delay to the monitoring service
        float reconfDelay = (float) (endReconf.unixTimestamp() - startReconf.unixTimestamp());
        monitoringService.updateGlobalReconfigurationDelayOfServiceChain(scId, reconfDelay);

        // Update the number of active CPU cores of the device
        monitoringService.addActiveCoresToDevice(deviceId, 1);

        // Reconfigure the Metron agent and the necessary network elements
        checkArgument(!tc.coresOfDevice(deviceId).contains(newCpu), "Adding a used CPU!");
        checkArgument(tc.coresOfDevice(deviceId).contains(overLoadedCpu), "The overloaded CPU is not scheduled...?");
        Set<Integer> newMap = tc.coresOfDevice(deviceId);
        newMap.add(newCpu);
        this.doReconfiguration(deviceId, sc.id(), tc, newRules, newMap);

        // Update the rules of this traffic class
        if (!limitedReconfiguration) {
            this.updateRulesOfTrafficClass(tree, tcId, newRules);
        }

        log.info("[{}] \t Done", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("");

        return newCpu;
    }

    @Override
    public int inflateLoad(
            ServiceChainInterface sc,
            TrafficClassRuntimeInfo tc,
            DeviceId deviceId,
            int underLoadedCpu,
            int maxCpus,
            boolean limitedReconfiguration,
            Set<Integer> inflateCandidates) {
        URI tcId = tc.trafficClassId();
        int initialCores = taggingService.getNumberOfActiveCoresOfTrafficClassGroup(tcId);
        if (initialCores == 1) {
            return -1;
        }

        log.info("");
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] \t Inflate load on traffic class group {}: Core {}", label(), tcId, underLoadedCpu);


        log.info("[{}] \t Initial # of CPU Cores {} --> New # of CPU cores {}",
            label(), initialCores, initialCores - 1);

        /**
         * Measure the time it takes to compute the reconfiguration.
         */
        // START
        WallClockNanoTimestamp startReconf = new WallClockNanoTimestamp();

        // Fetch the underloaded device object
        RestServerSBDevice device = (RestServerSBDevice) topologyService.getDevice(deviceId);

        // Fetch the dataplane tree of this service chain
        NfvDataplaneTreeInterface tree = serviceChainService.runnableServiceChainWithTrafficClass(sc.id(), tcId);

        Set<FlowRule> newRules = null;

        // Compute the inflated traffic classes
        Pair<Pair<RxFilterValue, RxFilterValue>, Set<TrafficClassInterface>> changes =
            taggingService.inflateTrafficClassGroup(tcId, underLoadedCpu, inflateCandidates);

        // Check what is sent back
        int removedCpu;
        if (changes == null) {
            log.warn("[{}] \t {}", label(), taggingService.getLbStatusOfTrafficClassGroup(tcId));
            return -1;
        } else {
            int a;
            int b;
            a = changes.getKey().getKey().cpuId();
            b = changes.getKey().getValue().cpuId();

            checkArgument(a != b, "Both inflated CPU are identical : {} !", a);
            if (a == underLoadedCpu) {
                removedCpu = b;
            } else if (b == underLoadedCpu) {
                removedCpu = a;
            } else {
                throw new IllegalArgumentException("Inflated the wrong CPU core");
            }
        }

        // Proper way of inflating
        if (!limitedReconfiguration) {
            // The Tag Manager sent us the affected traffic classes along with their new tag
            RxFilterValue changedTag = changes.getKey().getKey();
            Set<TrafficClassInterface> changedTcs = changes.getValue();
            RxFilter filterMechanism = taggingService.getTaggingMechanismOfTrafficClassGroup(tcId);

            checkNotNull(tree.pathEstablisher(), "No path established for service chain " + sc.id());

            // Get useful information about the device that will host the new rules
            DeviceId  offloaderId = tree.pathEstablisher().offloaderSwitchId();
            long offloaderInPort = tree.pathEstablisher().serverInressPort();
            long offloaderOutPort = tree.pathEstablisher().offloaderSwitchToServerPort();

            // Compute the new rules
            newRules = NfvDataplaneTree.convertTrafficClassSetToOpenFlowRules(
                    changedTcs, tcId, deployerService.applicationId(), offloaderId,
                    offloaderInPort, initialCores - 1, offloaderOutPort,
                    filterMechanism, changedTag, true
                    );
        }

        // STOP
        WallClockNanoTimestamp endReconf = new WallClockNanoTimestamp();

        // Send the reconfiguration delay to the monitoring service
        float reconfDelay = (float) (endReconf.unixTimestamp() - startReconf.unixTimestamp());
        monitoringService.updateGlobalReconfigurationDelayOfServiceChain(sc.id(), reconfDelay);

        // Update the number of active CPU cores of the device
        monitoringService.removeActiveCoresFromDevice(deviceId, 1);

        // Reconfigure the Metron agent and the necessary network elements
        checkArgument(tc.coresOfDevice(deviceId).contains(removedCpu), "Removing an unused CPU!");
        checkArgument(tc.coresOfDevice(deviceId).contains(underLoadedCpu), "The underloaded CPU is not scheduled...?");

        Set<Integer> newMap = tc.coresOfDevice(deviceId);
        newMap.remove(removedCpu);
        this.doReconfiguration(deviceId, sc.id(), tc, newRules, newMap);

        // Update the rules of this traffic class
        if (!limitedReconfiguration) {
            this.updateRulesOfTrafficClass(tree, tcId, newRules);
        }

        log.info("[{}] \t Done", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("");

        return removedCpu;
    }

    /**
     * Parse the collected CPU statistics and report the CPU cores that
     * exhibit load greater than a certain threshold.
     *
     * @param cpuStats the CPU statistics to be parsed
     * @param loadThreshold the load threshold we are looking for
     * @return a map of cores with the desired load pattern
     */
    private Map<Integer, Float> getCoresWithLoadGreaterThan(
            Collection<CpuStatistics> cpuStats, float loadThreshold) {
        Map<Integer, Float> cores = new ConcurrentHashMap<Integer, Float>();

        for (CpuStatistics cs : cpuStats) {
            if ((cs.load() > loadThreshold) || cs.busy()) {
                cores.put(cs.id(), cs.load());
            }
        }

        return cores;
    }

    /**
     * Parse the collected CPU statistics and report the CPU cores that
     * exhibit load less than a certain threshold.
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
     * Parse the collected CPU statistics and report the CPU core that
     * exhibits the least load.
     *
     * @param cpuStats the CPU statistics to be parsed
     * @return the ID of the least overloaded core
     */
    private int getLeastOverloadedCore(Collection<CpuStatistics> cpuStats) {
        int minCore = -1;
        float minLoad = (float) 1.0;
        for (CpuStatistics cs : cpuStats) {
            int coreId = cs.id();
            float coreLoad = cs.load();

            if (coreLoad == 0) {
                return coreId;
            }

            if (coreLoad < minLoad) {
                minCore = coreId;
                minLoad = coreLoad;
            }
        }

        return minCore;
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
        MonitoringStatistics stats = topologyService.getTrafficClassMonitoringStats(deviceId, scId, tcId);

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
     * @param tc the traffic class to monitor
     * @param newRules the set of new rulues to update the data plane
     * @param newCoreSet the new number of CPU cores to allocate
     */
    private void doReconfiguration(
            DeviceId deviceId, ServiceChainId scId, TrafficClassRuntimeInfo tc,
            Set<FlowRule> newRules, Set<Integer> newCoreSet) {
        /**
         * Measure the time it takes to enforce the decision.
         */
        WallClockNanoTimestamp startEnforc = new WallClockNanoTimestamp();

        // Ask from the topology manager to reconfigure this traffic class

        boolean status = topologyService.reconfigureTrafficClassOfServiceChain(
                deviceId, scId, tc.trafficClassId(), null, null, newCoreSet, -1
                );
        if (status) {
            tc.setCoresOfDevice(deviceId, newCoreSet);
        }

        // Push the rules to the device
        if (newRules != null) {
            deployerService.updateRules(scId, newRules);
        }

        // STOP
        WallClockNanoTimestamp endEnforc = new WallClockNanoTimestamp();

        // Compute the time difference
        float enforcDelay = (float) (endEnforc.unixTimestamp() - startEnforc.unixTimestamp());

        // Store the enforcement delay
        if (monitoringService != null) {
            monitoringService.updateEnforcementDelayOfTrafficClass(scId, tc.trafficClassId(), enforcDelay);
        }
    }

    /**
     * Updates the rules of a service chain after a load balancing decision.
     *
     * @param dpTree the data plane tree of a service chain
     * @param tcId the ID of the traffic class to update
     * @param newRules the set of rules to be updated
     */
    private void updateRulesOfTrafficClass(
            NfvDataplaneTreeInterface dpTree, URI tcId, Set<FlowRule> newRules) {
        // Fetch the current rules associated with this traffic class
        Set<FlowRule> currentRules = dpTree.hardwareConfigurationOfTrafficClass(tcId);

        Iterator<FlowRule> ruleIterator = currentRules.iterator();
        while (ruleIterator.hasNext()) {
            FlowRule rule = ruleIterator.next();

            // Found the guilty
            if (findRule(newRules, rule)) {
                ruleIterator.remove();
            }
        }

        // Now you can safely merge current with new rules
        currentRules.addAll(newRules);

        // Push to memory
        dpTree.setHardwareConfigurationOfTrafficClass(tcId, currentRules);
    }

    /**
     * Looks up for a particular rule in a rule set.
     *
     * @param rules the set of rules to look up
     * @param ruleToFind the rule to find
     * @return boolean look up status
     */
    private boolean findRule(Set<FlowRule> rules, FlowRule ruleToFind) {
        for (FlowRule r : rules) {
            if (r.id().equals(ruleToFind.id())) {
                return true;
            }
        }
        return false;
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
            log.info("[{}] Service chain with ID {} is active", label(), sc.id());
        }
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

        if (Tools.isPropertyEnabled(properties, SCALE_UP_LOAD_THRESHOLD) != null) {
            float previousScaleUpLoadThreshold = scaleUpLoadThreshold;
            scaleUpLoadThreshold = Tools.getFloatProperty(properties, SCALE_UP_LOAD_THRESHOLD);

            if ((scaleUpLoadThreshold < 0) || (scaleUpLoadThreshold > 1) ||
                (scaleUpLoadThreshold < scaleDownLoadThreshold)) {
                scaleUpLoadThreshold = previousScaleUpLoadThreshold;
                log.info(
                    "Not configured due to invalid range. CPU load to trigger scale up remains {}%",
                    scaleUpLoadThreshold * 100);
            } else if (scaleUpLoadThreshold != previousScaleUpLoadThreshold) {
                log.info("Configured. CPU load to trigger scale up is now {}%", scaleUpLoadThreshold * 100);
            } else {
                log.info("CPU load to trigger scale up remains {}%", scaleUpLoadThreshold * 100);
            }
        }

        if (Tools.isPropertyEnabled(properties, SCALE_DOWN_LOAD_THRESHOLD) != null) {
            float previousScaleDownLoadThreshold = scaleDownLoadThreshold;
            scaleDownLoadThreshold = Tools.getFloatProperty(properties, SCALE_DOWN_LOAD_THRESHOLD);

            if ((scaleDownLoadThreshold < 0) || (scaleDownLoadThreshold > 1) ||
                (scaleDownLoadThreshold > scaleUpLoadThreshold)) {
                scaleDownLoadThreshold = previousScaleDownLoadThreshold;
                log.info(
                    "Not configured due to invalid range. CPU load to trigger scale down remains {}%",
                    scaleDownLoadThreshold * 100);
            } else if (scaleDownLoadThreshold != previousScaleDownLoadThreshold) {
                log.info("Configured. CPU load to trigger scale down is now {}%", scaleDownLoadThreshold * 100);
            } else {
                log.info("CPU load to trigger scale down remains {}%", scaleDownLoadThreshold * 100);
            }
        }

        if (Tools.isPropertyEnabled(properties, MONITORING_PERIOD_MS) != null) {
            int previousMonitoringPeriodMilli = monitoringPeriodMilli;
            monitoringPeriodMilli = Tools.getIntegerProperty(
                properties, MONITORING_PERIOD_MS, MONITORING_PERIOD_MS_DEFAULT);

            if (monitoringPeriodMilli <= 0) {
                monitoringPeriodMilli = previousMonitoringPeriodMilli;
                log.info(
                    "Not configured due to invalid value. Monitoring frequency remains {} ms", monitoringPeriodMilli);
            } else if (monitoringPeriodMilli != previousMonitoringPeriodMilli) {
                log.info("Configured. Monitoring frequency is now {} ms", monitoringPeriodMilli);
            } else {
                log.info("Monitoring frequency remains {} ms", monitoringPeriodMilli);
            }
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
