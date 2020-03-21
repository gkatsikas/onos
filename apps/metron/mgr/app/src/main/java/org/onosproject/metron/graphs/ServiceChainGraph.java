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

package org.onosproject.metron.graphs;

import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.ServiceChainEdgeInterface;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;

import org.onlab.graph.MutableAdjacencyListsGraph;

import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a service chain's graph.
 */
public class ServiceChainGraph
        extends MutableAdjacencyListsGraph
        <
            ServiceChainVertexInterface, ServiceChainEdgeInterface
        >
        implements ServiceChainGraphInterface {

    private static final Logger log = getLogger(ServiceChainGraph.class);

    /**
     * Creates a service chain graph comprising of the specified vertices and edges.
     *
     * @param vertices set of graph vertices
     * @param edges    set of graph edges
     */
    public ServiceChainGraph(
            Set<ServiceChainVertexInterface> vertices,
            Set<ServiceChainEdgeInterface>   edges) {
        super(vertices, edges);
    }

    @Override
    public boolean isEmpty() {
        return this.getVertexes().size() == 0;
    }

    @Override
    public ServiceChainVertexInterface getVertexWithNetworkFunction(NetworkFunctionInterface nf) {
        for (ServiceChainVertexInterface vertex : this.getVertexes()) {
            if (vertex.networkFunction().equals(nf)) {
                return vertex;
            }
        }

        return null;
    }

    @Override
    public void print() {
        log.info("[{}] Service chain graph", this.label());

        for (ServiceChainVertexInterface vertex : this.getVertexes()) {
            Set<ServiceChainEdgeInterface> neighboringEdges = this.getEdgesFrom(vertex);

            log.info(
                "[{}] Network function {} with {} successors",
                this.label(),
                vertex.networkFunction().id(),
                neighboringEdges.size()
            );

            NetworkFunctionInterface nf = vertex.networkFunction();
            if (nf == null) {
                continue;
            }

            NetworkFunctionGraphInterface nfGraph = nf.networkFunctionGraph();
            if (nfGraph == null) {
                continue;
            }

            nfGraph.print();
        }
    }

    /**
     * Returns a label with the service chain graph's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private String label() {
        return "Service Chain Graph";
    }

}
