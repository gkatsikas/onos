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
import org.onosproject.net.flow.instructions.Instruction.Type;
import org.onosproject.net.flow.instructions.Instructions;

import com.google.common.base.Strings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.util.Tools.nullIsIllegal;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Component responsible for automatically loading OpenFlow rule configuration.
 */
public class OpenFlowRuleConfiguration
        extends RuleCodecContext
        implements RuleConfigurationInterface {

    protected static final Logger log = getLogger(OpenFlowRuleConfiguration.class);

    protected File configFile;
    protected String blockId;
    protected ApplicationId appId;

    protected ObjectNode root       = null;
    protected ArrayNode  rulesArray = null;

    /**
     * Map each traffic class to an output port.
     * Each traffic class has a primitive format
     * understood by the fast datapath engine.
     */
    protected Map<String, Integer> datapathTrafficClasses = new ConcurrentHashMap<String, Integer>();

    /**
     * An OpenFlow rule configuration can only generate
     * SDN traffic classes.
     */
    protected List<FlowRule> sdnTrafficClasses = new ArrayList<FlowRule>();

    protected static final String ENTRIES = "entries";
    protected static final String ENTRIES_ARRAY_REQUIRED = "Entries' array was not specified";

    public OpenFlowRuleConfiguration(ApplicationId appId, String blockId, String configFileName) {
        checkNotNull(
            appId,
            "Specify an application ID"
        );

        checkArgument(
            !Strings.isNullOrEmpty(blockId),
            "Specify an element ID that will host the rules"
        );

        checkArgument(
            !Strings.isNullOrEmpty(configFileName),
            "Specify a filename that contains the rules"
        );

        this.appId       = appId;
        this.blockId   = blockId;
        this.configFile = new File(configFileName);
    }

    public OpenFlowRuleConfiguration(ApplicationId appId, String blockId, File configFile) {
        checkNotNull(
            appId,
            "Specify an application ID"
        );

        checkArgument(
            !Strings.isNullOrEmpty(blockId),
            "Specify an element ID that will host the rules"
        );

        checkNotNull(
            configFile,
            "Specify a file that contains the rules"
        );

        this.appId       = appId;
        this.blockId   = blockId;
        this.configFile = configFile;
    }

    public File configFile() {
        return this.configFile;
    }

    public String blockId() {
        return this.blockId;
    }

    public ApplicationId applicationId() {
        return this.appId;
    }

    public ObjectNode root() {
        return this.root;
    }

    public ArrayNode rulesArray() {
        return this.rulesArray;
    }

    @Override
    public Map<String, Integer> datapathTrafficClasses() {
        return this.datapathTrafficClasses;
    }

    public void addDatapathTrafficClass(String trafficClass, Integer outputPort) {
        this.datapathTrafficClasses.put(trafficClass, outputPort);
    }

    public void setSdnTrafficClasses(List<FlowRule> sdnTrafficClasses) {
        this.sdnTrafficClasses = sdnTrafficClasses;
    }

    public void addSdnTrafficClass(FlowRule sdnTrafficClass) {
        this.sdnTrafficClasses.add(sdnTrafficClass);
    }

    @Override
    public void loadConfiguration() throws IOException, IllegalArgumentException {
        try {
            if (this.configFile().exists()) {
                root = (ObjectNode) new ObjectMapper().readTree(this.configFile);
                log.info("\tLoaded rule configuration from {}", this.configFile);
            } else {
                throw new IOException("Invalid file");
            }

        } catch (IOException ioEx) {
            log.warn(
                "Unable to load rule configuration from {}",
                this.configFile
            );
            throw ioEx;
        } catch (Exception ex) {
            log.warn(
                "\tException caught while loading {}",
                this.configFile
            );
            throw ex;
        }

        this.rulesArray = nullIsIllegal(
            (ArrayNode) root.get(ENTRIES), ENTRIES_ARRAY_REQUIRED
        );
    }

    @Override
    public void loadRules() throws IllegalArgumentException {
        if (this.rulesArray == null) {
            log.warn("Configuration to parse is unavailable");
            return;
        }

        // Create a list of FlowRule objects
        List<FlowRule> sdnTrafficClasses =
            codec(FlowRule.class).decode(this.rulesArray, this);

        // Add them to the local repository of SDN rules
        this.setSdnTrafficClasses(sdnTrafficClasses);

        // Add them to the local repository of datapath rules
        for (FlowRule rule : sdnTrafficClasses) {
            for (Instruction instruction : rule.treatment().allInstructions()) {
                long port = -1;
                if (instruction.type() == Type.OUTPUT) {
                    port = ((Instructions.OutputInstruction) instruction).port().toLong();
                    // log.info("\t\t{}", instruction.toString());
                }

                if (port >= 0) {
                    this.addDatapathTrafficClass(
                        rule.toString(),
                        new Integer((int) port)
                    );
                }
            }
        }

        log.info("");
    }

    @Override
    public String datapathTrafficClassesToString() {
        Map<String, Integer> rules = this.datapathTrafficClasses();
        if (rules == null) {
            return "";
        }

        /**
         * The rules in the map might not be in order,
         * but they should appear in order
         * (by increasing ouput port number).
         */
        String[] array = new String[rules.size()];
        for (Map.Entry<String, Integer> rule : rules.entrySet()) {
            // Reflects the rule's priority
            int index = rule.getValue().intValue();
            array[index] = rule.getKey();
        }

        String conf = "";
        for (String s : array) {
            conf += s + ", ";
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
         * (by increasing ouput port number).
         */
        List<Integer> discretePorts = new ArrayList<Integer>();
        for (Map.Entry<String, Integer> rule : rules.entrySet()) {
            if (discretePorts.contains(rule.getValue())) {
                continue;
            }
            discretePorts.add(rule.getValue());
        }

        return discretePorts.size();
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

        log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    @Override
    public Map<String, Integer> nfvTrafficClasses() {
        return null;
    }

    @Override
    public List<FlowRule> sdnTrafficClasses() {
        return this.sdnTrafficClasses;
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

}
