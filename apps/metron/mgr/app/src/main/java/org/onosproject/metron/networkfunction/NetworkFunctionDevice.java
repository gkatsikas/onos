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

package org.onosproject.metron.networkfunction;

import org.onosproject.metron.api.networkfunction.NetworkFunctionDeviceInterface;

import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;
import com.google.common.base.MoreObjects;

import static com.google.common.base.MoreObjects.*;

/**
 * Default representation of a network function's device.
 */
public final class NetworkFunctionDevice
        extends DefaultDevice implements NetworkFunctionDeviceInterface {

    private static final String VIRTUAL = "virtual";
    private static final ProviderId PID = new ProviderId(VIRTUAL, VIRTUAL);

    private String name;
    private final NetworkId networkId;

    /**
     * Creates a network function's device attributed to the specified provider.
     *
     * @param name      name of the network function
     * @param networkId network identifier
     * @param id        device identifier
     */
    public NetworkFunctionDevice(String name, NetworkId networkId, DeviceId id) {
        super(PID, id, Type.VIRTUAL, VIRTUAL, VIRTUAL, VIRTUAL, VIRTUAL,
              new ChassisId(0));
        this.name      = name;
        this.networkId = networkId;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public NetworkId networkId() {
        return this.networkId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, networkId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetworkFunctionDevice) {
            NetworkFunctionDevice that = (NetworkFunctionDevice) obj;
            return super.equals(that) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.networkId, that.networkId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name",  name)
                .add("networkId", networkId)
                .toString();
    }

}
