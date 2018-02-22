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

package org.onosproject.mantis.api.conf;

import org.onosproject.mantis.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.mantis.api.exception.InputConfigurationException;

import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Services provided by a Mantis Configuration manager.
 */
public interface MantisConfigurationService
        extends ListenerService<
            MantisConfigurationEvent, MantisConfigurationListenerInterface> {

    /**
     * Returns whether the Mantis configuration is ready or not.
     *
     * @return boolean readiness status
     */
    boolean isMantisConfigurationReady();

    /**
     * Returns the Mantis databaseconfiguration.
     *
     * @return DatabaseConfiguration object
     */
    DatabaseConfiguration databaseConfiguration();

    /**
     * Returns the set of ingress points
     * of the Mantis configuration.
     *
     * @return set of ingress points
     */
    Set<IngressPoint> ingressPoints();

    /**
     * Returns the Mantis prediction configuration.
     *
     * @return PredictionConfiguration object
     */
    PredictionConfiguration predictionConfiguration();

    /**
     * Returns the name of the Mantis database.
     *
     * @return Mantis database name
     */
    String databaseName();

    /**
     * Returns the retention policy of the database.
     *
     * @return retention policy of the database
     */
    RetentionPolicy retentionPolicy();

    /**
     * Returns whether batch database write operations are enabled.
     *
     * @return batch database write operations status
     */
    boolean isBatchWriteEnabled();

    /**
     * Returns the name of the database's load table.
     *
     * @return database load table name
     */
    String loadTable();

    /**
     * Returns the name of the database's prediction table.
     *
     * @return database prediction table name
     */
    String predictionTable();

    /**
     * Bootstraps Mantis configuration with INIT state.
     *
     * @param mantisConfiguration MantisConfiguration object
     */
    void processInitState(MantisConfiguration mantisConfiguration);

    /**
     * Activates Mantis configuration with ACTIVE state.
     *
     * @param mantisConfiguration MantisConfiguration object
     */
    void processActiveState(MantisConfiguration mantisConfiguration);

    /**
     * Deactivates Mantis configuration with INACTIVE state.
     *
     * @param mantisConfiguration MantisConfiguration object
     */
    void processInactiveState(MantisConfiguration mantisConfiguration);

    /**
     * Reads input configuration by calling the appropriate parser.
     *
     * @return the loaded MantisConfiguration
     * @throws InputConfigurationException if read fails
     */
    MantisConfiguration readConfiguration() throws InputConfigurationException;

    /**
     * Registers input configuration to the distributed ONOS core.
     *
     * @param mantisConfiguration MantisConfiguration to register
     */
    void registerConfiguration(MantisConfiguration mantisConfiguration);

    /**
     * Removes configuration.
     *
     * @return boolean status
     *         (true for success, false for failure)
     * @throws InputConfigurationException if remove fails
     */
    boolean removeConfiguration() throws InputConfigurationException;

    /******************************** Distributed Store services. *****************************/

    /**
     * Stores Mantis configuration into ONOS's distributed core.
     *
     * @param mantisConfiguration MantisConfiguration object
     */
    void storeMantisConfiguration(MantisConfiguration mantisConfiguration);

    /**
     * Updates a Mantis configuration in ONOS's distributed core.
     *
     * @param mantisConfiguration MantisConfiguration object
     */
    void updateMantisConfiguration(MantisConfiguration mantisConfiguration);

    /**
     * Returns the current Mantis configuration from ONOS's distributed core.
     *
     * @return MantisConfiguration object
     */
    MantisConfiguration mantisConfiguration();

    /**
     * Returns the state of the current Mantis configuration
     * from ONOS's distributed core.
     *
     * @return MantisConfigurationState object
     */
    MantisConfigurationState mantisConfigurationState();

}
