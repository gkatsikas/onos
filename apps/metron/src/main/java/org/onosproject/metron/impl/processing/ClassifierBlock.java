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

package org.onosproject.metron.impl.processing;

import org.onosproject.metron.api.processing.ProcessingBlockType;
import org.onosproject.metron.api.config.RuleConfigurationInterface;

import org.onosproject.net.flow.FlowRule;

import java.util.Map;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A generic classifier block.
 */
public abstract class ClassifierBlock extends ProcessingBlock {

    /**
     * The number of rules usually indicates
     * the number of output ports.
     */
    protected int outputPortsNumber;

    /**
     * A classifier is associated with a configuration.
     * This configuration is a set of rules.
     */
    protected RuleConfigurationInterface ruleConf;

    /**
     * We mirror the hardware and software rules of the configuration into
     * the element, to allow greater flexibility and expose them properly.
     */
    protected List<FlowRule>       hardwareRules;
    protected Map<String, Integer> softwareRules;

    protected ClassifierBlock(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.type = ProcessingBlockType.BLOCK_TYPE_CLASSIFIER;
        this.outputPortsNumber = 0;
        this.ruleConf = null;
        this.hardwareRules = null;
        this.softwareRules = null;
    }

    /**
     * Returns the number of output ports of this element.
     *
     * @return number of output ports of this element
     */
    public int outputPortsNumber() {
        return this.outputPortsNumber;
    }

    /**
     * Sets the number of output ports of this element.
     *
     * @param ports number of output ports of this element
     */
    public void setOutputPortsNumber(int ports) {
        checkArgument(
            ports > 0,
            "Invalid number of output ports for processing block " + this.id()
        );

        this.outputPortsNumber = ports;
    }

    /**
     * Returns the rule configuration object of this element.
     *
     * @return a rule configuration object
     */
    public RuleConfigurationInterface ruleConf() {
        return this.ruleConf;
    }

    /**
     * Sets the rule configuration object of this element.
     *
     * @param ruleConf a rule configuration object
     */
    public void setRuleConf(RuleConfigurationInterface ruleConf) {
        checkNotNull(
            ruleConf,
            "NULL or empty element configuration."
        );

        this.ruleConf = ruleConf;
        this.addConfiguration();
    }

    /**
     * Returns the hardware rules of this element.
     *
     * @return a list of hardware rules
     */
    public List<? extends FlowRule> hardwareRules() {
        this.populateConfiguration();

        return this.hardwareRules;
    }

    /**
     * Returns the software rules of this element.
     *
     * @return a list of software rules
     */
    public Map<String, Integer> softwareRules() {
        this.populateConfiguration();

        return this.softwareRules;
    }

    /**
     * Returns the number of hardware rules of this element.
     *
     * @return number of hardware rules
     */
    public int numberOfHardwareRules() {
        this.populateConfiguration();

        if (this.hardwareRules == null) {
            return 0;
        }
        return this.hardwareRules.size();
    }

    /**
     * Returns the number of software rules of this element.
     *
     * @return number of software rules
     */
    public int numberOfSoftwareRules() {
        this.populateConfiguration();

        if (this.softwareRules == null) {
            return 0;
        }
        return this.softwareRules.size();
    }

    public void addConfiguration() {
        if (this.ruleConf == null) {
            log.error("No rule configuration available for {}", this.id());
            return;
        }

        Map<String, Integer> rules = this.ruleConf.datapathTrafficClasses();
        if (rules == null) {
            log.error("No datapath rules available for {}", this.id());
            return;
        }
        if (rules.size() == 0) {
            log.error("No datapath rules available for {}", this.id());
            return;
        }

        // Turn the rules into a single string
        String conf = this.ruleConf.datapathTrafficClassesToString();

        // Update the block's configuration
        this.setConfiguration(conf);
        this.configurationMap.put("", conf);

        /**
         * Trigger this method to find how many
         * output ports this element has
         */
        this.findNumberOfOutputPorts();
    }

    private void findNumberOfOutputPorts() {
        int ports = this.ruleConf.numberOfDiscreteOutputPorts();
        this.setOutputPortsNumber(ports);
    }

    /**
     * Passes the rule configuration to this
     * element's local data structures.
     */
    @Override
    public void populateConfiguration() {
        if ((this.hardwareRules == null) && (this.ruleConf != null)) {
            this.hardwareRules = this.ruleConf().sdnTrafficClasses();
        }

        if ((this.softwareRules == null) && (this.ruleConf != null)) {
            this.softwareRules = this.ruleConf().nfvTrafficClasses();
        }
    }

}
