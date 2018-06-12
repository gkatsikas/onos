/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
// import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.EnumFormatter;
import org.onosproject.ui.table.cell.NumberFormatter;
import org.onosproject.ui.table.cell.DefaultCellFormatter;

import java.util.Collection;
// import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;


/**
 * Message handler for CPU view related messages.
 */
public class CpuViewMessageHandler extends UiMessageHandler {

    private static final String CPU_DATA_REQ = "cpuDataRequest";
    private static final String CPU_DATA_RESP = "cpuDataResponse";
    private static final String CPUS = "cpus";

    private static final String CPU_DETAILS_REQ = "cpuDetailsRequest";
    private static final String CPU_DETAILS_RESP = "cpuDetailsResponse";
    private static final String DETAILS = "details";
    private static final String CPU = "cpu";

    private static final String DEV_ID = "devId";
    private static final String ID = "id";
    private static final String CPU_VENDOR = "cpu_vendor";
    private static final String CPU_FREQ = "cpu_freq";
    private static final String CPU_LOAD = "cpu_load";
    private static final String CPU_STATUS = "cpu_status";

    private static final String[] COL_IDS = {
            ID, CPU_VENDOR, CPU_FREQ, CPU_STATUS, CPU_LOAD
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new CpuDataRequest(),
                new DetailRequestHandler()
        );
    }

    // Handler for CPU table requests
    private final class CpuDataRequest extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No CPU cores found";

        private CpuDataRequest() {
            super(CPU_DATA_REQ, CPU_DATA_RESP, CPUS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(CPU_VENDOR, EnumFormatter.INSTANCE);
            tm.setFormatter(CPU_FREQ, NumberFormatter.INTEGER);
            tm.setFormatter(CPU_STATUS, DefaultCellFormatter.INSTANCE);
            tm.setFormatter(CPU_LOAD, NumberFormatter.TO_5DP);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, DEV_ID);
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                DeviceService ds = get(DeviceService.class);
                Device dev = ds.getDevice(deviceId);

                // RestSBController controller = get(RestSBController.class);
                // RestServerSBDevice device = (RestServerSBDevice) controller.getDevice(deviceId);

                // Only servers have CPU statistics
                if (dev.type() != Device.Type.SERVER) {
                    return;
                }

                for (int i = 0; i < 16; i++) {
                    populateRow(tm.addRow(), i);
                }

                // List<CpuStatistics> stats = ds.getCpuStatistics(deviceId);
                // for (CpuStatistics stat : stats) {
                //     populateRow(tm.addRow(), stat);
                // }
            }
        }

        // private void populateRow(TableModel.Row row, CpuStatistics stats) {
        //     row.cell(ID, stats.id())
        //         .cell(CPU_STATUS, stats.busy() ? "Busy" : "Idle")
        //         .cell(CPU_LOAD, stats.load());
        // }

        private void populateRow(TableModel.Row row, int i) {
            row.cell(ID, i)
                .cell(CPU_VENDOR, "GenuineIntel")
                .cell(CPU_FREQ, (long) 3200)
                .cell(CPU_STATUS, "Idle")
                .cell(CPU_LOAD, 0);
        }
    }

    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(CPU_DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            String devId = string(payload, DEV_ID);
            DeviceId deviceId = DeviceId.deviceId(devId);

            // DeviceService deviceService = get(DeviceService.class);
            // Port port = deviceService.getPort(deviceId, portNumber(id));
            // RestSBController controller = get(RestSBController.class);
            // RestServerSBDevice device = (RestServerSBDevice) controller.getDevice(deviceId);
            // List<CpuStatistics> stats = device.cpus();
            // String vendor = device.vendor().toString();
            // long frequency = device.frequency();

            ObjectNode data = objectNode();

            data.put(ID, id);
            data.put(DEV_ID, devId);
            // data.put(CPU_VENDOR, displayVendor(vendor));
            data.put(CPU_VENDOR, displayVendor("GenuineIntel"));
            // data.put(CPU_FREQ, displayFrequency(frequency));
            data.put(CPU_FREQ, displayFrequency((long) 3200));
            // data.put(CPU_STATUS, displayStatus(stats.busy()));
            data.put(CPU_STATUS, displayStatus(false));
            // data.put(CPU_LOAD, displayLoad(stats.load()));
            data.put(CPU_LOAD, displayLoad((float) 0.0));

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);

            // NOTE: ... an alternate way of getting all the details of an item:
            // Use the codec context to get a JSON of the port. See ONOS-5976.
            // rootNode.set(CPU, getJsonCodecContext().encode(port, Port.class));

            sendMessage(CPU_DETAILS_RESP, rootNode);
        }

        /**
         * Returns the CPU vendor as a displayable string.
         *
         * @param vendor the CPU vendor
         * @return human readable CPU vendor
         */
        // private String displayVendor(CpuVendor vendor) {
        private String displayVendor(String vendor) {
            // return vendor.toString();
            return vendor;
        }

        /**
         * Returns CPU frequency as a displayable string.
         *
         * @param frequency CPU frequency in MHz
         * @return human readable CPU frequency
         */
        private String displayFrequency(long frequency) {
            return "" + Long.toString(frequency) + " MHz";
        }

        /**
         * Returns the CPU status as a displayable string.
         *
         * @param type the CPU status
         * @return human readable CPU status
         */
        private String displayStatus(Boolean status) {
            return status ? "Busy" : "Idle";
        }

        /**
         * Returns CPU load as a displayable string.
         *
         * @param load CPU load in [0, 1]
         * @return human readable CPU load
         */
        private String displayLoad(float load) {
            return "" + Float.toString(load) + " Mbps";
        }
    }
}
