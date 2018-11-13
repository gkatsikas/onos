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

import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.impl.processing.ClassifierBlock;
import org.onosproject.metron.impl.processing.ProcessingBlock;

import java.util.Map;

/**
 * Classifier block that performs OpenFlow-based traffic classification.
 */
public class OpenFlowClassifier extends ClassifierBlock {

    public OpenFlowClassifier(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.OPENFLOW_CLASSIFIER;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();
    }

    @Override
    public void addConfiguration() {
        Map<String, Integer> rules = this.ruleConf.datapathTrafficClasses();
        if (rules.size() == 0) {
            return;
        }

        String conf = "";
        for (Map.Entry<String, Integer> rule : rules.entrySet()) {
            conf += rule.getKey() + ", ";
        }

        // Remove the trailing ", "
        conf = conf.substring(0, conf.length() - 2);

        this.configurationMap.put("", conf);
    }

    @Override
    public String fullConfiguration() {
        // TODO
        return "";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new OpenFlowClassifier(
            id,
            this.configuration(),
            this.configurationFile()
        );
    }

}
