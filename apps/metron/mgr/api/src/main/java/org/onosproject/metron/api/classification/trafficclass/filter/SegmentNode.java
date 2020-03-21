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

package org.onosproject.metron.api.classification.trafficclass.filter;

import org.onosproject.metron.api.common.Constants;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A segment is the unit of an SNF/Metron filter.
 */
public class SegmentNode {

    private long lowerLimit;
    private long upperLimit;

    private SegmentNode parent;
    private SegmentNode child;

    public SegmentNode(long lowerLimit, long upperLimit) {
        checkArgument(
            (lowerLimit >= 0) &&
            (lowerLimit <= Constants.MAX_LONG),
            "Lower limit " + lowerLimit +
            " of segment is violated (range is [0, " + Constants.MAX_LONG + "])"
        );

        checkArgument(
            (upperLimit >= 0) &&
            (upperLimit >= lowerLimit) &&
            (upperLimit <= Constants.MAX_LONG),
            "Upper limit " + upperLimit +
            " of segment is violated (range is [0, " + Constants.MAX_LONG + "])"
        );

        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.parent = null;
        this.child  = null;
    }

    public SegmentNode(SegmentNode other) {
        checkNotNull(
            other,
            "Cannot construct a segment node out of a NULL one"
        );

        this.lowerLimit = other.lowerLimit;
        this.upperLimit = other.upperLimit;
        this.parent = new SegmentNode(
            other.parent().lowerLimit(),
            other.parent().upperLimit()
        );
        this.child  = new SegmentNode(
            other.child().lowerLimit(),
            other.child().upperLimit()
        );
    }

    /**
     * Returns the lower limit of this segment.
     *
     * @return lower limit of this segment
     */
    public long lowerLimit() {
        return this.lowerLimit;
    }

    /**
     * Sets the lower limit of this segment.
     *
     * @param lowerLimit lower limit of this segment
     */
    public void setLowerLimit(long lowerLimit) {
        checkArgument(
            (lowerLimit >= 0) &&
            (lowerLimit <= Constants.MAX_LONG),
            "Lower limit " + lowerLimit +
            " of segment is violated (range is [0, " + Constants.MAX_LONG + "])"
        );

        this.lowerLimit = lowerLimit;
    }

    /**
     * Returns the upper limit of this segment.
     *
     * @return upper limit of this segment
     */
    public long upperLimit() {
        return this.upperLimit;
    }

    /**
     * Sets the upper limit of this segment.
     *
     * @param upperLimit upper limit of this segment
     */
    public void setUpperLimit(long upperLimit) {
        checkArgument(
            (upperLimit >= 0) &&
            (upperLimit <= Constants.MAX_LONG),
            "Upper limit " + upperLimit +
            " of segment is violated (range is [0, " + Constants.MAX_LONG + "])"
        );

        this.upperLimit = upperLimit;
    }

    /**
     * Returns the parent node of this segment.
     *
     * @return parent segment node
     */
    public SegmentNode parent() {
        return this.parent;
    }

    /**
     * Sets the parent node of this segment.
     *
     * @param parent segment node
     */
    public void setParent(SegmentNode parent) {
        this.parent = parent;
    }

    /**
     * Returns the child node of this segment.
     *
     * @return child segment node
     */
    public SegmentNode child() {
        return this.child;
    }

    /**
     * Sets the child node of this segment.
     *
     * @param child segment node
     */
    public void setChild(SegmentNode child) {
        this.child = child;
    }

    /**
     * Translates this node into a MAC address range.
     *
     * @return string-based MAC address range
     */
    public String toMacString() {
        MacAddress macLower = MacAddress.valueOf((long) this.lowerLimit);
        MacAddress macUpper = MacAddress.valueOf((long) this.upperLimit);

        return "[" + macLower.toString() + ", " + macUpper.toString() + "]";
    }

    /**
     * Translates this node into an IP address range.
     *
     * @return string-based IP address range
     */
    public String toIpString() {
        IpAddress ipLower = IpAddress.valueOf((int) this.lowerLimit);
        IpAddress ipUpper = IpAddress.valueOf((int) this.upperLimit);

        return "[" + ipLower.toString() + ", " + ipUpper.toString() + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.lowerLimit, this.upperLimit, this.parent, this.child
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof SegmentNode))) {
            return false;
        }

        SegmentNode other = (SegmentNode) obj;

        return  (this.lowerLimit == other.lowerLimit) &&
                (this.upperLimit == other.upperLimit);
    }

    @Override
    public String toString() {
        return "[" + String.valueOf(this.lowerLimit) + ", " +
                String.valueOf(this.upperLimit) + "]";
    }

}
