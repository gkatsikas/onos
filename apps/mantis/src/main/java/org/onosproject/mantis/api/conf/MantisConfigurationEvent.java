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

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes Mantis configuration events.
 */
public class MantisConfigurationEvent
        extends AbstractEvent<MantisConfigurationState, MantisConfiguration> {

    /**
     * Creates a Mantis configuration event.
     *
     * @param state event state
     * @param mantisConfiguration Mantis configuration
     */
    public MantisConfigurationEvent(
            MantisConfigurationState state,
            MantisConfiguration mantisConfiguration) {
        super(state, mantisConfiguration);
    }

    /**
     * Creates a Mantis configuration event associated with a timestamp.
     *
     * @param state event state
     * @param mantisConfiguration Mantis configuration
     * @param time event timestamp
     */
    public MantisConfigurationEvent(
            MantisConfigurationState state,
            MantisConfiguration mantisConfiguration,
            long time) {
        super(state, mantisConfiguration, time);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("mantis-configuration-state", type())
                .add("mantis-configuration", subject())
                .toString();
    }

}
