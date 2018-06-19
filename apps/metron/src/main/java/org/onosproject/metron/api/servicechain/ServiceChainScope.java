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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scope of a service chain.
 */
public enum ServiceChainScope {

    /**
     * Service chain deployment scopes:
     * |-> NETWORK_MAC indicates that available network
     *     elements will be exploited for classification
     *     and tagging. These elements will use the destination
     *     MAC address as a field for tagging.
     * |-> NETWORK_VLAN indicates that available network
     *     elements will be exploited for classification
     *     and tagging. These elements will use the VLAN
     *     VID as a field for tagging.
     * |-> SERVER_RULES indicates that no network
     *     element will be used for offloading.
     *     Instead, server's NIC(s) will be programmed with
     *     explicit rules for classification and dispatching.
     * |-> SERVER_RSS indicates that no network
     *     element will be used for offloading.
     *     Instead, server's NIC(s) will utilize Receive-Side
     *     Scaling (RSS) to dispatch input traffic.
     *     In this case, traffic classification is entirely
     *     implemented in software.
     */
    NETWORK_MAC("network-mac"),
    NETWORK_VLAN("network-vlan"),
    SERVER_RULES("server-rules"),
    SERVER_RSS("server-rss");

    private String scScope;

    // Statically maps service chain scopes to enum types
    private static final Map<String, ServiceChainScope> MAP =
        new ConcurrentHashMap<String, ServiceChainScope>();

    static {
        for (ServiceChainScope scope : ServiceChainScope.values()) {
            MAP.put(scope.toString().toLowerCase(), scope);
        }
    }

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
        return MAP.get(scScope.toString().toUpperCase()) != null;
    }

    /**
     * Returns whether a service chain's scope is valid or not.
     *
     * @param scScopeStr the service chain's scope as a string
     * @return boolean validity status
     */
    public static boolean isValid(String scScopeStr) {
        return MAP.get(scScopeStr.toUpperCase()) != null;
    }

    /**
     * Returns whether a service chain's scope is server-level or not.
     *
     * @param scScope the service chain's scope
     * @return boolean status
     */
    public static boolean isServerLevel(ServiceChainScope scScope) {
        return (scScope.toString().contains("server"));
    }

    /**
     * Returns whether a service chain's scope is network-wide or not.
     *
     * @param scScope the service chain's scope
     * @return boolean status
     */
    public static boolean isNetworkWide(ServiceChainScope scScope) {
        return (scScope.toString().contains("network"));
    }

    /**
     * Returns whether a service chain's scope is software-based or not.
     * SERVER_RSS is a dispatching mechanism, hence it is considered
     * software-based because it requires additional traffic
     * classification in software.
     *
     * @param scScope the service chain's scope
     * @return boolean status
     */
    public static boolean isSoftwareBased(ServiceChainScope scScope) {
        return scScope == SERVER_RSS;
    }

    /**
     * Returns whether a service chain's scope is hardware-based or not.
     *
     * @param scScope the service chain's scope
     * @return boolean status
     */
    public static boolean isHardwareBased(ServiceChainScope scScope) {
        return !isSoftwareBased(scScope);
    }

}
