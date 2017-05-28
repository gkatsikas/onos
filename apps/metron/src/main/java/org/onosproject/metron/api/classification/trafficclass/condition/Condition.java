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

package org.onosproject.metron.api.classification.trafficclass.condition;

import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.operation.FieldOperation;

import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;

import org.slf4j.Logger;
import org.apache.commons.lang.ArrayUtils;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A condition on a header field.
 */
public class Condition {

    private static final Logger log = getLogger(Condition.class);

    private HeaderField       headerField;
    private Filter            filter;
    private FieldOperation    operation;
    private NfvDataplaneBlockInterface block;

    public Condition(
            HeaderField       headerField,
            Filter            filter,
            FieldOperation    operation,
            NfvDataplaneBlockInterface block) {
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        checkNotNull(
            filter,
            "NULL filter is associated with a condition"
        );

        checkNotNull(
            operation,
            "NULL operation is associated with a condition"
        );

        checkNotNull(
            block,
            "NULL NFV dataplane block is associated with a condition"
        );

        this.headerField = headerField;
        this.filter = filter;
        this.operation = operation;
        this.block = block;
    }

    /**
     * Returns the header field associated with this condition.
     *
     * @return HeaderField associated with this condition
     */
    public HeaderField headerField() {
        return this.headerField;
    }

    /**
     * Returns the filter associated with this condition.
     *
     * @return Filter associated with this condition
     */
    public Filter filter() {
        return this.filter;
    }

    /**
     * Returns the header field operation associated with this condition.
     *
     * @return FieldOperation associated with this condition
     */
    public FieldOperation operation() {
        return this.operation;
    }

    /**
     * Returns the dataplane block operation associated with this condition.
     *
     * @return NfvDataplaneBlockInterface associated with this condition
     */
    public NfvDataplaneBlockInterface block() {
        return this.block;
    }

    /**
     * Checks whether the filter of this condition is empty or not.
     *
     * @return boolean status of the filter
     */
    public boolean isNone() {
        return this.filter.isNone();
    }

    /**
     * Intersects a new filter with the filter of this condition.
     *
     * @param newFilter to be intersected with this condition
     * @return boolean intersection status
     */
    public boolean intersect(Filter newFilter) {
        this.filter = this.filter.intersect(newFilter);
        return this.isNone();
    }

    /**
     * Checks whether the input operation is the same with this one.
     *
     * @param otherOperation operation to be compared against this one
     * @return boolean similarity status
     */
    public boolean isSameOperation(FieldOperation otherOperation) {
        return this.operation == otherOperation;
    }

    @Override
    public String toString() {
        String result = "Condition on " + this.headerField.toString() +
                        ": " + this.filter.toString();
        return result;
    }

}
