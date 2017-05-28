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

import org.onosproject.metron.api.exceptions.ParseException;
import org.onosproject.metron.api.exceptions.InputConfigurationException;
import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.ClassificationSyntax;
import org.onosproject.metron.api.classification.ClassificationTreeInterface;
import org.onosproject.metron.api.net.ClickFlowRuleAction;
import org.onosproject.metron.api.structures.TreeNode;
import org.onosproject.metron.api.structures.TreeTuple;
import org.onosproject.metron.api.structures.StackNode;

import org.slf4j.Logger;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A binary expression tree to encode packet classification rules.
 */
public class ClassificationTree implements ClassificationTreeInterface {

    private static final Logger log = getLogger(ClassificationTree.class);

    /**
     * A rule expressed in a tcpdump-like format.
     */
    private String trafficClass;
    /**
     * The output port of this rule.
     */
    private int outputPort;
    /**
     * The priority of this rule.
     */
    private int priority;
    /**
     * The action of this rule.
     */
    private ClickFlowRuleAction action;
    /**
     * An instance of the stack.
     */
    private StackNode<String> top;
    /**
     * The traffic class encoded by this tree is mapped to
     * one or more datapath traffic classes.
     */
    private List<String> datapathTrafficClasses;
    /**
     * Each datapath traffic class is also mapped to
     * a set of TextualPacketFilter objects (headerField + operator + headerValue).
     */
    private Map<String, Set<TextualPacketFilter>> datapathPacketFilters;

    /**
     * To compute a datapath equivalent set of rules for this
     * classification tree, we need the following data structures.
     */
    private List<TreeNode> leavesList;
    private List<TreeNode> junctionsList;
    private Map<TreeNode, List<String>> collectedTrafficClassesLeft;
    private Map<TreeNode, List<String>> collectedTrafficClassesRight;

    /**
     * Constructs an empty expression tree.
     *
     * @param trafficClass a string-based traffic class
     * @param outputPort the output port of the traffic class
     * @param priority the priority of the traffic class
     * @param action an action to take for this traffic class
     */
    public ClassificationTree(
            String trafficClass,
            int outputPort,
            int priority,
            ClickFlowRuleAction action) {
        this.trafficClass = trafficClass;
        this.outputPort = outputPort;
        this.priority = priority;
        this.action = action;
        this.top = null;

        // The list of datapath rules
        this.datapathTrafficClasses = new ArrayList<String>();
        this.datapathPacketFilters  = new ConcurrentHashMap<String, Set<TextualPacketFilter>>();

        // Auxiliary data structures for the datapath rules' computation
        this.leavesList    = new ArrayList<TreeNode>();
        this.junctionsList = new ArrayList<TreeNode>();
        this.collectedTrafficClassesLeft  = new ConcurrentHashMap<TreeNode, List<String>>();
        this.collectedTrafficClassesRight = new ConcurrentHashMap<TreeNode, List<String>>();
    }

    @Override
    public String trafficClass() {
        return this.trafficClass;
    }

    @Override
    public int outputPort() {
        return this.outputPort;
    }

    @Override
    public int priority() {
        return this.priority;
    }

    @Override
    public ClickFlowRuleAction action() {
        return this.action;
    }

    @Override
    public List<String> datapathTrafficClasses() {
        return this.datapathTrafficClasses;
    }

    @Override
    public Set<TextualPacketFilter> getPacketFiltersOf(String trafficClass) {
        return this.datapathPacketFilters.get(trafficClass);
    }

    /**
     * Adds a tree node into the stack.
     *
     * @param node a tree node to be inserted
     */
    private void push(TreeNode node) {
        if (this.top == null) {
            this.top = new StackNode(node);
        } else {
            StackNode newNode = new StackNode(node);
            newNode.setNext(this.top);
            this.top = newNode;
        }
    }

    /**
     * Removes the tree node from the top of the stack.
     *
     * @return a tree node
     */
    private TreeNode pop() {
        if (this.top == null) {
            throw new ParseException("Stack underflow");
        } else {
            TreeNode node = this.top.treeNode();
            this.top = this.top.next();
            return node;
        }
    }

    /**
     * Returns the top of the stack (root).
     *
     * @return a tree node or null
     */
    private TreeNode top() {
        if (this.top == null) {
            return null;
        }
        return this.top.treeNode();
    }

    /**
     * Returns the root of the tree.
     *
     * @return a tree node
     */
    public TreeNode root() {
        return this.top();
    }

    /**
     * Returns the height of the tree.
     * A leaf has height 1 and it increases as we go toward the root.
     *
     * @param node a tree node to assess its height
     * @return the height of the input node
     */
    public static int height(TreeNode node) {
        if (node == null) {
            return 0;
        }

        int l = height(node.left());
        int r = height(node.right());

        return Math.max(l, r) + 1;
    }

    /**
     * Inserts a node in the expression tree.
     * This node can be an expression or an operator.
     *
     * @param word string (expression or operator)
     * @return insertion status
     */
    private boolean insert(String word) {
        if (word.isEmpty() || word.equals(" ")) {
            log.info("Empty word rejected");
            return true;
        }

        try {
            if (isOperator(word)) {
                String operator = unifyOperator(word);

                TreeNode newNode = new TreeNode(operator, true);
                TreeNode left = this.pop();
                left.setParent(newNode);
                newNode.setLeft(left);

                // Negation is a unary operator, so it has only one child
                if (!operator.equals(ClassificationSyntax.LOGICAL_NOT)) {
                    TreeNode right = this.pop();
                    right.setParent(newNode);
                    newNode.setRight(right);
                }

                this.push(newNode);
                log.debug("Operator {} inserted", operator);
            } else {
                TreeNode newNode = new TreeNode(word, false);
                this.push(newNode);
                log.debug("Expression {} inserted", word);
            }
        } catch (Exception e) {
            log.error("Invalid Expression: {}", word);
            return false;
        }

        return true;
    }

    /**
     * Checks whether a string belongs to the list of supported
     * boolean operators.
     *
     * @param operator a boolean operator
     * @return boolean
     */
    private static boolean isOperator(String operator) {
        return ClassificationSyntax.LOGICAL_OPERATORS.contains(operator.toLowerCase());
    }

    /**
     * Checks whether a string belongs to the list of supported
     * boolean operators.
     *
     * @param operator a boolean operator
     * @return boolean
     */
    private static boolean isSupportedOperator(String operator) {
        return ClassificationSyntax.LOGICAL_OPERATORS.contains(operator.toLowerCase());
    }

    /**
     * Turn an operator into a unified symbol to enforce consistency.
     * This is necessary because a logical AND can be expressed both
     * as `and` and `&amp&amp`.
     *
     * @param operator a boolean operator
     * @return a unified operator
     */
    private static String unifyOperator(String operator) {
        if (operator.equals("and") || operator.equals("&&")) {
            operator = ClassificationSyntax.LOGICAL_AND;
        } else if (operator.equals("or") || operator.equals("||")) {
            operator = ClassificationSyntax.LOGICAL_OR;
        } else if (operator.equals("not") || operator.equals("!")) {
            operator = ClassificationSyntax.LOGICAL_NOT;
        }

        return operator;
    }

    /**
     * Implementation of Dijkstra's shunting-yard algorithm for parsing
     * expressions and encoding them into a binary expression tree.
     *
     * @throws RuntimeException in the case that the stack is
     * not empty after the insertion of a pattern or in the case
     * that the user input has unbalanced parentheses
     */
    @Override
    public void buildTree() throws RuntimeException {
        // Check for unbalanced parentheses
        if (!hasBalancedParentheses(this.trafficClass)) {
            throw new InputConfigurationException("Unbalanced parentheses in rule");
        }

        ConcurrentLinkedDeque<String> operators = new ConcurrentLinkedDeque<String>();

        String  expression = "";
        boolean buildingExpression = false;

        for (String token : this.trafficClass.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }

            // End of an expression
            if (ClassificationSyntax.SYMBOLS.contains(token) && buildingExpression) {
                // Add it to the tree
                if (!this.insert(expression.trim())) {
                    throw new InputConfigurationException("Problem while parsing expression: " + this.trafficClass);
                }
                // Reset the expression string for the next one
                expression = "";
                buildingExpression = false;
            }

            // Add left parenthesis to the stack
            if (token.equals("(")) {
                operators.push(token);
            // Operators as well
            } else if (ClassificationSyntax.LOGICAL_OPERATORS.contains(token)) {
                operators.push(token);
            // Indicates that we should pop
            } else if (token.equals(")")) {
                // Add operators to the tree until you see a left parenthesis
                while (!operators.peek().equals("(")) {
                    if (!this.insert(operators.pop())) {
                        throw new InputConfigurationException("Problem while parsing expression: " + this.trafficClass);
                    }
                }
                // Remove the left parenthesis
                operators.pop();
            } else {
                buildingExpression = true;
                expression += token;
                expression += " ";
            }
        }

        if (!operators.isEmpty()) {
            throw new ParseException("The classification tree is built incorrectly");
        }
    }

    /**
     * Verify that the textual packet filters in each traffic class comply with the desired syntax.
     */
    @Override
    public void verifyTree() {
        // Get the mappings between traffic classes and primitive TextualPacketFilter objects
        for (Map.Entry<String, Set<TextualPacketFilter>> entry : this.datapathPacketFilters.entrySet()) {
            // For each primitive textual packet filter
            for (TextualPacketFilter pf : entry.getValue()) {
                // Verify this textual packet filter
                TextualPacketFilterVerifier packetFilterVerifier = new TextualPacketFilterVerifier(pf);
                packetFilterVerifier.verify();
            }
        }
    }

    /**
     * Traverse the binary classification tree to compute
     * a list of one or more datapath rules (or traffic classes).
     * Branches occur when OR operators are met.
     *
     * @throws ParseException (RuntimeException) in the case that the parsing
     *         of the tree is incorrect
     *
     * @return a list of traffic classes
     */
    @Override
    public List<String> toHardwareRules() {
        // Find the leaves of the tree
        discoverLeaves(this.root(), this.leavesList);

        // Find the junction nodes
        discoverJunctions(this.root(), this.junctionsList);

        // Populate traffic class information from leaves to junctions.
        if (!this.populateJunctions()) {
            throw new ParseException(
                "Error in the precomputation phase of the datapath traffic classes"
            );
        }

        // Trees without AND rules will finish here.
        if (this.computationIsComplete()) {
            return this.datapathTrafficClasses;
        }

        // Iteratively process and merge junctions unti we derive the final traffic classes.
        if (!this.harvestJunctions()) {
            throw new ParseException(
                "Error in the computation phase of the datapath traffic classes"
            );
        }

        // The computation must finish here
        if (!this.computationIsComplete()) {
            throw new ParseException(
                "The computation of the datapath traffic classes has not converged"
            );
        }

        return this.datapathTrafficClasses;
    }

    /**
     * Check whether the traffic classes computation process has been completed.
     * If so decompose and verify each traffic class.
     *
     * @return boolean status
     */
    private boolean computationIsComplete() {
        // We are not done yet..
        if ((this.leavesList.size() != 0) || (this.junctionsList.size() != 0)) {
            return false;
        }

        // Bingo!
        log.debug("The datapath rules are successfully computed!");

        // Verify each rule and decompose it into primitives (headerField + operator + headerValue).
        if (!this.decomposeAndVerifyDatapathRules()) {
            throw new ParseException(
                "Failed to decompose the datapath traffic classes into packet processing primitives"
            );
        }

        return true;
    }

    /**
     * Process each leaf of the classification tree to derive either:
     * |--> a direct traffic class rule or
     * |--> a set of sub-traffic classes that are hooked to a junction node.
     *
     * @return boolean status
     */
    private boolean populateJunctions() {
        int leavesNumber = this.leavesList.size();

        /**
         * Start from the leaves and merge traffic classes until you encounter a
         * junction node (AND operators or the root).
         * After this loop, all the leaves must be either aggregated by junctions
         * or translated into datapath traffic classes.
         */
        Collections.sort(this.leavesList);

        for (Iterator<TreeNode> iter = this.leavesList.listIterator(); iter.hasNext();) {
            TreeNode node = iter.next();
            String subTrafficClass = node.data().toString();

            // Create a dummy node to act as an origin indicator (will be recursively updated).
            TreeNode origin = new TreeNode("");

            // Useful flags for the
            AtomicBoolean firstCall = new AtomicBoolean(true);
            AtomicInteger negations = new AtomicInteger(0);

            // A junction and an origin are hopefully returned
            TreeTuple tuple = this.hookToJunction(node, origin, firstCall, negations);
            TreeNode junction = tuple.junction();
            origin = tuple.origin();

            // Odd number of negations means negation
            if (negations.intValue() % 2 != 0) {
                subTrafficClass = "! " + subTrafficClass;
            }

            log.debug("Leaf: {}", subTrafficClass);

            /**
             * No junction is present between this leaf and the root.
             * This is a traffic class on its own!
             */
            if (junction == null) {
                log.debug("\thas direct traffic class: {}", subTrafficClass);
                this.datapathTrafficClasses.add(subTrafficClass);
                leavesNumber--;
                iter.remove();
                continue;
            }

            // There is a junction
            log.debug("\twith  junction: {}", junction.data().toString());
            log.debug("\twith    origin: {}", origin.data().toString());
            log.debug("\twith negations: {}", negations.intValue());

            List<String> exTCs = null;

            // The origin indicates to update the traffic classes on the left side
            if (junction.left() == origin) {
                // log.info("left");
                if (this.collectedTrafficClassesLeft.containsKey(junction)) {
                    exTCs = this.collectedTrafficClassesLeft.get(junction);
                } else {
                    exTCs = new ArrayList<String>();
                }
                exTCs.add(subTrafficClass);
                this.collectedTrafficClassesLeft.put(junction, exTCs);
            // The origin indicates to update the traffic classes on the right side
            } else if (junction.right() == origin) {
                // log.info("right");
                if (this.collectedTrafficClassesRight.containsKey(junction)) {
                    exTCs = this.collectedTrafficClassesRight.get(junction);
                } else {
                    exTCs = new ArrayList<String>();
                }
                exTCs.add(subTrafficClass);
                this.collectedTrafficClassesRight.put(junction, exTCs);
            } else {
                log.error(
                    "Consistency problem while computing the datapath traffic classes of {}",
                    this.trafficClass
                );
                return false;
            }

            log.debug("\tThis leaf is successfully processed");
            log.debug("");

            // We are done with this leaf
            iter.remove();
        }

        // All leaves must have been processed by now and junction nodes might be created
        if (this.leavesList.size() != 0) {
            log.error(
                "Consistency problem while computing the datapath traffic classes of {}",
                this.trafficClass
            );
            return false;
        }

        return true;
    }

    /**
     * Iteratively merge the junction nodes of the classification tree,
     * until they are exhausted. In this case, the final number of traffic
     * classes is derived.
     *
     * @return boolean status
     */
    private boolean harvestJunctions() {
        int junctionsNumber  = this.junctionsList.size();
        int iterationsNumber = 0;

        /**
         * As long as there are junctions to process, pick one and do as follows:
         * |--> If this junction has both branches set with traffic classes and it does not
         *      rely on an upper level junction, then store its traffic classes and remove it
         *      from the list of active junctions.
         * |--> If this junction has both branches set with traffic classes, but relies on an
         *      upper level junction, then merge it with the upper level junction.
         * |--> If this junction has either of it's branches unset, then move to the next junction.
         */
        while (junctionsNumber > 0) {

            iterationsNumber++;

            log.debug("====================== Iteration {} ======================", iterationsNumber);
            for (Iterator<TreeNode> iter = this.junctionsList.listIterator(); iter.hasNext();) {
                TreeNode junction   = iter.next();
                String junctionData = junction.data().toString();

                log.debug("Junction: {}", junctionData);

                // Create a dummy node to act as an origin indicator (will be recursively updated).
                TreeNode origin = new TreeNode("");

                // Useful flags for the
                AtomicBoolean firstCall = new AtomicBoolean(true);
                AtomicInteger negations = new AtomicInteger(0);

                // A junction and an origin are hopefully returned
                TreeTuple tuple = this.hookToJunction(junction, origin, firstCall, negations);
                TreeNode nextJunction = tuple.junction();
                origin = tuple.origin();

                // This is a complete junction
                if (!this.junctionIsPartial(junction)) {
                    List<String> junctionTrafficClasses = this.crossProduct(
                        this.collectedTrafficClassesLeft.get(junction),
                        this.collectedTrafficClassesRight.get(junction)
                    );

                    iter.remove();

                    // This junciton does not have any higher junction to hook, we are done!
                    if (nextJunction == null) {
                        log.debug("\tDirect traffic class from junction");
                        this.datapathTrafficClasses.addAll(junctionTrafficClasses);
                    } else {
                        log.debug("\tMerging with the upper junction");
                        List<String> exTCs = null;

                        // The origin indicates to update the traffic classes on the left side
                        if (nextJunction.left() == origin) {
                            if (this.collectedTrafficClassesLeft.containsKey(nextJunction)) {
                                exTCs = this.collectedTrafficClassesLeft.get(nextJunction);
                            } else {
                                exTCs = new ArrayList<String>();
                            }
                            exTCs.addAll(junctionTrafficClasses);
                            this.collectedTrafficClassesLeft.put(nextJunction, exTCs);
                        // The origin indicates to update the traffic classes on the right side
                        } else if (nextJunction.right() == origin) {
                            if (this.collectedTrafficClassesRight.containsKey(nextJunction)) {
                                exTCs = this.collectedTrafficClassesRight.get(nextJunction);
                            } else {
                                exTCs = new ArrayList<String>();
                            }
                            exTCs.addAll(junctionTrafficClasses);
                            this.collectedTrafficClassesRight.put(nextJunction, exTCs);
                        } else {
                            log.info(
                                "Consistency problem while computing the datapath traffic classes of {}",
                                this.trafficClass
                            );
                            return false;
                        }
                    }
                } else {
                    log.debug("\tPARTIAL junction cannot be harvested in this iteration");
                    continue;
                }
            }
            log.debug("==========================================================");

            junctionsNumber = this.junctionsList.size();

            log.debug("");
        }

        return true;
    }

    /**
     * Checks whether a given junction node has both of it's branches set
     * with traffic classes. This indicates that the junction node is complete,
     * thus it is not considered as partial.
     *
     * @return boolean status
     */
    private boolean junctionIsPartial(TreeNode junction) {
        if (junction == null) {
            return false;
        }

        // If either of its branches is empty, then the junction is partial
        if (!this.collectedTrafficClassesLeft.containsKey(junction) ||
            !this.collectedTrafficClassesRight.containsKey(junction)) {
            return true;
        }

        return false;
    }

    /**
     * Starts from a leaf node (a the bottom of the tree) and climbs up
     * until the first junction node is found. We return this junction node
     * together with the child node of this junction toward our leaf
     * (to identify whether the leaf resides at the right or left from the
     * junction node).
     *
     * @param node a leaf node for which we are looking a junction node
     * @param origin the child of the identified junction toward the leaf
     * @param boolean flag to indicate the first call of the recursions
     * @param integer to indicate the number of negations seen across the path
     * @return a pair of tree nodes (the junction and the origin)
     */
    private TreeTuple hookToJunction(
            TreeNode node, TreeNode origin, AtomicBoolean firstCall, AtomicInteger negationsNb) {
        // We reached the root without encountering a junction
        if (node == null) {
            return new TreeTuple(null, origin);
        }

        if (node.isOperator() && node.data().equals(ClassificationSyntax.LOGICAL_AND)) {
            // An AND node (junction) has become a leaf, set the origin properly
            if (firstCall.get()) {
                origin = node;
                firstCall.set(false);
                return this.hookToJunction(node.parent(), origin, firstCall, negationsNb);
            }
            return new TreeTuple(node, origin);
        } else if (node.isOperator() && node.data().equals(ClassificationSyntax.LOGICAL_OR)) {
            origin = node;
            firstCall.set(false);
            return this.hookToJunction(node.parent(), origin, firstCall, negationsNb);
        } else if (node.isOperator() && node.data().equals(ClassificationSyntax.LOGICAL_NOT)) {
            origin = node;
            negationsNb.incrementAndGet();
            firstCall.set(false);
            return this.hookToJunction(node.parent(), origin, firstCall, negationsNb);
        } else {
            origin = node;
            firstCall.set(false);
            return this.hookToJunction(node.parent(), origin, firstCall, negationsNb);
        }
    }

    /**
     * Take left and right hand side lists of sub-traffic classes of a certain node
     * and calculate their cross product to derive larger traffic classes.
     *
     * @param left hand side list of sub-traffic classes
     * @param right hand side list of sub-traffic classes
     * @return the cross product of the inputs in a list
     */
    private List<String> crossProduct(
            List<String> collectedTrafficClassesLeft,
            List<String> collectedTrafficClassesRight) {
        List<String> product = new ArrayList<String>();

        if (collectedTrafficClassesLeft == null ||
            collectedTrafficClassesRight == null) {
            return null;
        }

        // Fetch the left hand side sub-traffic classes of this node, one by one
        for (String leftRule : collectedTrafficClassesLeft) {
            // Fetch the right hand side sub-traffic classes of this node
            for (String rightRule : collectedTrafficClassesRight) {
                String ordered = "";
                if (leftRule.compareTo(rightRule) < 0) {
                    ordered = leftRule  + " " + ClassificationSyntax.LOGICAL_AND + " " + rightRule;
                } else {
                    ordered = rightRule + " " + ClassificationSyntax.LOGICAL_AND + " " + leftRule;
                }

                product.add(ordered);
            }
        }

        return product;
    }

    /**
     * Prefix traversal of the tree to identify leaf nodes.
     * Leaf is a tree node without children.
     *
     * @param a tree node to start our search (root shall be used)
     * @param an empty (but initialized) list of tree nodes, where we store the leaves
     */
    private void discoverLeaves(TreeNode node, List<TreeNode> leavesList) {
        if (node == null) {
            return;
        }

        // This is a leaf
        if (node.isLeaf()) {
            leavesList.add(node);
        }

        // Continue your journey
        discoverLeaves(node.left(),  leavesList);
        discoverLeaves(node.right(), leavesList);
    }

    /**
     * Prefix traversal of the tree to identify junction nodes.
     * AND operators are the junctions nodes.
     *
     * @param a tree node to start our search (root shall be used)
     * @param an empty (but initialized) list of tree nodes, where we store the junctions
     */
    private void discoverJunctions(TreeNode node, List<TreeNode> junctionsList) {
        if (node == null) {
            return;
        }

        // This is a junction
        if (node.isOperator() && node.data().toString().equals(ClassificationSyntax.LOGICAL_AND)) {
            junctionsList.add(node);
        }

        // Continue your journey
        discoverJunctions(node.left(),  junctionsList);
        discoverJunctions(node.right(), junctionsList);
    }

    /**
     * Prefix traversal of the tree to identify operand nodes.
     * Nodes that are not operators are considered as operands.
     *
     * @param a tree node to start our search (root shall be used)
     * @param an empty (but initialized) list of tree nodes, where we store the operands
     */
    private void discoverOperands(TreeNode node, List<TreeNode> operandsList) {
        if (node == null) {
            return;
        }

        // Whatever is not an operator, it is an operand
        if (!node.isOperator()) {
            operandsList.add(node);
        }

        // Continue your journey
        discoverOperands(node.left(),  operandsList);
        discoverOperands(node.right(), operandsList);
    }

    /**
     * Each datapath rule is verified and mapped to a set
     * of TextualPacketFilter objects (headerField + operator + headerValue).
     *
     * @return boolean status
     */
    public boolean decomposeAndVerifyDatapathRules() {
        // Iterate through the list of datapath rules
        for (String rule : this.datapathTrafficClasses) {

            // Replace some auxiliary keywords with $ signs
            String modifiedRule = "";
            if (rule.contains("and") || rule.contains("&&")) {
                modifiedRule = rule.replaceAll("and", "\\$").replaceAll("&&", "\\$").trim();
            } else {
                modifiedRule = rule;
            }

            modifiedRule = modifiedRule.trim();

            boolean isAllowed = true;
            boolean isIpFilter = false;
            if (modifiedRule.equals("deny all") || modifiedRule.contains("drop all")) {
                isAllowed = false;
            } else if (modifiedRule.contains("deny") || modifiedRule.contains("drop")) {
                modifiedRule = modifiedRule.replaceAll("drop", "").replaceAll("deny", "");
                isAllowed = false;
                isIpFilter = true;
            } else if (modifiedRule.contains("allow")) {
                modifiedRule = modifiedRule.replaceAll("allow", "");
                isIpFilter = true;
            }

            modifiedRule = modifiedRule.trim();

            // Split based on the dollar sign
            String[] patterns = modifiedRule.split("\\$");

            // Keep a list with all the matches for verification purposes
            List<String> fieldMatches = new ArrayList<String>();
            List<TextualPacketFilter> matches = new ArrayList<TextualPacketFilter>();

            for (String pattern : patterns) {
                if (pattern.isEmpty() || pattern.equals(" ")) {
                    continue;
                }

                // Mark and remove negation
                boolean isNegated = false;
                if (pattern.contains("!")) {
                    isNegated = true;
                    pattern = pattern.replaceAll("!", "");
                }

                pattern = pattern.trim();

                // Convert this pattern into a TextualPacketFilter
                TextualPacketFilter pf = this.constructPacketFilter(pattern, isNegated, isAllowed);
                if (pf == null) {
                    log.error("Unable to construct a textual packet filter out of the pattern: {}", pattern);
                    return false;
                }
                matches.add(pf);
                fieldMatches.add(pf.headerField());
            }

            // Convert the matches into a set
            Set<String> uniqueFieldMatches = Sets.<String>newConcurrentHashSet(fieldMatches);
            Set<TextualPacketFilter> uniqueMatches = Sets.<TextualPacketFilter>newConcurrentHashSet(matches);

            // Convert back
            rule = modifiedRule.replaceAll("\\$", "and");
            if (isIpFilter) {
                String action = isAllowed ? "allow " : "drop ";
                rule = action + rule;
            }

            // The pattern has passed the verification, add it to memory
            this.datapathPacketFilters.put(rule, uniqueMatches);
        }

        return true;
    }

    /**
     * Split a traffic class pattern into its primitive components
     * and perform a sanity check.
     *
     * @param trafficClassPattern a traffic class pattern
     * @param isNegated whether it contains a negation or not
     * @param isAllowed whether it is allowed or not
     * @return a textual packet filter with the match information
     */
    private TextualPacketFilter constructPacketFilter(
            String trafficClassPattern, boolean isNegated, boolean isAllowed) {
        String pattern  = "";
        String operator = "";
        String value    = "";
        String remSubString = "";

        boolean matched = false;
        int expectedSubPatterns = 0;

        // Iterate through the list of supported header matches
        for (String p : ClassificationSyntax.PATTERNS) {
            // We have a match
            if (trafficClassPattern.startsWith(p)) {
                pattern = p;
                remSubString = trafficClassPattern.replace(p, "").trim();
                expectedSubPatterns = ClassificationSyntax.PATTERN_MATCH_INFO.get(p).intValue();
                matched = true;
                break;
            }
        }

        if (!matched) {
            log.error("No match for: {}", trafficClassPattern);
            return null;
        }

        if (remSubString.isEmpty() && (expectedSubPatterns == 0)) {
            return new TextualPacketFilter(pattern, operator, value, isNegated, isAllowed);
        }

        String[] tokens = remSubString.split("\\s+");

        if (tokens.length > expectedSubPatterns) {
            log.error("Maximum expected tokens after {} are: {}", pattern, expectedSubPatterns);
            return null;
        } else if (tokens.length == expectedSubPatterns) {
            if (tokens.length > 0) {
                operator = tokens[0].trim();
                value    = tokens[1].trim();
            }
        } else {
            if (tokens.length != 1) {
                log.error("Expected only value after {}", pattern);
                return null;
            }
            // The operator `==` is implied
            operator = "==";
            value    = tokens[0].trim();
        }

        return new TextualPacketFilter(pattern, operator, value, isNegated, isAllowed);
    }

    /**
     * Checks whether a traffic class contains balanced parentheses.
     *
     * @param a traffic class
     * @return boolean status
     */
    private static boolean hasBalancedParentheses(String trafficClass) {
        short counter = 0;
        char[] array = trafficClass.toCharArray();
        for (char ch : array) {
            if (ch == '(') {
                counter++;
            } else if (ch == ')') {
                counter--;
            }

            if (counter < 0) {
                return false;
            }
        }

        if (counter != 0) {
            return false;
        }

        return true;
    }

}
