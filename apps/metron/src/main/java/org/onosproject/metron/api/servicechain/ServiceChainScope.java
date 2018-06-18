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

package org.onosproject.metron.api.servicechain;

/**
 * Scope of a service chain.
 */
public enum ServiceChainScope {

    /**
     * Service chain deployment scopes:
     * |-> SERVER indicates that no network
     *     element will be used for offloading.
     *     Instead, server's NIC(s) will be used for
     *     classification, tagging, and dispatching.
     * |-> NETWORK indicates that available network
     *     elements will be exploited for classification
     *     and tagging.
     */
    SERVER("server"),
    NETWORK("network");

    private String scScope;

    private ServiceChainScope(String scScope) {
        this.scScope = scScope.toLowerCase();
    }

    @Override
    public String toString() {
        return scScope;
    }

    /**
     * Returns whether a service chain's scope is valid or not.
     *
     * @param scScope the service chain's scope
     * @return boolean validity status
     */
    public static boolean isValid(ServiceChainScope scScope) {
        if ((scScope != SERVER) && (scScope != NETWORK)) {
            return false;
        }

        return true;
    }

    /**
     * Returns whether a service chain's scope is server-level or not.
     *
     * @param scScope the service chain's scope
     * @return boolean status
     */
    public static boolean isServerLevel(ServiceChainScope scScope) {
        return scScope == SERVER;
    }

    /**
     * Returns whether a service chain's scope is network-wide or not.
     *
     * @param scScope the service chain's scope
     * @return boolean status
     */
    public static boolean isNetworkWide(ServiceChainScope scScope) {
        return scScope == NETWORK;
    }

}
