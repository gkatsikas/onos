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

package org.onosproject.metron.impl.gui;

// Metron libraries
import org.onosproject.metron.api.structures.LruCache;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.orchestrator.OrchestrationService;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

// ONOS libraries
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.chart.ChartModel;
import org.onosproject.ui.chart.ChartRequestHandler;

// Other libraries
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;

// Java libraries
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.metron.impl.gui.MetricType.SYNTHESIS_TIME;
import static org.onosproject.metron.impl.gui.MetricType.DEPLOYMENT_TIME;
import static org.onosproject.metron.impl.gui.MetricType.MONITORING_TIME;
import static org.onosproject.metron.impl.gui.MetricType.RECONFIGURATION_TIME;
import static org.onosproject.metron.impl.gui.Resource.SERVICE_CHAIN_DEPL_METRICS;

/**
 * Message handler for passing service chain stats to the Web UI.
 */
public class ServiceChainStatsViewMessageHandler extends UiMessageHandler {

    private static final Logger log = getLogger(ServiceChainStatsViewMessageHandler.class);

    private static final String SC_STATS_DATA_REQ = "scstatDataRequest";
    private static final String SC_STATS_DATA_RESP = "scstatDataResponse";
    private static final String SC_STATS = "scstats";

    private static final String SERVICE_CHAIN_IDS = "serviceChainIds";

    // Conversions
    private static final float NS_TO_US = (float) Math.pow(10, 3);
    private static final float MS_TO_US = (float) Math.pow(10, 3);

    // Data series
    private static final int NUM_OF_DATA_POINTS = 30;

    // Time axis
    private static final String TIME_FORMAT = "HH:mm:ss";
    private long timestamp = 0L;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ControlMessageRequest()
        );
    }

    private final class ControlMessageRequest extends ChartRequestHandler {

        private ControlMessageRequest() {
            super(SC_STATS_DATA_REQ, SC_STATS_DATA_RESP, SC_STATS);
        }

        @Override
        protected String[] getSeries() {
            return SERVICE_CHAIN_DEPL_METRICS.stream().map(type ->
                StringUtils.lowerCase(type.name())).toArray(String[]::new);
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            String uri = string(payload, "scId");

            OrchestrationService orchestrator = get(OrchestrationService.class);
            MonitorService monitoringService = get(MonitorService.class);

            // Project only one service chain
            if (!Strings.isNullOrEmpty(uri)) {
                // Get the ID of this service chain
                ServiceChainId scId = (ServiceChainId) ServiceChainId.id(uri);

                Map<MetricType, Float[]> metricsList = this.fetchMetricsOverTime(monitoringService, scId);
                // Only for devices that report CPU statistics
                if (metricsList != null) {
                    // Generate a timestamp
                    LocalDateTime ldt = new LocalDateTime(timestamp);

                    populateMetrics(cm, metricsList, ldt, NUM_OF_DATA_POINTS);

                    Set<ServiceChainId> scIds = Sets.newHashSet();
                    for (ServiceChainInterface sc : orchestrator.activeServiceChains()) {
                        scIds.add(sc.id());
                    }

                    // Drop down list to select service chains
                    attachServiceChainList(cm, scIds);
                }

            } else {
                // Get them all
                for (ServiceChainInterface sc : orchestrator.activeServiceChains()) {
                    ServiceChainId scId  = sc.id();
                    String scIdStr = scId.toString();

                    // Synthesis time
                    float synthesisTime = this.fetchSynthesisTime(monitoringService, scId);

                    // Deployment time
                    float deploymentTime = this.fetchAndComputeDeploymentTime(monitoringService, scId);

                    // Monitoring time
                    float monitoringTime = this.fetchMonitoringTime(monitoringService, scId);

                    // Monitoring time
                    float reconfigurationTime = this.fetchAndComputeReconfigurationTime(monitoringService, scId);

                    // Memory where the data will be stored
                    Map<String, Object> local = Maps.newHashMap();

                    // Report the time statistics
                    local.put(StringUtils.lowerCase(SYNTHESIS_TIME.name()),  new Float(synthesisTime));
                    local.put(StringUtils.lowerCase(DEPLOYMENT_TIME.name()), new Float(deploymentTime));
                    local.put(StringUtils.lowerCase(MONITORING_TIME.name()), new Float(monitoringTime));
                    local.put(StringUtils.lowerCase(RECONFIGURATION_TIME.name()), new Float(reconfigurationTime));

                    // Last piece of data is the service chain ID
                    local.put(LABEL, scIdStr);

                    // Send this data
                    populateMetric(cm.addDataPoint(scIdStr), local);
                }
            }
        }

        /**
         * Fetch the synthesis time from the monitoring service.
         *
         * @param monitoringService the monitoring service
         * @param scId the ID of the service chain that we monitor
         * @return synthesis delay
         */
        private float fetchSynthesisTime(MonitorService monitoringService, ServiceChainId scId) {
            float synthesisTime = monitoringService.synthesisDelayOfServiceChain(scId);

            // Consistency check
            checkArgument(synthesisTime > 0, "Invalid synthesis time for service chain: " + scId);

            // Converted to microseconds
            return synthesisTime / NS_TO_US;
        }

        /**
         * Fetch the deployment time from the monitoring service.
         * This time consists of multiple delay components that we need to add up.
         *
         * @param monitoringService the monitoring service
         * @param scId the ID of the service chain that we monitor
         * @return deployment delay
         */
        private float fetchAndComputeDeploymentTime(MonitorService monitoringService, ServiceChainId scId) {
            float deploymentTime = 0;

            // Delay to compute the HW rules
            Map<URI, LruCache<Float>> tcOfflCompTime =
                monitoringService.offloadingComputationDelayOfServiceChain(scId);
            deploymentTime += this.getListDelayAverageAcrossTrafficClasses(tcOfflCompTime, (float) 1);

            // Delay to install the HW rules
            LruCache<Float> tcOfflInstTime =
                monitoringService.offloadingInstallationDelayOfServiceChain(scId);
            deploymentTime += this.getListAverage(tcOfflInstTime);

            // Delay to launch the software part of the service chain
            Map<URI, Long> tcLaunchTime = monitoringService.launchDelayOfServiceChain(scId);
            deploymentTime += this.getDelayAverageAcrossTrafficClasses(tcLaunchTime);

            // Consistency check
            checkArgument(deploymentTime > 0, "Invalid deployment time for service chain: " + scId);

            // Total delay coverted to microseconds
            return deploymentTime / NS_TO_US;
        }

        /**
         * Fetch the monitoring time from the monitoring service.
         *
         * @param monitoringService the monitoring service
         * @param scId the ID of the service chain that we monitor
         * @return monitoring delay
         */
        private float fetchMonitoringTime(MonitorService monitoringService, ServiceChainId scId) {
            float monitoringTime = 0;

            Map<URI, LruCache<Float>> tcMonTime =
                monitoringService.monitoringDelayOfServiceChain(scId);

            // This might happen when the service chain is just deployed
            if (tcMonTime == null) {
                return monitoringTime;
            }

            // This delay is reported in milliseconds
            monitoringTime += this.getListDelayAverageAcrossTrafficClasses(tcMonTime, (float) 1);

            // Consistency check
            checkArgument(monitoringTime > 0, "Invalid monitoring time for service chain: " + scId);

            // Converted to microseconds
            return monitoringTime * MS_TO_US;
        }

        /**
         * Fetch the reconfiguration time from the monitoring service.
         * This time consists of multiple delay components that we need to add up.
         *
         * @param monitoringService the monitoring service
         * @param scId the ID of the service chain that we monitor
         * @return reconfiguration delay
         */
        private float fetchAndComputeReconfigurationTime(MonitorService monitoringService, ServiceChainId scId) {
            float reconfigurationTime = 0;

            // Delay to perform a global (controller-driven) reconfiguration
            LruCache<Float> tcGlobalReconfTime =
                monitoringService.globalReconfigurationDelayOfServiceChain(scId);
            reconfigurationTime += this.getListAverage(tcGlobalReconfTime);

            // Delay to enforce the new decision to the data plane
            Map<URI, LruCache<Float>> tcEnforcTime = monitoringService.enforcementDelayOfServiceChain(scId);
            float enfTimeNs = this.getListDelayAverageAcrossTrafficClasses(tcEnforcTime, (float) 1);
            reconfigurationTime += enfTimeNs;

            // Consistency check. This time can be zero if no load balancing decision has been made
            checkArgument(reconfigurationTime >= 0, "Invalid reconfiguration time for service chain: " + scId);

            // Total delay coverted to microseconds
            return reconfigurationTime / NS_TO_US;
        }

        /**
         * Fetch the metrics from the monitoring service over time.
         * Only monitoring data can actually change over time.
         *
         * @param monitoringService the monitoring service
         * @param scId the ID of the service chain that we monitor
         * @return map of metrics to their list of delays
         */
        private Map<MetricType, Float[]> fetchMetricsOverTime(
                MonitorService monitoringService, ServiceChainId scId) {
            Map<MetricType, Float[]> data = Maps.newHashMap();

            // Synthesis time
            float[] synDelays = new float[1];
            synDelays[0] = this.fetchSynthesisTime(monitoringService, scId);
            float[] filledSynDelays = fillData(synDelays, NUM_OF_DATA_POINTS);
            data.put(SYNTHESIS_TIME, ArrayUtils.toObject(filledSynDelays));

            // Deployment time
            float[] depDelays = new float[1];
            depDelays[0] = this.fetchAndComputeDeploymentTime(monitoringService, scId);
            float[] filledDepDelays = fillData(depDelays, NUM_OF_DATA_POINTS);
            data.put(DEPLOYMENT_TIME, ArrayUtils.toObject(filledDepDelays));

            // Monitoring time
            Map<URI, LruCache<Float>> tcMonTime =
                monitoringService.monitoringDelayOfServiceChain(scId);
            float[] monDelays = this.getDelayArrayAcrossTrafficClasses(tcMonTime, NUM_OF_DATA_POINTS, MS_TO_US);
            float[] filledMonDelays = fillData(monDelays, NUM_OF_DATA_POINTS);
            data.put(MONITORING_TIME, ArrayUtils.toObject(filledMonDelays));

            // Global reconfiguration time
            float[] globReconfDelays = new float[NUM_OF_DATA_POINTS];
            LruCache<Float> tcGlobalReconfTime =
                monitoringService.globalReconfigurationDelayOfServiceChain(scId);
            if (tcGlobalReconfTime != null) {
                List<Float> tcEntries = tcGlobalReconfTime.getLastValues(NUM_OF_DATA_POINTS);
                int index = NUM_OF_DATA_POINTS - 1;
                for (int i = tcEntries.size() - 1; i >= 0; i--) {
                    globReconfDelays[index] = tcEntries.get(i).floatValue() / NS_TO_US;
                    index--;
                }
            }

            // Enforcement time
            float[] enfDelays = null;
            Map<URI, LruCache<Float>> tcEnforcTime =
                monitoringService.enforcementDelayOfServiceChain(scId);
            if (tcEnforcTime != null) {
                float[] tcEnfDelays = this.getDelayArrayAcrossTrafficClasses(
                    tcEnforcTime, NUM_OF_DATA_POINTS, 1 / NS_TO_US
                );
                enfDelays = fillData(tcEnfDelays, NUM_OF_DATA_POINTS);
            } else {
                enfDelays = new float[NUM_OF_DATA_POINTS];
            }

            // Final
            float[] finalReconfDelay = this.addArrays(globReconfDelays, enfDelays);
            data.put(RECONFIGURATION_TIME, ArrayUtils.toObject(finalReconfDelay));

            // Keep a timestamp
            timestamp = System.currentTimeMillis();

            return data;
        }

        /**
         * Returns the total delay component as a result of internal, individual delays.
         * These internal delays are kept in lists, out of which we compute the average delay.
         *
         * @param delayMap the map of list of delays
         * @param scale factor to scale the data
         * @return total delay component
         */
        private float getListDelayAverageAcrossTrafficClasses(Map<URI, LruCache<Float>> delayMap, float scale) {
            float totalTime = 0;

            if (delayMap == null) {
                return totalTime;
            }

            for (Map.Entry<URI, LruCache<Float>> entry : delayMap.entrySet()) {
                URI tc = entry.getKey();
                LruCache<Float> delayList = entry.getValue();

                // Find the average delay of this traffic class
                float avg = this.getListAverage(delayList);

                // Add it to the total delay
                totalTime += avg;
            }

            return totalTime * scale;
        }

        /**
         * Returns the total delay component as a result of internal, individual delays.
         *
         * @param delayMap the map of delays
         * @return total delay component
         */
        private float getDelayAverageAcrossTrafficClasses(Map<URI, Long> delayMap) {
            float totalTime = 0;

            if (delayMap == null) {
                return totalTime;
            }

            for (Map.Entry<URI, Long> entry : delayMap.entrySet()) {
                URI tc = entry.getKey();
                Long delay = entry.getValue();

                totalTime += (float) delay;
            }

            return totalTime;
        }

        /**
         * Returns an array of the most recent delays out of the delay map.
         *
         * @param delayMap the map of list of delays
         * @param expectedSize the expected size of the array
         * @param scale the expected scale of the data
         * @return array of latest delay values
         */
        private float[] getDelayArrayAcrossTrafficClasses(
                Map<URI, LruCache<Float>> delayMap,
                int   expectedSize,
                float scale) {
            float[] delayArray = new float[expectedSize];

            if (delayMap == null) {
                return delayArray;
            }

            int delayMapSize = delayMap.size();

            if (delayMapSize == 0) {
                return delayArray;
            }

            int entriesPerTc = expectedSize / delayMap.size();

            int index = 0;
            for (Map.Entry<URI, LruCache<Float>> entry : delayMap.entrySet()) {
                URI tc = entry.getKey();
                LruCache<Float> delayList = entry.getValue();

                List<Float> tcEntries = delayList.getLastValues(entriesPerTc);

                for (Float f : tcEntries) {
                    delayArray[index] = f.floatValue() * scale;
                    index++;
                }
            }

            return delayArray;
        }

        /**
         * Returns the average value of a list.
         *
         * @param delayList the list of delays
         * @return average value of a list
         */
        private float getListAverage(LruCache<Float> delayList) {
            float avg = 0;

            if (delayList == null) {
                return avg;
            }

            float delayListSize = delayList.values().size();

            if (delayListSize == 0) {
                return avg;
            }

            for (Float value : delayList.values()) {
                avg += value.floatValue();
            }

            return avg / delayListSize;
        }

        /**
         * Fill the contents of an input array until a desired point.
         *
         * @param origin the original array with the data
         * @param expectedLength the desired length of the array
         * @return an array of a certain length
         */
        private float[] fillData(float[] origin, int expectedLength) {
            if (origin.length == expectedLength) {
                return origin;
            } else {
                int desiredLength = origin.length;
                if (origin.length > expectedLength) {
                    desiredLength = expectedLength;
                }

                float[] filled = new float[expectedLength];
                for (int i = 0; i < desiredLength; i++) {
                    filled[i] = origin[i];
                }

                for (int i = desiredLength - 1; i < expectedLength; i++) {
                    filled[i] = origin[origin.length - 1];
                }

                return filled;
            }
        }

        /**
         * Retunrs the index-by-index summary of 2 arrays.
         *
         * @param array1 the first array
         * @param array2 the second array
         * @return a summary array
         */
        private float[] addArrays(float[] array1, float[] array2) {
            float[] smallest = null;
            float[] largest = null;
            if (array1.length >= array2.length) {
                smallest = array2;
                largest  = array1;
            } else {
                smallest = array1;
                largest  = array2;
            }

            int smallestLen = smallest.length;
            int largestLen = largest.length;

            float[] result = new float[largestLen];

            for (int i = 0; i < smallestLen; i++) {
                result[i] = smallest[i] + largest[i];
            }

            for (int i = smallestLen; i < largestLen; i++) {
                result[i] = largest[i];
            }

            return result;
        }

        /**
         * Populate the metrics to the Web UI.
         *
         * @param cm the chart to be fed with data
         * @param data the data to feed the chart
         * @param time a timestamp
         * @param numOfDp the number of data points
         */
        private void populateMetrics(
                ChartModel               cm,
                Map<MetricType, Float[]> data,
                LocalDateTime            time,
                int                      numOfDp) {
            if (data == null) {
                log.error("Data to populate is NULL");
                return;
            }

            for (int i = 0; i < numOfDp; i++) {
                Map<String, Object> local = Maps.newHashMap();
                for (MetricType cmt : SERVICE_CHAIN_DEPL_METRICS) {
                    if (data.containsKey(cmt)) {
                        local.put(StringUtils.lowerCase(cmt.name()), data.get(cmt)[i]);
                    }
                }

                String calculated = time.minusSeconds(numOfDp - i).toString(TIME_FORMAT);

                local.put(LABEL, calculated);
                populateMetric(cm.addDataPoint(calculated), local);
            }
        }

        /**
         * Populate a specific metric with data.
         *
         * @param dataPoint the particular part of the chart to be fed with data
         * @param data the data to feed the metric of the chart
         */
        private void populateMetric(ChartModel.DataPoint dataPoint, Map<String, Object> data) {
            data.forEach(dataPoint::data);
        }

        /**
         * Attach the list of all service chains to the top of the chart.
         *
         * @param cm the chart to be fed with data
         * @param deviceIds the set of service chain IDs to show up
         */
        private void attachServiceChainList(ChartModel cm, Set<ServiceChainId> scIds) {
            ArrayNode array = arrayNode();
            scIds.forEach(id -> array.add(id.toString()));
            cm.addAnnotation(SERVICE_CHAIN_IDS, array);
        }
    }

}
