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

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for the SMA prediction mechanism.
 */
public class SmaPredictorTest {

    /**
     * Buffers to host input and test data.
     */
    private static long[] inputLoadPacketsZeros;
    private static long[] inputLoadPackets;
    private static long[] desiredLoadPackets;
    private static long[] desiredLoadBits;

    private static List<LoadMetric> timeseriesInputZeros;
    private static List<LoadMetric> timeseriesInput;
    private static List<LoadMetric> timeseriesOutput;
    private static List<LoadMetric> timeseriesDesiredOutput;

    private static BasePredictorInterface predictedInputZeros;
    private static BasePredictorInterface predictedInput;

    // We assume a fixed window size
    private static final long WINDOW_SIZE = 5;

    @Before
    public void setUp() throws Exception {
        // Emulate what can be retrieved by the DB
        inputLoadPacketsZeros = new long[20];
        for (long v : inputLoadPacketsZeros) {
            assertTrue(v == 0);
        }

        inputLoadPackets = new long[] {
            11, 14, 15, 2, 10,
            14, 6, 19, 14, 13,
            5, 17, 18, 3, 7,
            19, 17, 2, 13, 19
        };

        desiredLoadPackets = new long[] {
            11, 11, 10, 11, 13,
            14, 12, 14, 14, 12,
            10, 13, 13, 10, 12,
            14
        };

        desiredLoadBits = new long[] {
            666, 704, 602, 653, 807,
            845, 730, 871, 858, 717,
            640, 820, 820, 615, 743,
            896
        };
    }

    /**
     * Checks whether SMA produces the intended results,
     * when applied to an input array full of zeros.
     *
     * @throws Exception if the output is not the intended
     */
    @Test
    public void testSmaZeros() throws Exception {
        assertThat(inputLoadPacketsZeros, notNullValue());

        // Create the timeseries
        timeseriesInputZeros = PredictorTestsCommons.constructTimeseries(inputLoadPacketsZeros);
        assertThat(timeseriesInputZeros, notNullValue());
        assertTrue(timeseriesInputZeros.size() == inputLoadPacketsZeros.length);
        assertTrue(PredictorTestsCommons.verifyZeros(timeseriesInputZeros));
        int inputTimeseriesSize = timeseriesInputZeros.size();

        // Build the predictor using this timeseries
        SmaPredictor.Builder smaBuilder =
            SmaPredictor.builder()
                .inputTimeseries(timeseriesInputZeros)
                .dataWindowSize(WINDOW_SIZE);

        predictedInputZeros = smaBuilder.build();
        assertThat(predictedInputZeros, notNullValue());

        // Do the prediction
        timeseriesOutput = predictedInputZeros.predict();
        assertThat(timeseriesOutput, notNullValue());
        assertTrue(PredictorTestsCommons.verifyZeros(timeseriesOutput));

        /**
         * The length of the output list falls behind
         * the input one by WINDOW_SIZE entries.
         */
        assertTrue(
            (timeseriesOutput.size() + WINDOW_SIZE - 1) == inputTimeseriesSize
        );
    }

    /**
     * Checks whether SMA produces the intended results,
     * when applied to an input array.
     *
     * @throws Exception if the output is not the intended
     */
    @Test
    public void testSma() throws Exception {
        assertThat(inputLoadPackets, notNullValue());

        // Create the timeseries
        timeseriesInput = PredictorTestsCommons.constructTimeseries(inputLoadPackets);
        assertThat(timeseriesInput, notNullValue());
        assertFalse(PredictorTestsCommons.verifyZeros(timeseriesInput));

        // Build the predictor using this timeseries
        SmaPredictor.Builder smaBuilder =
            SmaPredictor.builder()
                .inputTimeseries(timeseriesInput)
                .dataWindowSize(WINDOW_SIZE);

        predictedInput = smaBuilder.build();
        assertThat(predictedInput, notNullValue());

        // Do the prediction
        timeseriesOutput = predictedInput.predict();
        assertThat(timeseriesOutput, notNullValue());
        assertFalse(PredictorTestsCommons.verifyZeros(timeseriesOutput));

        // Find the list of load metrics of the desired output
        timeseriesDesiredOutput = PredictorTestsCommons.constructTimeseries(desiredLoadPackets, desiredLoadBits);
        assertThat(timeseriesDesiredOutput, notNullValue());
        assertFalse(PredictorTestsCommons.verifyZeros(timeseriesDesiredOutput));

        // Check that desired and actual output match
        assertEquals(timeseriesDesiredOutput, timeseriesOutput);
    }

}
