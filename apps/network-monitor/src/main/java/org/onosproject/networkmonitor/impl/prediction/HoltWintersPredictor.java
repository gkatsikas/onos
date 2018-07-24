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

import java.util.List;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a Holt-Winters predictor.
 */
public final class HoltWintersPredictor extends BasePredictor {

    private static final Logger log = getLogger(
        HoltWintersPredictor.class
    );

    /**
     * Print ID.
     */
    private static final String COMPONET_LABEL = " Holt-Winters Predictor";

    /**
     * The seasonal pattern in seconds for Holt-Winters.
     */
    private int seasonalPatternSec;

    /**
     * Bounds for the seasonal pattern.
     */
    private static final int SEASONAL_PARAM_SEC_MIN = 0;

    /**
     * A label for the seasonal pattern in seconds.
     */
    public static final String HOLT_WINTERS_SEASONAL_PARAM_SEC = "seasonalPatternSec";

    /**
     * A default seasonal pattern in seconds.
     */
    public static final int DEF_HOLT_WINTERS_SEASONAL_PARAM_SEC = 0;

    public HoltWintersPredictor(
            List<LoadMetric> inputTimeseries,
            long dataWindowSize,
            int seasonalPatternSec) {
        super(inputTimeseries, dataWindowSize);

        checkArgument(
            (seasonalPatternSec >= SEASONAL_PARAM_SEC_MIN),
            "Seasonal pattern for Holt-Winters must be greater than " +
            SEASONAL_PARAM_SEC_MIN
        );

        this.seasonalPatternSec = seasonalPatternSec;
    }

    /**
     * Returns the seasonal pattern in seconds.
     *
     * @return seasonal pattern in seconds
     */
    public int seasonalPatternSec() {
        return this.seasonalPatternSec;
    }

    @Override
    public List<LoadMetric> predict() {
        // This method is built-in InfluxDB.
        return new ArrayList<LoadMetric>();
    }

    /**
     * Returns a new builder instance.
     *
     * @return Holt-Winters predictor builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A Holt-Winters predictor builder.
     */
    public static final class Builder extends BasePredictor.Builder {
        private int seasonalPatternSec = DEF_HOLT_WINTERS_SEASONAL_PARAM_SEC;

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
         * Sets the seasonal pattern in seconds.
         *
         * @param seasonalPatternSec seasonal pattern in seconds
         * @return Holt-Winters predictor builder
         */
        public Builder seasonalPatternSec(int seasonalPatternSec) {
            this.seasonalPatternSec = seasonalPatternSec;
            return this;
        }

        @Override
        public BasePredictorInterface build() {
            return new HoltWintersPredictor(
                inputTimeseries, dataWindowSize, seasonalPatternSec
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
