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

package org.onosproject.metron.api.server;

import org.onosproject.metron.api.servicechain.ServiceChainId;

import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

import org.onosproject.net.DeviceId;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Runtime information for a traffic class.
 */
public interface TrafficClassRuntimeInfo {
    /**
     * Returns the ID of the service chain where the
     * traffic class belongs to.
     *
     * @return service chain ID
     */
    ServiceChainId serviceChainId();

    /**
     * Returns the ID of the traffic class.
     *
     * @return traffic class ID
     */
    URI trafficClassId();

    /**
     * Returns the NIC ID where the packet processing
     * tree of this traffic class begins from.
     *
     * @return NIC ID
     */
    String primaryNic();

    /**
     * Returns the maximum number of CPUs that can be allocated for this traffic
     * class on a device.
     *
     * @return number of CPUs
     */
    int maxNumberOfCpus();

    /**
     * Returns the set of devices where this traffic class is deployed.
     *
     * @return set of devices
     */
    Set<DeviceId> devices();

    /**
     * Adds a new NFV device to this traffic class.
     *
     * @param deviceId the NFV device to be added
     */
    void addDevice(DeviceId deviceId);

    /**
     * Removes an NFV device from this traffic class.
     *
     * @param deviceId the NFV device to be removed
     */
    void removeDevice(DeviceId deviceId);

    /**
     * Returns the number of CPU cores allocated per
     * device for this traffic class.
     *
     * @return traffic class's number of CPU cores
     *         mapped to devices
     */
    Map<DeviceId, Integer> cores();

    /**
     * Returns the number of CPU cores allocated on a
     * particular device for this traffic class.
     *
     * @param deviceId the NFV device to be queried
     * @return traffic class's number of CPU cores on a device
     */
    int coresOfDevice(DeviceId deviceId);

    /**
     * Sets the number of CPU cores allocated per
     * device for this traffic class.
     *
     * @param deviceId the NFV device to be configured
     * @param cpus     the number of CPU cores to be allocated
     */
    void setCoresOfDevice(DeviceId deviceId, int cpus);

    /**
     * Returns the set of NIC IDs per device for this traffic class.
     *
     * @return traffic class's set of NIC IDs mapped to devices
     */
    Map<DeviceId, Set<String>> nics();

    /**
     * Returns the set of NIC IDs used by a
     * particular device for this traffic class.
     *
     * @param deviceId the NFV device to be queried
     * @return traffic class's set of NIC IDs on a device
     */
    Set<String> nicsOfDevice(DeviceId deviceId);

    /**
     * Sets the set of NIC IDs allocated per
     * device for this traffic class.
     *
     * @param deviceId the NFV device to be configured
     * @param nicIds   the set of NIC IDs to be allocated
     */
    void setNicsOfDevice(DeviceId deviceId, Set<String> nicIds);

    /**
     * Adds a new NIC ID on a device for this traffic class.
     *
     * @param deviceId the NFV device to be configured
     * @param nicId    the NIC ID to be allocated
     */
    void addNicToDevice(DeviceId deviceId, String nicId);

    /**
     * Returns the running traffic class configuration type
     * across all the devices and CPU cores.
     *
     * @return traffic class's running configuration type
     */
    Map<DeviceId, Map<Integer, String>> clusterConfigurationType();

    /**
     * Returns the running traffic class configuration across
     * all the devices and CPU cores.
     *
     * @return traffic class's running configuration
     */
    Map<DeviceId, Map<Integer, String>> clusterConfiguration();

    /**
     * Returns the running traffic class configuration type
     * of a specific device across all the CPU cores.
     *
     * @param deviceId the NFV device to be queried
     * @return traffic class's running configuration type on a device
     */
    Map<Integer, String> deviceConfigurationType(DeviceId deviceId);

    /**
     * Returns the running traffic class configuration of a
     * specific device across all the CPU cores.
     *
     * @param deviceId the NFV device to be queried
     * @return traffic class's running configuration on a device
     */
    Map<Integer, String> deviceConfiguration(DeviceId deviceId);

    /**
     * Returns the running configuration type of this traffic class.
     * Can be either click or standalone (for a blackbox NF)
     *
     * @param deviceId the NFV device to be queried
     * @param cpu      the CPU core that runs the configuration
     * @return traffic class's running configuration type on a device and a core
     */
    String deviceConfigurationTypeOfCore(DeviceId deviceId, int cpu);

    /**
     * Returns the running configuration of this traffic class.
     *
     * @param deviceId the NFV device to be queried
     * @param cpu      the CPU core that runs the configuration
     * @return traffic class's running configuration on a device and a core
     */
    String deviceConfigurationOfCore(DeviceId deviceId, int cpu);

    /**
     * Returns the CPU core that runs a given configuration of this traffic class.
     *
     * @param deviceId the NFV device to be queried
     * @param config   traffic class's running configuration
     * @return CPU core that runs the configuration
     */
    int findCoreOfDeviceConfiguration(DeviceId deviceId, String config);

    /**
     * Sets the running configuration type on a specific device
     * and core for this traffic class.
     *
     * @param deviceId   the NFV device to be configured
     * @param cpu        the CPU core that runs the configuration
     * @param configType traffic class's running configuration type
     */
    void setDeviceConfigurationTypeOfCore(DeviceId deviceId, int cpu, String configType);

    /**
     * Sets the running configuration on a specific device
     * and core for this traffic class.
     *
     * @param deviceId the NFV device to be configured
     * @param cpu      the CPU core that runs the configuration
     * @param config   traffic class's running configuration
     */
    void setDeviceConfigurationOfCore(DeviceId deviceId, int cpu, String config);

    /**
     * Removes the running configuration from a specific device
     * and core for this traffic class.
     *
     * @param deviceId the NFV device to be configured
     * @param cpu      the CPU core that runs the configuration
     * @return boolean deletion status
     */
    boolean removeDeviceConfigurationFromCore(DeviceId deviceId, int cpu);

    /**
     * Returns the Rx filter methods supported by the different
     * devices that run the traffic class.
     *
     * @return traffic class's Rx filter methods across all devices
     */
    Map<DeviceId, Map<String, RxFilter>> rxFilterMethodsMap();

    /**
     * Returns the Rx filter methods supported by a specific
     * device that runs the traffic class.
     * These can be different per NIC.
     *
     * @param deviceId the NFV device to be queried
     * @return traffic class's Rx filter methods on a device
     */
    Map<String, RxFilter> rxFilterMethodsOfDevice(DeviceId deviceId);

    /**
     * Returns the Rx filter methods supported by a specific
     * NIC of a specific device.
     *
     * @param deviceId the NFV device to be queried
     * @param nic      the NIC of the NFV device
     * @return traffic class's Rx filter methods on a device
     */
    RxFilter rxFilterMethodOfDeviceOfNic(DeviceId deviceId, String nic);

    /**
     * Sets the Rx filter method on a specific device
     * for this traffic class.
     *
     * @param deviceId the NFV device to be configured
     * @param nic      the NIC of the NFV device
     * @param method   the Rx filter method to be set
     */
    void setRxFilterMethodOfDeviceOfNic(DeviceId deviceId, String nic, RxFilter method);

    /**
     * Returns a map of devices to NICs and the Rx filter values used
     * by each NIC for this traffic class.
     *
     * @return map of devices to Rx filter values
     */
    Map<DeviceId, Map<String, Set<RxFilterValue>>> rxFiltersMap();

    /**
     * Returns the map of NICs to Rx filter values used by this traffic class.
     *
     * @param deviceId the NFV device to be queried
     * @return map of NICs to Rx filter values used by this traffic class
     */
    Map<String, Set<RxFilterValue>> rxFiltersOfDevice(DeviceId deviceId);

    /**
     * Returns the set of Rx filter values used by this traffic class.
     *
     * @param deviceId the NFV device to be queried
     * @param nic           the NIC of the NFV device
     * @return set of Rx filter values used by this traffic class
     */
    Set<RxFilterValue> rxFiltersOfDeviceOfNic(DeviceId deviceId, String nic);

    /**
     * Associates a new Rx filter value to this traffic class.
     * This Rx filter has a device and a NIC.
     *
     * @param deviceId      the NFV device
     * @param nic           the NIC of the NFV device
     * @param rxFilterValue the value of this Rx filter
     */
    void addRxFilterToDeviceToNic(DeviceId deviceId, String nic, RxFilterValue rxFilterValue);

    /**
     * Removes an Rx filter from this traffic class.
     * This Rx filter belongs to a device and a NIC.
     *
     * @param deviceId the NFV device
     * @param nic           the NIC of the NFV device
     * @param rxFilterValue the value of this Rx filter
     */
    void removeRxFilterFromDeviceFromNic(DeviceId deviceId, String nic, RxFilterValue rxFilterValue);

    /**
     * Returns the number of NICs being used by this traffic class on a device.
     *
     * @param deviceId the NFV device to be queried
     * @return number of NICs
     */
    int numberOfNics(DeviceId deviceId);

    /**
     * Returns the number of CPUs being used by this traffic class on a device.
     *
     * @param deviceId the NFV device to be queried
     * @return number of CPUs
     */
    int numberOfCpus(DeviceId deviceId);

    /**
     * Updates the runtime information of this object
     * using additional information from another.
     *
     * @param other the object where we get updates from
     * @return updated TrafficClassRuntimeInfo
     */
    TrafficClassRuntimeInfo update(TrafficClassRuntimeInfo other);

}
