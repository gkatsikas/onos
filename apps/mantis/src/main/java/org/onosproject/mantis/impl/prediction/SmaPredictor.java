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

package org.onosproject.mantis.impl.prediction;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import org.onosproject.mantis.api.metrics.LoadMetric;
import org.onosproject.mantis.api.prediction.BasePredictorInterface;

import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a simple moving average (SMA) predictor.
 */
public final class SmaPredictor extends BasePredictor {

    private static final Logger log = getLogger(
        SmaPredictor.class
    );

    /**
     * Print ID.
     */
    private static final String COMPONET_LABEL = " SMA Predictor";

    public SmaPredictor(
            List<LoadMetric> inputTimeseries,
            long dataWindowSize) {
        super(inputTimeseries, dataWindowSize);
    }

    @Override
    public List<LoadMetric> predict() {
        List<LoadMetric> loadList = this.inputTimeseries();
        if (loadList == null) {
            return null;
        }

        List<LoadMetric> result = new ArrayList<LoadMetric>();

        /**
         * The objective is to maintain a "rolling mean" of the most recent values.
         * First we need to create a DescriptiveStats instance and then set the
         * window size equal to dataWindowSize.
         */
        DescriptiveStatistics bitStats = new DescriptiveStatistics((int) dataWindowSize);
        DescriptiveStatistics packetStats = new DescriptiveStatistics((int) dataWindowSize);
        DescriptiveStatistics dropStats = new DescriptiveStatistics((int) dataWindowSize);

        int counter = 0;
        LoadMetric load;
        while (counter < loadList.size()) {
            load = loadList.get(counter);

            bitStats.addValue(load.bits().doubleValue()); // Long -> double
            packetStats.addValue(load.packets().doubleValue());
            dropStats.addValue(load.drops().doubleValue());

            /**
             * When counter = dataWindowSize-1, we have reached the last
             * element of this application window.
             * Start SMA calculation from now on.
             */
            if (counter >= (dataWindowSize - 1)) {
                load.setTime(Instant.now());

                load.setBits(Long.valueOf((long) Math.ceil(bitStats.getMean())));  // double -> Long
                load.setPackets(Long.valueOf((long) Math.ceil(packetStats.getMean())));
                load.setDrops(Long.valueOf((long) Math.ceil(dropStats.getMean())));

                result.add(load);
            }

            counter++;
        }

        return result;
    }

    /**
     * Returns a new builder instance.
     *
     * @return SMA predictor builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * An SMA predictor builder.
     */
    public static final class Builder extends BasePredictor.Builder {
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

        @Override
        public BasePredictorInterface build() {
            return new SmaPredictor(
                inputTimeseries, dataWindowSize
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
