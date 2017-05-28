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

import org.apache.commons.lang.ArrayUtils;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A condition map maps header fields to lists of Conditions.
 */
public class ConditionMap extends ConcurrentHashMap<HeaderField, List<Condition>> {

    public ConditionMap() {
        super();
    }

    public ConditionMap(ConditionMap other) {
        super(other);
    }

    /**
     * Merges two condition maps.
     *
     * @param headerField a header field to build a condition map.
     * @param conditions list of conditions to build a condition map.
     */
    public ConditionMap(HeaderField headerField, List<Condition> conditions) {
        this();

        // Ensure a valid header field
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        // Ensure a valid list of conditions
        checkNotNull(
            conditions,
            "Cannot construct condition map using a NULL condition list"
        );

        this.put(headerField, new ArrayList<Condition>(conditions));
    }

    /**
     * Adds a new condition map into this map.
     *
     * @param other condition map to add
     */
    public void addConditionMap(ConditionMap other) {
        for (Map.Entry<HeaderField, List<Condition>> entry : other.entrySet()) {
            HeaderField headerField = entry.getKey();
            List<Condition> conditions = entry.getValue();

            if (conditions.size() == 0) {
                continue;
            }

            if (this.containsKey(headerField)) {
                List<Condition> c = this.get(headerField);
                c.removeAll(conditions);
                c.addAll(conditions);
            } else {
                this.put(headerField, conditions);
            }
        }
    }

    /**
     * Adds a new condition on a header field into the map.
     *
     * @param headerField into which we add the condition
     * @param condition to be added
     */
    public void addCondition(HeaderField headerField, Condition condition) {
        // Ensure a valid header field
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        // Ensure a valid condition
        checkNotNull(
            condition,
            "Cannot construct condition map using a NULL condition"
        );

        // Append condition to existing header field
        if (this.containsKey(headerField)) {
            this.get(headerField).add(condition);
        // Create new one
        } else {
            List<Condition> conditions = new ArrayList<Condition>();
            conditions.add(condition);
            this.put(headerField, conditions);
        }
    }

}
