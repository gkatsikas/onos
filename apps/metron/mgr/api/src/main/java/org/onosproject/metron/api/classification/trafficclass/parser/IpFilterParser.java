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
package org.onosproject.metron.api.classification.trafficclass.parser;

import org.onosproject.metron.api.classification.ClassificationSyntax;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.exceptions.ParseException;
import org.onosproject.metron.api.exceptions.SynthesisException;

import org.slf4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Parsing library for IPFilter.
 */
public class IpFilterParser {

    private static final Logger log = getLogger(IpFilterParser.class);

    /**
     * No need for a constructor, methods are static.
     */
    protected IpFilterParser() {}

    /**
     * Adds a new filter to a packet filter.
     *
     * @param basePf the packet filter to be extended
     * @param f the filter to be added
     * @return boolean status
     */
    public static boolean addFilterToPacketFilter(PacketFilter basePf, Filter f) {
        if ((basePf == null) || (f == null)) {
            throw new SynthesisException(
                "Failed to add filter into packet filter. At least one of them is NULL"
            );
        }

        HeaderField headerField = f.headerField();

        if (basePf.containsKey(headerField)) {
            Filter filter = basePf.get(headerField);
            filter = filter.intersect(f);
            basePf.put(headerField, filter);
        } else {
            basePf.put(headerField, f);
        }

        return basePf.get(headerField).isNone();
    }

    /**
     * Negates the filters of a packet filter.
     *
     * @param pf the input packet filter
     * @return list of negated packet filters
     */
    public static List<PacketFilter> negatePacketFilter(PacketFilter pf) {
        if (pf == null) {
            throw new SynthesisException(
                "Cannot negate a NULL packet filter"
            );
        }

        List<PacketFilter> pfList = new ArrayList<PacketFilter>();

        for (Map.Entry<HeaderField, Filter> entry : pf.entrySet()) {
            HeaderField headerField = entry.getKey();
            Filter filter = entry.getValue();

            Filter newFilter = new Filter(headerField);
            newFilter = newFilter.differentiate(filter);

            // Construct the negated packet filter
            PacketFilter newPf = new PacketFilter(headerField, newFilter);

            if (!newPf.get(headerField).isNone()) {
                pfList.add(newPf);
            }
        }

        return pfList;
    }

    /**
     * Constructs a packet filter list after merging two input ones.
     *
     * @param a the first packet filter list
     * @param b the second packet filter list
     * @return the merged packet filter list
     */
    public static List<PacketFilter> mergePacketFilterLists(List<PacketFilter> a, List<PacketFilter> b) {
        if ((a == null) || (b == null)) {
            throw new SynthesisException(
                "Failed to merge packet filters. At least one of them is NULL"
            );
        }

        List<PacketFilter> result = new ArrayList<PacketFilter>();

        int isNone;
        PacketFilter tempPf = new PacketFilter();

        for (PacketFilter pfOut : a) {
            for (PacketFilter pfIn : b) {
                tempPf = new PacketFilter(pfOut);
                isNone = 0;

                for (Filter f : pfIn.values()) {
                    if (!addFilterToPacketFilter(tempPf, f)) {
                        isNone++;
                    }
                }

                if (isNone > 0) {
                    result.add(tempPf);
                }
            }
        }

        return result;
    }

    /**
     * Constructs a packet filter list after negating the filters of an input one.
     *
     * @param pfList the input packet filter list
     * @return the negated packet filter list
     */
    public static List<PacketFilter> negatePacketFilterList(List<PacketFilter> pfList) {
        if (pfList == null) {
            throw new SynthesisException(
                "Cannot negate a NULL packet filter list"
            );
        }

        if (pfList.isEmpty()) {
            throw new SynthesisException(
                "Cannot negate an empty packet filter list"
            );
        }

        List<PacketFilter> result = new ArrayList<PacketFilter>();
        result.add(new PacketFilter());

        for (PacketFilter pf : pfList) {
            result = mergePacketFilterLists(result, negatePacketFilter(pf));
        }

        return result;
    }

    /**
     * Resets the contents of a packet filter list.
     *
     * @param pfList the input packet filter list
     */
    public static void resetPacketFilterList(List<PacketFilter> pfList) {
        if (pfList == null) {
            log.warn("\tReseting a NULL packet list by creating a fresh one");
            pfList = new ArrayList<PacketFilter>();
        }

        pfList.clear();
        pfList.add(new PacketFilter());
    }

    /**
     * Converts a packet filter list into a string format.
     *
     * @param pfList the input packet filter list
     * @return the string format of the input packet filter list
     */
    public static String packetFilterListToString(List<PacketFilter> pfList) {
        if (pfList == null) {
            throw new SynthesisException(
                "Cannot convert NULL packet filter list into string"
            );
        }

        if (pfList.size() == 0) {
            log.warn("\tEmpty list of packet filters");
            return "";
        }

        String pfStr = "";

        for (PacketFilter pf : pfList) {
            pfStr += pf.toString() + "\n";
        }

        return pfStr;
    }

    /**
     * Converts a string-based IPFilter line into a list of packet filters.
     *
     * @param line the string-based IPFilter rule
     * @param position the current index in this line
     * @param end the last index in this line
     * @return list of packet filters
     */
    public static List<PacketFilter> filtersFromSubstr(char[] line, AtomicInteger position, int end) {
        /*
         * TODO:
         * - support patterns that are not of the type "primitive option [value]"
         * - Add a PacketFilter containing the current filter and wait for "or" that
         *   changes the primitive to push it into openFilters.
         */
        List<PacketFilter> finishedFilters = new ArrayList<PacketFilter>();
        List<PacketFilter> openFilters = new ArrayList<PacketFilter>();
        resetPacketFilterList(openFilters);

        if (position.get() == end) {
            finishedFilters.addAll(openFilters);
            return finishedFilters;
        }

        String currentWord = "";
        boolean negate = false;
        Primitive currPrim = Primitive.UNDEFINED;
        Primitive currOperator = Primitive.AND;
        Option currOpt = Option.UNDEFINED;

        while ((position.get() < end) && (line[position.get()] != ')')) {
            // String left = String.valueOf(line);
            // left = left.substring(position.get(), end);
            // log.info(
            //     "\tConsidering character {} and current word {} from {}",
            //     line[position.get()], currentWord, left
            // );

            if (line[position.get()] == '!') {
                // log.info("Position: {}/{} --> Character {}", position.get(), end, line[position.get()]);
                negate = true;
                position.incrementAndGet();
            } else if (line[position.get()] == '(') {
                position.incrementAndGet();

                List<PacketFilter> pfList = filtersFromSubstr(line, position, end);

                if (negate) {
                    pfList = negatePacketFilterList(pfList);
                    negate = false;
                }

                if (currOperator == Primitive.AND) {
                    openFilters = mergePacketFilterLists(openFilters, pfList);
                } else if (currOperator == Primitive.OR) {
                    finishedFilters.addAll(openFilters);
                    openFilters = pfList;
                } else {
                    throw new ParseException(currOperator + " is not an operator");
                }

                currOperator = Primitive.UNDEFINED;
                currPrim = Primitive.UNDEFINED;
                currOpt = Option.UNDEFINED;

                // Go past blank space
                position.incrementAndGet();
            } else if (line[position.get()] == ' ') {
                position.incrementAndGet();

                if (currOperator == Primitive.UNDEFINED) {
                    currOperator = Primitive.getByName(currentWord);
                    // log.info("Operator {} from word {}", currOperator, currentWord);
                    if (!Primitive.isOperator(currOperator)) {
                        throw new ParseException(currentWord + " is not an operator");
                    }
                // Current word is a primitive
                } else if (currPrim == Primitive.UNDEFINED) {
                    currPrim = Primitive.getByName(currentWord);
                    // log.info("Primitive {} from word {}", currPrim, currentWord);
                // Current word is an option
                } else if (currOpt == Option.UNDEFINED) {
                    currOpt = optionFromString(currPrim, currentWord);
                    checkNotNull(currOpt, "Option is NULL");
                    // log.info("Option {} from word {}", currOpt, currentWord);

                    String value = "";

                    // We expect a value OR an operator primitive
                    if (Option.hasNoValue(currOpt)) {
                        // do nothing
                    // We expect port followed by value (transport port)
                    } else if (Option.isPort(currOpt)) {
                        currentWord = matchAndAdvance(line, position, end, currOpt, "port");
                        value = parseValue(line, position, end);
                    // We expect host followed by value (MAC address)
                    } else if (Option.isEthernet(currOpt)) {
                        currentWord = matchAndAdvance(line, position, end, currOpt, "host");
                        value = parseValue(line, position, end);
                    } else {
                        value = parseValue(line, position, end);
                    }

                    PacketFilter pf = filterFromOption(currPrim, currOpt, value);
                    List<PacketFilter> temp = new ArrayList<PacketFilter>();
                    if (negate) {
                        temp = negatePacketFilter(pf);
                        negate = false;
                    } else {
                        temp.add(pf);
                    }

                    if (currOperator == Primitive.OR) {
                        finishedFilters.addAll(openFilters);
                        resetPacketFilterList(openFilters);
                        openFilters.add(0, pf);
                    } else if (currOperator == Primitive.AND) {
                        openFilters = mergePacketFilterLists(openFilters, temp);
                    } else {
                        throw new ParseException("Expected operator AND/OR instead of " + currOperator);
                    }

                    currOperator = Primitive.UNDEFINED;
                    currPrim = Primitive.UNDEFINED;
                    currOpt = Option.UNDEFINED;
                }

                currentWord = "";
            } else {
                currentWord += String.valueOf(line[position.get()]);
                position.incrementAndGet();
            }
        }

        position.incrementAndGet();
        finishedFilters.addAll(openFilters);

        return finishedFilters;
    }

    /**
     * Wraps the convertion of a string-based IPFilter line into a list of packet filters.
     *
     * @param line the string-based IPFilter rule
     * @return list of packet filters
     */
    public static List<PacketFilter> filtersFromIpFilterLine(String line) {
        if (line.isEmpty()) {
            throw new ParseException("Empty IPFilter configuration");
        }

        // First replace (&& --> AND) and (|| --> OR)
        line = line.replace("&&", ClassificationSyntax.LOGICAL_AND);
        line = line.replace("||", ClassificationSyntax.LOGICAL_OR);

        return filtersFromSubstr(line.toCharArray(), new AtomicInteger(0), line.length());
    }

    /**
     * Translates a (part of) a rule into a packet filter.
     *
     * @param primitive the primitive operation of this rule
     * @param option the primitive option of this rule
     * @param arg the rule as a string
     * @return a packet filter that derives from this rule
     */
    private static PacketFilter filterFromOption(Primitive primitive, Option option, String arg) {
        // log.info("\tPrimitive {} - Option {} - Value {}", primitive, option, arg);

        if (primitive == Primitive.IP) {
            return filterFromIpOption(option, arg);
        } else if (primitive == Primitive.SRC) {
            return filterFromSrcOption(option, arg);
        } else if (primitive == Primitive.DST) {
            return filterFromDstOption(option, arg);
        } else if (primitive == Primitive.TCP) {
            return filterFromTcpOption(option, arg);
        } else if (primitive == Primitive.ICMP) {
            return filterFromIcmpOption(option, arg);
        } else {
            throw new ParseException(
                "Cannot translate primitive " + primitive + " from option " + option
            );
        }
    }

    /**
     * Translates a (part of) a rule into an IP packet filter.
     *
     * @param option the primitive option of this rule
     * @param arg the rule as a string
     * @return a packet filter that derives from this rule
     */
    private static PacketFilter filterFromIpOption(Option option, String arg) {
        PacketFilter pf = new PacketFilter();
        Filter f = null;

        if (option == Option.IP_VERS) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_VERS, arg);
        } else if (option == Option.IP_IHL) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_IHL, arg);
        } else if (option == Option.IP_TOS) {
            throw new ParseException("\tIP ToS not supported, please use IP DSCP and IP ECT/CE");
        } else if (option == Option.IP_DSCP) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_DSCP, arg);
        } else if (option == Option.IP_ID) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_ID, arg);
        } else if (option == Option.IP_CE) {
            f = new Filter(HeaderField.IP_CE, 1);
        } else if ((option == Option.IP_FRAG) || (option == Option.IP_UNFRAG)) {
            throw new ParseException("\tIP fragmentation not supported yet");
        } else if (option == Option.IP_ECT) {
            f = new Filter(HeaderField.IP_ECT, 1);
        } else if (option == Option.IP_TTL) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_TTL, arg);
        } else if (option == Option.IP_PROTO) {
            if        (arg.equals(ClassificationSyntax.IPV4_PROTO_ICMP)) {
                f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_ICMP);
            } else if (arg.equals(ClassificationSyntax.IPV4_PROTO_TCP)) {
                f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_TCP);
            } else if (arg.equals(ClassificationSyntax.IPV4_PROTO_UDP)) {
                f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_UDP);
            } else if (Common.findFirstNotOf(arg, "0123456789", 0) < 0) {
                f = Filter.fromIpClassifierPattern(HeaderField.IP_PROTO, arg);
            } else {
                throw new ParseException("\tUnknown protocol name " + arg);
            }
        } else {
            throw new ParseException("\tInvalid option " + option + " for IP primitive");
        }

        addFilterToPacketFilter(pf, f);

        return pf;
    }

    /**
     * Translates a (part of) a rule into an ICMP packet filter.
     *
     * @param option the primitive option of this rule
     * @param arg the rule as a string
     * @return a packet filter that derives from this rule
     */
    private static PacketFilter filterFromIcmpOption(Option option, String arg) {
        PacketFilter pf = new PacketFilter();
        Filter f = null;

        if (option == Option.ICMP_TYPE) {
            f = Filter.fromIpClassifierPattern(HeaderField.ICMP_TYPE, arg);
        } else {
            f = new Filter();
        }

        addFilterToPacketFilter(pf, f);

        return pf;
    }

    /**
     * Translates a (part of) a rule into an TCP packet filter.
     *
     * @param option the primitive option of this rule
     * @param arg the rule as a string
     * @return a packet filter that derives from this rule
     */
    private static PacketFilter filterFromTcpOption(Option option, String arg) {
        PacketFilter pf = new PacketFilter(
            HeaderField.IP_PROTO,
            new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_TCP)
        );
        Filter f = null;

        if (option == Option.TCP_OPT) {
            if (arg.isEmpty()) {
                throw new ParseException("\tEmpty argument for TCP opt");
            }

            if (arg.equals(ClassificationSyntax.TCP_OPT_ACK)) {
                f = new Filter(HeaderField.TCP_FLAGS_ACK, 1);
            } else if (arg.equals(ClassificationSyntax.TCP_OPT_FIN)) {
                f = new Filter(HeaderField.TCP_FLAGS_FIN, 1);
            } else if (arg.equals(ClassificationSyntax.TCP_OPT_PSH)) {
                f = new Filter(HeaderField.TCP_FLAGS_PSH, 1);
            } else if (arg.equals(ClassificationSyntax.TCP_OPT_RST)) {
                f = new Filter(HeaderField.TCP_FLAGS_RST, 1);
            } else if (arg.equals(ClassificationSyntax.TCP_OPT_SYN)) {
                f = new Filter(HeaderField.TCP_FLAGS_SYN, 1);
            } else if (arg.equals(ClassificationSyntax.TCP_OPT_URG)) {
                f = new Filter(HeaderField.TCP_FLAGS_URG, 1);
            } else {
                throw new ParseException("\tUnknown TCP opt " + arg);
            }
        } else if (option == Option.TCP_WIN) {
            f = Filter.fromIpClassifierPattern(HeaderField.TCP_WIN, arg);
        } else {
            throw new ParseException("\tUnknown option " + option + " for TCP primitive");
        }

        addFilterToPacketFilter(pf, f);

        return pf;
    }

    /**
     * Translates a (part of) a rule with src option into a packet filter.
     *
     * @param option the primitive option of this rule
     * @param arg the rule as a string
     * @return a packet filter that derives from this rule
     */
    private static PacketFilter filterFromSrcOption(Option option, String arg) {
        PacketFilter pf = new PacketFilter();
        Filter f = null;

        if (option == Option.SRC_ETHER_HOST) {
            f = Filter.fromEthernetAddress(HeaderField.ETHER_SRC, arg);
        } else if (option == Option.SRC_HOST) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_SRC, arg);
        } else if (option == Option.SRC_NET) {
            f = Filter.fromPrefixPattern(HeaderField.IP_SRC, arg);
        } else if (option == Option.SRC_UDP_PORT) {
            f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_UDP);
            addFilterToPacketFilter(pf, f);
            f = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, arg);
        } else if (option == Option.SRC_TCP_PORT) {
            f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_TCP);
            addFilterToPacketFilter(pf, f);
            f = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, arg);
        } else if (option == Option.SRC_PORT) {
            f = Filter.fromIpClassifierPattern(HeaderField.TP_SRC_PORT, arg);
        } else {
            f = new Filter();
        }

        addFilterToPacketFilter(pf, f);

        return pf;
    }

    /**
     * Translates a (part of) a rule with dst option into a packet filter.
     *
     * @param option the primitive option of this rule
     * @param arg the rule as a string
     * @return a packet filter that derives from this rule
     */
    private static PacketFilter filterFromDstOption(Option option, String arg) {
        PacketFilter pf = new PacketFilter();
        Filter f = null;

        if (option == Option.DST_ETHER_HOST) {
            f = Filter.fromEthernetAddress(HeaderField.ETHER_DST, arg);
        } else if (option == Option.DST_HOST) {
            f = Filter.fromIpClassifierPattern(HeaderField.IP_DST, arg);
        } else if (option == Option.DST_NET) {
            f = Filter.fromPrefixPattern(HeaderField.IP_DST, arg);
        } else if (option == Option.DST_UDP_PORT) {
            f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_UDP);
            addFilterToPacketFilter(pf, f);
            f = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, arg);
        } else if (option == Option.DST_TCP_PORT) {
            f = new Filter(HeaderField.IP_PROTO, ClassificationSyntax.VALUE_PROTO_TCP);
            addFilterToPacketFilter(pf, f);
            f = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, arg);
        } else if (option == Option.DST_PORT) {
            f = Filter.fromIpClassifierPattern(HeaderField.TP_DST_PORT, arg);
        } else {
            f = new Filter();
        }

        addFilterToPacketFilter(pf, f);

        return pf;
    }

    /**
     * Translates a string-based option into an object.
     *
     * @param prim the primitive of this rule
     * @param optStr the option as a string
     * @return Option object that corresponds to optStr
     */
    public static Option optionFromString(Primitive prim, String optStr) {
        // log.info("Primitive {} - Option {}", prim, optStr);

        if (Primitive.hasProtocol(prim)) {
            return mappedOptionFromString(prim, optStr);
        } else if (Primitive.hasSrcDst(prim)) {
            return srcDstOptionFromString(prim, optStr);
        } else {
            throw new ParseException(
                "Undefined primitive " + prim + ", cannot parse option"
            );
        }
    }

    /**
     * Uses a direct map to translate IP, ICMP, and TCP string-based options.
     *
     * @param prim the primitive of this rule
     * @param optStr the option as a string
     * @return Option object that corresponds to optStr
     */
    private static Option mappedOptionFromString(Primitive prim, String optStr) {
        if (optStr.length() <= 0) {
            return Option.UNDEFINED;
        }

        Option opt = Option.getByName(prim + " " + optStr);
        if (opt != null) {
            return opt;
        }

        return Option.UNDEFINED;
    }

    /**
     * Handles src/dst string-based options.
     * Uses the first letter to classify linearly (avoiding costly comparisons).
     *
     * @param prim the primitive of this rule
     * @param optStr the option as a string
     * @return Option object that corresponds to optStr
     */
    private static Option srcDstOptionFromString(Primitive prim, String optStr) {
        if (optStr.length() <= 0) {
            return Option.UNDEFINED;
        }

        switch (optStr.charAt(0)) {
            case 'e': {
                if ((optStr.length() == 5) && (optStr.charAt(1) == 't') && (optStr.charAt(2) == 'h') &&
                    (optStr.charAt(3) == 'e') && (optStr.charAt(4) == 'r')) {
                    if (prim == Primitive.SRC) {
                        return Option.SRC_ETHER_HOST;
                    } else if (prim == Primitive.DST) {
                        return Option.DST_ETHER_HOST;
                    } else {
                        throw new ParseException(
                            "Unexpected primitive " + prim + " after ether keyword"
                        );
                    }
                }
                break;
            }
            case 'h': {
                if ((optStr.length() == 4) && (optStr.charAt(1) == 'o') && (optStr.charAt(2) == 's') &&
                    (optStr.charAt(3) == 't')) {
                    if (prim == Primitive.SRC) {
                        return Option.SRC_HOST;
                    } else if (prim == Primitive.DST) {
                        return Option.DST_HOST;
                    } else {
                        throw new ParseException(
                            "Unexpected primitive " + prim + " after host keyword"
                        );
                    }
                }
                break;
            }
            case 'n': {
                if ((optStr.length() == 3) && (optStr.charAt(1) == 'e') && (optStr.charAt(2) == 't')) {
                    if (prim == Primitive.SRC) {
                        return Option.SRC_NET;
                    } else if (prim == Primitive.DST) {
                        return Option.DST_NET;
                    } else {
                        throw new ParseException(
                            "Unexpected primitive " + prim + " after net keyword"
                        );
                    }
                }
                break;
            }
            case 'p': {
                if ((optStr.length() == 4) && (optStr.charAt(1) == 'o') && (optStr.charAt(2) == 'r') &&
                    (optStr.charAt(3) == 't')) {
                    if (prim == Primitive.SRC) {
                        return Option.SRC_PORT;
                    } else if (prim == Primitive.DST) {
                        return Option.DST_PORT;
                    } else {
                        throw new ParseException(
                            "Unexpected primitive " + prim + " after port keyword"
                        );
                    }
                }
                break;
            }
            case 't': {
                if ((optStr.length() == 3) && (optStr.charAt(1) == 'c') && (optStr.charAt(2) == 'p')) {
                    if (prim == Primitive.SRC) {
                        return Option.SRC_TCP_PORT;
                    } else if (prim == Primitive.DST) {
                        return Option.DST_TCP_PORT;
                    } else {
                        throw new ParseException(
                            "Unexpected primitive " + prim + " after tcp keyword"
                        );
                    }
                }
                break;
            }
            case 'u': {
                if ((optStr.length() == 3) && (optStr.charAt(1) == 'd') && (optStr.charAt(2) == 'p')) {
                    if (prim == Primitive.SRC) {
                        return Option.SRC_UDP_PORT;
                    } else if (prim == Primitive.DST) {
                        return Option.DST_UDP_PORT;
                    } else {
                        throw new ParseException(
                            "Unexpected primitive " + prim + " after udp keyword"
                        );
                    }
                }
                break;
            }
            default:
                break;
        }

        return Option.UNDEFINED;
    }

    /**
     * Parses raw input text to identify primitives and values.
     *
     * @param word the line we are working on
     * @param positionObj current position in this word
     * @param end expected terminal position to indicate end of parsing
     * @return a value separated from the primitives
     */
    private static String parseValue(char[] word, AtomicInteger positionObj, int end) {
        String currentWord = "";
        String value = "";
        String temp = "";
        int currentPosition = positionObj.get();

        while ((currentPosition != end) && (word[currentPosition] != ')')) {
            String left = String.valueOf(word);
            left = left.substring(currentPosition, end);

            if (word[currentPosition] == ' ') {
                Primitive p = Primitive.getByName(currentWord);
                /**
                 * Normal  case: p == null (We found a value after a primitive)
                 * Special case: We found a primitive but it is actually a value.
                 * E.g., `ip proto tcp` -> tcp is not the primitive but the value!
                 */
                if ((p == null) || (p == Primitive.ICMP) || (p == Primitive.TCP)) {
                    positionObj.set(currentPosition);
                    value += temp + currentWord + " ";
                    currentWord = "";
                    temp = "";
                // New primitive, end of the value
                } else if ((p != Primitive.UNDEFINED) && !Primitive.isOperator(p)) {
                    break;
                // We wait to see whether the next one is a primitive or not
                } else if (Primitive.isOperator(p)) {
                    temp = currentWord + " ";
                    currentWord = "";
                }
            } else if (!value.isEmpty() && isOpeningChar(word[currentPosition])) {
                break;
            } else {
                currentWord += String.valueOf(word[currentPosition]);
            }

            currentPosition++;
        }

        if ((currentPosition == end) || (word[currentPosition] == ')')) {
            value += temp + currentWord;
            positionObj.set(currentPosition);
        } else {
            // Reduce string length by one
            if (value.length() <= 1) {
                throw new ParseException(
                    "Expected a larger value but got " + value
                );
            }

            value = value.substring(0, value.length() - 1);
            positionObj.incrementAndGet();
        }

        log.debug("\tReturning value: {}", value);

        return value;
    }

    /**
     * Parses raw input text expecting to find a certain string.
     * After this function, the parser has advanced beyond that string.
     *
     * @param word the line we are working on
     * @param positionObj current position in this word
     * @param end expected terminal position to indicate end of parsing
     * @param currOpt the current Option of the parser
     * @param matchStr string we want to find as next word
     * @return a matched value
     * @throws ParseException if matchStr is not found
     */
    private static String matchAndAdvance(
            char[] word, AtomicInteger positionObj, int end,
            Option currOpt, String matchStr) {
        String desiredWord = "";
        int currentPosition = positionObj.get();

        while ((currentPosition < end) &&
               (word[currentPosition] != ')') &&
               (word[currentPosition] != ' ')) {
            desiredWord += String.valueOf(word[currentPosition]);
            positionObj.incrementAndGet();
            currentPosition = positionObj.get();
        }

        if (!desiredWord.equals(matchStr)) {
            throw new ParseException("Expected " + matchStr + " after " + currOpt);
        }
        positionObj.incrementAndGet();

        return desiredWord;
    }

    /**
     * Checks whether the input character is opening new parsing horizons.
     *
     * @param c input character to be checked
     * @return boolean status
     */
    private static boolean isOpeningChar(char c) {
        return (c == '(' || c == '!');
    }

}
