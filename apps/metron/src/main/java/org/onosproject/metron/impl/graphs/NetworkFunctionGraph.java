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

package org.onosproject.metron.impl.graphs;

import org.onosproject.metron.api.graphs.NetworkFunctionEdgeInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import org.onlab.graph.MutableAdjacencyListsGraph;

import org.slf4j.Logger;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of a network function's graph.
 */
public class NetworkFunctionGraph
        extends MutableAdjacencyListsGraph
        <
            NetworkFunctionVertexInterface, NetworkFunctionEdgeInterface
        >
        implements NetworkFunctionGraphInterface {

    private static final Logger log = getLogger(NetworkFunctionGraph.class);

    /**
     * Creates a network function graph comprising of the specified vertices and edges.
     *
     * @param vertices set of graph vertices
     * @param edges    set of graph edges
     */
    public NetworkFunctionGraph(
            Set<NetworkFunctionVertexInterface> vertices,
            Set<NetworkFunctionEdgeInterface> edges) {
        super(vertices, edges);
    }

    @Override
    public boolean isEmpty() {
        return this.getVertexes().size() == 0;
    }

    @Override
    public NetworkFunctionVertexInterface getVertexWithBlock(ProcessingBlockInterface block) {
        for (NetworkFunctionVertexInterface vertex : this.getVertexes()) {
            if (vertex.processingBlock().equals(block)) {
                return vertex;
            }
        }

        return null;
    }

    @Override
    public List<NetworkFunctionEdgeInterface> getSortedEdgesFrom(NetworkFunctionVertexInterface src) {
        List<NetworkFunctionEdgeInterface> edges = new ArrayList<NetworkFunctionEdgeInterface>(src.outputPortsNumber());

        for (int i = 0; i < src.outputPortsNumber(); i++) {
            ProcessingBlockInterface bl = src.getOutputBlockFromPort(i);

            for (NetworkFunctionEdgeInterface edge : this.getEdgesFrom(src)) {
                if (edge.dst().processingBlock().id().equals(bl.id())) {
                    edges.add(i, edge);
                    break;
                }
            }

            checkNotNull(
                edges.get(i),
                "[" + label() + "] Edge from output port " + i + " was not detected"
            );
        }

        return edges;
    }

    @Override
    public void print() {
        log.info("[{}] Network function graph", this.label());

        for (NetworkFunctionVertexInterface vertex : this.getVertexes()) {
            List<NetworkFunctionEdgeInterface> neighboringEdges = this.getSortedEdgesFrom(vertex);

            log.info(
                "[{}] Processing block {} with {} successors",
                this.label(),
                vertex.processingBlock().id(),
                neighboringEdges.size()
            );
        }
    }

    /**
     * Returns a label with the network function graph's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private String label() {
        return "Network Function Graph";
    }

}
