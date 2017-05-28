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

import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;

import org.onlab.graph.MutableGraph;

/**
 * Represents a mutable service chain graph.
 */
public interface ServiceChainGraphInterface
        extends MutableGraph<ServiceChainVertexInterface, ServiceChainEdgeInterface> {

    /**
     * Checks whether the service chain graph is empty or not.
     *
     * @return boolean emptiness
     */
    boolean isEmpty();

    /**
     * Returns the graph vertex associated with the input network function.
     *
     * @param nf service chain's network function
     * @return service chain's graph vertex that holds this network function
     */
    ServiceChainVertexInterface getVertexWithNetworkFunction(NetworkFunctionInterface nf);

    /**
     * Prints the state of the service chain graph.
     */
    void print();

}
