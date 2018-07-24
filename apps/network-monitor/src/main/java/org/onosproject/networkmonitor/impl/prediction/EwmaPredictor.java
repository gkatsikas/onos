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

package org.onosproject.networkmonitor.impl.prediction;

import org.onosproject.networkmonitor.api.metrics.LoadMetric;
import org.onosproject.networkmonitor.api.prediction.BasePredictorInterface;

import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of an exponentially weighted moving average (EWMA) predictor.
 */
public final class EwmaPredictor extends BasePredictor {

    private static final Logger log = getLogger(
        EwmaPredictor.class
    );

    /**
     * Print ID.
     */
    private static final String COMPONET_LABEL = "EWMA Predictor";

    /**
     * The decay factor of EWMA.
     * doubleing point number in [0, 1].
     */
    private double alphaDecayFactor;

    /**
     * Bounds for alpha.
     */
    private static final double ALPHA_MIN = 0.0;
    private static final double ALPHA_MAX = 1.0;

    /**
     * A label for the decay factor 'alpha'.
     */
    public static final String EWMA_DECAY_PARAM_ALPHA = "alpha";

    /**
     * A default decay factor.
     */
    public static final double DEF_EWMA_DECAY_ALPHA = 0.4;

    public EwmaPredictor(
            List<LoadMetric> inputTimeseries,
            long  dataWindowSize,
            double alphaDecayFactor) {
        super(inputTimeseries, dataWindowSize);

        checkArgument(
            (alphaDecayFactor >= ALPHA_MIN) &&
            (alphaDecayFactor <= ALPHA_MAX),
            "EWMA decay factor (alpha) must be in [" + ALPHA_MIN +
            ", " + ALPHA_MAX + "]"
        );

        this.alphaDecayFactor = alphaDecayFactor;
    }

    /**
     * Returns the decay factor (alpha) of this EWMA predictor.
     *
     * @return decay factor (alpha)
     */
    public double alphaDecayFactor() {
        return this.alphaDecayFactor;
    }

    @Override
    public List<LoadMetric> predict() {
        List<LoadMetric> loadList = this.inputTimeseries();
        if (loadList == null) {
            return null;
        }

        List<LoadMetric> result = new ArrayList<LoadMetric>();

        double bitStats    = 0.0;
        double packetStats = 0.0;
        double dropStats   = 0.0;
        double  alpha = this.alphaDecayFactor();

        int counter = 0;
        LoadMetric load;
        while (counter < loadList.size()) {
            load = loadList.get(counter);

            if (counter == 0) {
                // weighted_average[0] = arg[0]
                bitStats    = load.bits().doubleValue();  // Long -> double
                packetStats = load.packets().doubleValue();
                dropStats   = load.drops().doubleValue();
            } else {
                // weighted_average[i] = (1-alpha) * weighted_average[i-1] + alpha * arg[i]
                bitStats    = ((1 - alpha) * bitStats)    + (alpha * load.bits().doubleValue());
                packetStats = ((1 - alpha) * packetStats) + (alpha * load.packets().doubleValue());
                dropStats   = ((1 - alpha) * dropStats)   + (alpha * load.drops().doubleValue());

                bitStats = Math.ceil(bitStats);
                packetStats = Math.ceil(packetStats);
                dropStats = Math.ceil(dropStats);
            }

            /**
             * When counter = dataWindowSize-1, we have reached the last
             * element of this application window.
             * Start EWMA calculation from now on.
             */
            if (counter >= (dataWindowSize - 1)) {
                load.setTime(Instant.now());
                load.setBits(Long.valueOf((long) bitStats));
                load.setPackets(Long.valueOf((long) packetStats));
                load.setDrops(Long.valueOf((long) dropStats));

                result.add(load);
            }
            counter++;
        }

        return result;
    }

    /**
     * Returns a new builder instance.
     *
     * @return EWMA predictor builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * An EWMA Predictor builder.
     */
    public static final class Builder extends BasePredictor.Builder {
        private double alphaDecayFactor = DEF_EWMA_DECAY_ALPHA;

        public Builder() {
            super();
        }

        @Override
        public Builder inputTimeseries(
                List<LoadMetric> inputTimeseries) {
            this.inputTimeseries = inputTimeseries;
            return this;
        }

        @Override
        public Builder dataWindowSize(
                long dataWindowSize) {
            this.dataWindowSize = dataWindowSize;
            return this;
        }

        /**
         * Sets the decay factor (alpha) of this EWMA predictor.
         *
         * @param alphaDecayFactor decay factor (alpha)
         * @return EWMA predictor builder
         */
        public Builder alphaDecayFactor(
                double alphaDecayFactor) {
            this.alphaDecayFactor = alphaDecayFactor;
            return this;
        }

        @Override
        public BasePredictorInterface build() {
            return new EwmaPredictor(
                inputTimeseries, dataWindowSize, alphaDecayFactor
            );
        }
    }

    /**
     * Returns a label with the monitor's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}
