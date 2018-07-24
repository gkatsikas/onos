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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration necessary for Network Monitor.
 */
public final class NetworkMonitorConfiguration {

    private NetworkMonitorConfigurationId    id;
    private DatabaseConfiguration    databaseConfiguration;
    private List<IngressPoint>       ingressPoints;
    private PredictionConfiguration  predictionConfiguration;
    private NetworkMonitorConfigurationState state;

    public NetworkMonitorConfiguration(
            NetworkMonitorConfigurationId    id,
            DatabaseConfiguration            databaseConfiguration,
            List<IngressPoint>               ingressPoints,
            PredictionConfiguration          predictionConfiguration,
            NetworkMonitorConfigurationState state) {
        checkNotNull(
            id,
            "Configuration ID is NULL"
        );
        checkNotNull(
            databaseConfiguration,
            "Database configuration is NULL"
        );
        checkNotNull(
            ingressPoints,
            "Set of ingress points is NULL"
        );
        checkNotNull(
            predictionConfiguration,
            "Prediction configuration is NULL"
        );
        checkNotNull(
            state,
            "Configuration state is NULL"
        );

        this.id                      = id;
        this.databaseConfiguration   = databaseConfiguration;
        this.ingressPoints           = ingressPoints;
        this.predictionConfiguration = predictionConfiguration;
        this.state                   = state;
    }

    /**
     * Returns the ID of this configuration.
     *
     * @return configuration ID
     */
    public NetworkMonitorConfigurationId id() {
        return this.id;
    }

    /**
     * Returns the database configuration.
     *
     * @return database configuration
     */
    public DatabaseConfiguration databaseConfiguration() {
        return this.databaseConfiguration;
    }

    /**
     * Returns the set of ingress points.
     *
     * @return set of ingress points
     */
    public Set<IngressPoint> ingressPoints() {
        return ImmutableSet.copyOf(this.ingressPoints);
    }

    /**
     * Adds the input set of ingress points
     * to the current set.
     *
     * @param toAdd set of ingress points to add
     */
    public void addIngressPoints(Set<IngressPoint> toAdd) {
        checkNotNull(
            toAdd,
            "Cannot add NULL set of ingress points"
        );
        this.ingressPoints.addAll(new ArrayList<IngressPoint>(toAdd));
    }

    /**
     * Removes the input set of ingress points
     * from the current set.
     *
     * @param toRemove set of ingress points to remove
     */
    public void removeIngressPoints(Set<IngressPoint> toRemove) {
        checkNotNull(
            toRemove,
            "Cannot remove NULL set of ingress points"
        );
        this.ingressPoints.removeAll(new ArrayList<IngressPoint>(toRemove));
    }

    /**
     * Returns the prediction configuration.
     *
     * @return prediction configuration
     */
    public PredictionConfiguration predictionConfiguration() {
        return this.predictionConfiguration;
    }

    /**
     * Returns the state of this configuration.
     *
     * @return configuration state
     */
    public NetworkMonitorConfigurationState state() {
        return this.state;
    }

    /**
     * Sets the state of this configuration.
     *
     * @param newState new configuration state
     */
    public void setState(NetworkMonitorConfigurationState newState) {
        checkNotNull(
            newState,
            "Cannot set configuration state to NULL"
        );
        this.state = newState;
    }

    /**
     * Compares two Network Monitor configurations.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NetworkMonitorConfiguration) {
            NetworkMonitorConfiguration that = (NetworkMonitorConfiguration) obj;
            if (Objects.equals(this.databaseConfiguration, that.databaseConfiguration) &&
                Objects.equals(this.ingressPoints, that.ingressPoints) &&
                Objects.equals(this.predictionConfiguration, that.predictionConfiguration) &&
                Objects.equals(this.state, that.state)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, state);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("id", id.toString())
            .add("databaseConfiguration", databaseConfiguration.toString())
            .add("ingressPoints", ingressPoints.toString())
            .add("predictionConfiguration", predictionConfiguration.toString())
            .add("state", state.toString())
            .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return Network Monitor configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Network Monitor configuration builder.
     */
    public static final class Builder {

        private NetworkMonitorConfigurationId    id = NetworkMonitorConfigurationId.id("default");
        private DatabaseConfiguration            databaseConfiguration;
        private List<IngressPoint>               ingressPoints;
        private PredictionConfiguration          predictionConfiguration;
        private NetworkMonitorConfigurationState state = NetworkMonitorConfigurationState.INIT;

        private Builder() {
        }

        public NetworkMonitorConfiguration build() {
            return new NetworkMonitorConfiguration(
                id, databaseConfiguration, ingressPoints,
                predictionConfiguration, state
            );
        }

        /**
         * Returns configuration builder with ID.
         *
         * @param id configuration ID as a URI
         * @return Network Monitor configuration builder
         */
        public Builder id(URI id) {
            this.id = NetworkMonitorConfigurationId.id(id);
            return this;
        }

        /**
         * Returns configuration builder with database configuration.
         *
         * @param databaseConfiguration database configuration
         * @return Network Monitor configuration builder
         */
        public Builder databaseConfiguration(
                DatabaseConfiguration databaseConfiguration) {
            this.databaseConfiguration = databaseConfiguration;
            return this;
        }

        /**
         * Returns configuration builder with ingress points.
         *
         * @param ingressPoints set of ingress points
         * @return Network Monitor configuration builder
         */
        public Builder ingressPoints(Set<IngressPoint> ingressPoints) {
            this.ingressPoints = new ArrayList<IngressPoint>(ingressPoints);
            return this;
        }

        /**
         * Returns configuration builder with prediction configuration.
         *
         * @param predictionConfiguration prediction configuration
         * @return Network Monitor configuration builder
         */
        public Builder predictionConfiguration(
                PredictionConfiguration predictionConfiguration) {
            this.predictionConfiguration = predictionConfiguration;
            return this;
        }

        /**
         * Returns configuration builder with state.
         *
         * @param state configuration state
         * @return Network Monitor configuration builder
         */
        public Builder state(NetworkMonitorConfigurationState state) {
            this.state = state;
            return this;
        }
    }

}
