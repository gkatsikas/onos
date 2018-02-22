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

package org.onosproject.mantis.impl.conf;

import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onosproject.mantis.api.common.Constants;
import org.onosproject.mantis.api.conf.DatabaseConfiguration;
import org.onosproject.mantis.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.mantis.api.conf.IngressPoint;
import org.onosproject.mantis.api.conf.MantisConfiguration;
import org.onosproject.mantis.api.conf.MantisConfigurationService;
import org.onosproject.mantis.api.conf.PredictionConfiguration;
import org.onosproject.mantis.api.conf.MantisConfigurationEvent;
import org.onosproject.mantis.api.conf.MantisConfigurationDelegate;
import org.onosproject.mantis.api.conf.MantisConfigurationId;
import org.onosproject.mantis.api.conf.MantisConfigurationListenerInterface;
import org.onosproject.mantis.api.conf.MantisConfigurationProvider;
import org.onosproject.mantis.api.conf.MantisConfigurationProviderService;
import org.onosproject.mantis.api.conf.MantisConfigurationState;
import org.onosproject.mantis.api.conf.MantisConfigurationStoreService;
import org.onosproject.mantis.api.exception.InputConfigurationException;

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

@Component(immediate = true)
@Service
public class MantisConfigurationManager
        extends AbstractListenerProviderRegistry<
            MantisConfigurationEvent, MantisConfigurationListenerInterface,
            MantisConfigurationProvider, MantisConfigurationProviderService
        >
        implements MantisConfigurationService {

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
    private static final String COMPONET_LABEL = "Mantis Configuration Manager";

    /**
     * Application ID for MantisConfigurationManager.
     */
    private static ApplicationId appId;

    /**
     * Latest cached configuration from the store.
     */
    private static MantisConfiguration mantisConfiguration;

    /**
     * Indicates database readiness.
     */
    private static AtomicBoolean mantisConfigurationReady;

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
     * Services used by the Mantis Configuration manager.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MantisConfigurationStoreService mantisConfigurationStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netcfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    /**
     * Undertakes the delegation of Mantis configuration events.
     */
    private final MantisConfigurationDelegate delegate =
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

    public MantisConfigurationManager() {
        this.setMantisConfigurationReady(false);
    }

    @Activate
    public void activate() {
        this.appId = coreService.registerApplication(APP_NAME);

        // Set the delegate of the store
        mantisConfigurationStore.setDelegate(delegate);
        // .. and ensure that there is a sink for the generated events
        this.eventDispatcher.addSink(MantisConfigurationEvent.class, listenerRegistry);

        netcfgRegistry.addListener(netcfgListener);
        netcfgRegistry.registerConfigFactory(configFactory);

        log.info("[{}] Started", label());
    }

    @Deactivate
    public void deactivate() {
        log.info("[{}] Stopped", label());

        mantisConfigurationStore.unsetDelegate(delegate);

        netcfgRegistry.removeListener(netcfgListener);
        netcfgRegistry.unregisterConfigFactory(configFactory);
    }

    @Override
    public boolean isMantisConfigurationReady() {
        return (this.mantisConfiguration != null);
    }

    @Override
    public DatabaseConfiguration databaseConfiguration() {
        return this.mantisConfiguration.databaseConfiguration();
    }

    @Override
    public Set<IngressPoint> ingressPoints() {
        return this.mantisConfiguration.ingressPoints();
    }

    @Override
    public PredictionConfiguration predictionConfiguration() {
        return this.mantisConfiguration.predictionConfiguration();
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
    public void processInitState(MantisConfiguration mantisConfiguration) {
        mantisConfiguration.setState(MantisConfigurationState.INIT);
        log.info(
            "[{}] Mantis configuration is in state {}",
            label(), mantisConfiguration.state()
        );
    }

    @Override
    public void processActiveState(MantisConfiguration mantisConfiguration) {
        mantisConfiguration.setState(MantisConfigurationState.ACTIVE);
        log.info(
            "[{}] Mantis configuration is in state {}",
            label(), mantisConfiguration.state()
        );
    }

    @Override
    public void processInactiveState(MantisConfiguration mantisConfiguration) {
        mantisConfiguration.setState(MantisConfigurationState.INACTIVE);
        log.info(
            "[{}] Mantis configuration is in state {}",
            label(), mantisConfiguration.state()
        );
    }

    @Override
    public MantisConfiguration readConfiguration() {
        ConfigurationLoader config = netcfgRegistry.getConfig(this.appId, CONFIG_CLASS);
        if (config == null) {
            log.warn("[{}] No configuration found", this.label());
            log.info(Constants.STDOUT_BARS);
            return null;
        }

        // Load input configuration
        this.mantisConfiguration = config.loadMantisConf(this.appId);

        log.info(Constants.STDOUT_BARS);

        return this.mantisConfiguration;
    }

    @Override
    public void registerConfiguration(MantisConfiguration mantisConfiguration) {
        checkNotNull(
            mantisConfiguration,
            "Unable to register a NULL Mantis configuration"
        );

        // Flag that Mantis configuration is available
        setMantisConfigurationReady(true);

        //Configuration state transition: INIT --> ACTIVE
        this.processActiveState(mantisConfiguration);

        // Store this configuration into the distributed store
        this.storeMantisConfiguration(mantisConfiguration);

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
        MantisConfiguration remMonitorConfig = config.loadMantisConf(this.appId);
        if (remMonitorConfig == null) {
            return false;
        }

        // Ingress points to be removed
        Set<IngressPoint> remIngressPoints = remMonitorConfig.ingressPoints();
        if (remIngressPoints == null) {
            return false;
        }

        // Reduce the set of ingress points
        this.mantisConfiguration.removeIngressPoints(remIngressPoints);

        // Store this updated configuration into the distributed store
        this.updateMantisConfiguration(this.mantisConfiguration);

        return true;
    }

    /******************************** Distributed Store services. *****************************/

    @Override
    public void storeMantisConfiguration(MantisConfiguration mantisConfiguration) {
        mantisConfigurationStore.storeMantisConfiguration(mantisConfiguration);
        // Keep a local reference to this configuration
        this.mantisConfiguration = mantisConfiguration;
    }

    @Override
    public void updateMantisConfiguration(MantisConfiguration mantisConfiguration) {
        mantisConfigurationStore.updateMantisConfiguration(mantisConfiguration);
        // Keep a local reference to this configuration
        this.mantisConfiguration = mantisConfiguration;
    }

    @Override
    public MantisConfiguration mantisConfiguration() {
        return mantisConfigurationStore.mantisConfiguration();
    }

    @Override
    public MantisConfigurationState mantisConfigurationState() {
        return mantisConfigurationStore.mantisConfigurationState();
    }

    /************************************* Internal services. *********************************/

    /**
     * Sets the readiness status of the configuration.
     *
     * @param ready readiness status to set
     */
    private void setMantisConfigurationReady(boolean ready) {
        if (this.mantisConfigurationReady == null) {
            this.mantisConfigurationReady = new AtomicBoolean();
        }
        this.mantisConfigurationReady.set(ready);
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

                    MantisConfiguration mantisConf;
                    try {
                        // Read the configuration
                        mantisConf = readConfiguration();
                    } catch (InputConfigurationException icEx) {
                        mantisConf = null;
                    }

                    checkNotNull(
                        mantisConf,
                        "[" + label() + "] Failed to read configuration"
                    );

                    registerConfiguration(mantisConf);

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
     * @param provider a Mantis configuration provider
     * @return a new MantisConfigurationProviderService
     */
    @Override
    protected MantisConfigurationProviderService createProviderService(
            MantisConfigurationProvider provider) {
        return new InternalMantisConfigurationProviderService(provider);
    }

    /**
     * Personalized Mantis configuration provider service issued to the supplied provider.
     * TODO: Potentially useless class
     */
    private class InternalMantisConfigurationProviderService
            extends AbstractProviderService<MantisConfigurationProvider>
            implements MantisConfigurationProviderService {

        InternalMantisConfigurationProviderService(MantisConfigurationProvider provider) {
            super(provider);
        }

        @Override
        public void mantisConfigurationRegistered(MantisConfigurationId mantisConfigurationId) {
            checkNotNull(
                mantisConfigurationId,
                "[" + label() + "] " +
                "Mantis configuration ID is NULL"
            );
        }

    }

    /**
     * Generates events related to the state of the Mantis configuration.
     */
    private class InternalStoreDelegate implements MantisConfigurationDelegate {
        @Override
        public void notify(MantisConfigurationEvent event) {
            MantisConfigurationState state  = event.type();

            // Post this event, such that it can be caught by the interested listeners.
            post(event);

            log.debug(
                "[{}] [Delegate] Mantis configuration is in state: {}",
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

