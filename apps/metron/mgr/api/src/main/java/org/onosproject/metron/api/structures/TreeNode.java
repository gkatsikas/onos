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
 * A tree node for binary trees.
 */
public class TreeNode<T> implements Comparable<TreeNode<T>> {

    private T           data;
    private TreeNode<T> parent;
    private TreeNode<T> left;
    private TreeNode<T> right;
    private boolean     isOperator;

    public TreeNode(T data) {
        this.data   = data;
        this.parent = null;
        this.left   = null;
        this.right  = null;
        this.isOperator = false;
    }

    public TreeNode(T data, boolean isOperator) {
        this.data   = data;
        this.parent = null;
        this.left   = null;
        this.right  = null;
        this.isOperator = isOperator;
    }

    public TreeNode(TreeNode<T> another) {
        this.data       = another.data;
        this.parent     = another.parent;
        this.left       = another.left;
        this.right      = another.right;
        this.isOperator = another.isOperator;
    }

    /**
     * Returns the data stored in a tree node.
     *
     * @return data
     */
    public T data() {
        return this.data;
    }

    /**
     * Sets the data of a tree node.
     *
     * @param data input data
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * Returns the parent of a tree node.
     *
     * @return parent tree node
     */
    public TreeNode<T> parent() {
        return this.parent;
    }

    /**
     * Sets the parent of a tree node.
     *
     * @param parent the parent tree node
     */
    public void setParent(TreeNode<T> parent) {
        this.parent = parent;
    }

    /**
     * Returns the left child of a tree node.
     *
     * @return left child tree node
     */
    public TreeNode<T> left() {
        return this.left;
    }

    /**
     * Sets the left child of a tree node.
     *
     * @param left the left child tree node
     */
    public void setLeft(TreeNode<T> left) {
        this.left = left;
    }

    /**
     * Returns the right child of a tree node.
     *
     * @return right child tree node
     */
    public TreeNode<T> right() {
        return this.right;
    }

    /**
     * Sets the right child of a tree node.
     *
     * @param right the right child tree node
     */
    public void setRight(TreeNode<T> right) {
        this.right = right;
    }

    /**
     * Returns whether this tree node is an operator.
     *
     * @return boolean is operator or not
     */
    public boolean isOperator() {
        return this.isOperator;
    }

    /**
     * (Un)Sets the tree node as an operator.
     *
     * @param isOperator boolean is operator or not
     */
    public void setAsOperator(boolean isOperator) {
        this.isOperator = isOperator;
    }

    /**
     * Returns whether this tree node is the root.
     *
     * @return boolean is root or not
     */
    public boolean isRoot() {
        return (this.parent == null);
    }

    /**
     * Returns whether this tree node is a leaf.
     *
     * @return boolean is leaf or not
     */
    public boolean isLeaf() {
        return (this.left == null) && (this.right == null);
    }

    @Override
    public int compareTo(TreeNode<T> o) {
        return this.data.toString().compareTo(o.data().toString());
    }

    @Override
    public String toString() {
        return this.data != null ?
            this.data.toString() : "[Data is null]";
    }

}
