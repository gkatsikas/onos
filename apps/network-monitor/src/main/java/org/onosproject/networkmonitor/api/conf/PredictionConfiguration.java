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

import org.onosproject.networkmonitor.api.prediction.PredictionMechanism;

import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration useful for calibrating a prediction mechanism.
 */
public final class PredictionConfiguration {

    private static final Logger log = getLogger(
        PredictionConfiguration.class
    );

    /**
     * Default prediction mechanisms.
     */
    private static final long  DEF_BUFFER_SIZE_SEC   = 60;
    private static final long  DEF_WINDOW_SIZE       = 10;
    private static final float DEF_TRAINING_FRACTION = (float) 1.0;

    /**
     * Set of prediction mechanisms to be used.
     */
    private List<PredictionMechanism> predictionMechanisms;

    public PredictionConfiguration(List<PredictionMechanism> predictionMechanisms) {
        checkArgument(
            (predictionMechanisms != null) && !predictionMechanisms.isEmpty(),
            "Set of prediction mechanisms is NULL or empty"
        );

        this.predictionMechanisms = predictionMechanisms;
    }

    /**
     * Returns the set of prediction mechanisms.
     *
     * @return set of prediction mechanisms
     */
    public Set<PredictionMechanism> predictionMechanisms() {
        return ImmutableSet.copyOf(this.predictionMechanisms);
    }

    /**
     * Compares two prediction configurations.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PredictionConfiguration) {
            PredictionConfiguration that = (PredictionConfiguration) obj;
            if (Objects.equals(this.predictionMechanisms, that.predictionMechanisms)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(predictionMechanisms);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("predictionMechanisms", predictionMechanisms.stream()
                .map(p -> p.toString())
                .collect(Collectors.joining(","))
            )
            .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return prediction configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Prediction configuration builder.
     */
    public static final class Builder {
        private List<PredictionMechanism> predictionMechanisms;

        private Builder() {
        }

        public PredictionConfiguration build() {
            return new PredictionConfiguration(predictionMechanisms);
        }

        /**
         * Returns prediction configuration builder with mechanisms.
         *
         * @param predictionMechanisms prediction configuration mechanisms
         * @return prediction configuration builder
         */
        public Builder predictionMechanisms(
                Set<PredictionMechanism> predictionMechanisms) {
            this.predictionMechanisms =
                new ArrayList<PredictionMechanism>(predictionMechanisms);
            return this;
        }

    }

}
