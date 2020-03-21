/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.metron.api.classification.trafficclass.outputclass;

import org.onosproject.metron.api.structures.Pair;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;

import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for OutputClass.
 */
public class OutputClassTest {

    private final Logger log = getLogger(getClass());

    private OutputClass outClassA;
    private OutputClass outClassB;

    private Filter filter;
    private String pattern;

    /**
     * Checks whether the output class constructors work as expected.
     *
     * @throws Exception if any of the output classes is not built properly
     */
    @Test
    public void outputClassConstruction() throws IllegalArgumentException {
        pattern = "pattern - - 10.0.0.4 1024-10000 0 0";
        Pair<OutputClass, OutputClass> ocp = null;
        try {
            ocp = OutputClass.fromPattern(pattern.split(" "));
        } catch (Exception ex) {
            throw ex;
        }
        assertThat(ocp.getKey(), notNullValue());


        pattern = "pattern 192.168.23.45 67-590 103.50.60.48 34567-60000 0 1";
        try {
            ocp = OutputClass.fromPattern(pattern.split(" "));
        } catch (Exception ex) {
            throw ex;
        }
        assertThat(ocp.getKey(), notNullValue());


        filter = Filter.fromIpv4PrefixStr(HeaderField.IP_DST, "192.168.70.2/24");
        assertThat(filter, notNullValue());
        assertFalse(filter.filter().isEmpty());

        pattern = "204.56.78.98 0";
        OutputClass oc = null;
        try {
            oc = OutputClass.fromLookupRule(pattern, filter);
        } catch (Exception ex) {
            throw ex;
        }
        assertThat(oc, notNullValue());
    }

    /**
     * Checks whether the filter constructors assert
     * specific violations.
     *
     * @throws Exception if any of the violations is not detected
     */
    @Test
    public void invalidConstruction() throws Exception {
        // Filter is constructed on IP source
        filter = Filter.fromIpv4PrefixStr(HeaderField.IP_SRC, "192.168.70.2/24");
        assertThat(filter, notNullValue());
        assertFalse(filter.filter().isEmpty());

        /**
         * But output class will be constructed from destination IP,
         * using the filter as a higher priority rule.
         * This must fail because the filter has to be on the destination IP.
         */
        pattern = "204.56.78.98 0";
        OutputClass oc = null;
        try {
            oc = OutputClass.fromLookupRule(pattern, filter);
        } catch (Exception ex) {
            log.info("Successfuly detected invalid output class construction");
            return;
        }
        assertThat(oc, nullValue());

        throw new Exception("Failed to detect malformed output class");
    }

}
