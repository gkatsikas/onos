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

package org.onosproject.metron.gui;

import org.onosproject.metron.api.config.TrafficPoint;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.orchestrator.OrchestrationService;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainScope;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Message handler for service chain information related messages.
 */
public class ServiceChainInfoViewMessageHandler extends UiMessageHandler {

    private static final Logger log = getLogger(ServiceChainInfoViewMessageHandler.class);

    private static final String SC_INFO_DATA_REQ = "scinfoDataRequest";
    private static final String SC_INFO_DATA_RES = "scinfoDataResponse";
    private static final String SC_INFO = "scinfos";

    private static final String ID = "id";
    private static final String NETWORK_FUNCTIONS = "nfs";
    private static final String SCOPE = "scope";
    private static final String DISPATCHER = "dispatcher";
    private static final String INGRESS_POINTS = "ingress";
    private static final String PROCESSING_POINTS = "processing";
    private static final String EGRESS_POINTS = "egress";
    private static final String CPUS = "cpus";
    private static final String NICS = "nics";

    private static final String SCOPE_SERVER = "Server";
    private static final String SCOPE_NETWORK = "Network";

    private static final String DISPATCHER_RSS = "NIC RSS";
    private static final String DISPATCHER_FLOW = "NIC Flow API";
    private static final String DISPATCHER_VMDQ_MAC = "OpenFlow + NIC MAC-based VMDQ";
    private static final String DISPATCHER_VMDQ_VLAN = "OpenFlow + NIC VLAN-based VMDQ";
    private static final String DISPATCHER_UNKNOWN = "Unknown";

    private static final char DOT = '.';

    private static final String[] COL_IDS = {
        ID, NETWORK_FUNCTIONS, SCOPE, DISPATCHER,
        INGRESS_POINTS, PROCESSING_POINTS, EGRESS_POINTS,
        CPUS, NICS
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new ScInfoRequest());
    }

    // handler for service chains' info table requests
    private final class ScInfoRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No service chains information found";

        private ScInfoRequest() {
            super(SC_INFO_DATA_REQ, SC_INFO_DATA_RES, SC_INFO);
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
        protected String defaultColumnId() {
            return ID;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            OrchestrationService orchestrator = get(OrchestrationService.class);
            if (orchestrator == null) {
                log.warn("Metron Orchestrator service unavailable: Cannot visualize active service chains");
                return;
            }

            for (ServiceChainInterface sc : orchestrator.activeServiceChains()) {
                checkNotNull(sc, "Cannot visualize null service chain");
                populateRow(tm.addRow(), sc);
            }
        }

        /**
         * Populates table's row with information about an active service chain.
         *
         * @param row table's row
         * @param sc the service chain to visualize
         */
        private void populateRow(TableModel.Row row, ServiceChainInterface sc) {
            ServiceChainId scId  = sc.id();
            ServiceChainGraphInterface scGraph = sc.serviceChainGraph();
            ServiceChainScope scScope = sc.scope();

            row.cell(ID, scId.toString())
               .cell(NETWORK_FUNCTIONS, displayNetworkFunctions(scId, scGraph))
               .cell(SCOPE, displayScope(scScope))
               .cell(DISPATCHER, displayDispatcher(scScope))
               .cell(INGRESS_POINTS, displayTrafficPoints(scId, sc.ingressPoints()))
               .cell(PROCESSING_POINTS, displayTrafficPoints(scId, sc.processingPoints()))
               .cell(EGRESS_POINTS, displayTrafficPoints(scId, sc.egressPoints()))
               .cell(CPUS, Integer.toString(sc.cpuCores()))
               .cell(NICS, Integer.toString(sc.nics()));
        }

        /**
         * Returns a user friendly chain of network functions.
         *
         * @param scId a service chain's ID
         * @param scGraph a service chain's graph
         * @return a user friendly string with service chain's chain of functions
         */
        private String displayNetworkFunctions(ServiceChainId scId, ServiceChainGraphInterface scGraph) {
            checkNotNull(scId, "Service chain ID is null");
            String nfChainStr = "";

            if (scGraph == null) {
                log.error("Unavailable packet processing graph for service chain: {}", scId);
                return nfChainStr;
            }

            int nfsNumber = scGraph.getVertexes().size();
            if (nfsNumber == 0) {
                log.warn("Empty packet processing graph for service chain: {}", scId);
                return nfChainStr;
            }

            // Traverse the graph and log the types of the network functions
            int nfIndex = 1;
            while (nfIndex <= nfsNumber) {
                for (ServiceChainVertexInterface scVertex : scGraph.getVertexes()) {
                    NetworkFunctionInterface nf = scVertex.networkFunction();
                    int index = Common.findFirstNotOf(nf.name(), "nf", 0);
                    int nfNumber = Integer.parseInt(nf.name().substring(index));
                    if (nfNumber == nfIndex) {
                        nfChainStr += nf.nfClass().toString().toUpperCase() + " -> ";
                        nfIndex++;
                        break;
                    }
                }
            }

            // Remove the last ' -> '
            nfChainStr = nfChainStr.substring(0, nfChainStr.length() - 4);

            return nfChainStr;
        }

        /**
         * Returns a user friendly service chain scope.
         *
         * @param scScope a service chain's scope
         * @return a user friendly string with service chain's scope
         */
        private String displayScope(ServiceChainScope scScope) {
            if (ServiceChainScope.isServerLevel(scScope)) {
                return SCOPE_SERVER;
            } else {
                return SCOPE_NETWORK;
            }
        }

        /**
         * Returns a user friendly service chain dispatching method.
         *
         * @param scScope a service chain's scope
         * @return a user friendly string with service chain's dispatching method
         */
        private String displayDispatcher(ServiceChainScope scScope) {
            checkArgument(ServiceChainScope.isValid(scScope), "Invalid service chain scope");

            if (scScope == ServiceChainScope.SERVER_RSS) {
                return DISPATCHER_RSS;
            } else if (scScope == ServiceChainScope.SERVER_RULES) {
                return DISPATCHER_FLOW;
            } else if (scScope == ServiceChainScope.NETWORK_MAC) {
                return DISPATCHER_VMDQ_MAC;
            } else if (scScope == ServiceChainScope.NETWORK_VLAN) {
                return DISPATCHER_VMDQ_VLAN;
            }

            return DISPATCHER_UNKNOWN;
        }

        /**
         * Returns a service chain's traffic points.
         *
         * @param scId a service chain's ID
         * @param scTrafficPoints a service chain's set of traffic points (ingress, processing, or egress)
         * @return a user friendly string with service chain's traffic points
         */
        private String displayTrafficPoints(ServiceChainId scId, Set<TrafficPoint> scTrafficPoints) {
            checkNotNull(scId, "Service chain ID is null");
            checkArgument((scTrafficPoints != null) && (scTrafficPoints.size() > 0),
                "Invalid traffic points for service chain {}", scId);
            String trafficPointsStr = "";

            for (TrafficPoint tp : scTrafficPoints) {
                String portIds = tp.portIds().stream()
                                .map(p -> p.toString())
                                .collect(Collectors.joining(" "));
                trafficPointsStr += "Dev: " + tp.deviceId() + " - Ports: " + portIds + "\n";
            }

            return trafficPointsStr;
        }

    }

}
