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

package org.onosproject.metron.api.config;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Denotes a specific network location
 * where input/output traffic is expected.
 */
public final class TrafficPoint {

    /**
     * A traffic point can be ingress or egress.
     */
    public enum TrafficPointType {
        INGRESS("ingress"),
        EGRESS("egress");

        private String tpType;

        /**
         * Statically maps traffic point types to enum types.
         */
        private static final Map<String, TrafficPointType> MAP =
            new HashMap<String, TrafficPointType>();

        static {
            for (TrafficPointType tpType : TrafficPointType.values()) {
                MAP.put(tpType.toString().toLowerCase(), tpType);
            }
        }

        private TrafficPointType(String tpType) {
            checkArgument(
                !Strings.isNullOrEmpty(tpType),
                "Traffic point type is NULL or empty"
            );
            this.tpType = tpType;
        }

        /**
         * Returns a traffic point type deriving from an input name.
         *
         * @param tpType a string-based traffic point type
         * @return a traffic point type object
         */
        public static TrafficPointType getByName(String tpType) {
            if (Strings.isNullOrEmpty(tpType)) {
                return null;
            }

            return MAP.get(tpType.toLowerCase());
        }

        @Override
        public String toString() {
            return this.tpType;
        }

    }

    /**
     * A traffic point comprises of a type, device ID,
     * along with a set of port IDs.
     */
    private TrafficPointType tpType;
    private DeviceId         deviceId;
    private List<PortNumber> portIds;

    public TrafficPoint(
            TrafficPointType tpType, DeviceId deviceId, List<PortNumber> portIds) {
        checkNotNull(
            tpType,
            "Traffic point type is NULL"
        );
        checkNotNull(
            deviceId,
            "Device ID for traffic point is NULL"
        );
        checkNotNull(
            portIds,
            "Set of port IDs for traffic point is NULL"
        );

        this.tpType   = tpType;
        this.deviceId = deviceId;
        this.portIds  = portIds;
    }

    /**
     * Returns the type of this traffic point.
     *
     * @return type of traffic point
     */
    public TrafficPointType type() {
        return this.tpType;
    }

    /**
     * Returns the device ID of this traffic point.
     *
     * @return device ID of traffic point
     */
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the list of port IDs of this traffic point.
     *
     * @return list of port IDs of traffic point
     */
    public List<PortNumber> portIds() {
        return this.portIds;
    }

    /**
     * Compares two traffic points.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof TrafficPoint) {
            TrafficPoint that = (TrafficPoint) obj;
            if (Objects.equals(this.tpType,   that.tpType) &&
                Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.portIds,  that.portIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tpType, deviceId, portIds);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("type",     tpType.toString())
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
     * @return traffic points builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Traffic points builder.
     */
    public static final class Builder {
        private TrafficPointType tpType;
        private DeviceId         deviceId;
        private List<PortNumber> portIds;

        private Builder() {
        }

        public TrafficPoint build() {
            return new TrafficPoint(
                tpType, deviceId, portIds
            );
        }

        /**
         * Returns traffic points builder with type.
         *
         * @param tpType traffic point type
         * @return traffic points builder
         */
        public Builder type(String tpType) {
            this.tpType = TrafficPointType.getByName(tpType);
            return this;
        }

        /**
         * Returns traffic points builder with device ID.
         *
         * @param deviceId traffic point device ID
         * @return traffic points builder
         */
        public Builder deviceId(String deviceId) {
            this.deviceId = DeviceId.deviceId(deviceId);
            return this;
        }

        /**
         * Returns traffic points builder with port IDs.
         *
         * @param portIds set of traffic point port IDs
         * @return traffic points builder
         */
        public Builder portIds(Set<Long> portIds) {
            checkNotNull(
                portIds,
                "Set of port IDs for traffic point is NULL"
            );

            this.portIds = new ArrayList<PortNumber>();
            for (Long portL : portIds) {
                long port = portL.longValue();
                checkArgument(
                    port >= 0,
                    "Port ID for traffic point is negative"
                );
                this.portIds.add(PortNumber.portNumber(port));
            }

            return this;
        }
    }

}
