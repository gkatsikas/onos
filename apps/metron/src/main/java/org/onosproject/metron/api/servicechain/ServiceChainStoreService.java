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
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;

import org.onosproject.store.Store;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Service for storing service chains in ONOS.
 */
public interface ServiceChainStoreService
        extends Store<ServiceChainEvent, ServiceChainDelegate> {

    /******************************* Services for service chains. *****************************/
    /**
     * Create a service chain with specific ID.
     *
     * @param scId service chain ID
     * @param sc service chain
     * @return boolean status (success/failure)
     */
    boolean createServiceChain(ServiceChainId scId, ServiceChainInterface sc);

    /**
     * Update an existing service chain with specific ID.
     *
     * @param scId service chain ID
     * @param sc service chain
     * @return boolean status (success/failure)
     */
    boolean updateServiceChain(ServiceChainId scId, ServiceChainInterface sc);

    /**
     * Removes all network functions from a service chain with specific ID
     * and deletes this service chain.
     *
     * @param scId service chain ID
     * @return boolean status (success/failure)
     */
    boolean removeServiceChain(ServiceChainId scId);

    /**
     * Removes all the registered service chains.
     *
     * @return boolean status (success/failure)
     */
    boolean removeAllServiceChains();

    /**
     * Returns the service chain associated with the service chain ID.
     *
     * @param scId service chain ID
     * @return a service chain
     */
    ServiceChainInterface serviceChain(ServiceChainId scId);

    /**
     * Returns a set with the registered service chains.
     *
     * @return a set with registered service chains
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
     * Returns a set with all the network functions of this service chain.
     *
     * @param scId service chain ID
     * @return a set of network functions associated with this service chain
     */
    Set<NetworkFunctionInterface> serviceChainNFs(ServiceChainId scId);

    /**
     * Updates the network functions of this service chain.
     *
     * @param scId service chain ID
     * @param nfs a set of network functions to replace the existing ones
     */
    void updateServiceChainNFs(
        ServiceChainId scId, Set<NetworkFunctionInterface> nfs
    );

    /**
     * Replace the service chain graph of this service chain.
     *
     * @param scId service chain ID
     * @param scGraph a service chain graph to replace the existing one
     */
    void replaceServiceChainGraph(
        ServiceChainId scId, ServiceChainGraphInterface scGraph
    );

    /**
     * Prints a service chain searching by its ID.
     *
     * @param scId service chain ID
     */
    void printServiceChainById(ServiceChainId scId);

    /**
     * Prints all the service chains in the given state.
     *
     * @param state the state of a service chain
     */
    void printServiceChainsByState(ServiceChainState state);

    /**
     * Prints all the registered service chains.
     */
    void printRegisteredServiceChains();

    /************************** Services for RUNNABLE service chains. *************************/

    /**
     * Add a dataplane service chain with specific ID.
     *
     * @param scId service chain ID
     * @param iface entry point interface name for this runnable service chain
     * @param tree the processing tree of this interface of the service chain
     * @return boolean status (success/failure)
     */
    boolean addRunnableServiceChain(
        ServiceChainId scId, String iface, NfvDataplaneTreeInterface tree
    );

    /**
     * Update a dataplane service chain with specific ID.
     *
     * @param scId service chain ID
     * @param iface entry point interface name for this runnable service chain
     * @param tree the processing tree of this service chain
     * @return boolean update status (success/failure)
     */
    boolean updateRunnableServiceChain(
        ServiceChainId scId, String iface, NfvDataplaneTreeInterface tree
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
     * @param scId service chain ID
     * @param iface entry point interface name for this runnable service chain
     * @return the processing tree of the runnable service chain on that interface
     */
    NfvDataplaneTreeInterface runnableServiceChainOfIface(ServiceChainId scId, String iface);

    /**
     * Returns the runnable service chain associated with a service chain ID
     * and a traffic class ID.
     *
     * @param scId service chain ID
     * @param tcId traffic class ID
     * @return the processing tree of the runnable service chain on that traffic class
     */
    NfvDataplaneTreeInterface runnableServiceChainWithTrafficClass(ServiceChainId scId, URI tcId);

    /**
     * Returns the runnable service chains associated with a service chain ID.
     *
     * @param scId service chain ID
     * @return map of processing trees of the runnable service chain
     */
    Map<String, NfvDataplaneTreeInterface> runnableServiceChains(ServiceChainId scId);

    /**
     * Prints a runnable service chain searching by its ID.
     *
     * @param scId service chain ID
     */
    void printRunnableServiceChainById(ServiceChainId scId);

    /***************************** Services for Runtime Information. **************************/

    /**
     * Adds a set of runtime information entries for service chain with specific ID.
     *
     * @param scId service chain ID
     * @param scInfo the set of runtime information of this service chain
     * @return boolean status (success/failure)
     */
    boolean addRuntimeInformationToServiceChain(
        ServiceChainId scId, Set<TrafficClassRuntimeInfo> scInfo
    );

    /**
     * Adds a runtime information entry for service chain with specific ID.
     *
     * @param scId service chain ID
     * @param tcInfo a runtime information entry of this service chain
     * @return boolean status (success/failure)
     */
    boolean addRuntimeInformationForTrafficClassOfServiceChain(
        ServiceChainId scId, TrafficClassRuntimeInfo tcInfo
    );

    /**
     * Update a runtime information entry for service chain with specific ID.
     *
     * @param scId service chain ID
     * @param tcInfo a runtime information entry of this service chain
     * @return boolean status (success/failure)
     */
    boolean updateRuntimeInformationForTrafficClassOfServiceChain(
        ServiceChainId scId, TrafficClassRuntimeInfo tcInfo
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
     * @param scId service chain ID
     * @return set of TrafficClassRuntimeInfo the retrieved runtime information
     */
    Set<TrafficClassRuntimeInfo> runtimeInformationForServiceChain(ServiceChainId scId);

    /**
     * Get a specific runtime information entry for service chain with specific ID.
     *
     * @param scId service chain ID
     * @param trafficClassId the ID of this service chain's traffic class
     * @return TrafficClassRuntimeInfo the retrieved runtime information
     */
    TrafficClassRuntimeInfo runtimeInformationForTrafficClassOfServiceChain(
        ServiceChainId scId,
        URI trafficClassId
    );

    /**
     * Prints the runtime information of a service chain searching by its ID.
     *
     * @param scId service chain ID
     */
    void printRuntimeInformationForServiceChain(ServiceChainId scId);

}
