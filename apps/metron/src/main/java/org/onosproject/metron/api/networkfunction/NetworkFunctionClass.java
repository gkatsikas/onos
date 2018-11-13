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
 * Class of a network function.
 */
public enum NetworkFunctionClass {

    /**
     * Indicative classes of network functions.
     */
    BLACKBOX("blackbox"),
    DEEP_PACKET_INSPECTION("dpi"),
    DISPATCHER("dispatcher"),
    FIREWALL("firewall"),
    INTRUSION_DETECTION_SYSTEM("ids"),
    INTRUSION_PREVENTION_SYSTEM("ips"),
    IP_DECRYPTION("ipdecrypt"),
    IP_ENCRYPTION("ipencrypt"),
    L2_SWITCH("l2switch"),
    L3_SWITCH("l3switch"),
    LOAD_BALANCER("loadbalancer"),
    MONITOR("monitor"),
    NETWORK_ADDRESS_AND_PORT_TRANSLATOR("napt"),
    NETWORK_ADDRESS_TRANSLATOR("nat"),
    PROXY("proxy"),
    ROUTER("router"),
    TRANSPARENT("transparent"),
    WAN_OPTIMIZER("wanopt");

    private String nfClass;

    private NetworkFunctionClass(String nfClass) {
        this.nfClass = nfClass.toLowerCase();
    }

    @Override
    public String toString() {
        return nfClass;
    }

}
