/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.mantis.api.common;

import com.google.common.base.Strings;
import java.util.concurrent.ConcurrentSkipListMap;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Java generics useful for the modules of this NFV library.
 */
public class Common {

    private static final Logger log = getLogger(Common.class);

    /**
     * No need for a constructor, methods are static.
     */
    protected Common() {}

    /**
     * Returns an enumeration type that matches the input string or NULL.
     *
     * @param <E> the expected class of the enum
     * @param enumType the enum to be translated
     * @param s the string to indicate the translation
     * @return E enumeration type
     */
    public static <E extends Enum<E>> E enumFromString(Class<E> enumType, String s) {
        for (E en : EnumSet.allOf(enumType)) {
            if (en.toString().equals(s.toLowerCase())) {
                return en;
            }
        }

        return null;
    }

    /**
     * Returns all the enumeration's types in a set.
     *
     * @param <E> the expected class of the enum
     * @param enumType the enum class to get its types
     * @return Set with all enumeration types
     */
    public static <E extends Enum<E>> Set<String> enumTypes(Class<E> enumType) {
        LinkedList<String> list = new LinkedList<String>();
        for (E en : EnumSet.allOf(enumType)) {
            list.add(en.toString());
        }

        return list.stream().collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Returns all the enumeration's types in a space-separated string.
     *
     * @param <E> the expected class of the enum
     * @param enumType the enum class to get its types
     * @return String with all enumeration types
     */
    public static <E extends Enum<E>> String enumTypesToString(Class<E> enumType) {
        String allTypes = "";
        for (E en : EnumSet.allOf(enumType)) {
            allTypes += en.toString() + " ";
        }

        return allTypes.trim();
    }

    /**
     * Generates and returns a URI out of an input string.
     * Serves for printing.
     *
     * @param uriStr the input string from where the URI is generated
     * @return URI generated from string
     */
    public static URI uriFromString(String uriStr) {
        URI uri = null;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException sEx) {
            return null;
        }

        return uri;
    }

    /**
     * Sorts a map by the key either in ascending or descending order.
     *
     * @param <K> the expected class of the map's key
     * @param <V> the expected class of the map's value
     * @param map the map to be sorted
     * @param ascending the sorting order
     * @return sorted map
     */
    public static <K, V extends Comparable<V>> Map<K, V> sortMapByValues(
            final Map<K, V> map, int ascending) {
        Comparator<K> valueComparator = new Comparator<K>() {
            private int ascending;

            public int compare(K k1, K k2) {
                int compare = map.get(k2).compareTo(map.get(k1));
                if (compare == 0) {
                    return 1;
                } else {
                    return ascending * compare;
                }
            }

            public Comparator<K> setParam(int ascending) {
                this.ascending = ascending;
                return this;
            }
        }.setParam(ascending);

        Map<K, V> sortedByValues = new ConcurrentSkipListMap<K, V>(valueComparator);
        sortedByValues.putAll(map);

        return sortedByValues;
    }

    /**
     * Returns the index in a string (str) where we met a character different from
     * the ones contained in the pattern (pat).
     * This index occurs at or after the position pos.
     *
     * @param str the string we want to search
     * @param pat the patterns we want to avoid
     * @param pos the index after which we want to search
     * @return string index
     */
    public static int findFirstNotOf(String str, String pat, int pos) {
        for (int i = 0; i < str.length(); i++) {
            if (i < pos) {
                continue;
            }

            if (!pat.contains(String.valueOf(str.charAt(i)))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Converts a string into an unsigned integer.
     *
     * @param str the string we want to convert
     * @return unsigned int representation of the string
     */
    public static int stringToUnsignedInt(String str) {
        checkArgument(
            !Strings.isNullOrEmpty(str),
            "Cannot convert empty/NULL string to unsigned integer"
        );

        return Integer.parseUnsignedInt(str);
    }

    /**
     * Converts a normal string or an IP string into an unsigned integer.
     *
     * @param str the string we want to convert
     * @return unsigned int representation of the string
     */
    public static long stringOrIpToUnsignedInt(String str) {
        checkArgument(
            !Strings.isNullOrEmpty(str),
            "Cannot convert empty/NULL string to unsigned integer"
        );

        if (str.contains(".")) {
            return stringIpToInt(str);
        }

        return (long) Integer.parseUnsignedInt(str);
    }

    /**
     * Converts a signed integer to its unsigned format.
     *
     * @param signed int to be converted
     * @return unsigned version of this integer
     */
    public static long intToUnsignedInt(int signed) {
        return signed & Constants.MAX_UINT;
    }

    /**
     * Converts a string-based IP address into an unsigned integer.
     *
     * @param address the string-based IP we want to convert
     * @return unsigned int representation of the IP address
     */
    public static long stringIpToInt(String address) {
        long result = 0;

        String[] ipAddressInArray = address.split("\\.");

        checkArgument(
            ipAddressInArray.length == 4,
            "Malformed IP address " + address
        );

        for (int i = 3; i >= 0; i--) {

            long ip = Long.parseLong(ipAddressInArray[3 - i]);

            /**
             * Left shifting 24,16,8,0 and bitwise OR.
             * |-> 1st byte << 24
             * |->  2nd byte << 16
             * |->  3rd byte << 8
             * |->  4th byte << 0
             */
            result |= ip << (i * 8);

        }

        return result;
    }

    /**
     * Converts an integer-based IP address into a string.
     *
     * @param ip the integer-based IP we want to convert
     * @return string representation of the IP address
     */
    public static String intIpToString(long ip) {
        checkArgument(
            (ip >= 0) && (ip <= Constants.MAX_UINT),
            "The integer representation of the IP address " + ip + " is malformed"
        );

        return (
            ((ip >> 24) & 0xFF) + "." +
            ((ip >> 16) & 0xFF) + "." +
            ((ip >>  8) & 0xFF) + "." +
            (ip & 0xFF)
        );
    }

    /**
     * Converts a single string of arguments into a list.
     * The basic rule is to slpit based on commas.
     * We also pay attention to avoid comments!
     *
     * @param argStr the string to split
     * @return list of strings
     */
    public static List<String> separateArguments(String argStr) {
        List<String> arguments = new ArrayList<String>();
        String currentArg = "";
        int size = argStr.length();
        int position = 0;
        String spaces = " \t\n";

        while (position <= size - 1) {
            switch (argStr.charAt(position)) {
                case ' ':
                case '\t':
                case '\n': {
                    currentArg += " ";
                    position = findFirstNotOf(argStr, spaces, position);
                    break;
                }
                // New argument appears after comma
                case ',':
                    arguments.add(currentArg);
                    currentArg = "";
                    position = findFirstNotOf(argStr, spaces, position + 1);
                    break;
                // Comments
                case '/':
                    // A C-based comment begins
                    if ((position < size) && (argStr.charAt(position + 1) == '*')) {
                        position++;
                        do {
                            position = argStr.indexOf('*', position);
                        } while ((position < size) && (argStr.charAt(++position) != '/'));
                        position++;
                    // A CPP-based comment begins
                    } else if ((position < size) && (argStr.charAt(position + 1) == '/')) {
                        position = argStr.indexOf('\n', position) + 1;
                    // Single slash might be a prefix definition, keep it!
                    } else {
                        currentArg += argStr.charAt(position);
                        position++;
                    }
                    break;
                default:
                    currentArg += argStr.charAt(position);
                    position++;
                    break;
            }

            if ((position < 0) || (position >= size)) {
                break;
            }
        }

        if (!currentArg.isEmpty()) {
            arguments.add(currentArg);
        }

        return arguments;
    }

}
