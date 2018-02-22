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

/**
 * State of Mantis configuration.
 */
public enum MantisConfigurationState {

    /**
     * Signifies that a Mantis configuration has been initialized.
     */
    INIT {
        @Override
        public void process(
                MantisConfigurationService mantisConfigurationService,
                MantisConfiguration mantisConfiguration) {
            mantisConfigurationService.processInitState(mantisConfiguration);
        }
    },
    /**
     * Signifies that a Mantis configuration has become ready.
     */
    ACTIVE {
        @Override
        public void process(
                MantisConfigurationService mantisConfigurationService,
                MantisConfiguration mantisConfiguration) {
            mantisConfigurationService.processActiveState(mantisConfiguration);
        }
    },
    /**
     * Signifies that a Mantis configuration has been deactivated.
     */
    INACTIVE {
        @Override
        public void process(
                MantisConfigurationService mantisConfigurationService,
                MantisConfiguration mantisConfiguration) {
            mantisConfigurationService.processInactiveState(mantisConfiguration);
        }
    };

    /**
     * Processes the state of a Mantis configuration.
     *
     * @param mantisConfigurationService Mantis configuration service
     * @param mantisConfiguration Mantis configuration
     */
    public abstract void process(
        MantisConfigurationService mantisConfigurationService,
        MantisConfiguration mantisConfiguration
    );

}
