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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parsing primitives for IPFilter.
 */
public enum Primitive {

    AND("AND"),
    DST("DST"),
    ICMP("ICMP"),
    IP("IP"),
    OR("OR"),
    SRC("SRC"),
    TCP("TCP"),
    UNDEFINED("UNDEFINED");

    private String primitive;

    // Statically maps primitives to enum types
    private static final Map<String, Primitive> MAP =
        new ConcurrentHashMap<String, Primitive>();

    static {
        for (Primitive pr : Primitive.values()) {
            MAP.put(pr.toString(), pr);
        }
    }

    private Primitive(String primitive) {
        this.primitive = primitive;
    }

    /**
     * Returns a Primitive object created by a string-based primitive name.
     *
     * @param prStr string-based primitive
     * @return Primitive object
     */
    public static Primitive getByName(String prStr) {
        return MAP.get(prStr.toUpperCase());
    }

    /**
     * Returns whether a primitive is an operator or not.
     *
     * @param pr primitive
     * @return boolean is operator or not
     */
    public static boolean isOperator(Primitive pr) {
        return pr == AND || pr == OR;
    }

    /**
     * Returns whether a primitive has protocol or not.
     *
     * @param pr primitive
     * @return boolean has protocol or not
     */
    public static boolean hasProtocol(Primitive pr) {
        return (pr == IP) || (pr == ICMP) || (pr == TCP);
    }

    /**
     * Returns whether a primitive has src/dst or not.
     *
     * @param pr primitive
     * @return boolean has src/dst or not
     */
    public static boolean hasSrcDst(Primitive pr) {
        return (pr == SRC) || (pr == DST);
    }

    /**
     * Returns whether a primitive is undefined or not.
     *
     * @param pr primitive
     * @return boolean is undefined or not
     */
    public static boolean isUndefined(Primitive pr) {
        return pr == UNDEFINED;
    }

    @Override
    public String toString() {
        return this.primitive;
    }

}
