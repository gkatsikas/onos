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

package org.onosproject.metron.impl.classification;

import org.onosproject.metron.api.classification.ClassificationTreeInterface;
import org.onosproject.metron.api.structures.TreeNode;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A visualization class for binary trees.
 */
public class VisualTree {

    private static final Logger log = getLogger(VisualTree.class);

    private ClassificationTreeInterface tree;
    private TreeNode root;

    public VisualTree(ClassificationTreeInterface tree) {
        checkNotNull(
            tree,
            "Tree does not exist"
        );

        this.tree = tree;

        checkNotNull(
            tree.root(),
            "Tree is not built"
        );
        this.root = tree.root();
    }

    /**
     * Returns the root of the tree.
     *
     * @return the root node
     */
    public TreeNode root() {
        return this.root;
    }

    /**
     * Print the tree using a nice terminal structure.
     */
    public void print() {
        log.info("Expression tree");
        this.print("", this.root(), false);
    }

    /**
     * Recursive method to print the expression tree.
     *
     * @param a prefix to beautify the output
     * @param a tree node to print
     * @param boolean to check the branch type
     */
    private void print(String prefix, TreeNode node, boolean isLeft) {
        if (node != null) {
            log.info(prefix + (isLeft ? "|-- " : "\\-- ") + node.data());
            this.print(prefix + (isLeft ? "|   " : "    "), node.left(),  true);
            this.print(prefix + (isLeft ? "|   " : "    "), node.right(), false);
        }
    }

    /**
     * Prints the expression tree as a prefix notation.
     */
    public void printPrefix() {
        log.info("Prefix notation:");
        this.printPreOrder(this.root());
    }

    /**
     * Recursive method to print the prefix notation.
     *
     * @param a tree node to print
     */
    private void printPreOrder(TreeNode node) {
        if (node != null) {
            log.info(node.data().toString());
            this.printPreOrder(node.left());
            this.printPreOrder(node.right());
        }
    }

    /**
     * Prints the expression tree as an infix notation.
     */
    public void printInfix() {
        log.info("Infix notation:");
        this.printInOrder(this.root());
    }

    /**
     * Recursive method to print the infix notation.
     *
     * @param a tree node to print
     */
    private void printInOrder(TreeNode node) {
        if (node != null) {
            this.printInOrder(node.left());
            log.info(node.data().toString());
            this.printInOrder(node.right());
        }
    }

    /**
     * Prints the expression tree as a postfix notation.
     */
    public void printPostfix() {
        log.info("Postfix notation:");
        this.printPostOrder(this.root());
    }

    /**
     * Recursive method to print the postfix notation.
     *
     * @param a tree node to print
     */
    private void printPostOrder(TreeNode node) {
        if (node != null) {
            this.printPostOrder(node.left());
            this.printPostOrder(node.right());
            log.info(node.data().toString());
        }
    }

    /**
     * Prints the nodes of the tree that belong
     * to a particular level.
     *
     * @param node a tree node to print (root is preferred)
     * @param level tree level
     */
    public void printGivenLevel(TreeNode node, int level) {
        if ((node == null) || (level < 1)) {
            return;
        }

        // Right place
        if (level == 1) {
            log.info("Height {}: {}", ClassificationTree.height(node), node.data());
        // Keep going..
        } else if (level > 1) {
            printGivenLevel(node.left(),  level - 1);
            printGivenLevel(node.right(), level - 1);
        }
    }

    /**
     * Prints the tree nodes with increasing level (top down).
     */
    public void printLevelOrder() {
        log.info("Level order:");
        if ((this.tree == null) || (this.root == null)) {
            log.info("\tNULL");
            return;
        }

        int h = ClassificationTree.height(this.root);

        for (int i = 1; i <= h; i++) {
            printGivenLevel(this.root, i);
            log.info("");
        }
    }

    /**
     * Prints the tree nodes with decreasing level (bottom up).
     */
    public void printReverseLevelOrder() {
        log.info("Reverse level order:");
        if ((this.tree == null) || (this.root == null)) {
            log.info("\tNULL");
            return;
        }

        int h = ClassificationTree.height(this.root);

        for (int i = h; i >= 1; i--) {
            printGivenLevel(this.root, i);
            log.info("");
        }
    }

    /**
     * Print the list of datapath traffic classes
     * derived by this classification tree.
     */
    public void printHwTrafficClasses() {
        log.info("");
        log.info("Rule: {}", this.tree.trafficClass());
        log.info("Equivalent datapath rules: ");
        int i = 1;
        for (String rule : this.tree.datapathTrafficClasses()) {
            log.info("Traffic class {}: {}", i, rule);
            i++;
        }
    }

}
