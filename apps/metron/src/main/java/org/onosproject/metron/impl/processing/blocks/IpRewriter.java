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

package org.onosproject.metron.impl.processing.blocks;

import org.onosproject.metron.api.config.PatternConfigurationInterface;
import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.impl.processing.ModifierBlock;
import org.onosproject.metron.impl.processing.ProcessingBlock;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Modifier block that statefuly modifies src/dst IP addresses and ports.
 */
public class IpRewriter extends ModifierBlock {

    /**
     * The number of patterns usually indicates
     * the number of output ports.
     */
    protected int outputPortsNumber;

    /**
     * A rewriter is associated with a configuration.
     * This configuration is a set of rules.
     */
    protected PatternConfigurationInterface patternConf;

    /**
     * If true, marks flows for monitoring purposes.
     */
    protected boolean aggregate;

    public static final String PATTERN   = "PATTERN";
    public static final String AGGREGATE = "SET_AGGREGATE";

    protected static final boolean DEF_AGGREGATE_STATUS = false;

    public    static final String DROP_PATTERN    = "drop";
    public    static final String DISCARD_PATTERN = "discard";

    public IpRewriter(
            String  id,
            String  conf,
            String  confFile) {
        super(id, conf, confFile);

        this.outputPortsNumber = 0;
        this.patternConf = null;
        this.aggregate = DEF_AGGREGATE_STATUS;
    }

    public IpRewriter(
            String  id,
            String  conf,
            String  confFile,
            int     portsNumber,
            PatternConfigurationInterface patternConf) {
        super(id, conf, confFile);

        this.outputPortsNumber = portsNumber;
        this.patternConf = patternConf;
        this.aggregate = DEF_AGGREGATE_STATUS;
    }

    public IpRewriter(
            String  id,
            String  conf,
            String  confFile,
            int     portsNumber,
            PatternConfigurationInterface patternConf,
            boolean aggregate) {
        super(id, conf, confFile);

        this.outputPortsNumber = portsNumber;
        this.patternConf = patternConf;
        this.aggregate = aggregate;
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
     * Returns the pattern configuration object of this element.
     *
     * @return a pattern configuration object
     */
    public PatternConfigurationInterface patternConf() {
        return this.patternConf;
    }

    /**
     * Sets the pattern configuration object of this element.
     *
     * @param patternConf a pattern configuration object
     */
    public void setPatternConf(PatternConfigurationInterface patternConf) {
        checkNotNull(
            patternConf,
            "NULL or empty element configuration."
        );

        this.patternConf = patternConf;
        this.addConfiguration();
    }

    /**
     * Returns the whether aggregate flow monitoring is enabled or not.
     *
     * @return aggregate flow monitoring status
     */
    public boolean aggregate() {
        return this.aggregate;
    }

    /**
     * Sets the aggregate flow monitoring status of this element.
     *
     * @param aggregate aggregate flow monitoring status
     */
    public void setAggregate(boolean aggregate) {
        this.aggregate = aggregate;
    }

    public void addConfiguration() {
        if (this.patternConf == null) {
            log.error("No pattern configuration available for {}", this.id());
            return;
        }

        Map<String, Integer> patterns = this.patternConf.datapathPatterns();
        if ((patterns == null) || (patterns.size() == 0)) {
            log.error("No datapath patterns available for {}", this.id());
            return;
        }

        // Turn the patterns into a single string
        String conf = this.patternConf.datapathPatternsToString();

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
        int ports = this.patternConf.numberOfDiscreteOutputPorts();
        this.setOutputPortsNumber(ports);
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.IP_REWRITER;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap.get(AGGREGATE);
        if (val != null) {
            this.setAggregate(Boolean.valueOf(val.toString()));
            this.configurationMap.remove(AGGREGATE);
        } else {
            this.setAggregate(DEF_AGGREGATE_STATUS);
        }
    }

    @Override
    public String fullConfiguration() {
        return "IPRewriter(" + patternConf().datapathPatternsToString() + ", " +
                    AGGREGATE + " " + (aggregate() ? "true" : "false") +
                ")";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new IpRewriter(
            id,
            "",
            this.configurationFile(),
            this.outputPortsNumber(),
            this.patternConf(),
            this.aggregate()
        );
    }

}
