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

import org.onosproject.metron.api.exceptions.InputConfigurationException;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import org.onosproject.core.ApplicationId;

import java.util.Set;
import java.io.IOException;

/**
 * The interface for adding new service chains.
 */
public interface ServiceChainAddConfigInterface {

    /**
     * Returns the set of service chains read from network config.
     *
     * @param appId the application ID that requires these rules
     * @throws IOException I/O exception
     * @throws InputConfigurationException input configuration exception
     * @return set of service chains
     */
    Set<ServiceChainInterface> loadServiceChains(ApplicationId appId)
        throws IOException, InputConfigurationException;

    /**
     * Returns a block's rule configuration object read from a JSON file.
     *
     * @param blockConfFile the path to the file to be loaded
     * @param blockName the name of the block of this network function
     * @param blockInstance the instance ID of this block
     * @return block's configuration object
     */
    BasicConfigurationInterface readRules(
        String blockConfFile, String blockName, String blockInstance
    );

    /**
     * Returns a block's pattern configuration object read from a JSON file.
     *
     * @param blockConfFile the path to the file to be loaded
     * @param blockInstance the instance ID of this block
     * @return block's configuration object
     */
    BasicConfigurationInterface readPatterns(
        String blockConfFile, String blockInstance
    );

}
