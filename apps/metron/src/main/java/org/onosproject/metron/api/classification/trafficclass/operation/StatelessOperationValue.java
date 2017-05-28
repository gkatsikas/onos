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
 * A stateless value of a packet/flow operation.
 */
public class StatelessOperationValue extends OperationValue {

    protected long statelessValue;

    public StatelessOperationValue() {
        this.statelessValue = 0L;
    }

    public StatelessOperationValue(long statelessValue) {
        checkArgument(
            statelessValue >= 0L,
            "Stateless field operation's value must be non-negative"
        );

        this.statelessValue = statelessValue;
    }

    public StatelessOperationValue(StatelessOperationValue statelessOpValue) {
        checkNotNull(
            statelessOpValue,
            "Stateless field operation is NULL"
        );

        this.statelessValue = statelessOpValue.statelessValue();
    }

    /**
     * Returns the stateless value of this field operation.
     *
     * @return stateless value
     */
    public long statelessValue() {
        return this.statelessValue;
    }

    /**
     * Sets the stateless value of this field operation.
     *
     * @param statelessValue stateless value
     */
    public void setStatelessValue(long statelessValue) {
        checkArgument(
            statelessValue >= 0L,
            "Stateless field operation's value must be a positive integer"
        );

        this.statelessValue = statelessValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.statelessValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof StatelessOperationValue))) {
            return false;
        }

        StatelessOperationValue other = (StatelessOperationValue) obj;

        return this.statelessValue() == other.statelessValue();
    }

    @Override
    public String toString() {
        return String.valueOf(this.statelessValue);
    }

}
