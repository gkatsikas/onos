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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A stack node for trees.
 * This class essentially wraps the tree node into a linked list.
 */
public class StackNode<T> {

    TreeNode<T>  treeNode;
    StackNode<T> next;

    public StackNode(TreeNode<T> treeNode) {
        checkNotNull(
            treeNode,
            "Tree node is null"
        );

        this.treeNode = treeNode;
        this.next = null;
    }

    /**
     * Returns the tree node wrapped by this StackNode.
     *
     * @return a tree node
     */
    public TreeNode<T> treeNode() {
        return this.treeNode;
    }

    /**
     * Sets the tree node wrapped by this StackNode.
     *
     * @param treeNode a tree node
     */
    public void setTreeNode(TreeNode<T> treeNode) {
        this.treeNode = treeNode;
    }

    /**
     * Returns the next tree node in the stack.
     *
     * @return next tree node
     */
    public StackNode<T> next() {
        return this.next;
    }

    /**
     * Sets the next tree node in the stack.
     *
     * @param next the next tree node
     */
    public void setNext(StackNode<T> next) {
        this.next = next;
    }

}
