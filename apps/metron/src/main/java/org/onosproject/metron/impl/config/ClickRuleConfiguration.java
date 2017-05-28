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

package org.onosproject.metron.impl.config;

import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.ClassificationTreeInterface;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.net.ClickFlowRuleAction;

import org.onosproject.metron.impl.classification.ClassificationTree;
import org.onosproject.metron.impl.classification.VisualTree;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.core.ApplicationId;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Component responsible for automatically loading a Click classifier's rule configuration.
 */
public class ClickRuleConfiguration extends RuleConfiguration {

    protected static final String TRAFFIC_CLASS = "trafficClass";

    /**
     * Each Click rule configuration can be
     * encoded as a binary classification tree.
     */
    protected Map<String, ClassificationTreeInterface> classificationTrees = null;

    /**
     * Each binary classification tree gets
     * visualization services using this object.
     */
    protected Map<String, VisualTree> visualTrees = null;

    public ClickRuleConfiguration(ApplicationId appId, String elemId, String configFileName) {
        super(appId, elemId, configFileName);
        init();
    }

    public ClickRuleConfiguration(ApplicationId appId, String elemId, File configFile) {
        super(appId, elemId, configFile);
        init();
    }

    public void init() {
        this.classificationTrees = new ConcurrentHashMap<String, ClassificationTreeInterface>();
        this.visualTrees = new ConcurrentHashMap<String, VisualTree>();
    }

    public ClassificationTreeInterface getClassificationTree(String trafficClass) {
        return this.classificationTrees.get(trafficClass);
    }

    public void addClassificationTree(String trafficClass, ClassificationTreeInterface tree) {
        this.classificationTrees.put(trafficClass, tree);
    }

    public VisualTree getVisualTree(String trafficClass) {
        return this.visualTrees.get(trafficClass);
    }

    public void addVisualTree(String trafficClass, VisualTree visualTree) {
        this.visualTrees.put(trafficClass, visualTree);
    }

    public void loadRules() throws IllegalArgumentException {
        if (this.entriesArray == null) {
            log.warn("Configuration to parse is unavailable");
            return;
        }

        short i = 0;
        for (JsonNode tc : this.entriesArray) {
            if ((i > 0) && (i < this.entriesArray.size())) {
                log.debug("");
            }

            String trafficClass = tc.get(TRAFFIC_CLASS).asText().toLowerCase();
            log.debug("Traffic class: {}", trafficClass);

            int outputPort = tc.get(OUTPUT_PORT).asInt();
            checkArgument(outputPort >= 0, "Rule output port must be greater or equal than zero");
            log.debug("\tOutput Port: {}", outputPort);

            int priority = tc.get(PRIORITY).asInt();
            checkArgument(priority > 0, "Rule priority must be greater than zero");
            log.debug("\t   Priority: {}", priority);

            String actionStr = tc.get(ACTION).asText().toUpperCase();
            ClickFlowRuleAction action = Common.<ClickFlowRuleAction>enumFromString(
                ClickFlowRuleAction.class, actionStr
            );
            checkNotNull(action,
                "Incompatible Click flow rule action. Choose one in: " + Common.<ClickFlowRuleAction>
                enumTypesToString(ClickFlowRuleAction.class));
            log.debug("\t     Action: {}", action);

            // Build a classification tree to encode this traffic class
            ClassificationTreeInterface tree = this.buildClassificationTree(
                trafficClass, outputPort, priority, action
            );
            this.addClassificationTree(trafficClass, tree);

            // Translate this traffic class into rules (HW and/or SW)
            this.toDatapathRules(trafficClass, outputPort, priority, action);
            // Now it is safe to insert this class into our memory
            this.addDatapathTrafficClass(trafficClass, outputPort);

            // Get tree visualization services
            VisualTree visualTree = new VisualTree(tree);
            this.addVisualTree(trafficClass, visualTree);
            // visualTree.printHwTrafficClasses();

            i++;
        }

        // The entire ruleset
        this.printDatapathRules();
    }

    /**
     * Encodes a traffic class into a binary classification tree.
     *
     * @param a traffic class
     * @param the output port of this traffic class
     * @param the priority of this traffic class
     * @param the action of this traffic class
     * @throws RuntimeException if the verification of the tree fails
     * @return a binary classification tree that encodes this traffic class
     */
    private ClassificationTreeInterface buildClassificationTree(
        String trafficClass, int outputPort, int priority, ClickFlowRuleAction action) {
        /**
         * Post-process the traffic class to simplify the parsing operations
         * Otherwise, it requires regular expressions, such as
         * "\\s*[\\(\\)]+\\s*" to split the tokens
         */
        trafficClass = "(" + trafficClass + ")";
        trafficClass = trafficClass.replace("(", " ( ");
        trafficClass = trafficClass.replace(")", " ) ");
        trafficClass = trafficClass.replace("!", " ! ");

        // Build the classification tree
        ClassificationTreeInterface tree = new ClassificationTree(trafficClass, outputPort, priority, action);
        try {
            tree.buildTree();
        } catch (RuntimeException rtEx) {
            throw rtEx;
        }

        // Fetch the datapath traffic classes that derive from the classification tree.
        List<String> dpTrafficClasses = tree.toHardwareRules();

        // Verify that the constructed tree complies to the syntax
        tree.verifyTree();

        return tree;
    }

    /**
     * Translates the binary classification tree associated with the input traffic class
     * into a set of hardware (SDN rules) and/or software (NFV rules) traffic classes.
     * Hardware-compliant traffic classes are further translated into FlowRule objects.
     *
     * @param trafficClass the lookup rule
     * @param outputPort the output port of this rule
     * @param priority the priority of this rule
     * @param action the action of this rule
     */
    public void toDatapathRules(
            String trafficClass, int outputPort, int priority, ClickFlowRuleAction action) {
        // Fetch the classification tree of this traffic class
        ClassificationTreeInterface tree = this.getClassificationTree(trafficClass);
        // Fetch the datapath traffic classes derived from the tree
        List<String> dpTrafficClasses = tree.datapathTrafficClasses();

        // Each of them can be translated into a hardware or software rule
        for (String tc : dpTrafficClasses) {
            Set<TextualPacketFilter> packetFilters = tree.getPacketFiltersOf(tc);

            // Hardware-compliant rule
            if (TextualPacketFilter.isHardwareCompliant(packetFilters)) {
                FlowRule rule = this.trafficClassToHwRule(packetFilters, outputPort, priority, action);
                this.addSdnTrafficClass(rule);
            // Software-compliant rule
            } else {
                this.trafficClassToSwRule(tc, outputPort, priority, action);
            }
        }

        return;
    }

    /**
     * Turns a traffic class (set of textual packet filters) into a hardware rule.
     *
     * @param packetFilters a set of textual packet filters
     * @param outputPort the output port of this rule
     * @param priority the priority of this rule
     * @param action the action of this rule
     * @return a FlowRule object
     */
    private FlowRule trafficClassToHwRule(
            Set<TextualPacketFilter> packetFilters, int outputPort, int priority, ClickFlowRuleAction action) {
        // Rule builder
        FlowRule.Builder rule = new DefaultFlowRule.Builder();

        DeviceId deviceId = DeviceId.deviceId(this.blockId());

        rule.forDevice(deviceId);
        rule.fromApp(this.applicationId());
        rule.withPriority(priority);
        rule.makePermanent();

        /**
         * MATCH
         */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        // Integrate all the textual packet filters into the traffic selector
        for (TextualPacketFilter pf : packetFilters) {
            pf.updateTrafficSelector(selector, packetFilters);
        }
        rule.withSelector(selector.build());

        /**
         * ACTION
         */
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        // Drop rule
        if (action == ClickFlowRuleAction.DROP) {
            treatment.drop();
        // Allow rule
        } else {
            treatment.add(
                Instructions.createOutput(PortNumber.portNumber(outputPort))
            );
        }
        rule.withTreatment(treatment.build());

        return rule.build();
    }

    /**
     * Turns a traffic class into a software rule.
     *
     * @param trafficClass a traffic class
     * @param outputPort the output port of this rule
     * @param priority the priority of this rule
     * @param action the action of this rule
     */
    private void trafficClassToSwRule(
        String trafficClass, int outputPort, int priority, ClickFlowRuleAction action) {
        this.addNfvTrafficClass(trafficClass, outputPort);
    }

    /**
     * Prints the entire classifier as a set of rules.
     */
    public void printDatapathRules() {
        this.datapathTrafficClasses().forEach((k, v) -> log.debug(
            "Datapath traffic class: " + k + " --> Port: " + v
        ));
        log.info("");
    }

}
