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

package org.onosproject.networkmonitor.api.prediction;

import org.onosproject.networkmonitor.api.metrics.LoadMetric;

import java.util.List;

/**
 * A basic predictor's services.
 */
public interface BasePredictorInterface {

    /**
     * Returns the list of metrics that comprise
     * an input timeseries.
     *
     * @return timeseries as a list of LoadMetric objects
     */
    List<LoadMetric> inputTimeseries();

    /**
     * The window size of the prediction in terms of number
     * of data points.
     * This is how many data points the mechanism considers,
     * while attempting to predict the next value.
     *
     * @return data window size
     */
    long dataWindowSize();

    /**
     * Run the predictor.
     *
     * @return list of LoadMetric objects with predictions
     */
    abstract List<LoadMetric> predict();

    /**
     * A Predictor builder's interface.
     */
    interface Builder {

        /**
         * Sets the input timeseries of this predictor.
         *
         * @param inputTimeseries input timeseries
         * @return predictor builder
         */
        Builder inputTimeseries(List<LoadMetric> inputTimeseries);

        /**
         * Sets the data window of the prediction in
         * terms of number of data points.
         *
         * @param dataWindowSize data window of the
         *        prediction in number of data points
         * @return predictor builder
         */
        Builder dataWindowSize(long dataWindowSize);

        /**
         * Builds a basic predictor instance.
         *
         * @return basic predictor instance
         */
        abstract BasePredictorInterface build();

    }

}
