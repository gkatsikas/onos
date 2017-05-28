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

package org.onosproject.metron.impl.servicechain;

// Metron libraries
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.exceptions.ServiceChainException;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionState;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainProvider;
import org.onosproject.metron.api.servicechain.ServiceChainDelegate;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainStoreService;
import org.onosproject.metron.api.servicechain.ServiceChainProviderService;
import org.onosproject.metron.api.servicechain.ServiceChainService;
import org.onosproject.metron.api.servicechain.ServiceChainListenerInterface;

// ONOS libraries
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;

// Apache libraries
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Other libraries
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Java libraries
import java.net.URI;
import java.util.Set;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service that manages Metron service chains.
 */
@Component(immediate = true)
@Service
public final class ServiceChainManager
        extends AbstractListenerProviderRegistry<
            ServiceChainEvent, ServiceChainListenerInterface,
            ServiceChainProvider, ServiceChainProviderService
        >
        implements ServiceChainService {

    /**
     * Members of a Service Chain Manager.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".servicechain.manager";
    private static final String MANAGER_LABEL = "Service Chain Manager";

    private ApplicationId appId;

    // Undertakes the delegation of service chain events
    private final ServiceChainDelegate delegate = new InternalStoreDelegate();

    /**
     * Services used by the service chain manager.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ServiceChainStoreService serviceChainStore;

    /**
     * Service chain manager's lifecycle.
     */
    @Activate
    protected void activate() {
        this.appId = coreService.registerApplication(APP_NAME);

        // Set the delegate of the store
        serviceChainStore.setDelegate(delegate);
        // .. and ensure that there is a sink for the generated events
        this.eventDispatcher.addSink(ServiceChainEvent.class, listenerRegistry);

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        serviceChainStore.unsetDelegate(delegate);

        log.info("[{}] Stopped", label());
    }

    /******************************* Services for service chains. *****************************/

    @Override
    public void registerServiceChain(ServiceChainInterface sc)
            throws ServiceChainException {
        // Initialize the state of this service chain
        this.processInitState(sc);

        // Store the new service chain
        boolean status = serviceChainStore.createServiceChain(sc.id(), sc);

        if (!status) {
            throw new ServiceChainException(
                "[" + label() + "] Cannot create service chain with ID " + sc.id());
        }
    }

    @Override
    public void updateServiceChain(ServiceChainInterface sc)
            throws ServiceChainException {
        boolean status = serviceChainStore.updateServiceChain(sc.id(), sc);

        if (!status) {
            throw new ServiceChainException(
                "[" + label() + "] Cannot update service chain with ID " + sc.id());
        }
    }

    @Override
    public void updateServiceChainWithAnother(
            ServiceChainInterface scCur, ServiceChainInterface scNew)
            throws ServiceChainException {
        // First update the state of the current service chain
        this.setServiceChainState(scCur, scNew.state());

        // Then, update the entire service chain
        scCur = ServiceChain.updateServiceChain(scCur, scNew);

        // Store the updated service chain
        this.updateServiceChain(scCur);
    }

    @Override
    public void unregisterServiceChain(ServiceChainInterface sc)
            throws ServiceChainException {
        // Update the state to DESTROYED
        this.processDestroyedState(sc);

        boolean status = serviceChainStore.removeServiceChain(sc.id());

        if (!status) {
            throw new ServiceChainException(
                "[" + label() + "] Cannot remove service chain with ID " + sc.id());
        }
    }

    @Override
    public ServiceChainInterface serviceChain(ServiceChainId scId) {
        return serviceChainStore.serviceChain(scId);
    }

    @Override
    public Set<ServiceChainInterface> registeredServiceChains() {
        return serviceChainStore.registeredServiceChains();
    }

    @Override
    public Set<ServiceChainInterface> serviceChainsInState(ServiceChainState state) {
        return serviceChainStore.serviceChainsInState(state);
    }

    @Override
    public Set<NetworkFunctionInterface> serviceChainNFs(ServiceChainId scId) {
        return serviceChainStore.serviceChainNFs(scId);
    }

    @Override
    public void updateServiceChainNFs(ServiceChainId scId, Set<NetworkFunctionInterface> nfs) {
        serviceChainStore.updateServiceChainNFs(scId, nfs);
    }

    @Override
    public void processInitState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.INIT);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processConstructedState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.CONSTRUCTED);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processSynthesizedState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.SYNTHESIZED);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processReadyState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.READY);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processPlacedState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.PLACED);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processDeployedState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.DEPLOYED);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processSuspendedState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.SUSPENDED);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processTransientState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.TRANSIENT);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void processDestroyedState(ServiceChainInterface sc) {
        this.setServiceChainState(sc, ServiceChainState.DESTROYED);
        log.info("[{}] Service chain {} is in state: {}", label(), sc.name(), sc.state());
    }

    @Override
    public void printRegisteredServiceChains() {
        serviceChainStore.printRegisteredServiceChains();
    }

    @Override
    public void printServiceChainsByState(ServiceChainState state) {
        serviceChainStore.printServiceChainsByState(state);
    }

    /************************** Services for RUNNABLE service chains. *************************/

    @Override
    public boolean addRunnableServiceChain(
            ServiceChainId scId, String iface, NfvDataplaneTreeInterface tree) {
        return serviceChainStore.addRunnableServiceChain(scId, iface, tree);
    }

    @Override
    public boolean updateRunnableServiceChain(
            ServiceChainId scId, String iface, NfvDataplaneTreeInterface tree) {
        return serviceChainStore.updateRunnableServiceChain(scId, iface, tree);
    }

    @Override
    public boolean deleteRunnableServiceChain(ServiceChainId scId) {
        return serviceChainStore.deleteRunnableServiceChain(scId);
    }

    @Override
    public NfvDataplaneTreeInterface runnableServiceChainOfIface(ServiceChainId scId, String iface) {
        return serviceChainStore.runnableServiceChainOfIface(scId, iface);
    }

    @Override
    public NfvDataplaneTreeInterface runnableServiceChainWithTrafficClass(ServiceChainId scId, URI tcId) {
        return serviceChainStore.runnableServiceChainWithTrafficClass(scId, tcId);
    }

    @Override
    public Map<String, NfvDataplaneTreeInterface> runnableServiceChains(ServiceChainId scId) {
        return serviceChainStore.runnableServiceChains(scId);
    }

    @Override
    public void printRunnableServiceChainById(ServiceChainId scId) {
        serviceChainStore.printRunnableServiceChainById(scId);
    }

    /***************************** Services for Runtime Information. **************************/

    @Override
    public boolean addRuntimeInformationToServiceChain(
            ServiceChainId scId, Set<TrafficClassRuntimeInfo> scInfo) {
        return serviceChainStore.addRuntimeInformationToServiceChain(
            scId, scInfo);
    }

    @Override
    public boolean addRuntimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, TrafficClassRuntimeInfo tcInfo) {
        return serviceChainStore.addRuntimeInformationForTrafficClassOfServiceChain(
            scId, tcInfo);
    }

    @Override
    public boolean updateRuntimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, TrafficClassRuntimeInfo tcInfo) {
        return serviceChainStore.updateRuntimeInformationForTrafficClassOfServiceChain(
            scId, tcInfo);
    }

    @Override
    public boolean deleteRuntimeInformationForServiceChain(ServiceChainId scId) {
        return serviceChainStore.deleteRuntimeInformationForServiceChain(scId);
    }

    @Override
    public boolean deleteRuntimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, TrafficClassRuntimeInfo tcInfo) {
        return serviceChainStore.deleteRuntimeInformationForTrafficClassOfServiceChain(
            scId, tcInfo);
    }

    @Override
    public Set<TrafficClassRuntimeInfo> runtimeInformationForServiceChain(
            ServiceChainId scId) {
        return serviceChainStore.runtimeInformationForServiceChain(scId);
    }

    @Override
    public TrafficClassRuntimeInfo runtimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, URI tcId) {
        return serviceChainStore.runtimeInformationForTrafficClassOfServiceChain(scId, tcId);
    }

    @Override
    public void printRuntimeInformationForServiceChain(ServiceChainId scId) {
        serviceChainStore.printRuntimeInformationForServiceChain(scId);
    }

    /************************************ Internal methods. ***********************************/

    /**
     * Performs a state transition for a service chain.
     */
    private void setServiceChainState(ServiceChainInterface sc, ServiceChainState newScState) {
        ServiceChainState currState = sc.state();

        if (currState != newScState) {
            /**
             * Update the state of this service chain's
             * network functions.
             */
            this.setServiceChainNFsState(sc, newScState);

            /**
             * Now, we can update the state of the
             * service chain itself.
             */
            sc.setState(newScState);
            ServiceChainState updatedState = newScState;
            updatedState.process(this, sc);

            // Store the updated service chain
            serviceChainStore.updateServiceChain(sc.id(), sc);
        } else {
            log.debug("[{}] Current state {} is the same as the new state {}",
                label(), currState, newScState);
        }
    }

    /**
     * A service chain's state transition implies the
     * respective transitions of all of its network functions.
     */
    private void setServiceChainNFsState(
            ServiceChainInterface sc,
            ServiceChainState newScState) {
        for (ServiceChainVertexInterface scV : sc.serviceChainGraph().getVertexes()) {
            NetworkFunctionInterface nf = scV.networkFunction();

            if (nf == null) {
                log.info("[{}] Network function is NULL", label());
                continue;
            }

            NetworkFunctionState curNfState = nf.state();
            NetworkFunctionState newNfState = newScState.asNetworkFunctionState();

            if (curNfState != newNfState) {
                nf.setState(newNfState);
                log.info("[{}] Service chain {}: Network function {} state transition {} --> {}",
                    label(), sc.name(), nf.name(), curNfState, newNfState);
            }
        }
    }

    /**
     * Removes the existing configuration of service chains.
     */
    private void removeConfiguration() {
        serviceChainStore.removeAllServiceChains();
    }

    /**
     * Returns a label with the service chain manager's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return MANAGER_LABEL;
    }

    /**
     * Mandatory implementation mandated by the AbstractListenerProviderRegistry.
     *
     * @param provider a service chain provider
     * @return a new ServiceChainProviderService
     */
    @Override
    protected ServiceChainProviderService createProviderService(ServiceChainProvider provider) {
        return new InternalServiceChainProviderService(provider);
    }

    /**
     * Personalized service chain provider service
     * issued to the supplied provider.
     * TODO: Potentially useless class
     */
    private class InternalServiceChainProviderService
            extends AbstractProviderService<ServiceChainProvider>
            implements ServiceChainProviderService {

        InternalServiceChainProviderService(ServiceChainProvider provider) {
            super(provider);
        }

        @Override
        public void serviceChainRegistered(ServiceChainId serviceChainId) {
            checkNotNull(serviceChainId, "[" + label() + "] " + "Service Chain ID is NULL");
        }

    }

    /**
     * Generates events related to the state of a service chain
     * that resides in the Service Chain Store.
     */
    private class InternalStoreDelegate implements ServiceChainDelegate {

        @Override
        public void notify(ServiceChainEvent event) {
            ServiceChainState state  = event.type();
            ServiceChainInterface sc = event.subject();

            // Post this event, such that it can be caught by the interested listeners.
            post(event);

            log.debug("[{}] Service Chain Event: Service chain with ID {} is in state {}",
                label(), sc.id(), state);
        }

    }

}
