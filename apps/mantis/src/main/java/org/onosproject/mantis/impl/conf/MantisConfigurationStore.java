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

// Apache libraries
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

// Mantis libraries
import org.onosproject.mantis.api.conf.DatabaseConfiguration;
import org.onosproject.mantis.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.mantis.api.conf.IngressPoint;
import org.onosproject.mantis.api.conf.MantisConfiguration;
import org.onosproject.mantis.api.conf.MantisConfigurationEvent;
import org.onosproject.mantis.api.conf.MantisConfigurationDelegate;
import org.onosproject.mantis.api.conf.MantisConfigurationId;
import org.onosproject.mantis.api.conf.MantisConfigurationState;
import org.onosproject.mantis.api.conf.MantisConfigurationStoreService;
import org.onosproject.mantis.api.conf.PredictionConfiguration;
import org.onosproject.mantis.api.prediction.PredictionMechanism;
import org.onosproject.mantis.api.prediction.PredictionMechanism.PredictionMethod;

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
 * Manages the registry of Mantis configurations in the ONOS core.
 */
@Component(immediate = true)
@Service
public class MantisConfigurationStore
        extends AbstractStore<MantisConfigurationEvent, MantisConfigurationDelegate>
        implements MantisConfigurationStoreService {

    private static final Logger log = getLogger(MantisConfigurationStore.class);

    private static final String COMPONET_LABEL = "Distributed Store";

    /**
     * ONOS's distributed storage service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    /**
     * The distributed store keeps track of the latest Mantis configuration.
     */
    private AtomicValue<MantisConfiguration> mantisConfiguration;

    /**
     * Catch important events taking place in the store.
     */
    private AtomicValueEventListener<MantisConfiguration> mantisConfStoreListener =
        new InternalValueListener();

    @Activate
    protected void activate() {
        /**
         * The serializer of the distributed data store requires
         * all the objects of a Mantis configuration.
         */
        KryoNamespace.Builder mantisConfSerializer = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KryoNamespaces.BASIC)
            .register(
                URI.class,
                ElementId.class,
                MantisConfigurationId.class,
                MantisConfigurationState.class,
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
                MantisConfiguration.class
            );

        // Build an atomic distributed object
        this.mantisConfiguration = storageService.<MantisConfiguration>atomicValueBuilder()
            .withName("onos-mantis-conf-store")
            .withSerializer(Serializer.using(mantisConfSerializer.build()))
            .build()
            .asAtomicValue();

        // Add a listener
        this.mantisConfiguration.addListener(mantisConfStoreListener);

        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        // Remove the listener before exiting
        this.mantisConfiguration.removeListener(mantisConfStoreListener);

        log.info("[{}] Stopped", this.label());
    }

    /******************************** Distributed Store services. *****************************/

    @Override
    public void storeMantisConfiguration(MantisConfiguration mantisConfiguration) {
        checkNotNull(
            mantisConfiguration,
            "[" + label() +
            "] Failed to update Mantis configuration with ID " +
            mantisConfiguration.id()
        );

        this.mantisConfiguration.set(mantisConfiguration);

        log.info(
            "[{}] Updated Mantis configuration with ID {}",
            label(), mantisConfiguration.id()
        );

        return;
    }

    @Override
    public void updateMantisConfiguration(MantisConfiguration mantisConfiguration) {
        this.storeMantisConfiguration(mantisConfiguration);
    }

    @Override
    public MantisConfiguration mantisConfiguration() {
        return this.mantisConfiguration.get();
    }

    @Override
    public MantisConfigurationState mantisConfigurationState() {
        return this.mantisConfiguration().state();
    }

    /************************************ Internal methods. ***********************************/

    /**
     * Prints the current Mantis configuration from the distributed store.
     */
    private void printMantisConfiguration() {
        log.info("================================================================");
        log.info("=== Mantis configuration: {}", this.mantisConfiguration().toString());
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
     * Generate events when Mantis configuration is modified.
     */
    private class InternalValueListener
            implements AtomicValueEventListener<MantisConfiguration> {

        @Override
        public void event(AtomicValueEvent<MantisConfiguration> event) {
            switch (event.type()) {
                case UPDATE:
                    // A new/updated Mantis configuration
                    MantisConfiguration mantisConfiguration = event.newValue();
                    checkNotNull(
                        mantisConfiguration,
                        "Attempted to store a NULL Mantis configuration"
                    );

                    log.debug(
                        "[{}] Triggered Mantis configuration update with ID {}",
                        label(), mantisConfiguration.id()
                    );

                    // Notification about the inserted/updated instance
                    notifyDelegate(
                        new MantisConfigurationEvent(
                            mantisConfiguration.state(),
                            mantisConfiguration
                        )
                    );

                    break;

                default:
                    break;
            }
        }

    }

}
