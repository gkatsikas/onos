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

package org.onosproject.metron.api.classification.trafficclass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A traffic class might consist of several rules.
 * This enumeration classifies a traffic class to
 * different protocols according to the mix of rules
 * this traffic class contains.
 */
public enum TrafficClassType {

    AMBIGUOUS("AMBIGUOUS"),
    ARP("ARP"),
    ETHERNET("ETHERNET"),
    ICMP("ICMP"),
    IP("IP"),
    NEUTRAL("NEUTRAL"),
    PAYLOAD("PAYLOAD"),
    TCP("TCP"),
    VLAN("VLAN"),
    UDP("UDP");

    private String trafficclassType;

    // Statically maps traffic class types to their weight
    private static final Map<TrafficClassType, Integer> WEIGHT =
        new ConcurrentHashMap<TrafficClassType, Integer>();

    static {
        WEIGHT.put(AMBIGUOUS, 6);
        WEIGHT.put(PAYLOAD,   5);
        WEIGHT.put(TCP,       4);
        WEIGHT.put(UDP,       4);
        WEIGHT.put(ICMP,      4);
        WEIGHT.put(IP,        3);
        WEIGHT.put(VLAN,      2);
        WEIGHT.put(ARP,       2);
        WEIGHT.put(ETHERNET,  1);
        WEIGHT.put(NEUTRAL,   0);
    }

    private TrafficClassType(String trafficclassType) {
        this.trafficclassType = trafficclassType;
    }

    /**
     * Creates a traffic class type based upon the
     * input header field.
     *
     * @param headerField header field out of which a
     *        traffic class is created
     * @return TrafficClassType created
     */
    public static TrafficClassType createByHeaderField(HeaderField headerField) {
        if (HeaderField.isEthernet(headerField)) {
            return ETHERNET;
        }

        if (HeaderField.isArp(headerField)) {
            return ARP;
        }

        if (HeaderField.isVlan(headerField)) {
            return VLAN;
        }

        if (HeaderField.isIp(headerField)) {
            return IP;
        }

        if (HeaderField.isIcmp(headerField)) {
            return ICMP;
        }

        if (HeaderField.isTcp(headerField)) {
            return TCP;
        }

        if (HeaderField.isUdp(headerField)) {
            return UDP;
        }

        if (HeaderField.isPayload(headerField)) {
            return PAYLOAD;
        }

        if (HeaderField.isAmbiguous(headerField)) {
            return AMBIGUOUS;
        }

        return NEUTRAL;
    }

    /**
     * Checks whether a traffic class type belongs to L2.
     * (i.e., resides below the IP layer).
     *
     * @param type the type of the traffic class to be checked
     * @return boolean status (belongs or not)
     */
    public static boolean isL2(TrafficClassType type) {
        if (type == ETHERNET) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether a traffic class type belongs to L3.
     * (i.e., resides at the IP layer).
     *
     * @param type the type of the traffic class to be checked
     * @return boolean status (belongs or not)
     */
    public static boolean isL3(TrafficClassType type) {
        if (type == IP) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether a traffic class type belongs to L4.
     * (i.e., resides above the IP layer, including ICMP).
     *
     * @param type the type of the traffic class to be checked
     * @return boolean status (belongs or not)
     */
    public static boolean isL4(TrafficClassType type) {
        if ((type == ICMP) || (type == TCP) || (type == UDP)) {
            return true;
        }

        return false;
    }

    /**
     * Computes the type of a traffic class based upon
     * its current type and the type it will be merged with.
     *
     * @param currentType the current type of a traffic class
     * @param newType the type of the traffic class to be merged with
     * @return the derived traffic class type
     */
    public static TrafficClassType updateType(
            TrafficClassType currentType, TrafficClassType newType) {

        TrafficClassType result = checkNull(currentType, newType);
        if (result != null) {
            return result;
        }

        result = checkIdentical(currentType, newType);
        if (result != null) {
            return result;
        }

        result = checkNeutral(currentType, newType);
        if (result != null) {
            return result;
        }

        result = checkAmbiguous(currentType, newType);
        if (result != null) {
            return result;
        }

        int currentWeight = WEIGHT.get(currentType).intValue();
        int newWeight = WEIGHT.get(newType).intValue();
        return (currentWeight >= newWeight) ? currentType : newType;
    }

    /**
     * Returns one traffic class if the other is NULL.
     * Returns NEUTRAL if both are NULL.
     *
     * @param currentType the current type of a traffic class
     * @param newType the type of the traffic class to be merged with
     * @return the derived traffic class type or NULL
     */
    private static TrafficClassType checkNull(
            TrafficClassType currentType, TrafficClassType newType) {
        if ((currentType == null) && (newType == null)) {
            // TODO: Unlikely to happen, but why?
            return NEUTRAL;
        }

        if (currentType == null) {
            return newType;
        }

        if (newType == null) {
            return currentType;
        }

        return null;
    }

    /**
     * Returns NULL if the input traffic classes differ.
     * Otherwise, returns one of them.
     *
     * @param currentType the current type of a traffic class
     * @param newType the type of the traffic class to be merged with
     * @return the derived traffic class type or NULL
     */
    private static TrafficClassType checkIdentical(
            TrafficClassType currentType, TrafficClassType newType) {
        if (currentType == newType) {
            return currentType;
        }

        return null;
    }

    /**
     * Returns one traffic class if the other is NEUTRAL.
     * Otherwise, returns NULL.
     *
     * @param currentType the current type of a traffic class
     * @param newType the type of the traffic class to be merged with
     * @return the derived traffic class type or NULL
     */
    private static TrafficClassType checkNeutral(
            TrafficClassType currentType, TrafficClassType newType) {
        if (currentType == NEUTRAL) {
            return newType;
        }

        if (newType == NEUTRAL) {
            return currentType;
        }

        return null;
    }

    /**
     * Returns AMBIGUOUS if either of the traffic classes is AMBIGUOUS.
     * Otherwise, returns NULL.
     *
     * @param currentType the current type of a traffic class
     * @param newType the type of the traffic class to be merged with
     * @return the derived traffic class type or NULL
     */
    private static TrafficClassType checkAmbiguous(
            TrafficClassType currentType, TrafficClassType newType) {
        if ((newType == AMBIGUOUS) &&
            ((currentType == UDP) || ((currentType == TCP)))) {
            return currentType;
        }

        if ((currentType == AMBIGUOUS) &&
            ((newType == UDP) || ((newType == TCP)))) {
            return newType;
        }

        if ((currentType == AMBIGUOUS) || (newType == AMBIGUOUS)) {
            return AMBIGUOUS;
        }

        return null;
    }

    @Override
    public String toString() {
        return trafficclassType;
    }

}
