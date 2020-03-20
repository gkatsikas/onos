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

package org.onosproject.metron.api.application;

import org.onosproject.metron.api.exceptions.InputConfigurationException;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import java.io.IOException;
import java.util.Set;

/**
 * Services provided by the Metron Application Manager.
 */
public interface NfvApplicationService {

    /**
     * Registers a Metron service chain with the core.
     *
     * @param sc the service chain to be registered
     * @return boolean registration status
     */
    boolean register(ServiceChainInterface sc);

    /**
     * Unregisters a Metron service chain from the core.
     *
     * @param sc the service chain to be unregistered
     * @return boolean unregistration status
     */
    boolean unregister(ServiceChainInterface sc);

    /**
     * Deploy a Metron service chain.
     *
     * @param sc the service chain to be deployed
     * @return boolean deployment status
     */
    boolean deploy(ServiceChainInterface sc);

    /**
     * Tear a Metron service chain down.
     *
     * @param sc the service chain to be retracted
     * @return boolean retraction status
     */
    boolean retract(ServiceChainInterface sc);

    /**
     * Reads external Metron service chain configuration
     * related to the deployment of service chains.
     *
     * @throws IOException I/O exception
     * @throws InputConfigurationException input configuration exception
     * @return a set of Metron service chains to deploy
     */
    Set<ServiceChainInterface> readAddConfiguration()
        throws IOException, InputConfigurationException;

    /**
     * Reads external Metron service chain configuration
     * related to the undeployment of service chains and
     * initiates undeployment.
     *
     * @throws IOException I/O exception
     * @throws InputConfigurationException input configuration exception
     * @return boolean status
     */
    boolean readRemoveConfiguration()
        throws IOException, InputConfigurationException;

    /**
     * Reads external Metron service chain configuration
     * related to the undeployment of all service chains
     * and initiates undeployment.
     *
     * @throws IOException I/O exception
     * @throws InputConfigurationException input configuration exception
     * @return boolean status
     */
    boolean readRemoveAllConfiguration()
        throws IOException, InputConfigurationException;

    /**
     * Load external Metron service chain configuration into
     * the service chain store.
     *
     * @param chainsToLoad a set of service chain to load
     * @return boolean status
     */
    boolean loadConfiguration(Set<ServiceChainInterface> chainsToLoad);

}
