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

// OSGI libraries
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

// Network Monitor libraries
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.networkmonitor.api.conf.IngressPoint;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfiguration;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationEvent;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationDelegate;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationId;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationState;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationStoreService;
import org.onosproject.networkmonitor.api.conf.PredictionConfiguration;
import org.onosproject.networkmonitor.api.prediction.PredictionMechanism;
import org.onosproject.networkmonitor.api.prediction.PredictionMechanism.PredictionMethod;

// ONOS libraries
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.PortNumber;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onlab.util.KryoNamespace;

// Other libraries
import org.slf4j.Logger;

// Java libraries
import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the registry of Network Monitor configurations in the ONOS core.
 */
@Component(immediate = true, service = NetworkMonitorConfigurationStoreService.class)
public class NetworkMonitorConfigurationStore
        extends AbstractStore<NetworkMonitorConfigurationEvent, NetworkMonitorConfigurationDelegate>
        implements NetworkMonitorConfigurationStoreService {

    private static final Logger log = getLogger(NetworkMonitorConfigurationStore.class);

    private static final String COMPONET_LABEL = "Distributed Store";

    /**
     * ONOS's distributed storage service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    /**
     * The distributed store keeps track of the latest Network Monitor configuration.
     */
    private AtomicValue<NetworkMonitorConfiguration> networkMonitorConfiguration;

    /**
     * Catch important events taking place in the store.
     */
    private AtomicValueEventListener<NetworkMonitorConfiguration> networkMonitorConfStoreListener =
        new InternalValueListener();

    @Activate
    protected void activate() {
        /**
         * The serializer of the distributed data store requires
         * all the objects of a Network Monitor configuration.
         */
        KryoNamespace.Builder networkMonitorConfSerializer = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KryoNamespaces.BASIC)
            .register(
                URI.class,
                ElementId.class,
                NetworkMonitorConfigurationId.class,
                NetworkMonitorConfigurationState.class,
                DatabaseConfiguration.class,
                IpAddress.class,
                TpPort.class,
                RetentionPolicy.class,
                IngressPoint.class,
                DeviceId.class,
                PortNumber.class,
                PredictionConfiguration.class,
                PredictionMechanism.class,
                PredictionMethod.class,
                NetworkMonitorConfiguration.class
            );

        // Build an atomic distributed object
        this.networkMonitorConfiguration = storageService.<NetworkMonitorConfiguration>atomicValueBuilder()
            .withName("onos-networkMonitor-conf-store")
            .withSerializer(Serializer.using(networkMonitorConfSerializer.build()))
            .build()
            .asAtomicValue();

        // Add a listener
        this.networkMonitorConfiguration.addListener(networkMonitorConfStoreListener);

        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        // Remove the listener before exiting
        this.networkMonitorConfiguration.removeListener(networkMonitorConfStoreListener);

        log.info("[{}] Stopped", this.label());
    }

    /******************************** Distributed Store services. *****************************/

    @Override
    public void storeNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration) {
        checkNotNull(
            networkMonitorConfiguration,
            "[" + label() +
            "] Failed to update Network Monitor configuration with ID " +
            networkMonitorConfiguration.id()
        );

        this.networkMonitorConfiguration.set(networkMonitorConfiguration);

        log.info(
            "[{}] Updated Network Monitor configuration with ID {}",
            label(), networkMonitorConfiguration.id()
        );

        return;
    }

    @Override
    public void updateNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration) {
        this.storeNetworkMonitorConfiguration(networkMonitorConfiguration);
    }

    @Override
    public NetworkMonitorConfiguration networkMonitorConfiguration() {
        return this.networkMonitorConfiguration.get();
    }

    @Override
    public NetworkMonitorConfigurationState networkMonitorConfigurationState() {
        return this.networkMonitorConfiguration().state();
    }

    /************************************ Internal methods. ***********************************/

    /**
     * Prints the current Network Monitor configuration from the distributed store.
     */
    private void printNetworkMonitorConfiguration() {
        log.info("================================================================");
        log.info("=== Network Monitor configuration: {}", this.networkMonitorConfiguration().toString());
        log.info("================================================================");
    }

    /**
     * Returns a label with the distributed store's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

    /**
     * Generate events when Network Monitor configuration is modified.
     */
    private class InternalValueListener
            implements AtomicValueEventListener<NetworkMonitorConfiguration> {

        @Override
        public void event(AtomicValueEvent<NetworkMonitorConfiguration> event) {
            switch (event.type()) {
                case UPDATE:
                    // A new/updated Network Monitor configuration
                    NetworkMonitorConfiguration networkMonitorConfiguration = event.newValue();
                    checkNotNull(
                        networkMonitorConfiguration,
                        "Attempted to store a NULL Network Monitor configuration"
                    );

                    log.debug(
                        "[{}] Triggered Network Monitor configuration update with ID {}",
                        label(), networkMonitorConfiguration.id()
                    );

                    // Notification about the inserted/updated instance
                    notifyDelegate(
                        new NetworkMonitorConfigurationEvent(
                            networkMonitorConfiguration.state(),
                            networkMonitorConfiguration
                        )
                    );

                    break;

                default:
                    break;
            }
        }

    }

}
