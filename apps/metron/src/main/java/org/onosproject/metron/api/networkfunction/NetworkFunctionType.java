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

package org.onosproject.metron.api.networkfunction;

/**
 * Type of a network function.
 */
public enum NetworkFunctionType {

    /**
     * Supported types of network functions.
     */
    CLICK("click"),
    MIXED("mixed"),
    STANDALONE("standalone"),
    TRANSPARENT("transparent");

    private String nfType;

    private NetworkFunctionType(String nfType) {
        this.nfType = nfType.toLowerCase();
    }

    /**
     * Returns whether an NF type is valid or not.
     * MIXED type is not valid as this type is only
     * derived from the combination of CLICK and
     * STANDALONE types.
     *
     * @param nfType the network function's type
     * @return boolean validity status
     */
    public static boolean isValid(NetworkFunctionType nfType) {
        if ((nfType != CLICK) && (nfType != STANDALONE) && (nfType != TRANSPARENT)) {
            return false;
        }

        return true;
    }

    /**
     * Returns whether an NF type is specific to a technology or not.
     * MIXED type is specific as it derives from two or more specific NFs.
     * TRANSPARENT is the only nom-specific NF type.
     *
     * @param nfType the network function's type
     * @return boolean specificity status
     */
    public static boolean isSpecific(NetworkFunctionType nfType) {
        return (nfType != TRANSPARENT);
    }

    @Override
    public String toString() {
        return nfType;
    }

}