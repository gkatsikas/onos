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

package org.onosproject.metron.api.dataplane;

import org.onosproject.metron.api.exceptions.DeploymentException;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

import java.util.List;
import java.util.Set;

/**
 * Interface that provides path abstractions.
 */
public interface PathEstablisherInterface {

    /**
     * Indicates any port.
     */
    static final long ANY_PORT = -1;

    /**
     * Returns the forward path of this dataplane tree.
     *
     * @return forward path
     */
    Path fwdPath();

    /**
     * Returns the backward path of this dataplane tree.
     *
     * @return backward path
     */
    Path bwdPath();

    /**
     * Returns the list of forward links that comprise
     * the dataplane tree's path.
     * This path is from an ingress switch to a server.
     *
     * @return list of forward path links
     */
    List<Link> fwdPathLinks();

    /**
     * Returns the list of backward links that comprise
     * the dataplane tree's path.
     * This path is from an ingress switch to a server.
     *
     * @return list of backward path links
     */
    List<Link> bwdPathLinks();

    /**
     * Returns the ingress port of this dataplane tree.
     *
     * @return ingress port number
     */
    long ingressPort();

    /**
     * Sets the ingress port of this dataplane tree.
     *
     * @param ingrPort ingress port number
     */
    void setIngressPort(long ingrPort);

    /**
     * Returns the egress port of this dataplane tree.
     *
     * @return egress port number
     */
    long egressPort();

    /**
     * Sets the egress port of this dataplane tree.
     *
     * @param egrPort egress port number
     */
    void setEgressPort(long egrPort);

    /**
     * Resets the list of links for the path of this dataplane tree.
     * This implies that new empty lists of forward and backward
     * paths are created.
     * Then, you can add new links and re-build the path.
     */
    void resetLinks();

    /**
     * Returns the ID of the switch that realizes this traffic class.
     * There can only be a single switch acting as offloader.
     *
     * @return the offloader switch
     */
    ConnectPoint offloaderSwitch();

    /**
     * Resets the switch that offloads this dataplane tree.
     *
     * @param offlSwitch a new switch for offloading
     */
    void resetOffloaderSwitch(ConnectPoint offlSwitch);

    /**
     * Returns the switch ID that realizes this dataplane tree.
     * There can only be a single switch acting as offloader.
     *
     * @return device ID of the offloader switch
     */
    DeviceId offloaderSwitchId();

    /**
     * Returns the port of the switch towards this dataplane tree.
     *
     * @return offloader switch port number towards Metron
     */
    long offloaderSwitchToServerPort();

    /**
     * Returns the ID of the leaf switch of this dataplane tree.
     * There can only be a single switch acting as leaf.
     *
     * @return the leaf switch
     */
    ConnectPoint leafSwitch();

    /**
     * Resets the leaf switch of this dataplane tree.
     *
     * @param leafSwitch a new leaf switch
     */
    void resetLeafSwitch(ConnectPoint leafSwitch);

    /**
     * Returns the ID of the leaf switch of this dataplane tree.
     *
     * @return device ID of the leaf switch
     */
    DeviceId leafSwitchId();

    /**
     * Returns the port of the leaf switch of this dataplane tree.
     *
     * @return leaf switch port number
     */
    long leafSwitchEgressPort();

    /**
     * Returns the ingress ID of the server that realizes this dataplane tree.
     * There can only be a single server.
     *
     * @return the ingress server of this dataplane tree
     */
    ConnectPoint serverIngr();

    /**
     * Returns the egress ID of the server that realizes this dataplane tree.
     * Ingress and egress servers have the same device ID but potentially
     * different port numbers.
     *
     * @return the egress server of this dataplane tree
     */
    ConnectPoint serverEgr();

    /**
     * Resets the server of this dataplane tree.
     *
     * @param serverIngr a new ingress server
     * @param serverEgr  a new egress server
     */
    void resetServer(ConnectPoint serverIngr, ConnectPoint serverEgr);

    /**
     * Returns the ID of the server of this dataplane tree.
     *
     * @return device ID of the server
     */
    DeviceId serverId();

    /**
     * Returns the port of the ingress server that leads to this dataplane tree.
     *
     * @return server ingress port number
     */
    long serverInressPort();

    /**
     * Returns the egress port of the server after going
     * through the dataplane tree.
     *
     * @return server egress port number
     */
    long serverEgressPort();

    /**
     * Builds the forward and backward paths of this dataplane tree.
     *
     * @param withServer indicates whether the last node is a server
     *        If false, implies that the tree is fully offloaded
     * @throws DeploymentException if the path is not built properly
     */
    void buildPaths(boolean withServer) throws DeploymentException;

    /**
     * Returns a set of hardware rules that undertake to route
     * traffic from the ingress switch (after offloading has occured)
     * to a service chain.
     * The offloading and tagging is done by the DeploymentManager.
     *
     * @param appId the application ID that demands
     *        this hardware configuration
     * @param tag the tag value to match
     * @return set of hardware rules
     */
    Set<FlowRule> ingressRules(
        ApplicationId appId,
        RxFilterValue tag
    );

    /**
     * Returns a set of hardware rules that undertake to route
     * traffic from a service chain to an egress point.
     *
     * @param appId the application ID that demands
     *        this hardware configuration
     * @return set of hardware rules
     */
    Set<FlowRule> egressRules(ApplicationId appId);

}
