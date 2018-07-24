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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of prediction mechanism.
 */
public class PredictionMechanism {

    /**
     * Supported prediction types.
     */
    public enum PredictionMethod {
        SMA("SMA"),
        SMA_INFLUXDB("SMA_INFLUXDB"),
        EWMA("EWMA"),
        HOLT_WINTERS_INFLUXDB("HOLT_WINTERS_INFLUXDB"),
        ARIMA("ARIMA"),
        WAVELET("WAVELET"),
        UNDEFINED("UNDEFINED");

        private String methodStr;

        /**
         * Statically maps prediction methods to enum types.
         */
        private static final Map<String, PredictionMethod> MAP =
            new ConcurrentHashMap<String, PredictionMethod>();

        static {
            for (PredictionMethod method : PredictionMethod.values()) {
                MAP.put(method.toString(), method);
            }
        }

        private PredictionMethod(String methodStr) {
            checkArgument(
                !Strings.isNullOrEmpty(methodStr),
                "Prediction method is NULL or empty"
            );
            this.methodStr = methodStr;
        }

        /**
         * Returns a Prediction method derived by a name.
         *
         * @param methodStr prediction method's name
         * @return a PredictionMethod object or null
         */
        public static PredictionMethod getByName(String methodStr) {
            checkArgument(
                !Strings.isNullOrEmpty(methodStr),
                "Prediction method is NULL or empty"
            );
            return MAP.get(methodStr.toUpperCase());
        }

        /**
         * Returns whether the input prediction method
         * is an InfluxDB built-in method.
         *
         * @param method prediction method's name
         * @return boolean answer on whether the method is built-in or not
         */
        public static boolean isBuiltIn(PredictionMethod method) {
            checkNotNull(method, "Prediction method is NULL");
            return method == SMA_INFLUXDB ||
                   method == HOLT_WINTERS_INFLUXDB;
        }

        /**
         * Returns whether the input prediction method
         * is SMA or not.
         *
         * @param method prediction method's name
         * @return boolean answer on whether the method is SMA or not
         */
        public static boolean isSma(PredictionMethod method) {
            checkNotNull(method, "Prediction method is NULL");
            return method == SMA;
        }

        /**
         * Returns whether the input prediction method
         * is InfluxDB's built-in SMA or not.
         *
         * @param method prediction method's name
         * @return boolean answer on whether the method is
         *         InfluxDB's built-in SMA or not
         */
        public static boolean isSmaInfluxDb(PredictionMethod method) {
            checkNotNull(method, "Prediction method is NULL");
            return method == SMA_INFLUXDB;
        }

        /**
         * Returns whether the input prediction method
         * is EWMA or not.
         *
         * @param method prediction method's name
         * @return boolean answer on whether the method is EWMA or not
         */
        public static boolean isEwma(PredictionMethod method) {
            checkNotNull(method, "Prediction method is NULL");
            return method == EWMA;
        }

        /**
         * Returns whether the input prediction method
         * is InfluxDB's built-in Holt-Winters or not.
         *
         * @param method prediction method's name
         * @return boolean answer on whether the method is
         *         InfluxDB's built-in Holt-Winters or not
         */
        public static boolean isHoltWintersInfluxDb(PredictionMethod method) {
            checkNotNull(method, "Prediction method is NULL");
            return method == HOLT_WINTERS_INFLUXDB;
        }

        @Override
        public String toString() {
            return this.methodStr;
        }
    }

    /**
     * The method of this prediction mechanism.
     */
    private PredictionMethod method;

    /**
     * Time window of the prediction mechanism in seconds.
     * This tells how far (in seconds) from now we will
     * fetch data points from the database.
     */
    private long timeWindowSize;

    /**
     * The window size of this prediction mechanism
     * in terms of number of data points.
     * This is how many data points the mechanism considers,
     * while attempting to predict the next value.
     */
    private long dataWindowSize;

    /**
     * Portion of the data to be used for training.
     * Floating point number in (0, 1].
     */
    private float trainingFraction;

    /**
     * Maps custom parameter names to their values.
     * These parameters are method-specific.
     */
    private Map<String, Object> parameters;

    /**
     * Bounds for training fraction.
     */
    private static final float TRAINING_FRACTION_MIN = (float) 0.0;
    private static final float TRAINING_FRACTION_MAX = (float) 1.0;
    private static final float TRAINING_FRACTION_DEF = (float) 0.7;

    /**
     * A default vlaue for the seasonal pattern in seconds.
     */
    public static final String HOLT_WINTERS_SEASONAL_PARAM_SEC = "seasonalPatternSec";
    public static final float DEF_HOLT_WINTERS_SEASONAL_PARAM_SEC = (float) 0.0;

    public PredictionMechanism(
            String method,
            long   timeWindowSize,
            long   dataWindowSize,
            float  trainingFraction,
            Map<String, Object> parameters) {
        checkArgument(
            !Strings.isNullOrEmpty(method),
            "Prediction method is NULL or empty"
        );
        checkArgument(
            timeWindowSize > 0,
            "The time window size (in seconds) for prediction must be positive"
        );
        checkArgument(
            dataWindowSize > 0,
            "Data window size (in number of data points) must be positive"
        );
        checkArgument(
            (trainingFraction  > TRAINING_FRACTION_MIN) &&
            (trainingFraction <= TRAINING_FRACTION_MAX),
            "Training fraction must be in (" + TRAINING_FRACTION_MIN +
            ", " + TRAINING_FRACTION_MAX + "]. " +
            "Received: " + trainingFraction
        );
        checkNotNull(parameters, "Custom configuration map is NULL");

        this.method           = PredictionMethod.getByName(method);
        checkNotNull(this.method, "Prediction method is NULL");

        this.timeWindowSize   = timeWindowSize;
        this.dataWindowSize   = dataWindowSize;
        this.trainingFraction = trainingFraction;
        this.parameters       = parameters;
    }

    /**
     * Returns the method of this prediction mechanism.
     *
     * @return prediction method
     */
    public PredictionMethod method() {
        return this.method;
    }

    /**
     * Returns the time window size of the prediction in seconds.
     *
     * @return time window size in seconds
     */
    public long timeWindowSize() {
        return this.timeWindowSize;
    }

    /**
     * Returns the window size (number of data points) of this
     * prediction mechanism.
     *
     * @return data window size in number of data points
     */
    public long dataWindowSize() {
        return this.dataWindowSize;
    }

    /**
     * Returns the training fraction.
     *
     * @return training fraction in (0, 1)
     */
    public float trainingFraction() {
        return this.trainingFraction;
    }

    /**
     * Returns the custom configuration parameters' map.
     *
     * @return map of custom parameters to their values
     */
    public Map<String, Object> parameters() {
        return this.parameters;
    }

    /**
     * Returns the value of a specific custom configuration parameter.
     *
     * @param parameter custom parameter
     * @return the value of this parameter as a Java object or null
     */
    public Object parameter(String parameter) {
        return this.parameters.get(parameter);
    }

    /**
     * Compares two prediction mechanisms.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PredictionMechanism) {
            PredictionMechanism that = (PredictionMechanism) obj;
            if (Objects.equals(this.method,           that.method) &&
                Objects.equals(this.timeWindowSize,   that.timeWindowSize) &&
                Objects.equals(this.dataWindowSize,   that.dataWindowSize) &&
                Objects.equals(this.trainingFraction, that.trainingFraction) &&
                Objects.equals(this.parameters,       that.parameters)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            method, timeWindowSize, dataWindowSize, trainingFraction, parameters
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("method",           method.toString())
            .add("timeWindowSize",   String.valueOf(timeWindowSize))
            .add("dataWindowSize",   String.valueOf(dataWindowSize))
            .add("trainingFraction", Float.toString(trainingFraction))
            .add("parameters",       parameters.toString())
            .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return prediction mechanism builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Prediction mechanism builder.
     */
    public static final class Builder {

        private String method;
        private long   timeWindowSize;
        private long   dataWindowSize;
        private float  trainingFraction = TRAINING_FRACTION_DEF;
        private Map<String, Object> parameters =
            new ConcurrentHashMap<String, Object>();

        private Builder() {
        }

        public PredictionMechanism build() {
            return new PredictionMechanism(
                method, timeWindowSize,
                dataWindowSize, trainingFraction,
                parameters
            );
        }

        /**
         * Returns prediction mechanism builder with method.
         *
         * @param method prediction mechanism's method as a string
         * @return prediction mechanism builder
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Returns prediction mechanism builder with time window size.
         *
         * @param timeWindowSize prediction mechanism's time window size
         * @return prediction mechanism builder
         */
        public Builder timeWindowSize(long timeWindowSize) {
            this.timeWindowSize = timeWindowSize;
            return this;
        }

        /**
         * Returns prediction mechanism builder with data window size.
         *
         * @param dataWindowSize prediction mechanism's data window size
         * @return prediction mechanism builder
         */
        public Builder dataWindowSize(long dataWindowSize) {
            this.dataWindowSize = dataWindowSize;
            return this;
        }

        /**
         * Returns prediction mechanism builder with training fraction.
         *
         * @param trainingFraction prediction mechanism's training fraction
         * @return prediction mechanism builder
         */
        public Builder trainingFraction(float trainingFraction) {
            this.trainingFraction = trainingFraction;
            return this;
        }

        /**
         * Returns prediction mechanism builder with custom parameters.
         *
         * @param parameters prediction mechanism's custom parameters
         * @return prediction mechanism builder
         */
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

    }

}
