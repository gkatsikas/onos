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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A stateful value of a packet/flow operation.
 */
public class StatefulOperationValue extends StatelessOperationValue {

    protected long statefulValue;

    public StatefulOperationValue() {
        super();

        this.statefulValue = 0L;
    }

    public StatefulOperationValue(long statelessValue, long statefulValue) {
        super(statelessValue);

        checkArgument(
            statefulValue >= 0L,
            "Stateless field operation's value must be non-negative"
        );

        this.statefulValue = statefulValue;
    }

    public StatefulOperationValue(StatefulOperationValue statefulOpValue) {
        checkNotNull(
            statefulOpValue,
            "Stateful field operation is NULL"
        );

        this.statelessValue = statefulOpValue.statelessValue();
        this.statefulValue  = statefulOpValue.statefulValue();
    }

    /**
     * Returns the stateful value of this field operation.
     *
     * @return stateful value
     */
    public long statefulValue() {
        return this.statefulValue;
    }

    /**
     * Sets the stateful value of this field operation.
     *
     * @param statefulValue stateful value
     */
    public void setStatefulValue(long statefulValue) {
        checkArgument(
            statefulValue >= 0L,
            "Stateless field operation's value must be a positive integer"
        );

        this.statefulValue = statefulValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.statelessValue, this.statefulValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof StatefulOperationValue))) {
            return false;
        }

        StatefulOperationValue other = (StatefulOperationValue) obj;

        return this.statelessValue() == other.statelessValue() &&
               this.statefulValue()  == other.statefulValue();
    }

    @Override
    public String toString() {
        return  super.toString() + "," +
                String.valueOf(this.statefulValue);
    }

}
