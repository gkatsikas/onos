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

package org.onosproject.metron.application;

// Metron libraries
import org.onosproject.metron.api.application.NfvApplicationService;
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.exceptions.ServiceChainException;
import org.onosproject.metron.api.exceptions.InputConfigurationException;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainService;
import org.onosproject.metron.api.servicechain.ServiceChainListenerInterface;

import org.onosproject.metron.config.ServiceChainAddConfig;
import org.onosproject.metron.config.ServiceChainRemoveConfig;
import org.onosproject.metron.config.ServiceChainRemoveAllConfig;

// ONOS libraries
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;

// Apache libraries
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Other libraries
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Java libraries
import java.util.Iterator;
import java.util.Set;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Metron application Manager.
 * Metron applications remotely interact with the manager
 * in order to register with the core.
 */
@Component(immediate = true)
@Service
public class NfvApplicationManager
        extends ListenerRegistry<ServiceChainEvent, ServiceChainListenerInterface>
        implements NfvApplicationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Some constants.
     */
    public static final String APP_NAME = Constants.SYSTEM_APP_PREFIX;
    public static final String ADD_CONFIG_TITLE = "add";
    public static final String REMOVE_CONFIG_TITLE = "remove";
    public static final String REMOVE_ALL_CONFIG_TITLE = "removeAll";
    private static final Class<ServiceChainAddConfig> ADD_CONFIG_CLASS =
        ServiceChainAddConfig.class;
    private static final Class<ServiceChainRemoveConfig> REMOVE_CONFIG_CLASS =
        ServiceChainRemoveConfig.class;
    private static final Class<ServiceChainRemoveAllConfig> REMOVE_ALL_CONFIG_CLASS =
        ServiceChainRemoveAllConfig.class;
    private static final String COMPONET_LABEL = "Metron Application Manager";

    /**
     * Key members of the Metron Application Manager.
     */
    private ApplicationId appId;
    private Set<ServiceChainInterface> loadedServiceChains = null;

    /**
     * Developers can inject their service chains using this ONOS service.
     */
    private final NetworkConfigListener configListener = new InternalConfigListener();

    /**
     * A dedicated thread pool to load multiple Metron service chains concurrently.
     */
    private static final int LOADER_THREADS_NO = 3;
    private final ExecutorService chainLoaderExecutor = newFixedThreadPool(
        LOADER_THREADS_NO,
        groupedThreads(this.getClass().getSimpleName(), "loader", log)
    );

    /**
     * Services from ONOS.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfigRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ServiceChainService serviceChainService;

    private final ConfigFactory addConfigFactory =
        new ConfigFactory<ApplicationId, ServiceChainAddConfig>(
            SubjectFactories.APP_SUBJECT_FACTORY,
            ADD_CONFIG_CLASS,
            ADD_CONFIG_TITLE
        ) {
            @Override
            public ServiceChainAddConfig createConfig() {
                return new ServiceChainAddConfig();
            }
        };

    private final ConfigFactory removeConfigFactory =
        new ConfigFactory<ApplicationId, ServiceChainRemoveConfig>(
            SubjectFactories.APP_SUBJECT_FACTORY,
            REMOVE_CONFIG_CLASS,
            REMOVE_CONFIG_TITLE
        ) {
            @Override
            public ServiceChainRemoveConfig createConfig() {
                return new ServiceChainRemoveConfig();
            }
        };

    private final ConfigFactory removeAllConfigFactory =
        new ConfigFactory<ApplicationId, ServiceChainRemoveAllConfig>(
            SubjectFactories.APP_SUBJECT_FACTORY,
            REMOVE_ALL_CONFIG_CLASS,
            REMOVE_ALL_CONFIG_TITLE
        ) {
            @Override
            public ServiceChainRemoveAllConfig createConfig() {
                return new ServiceChainRemoveAllConfig();
            }
        };

    public NfvApplicationManager() {
        this.loadedServiceChains = Sets.<ServiceChainInterface>newConcurrentHashSet();
    }

    /**
     * Methods of the Metron Application Manager.
     */
    @Activate
    protected void activate() {
        this.appId = coreService.registerApplication(APP_NAME);

        networkConfigService.addListener(configListener);
        networkConfigRegistry.registerConfigFactory(addConfigFactory);
        networkConfigRegistry.registerConfigFactory(removeConfigFactory);
        networkConfigRegistry.registerConfigFactory(removeAllConfigFactory);

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        networkConfigService.removeListener(configListener);
        networkConfigRegistry.unregisterConfigFactory(addConfigFactory);
        networkConfigRegistry.unregisterConfigFactory(removeConfigFactory);
        networkConfigRegistry.unregisterConfigFactory(removeAllConfigFactory);

        if (!removeConfiguration()) {
            throw new ServiceChainException(
                "[" + label() + "] Failed to remove application configuration"
            );
        }

        this.chainLoaderExecutor.shutdown();

        log.info("[{}] Stopped", label());
    }

    @Override
    public boolean register(ServiceChainInterface sc) {
        if (sc == null) {
            log.error(
                "[{}] Cannot register a NULL service chain.", label());
            return false;
        }

        // Ask for a service chain with this ID
        ServiceChainInterface scCurr = this.serviceChainService.serviceChain(sc.id());

        // This service chain exists, update it
        if (scCurr != null) {
            try {
                this.serviceChainService.updateServiceChainWithAnother(scCurr, sc);
            } catch (ServiceChainException scEx) {
                throw scEx;
            }
        // This service chain does not exist, create it
        } else {
            try {
                this.serviceChainService.registerServiceChain(sc);
            } catch (ServiceChainException scEx) {
                throw scEx;
            }

            log.info("[{}] Service chain with ID {} is registered", label(), sc.id());
        }

        return true;
    }

    @Override
    public boolean unregister(ServiceChainInterface sc) {
        if (sc == null) {
            log.warn("[{}] Cannot unregister a NULL service chain", label());
            log.info(Constants.STDOUT_BARS);
            return false;
        }

        /**
         * This manager does not need to remove the service chain from the store,
         * but rather notify other managers that this service chain should be
         * undeployed. This is done by performing a state transition:
         * From DEPLOYED --> DESTROYED
         */
        this.serviceChainService.processDestroyedState(sc);

        log.info("");
        log.info("[{}] Service chain {} is unregistered", label(), sc.name());

        return true;
    }

    @Override
    public boolean deploy(ServiceChainInterface sc) {
        log.info("");
        log.info(Constants.STDOUT_BARS);
        log.info("=== Deploy service chain {}", sc.id());
        log.info(Constants.STDOUT_BARS);

        boolean success = false;
        try {
            success = this.register(sc);
        } catch (ServiceChainException scEx) {
            throw scEx;
        }

        if (!success) {
            log.info(Constants.STDOUT_BARS);
            return false;
        }

        /**
         * State transition for the service chain
         * From INIT --> CONSTRUCTED
         */
        this.serviceChainService.processConstructedState(
            this.serviceChainService.serviceChain(sc.id())
        );

        log.info(Constants.STDOUT_BARS);

        return true;
    }

    @Override
    public boolean retract(ServiceChainInterface sc) {
        log.info("");
        log.info(Constants.STDOUT_BARS);
        log.info("=== Retract service chain {}", sc.id());
        log.info(Constants.STDOUT_BARS);

        boolean status = false;
        try {
            status = this.unregister(sc);
        } catch (ServiceChainException scEx) {
            throw scEx;
        }

        log.info(Constants.STDOUT_BARS);
        log.info("");

        return status;
    }

    @Override
    public Set<ServiceChainInterface> readAddConfiguration()
            throws IOException, InputConfigurationException {
        ServiceChainAddConfig config = networkConfigRegistry.getConfig(this.appId, ADD_CONFIG_CLASS);
        if (config == null) {
            log.info("[{}] No configuration found", label());
            log.info(Constants.STDOUT_BARS);
            return null;
        }

        Set<ServiceChainInterface> newChains = null;

        // Load the new application configuration
        try {
            newChains = config.loadServiceChains(this.appId);
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (InputConfigurationException icEx) {
            throw icEx;
        }

        log.info(Constants.STDOUT_BARS);

        return newChains;
    }

    @Override
    public boolean readRemoveConfiguration() throws IOException, InputConfigurationException {
        ServiceChainRemoveConfig config = networkConfigRegistry.getConfig(this.appId, REMOVE_CONFIG_CLASS);
        if (config == null) {
            log.info("[{}] No configuration found", label());
            log.info(Constants.STDOUT_BARS);
            return false;
        }

        Set<ServiceChainId> scIds = null;

        // Load the new configuration
        try {
            scIds = config.undeployServiceChains(this.appId);
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (InputConfigurationException icEx) {
            throw icEx;
        }

        // Nothing essential given by the user
        if ((scIds == null) || scIds.isEmpty()) {
            log.info(Constants.STDOUT_BARS);
            return true;
        }

        // Undeploy these chains one by one
        short undeployed = 0;
        for (ServiceChainId scId : scIds) {
            Iterator<ServiceChainInterface> scIterator = this.loadedServiceChains.iterator();
            if (scIterator == null) {
                break;
            }

            while (scIterator.hasNext()) {
                ServiceChainInterface sc = scIterator.next();
                checkNotNull(sc, "Loaded service chain is NULL");
                ServiceChainId loadedScId = sc.id();

                // Not this one
                if (!scId.equals(loadedScId)) {
                    continue;
                }

                if (!this.retract(sc)) {
                    return false;
                }

                scIterator.remove();
                undeployed++;
            }
        }

        // Leave a message to the guy who composed the JSON configuration
        if (undeployed != scIds.size()) {
            log.warn(
                "[{}] Only {} out of {} service chains were undeployed. " +
                "Check your configuration next time.", label(), undeployed, scIds.size()
            );
        }

        log.info(Constants.STDOUT_BARS);

        return true;
    }

    @Override
    public boolean readRemoveAllConfiguration() throws IOException, InputConfigurationException {
        ServiceChainRemoveAllConfig config = networkConfigRegistry.getConfig(this.appId, REMOVE_ALL_CONFIG_CLASS);
        if (config == null) {
            log.info("[{}] No configuration found", label());
            log.info(Constants.STDOUT_BARS);
            return false;
        }

        return removeConfiguration();
    }

    @Override
    public boolean loadConfiguration(Set<ServiceChainInterface> chainsToLoad) {
        log.info("");

        if (chainsToLoad.isEmpty()) {
            log.error("No configuration is available");
            log.info(Constants.STDOUT_BARS);
            return false;
        }

        // Iterate through the loaded service chains and update the service chain store
        for (ServiceChainInterface sc : chainsToLoad) {
            log.info("[{}] Deploying service chain {} with ID {}", label(), sc.name(), sc.id());

            if (!this.deploy(sc)) {
                return false;
            }

            // Update our local memory of loaded service chains
            this.loadedServiceChains.add(sc);
        }

        return true;
    }

    /**
     * Removes the loaded Metron service chains during shutdown.
     * Notifies ther managers to properly cleanup the system.
     *
     * @return boolean removal status
     */
    private boolean removeConfiguration() {
        if (!this.availableServiceChains()) {
            return true;
        }

        Iterator<ServiceChainInterface> scIterator = this.loadedServiceChains.iterator();
        while (scIterator.hasNext()) {
            ServiceChainInterface sc = scIterator.next();
            log.info("[{}] Retracting service chain {} with ID {}", label(), sc.name(), sc.id());

            if (!this.retract(sc)) {
                return false;
            }

            scIterator.remove();
        }

        checkArgument(
            !this.availableServiceChains(),
            label() + " could not properly retract all service chains during shutdown"
        );

        return true;
    }

    /**
     * Returns whether there are loaded service chains in the manager.
     *
     * @return boolean status
     */
    private boolean availableServiceChains() {
        return (this.loadedServiceChains != null) &&
               !this.loadedServiceChains.isEmpty();
    }

    /**
     * Returns a label with the application's identify.
     * Serves for printing.
     *
     * @return application label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

    /**
     * Handles application configuration events.
     */
    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {

            if (event.configClass().equals(ADD_CONFIG_CLASS)) {
                switch (event.type()) {
                    case CONFIG_REGISTERED:
                        log.info("[{}] Add application configuration registered", label());

                        break;
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED: {
                        chainLoaderExecutor.execute(() -> {
                            log.info("");
                            log.info(Constants.STDOUT_BARS);
                            log.info("[{}] Add application configuration inserted/updated", label());
                            log.info(Constants.STDOUT_BARS);

                            // Read new configuration
                            Set<ServiceChainInterface> chainsToLoad = null;
                            try {
                                chainsToLoad = readAddConfiguration();
                            } catch (IOException ioEx) {
                                throw new InputConfigurationException(
                                    "[" + label() + "] Failed to read add application configuration"
                                );
                            }

                            if (chainsToLoad != null) {
                                // And load it to the core
                                if (!loadConfiguration(chainsToLoad)) {
                                    throw new InputConfigurationException(
                                        "[" + label() + "] Failed to load add application configuration"
                                    );
                                }
                            }
                        });

                        break;
                    }
                    case CONFIG_UNREGISTERED:
                    case CONFIG_REMOVED: {
                        break;
                    }

                    default:
                        break;
                }
            } else if (event.configClass().equals(REMOVE_CONFIG_CLASS)) {
                switch (event.type()) {
                    case CONFIG_REGISTERED:
                        log.info("[{}] Remove application configuration registered", label());

                        break;
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED: {
                        log.info("");
                        log.info(Constants.STDOUT_BARS);
                        log.info("[{}] Remove application configuration inserted/updated", label());
                        log.info(Constants.STDOUT_BARS);

                        // Read the configuration
                        boolean status = false;
                        try {
                            status = readRemoveConfiguration();
                        } catch (IOException ioEx) {
                            throw new InputConfigurationException(
                                "[" + label() + "] Failed to read remove application configuration"
                            );
                        }

                        break;
                    }
                    case CONFIG_UNREGISTERED:
                    case CONFIG_REMOVED: {
                        break;
                    }

                    default:
                        break;
                }
            } else if (event.configClass().equals(REMOVE_ALL_CONFIG_CLASS)) {
                switch (event.type()) {
                    case CONFIG_REGISTERED:
                        log.info("[{}] RemoveAll application configuration registered", label());

                        break;
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED: {
                        log.info("");
                        log.info(Constants.STDOUT_BARS);
                        log.info("[{}] RemoveAll application configuration inserted/updated", label());
                        log.info(Constants.STDOUT_BARS);

                        // Read the configuration
                        boolean status = false;
                        try {
                            status = readRemoveAllConfiguration();
                        } catch (IOException ioEx) {
                            throw new InputConfigurationException(
                                "[" + label() + "] Failed to read remove all application configuration"
                            );
                        }

                        break;
                    }
                    case CONFIG_UNREGISTERED:
                    case CONFIG_REMOVED: {
                        break;
                    }

                    default:
                        break;
                }
            }
        }

    }

}
