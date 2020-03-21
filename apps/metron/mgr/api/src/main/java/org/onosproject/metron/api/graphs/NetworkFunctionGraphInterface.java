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

package org.onosproject.metron.api.graphs;

import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import org.onlab.graph.MutableGraph;

import java.util.List;

/**
 * Represents a mutable network function graph.
 */
public interface NetworkFunctionGraphInterface
        extends MutableGraph<NetworkFunctionVertexInterface, NetworkFunctionEdgeInterface> {

    /**
     * Checks whether the network function graph is empty or not.
     *
     * @return boolean emptiness
     */
    boolean isEmpty();

    /**
     * Returns the graph vertex associated with the input packet processing block.
     *
     * @param block network function's packet processing block
     * @return network function's graph vertex that holds this block
     */
    NetworkFunctionVertexInterface getVertexWithBlock(ProcessingBlockInterface block);

    /**
     * Returns the sorted list of edges of this graph.
     *
     * @param src graph's vertex
     * @return list of sorted edges
     */
    List<NetworkFunctionEdgeInterface> getSortedEdgesFrom(NetworkFunctionVertexInterface src);

    /**
     * Prints the state of the network function graph.
     */
    void print();

}
