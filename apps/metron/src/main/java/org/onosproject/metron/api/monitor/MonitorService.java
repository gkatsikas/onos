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

package org.onosproject.metron.api.monitor;

import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.structures.LruCache;

import org.onosproject.net.DeviceId;

import java.net.URI;
import java.util.Map;

/**
 * Interface for monitoring several operations.
 */
public interface MonitorService {

    /*********************************** Synthesis Delay. **********************************/

    /**
     * Return the memory that keeps the synthesis delay for all the service chains.
     *
     * @return the map between service chains and their synthesis delays
     */
    Map<ServiceChainId, Float> synthesisDelayMap();

    /**
     * Return the synthesis delay of a particular service chain.
     * The time is reported in nanoseconds.
     *
     * @param scId the ID of the monitored service chain
     * @return the synthesis delay of this service chain
     */
    float synthesisDelayOfServiceChain(ServiceChainId scId);

    /**
     * Add the synthesis delay of this service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param delay the synthesis delay value to be stored
     */
    void addSynthesisDelayOfServiceChain(ServiceChainId scId, float delay);

    /********************************** Monitoring Delay. **********************************/

    /**
     * Return the memory that keeps the monitoring delay for all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their monitoring delay LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Float>>> monitoringDelayMap();

    /**
     * Return the monitoring delay LRU cache of all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the map of delays to monitor this service chain
     */
    Map<URI, LruCache<Float>> monitoringDelayOfServiceChain(ServiceChainId scId);

    /**
     * Return the monitoring delay LRU cache of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the delay LRU cache to monitor this traffic class
     */
    LruCache<Float> monitoringDelayOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the monitoring delay LRU cache of a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the delay LRU cache to monitor this traffic class
     */
    LruCache<Float> monitoringDelayOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the monitoring delay of this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param delay the delay value to be stored
     */
    void updateMonitoringDelayOfTrafficClass(ServiceChainId scId, URI tcId, float delay);

    /**************************** Offloading Computation Delay. ****************************/

    /**
     * Return the memory that keeps the computational delay to offload the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their computational offloading delay LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Float>>> offloadingComputationDelayMap();

    /**
     * Return the computational delay to offload all of the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return map of computational delays to offload the traffic classes of this service chain
     */
    Map<URI, LruCache<Float>> offloadingComputationDelayOfServiceChain(ServiceChainId scId);

    /**
     * Return the computational delay to offload a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return computational delay to offload this traffic class
     */
    LruCache<Float> offloadingComputationDelayOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the computational delay to offload a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return computational delay to offload this traffic class
     */
    LruCache<Float> offloadingComputationDelayOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the computation delay to offload this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param delay the delay value to be stored
     */
    void updateOffloadingComputationDelayOfTrafficClass(
        ServiceChainId scId, URI tcId, float delay
    );

    /**************************** Offloading Installation Delay. ***************************/

    /**
     * Return the memory that keeps the installation delay to offload all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their installation offloading delay LRU cache
     */
    Map<ServiceChainId, LruCache<Float>> offloadingInstallationDelayMap();

    /**
     * Return the installation delay to offload all of the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return installation delay LRU cache to offload the traffic classes of this service chain
     */
    LruCache<Float> offloadingInstallationDelayOfServiceChain(ServiceChainId scId);

    /**
     * Update the memory that keeps the installation delay to offload this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param delay the delay value to be stored
     */
    void updateOffloadingInstallationDelay(ServiceChainId scId, float delay);

    /************************************ Launch Delay. ************************************/

    /**
     * Return the memory that keeps the delay to launch each service chains.
     * This is the dataplane delay once the NFv agent receives a `deploy`
     * instruction from ONOS.
     *
     * @return the map between service chains, traffic classes
     *         and their launch delays
     */
    Map<ServiceChainId, Map<URI, Long>> launchDelayMap();

    /**
     * Return the delay to launch all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the delay map to launch each traffic class of this service chain
     */
    Map<URI, Long> launchDelayOfServiceChain(ServiceChainId scId);

    /**
     * Return the delay to launch a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the delay to launch this traffic class
     */
    long launchDelayOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the delay to launch a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the delay to launch this traffic class
     */
    long launchDelayOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the delay to launch this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param delay the delay value to be stored
     */
    void updateLaunchDelayOfTrafficClass(ServiceChainId scId, URI tcId, long delay);

    /************************** Data plane Reconfiguration Delay. **************************/

    /**
     * Return the memory that keeps the server-level (data plane) reconfiguration delay
     * for all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their server-level reconfiguration delay LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Float>>> agentReconfigurationDelayMap();

    /**
     * Return the server-level reconfiguration delay LRU cache of all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the delay map to reconfigure each traffic class of this service chain
     */
    Map<URI, LruCache<Float>> agentReconfigurationDelayOfServiceChain(ServiceChainId scId);

    /**
     * Return the server-level reconfiguration delay LRU cache of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the reconfiguration delay LRU cache of this traffic class
     */
    LruCache<Float> agentReconfigurationDelayOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the server-level reconfiguration delay of a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the reconfiguration delay LRU cache of this traffic class
     */
    LruCache<Float> agentReconfigurationDelayOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the server-level reconfiguration delay of this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param delay the delay value to be stored
     */
    void updateAgentReconfigurationDelayOfTrafficClass(ServiceChainId scId, URI tcId, float delay);

    /**************************** Global Reconfiguration Delay. ****************************/

    /**
     * Return the memory that keeps the delay to perform a control plane-driven
     * reconfiguration for each service chain.
     *
     * @return the map between service chains, traffic classes
     *         and their control plane-driven reconfiguration delay LRU cache
     */
    Map<ServiceChainId, LruCache<Float>> globalReconfigurationDelayMap();

    /**
     * Return the control plane-driven reconfiguration delay LRU cache
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the delay map to reconfigure each traffic class of this service chain
     */
    LruCache<Float> globalReconfigurationDelayOfServiceChain(ServiceChainId scId);

    /**
     * Update the memory that keeps the control plane-driven reconfiguration
     * delay of this service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param delay the delay value to be stored
     */
    void updateGlobalReconfigurationDelayOfServiceChain(ServiceChainId scId, float delay);

    /********************************* Enforcement Delay. **********************************/

    /**
     * Return the memory that keeps the enforcement delay for all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their enforecement delay LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Float>>> enforcementDelayMap();

    /**
     * Return the enforcement delay LRU cache of all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the delay map to enforce new configuration to each traffic
     *         class of this service chain
     */
    Map<URI, LruCache<Float>> enforcementDelayOfServiceChain(ServiceChainId scId);

    /**
     * Return the enforcement delay LRU cache of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the enforcement delay LRU cache of this traffic class
     */
    LruCache<Float> enforcementDelayOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the enforcement delay of a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the enforcement delay LRU cache of this traffic class
     */
    LruCache<Float> enforcementDelayOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the enforcement delay of this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param delay the delay value to be stored
     */
    void updateEnforcementDelayOfTrafficClass(ServiceChainId scId, URI tcId, float delay);

    /************************** CPU Utilization per Service Chain. *************************/

    /**
     * Return the memory that keeps the CPU utilization for all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their CPU utilization LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Float>>> cpuLoadMap();

    /**
     * Return the CPU utilization LRU cache of all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the map of the CPU utilization of this service chain
     */
    Map<URI, LruCache<Float>> cpuLoadOfServiceChain(ServiceChainId scId);

    /**
     * Return the CPU utilization LRU cache of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the LRU cache with the CPU load of this traffic class
     */
    LruCache<Float> cpuLoadOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the CPU utilization LRU cache of a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the LRU cache with the CPU load of this traffic class
     */
    LruCache<Float> cpuLoadOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the CPU utilization of this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param load the load value to be stored
     */
    void updateCpuLoadOfTrafficClass(ServiceChainId scId, URI tcId, float load);

    /******************************** CPU Cache Utilization. *******************************/

    /**
     * Return the memory that keeps the number of CPU cache misses for all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their CPU cache misses LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Integer>>> cacheMissesMap();

    /**
     * Return the CPU cache misses LRU cache of all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the map of the CPU cache misses of this service chain
     */
    Map<URI, LruCache<Integer>> cacheMissesOfServiceChain(ServiceChainId scId);

    /**
     * Return the CPU cache misses LRU cache of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the LRU cache with the CPU cache misses of this traffic class
     */
    LruCache<Integer> cacheMissesOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the CPU cache misses LRU cache of a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the LRU cache with the CPU cache misses of this traffic class
     */
    LruCache<Integer> cacheMissesOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the CPU cache misses of this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param misses the cache misses value to be stored
     */
    void updateCacheMissesOfTrafficClass(ServiceChainId scId, URI tcId, int misses);

    /************************************ Packet Drops. ************************************/

    /**
     * Return the memory that keeps the number of packet drops for all the service chains.
     *
     * @return the map between service chains, traffic classes
     *         and their packet drops LRU cache
     */
    Map<ServiceChainId, Map<URI, LruCache<Integer>>> droppedPacketsMap();

    /**
     * Return the packet drops LRU cache of all the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @return the map of the packet drops of this service chain
     */
    Map<URI, LruCache<Integer>> droppedPacketsOfServiceChain(ServiceChainId scId);

    /**
     * Return the packet drops LRU cache of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @return the LRU cache with the packet drops of this traffic class
     */
    LruCache<Integer> droppedPacketsOfTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Return the packet drops LRU cache of a particular traffic class.
     * The service chain of this traffic class is not specified.
     *
     * @param tcId the ID of the monitored traffic class
     * @return the LRU cache with the packet drops of this traffic class
     */
    LruCache<Integer> droppedPacketsOfTrafficClass(URI tcId);

    /**
     * Update the memory that keeps the packet drops of this traffic class.
     *
     * @param scId the ID of the monitored service chain
     * @param tcId the ID of the monitored traffic class
     * @param drops the packet drops value to be stored
     */
    void updateDroppedPacketsOfTrafficClass(ServiceChainId scId, URI tcId, int drops);

    /******************************* Active CPU Cores/Device. ******************************/

    /**
     * Return the cache with the number of active cores of each NFV device.
     *
     * @return the map between NFV devices and the number of active CPU cores
     */
    Map<DeviceId, LruCache<Integer>> activeCoresPerDeviceMap();

    /**
     * Return the cache with the number of active cores of a particular NFV device.
     *
     * @param deviceId the ID of the device
     * @return the cache with the number of active cores of this NFV device
     */
    LruCache<Integer> activeCoresOfDevice(DeviceId deviceId);

    /**
     * Add active CPU cores to a particular device.
     *
     * @param deviceId the ID of the device
     * @param cores the number of active CPU cores to add
     */
    void addActiveCoresToDevice(DeviceId deviceId, int cores);

    /**
     * Remove active CPU cores from a particular device.
     *
     * @param deviceId the ID of the device
     * @param cores the number of active CPU cores to remove
     */
    void removeActiveCoresFromDevice(DeviceId deviceId, int cores);

    /****************************** CPU Utilization per Device. ****************************/

    /**
     * Return the memory that keeps the CPU utilization for all the devices.
     *
     * @return the map between devices and their CPU utilization LRU cache
     */
    Map<DeviceId, Map<Integer, LruCache<Float>>> cpuLoadPerDeviceMap();

    /**
     * Return the CPU utilization LRU cache of all the
     * CPU cores of a particular device.
     *
     * @param deviceId the ID of the monitored device
     * @return the map of the CPU utilization of this device
     */
    Map<Integer, LruCache<Float>> cpuLoadOfDevice(DeviceId deviceId);

    /**
     * Return the CPU utilization LRU cache of a particular CPU core
     * of a particular device.
     *
     * @param deviceId the ID of the monitored device
     * @param cpuId the ID of the monitored CPU core
     * @return the LRU cache with the CPU load of this CPU core
     */
    LruCache<Float> cpuLoadOfDeviceOfCore(DeviceId deviceId, int cpuId);

    /**
     * Update the memory that keeps the CPU utilization of a CPU of this device.
     *
     * @param deviceId the ID of the monitored device
     * @param cpuId the ID of the monitored CPU core
     * @param load the load value to be stored
     */
    void updateCpuLoadOfDeviceOfCore(DeviceId deviceId, int cpuId, float load);

    /********************************* End of Monitoring. ***********************************/

}
