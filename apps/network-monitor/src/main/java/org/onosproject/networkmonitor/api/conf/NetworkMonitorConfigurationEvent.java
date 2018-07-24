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

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes Network Monitor configuration events.
 */
public class NetworkMonitorConfigurationEvent
        extends AbstractEvent<NetworkMonitorConfigurationState, NetworkMonitorConfiguration> {

    /**
     * Creates a Network Monitor configuration event.
     *
     * @param state event state
     * @param networkMonitorConfiguration Network Monitor configuration
     */
    public NetworkMonitorConfigurationEvent(
            NetworkMonitorConfigurationState state,
            NetworkMonitorConfiguration networkMonitorConfiguration) {
        super(state, networkMonitorConfiguration);
    }

    /**
     * Creates a Network Monitor configuration event associated with a timestamp.
     *
     * @param state event state
     * @param networkMonitorConfiguration Network Monitor configuration
     * @param time event timestamp
     */
    public NetworkMonitorConfigurationEvent(
            NetworkMonitorConfigurationState state,
            NetworkMonitorConfiguration networkMonitorConfiguration,
            long time) {
        super(state, networkMonitorConfiguration, time);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("configuration-state", type())
                .add("configuration", subject())
                .toString();
    }

}
