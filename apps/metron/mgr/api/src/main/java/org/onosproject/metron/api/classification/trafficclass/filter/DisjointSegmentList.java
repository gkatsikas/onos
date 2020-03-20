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

import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A disjoint list of segments to be used for
 * the construction of complex traffic class filters.
 */
public class DisjointSegmentList {

    private static final Logger log = getLogger(DisjointSegmentList.class);

    /**
     * The head of the segment list is a segment node.
     */
    private SegmentNode head;

    public DisjointSegmentList() {
        this.head = null;
    }

    public DisjointSegmentList(DisjointSegmentList other) {
        this.head = new SegmentNode(other.head());
    }

    /**
     * Returns the head of this segment node.
     *
     * @return SegmentNode head
     */
    public SegmentNode head() {
        return this.head;
    }

    /**
     * Returns whether the segment list is empty or not.
     *
     * @return boolean is empty
     */
    public boolean isEmpty() {
        return this.head == null;
    }

    /**
     * Checks whether the segment list contains a certain value.
     *
     * @param value to be checked
     * @return boolean status
     */
    public boolean contains(long value) {
        SegmentNode currentNode = this.head;

        while (currentNode != null) {
            if (value < currentNode.lowerLimit()) {
                return false;
            } else if (value <= currentNode.upperLimit()) {
                return true;
            }
            currentNode = currentNode.child();
        }

        return false;
    }

    /**
     * Checks whether the segment list contains a certain value.
     *
     * @param lowerLimit the lower limit of the segment to be checked
     * @param upperLimit the upper limit of the segment to be checked
     * @return boolean status
     */
    public boolean containsSegment(long lowerLimit, long upperLimit) {
        SegmentNode segment = new SegmentNode(lowerLimit, upperLimit);
        return this.include(this.head, segment);
    }

    /**
     * Checks whether a segment list is contained in another segment list.
     *
     * @param other another segment list to check if it is contained
     * @return boolean status
     */
    public boolean containsSegmentList(DisjointSegmentList other) {
        return this.include(this.head, other.head);
    }

    /**
     * Recursively checks whether a segment list is contained in another segment list.
     *
     * @param container the container segment
     * @param containee the containee segment
     * @return boolean status
     */
    public boolean include(SegmentNode container, SegmentNode containee) {
        if (containee == null) {
            return true;
        } else if (container == null) {
            return false;
        }

        long lowerLimit = containee.lowerLimit();
        long upperLimit = containee.upperLimit();

        SegmentNode currentSegment = container;

        while ((currentSegment != null) && (lowerLimit > currentSegment.upperLimit())) {
            currentSegment = currentSegment.child();
        }

        return (
            (currentSegment != null) &&
            (lowerLimit >= currentSegment.lowerLimit()) &&
            (upperLimit <= currentSegment.upperLimit()) &&
            this.include(currentSegment, containee.child())
        );
    }

    /**
     * Adds a new segment node to the segment list.
     *
     * @param lowerLimit the lower value of the new segment
     * @param upperLimit the upper value of the new segment
     */
    public void addSegment(long lowerLimit, long upperLimit) {
        // The new segment to be added
        SegmentNode segmentNode = new SegmentNode(lowerLimit, upperLimit);
        // Merge it with the existing segment list
        this.head = this.unify(this.head, segmentNode);
    }

    /**
     * Adds a new segment list to this segment list.
     *
     * @param newSegmentList the new segment list
     */
    public void addSegmentList(DisjointSegmentList newSegmentList) {
        this.head = this.unify(this.head, newSegmentList.head);
    }

    /**
     * Merges one segment node with another.
     *
     * @param container segment node to host the new node
     * @param toAdd segment node to be hosted by the former node
     * @return updates segment node
     */
    public SegmentNode unify(SegmentNode container, SegmentNode toAdd) {
        if (toAdd == null) {
            return container;
        }

        long lowerLimit = toAdd.lowerLimit();
        long upperLimit = toAdd.upperLimit();

        if (container == null) {
            SegmentNode segment = new SegmentNode(lowerLimit, upperLimit);
            return unify(segment, toAdd.child());
        }

        SegmentNode currentParent = null;
        SegmentNode currentChild  = container;

        // This puts us in front of all the stricly smaller segments
        while ((currentChild != null) && (lowerLimit > safeAdd(currentChild.upperLimit(), 1))) {
            currentParent = currentChild;
            currentChild  = currentChild.child();
        }

        // We are past all the other segments, we can just insert a new one here
        if (currentChild == null) {
            SegmentNode seg = new SegmentNode(lowerLimit, upperLimit);
            seg = this.unify(seg, toAdd.child());
            this.updateRelation(currentParent, seg);
            return container;
        }

        long newMinimum = Math.min(lowerLimit, currentChild.lowerLimit());

        List<SegmentNode> toClean = new ArrayList<>();
        long newMaximum = upperLimit;

        // We find all the segments that we touch
        while ((currentChild != null) && (safeAdd(upperLimit, 1) >= currentChild.lowerLimit())) {
            newMaximum = Math.max(upperLimit, currentChild.upperLimit());
            toClean.add(currentChild);
            currentChild = currentChild.child();
        }

        // Reset pointers to delete objects
        for (SegmentNode node : toClean) {
            this.resetNode(node);
        }

        SegmentNode seg = new SegmentNode(newMinimum, newMaximum);

        if (currentChild != null) {
            this.updateRelation(seg, currentChild);
        }

        seg = this.unify(seg, toAdd.child());

        if (currentParent != null) {
            this.updateRelation(currentParent, seg);
        } else {
            // If we have no parent we're the lowest one
            return seg;
        }

        return container;
    }

    /**
     * Subtracts the segment defined by the input values from the current segment list.
     *
     * @param lowerLimit lower value of the segment to be subtracted
     * @param upperLimit upper value of the segment to be subtracted
     */
    public void subtractSegment(long lowerLimit, long upperLimit) {
        this.head = this.differentiate(this.head, new SegmentNode(lowerLimit, upperLimit));
    }

    /**
     * Subtracts the input segment list from the current segment list.
     *
     * @param other segment list to be subtracted
     */
    public void subtractSegmentList(DisjointSegmentList other) {
        this.head = this.differentiate(this.head, other.head);
    }

    /**
     * Computes the difference between two segment nodes.
     *
     * @param container base segment node
     * @param toSubtract segment node to be subtracted
     * @return the output segment node after the difference is applied
     */
    public SegmentNode differentiate(SegmentNode container, SegmentNode toSubtract) {
        // There's nothing to subtract or to subtract from
        if ((container == null) || (toSubtract == null)) {
            return container;
        }

        SegmentNode currentParent = null;
        SegmentNode currentChild  = container;

        long lowerLimit = toSubtract.lowerLimit();
        long upperLimit = toSubtract.upperLimit();

        // This puts us in front of all the strictly smaller segments
        while ((currentChild != null) && (lowerLimit > currentChild.upperLimit())) {
            currentParent = currentChild;
            currentChild  = currentChild.child();
        }

        // All the segments are strictly smaller -> Nothing to do then
        if (currentChild == null) {
            return container;
        }

        // Goes past the first segment
        if (currentChild.lowerLimit() < lowerLimit) {
            SegmentNode newParent = new SegmentNode(currentChild.lowerLimit(), lowerLimit - 1);

            if (currentParent != null) {
                this.updateRelation(currentParent, newParent);
            } else {
                container = newParent;
            }

            currentParent = newParent;
            if (currentChild.upperLimit() > upperLimit) {
                currentChild.setLowerLimit(upperLimit + 1);
            } else {
                currentChild = currentChild.child();
            }
        }

        List<SegmentNode> toClean = new ArrayList<>();
        while ((currentChild != null) && (upperLimit >= currentChild.upperLimit())) {
            toClean.add(currentChild);
            currentChild = currentChild.child();
        }

        for (SegmentNode node : toClean) {
            this.resetNode(node);
        }

        currentChild = this.differentiate(currentChild, toSubtract.child());
        if (currentChild == null) {
            if (currentParent != null) {
                currentParent.setChild(null);
            } else {
                return null;
            }
        } else {
            // We are in the last segment
            if (currentChild.lowerLimit() <= upperLimit) {
                currentChild.setLowerLimit(upperLimit + 1);
            }

            if (currentParent != null) {
                this.updateRelation(currentParent, currentChild);
            } else {
                container = currentChild;
            }
        }

        return container;
    }

    /**
     * Intersects the segment defined by the input values with the current segment list.
     *
     * @param lowerLimit lower value of the segment to be intersected
     * @param upperLimit upper value of the segment to be intersected
     */
    public void intersectSegment(long lowerLimit, long upperLimit) {
        SegmentNode seg = new SegmentNode(lowerLimit, upperLimit);
        this.head = this.intersect(this.head, seg);
    }

    /**
     * Intersects the input segment list with the current segment list.
     *
     * @param other the segment list to be intersected
     */
    public void intersectSegmentList(DisjointSegmentList other) {
        this.head = intersect(this.head, other.head);
    }

    /**
     * Intersects the segment defined by the input values with the current segment list.
     *
     * @param container the base segment node
     * @param toIntersect the segment node to be intersected with the base
     * @return the segment list that derives after the intersection
     */
    public SegmentNode intersect(SegmentNode container, SegmentNode toIntersect) {
        if ((container == null) || (toIntersect == null)) {
            this.destroyList(container);
            return container;
        }

        long lowerLimit = toIntersect.lowerLimit();
        long upperLimit = toIntersect.upperLimit();
        SegmentNode currentNode = container;

        // This puts us in front of all the strictly smaller segments
        while ((currentNode != null) && (lowerLimit > currentNode.upperLimit())) {
            currentNode = currentNode.child();
        }

        if ((currentNode == null) || (upperLimit < currentNode.lowerLimit())) {
            this.destroyList(container);
            return null;
        }

        // Removes leading segments if necessary
        if (currentNode.parent() != null) {
            currentNode.parent().setChild(null);
            currentNode.setParent(null);

            this.destroyList(container);

            container = currentNode;
        }

        currentNode.setLowerLimit(Math.max(lowerLimit, currentNode.lowerLimit()));

        // Goes through all nodes that intersect
        while (
                (currentNode.child() != null) &&
                (upperLimit >= currentNode.child().lowerLimit())) {
            currentNode = currentNode.child();
        }

        long oldUpperLimit = currentNode.upperLimit();
        currentNode.setUpperLimit(Math.min(upperLimit, currentNode.upperLimit()));

        if (oldUpperLimit > upperLimit + 1) {
            SegmentNode grandChild = currentNode.child();
            SegmentNode newChild = new SegmentNode(upperLimit + 1, oldUpperLimit);
            this.updateRelation(currentNode, newChild);

            if (grandChild != null) {
                this.updateRelation(newChild, grandChild);
            }
        }

        SegmentNode newChild = this.intersect(currentNode.child(), toIntersect.child());
        if (newChild == null) {
            currentNode.setChild(null);
        } else {
            this.updateRelation(currentNode, newChild);
        }

        return container;
    }

    /**
     * Translates the current segment list according to the input value.
     * The translation can be either forward or backword.
     *
     * @param value the integer according to which the segment list
     *        will be translated
     * @param forward boolean flag to indicate the direction we should move
     */
    public void translate(long value, boolean forward) {
        if (forward) {
            this.moveForward(value);
        } else {
            this.moveBackwards(value);
        }
    }

    private void moveForward(long value) {
        SegmentNode currentNode = this.head;

        if ((this.head != null) && (Constants.MAX_UINT - this.head.lowerLimit() < value)) {
            this.destroyList(this.head);
            this.head = null;
            return;
        }

        while ((currentNode != null) && (Constants.MAX_UINT - currentNode.lowerLimit() >= value)) {
            currentNode.setLowerLimit(currentNode.lowerLimit() + value);
            currentNode.setUpperLimit(this.safeAdd(currentNode.upperLimit(), value));
            currentNode = currentNode.child();
        }

        if (currentNode == null) {
            return;
        }

        this.destroyList(currentNode.child());
    }

    private void moveBackwards(long value) {
        SegmentNode currentNode = this.head;

        // Let's go past all the segments that are below value
        while ((currentNode != null) && (currentNode.upperLimit() < value)) {
            currentNode = currentNode.child();
        }

        if (currentNode == null) {
            this.destroyList(this.head);
            this.head = null;
            return;
        }

        // All the previous nodes are too small to be translated so they must be destroyed
        if ((currentNode != null) && (currentNode.parent() != null)) {
            currentNode.parent().setChild(null);
            currentNode.setParent(null);

            this.destroyList(this.head);
            this.head = currentNode;
        }

        currentNode.setLowerLimit(this.safeSubtract(currentNode.lowerLimit(), value));

        currentNode.setUpperLimit(currentNode.upperLimit() - value);
        currentNode = currentNode.child();

        while (currentNode != null) {
            currentNode.setLowerLimit(currentNode.lowerLimit() - value);
            currentNode.setUpperLimit(currentNode.upperLimit() - value);
            currentNode = currentNode.child();
        }
    }

    /**
     * Updates the list pointers between two nodes.
     *
     * @param parent the parent node in the list
     * @param child the child node in the list
     */
    public void updateRelation(SegmentNode parent, SegmentNode child) {
        parent.setChild(child);
        child.setParent(parent);
    }

    /**
     * Resets the memory of a given segment node.
     *
     * @param node a segment node
     */
    public void resetNode(SegmentNode node) {
        node.setParent(null);
        node.setChild(null);
        node = null;
    }

    /**
     * Returns this segment list as a list of
     * pair integer values (i.e., segments).
     *
     * @return a list of paired integers (segments)
     */
    public List<Segment> segments() {
        List<Segment> output = new ArrayList<>();

        SegmentNode currentNode = head;
        while (currentNode != null) {
            Segment seg = new Segment(
                new Long(currentNode.lowerLimit()),
                new Long(currentNode.upperLimit())
            );
            output.add(seg);
            currentNode = currentNode.child();
        }

        return output;
    }

    /**
     * Resets the memory of the list past the input node.
     *
     * @param node a segment node
     */
    void destroyList(SegmentNode node) {
        if (node != null) {
            if (node.child() != null) {
                this.destroyList(node.child());
            }
            this.resetNode(node);
        }
    }

    /**
     * Creates a copy of the segment list
     * starting from the input node.
     *
     * @param oldList a segment node to be copied
     * @return the copied list
     */
    SegmentNode copyList(SegmentNode oldList) {
        if (oldList == null) {
            return null;
        }

        SegmentNode newList = new SegmentNode(oldList.lowerLimit(), oldList.upperLimit());
        SegmentNode newChild = copyList(oldList.child());

        if (newChild != null) {
            this.updateRelation(newList, newChild);
        }

        return newList;
    }

    /**
     * Subtracts integers while paying attention to their signs.
     *
     * @param a first operand
     * @param b second operand
     * @result result of the subtraction
     */
    private long safeSubtract(long a, long b) {
        return ((b > a) ? 0 : a - b);
    }

    /**
     * Adds integers while paying attention not to
     * exceed the upper limit of an integer.
     *
     * @param a first operand
     * @param b second operand
     * @result result of the sum
     */
    private long safeAdd(long a, long b) {
        return ((Constants.MAX_UINT - b < a) ? Constants.MAX_UINT : b + a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.head);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof DisjointSegmentList))) {
            return false;
        }

        DisjointSegmentList segList = (DisjointSegmentList) obj;

        return (this.head.equals(segList.head));
    }

    /**
     * Translates this segment list into a textual representation.
     *
     * @return textual representation
     */
    @Override
    public String toString() {
        String output = "";

        if (head == null) {
            return output;
        }

        SegmentNode currentNode = head;
        while (currentNode != null) {
            output += currentNode.toString() + " ";
            currentNode = currentNode.child();
        }

        return output.trim();
    }

    /**
     * Translates this segment list into a textual representation of IP addresses.
     *
     * @return textual representation with IP addresses
     */
    public String toIpString() {
        String output = "";

        if (head == null) {
            return output;
        }

        SegmentNode currentNode = head;
        while (currentNode != null) {
            output += currentNode.toIpString() + " ";
            currentNode = currentNode.child();
        }

        return output.trim();
    }

}
