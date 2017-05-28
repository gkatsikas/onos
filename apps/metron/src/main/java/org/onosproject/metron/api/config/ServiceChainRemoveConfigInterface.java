/*
 * Copyright 2018-present Open Networking Foundation
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
import org.onosproject.metron.api.servicechain.ServiceChainId;

import org.onosproject.core.ApplicationId;

import java.io.IOException;
import java.util.Set;

/**
 * The interface for removing deployed service chains.
 */
public interface ServiceChainRemoveConfigInterface {

    /**
     * Tears a list of deployed service chains down.
     *
     * @param appId the application ID that issues this command
     * @throws IOException I/O exception
     * @throws InputConfigurationException input configuration exception
     * @return a set of service chain IDs to be undeployed or an empty set
     */
    Set<ServiceChainId> undeployServiceChains(ApplicationId appId)
        throws IOException, InputConfigurationException;

}
