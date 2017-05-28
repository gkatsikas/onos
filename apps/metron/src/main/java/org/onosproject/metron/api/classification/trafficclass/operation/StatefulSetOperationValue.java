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

package org.onosproject.metron.api.classification.trafficclass.operation;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A stateful set value of a packet/flow operation.
 */
public class StatefulSetOperationValue extends StatefulOperationValue {

    private Set<Long> statefulSetValue = Sets.<Long>newConcurrentHashSet();

    public StatefulSetOperationValue() {
        super();
    }

    public StatefulSetOperationValue(Set<Long> statefulSetValue) {
        super((long) 0, (long) 0);

        checkNotNull(
            statefulSetValue,
            "Stateful field operation's set value is NULL"
        );

        this.statefulSetValue = statefulSetValue;
    }

    public StatefulSetOperationValue(StatefulSetOperationValue statefulSetOpValue) {
        checkNotNull(
            statefulSetOpValue,
            "Set-based stateful field operation is NULL"
        );

        this.statelessValue   = statefulSetOpValue.statelessValue();
        this.statefulValue    = statefulSetOpValue.statefulValue();
        this.statefulSetValue = Sets.<Long>newConcurrentHashSet(statefulSetOpValue.statefulSetValue());
    }

    /**
     * Returns the stateful set value of this field operation.
     *
     * @return stateful set value
     */
    public Set<Long> statefulSetValue() {
        return this.statefulSetValue;
    }

    /**
     * Sets the stateful set value of this field operation.
     *
     * @param statefulSetValue stateful set value
     */
    public void setStatefulSetValue(Set<Long> statefulSetValue) {
        checkNotNull(
            statefulSetValue,
            "Stateful field operation's set value must not be NULL"
        );

        this.statefulSetValue = statefulSetValue;
    }

    /**
     * Sets the stateful set value of this field operation.
     *
     * @param statefulValue stateful set value
     */
    public void addStatefulValue(long statefulValue) {
        checkArgument(
            statefulValue >= 0,
            "Stateful field operation's set value must not be NULL"
        );

        this.statefulSetValue.add(new Long(statefulValue));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.statefulSetValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof StatefulSetOperationValue))) {
            return false;
        }

        StatefulSetOperationValue other = (StatefulSetOperationValue) obj;

        return this.statefulSetValue().equals(other.statefulSetValue());
    }

    @Override
    public String toString() {
        String val = "";
        for (Long i : this.statefulSetValue) {
            val += Long.toString(i) + ",";
        }

        return  super.toString() + ", " +
                String.valueOf(this.statefulValue) + ", " +
                val.trim();
    }

}
