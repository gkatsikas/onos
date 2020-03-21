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

import java.util.Map;

/**
 * Metron's pattern configuration interface.
 */
public interface PatternConfigurationInterface extends BasicConfigurationInterface {

    /**
     * Parses the loaded configuration and stores a list of patterns.
     *
     * @throws IllegalArgumentException if a pattern does
     *         not follow the template
     */
    void loadPatterns() throws IllegalArgumentException;

    /**
     * Returns a map of the entire pattern set.
     *
     * @return map of patterns
     */
    Map<String, Integer> datapathPatterns();

    /**
     * Parses the dataplane patterns and translates them into a single string.
     *
     * @return dataplane patterns as a string
     */
    String datapathPatternsToString();

    /**
     * Parses the dataplane patterns and returns the number of discrete output ports.
     *
     * @return number of discrete output ports
     */
    int numberOfDiscreteOutputPorts();

    /**
     * Prints the stored list of patterns.
     */
    void printPatterns();

}
