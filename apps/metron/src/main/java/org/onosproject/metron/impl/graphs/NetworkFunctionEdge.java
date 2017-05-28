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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of an edge in a network function's graph.
 */
public class NetworkFunctionEdge implements NetworkFunctionEdgeInterface {

    private final NetworkFunctionVertexInterface src;
    private final NetworkFunctionVertexInterface dst;

    /**
     * Creates a network function edge.
     *
     * @param src source vertex
     * @param dst destination vertex
     */
    public NetworkFunctionEdge(
            NetworkFunctionVertexInterface src,
            NetworkFunctionVertexInterface dst) {
        checkNotNull(
            src,
            "Source network function vertex is NULL"
        );
        checkNotNull(
            dst,
            "Destination network function vertex is NULL"
        );
        this.src = src;
        this.dst = dst;
    }

    @Override
    public NetworkFunctionVertexInterface src() {
        return src;
    }

    @Override
    public NetworkFunctionVertexInterface dst() {
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
        if (obj instanceof NetworkFunctionEdge) {
            final NetworkFunctionEdge other = (NetworkFunctionEdge) obj;
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
