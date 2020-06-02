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

package org.onosproject.metron.server;

// Metron Libraries
import org.onosproject.metron.api.exceptions.ProtocolException;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.server.ServerService;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainScope;

// ONOS Libraries
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.drivers.server.BasicServerDriver;
import org.onosproject.drivers.server.behavior.MonitoringStatisticsDiscovery;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.nic.FlowRxFilterValue;
import org.onosproject.drivers.server.devices.nic.MacRxFilterValue;
import org.onosproject.drivers.server.devices.nic.MplsRxFilterValue;
import org.onosproject.drivers.server.devices.nic.RssRxFilterValue;
import org.onosproject.drivers.server.devices.nic.VlanRxFilterValue;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;

// Apache Libraries
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Other Libraries
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

// Java Libraries
import java.net.URI;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.ProcessingException;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.PARAM_AUTOSCALE;
import static org.onosproject.drivers.server.Constants.PARAM_CPUS;
import static org.onosproject.drivers.server.Constants.PARAM_CPUS_MAX;
import static org.onosproject.drivers.server.Constants.PARAM_ID;
import static org.onosproject.drivers.server.Constants.PARAM_NICS;
import static org.onosproject.drivers.server.Constants.PARAM_STATUS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_FILTER;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD_FLOW;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD_MAC;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD_MPLS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD_RSS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD_VLAN;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD_VALUES;
import static org.onosproject.drivers.server.Constants.SLASH;
import static org.onosproject.drivers.server.Constants.URL_BASE;
import static org.onosproject.metron.api.common.Constants.SYSTEM_PREFIX;
import static org.onosproject.metron.server.DefaultTrafficClassRuntimeInfo.AVAILABLE_CPU_CORE;

/**
 * Implementation of the service that establishes the communication
 * between the Metron controller (client-side) and the Metron agents (server-side).
 * The communication is implemented via JSON-based HTTP messages.
 */
@Component(immediate = true)
@Service
public class ServerManager implements ServerService {

    private static final Logger log = getLogger(ServerManager.class);

    /**
     * Application ID, name and label for the Server Manager.
     */
    private ApplicationId appId = null;
    private static final String APP_NAME = SYSTEM_PREFIX + ".drivers.server.manager";
    private static final String COMPONET_LABEL = "Server Manager";

    /**
     * Resource endpoints of the server agent.
     */
    private static final String URL_SERVICE_CHAINS_DEPLOY       = URL_BASE + "/service_chains";
    private static final String URL_SERVICE_CHAINS_RUNTIME_INFO = URL_SERVICE_CHAINS_DEPLOY; // + /ID
    private static final String URL_SERVICE_CHAINS_DELETE       = URL_SERVICE_CHAINS_DEPLOY; // + /ID

    /**
     * Resources to be asked/passed from/to the server agent.
     */
    private static final String PARAM_TITLE       = "serviceChains";

    private static final String PARAM_CONFIG_TYPE = "configType";
    private static final String PARAM_CONFIG      = "config";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RestSBController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MonitorService monitoringService;

    public ServerManager() {
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
    public boolean deviceExists(DeviceId deviceId) {
        return controller.getDevice(deviceId) != null;
    }

    @Override
    public RestSBDevice getDevice(DeviceId deviceId) {
        return controller.getDevice(deviceId);
    }

    @Override
    public TrafficClassRuntimeInfo deployTrafficClassOfServiceChain(
            DeviceId          deviceId,
            ServiceChainId    scId,
            URI               tcId,
            ServiceChainScope scScope,
            String            configurationType,
            String            configuration,
            Set<Integer>      newCpuSet,
            int               maxNumberOfCores,
            Set<String>       nicIds,
            boolean           autoscale) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcId,     "[" + label() + "] NULL traffic class ID.");
        checkArgument(!Strings.isNullOrEmpty(configuration),
            "[" + label() + "] Configuration is NULL or empty.");
        checkArgument(newCpuSet.size() > 0,
            "[" + label() + "] Number of cores must be positive.");
        checkArgument((maxNumberOfCores > 0) && (newCpuSet.size() <= maxNumberOfCores),
            "[" + label() + "] Maximum number of cores must be positive.");
        checkArgument((nicIds != null) && !nicIds.isEmpty(),
            "[" + label() + "] Cannot deploy traffic class without NICs.");

        log.info("[{}] ================================================================", label());
        log.info("[{}] Deploy", label());
        log.info("[{}] \t traffic class {}", label(), tcId);
        log.info("[{}] \t of service chain {}", label(), scId);
        log.info("[{}] \t with scope {}", label(), scScope.toString());
        log.info("[{}] \t on server {}", label(), deviceId);
        log.info("[{}] \t with configuration type {}", label(), configurationType);
        log.info("[{}] \t with configuration {}", label(), configuration);
        log.info("[{}] \t using {} CPU cores", label(), newCpuSet.size());
        log.info("[{}] \t with  {} maximum CPU cores", label(), maxNumberOfCores);
        log.info("[{}] \t using NIC(s) {}", label(), nicIds);
        log.info("[{}] \t with auto-scale {}", label(), autoscale ? "true" : "false");
        log.info("[{}] ================================================================", label());

        // TODO: Why always the first?
        String primaryNic = nicIds.iterator().next();
        RxFilter rxFilterMethod = null;
        boolean rxFilterFound = false;

        // Get the device's object from the controller
        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) controller.getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            return null;
        }

        if ((device == null) || (device.nics() == null)) {
            log.error("[{}] \t Failed to retrieve device {}", label(), deviceId);
            return null;
        }

        // Find the Rx filtering mechanism to be used
        for (String nicStr : nicIds) {
            for (NicDevice nic : device.nics()) {
                if (nic.name().equals(nicStr)) {
                    for (RxFilter rf : nic.rxFilterMechanisms().rxFilters()) {
                        // Pick the first supported Rx filter
                        if (RxFilter.isSupported(rf)) {
                            rxFilterMethod = rf;
                            rxFilterFound  = true;
                            break;
                        }
                    }
                }
            }

            if (rxFilterFound) {
                break;
            }
        }

        // The NICs of this device do not support
        if (!rxFilterFound || (rxFilterMethod == null)) {
            log.error("[{}] \t Unsupported Rx filter method on device {}", label(), deviceId);
            return null;
        }

        // Service chain's scope has to comply with the Rx filter method
        if (rxFilterMethod == RxFilter.FLOW) {
            checkArgument(
                scScope == ServiceChainScope.SERVER_RULES,
                "[" + label() + "] Metron agent advertized flow-based Rx filtering, " +
                "but the scope of service chain " + scId + " is not '" + ServiceChainScope.SERVER_RULES + "'."
            );
        } else if (rxFilterMethod == RxFilter.MAC) {
            checkArgument(
                scScope == ServiceChainScope.NETWORK_MAC,
                "[" + label() + "] Metron agent advertized MAC-based Rx filtering using VMDq, " +
                "but the scope of service chain " + scId + " is not '" + ServiceChainScope.NETWORK_MAC + "'."
            );
        } else if (rxFilterMethod == RxFilter.VLAN) {
            checkArgument(
                scScope == ServiceChainScope.NETWORK_MAC,
                "[" + label() + "] Metron agent advertized VLAN-based Rx filtering using VMDq, " +
                "but the scope of service chain " + scId + " is not '" + ServiceChainScope.NETWORK_VLAN + "'."
            );
        } else if (rxFilterMethod == RxFilter.MPLS) {
            checkArgument(
                false,
                "[" + label() + "] Metron agent advertized MPLS-based Rx filtering, " +
                "but the Metron controller does not currently support this type of tagging."
            );
        } else if (rxFilterMethod == RxFilter.RSS) {
            checkArgument(
                scScope == ServiceChainScope.SERVER_RSS,
                "[" + label() + "] Metron agent advertized RSS-based Rx filtering, " +
                "but the scope of service chain " + scId + " is not '" + ServiceChainScope.SERVER_RSS + "'."
            );
        }

        String rxFilterMethodStr = rxFilterMethod.toString();
        log.info("[{}] \t Supported Rx filter method {} on device {}", label(), rxFilterMethodStr, deviceId);

        ObjectMapper mapper = new ObjectMapper();

        // Create the object node to host the data
        ObjectNode sendObjNode = mapper.createObjectNode();

        // Create the object node to host this service chain
        ObjectNode chainObjNode = mapper.createObjectNode();

        // Add the service chain's traffic class ID
        chainObjNode.put(PARAM_ID, tcId.toString());

        // Add the Rx filter method
        ObjectNode rxFilterMethodNode = mapper.createObjectNode().put(
            PARAM_NIC_RX_METHOD, rxFilterMethodStr
        );
        chainObjNode.put(PARAM_NIC_RX_FILTER, rxFilterMethodNode);

        // Add the service chain's configuration type
        chainObjNode.put(PARAM_CONFIG_TYPE, configurationType);

        // Add the service chain's configuration
        chainObjNode.put(PARAM_CONFIG, configuration);

        // Add the list of CPUs
        ArrayNode cpuArrayNode = chainObjNode.putArray(PARAM_CPUS);
        for (Integer cpu : newCpuSet) {
            cpuArrayNode.add(cpu);
        }

        // Add an estimation of the maximum the number of CPUs you might need
        chainObjNode.put(PARAM_CPUS_MAX, Integer.toString(maxNumberOfCores));

        // Set the autoscale mode: If true, the agent can load balance itself
        chainObjNode.put(PARAM_AUTOSCALE, new Boolean(autoscale));

        // Add the list of NICs
        ArrayNode nicArrayNode = chainObjNode.putArray(PARAM_NICS);
        for (String nic : nicIds) {
            nicArrayNode.add(nic);
        }

        // Add the service chain into an array
        sendObjNode.putArray(PARAM_TITLE).add(chainObjNode);

        // Post the service chain
        int response = controller.post(
            deviceId, URL_SERVICE_CHAINS_DEPLOY,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        if (!BasicServerDriver.checkStatusCode(response)) {
            log.error(
                "[{}] \t Failed to deploy traffic class {} of service chain {} on device {} with status {}",
                label(), tcId, scId, deviceId, response);
            return null;
        }

        // Proceed to construct a runtime information object for this service chain
        return this.buildRuntimeInformation(
            deviceId, scId, tcId, primaryNic,
            configurationType, configuration, newCpuSet, maxNumberOfCores,
            nicIds, rxFilterMethodStr
        );
    }

    @Override
    public boolean reconfigureTrafficClassOfServiceChain(
            DeviceId       deviceId,
            ServiceChainId scId,
            URI            tcId,
            String         configurationType,
            String         configuration,
            Set<Integer>   newCpuMap,
            int            maxNumberOfCores) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcId,     "[" + label() + "] NULL traffic class ID.");

        log.info("[{}] \t ================================================================", label());
        log.info("[{}] \t Reconfigure", label());
        log.info("[{}] \t\t traffic class {} of", label(), tcId);
        log.info("[{}] \t\t service chain {} on", label(), scId);
        log.info("[{}] \t\t server {}", label(), deviceId);
        log.info("[{}] \t\t with {} configuration type",
            label(), configurationType == null ? "the same" : configurationType);
        log.info("[{}] \t\t with {} configuration",
            label(), configuration == null ? "the same" : configuration
        );
        log.info("[{}] \t\t using {} CPU cores",
            label(), newCpuMap.size()
        );
        log.info("[{}] \t\t with {} maximum CPU cores",
            label(), maxNumberOfCores < 0 ? "the same" : maxNumberOfCores
        );
        log.info("[{}] \t ================================================================", label());

        ObjectMapper mapper = new ObjectMapper();

        // Create the object node to host the data
        ObjectNode sendObjNode = mapper.createObjectNode();

        // Add the service chain's configuration type. Null means that it has not changed.
        if (configurationType != null) {
            sendObjNode.put(PARAM_CONFIG_TYPE, configurationType);
        }

        // Add the service chain's configuration. Null means that it has not changed.
        if (configuration != null) {
            sendObjNode.put(PARAM_CONFIG, configuration);
        }

        // Add the list of CPUs
        ArrayNode cpuArrayNode = sendObjNode.putArray(PARAM_CPUS);
        for (Integer cpu : newCpuMap) {
            cpuArrayNode.add(cpu);
        }

            // Add a new estimation of the maximum the number of CPUs. Negative means that it has not changed.
        if (maxNumberOfCores > 0) {
            sendObjNode.put(PARAM_CPUS_MAX, Integer.toString(maxNumberOfCores));
        }

        // Post the service chain
        String url = URL_SERVICE_CHAINS_DEPLOY + SLASH + tcId.toString();

        int response = controller.put(
            deviceId, url,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        if (!BasicServerDriver.checkStatusCode(response)) {
            log.error(
                "[{}] \t\t Failed to reconfigure traffic class {} of service chain {} on device {} with status {}",
                label(), tcId, scId, deviceId, response
            );
            return false;
        }

        return true;
    }

    @Override
    public TrafficClassRuntimeInfo updateTrafficClassRuntimeInfo(
            DeviceId                deviceId,
            ServiceChainId          scId,
            URI                     tcId,
            TrafficClassRuntimeInfo tcInfo) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcId,     "[" + label() + "] NULL traffic class ID.");

        log.info("[{}] ================================================================", label());
        log.info("[{}] Runtime information for", label());
        log.info("[{}] \t traffic class {} of", label(), tcId);
        log.info("[{}] \t service chain {}", label(), scId);
        log.info("[{}] ================================================================", label());

        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) controller.getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            return null;
        }
        if (device == null) {
            log.error("[{}] \t Failed to retrieve device {}", label(), deviceId);
            return null;
        }

        String scUrl = URL_SERVICE_CHAINS_RUNTIME_INFO + SLASH + tcId.toString();

        // Hit the path that provides the resources for this service chain
        InputStream response = null;
        try {
            response = controller.get(deviceId, scUrl, JSON);
        } catch (ProcessingException pEx) {
            log.error("[{}] \t Failed to retrieve runtime information for traffic class {} of service chain {}",
                label(), tcId, scId);
            return null;
        }

        // Load the JSON into objects
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode  jsonNode  = null;
        ObjectNode objNode  = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode  = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("[{}] \t Failed to retrieve runtime information for traffic class {} of service chain {}",
                label(), tcId, scId);
            return null;
        }
        checkNotNull(jsonMap, "[" + label() + "] Received NULL runtime information object");

        // Get the ID of the service chain
        String id = BasicServerDriver.get(jsonNode, PARAM_ID);

        // And verify that this is the traffic class we want to monitor
        if (!id.equals(tcId.toString())) {
            throw new ProtocolException("[" + label() + "] Failed to retrieve monitoring data for traffic class " +
                tcId + " of service chain " + scId + ". Traffic class chain ID does not agree.");
        }

        JsonNode    tagNode = objNode.path(PARAM_NIC_RX_FILTER);
        String    tagMethod = BasicServerDriver.get(tagNode, PARAM_NIC_RX_METHOD);

        JsonNode tagValNode = tagNode.path(PARAM_NIC_RX_METHOD_VALUES);
        String          nic = tcInfo.nicsOfDevice(deviceId).iterator().next();

        // Check if the Rx filter type conforms to what we have
        RxFilter supportedMethod = tcInfo.rxFilterMethodOfDeviceOfNic(deviceId, nic);
        if (!tagMethod.equals(supportedMethod.toString())) {
            throw new ProtocolException("[" + label() + "] Rx filter method for traffic class " + tcId +
                " of service chain " + scId + " does not agree with what the device reported.");
        }

        // Each NIC has a list of tags, one per CPU core
        Map<String, String> nicsToTags = mapper.convertValue(tagValNode, Map.class);
        for (Map.Entry<String, String> entry : nicsToTags.entrySet()) {
            String nicName = entry.getKey();
            JsonNode filterNode = tagValNode.path(nicName);

            // Convert the JSON list into a list of strings
            List<String> rxFilterValuesStr = null;
            try {
                rxFilterValuesStr = sortTags(
                    mapper.readValue(filterNode.traverse(), new TypeReference<TreeSet<String>>() { }));
            } catch (IOException ioEx) {
                throw new ProtocolException(
                    "[" + label() + "] Failed to retrieve the list of Rx filter values of traffic class " +
                    tcId + " of service chain " + scId + ".");
            }

            // Add the tags for this NIC
            addTag(deviceId, scId, tcId, tcInfo, nicName, tagMethod, rxFilterValuesStr);
        }

        String    configType = BasicServerDriver.get(jsonNode, PARAM_CONFIG_TYPE);
        String    config     = BasicServerDriver.get(jsonNode, PARAM_CONFIG);
        JsonNode cpuNode     = objNode.path(PARAM_CPUS);
        boolean   status     = objNode.path(PARAM_STATUS).asInt() == 1 ? true : false;
        JsonNode nicNode     = objNode.path(PARAM_NICS);

        // The service chain is expected to be active
        if (!status) {
            throw new ProtocolException("[" + label() + "] Traffic class " + tcId + " of service chain " + scId +
                " is inactive although it should have been active.");
        }

        // Convert the JSON list of CPUs into a set
        Set<Integer> cpuList = null;
        try {
            cpuList = mapper.readValue(cpuNode.traverse(), new TypeReference<TreeSet<Integer>>() { });
        } catch (IOException ioEx) {
            throw new ProtocolException("[" + label() + "] Failed to retrieve the list of CPUs of traffic class " +
                tcId + " of service chain " + scId + ".");
        }

        // Fetch the existing CPU core number associated with the configuration
        int currentCore = tcInfo.findCoreOfDeviceConfiguration(deviceId, config);

        /**
         * If core is negative, it means that there was no explicit core assignment
         * because the controller did not know which core would be chosen by the NFV agent.
         * Now, the NFV agent reported this core, so we can update.
         */
        if (currentCore < 0) {
            if (!tcInfo.removeDeviceConfigurationFromCore(deviceId, currentCore)) {
                throw new ProtocolException(
                    "[" + label() + "] Failed to remove old CPU configuration for traffic class " + tcId +
                    " of service chain " + scId + ".");
            }
        } else {
            if (!cpuList.contains(Integer.valueOf(currentCore))) {
                throw new ProtocolException(
                    "[" + label() + "] Inconsistent CPU configuration for traffic class " + tcId +
                    " of service chain " + scId + ".");
            }
        }

        // Update the CPU information of this traffic class
        for (Integer cpu : cpuList) {
            tcInfo.setDeviceConfigurationOfCore(deviceId, cpu.intValue(), config);
            tcInfo.setDeviceConfigurationTypeOfCore(deviceId, cpu.intValue(), configType);
        }

        // Convert the JSON list of NICs into a set of strings
        Set<String> nicsStr = null;
        try {
            nicsStr = mapper.readValue(nicNode.traverse(), new TypeReference<TreeSet<String>>() { });
        } catch (IOException ioEx) {
            throw new ProtocolException(
                "[" + label() + "] Failed to retrieve the list of NICs of traffic class " +
                tcId + " of service chain " + scId + ".");
        }

        // Get the difference between the stored NICs and the ones retrieved by the device
        Set<String> difference = Sets.difference(nicsStr, tcInfo.nicsOfDevice(deviceId));

        // These two must agree!
        if (difference.size() != 0) {
            throw new ProtocolException(
                "[" + label() + "] NICs for traffic class " + tcId +
                " of service chain " + scId + " do not agree with what the device reported.");
        }
        return tcInfo;
    }

    @Override
    public Set<MonitoringStatistics> getServiceChainMonitoringStats(
            DeviceId deviceId, ServiceChainId scId, Set<URI> tcIds) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcIds,    "[" + label() + "] NULL traffic class IDs.");

        Set<MonitoringStatistics> serviceChainStats = Sets.<MonitoringStatistics>newConcurrentHashSet();

        for (URI tcId : tcIds) {
            MonitoringStatistics tcStats = this.getTrafficClassMonitoringStats(deviceId, scId, tcId);
            if (tcStats == null) {
                // Graceful return to properly tear down the service
                log.error("[{}] Failed to retrieve monitoring data for traffic class {} of service chain {}",
                    label(), tcId, scId);
                continue;
            }
            serviceChainStats.add(tcStats);
        }

        return serviceChainStats;
    }

    @Override
    public MonitoringStatistics getTrafficClassMonitoringStats(
            DeviceId deviceId, ServiceChainId scId, URI tcId) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcId,     "[" + label() + "] NULL traffic class ID.");

        log.debug("[{}] ================================================================", label());
        log.debug("[{}] Monitoring stats for", label());
        log.debug("[{}] \t traffic class {} of", label(), tcId);
        log.debug("[{}] \t service chain {}", label(), scId);
        log.debug("[{}] ================================================================", label());

        Device device = findDeviceById(deviceId);

        if (device == null) {
            log.error("[{}] \t Failed to retrieve device {}", label(), deviceId);
            return null;
        }

        MonitoringStatistics trafficClassStats = null;
        if (device.is(MonitoringStatisticsDiscovery.class)) {
            MonitoringStatisticsDiscovery monStatisticsDiscovery = device.as(
                MonitoringStatisticsDiscovery.class
            );
            trafficClassStats = monStatisticsDiscovery.discoverMonitoringStatistics(tcId);
        } else {
            log.debug("No monitoring statistics behaviour for device {}", deviceId);
            return null;
        }

        if (trafficClassStats == null) {
            return null;
        }

        // Store the total time to launch this traffic class
        monitoringService.updateLaunchDelayOfTrafficClass(
            scId,
            tcId,
            trafficClassStats.timingStatistics().totalDeploymentTime()
        );

        // Store the time required by the NFV agent to autoscale
        monitoringService.updateAgentReconfigurationDelayOfTrafficClass(
            scId,
            tcId,
            (float) trafficClassStats.timingStatistics().autoScaleTime()
        );

        log.debug("[{}] {}", label(), trafficClassStats.toString());
        log.debug("[{}] ================================================================", label());

        return trafficClassStats;
    }

    @Override
    public MonitoringStatistics getGlobalMonitoringStats(DeviceId deviceId) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");

        log.info("[{}] ================================================================", label());
        log.info("[{}] Global Monitoring on", label());
        log.info("[{}] \t device {}", label(), deviceId.toString());
        log.info("[{}] ================================================================", label());

        Device device = findDeviceById(deviceId);

        if (device == null) {
            log.error("[{}] \t Failed to retrieve device {}", label(), deviceId);
            return null;
        }

        MonitoringStatistics globalStats = null;
        if (device.is(MonitoringStatisticsDiscovery.class)) {
            MonitoringStatisticsDiscovery monStatisticsDiscovery = device.as(
                MonitoringStatisticsDiscovery.class
            );
            globalStats = monStatisticsDiscovery.discoverGlobalMonitoringStatistics();
        } else {
            log.debug("No monitoring statistics behaviour for device {}", deviceId);
            return null;
        }

        log.debug("[{}] {}", label(), globalStats.toString());
        log.debug("[{}] ================================================================", label());

        return globalStats;
    }

    @Override
    public boolean deleteServiceChain(DeviceId deviceId, ServiceChainId scId, Set<URI> tcIds) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcIds,    "[" + label() + "] NULL traffic class IDS.");

        for (URI tcId : tcIds) {
            if (!this.deleteTrafficClassOfServiceChain(deviceId, scId, tcId)) {
                throw new ProtocolException("[" + label() + "] Failed to delete traffic class " + tcId +
                    " of service chain " + scId + " from device " + deviceId);
            }
        }

        return true;
    }

    @Override
    public boolean deleteTrafficClassOfServiceChain(
            DeviceId deviceId, ServiceChainId scId, URI tcId) {
        checkNotNull(deviceId, "[" + label() + "] NULL device ID.");
        checkNotNull(scId,     "[" + label() + "] NULL service chain ID.");
        checkNotNull(tcId,     "[" + label() + "] NULL traffic class ID.");

        log.info("[{}] ================================================================", label());
        log.info("[{}] Delete", label());
        log.info("[{}] \t traffic class {} of", label(), tcId);
        log.info("[{}] \t service chain {}", label(), scId);
        log.info("[{}] ================================================================", label());

        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) controller.getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            return false;
        }

        if (device == null) {
            log.error("[{}] \t Failed to retrieve device {}", label(), deviceId);
            return false;
        }

        String scUrl = URL_SERVICE_CHAINS_DELETE + SLASH + tcId.toString();

        // Delete this traffic class
        int response = controller.delete(deviceId, scUrl, null, JSON);

        if (!BasicServerDriver.checkStatusCode(response)) {
            log.error("[{}] \t Failed to delete traffic class {} of service chain {} on device {} with status {}",
                label(), tcId, scId, deviceId, response);
            return false;
        }

        return true;
    }

    @Override
    public TrafficClassRuntimeInfo buildRuntimeInformation(
            DeviceId       deviceId,
            ServiceChainId scId,
            URI            tcId,
            String         primaryNic,
            String         configurationType,
            String         configuration,
            Set<Integer>   newCpuSet,
            int            maxNumberOfCores,
            Set<String>    nicIds,
            String         rxFilterMethodStr) {
        // Devices that run this service chain
        Set<DeviceId> devices = Sets.<DeviceId>newConcurrentHashSet();
        devices.add(deviceId);

        // Build the runtime information object
        TrafficClassRuntimeInfo rtInfo = new DefaultTrafficClassRuntimeInfo(
            scId, tcId, primaryNic, maxNumberOfCores, devices
        );

        // Add cores
        rtInfo.setCoresOfDevice(deviceId, newCpuSet);

        // Add NICs
        rtInfo.setNicsOfDevice(deviceId, nicIds);

        /**
         * Add configuration. We set core to -1 because we do not know
         * which core will be used by the NFV agent.
         * The agent will let us know once the chain is deployed.
         */
        rtInfo.setDeviceConfigurationTypeOfCore(deviceId, AVAILABLE_CPU_CORE, configurationType);
        rtInfo.setDeviceConfigurationOfCore(deviceId, AVAILABLE_CPU_CORE, configuration);

        // Set the Rx filter method of this device
        if (rxFilterMethodStr != null) {
            for (String nic : nicIds) {
                rtInfo.setRxFilterMethodOfDeviceOfNic(deviceId, nic, RxFilter.getByName(rxFilterMethodStr));
            }
        }

        return rtInfo;
    }

    /**
     * Adds tags to a service chain's traffic class information.
     *
     * @param deviceId the device ID where the tags are applied
     * @param scId the service chain's ID
     * @param tcId the service chain's traffic class ID
     * @param tcInfo the service chain's traffic class information
     * @param nicName the NIC where the tags are applied
     * @param tagMethod the Rx filter methodo of this service chain
     * @param rxFilterValuesStr a list of Rx filter values to add
     */
    private void addTag(
            DeviceId deviceId, ServiceChainId scId, URI tcId, TrafficClassRuntimeInfo tcInfo,
            String nicName, String tagMethod, List<String> rxFilterValuesStr) {
        int coreId = 0;
        for (String tagValue : rxFilterValuesStr) {
            if (Strings.isNullOrEmpty(tagValue)) {
                continue;
            }

            if (tagMethod.equals(PARAM_NIC_RX_METHOD_MAC)) {
                MacAddress mac = MacAddress.valueOf(tagValue);
                tcInfo.addRxFilterToDeviceToNic(deviceId, nicName, new MacRxFilterValue(mac, coreId));
            } else if (tagMethod.equals(PARAM_NIC_RX_METHOD_MPLS)) {
                MplsLabel mplsLabel = MplsLabel.mplsLabel(tagValue);
                tcInfo.addRxFilterToDeviceToNic(deviceId, nicName, new MplsRxFilterValue(mplsLabel, coreId));
            } else if (tagMethod.equals(PARAM_NIC_RX_METHOD_VLAN)) {
                VlanId vlanId = VlanId.vlanId(tagValue);
                tcInfo.addRxFilterToDeviceToNic(deviceId, nicName, new VlanRxFilterValue(vlanId, coreId));
            } else if (tagMethod.equals(PARAM_NIC_RX_METHOD_FLOW)) {
                long physCoreId = Long.parseLong(tagValue);
                tcInfo.addRxFilterToDeviceToNic(deviceId, nicName, new FlowRxFilterValue(physCoreId, coreId));
            } else if (tagMethod.equals(PARAM_NIC_RX_METHOD_RSS)) {
                int physCoreId = Integer.parseInt(tagValue);
                tcInfo.addRxFilterToDeviceToNic(deviceId, nicName, new RssRxFilterValue(physCoreId, coreId));
            } else {
                throw new ProtocolException("[" + label() + "] Unsupported Rx filter method for traffic class " +
                    tcId + " of service chain " + scId + ".");
            }

            log.info("[{}] \t NIC {} - Tag {} Core {}", label(), nicName, tagValue, coreId);
            coreId++;
        }
    }

    /**
     * Sorts tags numerically.
     *
     * @param tagSet set of tags to sort
     * @return a list of sorted tags
     */
    private List<String> sortTags(Set<String> tagSet) {
        Map<Integer, String> map = new HashMap<>();
        Set<Integer> sortedListInt = new TreeSet<Integer>();

        for (String tag : tagSet) {
            int intTag = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
            map.put(intTag, tag);
            sortedListInt.add(intTag);
        }

        List<String> sortedList = new ArrayList<String>(tagSet.size());

        int index = 0;
        for (Integer i : sortedListInt) {
            sortedList.add(index++, map.get(i.intValue()));
        }

        return sortedList;
    }

    /**
     * Returns a device object with specific identity.
     *
     * @param deviceId the ID of the desired device
     * @return device object or null
     */
    private Device findDeviceById(DeviceId deviceId) {
        return deviceService.getDevice(deviceId);
    }

    /**
     * Returns a label with the Server Manager's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}
