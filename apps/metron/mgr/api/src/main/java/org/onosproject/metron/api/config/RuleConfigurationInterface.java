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

package org.onosproject.metron.api.config;

import org.onosproject.net.flow.FlowRule;

import java.util.Map;
import java.util.List;

/**
 * Metron's rule configuration interface.
 */
public interface RuleConfigurationInterface extends BasicConfigurationInterface {

    /**
     * Parses the loaded configuration and stores a list of rules.
     *
     * @throws IllegalArgumentException if a rule does
     *         not follow the FlowRule template
     */
    void loadRules() throws IllegalArgumentException;

    /**
     * Returns a map of the entire ruleset (no matter if HW or SW).
     *
     * @return map of traffic classes
     */
    Map<String, Integer> datapathTrafficClasses();

    /**
     * Parses the dataplane rules and translates them into a single string.
     *
     * @return dataplane rules as a string
     */
    String datapathTrafficClassesToString();

    /**
     * Parses the dataplane rules and returns the number of discrete output ports.
     *
     * @return number of discrete output ports
     */
    int numberOfDiscreteOutputPorts();

    /**
     * Returns a list of hardware-compliant SDN rules.
     * This list is a subset of the datapath traffic classes.
     *
     * @return list of FlowRule objects
     */
    List<FlowRule> sdnTrafficClasses();

    /**
     * Returns a map of software-compliant rules to output ports.
     * This is a subset of the datapath traffic classes.
     *
     * @return map between software rules and output ports
     */
    Map<String, Integer> nfvTrafficClasses();

    /**
     * Prints the stored list of rules.
     */
    void printRules();

}
