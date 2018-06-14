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

// ONOS libraries
import org.onosproject.drivers.server.behavior.CpuStatisticsDiscovery;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.metron.impl.gui.Resource.CPU_CORES_METRICS;

/**
 * Message handler for passing CPU load data to the Web UI.
 */
public class ServerCpuViewMessageHandler extends UiMessageHandler {

    private static final Logger log = getLogger(ServerCpuViewMessageHandler.class);

    private static final String SERVER_CPU_DATA_REQ = "servercpuDataRequest";
    private static final String SERVER_CPU_DATA_RESP = "servercpuDataResponse";
    private static final String SERVER_CPUS = "servercpus";

    private static final String DEVICE_IDS = "deviceIds";

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
            super(SERVER_CPU_DATA_REQ, SERVER_CPU_DATA_RESP, SERVER_CPUS);
        }

        @Override
        protected String[] getSeries() {
            return CPU_CORES_METRICS.stream().map(type ->
                StringUtils.lowerCase(type.name())).toArray(String[]::new);
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            String uri = string(payload, "devId");
            DeviceService ds = get(DeviceService.class);
            MonitorService monitoringService = get(MonitorService.class);

            // Project only one device over time
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);

                Map<Integer, LruCache<Float>> devCpuLoad = monitoringService.cpuLoadOfDevice(deviceId);
                // Only for devices that report CPU statistics
                if (devCpuLoad != null) {
                    // We need an array of data points
                    Map<MetricType, Float[]> data = populateCpuDataHistory(devCpuLoad);
                    // Generate a timestamp
                    LocalDateTime ldt = new LocalDateTime(timestamp);

                    populateMetrics(cm, data, ldt, NUM_OF_DATA_POINTS);

                    Set<DeviceId> deviceIds = Sets.newHashSet();
                    for (Device device : ds.getAvailableDevices()) {
                        // Only devices that support CPU monitoring are included
                        if (device.is(CpuStatisticsDiscovery.class)) {
                            deviceIds.add(device.id());
                        }
                    }

                    // Drop down list to select devices
                    attachDeviceList(cm, deviceIds);
                }
            } else {
                for (Device device : ds.getAvailableDevices()) {
                    DeviceId deviceId  = device.id();
                    String deviceIdStr = deviceId.toString();

                    Map<Integer, LruCache<Float>> devCpuLoad = monitoringService.cpuLoadOfDevice(deviceId);
                    // This device does not report CPU statistics
                    if (devCpuLoad == null) {
                        continue;
                    }

                    // Get the latest CPU measurements
                    Map<MetricType, Float> data = this.populateCpuData(devCpuLoad);

                    // Map them to the CPU cores
                    Map<String, Object> local = Maps.newHashMap();
                    for (MetricType cmt : CPU_CORES_METRICS) {
                        local.put(StringUtils.lowerCase(cmt.name()), data.get(cmt));
                    }

                    // Last piece of data is the device ID
                    local.put(LABEL, deviceId);

                    // Send this data
                    populateMetric(cm.addDataPoint(deviceId), local);
                }
            }
        }

        /**
         * Turn the current monitoring data into a data structure that can feed the UI memory.
         *
         * @param devCpuLoad the monitoring memory that holds the CPU load per core
         * @return a map of metrics to their values
         */
        private Map<MetricType, Float> populateCpuData(Map<Integer, LruCache<Float>> devCpuLoad) {
            Map<MetricType, Float> data = Maps.newHashMap();

            int i = 0;
            for (MetricType cmt : CPU_CORES_METRICS) {
                LruCache<Float> loadCache = devCpuLoad.get(new Integer(i));
                Float val = loadCache.getLastValue();

                // Project the floating point load value in [0, 1] to [0, 100]
                Float projectedVal = new Float(val.floatValue() * (float) 100);

                // Now the data is in the right form
                data.put(cmt, projectedVal);

                i++;
            }

            return data;
        }

        /**
         * Turn the monitoring data history into a data structure that can feed the UI memory.
         *
         * @param devCpuLoad the monitoring memory that holds the CPU load per core
         * @return a map of metrics to their arrays of values
         */
        private Map<MetricType, Float[]> populateCpuDataHistory(Map<Integer, LruCache<Float>> devCpuLoad) {
            Map<MetricType, Float[]> data = Maps.newHashMap();

            int i = 0;
            for (MetricType cmt : CPU_CORES_METRICS) {
                LruCache<Float> loadCache = devCpuLoad.get(new Integer(i));
                Float[] floatArray = loadCache.values().toArray(new Float[0]);

                // Project the load array to the range of [0, 100]
                float[] loadArray = new float[floatArray.length];
                for (int j = 0; j < floatArray.length; j++) {
                    loadArray[j] = floatArray[j].floatValue() * (float) 100;
                }

                // Fill the missing points
                float[] filledLoadArray = fillData(loadArray, NUM_OF_DATA_POINTS);

                // Set the data
                data.put(cmt, ArrayUtils.toObject(filledLoadArray));

                i++;
            }

            // Keep a timestamp
            timestamp = System.currentTimeMillis();

            return data;
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
         * Populate the metrics to the Web UI.
         *
         * @param cm the chart to be fed with data
         * @param data the data to feed the chart
         * @param time a timestamp
         * @param numberOfPoints the number of data points
         */
        private void populateMetrics(
                ChartModel               cm,
                Map<MetricType, Float[]> data,
                LocalDateTime            time,
                int                      numberOfPoints) {
            for (int i = 0; i < numberOfPoints; i++) {
                Map<String, Object> local = Maps.newHashMap();
                for (MetricType cmt : CPU_CORES_METRICS) {
                    if (data.containsKey(cmt)) {
                        local.put(StringUtils.lowerCase(cmt.name()), data.get(cmt)[i]);
                    }
                }

                String calculated = time.minusSeconds(numberOfPoints - i).toString(TIME_FORMAT);

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
         * Attach the list of all devices to the top of the chart.
         *
         * @param cm the chart to be fed with data
         * @param deviceIds the set of Device IDs to show up
         */
        private void attachDeviceList(ChartModel cm, Set<DeviceId> deviceIds) {
            ArrayNode array = arrayNode();
            deviceIds.forEach(id -> array.add(id.toString()));
            cm.addAnnotation(DEVICE_IDS, array);
        }
    }

}
