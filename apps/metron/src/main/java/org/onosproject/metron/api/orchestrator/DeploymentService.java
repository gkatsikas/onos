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

package org.onosproject.metron.api.orchestrator;

import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.FlowRule;

import java.util.Set;
import java.util.Iterator;

/**
 * Services provided by Metron's service chain deployer.
 */
public interface DeploymentService {

    /**
     * Returns the application ID of the Metron Deployer.
     *
     * @return application ID
     */
    ApplicationId applicationId();

    /**
     * Adds a service chain to the set of "ready to deploy" service chains.
     *
     * @param sc a "deployable" service chain
     */
    void sendServiceChainToDeployer(ServiceChainInterface sc);

    /**
     * Returns the set of "ready to deploy" service chains.
     * This set is actually the set of placed service chains above.
     *
     * @return set of deployable service chains
     */
    Set<ServiceChainInterface> readyToDeployServiceChains();

    /**
     * Iterate through the set of "ready to deploy" service chains
     * and deploy them one-by-one.
     *
     * @throws DeploymentException if deployment fails
     */
    void deploy() throws DeploymentException;

    /**
     * When the deployer deploys a service chain, it marks its state accordingly.
     * Moreover, this service chain needs to be moved from the set of 'ready to deploy'
     * service chains to the set of 'deployed' ones.
     *
     * @param sc a deployed service chain
     * @param scIterator the iterator of this memory to be used for safe removal
     */
    void markServiceChainAsDeployed(
        ServiceChainInterface sc,
        Iterator<ServiceChainInterface> scIterator
    );

    /**
     * Returns the set of deployed (i.e., active) service chains.
     *
     * @return set of deployed service chains
     */
    Set<ServiceChainInterface> deployedServiceChains();

    /**
     * Undeploy a deployed service chain and update the corresponding memories.
     *
     * @param sc the service chain to undeploy
     * @param scIterator the iterator of this memory to be used for safe removal
     */
    void markServiceChainAsUndeployed(
        ServiceChainInterface sc,
        Iterator<ServiceChainInterface> scIterator
    );

    /**
     * Push a set of flow rules to the target devices.
     *
     * @param scId the ID of the service chain that requires these rules
     * @param rules set of rules to be installed
     * @param update if true, indicates update of existing rules
     * @return boolean status of this operation
     */
    boolean installRules(ServiceChainId scId, Set<FlowRule> rules, boolean update);

    /**
     * Update a set of flow rules on a target device.
     *
     * @param scId the ID of the service chain that requires these rules
     * @param rules set of rules to be updated
     * @return boolean status of this operation
     */
    boolean updateRules(ServiceChainId scId, Set<FlowRule> rules);

    /**
     * Remove a set of rules from their devices.
     *
     * @param scId the ID of the service chain that implements these rules
     * @param rules set of rules to be removed
     * @return boolean status of this operation
     */
    boolean removeRules(ServiceChainId scId, Set<FlowRule> rules);

    /**
     * Returns whether the autoscale feature is active or not.
     *
     * @return boolean autoscale status
     */
    boolean hasAutoScale();

    /**
     * Returns whether the HW offloading feature is active or not.
     *
     * @return boolean HW offloading status
     */
    boolean hwOffloadingEnabled();

}
