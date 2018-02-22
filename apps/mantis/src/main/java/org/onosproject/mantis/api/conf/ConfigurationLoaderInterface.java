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

import org.onosproject.core.ApplicationId;

/**
 * Configuration loader's interface.
 */
public interface ConfigurationLoaderInterface {

    /**
     * Returns a Mantis configuration loaded from JSON.
     *
     * @param appId the application ID that requires this configuration
     * @return MantisConfiguration object
     */
    MantisConfiguration loadMantisConf(ApplicationId appId);

}
