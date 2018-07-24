/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.networkmonitor.impl.rest;

import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration;
import org.onosproject.networkmonitor.api.common.Constants;
import org.onosproject.networkmonitor.api.monitor.MonitorService;
import org.onosproject.networkmonitor.api.metrics.LoadMetric;

import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Network Monitor statistics exposed via a REST API.
 */
@Path("/networkmonitor")
public class StatsWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(StatsWebResource.class);

    /**
     * The label of this component.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".rest.stats";
    private static final String COMPONET_LABEL = "Network Monitor REST";

    /**
     * Use the monitoring service to generate content.
     */
    private final MonitorService monitoringService = get(MonitorService.class);

    /**
     * Default time window in seconds if not specified.
     */
    private static final int DEF_TIME_WINDOW_SEC = 300;

    /*********************************** Load Statistics. ***********************************/

    /**
     * Get the network load statistics in bits/packets per timestamp
     * on a given device.
     * The size of the load statistics is set to default
     * (i.e., last 60 timestamps).
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param attribute the load attribute (bits, packets, or drops)
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/load_stats/{databaseName}/{tableName}/{deviceId}/{attribute}/")
    public Response loadStatistics(
            @PathParam("databaseName")  String databaseName,
            @PathParam("tableName")     String tableName,
            @PathParam("deviceId")      String deviceId,
            @PathParam("attribute")     String attribute) {
        return loadStatisticsTimeLimited(
            databaseName, tableName, deviceId, attribute, DEF_TIME_WINDOW_SEC
        );
    }

    /**
     * Get the network load statistics in bits/packets per timestamp
     * on a given device for a certain time window.
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param attribute the load attribute (bits, packets, or drops)
     * @param timeWindowSec the time window in seconds
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/load_stats/{databaseName}/{tableName}/{deviceId}/{attribute}/{timeWindowSec}/")
    public Response loadStatisticsTimeLimited(
            @PathParam("databaseName")  String databaseName,
            @PathParam("tableName")     String tableName,
            @PathParam("deviceId")      String deviceId,
            @PathParam("attribute")     String attribute,
            @PathParam("timeWindowSec") int    timeWindowSec) {
        // Sanity check
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(deviceId),
            "Device ID is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(attribute) ||
            !DatabaseConfiguration.DEF_LOAD_TABLE_ATTRIBUTES.contains(attribute),
            "Invalid load table attribute"
        );
        checkArgument(
            timeWindowSec > 0,
            "Time window in seconds must be positive"
        );

        log.info(
            "[{}] [HTTP Get] Load statistics (in {} per time unit) on device: {}",
            this.label(), attribute, deviceId.toString()
        );

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        root.put("deviceId", deviceId);

        // Query the load on this device
        List<LoadMetric> loadStats = monitoringService.queryDeviceLoadTimeLimited(
            databaseName,
            tableName,
            DeviceId.deviceId(deviceId),
            timeWindowSec
        );

        // Sanity check
        long gottenNumberOfEntries = loadStats.size();
        long expectedNumberOfEntries = monitoringService.numberOfRecordsGivenTime(timeWindowSec);
        checkArgument(
            gottenNumberOfEntries == expectedNumberOfEntries,
            "Got " + gottenNumberOfEntries + " entries, but expected " + expectedNumberOfEntries
        );

        // Return an empty array if device not found or there is no load
        if ((loadStats == null) || (gottenNumberOfEntries == 0)) {
            root.put(attribute + "Stats", arrayNode);
        } else {
            monitoringService.printLoadMetrics(loadStats);

            for (LoadMetric lm : loadStats) {
                String value;

                if (attribute.equals("bits")) {
                    value = Long.toString(lm.bits());
                } else if (attribute.equals("packets")) {
                    value = Long.toString(lm.packets());
                } else {
                    value = Long.toString(lm.drops());
                }

                arrayNode.add(value);
            }

            root.put(attribute + "Stats", arrayNode);
        }

        return Response.ok(root.toString()).build();
    }

    /******************************** End of Load Statistics. *******************************/

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
