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

package org.onosproject.metron.api.classification.trafficclass.filter;

import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;

import org.onlab.packet.IPv4;

import org.apache.commons.lang.ArrayUtils;

import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A packet filter is a map of filters to
 * various header fields of a packet.
 */
public class PacketFilter extends ConcurrentHashMap<HeaderField, Filter> {

    private static final Logger log = getLogger(PacketFilter.class);

    public PacketFilter() {
        super();
    }

    public PacketFilter(PacketFilter other) {
        super(other);
    }

    public PacketFilter(Filter filter) {
        this();

        // Ensure a valid filter
        checkNotNull(
            filter,
            "Cannot construct packet filter using a NULL filter"
        );

        HeaderField headerField = filter.headerField();

        // Ensure a valid header field
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        this.put(headerField, new Filter(filter));
    }

    public PacketFilter(HeaderField headerField, Filter filter) {
        this();

        // Ensure a valid header field
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        // Ensure a valid filter
        checkNotNull(
            filter,
            "Cannot construct packet filter using a NULL filter"
        );

        this.put(headerField, new Filter(filter));
    }

    /**
     * Merges two packet filters.
     *
     * @param other packet filter to be merged with this one.
     */
    public void addPacketFilter(PacketFilter other) {
        for (Map.Entry<HeaderField, Filter> entry : other.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter filter = entry.getValue();

            if (filter.isNone()) {
                continue;
            }

            if (this.containsKey(headerField)) {
                Filter f = this.get(headerField);
                f.unite(filter);
            } else {
                this.put(headerField, filter);
            }
        }
    }

    /**
     * Checks whether one packet filter fully covers another packet filter.
     *
     * @param other packet filter to be checked against this one
     * @return boolean coverage status
     */
    public boolean covers(PacketFilter other) {
        for (Map.Entry<HeaderField, Filter> entry : other.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter otherFilter = entry.getValue();

            // Skip empty filters
            if ((otherFilter == null) || otherFilter.isNone()) {
                continue;
            }

            Filter thisFilter = this.get(headerField);

            if ((thisFilter == null) || thisFilter.isNone()) {
                return false;
            }

            /**
             * The filter of this traffic class does not cover the one of the other.
             * No point to continue.
             */
            if (!thisFilter.contains(otherFilter)) {
                return false;
            }
        }

        /**
         * After all the header fields have been exhausted, we know that
         * this traffic class covers the other traffic classes.
         */
        return true;
    }

    /**
     * Checks whether two packet filters have conflicting header fields or not.
     *
     * @param other packet filter to be checked against this one
     * @return boolean conflict
     */
    public boolean conflictsWith(PacketFilter other) {
        TrafficClassType thisType  = this.type();
        TrafficClassType otherType = other.type();

        // The type of each traffic class might already clarify if there is any intersection.
        if (
            ((thisType == otherType) && (thisType != TrafficClassType.NEUTRAL)) ||
            ((thisType == TrafficClassType.AMBIGUOUS) && (otherType == TrafficClassType.TCP
                || otherType == TrafficClassType.UDP)) ||
            ((otherType == TrafficClassType.AMBIGUOUS) && (thisType == TrafficClassType.TCP
                || thisType == TrafficClassType.UDP))) {
            return true;
        }

        for (Map.Entry<HeaderField, Filter> entry : other.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter otherFilter = entry.getValue();

            // Skip empty filters
            if ((otherFilter == null) || otherFilter.isNone()) {
                continue;
            }

            Filter thisFilter = this.get(headerField);

            /**
             * This traffic class has such header field, with a meaningful content,
             * that contains the other one.
             */
            if ((thisFilter != null) && !thisFilter.isNone() && !thisFilter.contains(otherFilter)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Identifies the 'post IP protocol' type of this packet filter,
     * by looking at the types of its individual header fields.
     *
     * @return TrafficClassType the type of this packet filter
     */
    public TrafficClassType type() {
        TrafficClassType type = null;

        for (Map.Entry<HeaderField, Filter> entry : this.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter filter = entry.getValue();

            // Skip empty filters
            if (filter.isNone()) {
                continue;
            }

            TrafficClassType newType = null;

            if        (this.hasIcmp(headerField)) {
                newType = TrafficClassType.ICMP;
            } else if (this.hasTcp(headerField)) {
                newType = TrafficClassType.TCP;
            } else if (this.hasUdp(headerField)) {
                newType = TrafficClassType.UDP;
            } else {
                newType = TrafficClassType.NEUTRAL;
            }

            if (type == null) {
                type = newType;
            } else {
                if ((type == TrafficClassType.NEUTRAL) && (type != newType)) {
                    type = newType;
                } else if ((type != newType) && (newType != TrafficClassType.NEUTRAL)) {
                    type = TrafficClassType.AMBIGUOUS;
                }
            }
        }

        return type;
    }

    /**
     * Checks whether a header field of this packet filter is TCP-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean TCP or not
     */
    public boolean hasTcp(HeaderField headerField) {
        if (HeaderField.isTcp(headerField)) {
            return true;
        }

        Filter f = this.get(headerField);

        if (headerField == HeaderField.IP_PROTO &&
           f.match(Long.parseLong(String.valueOf(IPv4.PROTOCOL_TCP)))) {
           return true;
        }

        return false;
    }

    /**
     * Checks whether a header field of this packet filter is UDP-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean UDP or not
     */
    public boolean hasUdp(HeaderField headerField) {
        if (HeaderField.isUdp(headerField)) {
            return true;
        }

        Filter f = this.get(headerField);

        if (headerField == HeaderField.IP_PROTO &&
           f.match(Long.parseLong(String.valueOf(IPv4.PROTOCOL_UDP)))) {
           return true;
        }

        return false;
    }

    /**
     * Checks whether a header field of this packet filter is ICMP-based or not.
     *
     * @param headerField the header field to be checked
     * @return boolean ICMP or not
     */
    public boolean hasIcmp(HeaderField headerField) {
        if (HeaderField.isIcmp(headerField)) {
            return true;
        }

        Filter f = this.get(headerField);

        if (headerField == HeaderField.IP_PROTO &&
           f.match(Long.parseLong(String.valueOf(IPv4.PROTOCOL_ICMP)))) {
           return true;
        }

        return false;
    }

    /**
     * Checks whether a header field of this packet filter depends
     * on any 'post IP' protocol or not.
     *
     * @param headerField the header field to be checked
     * @return boolean neutral or not
     */
    public boolean isNeutral(HeaderField headerField) {
        if (this.hasIcmp(headerField) || this.hasTcp(headerField) || this.hasUdp(headerField)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.keySet(), this.values()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof PacketFilter))) {
            return false;
        }

        PacketFilter other = (PacketFilter) obj;

        for (Filter ft : this.values()) {
            for (Filter fo : other.values()) {
                if (!ft.equals(fo)) {
                    return false;
                }
            }
        }

        for (Filter fo : this.values()) {
            for (Filter ft : other.values()) {
                if (!ft.equals(fo)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Converts a packet filter into a string format.
     *
     * @return the string format of the input packet filter
     */
    @Override
    public String toString() {
        String pfStr = "";

        for (Map.Entry<HeaderField, Filter> entry : this.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter filter = entry.getValue();

            pfStr += "[Header Field: " + headerField + ", " +
                     "Filter: " + filter.toString();
        }

        return pfStr;
    }

}
