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

import org.onosproject.mantis.api.metrics.LoadMetric;
import org.onosproject.mantis.api.prediction.BasePredictorInterface;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of a base predictor.
 */
public abstract class BasePredictor implements BasePredictorInterface {

    protected List<LoadMetric> inputTimeseries;
    protected long dataWindowSize;

    public BasePredictor(
            List<LoadMetric> inputTimeseries,
            long dataWindowSize) {
        checkNotNull(
            inputTimeseries,
            "Input timeseries is NULL"
        );
        checkArgument(
            dataWindowSize > 0,
            "Data window size (in number of data points) must be positive"
        );

        this.inputTimeseries = inputTimeseries;
        this.dataWindowSize  = dataWindowSize;
    }

    @Override
    public List<LoadMetric> inputTimeseries() {
        return this.inputTimeseries;
    }

    @Override
    public long dataWindowSize() {
        return this.dataWindowSize;
    }

    @Override
    public abstract List<LoadMetric> predict();

    /**
     * A Predictor builder.
     */
    public abstract static class Builder
            implements BasePredictorInterface.Builder {

        protected List<LoadMetric> inputTimeseries;
        protected long dataWindowSize;

        public Builder() {
        }

        @Override
        public BasePredictorInterface.Builder inputTimeseries(
                List<LoadMetric> inputTimeseries) {
            this.inputTimeseries = inputTimeseries;
            return this;
        }

        @Override
        public BasePredictorInterface.Builder dataWindowSize(
                long dataWindowSize) {
            this.dataWindowSize = dataWindowSize;
            return this;
        }

        @Override
        public abstract BasePredictorInterface build();

    }

}
