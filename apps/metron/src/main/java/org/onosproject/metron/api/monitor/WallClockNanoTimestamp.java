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

package org.onosproject.metron.api.monitor;

import org.joda.time.DateTime;
import org.onosproject.store.Timestamp;

import com.google.common.collect.ComparisonChain;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A timestamp that derives its value from the prevailing
 * wallclock time on the controller where it is generated.
 * The timestamp's ganularity is at the level of nanoseconds.
 */
public class WallClockNanoTimestamp implements Timestamp {

    private final long unixTimestamp;

    public WallClockNanoTimestamp() {
        unixTimestamp = System.nanoTime();
    }

    public WallClockNanoTimestamp(long timestamp) {
        unixTimestamp = timestamp;
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof WallClockNanoTimestamp,
                "Must be WallClockNanoTimestamp", o);
        WallClockNanoTimestamp that = (WallClockNanoTimestamp) o;

        return ComparisonChain.start()
                .compare(this.unixTimestamp, that.unixTimestamp)
                .result();
    }
    @Override
    public int hashCode() {
        return Long.hashCode(unixTimestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WallClockNanoTimestamp)) {
            return false;
        }
        WallClockNanoTimestamp that = (WallClockNanoTimestamp) obj;
        return Objects.equals(this.unixTimestamp, that.unixTimestamp);
    }

    @Override
    public String toString() {
        return new DateTime(unixTimestamp).toString();
    }

    /**
     * Returns the unixTimestamp.
     *
     * @return unix timestamp
     */
    public long unixTimestamp() {
        return unixTimestamp;
    }
}