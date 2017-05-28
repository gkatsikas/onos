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

import org.onosproject.drivers.server.stats.MonitoringStatistics;

import org.onosproject.net.DeviceId;
import org.onosproject.protocol.rest.RestSBDevice;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Service that prescribes the communication between
 * the ONOS controller and a Metron server.
 */
public interface ServerService {

    /**
     * Returns the available devices.
     *
     * @return map of device IDs to devices
     */
    Map<DeviceId, RestSBDevice> discoverDevices();

    /**
     * Returns whether a device ID belongs to
     * an active server or not.
     *
     * @param deviceId the device ID to be checked
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
     * Queries the device about their available features.
     *
     * @param deviceId the device ID to be discovered
     * @return NfvDeviceDescription object with NFV device features
     */
    RestSBDevice discoverDeviceFeatures(DeviceId deviceId);

    /**
     * Instructs the device to deploy a traffic class of a service chain.
     *
     * @param deviceId the server where the deployment takes place
     * @param scId the ID of the service chain
     * @param tcId the ID of the service chain's traffic class
     * @param configurationType the type of the target packet processing element
     *        Can be Click-based (click) or a blackbox NF (standalone)
     * @param configuration the packet processing instructions as a string
     * @param numberOfCores the number of CPU cores to be used for this traffic class
     * @param maxNumberOfCores estimation of the maximum the number of CPUs you might need
     * @param nicIds the IDs of the NICs that participate in the processing
     * @param autoscale allows the agent to handle load imbalances autonomously
     * @return TrafficClassRuntimeInfo a description of the runtime information
     *         of this traffic class
     */
    TrafficClassRuntimeInfo deployTrafficClassOfServiceChain(
        DeviceId       deviceId,
        ServiceChainId scId,
        URI            tcId,
        String         configurationType,
        String         configuration,
        int            numberOfCores,
        int            maxNumberOfCores,
        Set<String>    nicIds,
        boolean        autoscale
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
     * @param numberOfCores the new number of CPU cores to be used for this traffic class
     * @param maxNumberOfCores the new maximum number of CPUs you need
     * @return boolean reconfiguration status
     */
    boolean reconfigureTrafficClassOfServiceChain(
        DeviceId       deviceId,
        ServiceChainId scId,
        URI            tcId,
        String         configurationType,
        String         configuration,
        int            numberOfCores,
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
     * @param numberOfCores the number of CPU cores to be used for this traffic class
     * @param maxNumberOfCores estimation of the maximum the number of CPUs you might need
     * @param nicIds the IDs of the NICs that participate in the processing
     * @param rxFilterMethod tagging method supported by the NIC of this service chain
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
            int            numberOfCores,
            int            maxNumberOfCores,
            Set<String>    nicIds,
            String         rxFilterMethod
    );

}
