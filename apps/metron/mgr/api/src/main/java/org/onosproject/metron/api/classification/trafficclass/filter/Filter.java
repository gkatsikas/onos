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
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.classification.ClassificationSyntax;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;
import org.onosproject.metron.api.processing.ProcessingLayer;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import org.slf4j.Logger;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A header field-specific packet filter.
 */
public class Filter {

    private static final Logger log = getLogger(Filter.class);

    /**
     * The header field of this filter.
     * This is the fundamental concept of a filter.
     */
    private HeaderField headerField;

    /**
     * In case of an ambiguous header field (e.g., IP DSCP),
     * which is part of a byte, we set a specific header field
     * type with position and length.
     */
    private short fieldPosition;
    private short fieldLength;

    /**
     * The type of a filter depends on its header field.
     * If its header field belongs to a specific protocol
     * (e.g., TCP), then the traffic class type is TCP.
     */
    private TrafficClassType fieldType = null;

    /**
     * The layer on which this filter operates.
     */
    private ProcessingLayer fieldLayer = null;

    private DisjointSegmentList filter;
    private DisjointSegmentList toSubtract;

    /**
     * Constructs the largest possible filter out of an unsigned integer.
     */
    public Filter() {
        this(HeaderField.UNKNOWN, (long) 0, (long) Constants.MAX_UINT);
    }

    /**
     * Constructs the largest possible filter for this header field.
     *
     * @param headerField the header field for which we construct this filter
     */
    public Filter(HeaderField headerField) {
        this(headerField, (long) 0, (long) Constants.HEADER_FIELD_UPPER_BOUND.get(headerField));
    }

    /**
     * Constructs a filter without range.
     *
     * @param headerField the header field for which we construct this filter
     * @param value a single value for this filter
     */
    public Filter(HeaderField headerField, long value) {
        this(headerField, (long) value, (long) value);
    }

    /**
     * Constructs a filter for ambiguous header fields (i.e., those less than one byte long).
     *
     * @param headerField the header field for which we construct this filter
     * @param fieldPosition the field position in case that the header field is part of a byte
     * @param fieldLength the field length in case that the header field is part of a byte
     */
    public Filter(HeaderField headerField, short fieldPosition, short fieldLength) {
        checkArgument(
            headerField == HeaderField.AMBIGUOUS,
            "Ambiguous header field type is expected"
        );

        // TODO: Provide better upper bound
        checkArgument(
            (fieldPosition >= 0) && (fieldPosition < 300),
            "Header position of ambiguous header field is out of bounds"
        );

        // TODO: Provide better upper bound
        checkArgument(
            (fieldLength >= 0) && (fieldLength < 64),
            "Header length of ambiguous header field is out of bounds"
        );

        this.headerField   = headerField;
        this.fieldPosition = fieldPosition;
        this.fieldLength   = fieldLength;
        this.fieldType     = TrafficClassType.createByHeaderField(this.headerField);
        checkArgument(this.fieldType == TrafficClassType.AMBIGUOUS);
        this.fieldLayer    = ProcessingLayer.createByHeaderField(this.headerField);
        checkArgument(this.fieldLayer == ProcessingLayer.TRANSPORT);

        this.filter     = new DisjointSegmentList();
        this.toSubtract = new DisjointSegmentList();

        log.warn("Ambiguous header field is created");
    }

    /**
     * Constructs a filter with given bounds.
     *
     * @param headerField the header field for which we construct this filter
     * @param lowerLimit lower limit of this filter
     * @param upperLimit upper limit of this filter
     */
    public Filter(HeaderField headerField, long lowerLimit, long upperLimit) {
        // Sanity check for the bounds and the header field
        this.checkFilterBounds(headerField, lowerLimit, upperLimit);

        this.headerField   = headerField;
        this.fieldPosition = -1;
        this.fieldLength   = -1;
        this.fieldType     = TrafficClassType.createByHeaderField(this.headerField);
        this.fieldLayer    = ProcessingLayer.createByHeaderField(this.headerField);

        this.filter     = new DisjointSegmentList();
        this.toSubtract = new DisjointSegmentList();

        this.filter.addSegment(lowerLimit, upperLimit);
    }

    /**
     * Constructs a filter out of another.
     *
     * @param other the filter to copy-construct this one
     */
    public Filter(Filter other) {
        this.headerField   = other.headerField();
        this.fieldPosition = other.fieldPosition();
        this.fieldLength   = other.fieldLength();
        this.fieldType     = TrafficClassType.createByHeaderField(this.headerField);
        this.fieldLayer    = ProcessingLayer.createByHeaderField(this.headerField);

        this.filter     = new DisjointSegmentList();
        this.toSubtract = new DisjointSegmentList();

        this.filter.addSegmentList(other.filter());
        this.toSubtract.addSegmentList(other.filterToSubtract());
    }

    /**
     * Returns the header field of this filter.
     *
     * @return header field of this filter
     */
    public HeaderField headerField() {
        return this.headerField;
    }

    /**
     * Sets the header field of this filter.
     *
     * @param headerField the header field to be set
     * @throws IllegalArgumentException if header field cannot be set
     */
    public void setHeaderField(HeaderField headerField) {
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        this.headerField = headerField;
    }

    /**
     * Returns the header position of the header field of this filter.
     *
     * @return header position of the header field of this filter
     */
    public short fieldPosition() {
        return this.fieldPosition;
    }

    /**
     * Sets the header position of the header field of this filter.
     *
     * @param fieldPosition header position of the header field of this filter
     */
    public void setFieldPosition(short fieldPosition) {
        this.fieldPosition = fieldPosition;
    }

    /**
     * Returns the header length of the header field of this filter.
     *
     * @return header length of the header field of this filter
     */
    public short fieldLength() {
        return this.fieldLength;
    }

    /**
     * Sets the header length of the header field of this filter.
     *
     * @param fieldLength header length of the header field of this filter
     */
    public void setFieldLength(short fieldLength) {
        this.fieldLength = fieldLength;
    }

    /**
     * Returns the segment list associated with this filter.
     *
     * @return segment list associated with this filter
     */
    public DisjointSegmentList filter() {
        return this.filter;
    }

    /**
     * Sets the segment list associated with this filter.
     *
     * @param filter segment list associated with this filter
     */
    public void setFilter(DisjointSegmentList filter) {
        this.filter = filter;
    }

    /**
     * Returns the segment list to subtract from this filter.
     * This is an optimization to compress a traffic class.
     *
     * @return segment list to subtract from the filter
     */
    public DisjointSegmentList filterToSubtract() {
        return this.toSubtract;
    }

    /**
     * Sets the segment list to subtract from this filter.
     *
     * @param toSubtract segment list to subtract from the filter
     */
    public void setFilterToSubtract(DisjointSegmentList toSubtract) {
        this.toSubtract = toSubtract;
    }

    /**
     * Returns the type of the header field of this filter.
     *
     * @return type of the header field of this filter
     */
    public TrafficClassType fieldType() {
        return this.fieldType;
    }

    /**
     * Sets the type of the header field of this filter.
     *
     * @param fieldType type of the header field of this filter
     */
    public void setFieldType(TrafficClassType fieldType) {
        this.fieldType = fieldType;
    }

    /**
     * Returns the processing layer of the header field of this filter.
     *
     * @return processing layer of the header field of this filter
     */
    public ProcessingLayer fieldLayer() {
        return this.fieldLayer;
    }

    /**
     * Sets the processing layer of the header field of this filter.
     *
     * @param fieldLayer processing layer of the header field of this filter
     */
    public void setFieldType(ProcessingLayer fieldLayer) {
        this.fieldLayer = fieldLayer;
    }

    /**
     * Checks whether a filter's bounds comply with its type (header field).
     * For example, port numbers cannot exceed 65535, although the
     * upper bound of an unsigned integer is much higher.
     *
     * @param headerField the header field to be checked
     * @param lowerLimit the lower bound of the header field
     * @param upperLimit the upper bound of the header field
     */
    private static void checkFilterBounds(HeaderField headerField, long lowerLimit, long upperLimit) {
        if (!ArrayUtils.contains(HeaderField.values(), headerField)) {
            throw new IllegalArgumentException(String.valueOf(headerField));
        }

        // This is the maximum value that this header field can take.
        long maxValue = Constants.HEADER_FIELD_UPPER_BOUND.get(headerField);

        // First check whether we respect the limits of unsigned integers
        checkArgument(
            (lowerLimit >= 0) &&
            (lowerLimit <= maxValue) &&
            (upperLimit >= 0) &&
            (upperLimit >= lowerLimit) &&
            (upperLimit <= maxValue),
            "Header field " + headerField.toString() +
            " must take values in [0, " + maxValue + "]"
        );
    }

    /**
     * Creates a filter from an Ethernet address.
     *
     * @param headerField header field of the filter
     * @param macAddressStr Ethernet address as a string
     * @return a filter
     */
    public static Filter fromEthernetAddress(HeaderField headerField, String macAddressStr) {
        // Verify that the input header field is valid
        checkArgument(HeaderField.isEthernetAddress(headerField));

        MacAddress mac = null;
        try {
            mac = MacAddress.valueOf(macAddressStr);
        } catch (IllegalArgumentException iaEx) {
            log.error("Invalid MAC address {} with error: {}", macAddressStr, iaEx.toString());
            throw iaEx;
        }

        long value = mac.toLong();

        // Sanity check on the bounds
        checkFilterBounds(headerField, value, value);

        return new Filter(headerField, value);
    }

    /**
     * Creates a filter from an integer IPv4 prefix.
     *
     * @param headerField header field of the filter
     * @param value IP address as an integer
     * @param prefix IP prefix
     * @return a filter
     */
    public static Filter fromIpv4Prefix(HeaderField headerField, long value, int prefix) {
        // Verify that the input header field is valid
        checkArgument(HeaderField.isIpAddress(headerField));

        // Prefix must not be greater than the maximum IPv4 prefix (32)
        checkArgument(
            prefix <= IpPrefix.MAX_INET_MASK_LENGTH,
            "Network prefix " + prefix + " higher than " + IpPrefix.MAX_INET_MASK_LENGTH
        );

        if (prefix == IpPrefix.MAX_INET_MASK_LENGTH) {
            return new Filter(headerField, value);
        }

        long translation = IpPrefix.MAX_INET_MASK_LENGTH - prefix;
        long lowerLimit = value & (Constants.MAX_UINT << translation);
        long upperLimit = value | (Constants.MAX_UINT >> prefix);

        // Sanity check on the bounds
        checkFilterBounds(headerField, lowerLimit, upperLimit);

        return new Filter(headerField, lowerLimit, upperLimit);
    }

    /**
     * Creates a filter from a string-based IPv4 prefix expression.
     *
     * @param headerField header field of the filter
     * @param ipPrefixStr IP address/prefix as a string
     * @return a filter
     */
    public static Filter fromIpv4PrefixStr(HeaderField headerField, String ipPrefixStr) {
        int prefixPos = ipPrefixStr.indexOf("/");
        checkArgument(
            (prefixPos > 0) && (prefixPos < ipPrefixStr.length()),
            "IP address " + ipPrefixStr + " does not have a valid prefix"
        );

        // Split the IP prefix
        String[] tokens    = ipPrefixStr.split("/");
        String   ipStr     = tokens[0];
        String   prefixStr = tokens[1];

        // Convert into unsigned integer
        long address = Common.stringIpToInt(ipStr);
        int prefix   = Integer.parseInt(prefixStr);

        return fromIpv4Prefix(headerField, address, prefix);
    }

    /**
     * Creates a filter from an IPClassifier expression.
     * Cases:
     * |-> Single port: 1234
     * |-> Port range: 1234-5678
     * |-> Any operator on port: [=<>!]= 1234
     * |-> Inequality operator on port: [<>] 1234
     * |-> Logical OR: 1234 or/|| 4567
     * |-> IP address
     *
     * @param headerField header field of the filter
     * @param pattern IPClassifier's pattern
     * @return a filter
     */
    public static Filter fromIpClassifierPattern(HeaderField headerField, String pattern) {
        final String errorMessage = "Wrong argument in IPFilter: " + pattern + ". ";

        // To be excluded from the search
        String numbers = "0123456789";

        if (HeaderField.isIpAddress(headerField)) {
            // Exclude also the dots between bytes
            numbers += ".";
        }

        int pos = Common.findFirstNotOf(pattern, numbers, 0);
        Filter f = new Filter();

        // End of the pattern (implies a single IP or number)
        if (pos < 0) {
            return new Filter(headerField, Common.stringOrIpToUnsignedInt(pattern));
        // Either [=<>!]= number or [<>] number
        } else if (pos == 0) {
            // log.info("POS = 0 with pattern {}", pattern);
            if (pattern.length() < 3) {
                throw new IllegalArgumentException(errorMessage);
            }

            if (pattern.charAt(0) == '=') {
                if (pattern.length() < 2 || pattern.charAt(1) != '=') {
                    throw new IllegalArgumentException(
                        errorMessage + "Expected '=' after '=' in: " + pattern
                    );
                }

                String numberStr = pattern.substring(2, pattern.length()).trim();
                f = new Filter(
                    headerField,
                    Common.stringToUnsignedInt(numberStr)
                );
            } else if (pattern.charAt(0) == '<') {
                String numberStr = pattern.substring(2, pattern.length()).trim();
                long number = Common.stringOrIpToUnsignedInt(numberStr);

                // The filter must include this mumber (<=)
                if (pattern.charAt(1) == '=') {
                    // Do nothing
                // The filter must end right before this number (<)
                } else if (pattern.charAt(1) == ' ') {
                    number--;
                } else {
                    throw new IllegalArgumentException(
                        errorMessage + "Expected one of ' ' or '=' after '<' in: " + pattern
                    );
                }

                // Construct the filter from the minimum to this number
                f = new Filter(
                    headerField,
                    0,
                    number
                );
            } else if (pattern.charAt(0) == '>') {
                String numberStr = pattern.substring(2, pattern.length()).trim();
                long number = Common.stringOrIpToUnsignedInt(numberStr);

                // The filter must include this mumber (>=)
                if (pattern.charAt(1) == '=') {
                    // Do nothing
                // The filter must start right after this number (>)
                } else if (pattern.charAt(1) == ' ') {
                    number++;
                } else {
                    throw new IllegalArgumentException(
                        errorMessage + "Expected one of ' ' or '=' after '>'' in: " + pattern
                    );
                }

                // Construct the filter from this number to the maximum value of this field
                f = new Filter(
                    headerField,
                    number,
                    Constants.HEADER_FIELD_UPPER_BOUND.get(headerField)
                );
            } else if (pattern.charAt(0) == '!') {
                if (pattern.charAt(1) != '=') {
                    throw new IllegalArgumentException(
                        errorMessage + "Expected '=' after '!'' in: " + pattern
                    );
                } else {
                    // Construct a filter that covers the whole range for this header field
                    f = new Filter(headerField);

                    // Now construct a filter for the number we want to exclude
                    String numberStr = pattern.substring(2, pattern.length()).trim();
                    Filter temp = new Filter(
                        headerField,
                        Common.stringOrIpToUnsignedInt(numberStr)
                    );

                    // Take the difference, to find what is left (negation)
                    f = f.differentiate(temp);
                }
            }
        } else {
            int start;

            String lowerBoundStr = pattern.substring(0, pos).trim();

            // Range (e.g., 1234-5678)
            if (HeaderField.isTransportPort(headerField) && (pattern.charAt(pos) == '-')) {
                long lowerBoundInt = Common.stringToUnsignedInt(lowerBoundStr);

                // Move forward
                start = pos + 1;

                // Find the upper bound
                pos = Common.findFirstNotOf(pattern, numbers, start);
                // Negative means we go until the end of the string
                if (pos < 0) {
                    pos = pattern.length();
                }

                // Fetch the upper bound
                long upperBoundInt = Common.stringToUnsignedInt(pattern.substring(start, pos).trim());

                // Create a filter out of these bounds
                f = new Filter(headerField, lowerBoundInt, upperBoundInt);
            } else {
                f = new Filter(
                    headerField,
                    Common.stringOrIpToUnsignedInt(lowerBoundStr)
                );
            }

            start = Common.findFirstNotOf(pattern, numbers, pos);

            // From now on we take the union of filters (more than one)
            while ((start != pattern.length()) && (start > 0)) {
                // TODO: Check that it's or/|| in the middle
                pos = Common.findFirstNotOf(pattern, numbers, start);
                if (pos < 0) {
                    pos = pattern.length();
                }

                // Range (e.g., 1234-5678)
                if (HeaderField.isTransportPort(headerField) && (pattern.charAt(pos) == '-')) {
                    // Compute the lower bound
                    long lowerBoundInt = Common.stringToUnsignedInt(pattern.substring(start, pos).trim());
                    // log.info("Lower {}", lowerBoundInt);

                    // Move forward
                    start = pos + 1;

                    // Find the new end
                    pos = Common.findFirstNotOf(pattern, numbers, start);
                    // Negative means we go until the end of the string
                    if (pos < 0) {
                        pos = pattern.length();
                    }

                    // Find the upper bound
                    long upperBoundInt = Common.stringToUnsignedInt(pattern.substring(start, pos).trim());
                    // log.info("Upper {}", upperBoundInt);

                    // Create a filter out of these bounds
                    Filter temp = new Filter(headerField, lowerBoundInt, upperBoundInt);

                    // ..and merge it with the existing one
                    f = f.unite(temp);
                } else {
                    if (pos - start > 0) {
                        Filter temp = new Filter(
                            headerField,
                            Common.stringOrIpToUnsignedInt(pattern.substring(start, pos - start).trim())
                        );

                        // Merge with the existing filter
                        f = f.unite(temp);
                    } else {
                        break;
                    }
                }

                start = Common.findFirstNotOf(pattern, numbers, pos);
            }

            return f;
        }

        return f;
    }

    /**
     * Creates a filter from an IP lookup pattern.
     *
     * @param headerField header field of the filter
     * @param pattern IP lookup pattern
     * @return a filter
     */
    public static Filter fromPrefixPattern(HeaderField headerField, String pattern) {
        Filter f = null;

        String prefixChars = "0123456789./";
        int pos = Common.findFirstNotOf(pattern, prefixChars, 0);

        // End of the pattern
        if (pos < 0) {
            f = fromIpv4PrefixStr(headerField, pattern);
        } else if (pos == 0) {
            // ==
            if (pattern.charAt(0) == '=') {
                if (pattern.length() < 2 || pattern.charAt(1) != '=') {
                    throw new IllegalArgumentException(
                        "Expected '=' after '=' in: " + pattern
                    );
                }

                f = fromIpv4PrefixStr(headerField, pattern.substring(2, pattern.length()));
            // !=
            } else if (pattern.charAt(0) == '!') {
                if (pattern.length() < 2 || pattern.charAt(1) != '=') {
                    throw new IllegalArgumentException(
                        "Expected '=' after '!' in: " + pattern
                    );
                }

                f = fromIpv4PrefixStr(headerField, pattern.substring(2, pattern.length()));

                Filter full = new Filter(headerField);
                f = full.differentiate(f);
            } else {
                f = new Filter(headerField);
            }
        } else {
            f = new Filter(headerField);
            int start = 0;

            while ((start < pattern.length()) && (start > 0)) {
                pos = Common.findFirstNotOf(pattern, prefixChars, start);
                if (pos < 0) {
                    pos = pattern.length();
                }

                // Merge
                if (pos - start > 0) {
                    f = f.unite(
                        fromIpv4PrefixStr(headerField, pattern.substring(start, pos - start))
                    );
                }

                // Find next
                start = Common.findFirstNotOf(pattern, prefixChars, pos);
            }
        }

        return f;
    }

    /**
     * Checks whether this filter matches the input value.
     *
     * @param value to match against
     * @return boolean match status
     */
    public boolean match(long value) {
        return (this.filter.contains(value) && !this.toSubtract.contains(value));
    }

    /**
     * Checks whether this filter contains the input filter.
     *
     * @param newFilter filter to check against
     * @return boolean contains status
     */
    public boolean contains(Filter newFilter) {
        DisjointSegmentList isIn = newFilter.filter;
        isIn.subtractSegmentList(newFilter.toSubtract);

        DisjointSegmentList contains = this.filter;
        contains.subtractSegmentList(this.toSubtract);

        return contains.containsSegmentList(isIn);
    }

    /**
     * Checks whether this filter is empty.
     *
     * @return boolean emptiness status
     */
    public boolean isNone() {
        return (this.filter.isEmpty() || this.toSubtract.containsSegmentList(this.filter));
    }

    /**
     * Translation between this and the input filter.
     *
     * @param value filter to translate
     * @param forward direction to follow
     * @return updated filter
     */
    public Filter translate(long value, boolean forward) {
        this.filter.translate(value, forward);
        this.toSubtract.translate(value, forward);

        return this;
    }

    /**
     * Computes the union between this and the input filter.
     *
     * @param newFilter filter to unite
     * @return updated filter
     */
    public Filter unite(Filter newFilter) {
        log.debug("Unite " + this.toString() + " with " + newFilter.toString());

        // Union of fields means updated type
        this.updateType(newFilter.fieldType());
        // .. and layer
        this.updateLayer(newFilter.fieldLayer());

        this.filter.addSegmentList(newFilter.filter);
        this.toSubtract.subtractSegmentList(newFilter.filter);

        DisjointSegmentList temp = newFilter.toSubtract;
        temp.subtractSegmentList(this.filter);
        this.toSubtract.addSegmentList(temp);

        return this;
    }

    /**
     * Computes the intersection between this and the input filter.
     *
     * @param newFilter filter to intersect
     * @return updated filter
     */
    public Filter intersect(Filter newFilter) {
        log.debug("Intersect " + this.toString() + " with " + newFilter.toString());

        // Intersection of fields means updated type
        this.updateType(newFilter.fieldType());
        // .. and layer
        this.updateLayer(newFilter.fieldLayer());

        this.filter.intersectSegmentList(newFilter.filter);
        this.toSubtract.addSegmentList(newFilter.toSubtract);

        return this;
    }

    /**
     * Computes the difference between this and the input filter.
     *
     * @param newFilter filter to differentiate from
     * @return updated filter
     */
    public Filter differentiate(Filter newFilter) {
        log.debug("Differentiate: " + this.toString() + " from " + newFilter.toString());

        // Difference of fields means updated type
        this.updateType(newFilter.fieldType());
        // .. and layer
        this.updateLayer(newFilter.fieldLayer());

        this.toSubtract.addSegmentList(newFilter.filter);

        return this;
    }

    /**
     * Create an empty filter.
     */
    public void makeNone() {
        this.filter     = new DisjointSegmentList();
        this.toSubtract = new DisjointSegmentList();
    }

    /**
     * Translates this filter into a format understandable
     * by Click's IPClassfier element.
     *
     * @return translated filter
     */
    public String toIpClassifierPattern() {
        String keyword = "";
        String output  = "";

        if (this.headerField == HeaderField.AMBIGUOUS) {
            // ip[POS:LEN] match of IPClassifier
            String posStr = "[" + String.valueOf(this.fieldPosition) +
                            ":" + String.valueOf(this.fieldLength) + "]";
            keyword = ClassificationSyntax.PATTERN_IP_POS + posStr;
        } else if (this.headerField == HeaderField.ETHER_SRC) {
            keyword = ClassificationSyntax.PATTERN_SRC_ETHER_HOST + " ";
            output = this.ethernetMatchToIpFilterPattern(keyword);
            return output;
        } else if (this.headerField == HeaderField.ETHER_DST) {
            keyword = ClassificationSyntax.PATTERN_DST_ETHER_HOST + " ";
            output = this.ethernetMatchToIpFilterPattern(keyword);
            return output;
        } else if (this.headerField == HeaderField.ICMP_TYPE) {
            keyword = ClassificationSyntax.PATTERN_ICMP_TYPE + " ";
        } else if (this.headerField == HeaderField.IP_CE) {
            if (this.match(1)) {
                return ClassificationSyntax.PATTERN_IP_CE;
            } else {
                return this.negateExpression(ClassificationSyntax.PATTERN_IP_CE);
            }
        } else if (this.headerField == HeaderField.IP_ECT) {
            if (this.match(1)) {
                return ClassificationSyntax.PATTERN_IP_ECT;
            } else {
                return this.negateExpression(ClassificationSyntax.PATTERN_IP_ECT);
            }
        } else if (this.headerField == HeaderField.IP_DSCP) {
            keyword = ClassificationSyntax.PATTERN_IP_DSCP + " ";
        } else if (this.headerField == HeaderField.IP_DST) {
            keyword = ClassificationSyntax.PATTERN_DST_NET + " ";
            return ipFilterToIpClassifierPattern(keyword);
        } else if (this.headerField == HeaderField.IP_ID) {
            keyword = ClassificationSyntax.PATTERN_IP_ID + " ";
        } else if (this.headerField == HeaderField.IP_IHL) {
            keyword = ClassificationSyntax.PATTERN_IP_HDR_LEN + " ";
        } else if (this.headerField == HeaderField.IP_PROTO) {
            keyword = ClassificationSyntax.PATTERN_IP_PROTO + " ";
        } else if (this.headerField == HeaderField.IP_SRC) {
            keyword = ClassificationSyntax.PATTERN_SRC_NET + " ";
            return ipFilterToIpClassifierPattern(keyword);
        } else if (this.headerField == HeaderField.IP_TTL) {
            keyword = ClassificationSyntax.PATTERN_IP_TTL + " ";
        } else if (this.headerField == HeaderField.IP_VERS) {
            keyword = ClassificationSyntax.PATTERN_IP_VERS + " ";
        } else if (this.headerField == HeaderField.TCP_FLAGS_ACK) {
            if (this.match(1)) {
                return  ClassificationSyntax.PATTERN_TCP_OPT + " " +
                        ClassificationSyntax.TCP_OPT_ACK;
            } else {
                return this.negateExpression(
                    ClassificationSyntax.PATTERN_TCP_OPT + " " + ClassificationSyntax.TCP_OPT_ACK
                );
            }
        } else if (this.headerField == HeaderField.TCP_FLAGS_FIN) {
            if (this.match(1)) {
                return  ClassificationSyntax.PATTERN_TCP_OPT + " " +
                        ClassificationSyntax.TCP_OPT_FIN;
            } else {
                return this.negateExpression(
                    ClassificationSyntax.PATTERN_TCP_OPT + " " + ClassificationSyntax.TCP_OPT_FIN
                );
            }

        } else if (this.headerField == HeaderField.TCP_FLAGS_PSH) {
            if (this.match(1)) {
                return  ClassificationSyntax.PATTERN_TCP_OPT + " " +
                        ClassificationSyntax.TCP_OPT_PSH;
            } else {
                return this.negateExpression(
                    ClassificationSyntax.PATTERN_TCP_OPT + " " + ClassificationSyntax.TCP_OPT_PSH
                );
            }
        } else if (this.headerField == HeaderField.TCP_FLAGS_RST) {
            if (this.match(1)) {
                return  ClassificationSyntax.PATTERN_TCP_OPT + " " +
                        ClassificationSyntax.TCP_OPT_RST;
            } else {
                return this.negateExpression(
                    ClassificationSyntax.PATTERN_TCP_OPT + " " + ClassificationSyntax.TCP_OPT_RST
                );
            }
        } else if (this.headerField == HeaderField.TCP_FLAGS_SYN) {
            if (this.match(1)) {
                return  ClassificationSyntax.PATTERN_TCP_OPT + " " +
                        ClassificationSyntax.TCP_OPT_SYN;
            } else {
                return this.negateExpression(
                    ClassificationSyntax.PATTERN_TCP_OPT + " " + ClassificationSyntax.TCP_OPT_SYN
                );
            }
        } else if (this.headerField == HeaderField.TCP_FLAGS_URG) {
            if (this.match(1)) {
                return  ClassificationSyntax.PATTERN_TCP_OPT + " " +
                        ClassificationSyntax.TCP_OPT_URG;
            } else {
                return this.negateExpression(
                    ClassificationSyntax.PATTERN_TCP_OPT + " " + ClassificationSyntax.TCP_OPT_URG
                );
            }
        } else if (this.headerField == HeaderField.TCP_WIN) {
            keyword = ClassificationSyntax.PATTERN_TCP_WIN + " ";
        } else if (this.headerField == HeaderField.TP_DST_PORT) {
            keyword = ClassificationSyntax.PATTERN_DST_PORT + " ";
        } else if (this.headerField == HeaderField.TP_SRC_PORT) {
            keyword = ClassificationSyntax.PATTERN_SRC_PORT + " ";
        } else {
            throw new IllegalArgumentException(
                "Cannot convert header " + headerField.toString() +
                " filter to IPClassifier pattern"
            );
        }

        List<Segment> segments = this.filter.segments();

        for (Segment seg : segments) {
            // FIXME: handle IP subnets differently
            if (seg.getKey().equals(seg.getValue())) {
                output += "(" + keyword + String.valueOf(seg.getKey()) + ") || ";
            } else {
                // Lower bound is zero
                if (seg.getKey().equals(new Long((long) 0))) {
                    output += keyword + "<= " + String.valueOf(seg.getValue());
                } else {
                    // Upper bound is the floor
                    if (seg.getValue() == Constants.HEADER_FIELD_UPPER_BOUND.get(this.headerField)) {
                        output += keyword + ">= " + String.valueOf(seg.getKey());
                    } else {
                        output += "(" + keyword + ">= " + String.valueOf(seg.getKey()) + " && "
                                + keyword + "<= " + String.valueOf(seg.getValue()) + ")";
                    }
                }
                output += " || ";
            }
        }

        // Removes trailing  " || "
        if (segments.size() > 0) {
            output = output.substring(0, output.length() - 4);
        }

        // Lazy subtraction
        if (!toSubtract.isEmpty()) {
            segments = toSubtract.segments();

            if (segments.size() == 0) {
                return output;
            }

            output += " && !(";

            for (Segment seg : segments) {
                // TODO: handle IP subnets differently
                if (seg.getKey().equals(seg.getValue())) {
                    output += "(" + keyword + String.valueOf(seg.getKey()) + ") || ";
                } else {
                    output += "(" + keyword + ">= " + String.valueOf(seg.getKey()) + " && "
                            + keyword + "<= " + String.valueOf(seg.getValue()) + ") || ";
                }
            }

            // Removes trailing  " || "
            output = output.substring(0, output.length() - 4);
            output += ")";
        }

        return output;
    }

    /**
     * Algorithm to decompose interval in prefixes: we take the biggest possible prefix
     * containing lower and whose bounds are <= upper, then we keep going on the rest.
     * This would go quicker if we could detect !() patterns.
     *
     * @param keyword the keyword to translate
     * @param lower lower IP prefix
     * @param upper upper IP prefix
     * @return an IPClassifer's pattern
     */
    private String ipSegmentToIpClassifierPattern(String keyword, long lower, long upper) {
        String output  = "";
        long currentLow = lower;

        boolean atLeastOne = false;

        while (currentLow <= upper) {
            long prefixSize = IpPrefix.MAX_INET_MASK_LENGTH;

            while ((prefixSize > 0) &&
                   ((currentLow >> (IpPrefix.MAX_INET_MASK_LENGTH - prefixSize)) % 2 == 0) &&
                   (currentLow + (Constants.MAX_UINT >> (prefixSize - 1)) <= upper)) {
                prefixSize--;
            }

            // TODO: Check the correctness
            IpAddress ip = IpAddress.valueOf((int) currentLow);
            output += "(" + keyword + ip.toString() + "/" + String.valueOf(prefixSize) + ") || ";
            atLeastOne = true;

            if (prefixSize == IpPrefix.MAX_INET_MASK_LENGTH) {
                currentLow++;
            } else {
                currentLow += (Constants.MAX_UINT >> prefixSize) + 1;
            }

            if (currentLow == 0) {
                break;
            }
        }

        // Remove the trailing " || "
        if (atLeastOne) {
            output = output.substring(0, output.length() - 4);
        }

        return output;
    }

    /**
     * Translates a (complex) condition on IP src/dst address fields to
     * a format understandable by Click's IPClassfier element.
     *
     * @param keyword the keyword to translate
     * @return translated keyword
     */
    public String ipFilterToIpClassifierPattern(String keyword) {
        String output = "";
        List<Segment> segments = this.filter.segments();

        if (segments.isEmpty()) {
            return output;
        }

        for (Segment seg : segments) {
            output += this.ipSegmentToIpClassifierPattern(
                        keyword, seg.getKey(), seg.getValue()
                    ) + " || ";
        }

        // Remove the trailing " || "
        output = output.substring(0, output.length() - 4);

        if (!this.toSubtract.isEmpty()) {
            segments = this.toSubtract.segments();

            if (segments.isEmpty()) {
                return output;
            }

            output += " && !(";

            for (Segment seg : segments) {
                output += this.ipSegmentToIpClassifierPattern(
                            keyword, seg.getKey(), seg.getValue()
                        ) + " || ";
            }

            // Remove the trailing " || "
            output  = output.substring(0, output.length() - 4);
            output += ")";
        }

        return output;
    }

    /**
     * Translates a (complex) condition on IP src/dst address fields to
     * a format understandable by DPDK's Flow API classifiers.
     *
     * @param keyword the keyword to translate
     * @return translated keyword
     */
    public String ipFilterToFlowDirectorPattern(String keyword) {
        String output = "";

        // Get all the segments of the filter
        List<Segment> segments = this.filter.segments();
        if (segments.isEmpty()) {
            return output;
        }

        for (Segment seg : segments) {
            output += this.ipSegmentToIpClassifierPattern(
                keyword, seg.getKey(), seg.getValue()
            ) + " || ";
        }

        // Remove the trailing " || "
        return output.substring(0, output.length() - 4);
    }

    /**
     * Returns the updated filter type by combining the current type
     * with the input one.
     *
     * @param newType the type of the traffic class to be merged with
     */
    private void updateType(TrafficClassType newType) {
        this.fieldType = TrafficClassType.updateType(this.fieldType, newType);
    }

    /**
     * Returns the updated filter layer by combining the current layer
     * with the input one.
     *
     * @param newLayer the layer of the traffic class to be merged with
     */
    private void updateLayer(ProcessingLayer newLayer) {
        this.fieldLayer = ProcessingLayer.updateLayer(this.fieldLayer, newLayer);
    }

    /**
     * Translates an Ethernet address filter to a valid IPFilter configuration.
     *
     * @param keyword the keyword that denotes an Ethernet address match
     *        e.g., src/dst ether host
     * @return translated filter
     */
    private String ethernetMatchToIpFilterPattern(String keyword) {
        String output = "";
        List<Segment> segments = this.filter.segments();

        for (Segment seg : segments) {
            if (seg.getKey().equals(seg.getValue())) {
                output += "(" + keyword + MacAddress.valueOf((seg.getKey().longValue())) + ") || ";
            }
        }

        // Removes trailing  " || "
        if (segments.size() > 0) {
            output = output.substring(0, output.length() - 4);
        }

        return output;
    }

    /**
     * Returns a negated version of the input expression.
     *
     * @param expression the expression to be negated
     * @return negated expression
     */
    private String negateExpression(String expression) {
        return "!(" + expression + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.headerField,
            this.fieldPosition,
            this.fieldLength,
            this.filter,
            this.toSubtract
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof Filter))) {
            return false;
        }

        Filter other = (Filter) obj;

        DisjointSegmentList lhsDsl = this.filter;
        lhsDsl.subtractSegmentList(this.toSubtract);

        DisjointSegmentList rhsDsl = other.filter;
        rhsDsl.subtractSegmentList(other.toSubtract);

        return (this.headerField == other.headerField && lhsDsl.equals(rhsDsl));
    }

    @Override
    public String toString() {
        String result = "Filter on " + this.headerField.toString() + ": ";

        if (HeaderField.isIpAddress(this.headerField)) {
            return result + this.filter.toIpString();
        }

        return result + this.filter.toString();
    }

}
