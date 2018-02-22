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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ArrayList;

/**
 * Library with common utilities used by prediction tests.
 */
public class PredictorTestsCommons {

    // We assume a static packet size in bytes
    public static final long PACKET_SIZE = 64;

    /**
     * Utility classes do not require a public construcotr.
     */
    protected PredictorTestsCommons() {
    }

    /**
     * Creates a list of LoadMetrics out of an input array.
     *
     * @param inputLoadPackets array of integers that emulate input load
     * @return a list of LoadMetric objects
     */
    public static List<LoadMetric> constructTimeseries(long[] inputLoadPackets) {
        List<LoadMetric> timeseries = new ArrayList<LoadMetric>();

        for (long v : inputLoadPackets) {
            LoadMetric metric = new LoadMetric();
            metric.setPackets(Long.valueOf(v));
            metric.setBits(Long.valueOf(v * PACKET_SIZE));
            metric.setDrops(Long.valueOf(0));

            timeseries.add(metric);
        }

        return timeseries;
    }

    /**
     * Creates a list of LoadMetrics out of an input array.
     *
     * @param inputLoadPackets array that represents number of input packets
     * @param inputLoadBits array that represents the number of input bits
     * @return a list of LoadMetric objects
     */
    public static List<LoadMetric> constructTimeseries(
            long[] inputLoadPackets,
            long[] inputLoadBits) {

        // Two arrays must have same size:
        assertEquals(inputLoadPackets.length, inputLoadBits.length);

        List<LoadMetric> timeseries = new ArrayList<LoadMetric>();

        for (int i = 0; i < inputLoadPackets.length; i++) {
            LoadMetric metric = new LoadMetric();
            metric.setPackets(Long.valueOf(inputLoadPackets[i]));
            metric.setBits(Long.valueOf(inputLoadBits[i]));
            metric.setDrops(Long.valueOf(0));

            timeseries.add(metric);
        }

        return timeseries;
    }

    /**
     * Verifies that an array of load values comprises of zeros.
     *
     * @param timeseries array of integers that emulate input load
     * @return boolean status (all zeros or not)
     */
    public static boolean verifyZeros(long[] timeseries) {
        for (long v : timeseries) {
            if (v != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifies that a list of LoadMetrics comprises of objects
     * with zero packet, bit, and drop counters.
     *
     * @param timeseries array of integers that emulate input load
     * @return boolean status (all zeros or not)
     */
    public static boolean verifyZeros(List<LoadMetric> timeseries) {
        for (LoadMetric m : timeseries) {
            if (m.bits() != 0) {
                return false;
            }
            if (m.packets() != 0) {
                return false;
            }
            if (m.drops() != 0) {
                return false;
            }
        }

        return true;
    }

}
