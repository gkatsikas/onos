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

import org.onlab.graph.Vertex;

/**
 * Represents a vertex in a service chain.
 * Such a vertex in a network function.
 */
public interface ServiceChainVertexInterface extends Vertex {

    /**
     * Returns the associated network function.
     *
     * @return network function
     */
    NetworkFunctionInterface networkFunction();

    /**
     * Associates a network function with the service chain.
     *
     * @param nf network function
     */
    void setNetworkFunction(NetworkFunctionInterface nf);

    /**
     * Returns whether this service chain vertex is an end point or not.
     * This occurs when the NF of this vertex is an end point itself.
     *
     * @return boolean end point status
     */
    boolean isEndPoint();

    /**
     * Returns whether this service chain vertex is an entry point or not.
     * This occurs when the NF of this vertex is an entry point itself.
     *
     * @return boolean entry point status
     */
    boolean isEntryPoint();

    /**
     * Returns whether this service chain vertex is an exit point or not.
     * This occurs when the NF of this vertex is an exit point itself.
     *
     * @return boolean exit point status
     */
    boolean isExitPoint();

}
