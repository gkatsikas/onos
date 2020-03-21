/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.metron.api.networkfunction;

import com.google.common.annotations.Beta;
import org.onosproject.incubator.net.virtual.VirtualDevice;

/**
 * Abstraction of a network function's device.
 */
@Beta
public interface NetworkFunctionDeviceInterface extends VirtualDevice {

    /**
     * Returns the name of this device.
     *
     * @return network function's device name
     */
    String name();

}
