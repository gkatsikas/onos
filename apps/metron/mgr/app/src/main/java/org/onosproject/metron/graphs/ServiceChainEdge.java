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

import org.onosproject.metron.api.graphs.ServiceChainEdgeInterface;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of an edge in a service chain's graph.
 */
public class ServiceChainEdge implements ServiceChainEdgeInterface {

    private final ServiceChainVertexInterface src;
    private final ServiceChainVertexInterface dst;

    /**
     * Creates a service chain edge.
     *
     * @param src source vertex
     * @param dst destination vertex
     */
    public ServiceChainEdge(
            ServiceChainVertexInterface src,
            ServiceChainVertexInterface dst) {
        checkNotNull(
            src,
            "Source service chain vertex is NULL"
        );
        checkNotNull(
            dst,
            "Destination service chain vertex is NULL"
        );
        this.src = src;
        this.dst = dst;
    }

    @Override
    public ServiceChainVertexInterface src() {
        return src;
    }

    @Override
    public ServiceChainVertexInterface dst() {
        return dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ServiceChainEdge) {
            final ServiceChainEdge other = (ServiceChainEdge) obj;
            return  Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
            .add("src", src)
            .add("dst", dst)
            .toString();
    }

}
