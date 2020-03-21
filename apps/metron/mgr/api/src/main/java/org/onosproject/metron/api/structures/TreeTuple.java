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

package org.onosproject.metron.api.structures;

/**
 * A pair of nodes that we use to maintain the internal state,
 * while building an element's classification tree.
 */
public final class TreeTuple {

    private final TreeNode junction;
    private final TreeNode origin;

    public TreeTuple(TreeNode junction, TreeNode origin) {
        this.junction = junction;
        this.origin = origin;
    }

    /**
     * Returns the junction tree node of this tuple.
     *
     * @return a junction tree node
     */
    public TreeNode junction() {
        return this.junction;
    }

    /**
     * Returns the origin tree node of this tuple.
     *
     * @return an origin tree node
     */
    public TreeNode origin() {
        return this.origin;
    }

}
