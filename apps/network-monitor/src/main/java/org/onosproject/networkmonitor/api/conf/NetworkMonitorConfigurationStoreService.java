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

import org.onosproject.store.Store;

/**
 * Service for storing Network Monitor configuration in ONOS.
 */
public interface NetworkMonitorConfigurationStoreService
        extends Store<NetworkMonitorConfigurationEvent, NetworkMonitorConfigurationDelegate> {

    /**
     * Stores Network Monitor configuration into ONOS's distributed store.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void storeNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Updates a Network Monitor configuration in ONOS's distributed store.
     *
     * @param networkMonitorConfiguration NetworkMonitorConfiguration object
     */
    void updateNetworkMonitorConfiguration(NetworkMonitorConfiguration networkMonitorConfiguration);

    /**
     * Returns the current Network Monitor configuration from ONOS's distributed store.
     *
     * @return NetworkMonitorConfiguration object
     */
    NetworkMonitorConfiguration networkMonitorConfiguration();

    /**
     * Returns the state of the current Network Monitor configuration from ONOS's distributed store.
     *
     * @return NetworkMonitorConfigurationState object
     */
    NetworkMonitorConfigurationState networkMonitorConfigurationState();

}
