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

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ingress point denotes a specific network location
 * where input traffic is expected.
 */
public final class IngressPoint {

    /**
     * An ingress point comprises of a device ID
     * along with a set of port IDs.
     */
    private DeviceId deviceId;
    private List<PortNumber> portIds;

    public IngressPoint(DeviceId deviceId, List<PortNumber> portIds) {
        checkNotNull(
            deviceId,
            "Device ID for ingress point is NULL"
        );
        checkNotNull(
            portIds,
            "Set of port IDs for ingress point is NULL"
        );

        this.deviceId = deviceId;
        this.portIds  = portIds;
    }

    /**
     * Returns the device ID of this ingress point.
     *
     * @return device ID of ingress point
     */
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the set of port IDs of this ingress point.
     *
     * @return set of port IDs of ingress point
     */
    public Set<PortNumber> portIds() {
        return ImmutableSet.copyOf(this.portIds);
    }

    /**
     * Compares two ingress points.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IngressPoint) {
            IngressPoint that = (IngressPoint) obj;
            if (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.portIds, that.portIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, portIds);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("deviceId", deviceId.toString())
            .add("portIds",  portIds.stream()
                                .map(p -> p.toString())
                                .collect(Collectors.joining(" "))
            )
            .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return ingress points builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Ingress points builder.
     */
    public static final class Builder {
        private DeviceId deviceId;
        private List<PortNumber> portIds;

        private Builder() {
        }

        public IngressPoint build() {
            return new IngressPoint(
                deviceId, portIds
            );
        }

        /**
         * Returns ingress points builder with device ID.
         *
         * @param deviceId ingress point device ID
         * @return ingress points builder
         */
        public Builder deviceId(String deviceId) {
            this.deviceId = DeviceId.deviceId(deviceId);
            return this;
        }

        /**
         * Returns ingress points builder with port IDs.
         *
         * @param portIds set of ingress point port IDs
         * @return ingress points builder
         */
        public Builder portIds(Set<Long> portIds) {
            checkNotNull(
                portIds,
                "Set of port IDs for ingress point is NULL"
            );

            this.portIds = new ArrayList<PortNumber>();
            for (Long portL : portIds) {
                long port = portL.longValue();
                checkArgument(
                    port >= 0,
                    "Port ID for ingress point is negative"
                );
                this.portIds.add(PortNumber.portNumber(port));
            }

            return this;
        }
    }

}
