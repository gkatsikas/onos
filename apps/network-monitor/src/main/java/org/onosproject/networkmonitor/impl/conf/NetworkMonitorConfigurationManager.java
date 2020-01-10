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

package org.onosproject.networkmonitor.impl.conf;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onosproject.networkmonitor.api.common.Constants;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.networkmonitor.api.conf.IngressPoint;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfiguration;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationService;
import org.onosproject.networkmonitor.api.conf.PredictionConfiguration;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationEvent;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationDelegate;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationId;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationListenerInterface;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationProvider;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationProviderService;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationState;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationStoreService;
import org.onosproject.networkmonitor.api.exception.InputConfigurationException;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;

import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = NetworkMonitorConfigurationService.class)
public class NetworkMonitorConfigurationManager
        extends AbstractListenerProviderRegistry<
            NetworkMonitorConfigurationEvent, NetworkMonitorConfigurationListenerInterface,
            NetworkMonitorConfigurationProvider, NetworkMonitorConfigurationProviderService
        >
        implements NetworkMonitorConfigurationService {

    private final Logger log = getLogger(getClass());

    /**
     * ONOS application information.
     */
    public static final String APP_NAME = Constants.SYSTEM_PREFIX + ".configuration";
    public static final String CONFIG_TITLE = "configuration";
    public static final Class<ConfigurationLoader> CONFIG_CLASS = ConfigurationLoader.class;

    /**
     * Print ID.
     */
    private static final String COMPONET_LABEL = "Network Monitor Configuration Manager";

    /**
     * Application ID for NetworkMonitorConfigurationManager.
     */
    private static ApplicationId appId;

    /**
     * Latest cached configuration from the store.
     */
    private static NetworkMonitorConfiguration networkMonitorConfiguration;

    /**
     * Indicates database readiness.
     */
    private static AtomicBoolean networkMonitorConfigurationReady;

    /**
     * Database tables' structure.
     */
    public static final String LOAD_TABLE_PRIM_KEY_DEV =
        DatabaseConfiguration.LOAD_TABLE_PRIM_KEY_DEV;
    public static final String LOAD_TABLE_PRIM_KEY_PORT =
        DatabaseConfiguration.LOAD_TABLE_PRIM_KEY_PORT;
    public static final String LOAD_TABLE_ATTRIBUTE_BITS =
        DatabaseConfiguration.LOAD_TABLE_ATTRIBUTE_BITS;
    public static final String LOAD_TABLE_ATTRIBUTE_PACKETS =
        DatabaseConfiguration.LOAD_TABLE_ATTRIBUTE_PACKETS;
    public static final String LOAD_TABLE_ATTRIBUTE_DROPS =
        DatabaseConfiguration.LOAD_TABLE_ATTRIBUTE_DROPS;

    /**
     * Services used by the Network Monitor Configuration manager.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkMonitorConfigurationStoreService networkMonitorConfigurationStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netcfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    /**
     * Undertakes the delegation of Network Monitor configuration events.
     */
    private final NetworkMonitorConfigurationDelegate delegate =
        new InternalStoreDelegate();

    /**
     * Listener for network configuration events.
     */
    private final InternalNetworkConfigListener netcfgListener =
        new InternalNetworkConfigListener();

    /**
     * Network configuration object.
     */
    private final ConfigFactory configFactory =
        new ConfigFactory<ApplicationId, ConfigurationLoader>(
            SubjectFactories.APP_SUBJECT_FACTORY,
            CONFIG_CLASS,
            CONFIG_TITLE) {
                @Override
                public ConfigurationLoader createConfig() {
                    return new ConfigurationLoader();
                }
        };

    public NetworkMonitorConfigurationManager() {
        this.setNetworkMonitorConfigurationReady(false);
    }

    @Activate
    public void activate() {
        this.appId = coreService.registerApplication(APP_NAME);

        // Set the delegate of the store
        networkMonitorConfigurationStore.setDelegate(delegate);
        // .. and ensure that there is a sink for the generated events
        this.eventDispatcher.addSink(NetworkMonitorConfigurationEvent.class, listenerRegistry);

        netcfgRegistry.addListener(netcfgListener);
        netcfgRegistry.registerConfigFactory(configFactory);

        log.info("[{}] Started", label());
    }

    @Deactivate
    public void deactivate() {
        log.info("[{}] Stopped", label());

        networkMonitorConfigurationStore.unsetDelegate(delegate);

        netcfgRegistry.removeListener(netcfgListener);
        netcfgRegistry.unregisterConfigFactory(configFactory);
    }

    @Override
    public boolean isNetworkMonitorConfigurationReady() {
        return (this.networkMonitorConfiguration != null);
    }

    @Override
    public DatabaseConfiguration databaseConfiguration() {
        return this.networkMonitorConfiguration.databaseConfiguration();
    }

    @Override
    public Set<IngressPoint> ingressPoints() {
        return this.networkMonitorConfiguration.ingressPoints();
    }

    @Override
    public PredictionConfiguration predictionConfiguration() {
        return this.networkMonitorConfiguration.predictionConfiguration();
    }

    @Override
    public String databaseName() {
        return this.databaseConfiguration().databaseName();
    }

    @Override
    public RetentionPolicy retentionPolicy() {
        return this.databaseConfiguration().retentionPolicy();
    }

    @Override
    public boolean isBatchWriteEnabled() {
        return this.databaseConfiguration().batchWrite();
    }

    @Override
    public String loadTable() {
        return this.databaseConfiguration().loadTable();
    }

    @Override
    public String predictionTable() {
        return this.databaseConfiguration().predictionTable();
    }

    @Override
    public void processInitState(NetworkMonitorConfiguration networkMonitorConfiguration) {
        networkMonitorConfiguration.setState(NetworkMonitorConfigurationState.INIT);
        log.info(
            "[{}] Network Monitor configuration is in state {}",
            label(), networkMonitorConfiguration.state()
        );
    }

    @Override
    public void processActiveState(NetworkMonitorConfiguration networkMonitorConfiguration) {
        networkMonitorConfiguration.setState(NetworkMonitorConfigurationState.ACTIVE);
        log.info(
            "[{}] Network Monitor configuration is in state {}",
            label(), networkMonitorConfiguration.state()
        );
    }

    @Override
    public void processInactiveState(NetworkMonitorConfiguration networkMonitorConfiguration) {
        networkMonitorConfiguration.setState(NetworkMonitorConfigurationState.INACTIVE);
        log.info(
            "[{}] Network Monitor configuration is in state {}",
            label(), networkMonitorConfiguration.state()
        );
    }

    @Override
    public NetworkMonitorConfiguration readConfiguration() {
        ConfigurationLoader config = netcfgRegistry.getConfig(this.appId, CONFIG_CLASS);
        if (config == null) {
            log.warn("[{}] No configuration found", this.label());
            log.info(Constants.STDOUT_BARS);
            return null;
        }

        // Load input configuration
        this.networkMonitorConfiguration = config.loadNetworkMonitorConf(this.appId);

        log.info(Constants.STDOUT_BARS);

        return this.networkMonitorConfiguration;
    }

    @Override
    public void registerConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration) {
        checkNotNull(
            networkMonitorConfiguration,
            "Unable to register a NULL Network Monitor configuration"
        );

        // Flag that Network Monitor configuration is available
        setNetworkMonitorConfigurationReady(true);

        //Configuration state transition: INIT --> ACTIVE
        this.processActiveState(networkMonitorConfiguration);

        // Store this configuration into the distributed store
        this.storeNetworkMonitorConfiguration(networkMonitorConfiguration);

        return;
    }

    @Override
    public boolean removeConfiguration() {
        // Nothing to remove, ingress points set is already empty
        if ((this.ingressPoints() == null) || (this.ingressPoints().isEmpty())) {
            return true;
        }

        ConfigurationLoader config = netcfgRegistry.getConfig(this.appId, CONFIG_CLASS);
        if (config == null) {
            log.warn("[{}] No configuration found", this.label());
            log.info(Constants.STDOUT_BARS);
            return false;
        }

        // Load input configuration
        NetworkMonitorConfiguration remMonitorConfig = config.loadNetworkMonitorConf(this.appId);
        if (remMonitorConfig == null) {
            return false;
        }

        // Ingress points to be removed
        Set<IngressPoint> remIngressPoints = remMonitorConfig.ingressPoints();
        if (remIngressPoints == null) {
            return false;
        }

        // Reduce the set of ingress points
        this.networkMonitorConfiguration.removeIngressPoints(remIngressPoints);

        // Store this updated configuration into the distributed store
        this.updateNetworkMonitorConfiguration(this.networkMonitorConfiguration);

        return true;
    }

    /******************************** Distributed Store services. *****************************/

    @Override
    public void storeNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration) {
        networkMonitorConfigurationStore.storeNetworkMonitorConfiguration(networkMonitorConfiguration);
        // Keep a local reference to this configuration
        this.networkMonitorConfiguration = networkMonitorConfiguration;
    }

    @Override
    public void updateNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration) {
        networkMonitorConfigurationStore.updateNetworkMonitorConfiguration(networkMonitorConfiguration);
        // Keep a local reference to this configuration
        this.networkMonitorConfiguration = networkMonitorConfiguration;
    }

    @Override
    public NetworkMonitorConfiguration networkMonitorConfiguration() {
        return networkMonitorConfigurationStore.networkMonitorConfiguration();
    }

    @Override
    public NetworkMonitorConfigurationState networkMonitorConfigurationState() {
        return networkMonitorConfigurationStore.networkMonitorConfigurationState();
    }

    /************************************* Internal services. *********************************/

    /**
     * Sets the readiness status of the configuration.
     *
     * @param ready readiness status to set
     */
    private void setNetworkMonitorConfigurationReady(boolean ready) {
        if (this.networkMonitorConfigurationReady == null) {
            this.networkMonitorConfigurationReady = new AtomicBoolean();
        }
        this.networkMonitorConfigurationReady.set(ready);
    }

    /**
     * Handles network configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            // We only care about our events
            if (!event.configClass().equals(CONFIG_CLASS)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_REGISTERED:
                    log.info(Constants.STDOUT_BARS);
                    log.info("[{}] Configuration registered", label());
                    log.info(Constants.STDOUT_BARS);

                    break;
                case CONFIG_ADDED:
                case CONFIG_UPDATED: {
                    log.info("");
                    log.info(Constants.STDOUT_BARS);
                    log.info("[{}] Configuration inserted/updated", label());
                    log.info(Constants.STDOUT_BARS);

                    NetworkMonitorConfiguration networkMonitorConf;
                    try {
                        // Read the configuration
                        networkMonitorConf = readConfiguration();
                    } catch (InputConfigurationException icEx) {
                        networkMonitorConf = null;
                    }

                    checkNotNull(
                        networkMonitorConf,
                        "[" + label() + "] Failed to read configuration"
                    );

                    registerConfiguration(networkMonitorConf);

                    break;
                }
                case CONFIG_UNREGISTERED:
                case CONFIG_REMOVED: {
                    log.info(Constants.STDOUT_BARS);
                    log.info("[{}] Configuration unregistered", label());
                    log.info(Constants.STDOUT_BARS);

                    // Remove configuration
                    if (!removeConfiguration()) {
                        throw new RuntimeException(
                            "[" + label() + "] Failed to remove configuration"
                        );
                    }

                    break;
                }

                default:
                    break;
            }
        }

    }

    /**
     * Mandatory implementation for the AbstractListenerProviderRegistry.
     *
     * @param provider a Network Monitor configuration provider
     * @return a new NetworkMonitorConfigurationProviderService
     */
    @Override
    protected NetworkMonitorConfigurationProviderService createProviderService(
            NetworkMonitorConfigurationProvider provider) {
        return new InternalNetworkMonitorConfigurationProviderService(provider);
    }

    /**
     * Personalized Network Monitor configuration provider service issued to the supplied provider.
     * TODO: Potentially useless class
     */
    private class InternalNetworkMonitorConfigurationProviderService
            extends AbstractProviderService<NetworkMonitorConfigurationProvider>
            implements NetworkMonitorConfigurationProviderService {

        InternalNetworkMonitorConfigurationProviderService(NetworkMonitorConfigurationProvider provider) {
            super(provider);
        }

        @Override
        public void networkMonitorConfigurationRegistered(NetworkMonitorConfigurationId networkMonitorConfigurationId) {
            checkNotNull(
                networkMonitorConfigurationId,
                "[" + label() + "] " +
                "Network Monitor configuration ID is NULL"
            );
        }

    }

    /**
     * Generates events related to the state of the Network Monitor configuration.
     */
    private class InternalStoreDelegate implements NetworkMonitorConfigurationDelegate {
        @Override
        public void notify(NetworkMonitorConfigurationEvent event) {
            NetworkMonitorConfigurationState state  = event.type();

            // Post this event, such that it can be caught by the interested listeners.
            post(event);

            log.debug(
                "[{}] [Delegate] Network Monitor configuration is in state: {}",
                label(), state
            );
        }

    }

    /**
     * Returns a label with the monitor's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}

