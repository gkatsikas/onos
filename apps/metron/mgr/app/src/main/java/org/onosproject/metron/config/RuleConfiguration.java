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

package org.onosproject.metron.config;

import org.onosproject.metron.api.config.RuleConfigurationInterface;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;

import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component responsible for automatically loading a rule configuration file.
 */
public abstract class RuleConfiguration
        extends BasicConfiguration
        implements RuleConfigurationInterface {

    protected static final Logger log = getLogger(RuleConfiguration.class);

    /**
     * Map each traffic class to an output port.
     * Each traffic class has a primitive format
     * understood by the fast datapath engine.
     */
    protected Map<String, Integer> datapathTrafficClasses = new ConcurrentSkipListMap<String, Integer>();

    /**
     * The goal is to translate the original rule configuration
     * into a set of SDN + NFV rules.
     * The former will be executed by the switches toward
     * the path to a server, while the latter wiil be executed
     * by the server itself.
     */
    protected List<FlowRule>       sdnTrafficClasses = Collections.synchronizedList(new ArrayList<FlowRule>());
    protected Map<String, Integer> nfvTrafficClasses = new ConcurrentSkipListMap<String, Integer>();

    public RuleConfiguration(ApplicationId appId, String blockId, String configFileName) {
        super(appId, blockId, configFileName);
    }

    public RuleConfiguration(ApplicationId appId, String blockId, File configFile) {
        super(appId, blockId, configFile);
    }

    public Map<String, Integer> datapathTrafficClasses() {
        return this.datapathTrafficClasses;
    }

    public void addDatapathTrafficClass(String trafficClass, Integer outputPort) {
        this.datapathTrafficClasses.put(trafficClass, outputPort);
    }

    public List<FlowRule> sdnTrafficClasses() {
        return this.sdnTrafficClasses;
    }

    public void setSdnTrafficClasses(List<FlowRule> sdnTrafficClasses) {
        this.sdnTrafficClasses = sdnTrafficClasses;
    }

    public void addSdnTrafficClass(FlowRule sdnTrafficClass) {
        this.sdnTrafficClasses.add(sdnTrafficClass);
    }

    public Map<String, Integer> nfvTrafficClasses() {
        return this.nfvTrafficClasses;
    }

    public void setNfvTrafficClasses(Map<String, Integer> nfvTrafficClasses) {
        this.nfvTrafficClasses = nfvTrafficClasses;
    }

    public void addNfvTrafficClass(String nfvTrafficClass, Integer outputPort) {
        this.nfvTrafficClasses.put(nfvTrafficClass, outputPort);
    }

    @Override
    public void printRules() {
        log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        // The hardware-part of the ruleset
        if ((this.sdnTrafficClasses == null) || (this.sdnTrafficClasses.isEmpty())) {
            log.warn("No hardware rules to print");
            return;
        } else {
            this.printSdnRules();
        }

        // The software-part of the ruleset
        if ((this.nfvTrafficClasses == null) || (this.nfvTrafficClasses.isEmpty())) {
            log.warn("No software rules to print");
            return;
        } else {
            this.printNfvRules();
        }

        log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    @Override
    public String datapathTrafficClassesToString() {
        Map<String, Integer> rules = this.datapathTrafficClasses();
        if ((rules == null) || (rules.size() == 0)) {
            return "";
        }

        String conf = "";

        /**
         * The rules in the map might not be in order,
         * but they should appear in order
         * (by increasing output port number).
         */
        // String[] array = new String[rules.size()];
        // for (Map.Entry<String, Integer> rule : rules.entrySet()) {
        //     // Reflects the rule's priority
        //     int index = rule.getValue().intValue();

        //     // This is occupied, traverse to find the first available
        //     if (array[index] != null) {
        //         int i = index;
        //         while (i < rules.size()) {
        //             if (array[i] == null) {
        //                 array[i] = rule.getKey();
        //                 break;
        //             }
        //             i++;
        //         }
        //     } else {
        //         array[index] = rule.getKey();
        //     }
        // }

        // for (String s : array) {
        //     conf += s + ", ";
        // }

        for (Map.Entry<String, Integer> rule : rules.entrySet()) {
            conf += rule.getKey() + ", ";
        }

        // Remove the trailing ", "
        conf = conf.substring(0, conf.length() - 2);

        return conf;
    }

    @Override
    public int numberOfDiscreteOutputPorts() {
        Map<String, Integer> rules = this.datapathTrafficClasses();
        if (rules == null) {
            return 0;
        }

        /**
         * The rules in the map might not be in order,
         * but they should appear in order
         * (by increasing output port number).
         */
        List<Integer> discretePorts = Collections.synchronizedList(new ArrayList<Integer>());
        for (Map.Entry<String, Integer> rule : rules.entrySet()) {
            if (discretePorts.contains(rule.getValue())) {
                continue;
            }
            discretePorts.add(rule.getValue());
        }

        return discretePorts.size();
    }

    /**
     * Prints the software-based part of the classifier as a set of rules.
     */
    public void printNfvRules() {
        this.nfvTrafficClasses.forEach(
            (k, v) -> log.info("Software traffic class: " + k + " --> Port: " + v)
        );
        log.info("");
    }

    /**
     * Prints the hardware-based part of the classifier as a set of rules.
     */
    public void printSdnRules() {
        this.sdnTrafficClasses.forEach(
            rule -> this.printSdnRule(rule)
        );
        log.info("");
    }

    /**
     * Prints a single hardware-based rule.
     */
    private void printSdnRule(FlowRule rule) {
        log.info("Hardware traffic class:");

        if (rule == null) {
            log.warn("\tEmpty");
            return;
        }

        log.info("   Flow ID: {}", Long.toString(rule.id().value()));
        log.info(" Device ID: {}", rule.deviceId());
        log.info("  Priority: {}", rule.priority());
        log.info("   Timeout: {}", rule.timeout());
        log.info(" Permanent: {}", rule.isPermanent());
        log.info("   Matches: ");
        for (Criterion criterion : rule.selector().criteria()) {
            log.info("\t\t{}", criterion.toString());
        }
        log.info("   Actions: ");
        for (Instruction instruction : rule.treatment().allInstructions()) {
            log.info("\t\t{}", instruction.toString());
        }
        log.info("");
    }

    /**
     * Parses the loaded configuration and stores a list of rules.
     */
    @Override
    public abstract void loadRules() throws IllegalArgumentException;

}
