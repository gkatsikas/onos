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

package org.onosproject.metron.impl.server;

import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;

import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Default implementation of a traffic class's runtime information.
 */
public class DefaultTrafficClassRuntimeInfo implements TrafficClassRuntimeInfo {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Indicates that we have not yet decided about the exact CPU core.
     */
    public static final int AVAILABLE_CPU_CORE = -1;

    /**
     * The ID of the service chain.
     */
    private ServiceChainId serviceChainId;
    /**
     * The ID of the traffic class.
     */
    private URI trafficClassId;
    /**
     * The NIC ID where the packet processing
     * tree of this traffic class begins from.
     */
    private String primaryNic;
    /**
     * The maximum number of CPU cores for this traffic class.
     */
    private int maxNumberOfCpus;
    /**
     * The set of device IDs, where this traffic class is deployed.
     */
    private Set<DeviceId> devices;
    /**
     * Per-device CPU cores occupied by the traffic class.
     */
    private Map<DeviceId, Integer> cores;
    /**
     * Per-device set of NIC IDs used by the traffic class.
     */
    private Map<DeviceId, Set<String>> nics;
    /**
     * Per-device, per-core running configuration type of this traffic class.
     */
    private Map<DeviceId, Map<Integer, String>> configurationType;
    /**
     * Per-device, per-core running configuration of this traffic class.
     */
    private Map<DeviceId, Map<Integer, String>> configuration;
    /**
     * Per-device per NIC Rx filter methods (MAC, VLAN, MPLS, etc.).
     */
    private Map<DeviceId, Map<String, RxFilter>> rxFilterMethodsMap;
    /**
     * Per-device set of Rx filter values (i.e., tags).
     */
    private Map<DeviceId, Map<String, Set<RxFilterValue>>> rxFiltersMap;

    public DefaultTrafficClassRuntimeInfo(ServiceChainId scId, URI tcId, String nic, int maxCpus) {
        Preconditions.checkNotNull(
            scId, "Service chain ID is NULL"
        );
        Preconditions.checkNotNull(
            tcId, "Traffic class ID is NULL"
        );
        Preconditions.checkNotNull(
            nic, "Primary NIC ID is NULL"
        );
        Preconditions.checkArgument(
            maxCpus >= 0, "Maximum number of CPU cores must be positive"
        );

        this.serviceChainId     = scId;
        this.trafficClassId     = tcId;
        this.primaryNic         = nic;
        this.maxNumberOfCpus    = maxCpus;
        this.devices            = Sets.<DeviceId>newConcurrentHashSet();
        this.cores              = new ConcurrentHashMap<DeviceId, Integer>();
        this.nics               = new ConcurrentHashMap<DeviceId, Set<String>>();
        this.configurationType  = new ConcurrentHashMap<DeviceId, Map<Integer, String>>();
        this.configuration      = new ConcurrentHashMap<DeviceId, Map<Integer, String>>();
        this.rxFilterMethodsMap = new ConcurrentHashMap<DeviceId, Map<String, RxFilter>>();
        this.rxFiltersMap       = new ConcurrentHashMap<DeviceId, Map<String, Set<RxFilterValue>>>();
    }

    public DefaultTrafficClassRuntimeInfo(
            ServiceChainId scId, URI tcId, String nic, int maxCpus, Set<DeviceId> devices) {
        Preconditions.checkNotNull(
            scId, "Service chain ID is NULL"
        );
        Preconditions.checkNotNull(
            tcId, "Traffic class ID is NULL"
        );
        Preconditions.checkNotNull(
            nic, "Primary NIC ID is NULL"
        );
        Preconditions.checkArgument(
            maxCpus >= 0, "Maximum number of CPU cores must be positive"
        );
        Preconditions.checkNotNull(
            devices, "The NFV devices of traffic class " + tcId + " cannot be NULL"
        );

        this.serviceChainId     = scId;
        this.trafficClassId     = tcId;
        this.primaryNic         = nic;
        this.maxNumberOfCpus    = maxCpus;
        this.devices            = devices;
        this.cores              = new ConcurrentHashMap<DeviceId, Integer>();
        this.nics               = new ConcurrentHashMap<DeviceId, Set<String>>();
        this.configurationType  = new ConcurrentHashMap<DeviceId, Map<Integer, String>>();
        this.configuration      = new ConcurrentHashMap<DeviceId, Map<Integer, String>>();
        this.rxFilterMethodsMap = new ConcurrentHashMap<DeviceId, Map<String, RxFilter>>();
        this.rxFiltersMap       = new ConcurrentHashMap<DeviceId, Map<String, Set<RxFilterValue>>>();

        for (DeviceId dev : this.devices) {
            this.configuration.put(dev, new ConcurrentHashMap<Integer, String>());
            this.rxFiltersMap.put(dev,  new ConcurrentHashMap<String, Set<RxFilterValue>>());
        }
    }

    public DefaultTrafficClassRuntimeInfo(
            ServiceChainId scId,
            URI            tcId,
            String         nic,
            int            maxCpus,
            Set<DeviceId>  devices,
            Map<DeviceId, Integer> cores,
            Map<DeviceId, Set<String>> nics,
            Map<DeviceId, Map<Integer, String>> configurationType,
            Map<DeviceId, Map<Integer, String>> configuration,
            Map<DeviceId, Map<String, RxFilter>> rxFilterMethodsMap,
            Map<DeviceId, Map<String, Set<RxFilterValue>>> rxFiltersMap) {
        Preconditions.checkNotNull(
            scId, "Service chain ID is NULL"
        );
        Preconditions.checkNotNull(
            tcId, "Traffic class ID is NULL"
        );
        Preconditions.checkNotNull(
            nic, "Primary NIC ID is NULL"
        );
        Preconditions.checkArgument(
            maxCpus > 0, "Maximum number of CPU cores must be positive"
        );
        Preconditions.checkNotNull(
            devices, "The NFV devices of traffic class " + tcId + " cannot be NULL"
        );
        Preconditions.checkNotNull(
            cores, "The number of cores of traffic class " + tcId + " cannot be NULL"
        );
        Preconditions.checkNotNull(
            nics, "The set of NICs of traffic class " + tcId + " cannot be NULL"
        );
        Preconditions.checkNotNull(
            configurationType, "The configuration type of traffic class " + tcId + " cannot be NULL"
        );
        Preconditions.checkNotNull(
            configuration, "The configuration of traffic class " + tcId + " cannot be NULL"
        );
        Preconditions.checkNotNull(
            rxFilterMethodsMap, "The Rx filter methods' map of traffic class " + tcId + " cannot be NULL"
        );
        Preconditions.checkNotNull(
            rxFiltersMap, "The Rx filter values' map of traffic class " + tcId + " cannot be NULL"
        );

        this.serviceChainId     = scId;
        this.trafficClassId     = tcId;
        this.primaryNic         = nic;
        this.maxNumberOfCpus    = maxCpus;
        this.devices            = devices;
        this.cores              = cores;
        this.nics               = nics;
        this.configurationType  = configurationType;
        this.configuration      = configuration;
        this.rxFilterMethodsMap = rxFilterMethodsMap;
        this.rxFiltersMap       = rxFiltersMap;
    }

    @Override
    public ServiceChainId serviceChainId() {
        return this.serviceChainId;
    }

    @Override
    public URI trafficClassId() {
        return this.trafficClassId;
    }

    @Override
    public String primaryNic() {
        return this.primaryNic;
    }

    @Override
    public int maxNumberOfCpus() {
        return this.maxNumberOfCpus;
    }

    @Override
    public Set<DeviceId> devices() {
        return this.devices;
    }

    @Override
    public void addDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot add a NULL device to traffic class " + this.trafficClassId
        );
        this.devices.add(deviceId);
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot remove a NULL device from traffic class " + this.trafficClassId
        );
        this.devices.remove(deviceId);
    }

    @Override
    public Map<DeviceId, Integer> cores() {
        return this.cores;
    }

    @Override
    public int coresOfDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the number of cores of a NULL device for traffic class " + this.trafficClassId
        );

        return this.cores.get(deviceId).intValue();
    }

    @Override
    public void setCoresOfDevice(DeviceId deviceId, int cpus) {
        Preconditions.checkNotNull(
            deviceId, "Cannot set the number of cores of a NULL device for traffic class " + this.trafficClassId
        );

        Preconditions.checkArgument(cpus >= 0, "Number of CPU cores must not be negative");

        this.cores.put(deviceId, cpus);
    }

    @Override
    public Map<DeviceId, Set<String>> nics() {
        return this.nics;
    }

    @Override
    public Set<String> nicsOfDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the NIC IDs of a NULL device for traffic class " + this.trafficClassId
        );
        return this.nics.get(deviceId);
    }

    @Override
    public void setNicsOfDevice(DeviceId deviceId, Set<String> nicIds) {
        Preconditions.checkNotNull(
            deviceId, "Cannot set the NIC IDs of a NULL device for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            nicIds, "Cannot set the NULL NIC IDs for traffic class " + this.trafficClassId
        );
        Preconditions.checkArgument(
            nicIds.size() > 0, "Attempted to set empty set of NICs for device " + deviceId
        );

        this.nics.put(deviceId, nicIds);
    }

    @Override
    public void addNicToDevice(DeviceId deviceId, String nicId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot set the NIC IDs of a NULL device for traffic class " + this.trafficClassId
        );
        Preconditions.checkArgument(
            !nicId.isEmpty(), "Cannot add an empty NIC ID to traffic class " + this.trafficClassId
        );

        if (nicId.isEmpty()) {
            return;
        }

        this.nics.get(deviceId).add(nicId);
    }

    @Override
    public Map<DeviceId, Map<Integer, String>> clusterConfigurationType() {
        return this.configurationType;
    }

    @Override
    public Map<DeviceId, Map<Integer, String>> clusterConfiguration() {
        return this.configuration;
    }

    @Override
    public Map<Integer, String> deviceConfigurationType(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot get the configuration type of a NULL device for traffic class " + this.trafficClassId
        );
        return this.configurationType.get(deviceId);
    }

    @Override
    public Map<Integer, String> deviceConfiguration(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot get the configuration of a NULL device for traffic class " + this.trafficClassId
        );
        return this.configuration.get(deviceId);
    }

    @Override
    public String deviceConfigurationTypeOfCore(DeviceId deviceId, int cpu) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot get the configuration type of a NULL device for traffic class " + this.trafficClassId
        );

        Preconditions.checkArgument(cpu >= 0, "CPU core number must not be negative");

        Integer core = Integer.valueOf(cpu);

        // No entry for this device
        if (this.configurationType.get(deviceId) == null) {
            return "";
        }

        return this.configurationType.get(deviceId).get(core);
    }

    @Override
    public String deviceConfigurationOfCore(DeviceId deviceId, int cpu) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot get the configuration of a NULL device for traffic class " + this.trafficClassId
        );

        Preconditions.checkArgument(cpu >= 0, "CPU core number must not be negative");

        Integer core = Integer.valueOf(cpu);

        // No entry for this device
        if (this.configuration.get(deviceId) == null) {
            return "";
        }

        return this.configuration.get(deviceId).get(core);
    }

    @Override
    public int findCoreOfDeviceConfiguration(DeviceId deviceId, String config) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot get the CPU core of a NULL device for traffic class " + this.trafficClassId
        );

        Preconditions.checkArgument(!config.isEmpty(), "Traffic class configuration must not be empty");

        Map<Integer, String> devConf = this.configuration.get(deviceId);
        if (devConf == null) {
            return -1;
        }

        for (Map.Entry<Integer, String> confMap : devConf.entrySet()) {
            int core = confMap.getKey();
            String conf = confMap.getValue();

            if (conf.equals(config)) {
                return core;
            }
        }

        return -1;
    }

    @Override
    public void setDeviceConfigurationTypeOfCore(DeviceId deviceId, int cpu, String config) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot set the configuration type on a NULL device for traffic class " + this.trafficClassId
        );

        /**
         * AVAILABLE_CPU_CORE means that we are asked to place this configuration
         * without specifying a CPU core.
         * This is acceptable as it can be dynamically decided by the NFV agent.
         */
        if ((cpu < 0) && (cpu != AVAILABLE_CPU_CORE)) {
            throw new DeploymentException("Cannot associate configuration type with core " + cpu);
        }

        Integer core = Integer.valueOf(cpu);

        // No entry for this device
        if (this.configurationType.get(deviceId) == null) {
            this.configurationType.put(deviceId, new ConcurrentHashMap<Integer, String>());
        }

        this.configurationType.get(deviceId).put(Integer.valueOf(cpu), config);
    }

    @Override
    public void setDeviceConfigurationOfCore(DeviceId deviceId, int cpu, String config) {
        Preconditions.checkNotNull(
            deviceId,
            "Cannot set the configuration on a NULL device for traffic class " + this.trafficClassId
        );

        /**
         * AVAILABLE_CPU_CORE means that we are asked to place this configuration
         * without specifying a CPU core.
         * This is acceptable as it can be dynamically decided by the NFV agent.
         */
        if ((cpu < 0) && (cpu != AVAILABLE_CPU_CORE)) {
            throw new DeploymentException("Cannot associate configuration with core " + cpu);
        }

        Integer core = Integer.valueOf(cpu);

        // No entry for this device
        if (this.configuration.get(deviceId) == null) {
            this.configuration.put(deviceId, new ConcurrentHashMap<Integer, String>());
        }

        this.configuration.get(deviceId).put(Integer.valueOf(cpu), config);
    }

    @Override
    public boolean removeDeviceConfigurationFromCore(DeviceId deviceId, int cpu) {
        Preconditions.checkNotNull(
            deviceId, "Cannot remove the configuration of a NULL device for traffic class " + this.trafficClassId
        );

        Integer core = Integer.valueOf(cpu);

        // No entry for this device
        if (this.configuration.get(deviceId) == null) {
            return false;
        }

        this.configurationType.get(deviceId).remove(core);
        if (this.configuration.get(deviceId).remove(core) != null) {
            return true;
        }

        return false;
    }

    @Override
    public Map<DeviceId, Map<String, RxFilter>> rxFilterMethodsMap() {
        return this.rxFilterMethodsMap;
    }

    @Override
    public Map<String, RxFilter> rxFilterMethodsOfDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the Rx filter method of a NULL device of traffic class " + this.trafficClassId
        );

        return this.rxFilterMethodsMap.get(deviceId);
    }

    @Override
    public RxFilter rxFilterMethodOfDeviceOfNic(DeviceId deviceId, String nic) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the Rx filter method of a NULL device of traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            nic, "Cannot get the Rx filter method of a NULL NIC for traffic class " + this.trafficClassId
        );

        if (!this.rxFilterMethodsMap.containsKey(deviceId)) {
            return null;
        }

        return this.rxFilterMethodsMap.get(deviceId).get(nic);
    }

    @Override
    public void setRxFilterMethodOfDeviceOfNic(DeviceId deviceId, String nic, RxFilter method) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the Rx filter method of a NULL device of traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            nic, "Cannot get the Rx filter method of a NULL NIC for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            method, "NULL Rx filter method for traffic class " + this.trafficClassId
        );

        if (!this.rxFilterMethodsMap.containsKey(deviceId)) {
            this.rxFilterMethodsMap.put(deviceId, new ConcurrentHashMap<String, RxFilter>());
        }

        this.rxFilterMethodsMap.get(deviceId).put(nic, method);
    }

    @Override
    public Map<DeviceId, Map<String, Set<RxFilterValue>>> rxFiltersMap() {
        return this.rxFiltersMap;
    }

    @Override
    public Map<String, Set<RxFilterValue>> rxFiltersOfDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the Rx filters of a NULL device for traffic class " + this.trafficClassId
        );

        return this.rxFiltersMap.get(deviceId);
    }

    @Override
    public Set<RxFilterValue> rxFiltersOfDeviceOfNic(DeviceId deviceId, String nic) {
        Preconditions.checkNotNull(
            deviceId, "Cannot get the Rx filters of a NULL device for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            nic, "Cannot get the Rx filters of a NULL NIC for traffic class " + this.trafficClassId
        );

        return this.rxFiltersMap.get(deviceId).get(nic);
    }

    @Override
    public void addRxFilterToDeviceToNic(DeviceId deviceId, String nic, RxFilterValue rxFilterValue) {
        Preconditions.checkNotNull(
            deviceId, "Cannot add an Rx filter to a NULL device for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            nic, "Cannot add an Rx filter to a NULL NIC for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            rxFilterValue, "Cannot add a NULL Rx filter value to traffic class " + this.trafficClassId
        );

        if (!this.rxFiltersMap.containsKey(deviceId)) {
            this.rxFiltersMap.put(deviceId, new ConcurrentHashMap<String, Set<RxFilterValue>>());
        }

        if (!this.rxFiltersMap.get(deviceId).containsKey(nic)) {
            this.rxFiltersMap.get(deviceId).put(nic, new ConcurrentSkipListSet<RxFilterValue>());
        }

        this.rxFiltersMap.get(deviceId).get(nic).add(rxFilterValue);
    }

    @Override
    public void removeRxFilterFromDeviceFromNic(DeviceId deviceId, String nic, RxFilterValue rxFilterValue) {
        Preconditions.checkNotNull(
            deviceId, "Cannot remove an Rx filter from a NULL device for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            nic, "Cannot remove an Rx filter from a NULL NIC for traffic class " + this.trafficClassId
        );
        Preconditions.checkNotNull(
            rxFilterValue, "Cannot remove a NULL Rx filter value from traffic class " + this.trafficClassId
        );

        if (rxFiltersMap.get(deviceId) == null) {
            return;
        }

        if (rxFiltersMap.get(deviceId).get(nic) == null) {
            return;
        }

        this.rxFiltersMap.get(deviceId).get(nic).remove(rxFilterValue);
    }

    @Override
    public int numberOfNics(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot query the NICs of a NULL device of traffic class " + this.trafficClassId
        );

        return this.nics.get(deviceId).size();
    }

    @Override
    public int numberOfCpus(DeviceId deviceId) {
        Preconditions.checkNotNull(
            deviceId, "Cannot query the CPUs of a NULL device of traffic class " + this.trafficClassId
        );

        return this.coresOfDevice(deviceId);
    }

    @Override
    public TrafficClassRuntimeInfo update(TrafficClassRuntimeInfo other) {
        // Update devices
        for (DeviceId dev : other.devices()) {
            if (!this.devices().contains(dev)) {
                this.addDevice(dev);
                log.info("Device {} added", dev);
            }
        }

        // Update CPU cores
        for (Map.Entry<DeviceId, Integer> coreMap : other.cores().entrySet()) {
            DeviceId dev = coreMap.getKey();
            int cores = coreMap.getValue().intValue();

            if (this.coresOfDevice(dev) < other.coresOfDevice(dev)) {
                this.setCoresOfDevice(dev, cores);
                log.info("Cores number {} updated", cores);
            }
        }

        // Update NICs
        for (Map.Entry<DeviceId, Set<String>> nicMap : other.nics().entrySet()) {
            DeviceId dev = nicMap.getKey();
            Set<String> nics = nicMap.getValue();

            for (String nicId : nics) {
                if (!this.nicsOfDevice(dev).contains(nicId)) {
                    this.addNicToDevice(dev, nicId);
                    log.info("NIC {} added", nicId);
                }
            }
        }

        // Update configuration type
        for (Map.Entry<DeviceId, Map<Integer, String>> confTypeMap : other.clusterConfigurationType().entrySet()) {
            DeviceId dev = confTypeMap.getKey();
            Map<Integer, String> devConfType = confTypeMap.getValue();

            Set<Integer> diff = Sets.symmetricDifference(
                devConfType.keySet(), this.deviceConfigurationType(dev).keySet()
            );

            for (Integer c : diff) {
                if (!this.deviceConfigurationType(dev).containsKey(c)) {
                    this.setDeviceConfigurationTypeOfCore(dev, c.intValue(), devConfType.get(c));
                    log.info("Core {} with conf type {} added", c.intValue(), devConfType.get(c));
                }
            }
        }

        // Update configuration
        for (Map.Entry<DeviceId, Map<Integer, String>> confMap : other.clusterConfiguration().entrySet()) {
            DeviceId dev = confMap.getKey();
            Map<Integer, String> devConf = confMap.getValue();

            Set<Integer> diff = Sets.symmetricDifference(
                devConf.keySet(), this.deviceConfiguration(dev).keySet()
            );

            for (Integer c : diff) {
                if (!this.deviceConfiguration(dev).containsKey(c)) {
                    this.setDeviceConfigurationOfCore(dev, c.intValue(), devConf.get(c));
                    log.info("Core {} with conf {} added", c.intValue(), devConf.get(c));
                }
            }
        }

        // Update Rx filter values
        for (Map.Entry<DeviceId, Map<String, Set<RxFilterValue>>> rxValueMap : other.rxFiltersMap().entrySet()) {
            DeviceId dev = rxValueMap.getKey();
            Map<String, Set<RxFilterValue>> rxFilters = rxValueMap.getValue();

            for (Map.Entry<String, Set<RxFilterValue>> nicValueMap : rxFilters.entrySet()) {
                String nic = nicValueMap.getKey();
                Set<RxFilterValue> filters = nicValueMap.getValue();

                for (RxFilterValue filter : filters) {
                    this.addRxFilterToDeviceToNic(dev, nic, filter);
                    log.info("Rx filter value {} added", filter.toString());
                }
            }
        }

        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("serviceChainId", serviceChainId())
                .add("devices", devices())
                .add("cores", cores())
                .add("nics", nics())
                .add("configuration", clusterConfiguration())
                .add("rxFilterMethodsMap", rxFilterMethodsMap())
                .add("rxFiltersMap", rxFiltersMap())
                .toString();
    }

}
