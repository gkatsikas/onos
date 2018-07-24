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

import org.onosproject.net.provider.Provider;

/**
 * Abstraction of an entity capable of supplying
 * events collected from Network Monitor configuration.
 */
public interface NetworkMonitorConfigurationProvider extends Provider {

    /**
     * Triggers an asynchronous discovery of events on the specified Network Monitor
     * configuration, intended to refresh internal event model for configuration.
     *
     * @param networkMonitorConfigurationId ID of the Network Monitor configuration to be probed
     */
    void triggerProbe(NetworkMonitorConfigurationId networkMonitorConfigurationId);

}
