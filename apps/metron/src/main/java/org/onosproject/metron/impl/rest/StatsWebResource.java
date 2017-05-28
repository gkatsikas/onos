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

package org.onosproject.metron.impl.rest;

import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.structures.LruCache;

import org.onosproject.net.DeviceId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;

import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Metron statistics exposed via a REST API.
 */
@Path("/stats")
public class StatsWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(StatsWebResource.class);

    /**
     * The label of this component.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".rest.stats";
    private static final String COMPONET_LABEL = "REST Service";

    /**
     * Use the monitoring service to generate content for the users.
     */
    private final MonitorService monitoringService = get(MonitorService.class);

    /*********************************** Synthesis Delay. **********************************/

    /**
     * Get the synthesis delay of the registered service chains.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/synthesis_delay")
    public Response synthesisDelayMap() {
        log.info("[{}] [HTTP Get] Synthesis delay of all service chains", this.label());

        Map<ServiceChainId, Float> synthesisDelayMap =
            this.monitoringService.synthesisDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Float> entry : synthesisDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Float scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());
            scNode.put("synthesisDelay", scDelay);

            arrayNode.add(scNode);
        }

        root.set("synthesisDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the synthesis delay of a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/synthesis_delay/{scId}")
    public Response synthesisDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Synthesis delay of service chain {}", this.label(), scId);

        float synthDelay = this.monitoringService.synthesisDelayOfServiceChain(
            (ServiceChainId) ServiceChainId.id(scId)
        );
        if (synthDelay < 0) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);
        scNode.put("monitoringDelay", synthDelay);

        return Response.ok(scNode.toString()).build();
    }

    /********************************** Monitoring Delay. **********************************/

    /**
     * Get the monitoring delay of the registered service chains.
     * Time is reported in milliseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/monitoring_delay")
    public Response monitoringDelayMap() {
        log.info("[{}] [HTTP Get] Monitoring delay of all service chains", this.label());

        Map<ServiceChainId, Map<URI, LruCache<Float>>> monitoringDelayMap =
            this.monitoringService.monitoringDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Float>>> entry :
                monitoringDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Float>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Float>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode delArrayNode = mapper.createArrayNode();

                for (Float delay : scEntry.getValue().values()) {
                    delArrayNode.add(delay);
                }
                tcNode.put("monitoringDelay", delArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("monitoringDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the monitoring delay of the traffic classes
     * of a particular service chain.
     * Time is reported in milliseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/monitoring_delay/{scId}")
    public Response monitoringDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Monitoring delay of service chain {}", this.label(), scId);

        Map<URI, LruCache<Float>> scMonitoring =
            this.monitoringService.monitoringDelayOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Float>> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode delArrayNode = mapper.createArrayNode();

            for (Float delay : scEntry.getValue().values()) {
                delArrayNode.add(delay);
            }
            tcNode.put("monitoringDelay", delArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the monitoring delay of a particular traffic class
     * of a particular service chain.
     * Time is reported in milliseconds.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/monitoring_delay/{scId}/{tcId}")
    public Response monitoringDelayOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] Monitoring Delay of traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Float> tcMonitoring =
            this.monitoringService.monitoringDelayOfTrafficClass(sId, tId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode delArrayNode = mapper.createArrayNode();

        for (Float delay : tcMonitoring.values()) {
            delArrayNode.add(delay);
        }
        root.set("monitoringDelay", delArrayNode);

        return Response.ok(root.toString()).build();
    }

    /**************************** Offloading Computation Delay. ****************************/

    /**
     * Get the computational delay to offload the registered service chains.
     * This delay represents the time it takes to compute OpenFlow rules
     * for all registered service chains.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/comp_offloading_delay")
    public Response offloadingComputationDelayMap() {
        log.info("[{}] [HTTP Get] Computational delay to offload all service chains", this.label());

        Map<ServiceChainId, Map<URI, LruCache<Float>>> offloadingComputationDelayMap =
            this.monitoringService.offloadingComputationDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Float>>> entry :
                offloadingComputationDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Float>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Float>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode delArrayNode = mapper.createArrayNode();

                for (Float delay : scEntry.getValue().values()) {
                    delArrayNode.add(delay);
                }
                tcNode.put("offloadingComputationDelay", delArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("offloadingComputationDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the computational delay to offload the traffic classes
     * of a particular service chain.
     * This delay represents the time it takes to compute OpenFlow
     * rules for this service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/comp_offloading_delay/{scId}")
    public Response offloadingComputationDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Computational delay to offload service chain {}", this.label(), scId);

        Map<URI, LruCache<Float>> scOffloading =
            this.monitoringService.offloadingComputationDelayOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scOffloading == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Float>> scEntry : scOffloading.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode delArrayNode = mapper.createArrayNode();

            for (Float delay : scEntry.getValue().values()) {
                delArrayNode.add(delay);
            }
            tcNode.put("offloadingComputationDelay", delArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the computational delay to offload a particular traffic class
     * of a particular service chain.
     * This delay represents the time it takes to compute OpenFlow rules
     * for this traffic class.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/comp_offloading_delay/{scId}/{tcId}")
    public Response offloadingComputationDelayOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] Computational delay to offload traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Float> tcOffloading =
            this.monitoringService.offloadingComputationDelayOfTrafficClass(sId, tId);
        if (tcOffloading == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode delArrayNode = mapper.createArrayNode();

        for (Float delay : tcOffloading.values()) {
            delArrayNode.add(delay);
        }
        root.set("offloadingComputationDelay", delArrayNode);

        return Response.ok(root.toString()).build();
    }

    /**************************** Offloading Installation Delay. ***************************/

    /**
     * Get the installation delay to offload the registered service chains.
     * This delay represents the time it takes to install OpenFlow rules
     * for all registered service chains.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/inst_offloading_delay")
    public Response offloadingInstallationDelayMap() {
        log.info("[{}] [HTTP Get] Installation delay to offload all service chains", this.label());

        Map<ServiceChainId, LruCache<Float>> offloadingInstallationDelayMap =
            this.monitoringService.offloadingInstallationDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, LruCache<Float>> entry :
                offloadingInstallationDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            LruCache<Float> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode delArrayNode = mapper.createArrayNode();

            for (Float delay : scDelay.values()) {
                delArrayNode.add(delay);
            }
            scNode.put("offloadingInstallationDelay", delArrayNode);

            arrayNode.add(scNode);
        }

        root.set("offloadingInstallationDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the installation delay to offload the traffic classes
     * of a particular service chain.
     * This delay represents the time it takes to install OpenFlow
     * rules for this service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/inst_offloading_delay/{scId}")
    public Response offloadingInstallationDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Installation delay to offload service chain {}", this.label(), scId);

        LruCache<Float> scOffloading =
            this.monitoringService.offloadingInstallationDelayOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scOffloading == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode delArrayNode = mapper.createArrayNode();

        for (Float delay : scOffloading.values()) {
            delArrayNode.add(delay);
        }
        scNode.put("offloadingInstallationDelay", delArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /************************************ Launch Delay. ************************************/

    /**
     * Get the delay to launch the software part of each service chain.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/launch_delay")
    public Response launchDelayMap() {
        log.info("[{}] [HTTP Get] Delay to launch each service chain", this.label());

        Map<ServiceChainId, Map<URI, Long>> launchDelayMap =
            this.monitoringService.launchDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, Long>> entry :
                launchDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, Long> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, Long> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();
                Long delay = scEntry.getValue();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());
                tcNode.put("launchDelay", delay);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("launchDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the delay to launch the software-based traffic classes
     * of a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/launch_delay/{scId}")
    public Response launchDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Delay to launch service chain {}", this.label(), scId);

        Map<URI, Long> scMonitoring = this.monitoringService.launchDelayOfServiceChain(
            (ServiceChainId) ServiceChainId.id(scId)
        );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, Long> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();
            Long delay = scEntry.getValue();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());
            tcNode.put("launchDelay", delay);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the delay to launch a particular software-based traffic class
     * of a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/launch_delay/{scId}/{tcId}")
    public Response launchDelayOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] Delay to launch traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        long tcLaunchDelay = this.monitoringService.launchDelayOfTrafficClass(sId, tId);
        if (tcLaunchDelay <= 0) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("launchDelay", new Long(tcLaunchDelay));

        return Response.ok(root.toString()).build();
    }

    /************************** Data plane Reconfiguration Delay. **************************/

    /**
     * Get the delay to perform a server-level reconfiguration
     * for each registered service chain.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/agent_reconfiguration_delay")
    public Response agentReconfigurationDelayMap() {
        log.info(
            "[{}] [HTTP Get] Data plane reconfiguration delay of all service chains",
            this.label()
        );

        Map<ServiceChainId, Map<URI, LruCache<Float>>> agentReconfigurationDelayMap =
            this.monitoringService.agentReconfigurationDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Float>>> entry :
                agentReconfigurationDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Float>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Float>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode delArrayNode = mapper.createArrayNode();

                for (Float delay : scEntry.getValue().values()) {
                    delArrayNode.add(delay);
                }
                tcNode.put("agentReconfigurationDelay", delArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("agentReconfigurationDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the delay to perform a server-level reconfiguration
     * of the traffic classes of a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/agent_reconfiguration_delay/{scId}")
    public Response agentReconfigurationDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info(
            "[{}] [HTTP Get] Data plane reconfiguration delay of service chain {}",
            this.label(), scId
        );

        Map<URI, LruCache<Float>> scMonitoring =
            this.monitoringService.agentReconfigurationDelayOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Float>> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode delArrayNode = mapper.createArrayNode();

            for (Float delay : scEntry.getValue().values()) {
                delArrayNode.add(delay);
            }
            tcNode.put("agentReconfigurationDelay", delArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the delay to perform a server-level reconfiguration
     * of a particular traffic class of a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/agent_reconfiguration_delay/{scId}/{tcId}")
    public Response agentReconfigurationDelayOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info(
            "[{}] [HTTP Get] Data plane reconfiguration delay of traffic class {}",
            this.label(), tcId
        );

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Float> tcMonitoring =
            this.monitoringService.agentReconfigurationDelayOfTrafficClass(sId, tId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode delArrayNode = mapper.createArrayNode();

        for (Float delay : tcMonitoring.values()) {
            delArrayNode.add(delay);
        }
        root.set("agentReconfigurationDelay", delArrayNode);

        return Response.ok(root.toString()).build();
    }

    /**************************** Global Reconfiguration Delay. ****************************/

    /**
     * Get the delay to perform a control plane-driven reconfiguration
     * for each registered service chain.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/global_reconfiguration_delay")
    public Response globalReconfigurationDelayMap() {
        log.info(
            "[{}] [HTTP Get] Global reconfiguration delay of all service chains",
            this.label()
        );

        Map<ServiceChainId, LruCache<Float>> globalReconfigurationDelayMap =
            this.monitoringService.globalReconfigurationDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, LruCache<Float>> entry :
                globalReconfigurationDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            LruCache<Float> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode delArrayNode = mapper.createArrayNode();

            for (Float delay : scDelay.values()) {
                delArrayNode.add(delay);
            }
            scNode.put("globalReconfigurationDelay", delArrayNode);

            arrayNode.add(scNode);
        }

        root.set("globalReconfigurationDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the delay to perform a control plane-driven reconfiguration
     * for a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/global_reconfiguration_delay/{scId}")
    public Response globalReconfigurationDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info(
            "[{}] [HTTP Get] Global reconfiguration delay of service chain {}",
            this.label(), scId
        );

        LruCache<Float> recDelay =
            this.monitoringService.offloadingInstallationDelayOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (recDelay == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode delArrayNode = mapper.createArrayNode();

        for (Float delay : recDelay.values()) {
            delArrayNode.add(delay);
        }
        scNode.put("globalReconfigurationDelay", delArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /********************************* Enforcement Delay. **********************************/

    /**
     * Get the delay to enforce a reconfiguration to the registered service chains.
     * Time is reported in nanoseconds.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/enforcement_delay")
    public Response enforcementDelayMap() {
        log.info("[{}] [HTTP Get] Delay to enforce reconfiguration for each service chain", this.label());

        Map<ServiceChainId, Map<URI, LruCache<Float>>> enforcementDelayMap =
            this.monitoringService.enforcementDelayMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Float>>> entry :
                enforcementDelayMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Float>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Float>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode delArrayNode = mapper.createArrayNode();

                for (Float delay : scEntry.getValue().values()) {
                    delArrayNode.add(delay);
                }
                tcNode.put("enforcementDelay", delArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("enforcementDelayMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the delay to enforce a reconfiguration to a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/enforcement_delay/{scId}")
    public Response enforcementDelayOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Delay to enforce reconfiguration for service chain {}", this.label(), scId);

        Map<URI, LruCache<Float>> scMonitoring =
            this.monitoringService.enforcementDelayOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Float>> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode delArrayNode = mapper.createArrayNode();

            for (Float delay : scEntry.getValue().values()) {
                delArrayNode.add(delay);
            }
            tcNode.put("enforcementDelay", delArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the delay to enforce a reconfiguration to a particular
     * traffic class of a particular service chain.
     * Time is reported in nanoseconds.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/enforcement_delay/{scId}/{tcId}")
    public Response enforcementDelayOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] Delay to enforce reconfiguration for traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Float> tcMonitoring =
            this.monitoringService.enforcementDelayOfTrafficClass(sId, tId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode delArrayNode = mapper.createArrayNode();

        for (Float delay : tcMonitoring.values()) {
            delArrayNode.add(delay);
        }
        root.set("enforcementDelay", delArrayNode);

        return Response.ok(root.toString()).build();
    }

    /************************** CPU Utilization per Service Chain. *************************/

    /**
     * Get the CPU utilization ([0, 1]) of the registered service chains.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cpu_load_sc")
    public Response cpuLoadMap() {
        log.info("[{}] [HTTP Get] CPU utilization of all service chains", this.label());

        Map<ServiceChainId, Map<URI, LruCache<Float>>> cpuLoadMap =
            this.monitoringService.cpuLoadMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Float>>> entry :
                cpuLoadMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Float>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Float>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode loadArrayNode = mapper.createArrayNode();

                for (Float load : scEntry.getValue().values()) {
                    loadArrayNode.add(load);
                }
                tcNode.put("cpuLoad", loadArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("cpuLoadMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the CPU utilization ([0, 1]) of the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cpu_load_sc/{scId}")
    public Response cpuLoadOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] CPU utilization of service chain {}", this.label(), scId);

        Map<URI, LruCache<Float>> scMonitoring =
            this.monitoringService.cpuLoadOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Float>> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode loadArrayNode = mapper.createArrayNode();

            for (Float load : scEntry.getValue().values()) {
                loadArrayNode.add(load);
            }
            tcNode.put("cpuLoad", loadArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the CPU utilization ([0, 1]) of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cpu_load_sc/{scId}/{tcId}")
    public Response cpuLoadOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] CPU utilization of traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Float> tcMonitoring =
            this.monitoringService.cpuLoadOfTrafficClass(sId, tId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode loadArrayNode = mapper.createArrayNode();

        for (Float load : tcMonitoring.values()) {
            loadArrayNode.add(load);
        }
        root.set("cpuLoad", loadArrayNode);

        return Response.ok(root.toString()).build();
    }

    /******************************** CPU Cache Utilization. *******************************/

    /**
     * Get the number of CPU cache misses of the registered service chains.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cache_misses")
    public Response cacheMissesMap() {
        log.info("[{}] [HTTP Get] Number of CPU cache misses of all service chains", this.label());

        Map<ServiceChainId, Map<URI, LruCache<Integer>>> cacheMissesMap =
            this.monitoringService.cacheMissesMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Integer>>> entry :
                cacheMissesMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Integer>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Integer>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode missesArrayNode = mapper.createArrayNode();

                for (Integer misses : scEntry.getValue().values()) {
                    missesArrayNode.add(misses);
                }
                tcNode.put("cacheMisses", missesArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("cacheMissesMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the number of CPU cache misses of the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cache_misses/{scId}")
    public Response cacheMissesOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Number of CPU cache misses of service chain {}", this.label(), scId);

        Map<URI, LruCache<Integer>> scMonitoring =
            this.monitoringService.cacheMissesOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Integer>> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode missesArrayNode = mapper.createArrayNode();

            for (Integer misses : scEntry.getValue().values()) {
                missesArrayNode.add(misses);
            }
            tcNode.put("cacheMisses", missesArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the number of CPU cache misses of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cache_misses/{scId}/{tcId}")
    public Response cacheMissesOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] Number of CPU cache misses of traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Integer> tcMonitoring =
            this.monitoringService.cacheMissesOfTrafficClass(sId, tId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode missesArrayNode = mapper.createArrayNode();

        for (Integer misses : tcMonitoring.values()) {
            missesArrayNode.add(misses);
        }
        root.set("cacheMisses", missesArrayNode);

        return Response.ok(root.toString()).build();
    }

    /************************************ Packet Drops. ************************************/

    /**
     * Get the number of dropped packets of the registered service chains.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/dropped_packets")
    public Response droppedPacketsMap() {
        log.info("[{}] [HTTP Get] Number of dropped packets of all service chains", this.label());

        Map<ServiceChainId, Map<URI, LruCache<Integer>>> droppedPacketsMap =
            this.monitoringService.droppedPacketsMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<ServiceChainId, Map<URI, LruCache<Integer>>> entry :
                droppedPacketsMap.entrySet()) {
            ServiceChainId scId = entry.getKey();
            Map<URI, LruCache<Integer>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("serviceChainId", scId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<URI, LruCache<Integer>> scEntry : scDelay.entrySet()) {
                URI trafClassId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("traffiClassId", trafClassId.toString());

                ArrayNode dropsArrayNode = mapper.createArrayNode();

                for (Integer drops : scEntry.getValue().values()) {
                    dropsArrayNode.add(drops);
                }
                tcNode.put("droppedPackets", dropsArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("trafficClasses", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("droppedPacketsMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the number of dropped packets of the traffic classes
     * of a particular service chain.
     *
     * @param scId the ID of the service chain
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/dropped_packets/{scId}")
    public Response droppedPacketsOfServiceChain(@PathParam("scId") String scId) {
        log.info("[{}] [HTTP Get] Number of dropped packets of service chain {}", this.label(), scId);

        Map<URI, LruCache<Integer>> scMonitoring =
            this.monitoringService.droppedPacketsOfServiceChain(
                (ServiceChainId) ServiceChainId.id(scId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Service chain ID " + scId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("serviceChainId", scId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<URI, LruCache<Integer>> scEntry : scMonitoring.entrySet()) {
            URI trafClassId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("traffiClassId", trafClassId.toString());

            ArrayNode dropsArrayNode = mapper.createArrayNode();

            for (Integer drops : scEntry.getValue().values()) {
                dropsArrayNode.add(drops);
            }
            tcNode.put("droppedPackets", dropsArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("trafficClasses", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the number of dropped packets of a particular traffic class
     * of a particular service chain.
     *
     * @param scId the ID of the service chain
     * @param tcId the ID of the traffic class
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/dropped_packets/{scId}/{tcId}")
    public Response droppedPacketsOfTrafficClass(
            @PathParam("scId") String scId, @PathParam("tcId") String tcId) {
        log.info("[{}] [HTTP Get] Number of dropped packets of traffic class {}", this.label(), tcId);

        URI tId = uriFromString(tcId);
        if (tId == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Failed to create URI from traffic class ID " + tcId
            ).build();
        }

        ServiceChainId sId = (ServiceChainId) ServiceChainId.id(scId);

        LruCache<Integer> tcMonitoring =
            this.monitoringService.droppedPacketsOfTrafficClass(sId, tId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Traffic class ID " + tcId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode dropsArrayNode = mapper.createArrayNode();

        for (Integer drops : tcMonitoring.values()) {
            dropsArrayNode.add(drops);
        }
        root.set("droppedPackets", dropsArrayNode);

        return Response.ok(root.toString()).build();
    }

    /******************************* Active CPU Cores/Device. ******************************/

    /**
     * Get the number of active CPU cores of each NFV device.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/active_cores")
    public Response activeCoresPerDeviceMap() {
        log.info("[{}] [HTTP Get] Number of active CPU cores of each NFV device", this.label());

        Map<DeviceId, LruCache<Integer>> activeCoresPerDeviceMap =
            this.monitoringService.activeCoresPerDeviceMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<DeviceId, LruCache<Integer>> entry : activeCoresPerDeviceMap.entrySet()) {
            DeviceId deviceId = entry.getKey();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("deviceId", deviceId.toString());

            Integer cores = entry.getValue().getLastValue();
            scNode.put("activeCores", cores);

            arrayNode.add(scNode);
        }

        root.set("activeCoresPerDeviceMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the number of active CPU cores of a particular NFV device.
     *
     * @param deviceId the ID of NFV device
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/active_cores/{deviceId}")
    public Response activeCoresOfDevice(@PathParam("deviceId") String deviceId) {
        log.info("[{}] [HTTP Get] Number of active CPU cores of NFV device {}", this.label(), deviceId);

        LruCache<Integer> activeCores = this.monitoringService.activeCoresOfDevice(
            DeviceId.deviceId(deviceId)
        );
        if (activeCores == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Device ID " + deviceId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        Integer cores = activeCores.getLastValue();
        root.put("activeCores", cores);

        return Response.ok(root.toString()).build();
    }

    /****************************** CPU Utilization per Device. ****************************/

    /**
     * Get the CPU utilization ([0, 1]) of the registered devices.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cpu_load")
    public Response cpuLoadPerDeviceMap() {
        log.info("[{}] [HTTP Get] CPU utilization of all the devices", this.label());

        Map<DeviceId, Map<Integer, LruCache<Float>>> cpuLoadMap =
            this.monitoringService.cpuLoadPerDeviceMap();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Map.Entry<DeviceId, Map<Integer, LruCache<Float>>> entry :
                cpuLoadMap.entrySet()) {
            DeviceId deviceId = entry.getKey();
            Map<Integer, LruCache<Float>> scDelay = entry.getValue();

            ObjectNode scNode = mapper.createObjectNode();
            scNode.put("deviceId", deviceId.toString());

            ArrayNode scArrayNode = mapper.createArrayNode();

            for (Map.Entry<Integer, LruCache<Float>> scEntry : scDelay.entrySet()) {
                Integer cpuId = scEntry.getKey();

                ObjectNode tcNode = mapper.createObjectNode();
                tcNode.put("cpuId", cpuId);

                ArrayNode loadArrayNode = mapper.createArrayNode();

                for (Float load : scEntry.getValue().values()) {
                    loadArrayNode.add(load);
                }
                tcNode.put("cpuLoad", loadArrayNode);

                scArrayNode.add(tcNode);
            }

            scNode.put("cpus", scArrayNode);

            arrayNode.add(scNode);
        }

        root.set("cpuLoadMap", arrayNode);

        return Response.ok(root.toString()).build();
    }

    /**
     * Get the CPU utilization ([0, 1]) of a particular device.
     *
     * @param deviceId the ID of the device
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cpu_load/{deviceId}")
    public Response cpuLoadOfDevice(@PathParam("deviceId") String deviceId) {
        log.info("[{}] [HTTP Get] CPU utilization of device {}", this.label(), deviceId);

        Map<Integer, LruCache<Float>> scMonitoring =
            this.monitoringService.cpuLoadOfDevice(
                DeviceId.deviceId(deviceId)
            );
        if (scMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Device ID " + deviceId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scNode = mapper.createObjectNode();
        scNode.put("deviceId", deviceId);

        ArrayNode scArrayNode = mapper.createArrayNode();

        for (Map.Entry<Integer, LruCache<Float>> scEntry : scMonitoring.entrySet()) {
            Integer cpuId = scEntry.getKey();

            ObjectNode tcNode = mapper.createObjectNode();
            tcNode.put("cpuId", cpuId);

            ArrayNode loadArrayNode = mapper.createArrayNode();

            for (Float load : scEntry.getValue().values()) {
                loadArrayNode.add(load);
            }
            tcNode.put("cpuLoad", loadArrayNode);

            scArrayNode.add(tcNode);
        }

        scNode.put("cpus", scArrayNode);

        return Response.ok(scNode.toString()).build();
    }

    /**
     * Get the CPU utilization ([0, 1]) of a particular CPU core of a particular device.
     *
     * @param deviceId the ID of the device
     * @param cpuId the ID of the CPU core
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cpu_load/{deviceId}/{cpuId}")
    public Response cpuLoadOfDeviceOfCore(
            @PathParam("deviceId") String deviceId, @PathParam("cpuId") String cpuId) {
        log.info(
            "[{}] [HTTP Get] CPU utilization of CPU core {} of device {}",
            this.label(), cpuId, deviceId
        );

        int cId = Integer.parseInt(cpuId);
        if (cId < 0) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "Negative CPU core ID " + cId
            ).build();
        }

        DeviceId devId = DeviceId.deviceId(deviceId);

        LruCache<Float> tcMonitoring =
            this.monitoringService.cpuLoadOfDeviceOfCore(devId, cId);
        if (tcMonitoring == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                "CPU core ID " + cId + "not found"
            ).build();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode loadArrayNode = mapper.createArrayNode();

        root.put("cpuId", new Integer(cpuId));

        for (Float load : tcMonitoring.values()) {
            loadArrayNode.add(load);
        }
        root.put("cpuLoad", loadArrayNode);

        return Response.ok(root.toString()).build();
    }


    /********************************* End of Monitoring. ***********************************/

    /**
     * Generates and returns a URI out of an input string.
     * Serves for printing.
     *
     * @param uriStr the input string from where the URI is generated
     * @return URI generated from string
     */
    private URI uriFromString(String uriStr) {
        URI uri = null;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException sEx) {
            return null;
        }

        return uri;
    }

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
