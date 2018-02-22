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

package org.onosproject.mantis.api.metrics;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.time.Instant;

/**
 * Load metric for InfluxDB.
 * Mapped to database entries by InfluxDBResultMapper.
 */
@Measurement(name = "load")
public class LoadMetric {

    /**
     * The timestamp of the load metric.
     */
    @Column(name = "time")
    private Instant time;

    /**
     * The device on which the network load is observed.
     */
    @Column(name = "device_id", tag = true)
    private String deviceId;

    /**
     * The device's port on which the network load is observed.
     */
    @Column(name = "port_id", tag = true)
    private String portId;

    /**
     * The observed network load in bits.
     */
    @Column(name = "bits")
    private Long bits;

    /**
     * The observed network load in packets.
     */
    @Column(name = "packets")
    private Long packets;

    /**
     * The number of packet drops.
     */
    @Column(name = "drops")
    private Long drops;

    /**
     * Returns the timestamp of this load metric.
     *
     * @return timestamp
     */
    public Instant time() {
        return this.time;
    }

    /**
     * Returns the timestamp since epoch in seconds.
     *
     * @return timestamp since epoch in seconds
     */
    public Long timeEpochSec() {
        return this.time.getEpochSecond();
    }

    /**
     * Returns the timestamp since epoch in milliseconds.
     *
     * @return timestamp since epoch in milliseconds
     */
    public Long timeEpochMilli() {
        return this.time.toEpochMilli();
    }

    /**
     * Returns the device ID of this load metric.
     *
     * @return device ID
     */
    public String deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the port ID of this load metric.
     *
     * @return port ID
     */
    public String portId() {
        return this.portId;
    }

    /**
     * Returns the number of bits of this load metric.
     *
     * @return number of bits
     */
    public Long bits() {
        return this.bits;
    }

    /**
     * Returns the number of packets of this load metric.
     *
     * @return number of packets
     */
    public Long packets() {
        return this.packets;
    }

    /**
     * Returns the number of dropped packets of this load metric.
     *
     * @return number of dropped packets
     */
    public Long drops() {
        return this.drops;
    }

    /**
     * Sets the timestamp of this load metric.
     *
     * @param time time of this load metric
     */
    public void setTime(Instant time) {
        this.time = time;
    }

    /**
     * Sets the device ID of this load metric.
     *
     * @param deviceId the device ID of this load metric
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Sets the port ID of this load metric.
     *
     * @param portId the port ID of this load metric
     */
    public void setPortId(String portId) {
        this.portId = portId;
    }

    /**
     * Sets the number of bits of this load metric.
     *
     * @param bits the number of bits of this load metric
     */
    public void setBits(Long bits) {
        this.bits = bits;
    }

    /**
     * Sets the number of packets of this load metric.
     *
     * @param packets the number of packets of this load metric
     */
    public void setPackets(Long packets) {
        this.packets = packets;
    }

    /**
     * Sets the number of dropped packets of this load metric.
     *
     * @param drops the number of drops of this load metric
     */
    public void setDrops(Long drops) {
        this.drops = drops;
    }

    /**
     * Compares two load metris.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LoadMetric) {
            LoadMetric that = (LoadMetric) obj;
            if (Objects.equals(this.bits,    that.bits) &&
                Objects.equals(this.packets, that.packets) &&
                Objects.equals(this.drops,   that.drops)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, deviceId, portId, bits, packets, drops);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("time",     time.toString())
            .add("deviceId", deviceId)
            .add("portId",   portId)
            .add("bits",     bits.toString())
            .add("packets",  packets.toString())
            .add("drops",    drops.toString())
            .toString();
    }

}
