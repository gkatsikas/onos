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

/**
 * State of Network Monitor configuration.
 */
public enum NetworkMonitorConfigurationState {

    /**
     * Signifies that a Network Monitor configuration has been initialized.
     */
    INIT {
        @Override
        public void process(
                NetworkMonitorConfigurationService networkMonitorConfigurationService,
                NetworkMonitorConfiguration networkMonitorConfiguration) {
            networkMonitorConfigurationService.processInitState(networkMonitorConfiguration);
        }
    },
    /**
     * Signifies that a Network Monitor configuration has become ready.
     */
    ACTIVE {
        @Override
        public void process(
                NetworkMonitorConfigurationService networkMonitorConfigurationService,
                NetworkMonitorConfiguration networkMonitorConfiguration) {
            networkMonitorConfigurationService.processActiveState(networkMonitorConfiguration);
        }
    },
    /**
     * Signifies that a Network Monitor configuration has been deactivated.
     */
    INACTIVE {
        @Override
        public void process(
                NetworkMonitorConfigurationService networkMonitorConfigurationService,
                NetworkMonitorConfiguration networkMonitorConfiguration) {
            networkMonitorConfigurationService.processInactiveState(networkMonitorConfiguration);
        }
    };

    /**
     * Processes the state of a Network Monitor configuration.
     *
     * @param networkMonitorConfigurationService NetworkMonitor configuration service
     * @param networkMonitorConfiguration NetworkMonitor configuration
     */
    public abstract void process(
        NetworkMonitorConfigurationService networkMonitorConfigurationService,
        NetworkMonitorConfiguration networkMonitorConfiguration
    );

}
