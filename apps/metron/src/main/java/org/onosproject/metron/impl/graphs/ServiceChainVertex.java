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

import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;

import java.util.Objects;

/**
 * Implementation of a vertex in a service chain's graph.
 */
public class ServiceChainVertex implements ServiceChainVertexInterface {

    private NetworkFunctionInterface networkFunction;

    /**
     * Creates an empty service chain vertex.
     */
    public ServiceChainVertex() {
        this.networkFunction = null;
    }

    /**
     * Creates a service chain vertex with a network function.
     *
     * @param networkFunction the pillar of this vertex
     */
    public ServiceChainVertex(NetworkFunctionInterface networkFunction) {
        this.networkFunction = networkFunction;
    }

    @Override
    public NetworkFunctionInterface networkFunction() {
        return this.networkFunction;
    }

    @Override
    public void setNetworkFunction(NetworkFunctionInterface nf) {
        this.networkFunction = nf;
    }

    @Override
    public boolean isEndPoint() {
        return this.networkFunction.isEndPoint();
    }

    @Override
    public boolean isEntryPoint() {
        return this.networkFunction.isEntryPoint();
    }

    @Override
    public boolean isExitPoint() {
        return this.networkFunction.isExitPoint();
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkFunction);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ServiceChainVertex) {
            final ServiceChainVertex other = (ServiceChainVertex) obj;
            return Objects.equals(this.networkFunction, other.networkFunction);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.networkFunction.toString();
    }

}
