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

package org.onosproject.networkmonitor.api.conf;

import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.networkmonitor.api.exception.InputConfigurationException;

import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Services provided by a Network Monitor Configuration manager.
 */
public interface NetworkMonitorConfigurationService
        extends ListenerService<
            NetworkMonitorConfigurationEvent, NetworkMonitorConfigurationListenerInterface> {

    /**
     * Returns whether the Network Monitor configuration is ready or not.
     *
     * @return boolean readiness status
     */
    boolean isNetworkMonitorConfigurationReady();

    /**
     * Returns the Network Monitor databaseconfiguration.
     *
     * @return DatabaseConfiguration object
     */
    DatabaseConfiguration databaseConfiguration();

    /**
     * Returns the set of ingress points
     * of the Network Monitor configuration.
     *
     * @return set of ingress points
     */
    Set<IngressPoint> ingressPoints();

    /**
     * Returns the Network Monitor prediction configuration.
     *
     * @return PredictionConfiguration object
     */
    PredictionConfiguration predictionConfiguration();

    /**
     * Returns the name of the Network Monitor database.
     *
     * @return Network Monitor database name
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
     * Bootstraps Network Monitor configuration with INIT state.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void processInitState(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Activates Network Monitor configuration with ACTIVE state.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void processActiveState(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Deactivates Network Monitor configuration with INACTIVE state.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void processInactiveState(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Reads input configuration by calling the appropriate parser.
     *
     * @return the loaded NetworkMonitorConfiguration
     * @throws InputConfigurationException if read fails
     */
    NetworkMonitorConfiguration readConfiguration() throws InputConfigurationException;

    /**
     * Registers input configuration to the distributed ONOS core.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration to register
     */
    void registerConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration);

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
     * Stores Network Monitor configuration into ONOS's distributed core.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void storeNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Updates a Network Monitor configuration in ONOS's distributed core.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void updateNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Returns the current Network Monitor configuration from ONOS's distributed core.
     *
     * @return NetworkMonitorConfiguration object
     */
    NetworkMonitorConfiguration networkMonitorConfiguration();

    /**
     * Returns the state of the current NetworkMonitor configuration
     * from ONOS's distributed core.
     *
     * @return NetworkMonitorConfigurationState object
     */
    NetworkMonitorConfigurationState networkMonitorConfigurationState();

}
