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

package org.onosproject.metron.api.servicechain;

import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.exceptions.ServiceChainException;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;

import org.onosproject.event.ListenerService;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Services provided by Metron's Service Chain Manager.
 */
public interface ServiceChainService
        extends ListenerService
            <ServiceChainEvent, ServiceChainListenerInterface> {

    /******************************* Services for service chains. *****************************/
    /**
     * Registers a service chain to the service.
     *
     * @param sc service chain
     * @throws ServiceChainException service chain exception
     */
    void registerServiceChain(ServiceChainInterface sc) throws ServiceChainException;

    /**
     * The service chain store will search for and then update
     * a service chain with the ID of the inpur service chain.
     *
     * @param sc the service chain to be updated
     * @throws ServiceChainException service chain exception
     */
    public void updateServiceChain(ServiceChainInterface sc)
            throws ServiceChainException;

    /**
     * Updates the registry of a service chain with a new one.
     *
     * @param scCur the current service chain
     * @param scNew the new service chain
     * @throws ServiceChainException service chain exception
     */
    void updateServiceChainWithAnother(
        ServiceChainInterface scCur, ServiceChainInterface scNew)
        throws ServiceChainException;

    /**
     * Deletes a service chain from the service.
     *
     * @param sc service chain
     * @throws ServiceChainException service chain exception
     */
    void unregisterServiceChain(ServiceChainInterface sc) throws ServiceChainException;

    /**
     * Bootstraps service chain with INIT state.
     *
     * @param sc service chain
     */
    void processInitState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with CONSTRUCTED state.
     *
     * @param sc service chain
     */
    void processConstructedState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with SYNTHESIZED state.
     *
     * @param sc service chain
     */
    void processSynthesizedState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with READY state.
     *
     * @param sc service chain
     */
    void processReadyState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with PLACED state.
     *
     * @param sc service chain
     */
    void processPlacedState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with DEPLOYED state.
     *
     * @param sc service chain
     */
    void processDeployedState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with SUSPENDED state.
     *
     * @param sc service chain
     */
    void processSuspendedState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with TRANSIENT state.
     *
     * @param sc service chain
     */
    void processTransientState(ServiceChainInterface sc);

    /**
     * Bootstraps service chain with DESTROYED state.
     *
     * @param sc service chain
     */
    void processDestroyedState(ServiceChainInterface sc);

    /**
     * Lookup a service chain by using its identifier as a key.
     *
     * @param chainId service chain ID
     * @return a service chain or null
     */
    ServiceChainInterface serviceChain(ServiceChainId chainId);

    /**
     * Updates the NFs of a service chain.
     *
     * @param chainId service chain ID
     * @param nfs the set of NFs
     */
    void updateServiceChainNFs(
        ServiceChainId chainId, Set<NetworkFunctionInterface> nfs
    );

    /**
     * Returns all service chains known to the service.
     *
     * @return a set with the registered service chains
     */
    Set<ServiceChainInterface> registeredServiceChains();

    /**
     * Returns all service chains in the given state.
     *
     * @param state the state of a service chain
     * @return a set with the service chains in the given state
     */
    Set<ServiceChainInterface> serviceChainsInState(ServiceChainState state);

    /**
     * Returns all the network functions of a service chain.
     *
     * @param chainId service chain ID
     * @return a set of network functions
     */
    Set<NetworkFunctionInterface> serviceChainNFs(ServiceChainId chainId);

    /**
     * Prints all the registered service chains.
     */
    void printRegisteredServiceChains();

    /**
     * Prints all the service chains in the given state.
     *
     * @param state the state of a service chain
     */
    void printServiceChainsByState(ServiceChainState state);

    /************************** Services for RUNNABLE service chains. *************************/
    /**
     * Add a dataplane service chain with specific ID.
     *
     * @param chainId service chain ID
     * @param iface entry point interface name for this runnable service chain
     * @param tree the processing tree of this interface of the service chain
     * @return boolean status (success/failure)
     */
    boolean addRunnableServiceChain(
        ServiceChainId chainId, String iface, NfvDataplaneTreeInterface tree
    );

    /**
     * Update a dataplane service chain with specific ID.
     *
     * @param chainId service chain ID
     * @param iface entry point interface name for this runnable service chain
     * @param tree the processing tree of this service chain
     * @return boolean update status (success/failure)
     */
    boolean updateRunnableServiceChain(
        ServiceChainId chainId, String iface, NfvDataplaneTreeInterface tree
    );

    /**
     * Delete a dataplane service chain with specific ID.
     *
     * @param scId service chain ID
     * @return boolean deletion status (success/failure)
     */
    boolean deleteRunnableServiceChain(
        ServiceChainId scId
    );

    /**
     * Returns the runnable service chain associated with a service chain ID
     * and a particular network interface.
     *
     * @param chainId service chain ID
     * @param iface entry point interface name for this runnable service chain
     * @return the processing tree of the runnable service chain on that interface
     */
    NfvDataplaneTreeInterface runnableServiceChainOfIface(ServiceChainId chainId, String iface);

    /**
     * Returns the runnable service chain associated with a service chain ID
     * and a traffic class ID.
     *
     * @param chainId service chain ID
     * @param tcId traffic class ID
     * @return the processing tree of the runnable service chain on that traffic class
     */
    NfvDataplaneTreeInterface runnableServiceChainWithTrafficClass(ServiceChainId chainId, URI tcId);

    /**
     * Returns the runnable service chains associated with a service chain ID.
     *
     * @param chainId service chain ID
     * @return map of processing trees of the runnable service chain
     */
    Map<String, NfvDataplaneTreeInterface> runnableServiceChains(ServiceChainId chainId);

    /**
     * Prints a runnable service chain searching by its ID.
     *
     * @param chainId service chain ID
     */
    void printRunnableServiceChainById(ServiceChainId chainId);

    /***************************** Services for Runtime Information. **************************/
    /**
     * Adds a set of runtime information entries for service chain with specific ID.
     *
     * @param chainId service chain ID
     * @param scInfo the set of runtime information of this service chain
     * @return boolean status (success/failure)
     */
    boolean addRuntimeInformationToServiceChain(
        ServiceChainId chainId, Set<TrafficClassRuntimeInfo> scInfo
    );

    /**
     * Adds a runtime information entry for service chain with specific ID.
     *
     * @param chainId service chain ID
     * @param tcInfo a runtime information entry of this service chain
     * @return boolean status (success/failure)
     */
    boolean addRuntimeInformationForTrafficClassOfServiceChain(
        ServiceChainId chainId, TrafficClassRuntimeInfo tcInfo
    );

    /**
     * Update a runtime information entry for service chain with specific ID.
     *
     * @param chainId service chain ID
     * @param tcInfo a runtime information entry of this service chain
     * @return boolean status (success/failure)
     */
    boolean updateRuntimeInformationForTrafficClassOfServiceChain(
        ServiceChainId chainId, TrafficClassRuntimeInfo tcInfo
    );

    /**
     * Delete a set of runtime information entries for service chain with specific ID.
     *
     * @param scId service chain ID
     * @return boolean status (success/failure)
     */
    boolean deleteRuntimeInformationForServiceChain(
        ServiceChainId scId
    );

    /**
     * Delete a runtime information entry for service chain with specific ID.
     *
     * @param scId service chain ID
     * @param tcInfo a runtime information entry of this service chain
     * @return boolean status (success/failure)
     */
    boolean deleteRuntimeInformationForTrafficClassOfServiceChain(
        ServiceChainId scId, TrafficClassRuntimeInfo tcInfo
    );

    /**
     * Get the runtime information entries for service chain with specific ID.
     *
     * @param chainId service chain ID
     * @return set of TrafficClassRuntimeInfo the retrieved runtime information
     */
    Set<TrafficClassRuntimeInfo> runtimeInformationForServiceChain(ServiceChainId chainId);

    /**
     * Get a specific runtime information entry for service chain with specific ID.
     *
     * @param chainId service chain ID
     * @param trafficClassId the ID of this service chain's traffic class
     * @return TrafficClassRuntimeInfo the retrieved runtime information
     */
    TrafficClassRuntimeInfo runtimeInformationForTrafficClassOfServiceChain(
        ServiceChainId chainId,
        URI trafficClassId
    );

    /**
     * Prints the runtime information of a service chain searching by its ID.
     *
     * @param chainId service chain ID
     */
    void printRuntimeInformationForServiceChain(ServiceChainId chainId);

}
