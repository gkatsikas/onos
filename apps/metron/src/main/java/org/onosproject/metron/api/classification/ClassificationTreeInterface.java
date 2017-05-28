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

package org.onosproject.metron.api.classification;

import org.onosproject.metron.api.structures.TreeNode;
import org.onosproject.metron.api.net.ClickFlowRuleAction;

import java.util.Set;
import java.util.List;

public interface ClassificationTreeInterface {

    /**
     * Returns the root of the classification tree.
     *
     * @return the root node
     */
    TreeNode root();

    /**
     * Returns the traffic class of the rule
     * encoded by the classification tree.
     *
     * @return traffic class
     */
    String trafficClass();

    /**
     * Returns the output port of the rule
     * encoded by the classification tree.
     *
     * @return output port
     */
    int outputPort();

    /**
     * Returns the priority of the rule
     * encoded by the classification tree.
     *
     * @return priority
     */
    int priority();

    /**
     * Returns the action of the rule
     * encoded by the classification tree.
     *
     * @return action
     */
    ClickFlowRuleAction action();

    /**
     * Returns the list of datapath traffic classes which are
     * associated with the software traffic class of this classification tree.
     *
     * @return list of traffic classes (string format)
     */
    List<String> datapathTrafficClasses();

    /**
     * Builds the classification tree out of an input pattern.
     */
    void buildTree();

    /**
     * Verify that the patterns in each traffic class comply with the desired syntax.
     */
    void verifyTree();

    /**
     * Traverses the binary classification tree to compute
     * a list of one or more hardware rules (or traffic classes).
     * Branches occur when OR operators are met.
     *
     * @return a list of rules that represent traffic classes
     */
    List<String> toHardwareRules();

    /**
     * Returns the set of textual packet filters associated with the input traffic class.
     *
     * @param trafficClass a traffic class
     * @return set of textual packet filters for this traffic class
     */
    Set<TextualPacketFilter> getPacketFiltersOf(String trafficClass);

}
