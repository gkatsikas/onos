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

import org.onosproject.metron.api.config.PatternConfigurationInterface;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.net.ClickFlowRuleAction;

import org.onosproject.core.ApplicationId;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Component responsible for automatically loading a pattern configuration file.
 */
public class PatternConfiguration
        extends BasicConfiguration
        implements PatternConfigurationInterface {

    protected static final String PATTERN = "pattern";

    /**
     * Map each pattern to an output port.
     */
    protected Map<String, Integer> datapathPatterns = new ConcurrentHashMap<String, Integer>();

    public PatternConfiguration(ApplicationId appId, String blockId, String configFileName) {
        super(appId, blockId, configFileName);
    }

    public PatternConfiguration(ApplicationId appId, String blockId, File configFile) {
        super(appId, blockId, configFile);
    }

    @Override
    public Map<String, Integer> datapathPatterns() {
        return this.datapathPatterns;
    }

    private void addDatapathPattern(String pattern, Integer outputPort) {
        this.datapathPatterns.put(pattern, outputPort);
    }

    @Override
    public void loadPatterns() throws IllegalArgumentException {
        if (this.entriesArray == null) {
            log.warn("Configuration to parse is unavailable");
            return;
        }

        short i = 0;
        for (JsonNode pat : this.entriesArray) {
            if ((i > 0) && (i < this.entriesArray.size())) {
                log.debug("");
            }

            String pattern = pat.get(PATTERN).asText().toLowerCase();
            log.debug("Pattern: {}", pattern);

            if (!pattern.contains("pattern")) {
                pattern = "pattern " + pattern;
            }

            int outputPort = pat.get(OUTPUT_PORT).asInt();
            checkArgument(
                outputPort >= 0,
                "Pattern output port must be greater or equal than zero"
            );
            log.debug("\tOutput Port: {}", outputPort);

            int priority = pat.get(PRIORITY).asInt();
            checkArgument(
                priority > 0,
                "Pattern priority must be greater than zero"
            );
            log.debug("\t   Priority: {}", priority);

            String actionStr = pat.get(ACTION).asText().toUpperCase();
            ClickFlowRuleAction action = Common.<ClickFlowRuleAction>enumFromString(
                ClickFlowRuleAction.class, actionStr
            );
            checkNotNull(
                action,
                "Incompatible Click flow rule action. Choose one in: " + Common.<ClickFlowRuleAction>
                enumTypesToString(
                    ClickFlowRuleAction.class
                )
            );
            log.debug("\t     Action: {}", action);

            // Insert this class into our memory
            this.addDatapathPattern(pattern, outputPort);

            i++;
        }

        // The entire pattern set
        this.printPatterns();
    }

    @Override
    public String datapathPatternsToString() {
        Map<String, Integer> patterns = this.datapathPatterns();
        if (patterns == null) {
            return "";
        }

        /**
         * The patterns in the map might not be in order,
         * but they should appear in order
         * (by increasing ouput port number).
         */
        String[] array = new String[patterns.size()];
        for (Map.Entry<String, Integer> pattern : patterns.entrySet()) {
            // Reflects the pattern's priority
            int index = pattern.getValue().intValue();
            array[index] = pattern.getKey();
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
        Map<String, Integer> patterns = this.datapathPatterns();
        if (patterns == null) {
            return 0;
        }

        /**
         * The patterns in the map might not be in order,
         * but they should appear in order
         * (by increasing output port number).
         */
        List<Integer> discretePorts = new ArrayList<Integer>();
        for (Map.Entry<String, Integer> pattern : patterns.entrySet()) {
            if (discretePorts.contains(pattern.getValue())) {
                continue;
            }
            discretePorts.add(pattern.getValue());
        }

        return discretePorts.size();
    }

    @Override
    public void printPatterns() {
        this.datapathPatterns().forEach((k, v) -> log.debug(
            "Datapath pattern: " + k + " --> Port: " + v
        ));
        log.info("");
    }

}
