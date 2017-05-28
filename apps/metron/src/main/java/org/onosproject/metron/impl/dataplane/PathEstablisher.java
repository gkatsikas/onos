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

package org.onosproject.metron.impl.dataplane;

import org.onosproject.metron.api.dataplane.PathEstablisherInterface;
import org.onosproject.metron.api.exceptions.DeploymentException;

import org.onosproject.metron.impl.classification.trafficclass.TrafficClass;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.drivers.server.devices.MacRxFilterValue;
import org.onosproject.drivers.server.devices.RxFilterValue;

import org.onlab.packet.MacAddress;

import org.slf4j.Logger;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * An entity that establishes the path between an ingress point,
 * a Metron server, and an egress point.
 */
public final class PathEstablisher implements PathEstablisherInterface {

    private static final Logger log = getLogger(PathEstablisher.class);

    private String label;

    // Indicates any port
    private static final long ANY_PORT = -1;

    /**
     * Paths that this traffic class goes through.
     * |-> fwdPath is the path from an ingress point to a server
     * |-> bwdPath is the path from a server to an egress point
     */
    private Path fwdPath = null;
    private Path bwdPath = null;

    /**
     * The set of nodes that comprise a path.
     */
    private List<ConnectPoint> fwdPathNodes = null;
    private List<ConnectPoint> bwdPathNodes = null;

    /**
     * The links that comprise a path.
     * We need to construct the entire list first, before creating a path.
     */
    private List<Link> fwdLinks = null;
    private List<Link> bwdLinks = null;

    /**
     * Information about the switch that offloads this traffic class tree.
     */
    private ConnectPoint offloaderSwitch = null;
    private long ingressPort = -1;
    private long egressPort  = -1;

    /**
     * Information about the last switch before the server that hosts
     * this traffic class tree.
     */
    private ConnectPoint leafSwitch = null;

    /**
     * Information about the server that hosts this traffic class tree.
     */
    private ConnectPoint serverIngr = null;
    private ConnectPoint serverEgr  = null;

    /**
     * Provider ID used for building paths.
     */
    private static final ProviderId PATH_PROVIDER_ID = new ProviderId(
        "metron", "org.onosproject.metron"
    );

    public PathEstablisher(
            Path fwdPath, Path bwdPath,
            long ingressPort, long egressPort,
            boolean withServer) {
        checkNotNull(fwdPath, "Forward path is NULL");
        checkNotNull(bwdPath, "Backward path is NULL");
        checkArgument(ingressPort > 0, "Attempted to set negative ingress port");
        checkArgument(egressPort > 0, "Attempted to set negative egress port");

        init();

        // Nodes
        this.fwdPathNodes = this.nodesOfPath(fwdPath);
        this.bwdPathNodes = this.nodesOfPath(bwdPath);

        // Links
        this.addLinksToFwdPath(fwdPath.links());
        this.addLinksToBwdPath(bwdPath.links());

        // Ingress/Egress info
        this.setIngressPort(ingressPort);
        this.setEgressPort(egressPort);

        // Build
        this.buildPaths(withServer);
    }

    public PathEstablisher(
            ConnectPoint point1, ConnectPoint point2,
            long ingressPort, long egressPort,
            boolean withServer) {
        checkNotNull(point1, "Start connection point is NULL");
        checkNotNull(point2, "End connection point is NULL");
        checkArgument(ingressPort > 0, "Attempted to set negative ingress port");
        checkArgument(egressPort  > 0, "Attempted to set negative egress port");

        init();

        // Nodes
        this.addNodeToFwdPath(point1);
        this.addNodeToFwdPath(point2);
        this.addNodeToBwdPath(point2);
        this.addNodeToBwdPath(point1);

        // Links
        this.addLinkToFwdPath(point1, point2);
        this.addLinkToBwdPath(point2, point1);

        // Ingress/Egress info
        this.setIngressPort(ingressPort);
        this.setEgressPort(egressPort);

        // Build
        this.buildPaths(withServer);
    }

    /**
     * Initialize some members.
     */
    private void init() {
        this.label = "Metron Deployer";
    }

    @Override
    public Path fwdPath() {
        return this.fwdPath;
    }

    @Override
    public Path bwdPath() {
        return this.bwdPath;
    }

    @Override
    public List<Link> fwdPathLinks() {
        if (this.fwdPath == null) {
            return null;
        }
        return this.fwdPath.links();
    }

    @Override
    public List<Link> bwdPathLinks() {
        if (this.bwdPath == null) {
            return null;
        }
        return this.bwdPath.links();
    }

    @Override
    public long ingressPort() {
        return this.ingressPort;
    }

    @Override
    public void setIngressPort(long ingrPort) {
        checkArgument(ingrPort >= 0, "Attempted to set negative offloader switch ingress port");
        this.ingressPort = ingrPort;
    }

    @Override
    public long egressPort() {
        return this.egressPort;
    }

    @Override
    public void setEgressPort(long egrPort) {
        checkArgument(egrPort >= 0, "Attempted to set negative offloader switch egress port");
        this.egressPort = egrPort;
    }

    @Override
    public void resetLinks() {
        this.fwdLinks.clear();
        this.bwdLinks.clear();
        this.fwdLinks = new ArrayList<Link>();
        this.bwdLinks = new ArrayList<Link>();
    }

    @Override
    public ConnectPoint offloaderSwitch() {
        return this.offloaderSwitch;
    }

    @Override
    public void resetOffloaderSwitch(ConnectPoint offlSwitch) {
        checkNotNull(offlSwitch, "Attempted to set NULL offloader switch for dataplane tree");
        this.offloaderSwitch = offlSwitch;
    }

    @Override
    public DeviceId offloaderSwitchId() {
        return this.offloaderSwitch.deviceId();
    }

    @Override
    public long offloaderSwitchMetronPort() {
        return this.offloaderSwitch.port().toLong();
    }

    @Override
    public ConnectPoint leafSwitch() {
        return this.leafSwitch;
    }

    @Override
    public void resetLeafSwitch(ConnectPoint leafSwitch) {
        checkNotNull(leafSwitch, "Attempted to set NULL leaf switch for dataplane tree");
        this.leafSwitch = leafSwitch;
    }

    @Override
    public DeviceId leafSwitchId() {
        return this.leafSwitch.deviceId();
    }

    @Override
    public long leafSwitchEgressPort() {
        return this.leafSwitch.port().toLong();
    }

    @Override
    public ConnectPoint serverIngr() {
        return this.serverIngr;
    }

    @Override
    public ConnectPoint serverEgr() {
        return this.serverEgr;
    }

    @Override
    public void resetServer(ConnectPoint serverIngr, ConnectPoint serverEgr) {
        checkNotNull(serverIngr, "Attempted to set NULL ingress server for dataplane tree");
        checkNotNull(serverEgr,  "Attempted to set NULL egress server for dataplane tree");
        this.serverIngr = serverIngr;
        this.serverEgr  = serverEgr;
    }

    @Override
    public DeviceId serverId() {
        checkArgument(this.serverIngr.deviceId().equals(this.serverEgr.deviceId()));
        return this.serverIngr.deviceId();
    }

    @Override
    public long serverInressPort() {
        return this.serverIngr.port().toLong();
    }

    @Override
    public long serverEgressPort() {
        return this.serverEgr.port().toLong();
    }

    @Override
    public void buildPaths(boolean withServer) throws DeploymentException {
        checkArgument(!this.fwdLinks.isEmpty(), "Attempted to build path without forward links");
        checkArgument(!this.bwdLinks.isEmpty(), "Attempted to build path without backward links");

        Annotations annotations = DefaultAnnotations.builder().build();
        this.fwdPath = new DefaultPath(PATH_PROVIDER_ID, this.fwdLinks, null, annotations);
        this.bwdPath = new DefaultPath(PATH_PROVIDER_ID, this.bwdLinks, null, annotations);

        // Forward path: The src of the first link is the switch to offload
        Link firstFwdLink = this.fwdLinks.get(0);
        checkNotNull(firstFwdLink, "First link in forward path is NULL");
        this.offloaderSwitch = new ConnectPoint(
            firstFwdLink.src().deviceId(), firstFwdLink.src().port()
        );

        // Forward path: The src of the last link is the leaf switch
        Link lastFwdLink = this.fwdLinks.get(this.fwdLinks.size() - 1);
        checkNotNull(lastFwdLink, "Last link in forward path is NULL");
        this.leafSwitch = new ConnectPoint(
            lastFwdLink.src().deviceId(),
            lastFwdLink.src().port()
        );

        if (withServer) {
            // Forward path: The dst of the last link is the ingress point of the server
            this.serverIngr = new ConnectPoint(
                lastFwdLink.dst().deviceId(),
                lastFwdLink.dst().port()
            );

            // Backward path: The src of the first link is the egress point of the server
            Link firstBwdLink = this.bwdLinks.get(0);
            this.serverEgr = new ConnectPoint(
                firstBwdLink.src().deviceId(),
                firstBwdLink.src().port()
            );
        }

        try {
            this.verifyPath(withServer);
        } catch (DeploymentException depEx) {
            throw depEx;
        }
    }

    @Override
    public Set<FlowRule> ingressRules(ApplicationId appId, RxFilterValue tag) {
        checkNotNull(this.fwdLinks, "Cannot install rules for forward path; links are NULL");
        checkNotNull(tag, "Cannot install rules for forward path; tag is NULL");

        log.info("[{}] \t Installing ingress rules...", label);

        Set<FlowRule> rules = Sets.<FlowRule>newConcurrentHashSet();
        long prevEgrPort = ANY_PORT;

        /**
         * Traverse the forward path and install one rule per device.
         * This rule matches the tag and sends the patched packets
         * towards the server.
         */
        for (Link link : this.fwdLinks) {
            log.debug(
                "[{}] \t\t Src {}/{} -> Dst {}/{}", label,
                link.src().deviceId(), link.src().port().toLong(),
                link.dst().deviceId(), link.dst().port().toLong()
            );

            DeviceId targetDev = link.src().deviceId();

            // Rules are installed on switches between ingress and server
            if (targetDev.equals(this.serverId()) ||
                targetDev.equals(this.offloaderSwitchId())) {
                log.info("[{}] \t\t Skipping already configured device {}", label, targetDev);
                continue;
            }

            long matchPort  = link.src().port().toLong();
            long actionPort = ANY_PORT;

            // Egress device
            if (targetDev.equals(this.leafSwitchId())) {
                actionPort = this.leafSwitchEgressPort();
                prevEgrPort = link.src().port().toLong();
            } else {
                actionPort = prevEgrPort;
            }
            checkArgument(
                (actionPort != ANY_PORT) && (prevEgrPort != ANY_PORT),
                "Invalid action port for exit rule"
            );

            log.info("[{}] \t\t Ingress rule on {}: Match {} -> Action {}", label, targetDev, matchPort, actionPort);

            // Install the rule
            rules.addAll(installRulesOnDevice(appId, targetDev, matchPort, tag, actionPort));
        }

        return rules;
    }

    @Override
    public Set<FlowRule> egressRules(ApplicationId appId) {
        checkNotNull(this.bwdLinks, "Cannot install rules for backward path; links are NULL");

        log.info("[{}] \t Installing egress rules...", label);

        Set<FlowRule> rules = Sets.<FlowRule>newConcurrentHashSet();
        long prevEgrPort = ANY_PORT;

        /**
         * Traverse the backward path and install one rule per device.
         * This rule helps traffic exiting the Metron to "escape"
         * towards an egress point.
         */
        for (Link link : this.bwdLinks) {
            log.info(
                "[{}] \t\t Src {}/{} -> Dst {}/{}", label,
                link.src().deviceId(), link.src().port().toLong(),
                link.dst().deviceId(), link.dst().port().toLong()
            );

            DeviceId targetDev = link.dst().deviceId();

            // Rules are installed on switches
            if (targetDev.equals(this.serverId())) {
                continue;
            }

            long matchPort  = link.dst().port().toLong();
            long actionPort = ANY_PORT;

            // Egress device
            if (targetDev.equals(this.offloaderSwitchId())) {
                actionPort = this.egressPort();
                prevEgrPort = link.dst().port().toLong();
            } else {
                actionPort = prevEgrPort;
            }
            checkArgument(
                (actionPort != ANY_PORT) && (prevEgrPort != ANY_PORT),
                "Invalid action port for egress rule"
            );

            log.info("[{}] \t\t Egress rule on {}: Match {} -> Action {}", label, targetDev, matchPort, actionPort);

            // Install the rule
            rules.addAll(installRulesOnDevice(appId, targetDev, matchPort, null, actionPort));
        }

        return rules;
    }

    /**
     * Returns a set of hardware rules that follow the input requirements.
     * These rules are meant for a specific device.
     *
     * @param appId the application ID that demands
     *        this hardware configuration
     * @param deviceId device ID where rules will be installed
     * @param matchPort port to match
     * @param matchTag  tag to match
     * @param actionPort port to output
     * @return set of hardware rules
     */
    private Set<FlowRule> installRulesOnDevice(
            ApplicationId appId, DeviceId deviceId,
            long matchPort, RxFilterValue tag, long actionPort) {
        checkNotNull(deviceId, "Invalid device; cannot install OpenFlow rules");
        checkArgument(matchPort >= 0, "Invalid port to match traffic");
        checkArgument(actionPort >= 0, "Invalid port to output");

        // Rule builder
        FlowRule.Builder rule = new DefaultFlowRule.Builder();

        rule.forDevice(deviceId);
        rule.fromApp(appId);
        rule.withPriority(TrafficClass.DEFAULT_PRIORITY);
        rule.makePermanent();

        /**
         * MATCH: whatever traffic comes out of this port.
         */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(matchPort));
        if (tag != null) {
            MacAddress tagVal = ((MacRxFilterValue) tag).value();
            selector.matchEthDst(tagVal);
        }
        rule.withSelector(selector.build());

        /**
         * ACTION: send out to a known egress port.
         */
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.add(Instructions.createOutput(PortNumber.portNumber(actionPort)));
        rule.withTreatment(treatment.build());

        Set<FlowRule> rules = Sets.<FlowRule>newConcurrentHashSet();
        rules.add(rule.build());

        return rules;
    }

    /**
     * Verify that buildPath has done its job.
     *
     * @param withServer indicates whether the last node is a server
     * @throws DeploymentException if the path is not built properly
     */
    private void verifyPath(boolean withServer)
            throws DeploymentException {
        DeviceId     coreSwitchId  = this.offloaderSwitchId();
        long coreSwitchIngressPort = this.ingressPort();
        long coreSwitchMetronPort  = this.offloaderSwitchMetronPort();
        long coreSwitchEgressPort  = this.egressPort();
        DeviceId     leafSwitchId  = this.leafSwitchId();
        long leafSwitchEgressPort  = this.leafSwitchEgressPort();

        if ((coreSwitchEgressPort < 0) || (leafSwitchEgressPort < 0)) {
            throw new DeploymentException(
                "Failed to establish a valid path for a service chain. " +
                "The path invloves core switch " + coreSwitchId + " " +
                "with egress port " + coreSwitchEgressPort + "and " +
                "leaf switch " + leafSwitchId + " with egress port " +
                leafSwitchEgressPort
            );
        }

        String msg = "";

        if ((coreSwitchId == null) && (leafSwitchId == null)) {
            log.info("[{}] No switches in the path", label);
        } else if (coreSwitchId.equals(leafSwitchId)) {
            msg = String.format("[%s] \t Path: Ingress switch [IN %d]%s",
                label, coreSwitchIngressPort, coreSwitchId
            );
        } else {
            msg = String.format(
                "[%s] \t Path: Ingress switch [IN %d]%s[OUT %d] -> Leaf switch %s[OUT %d]",
                label,
                coreSwitchIngressPort, coreSwitchId, coreSwitchMetronPort,
                leafSwitchId, leafSwitchEgressPort
            );
        }

        if (!withServer) {
            msg += String.format(" -> Egress switch %s[OUT %d]", coreSwitchId, coreSwitchEgressPort);
            log.info("{}", msg);
            return;
        }

        DeviceId      serverId = this.serverId();
        long serverIngressPort = this.serverInressPort();
        long serverEgressPort  = this.serverEgressPort();

        if ((serverId == null) || (serverIngressPort < 0) || (serverEgressPort < 0)) {
            throw new DeploymentException(
                "Failed to establish a valid path for a service chain. " +
                "The path invloves server " + serverId + " " +
                "with ingress port " + serverIngressPort + " " +
                "and egress port " + serverEgressPort
            );
        }

        if (!msg.isEmpty()) {
            msg += " -> ";
        }
        msg += String.format(
            "Server [IN %d]%s[OUT %d] -> Egress Switch %s[OUT %d]",
            serverIngressPort, serverId, serverEgressPort,
            coreSwitchId, coreSwitchEgressPort
        );
        log.info("{}", msg);
    }

    /**
     * Adds a new link to the forward path of this dataplane tree.
     *
     * @param link a link to be added to the forward path
     */
    private void addLinkToFwdPath(Link link) {
        checkNotNull(link, "Attempted to add NULL link to the forward path of dataplane tree");
        if (this.fwdLinks == null) {
            this.fwdLinks = new ArrayList<Link>();
        }

        this.fwdLinks.add(link);
    }

    /**
     * Adds a new link to the backward path of this dataplane tree.
     *
     * @param link a link to be added to the backward path
     */
    private void addLinkToBwdPath(Link link) {
        checkNotNull(link, "Attempted to add NULL link to the backward path of dataplane tree");
        if (this.bwdLinks == null) {
            this.bwdLinks = new ArrayList<Link>();
        }

        this.bwdLinks.add(link);
    }

    /**
     * Adds a new link to the forward path of this dataplane tree.
     *
     * @param src the source connection point of the link
     * @param dst the destination connection point of the link
     */
    private void addLinkToFwdPath(ConnectPoint src, ConnectPoint dst) {
        checkNotNull(
            src,
            "Attempted to add NULL source point to the forward path of dataplane tree"
        );
        checkNotNull(
            dst,
            "Attempted to add NULL destination point to the forward path of dataplane tree"
        );

        Link link = DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PATH_PROVIDER_ID)
                    .src(src)
                    .dst(dst)
                    .build();
        this.addLinkToFwdPath(link);
    }

    /**
     * Adds a new link to the backward path of this dataplane tree.
     *
     * @param src the source connection point of the link
     * @param dst the destination connection point of the link
     */
    private void addLinkToBwdPath(ConnectPoint src, ConnectPoint dst) {
        checkNotNull(
            src,
            "Attempted to add NULL source point to the backward path of dataplane tree"
        );
        checkNotNull(
            dst,
            "Attempted to add NULL destination point to the backward path of dataplane tree"
        );

        Link link = DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PATH_PROVIDER_ID)
                    .src(src)
                    .dst(dst)
                    .build();
        this.addLinkToBwdPath(link);
    }

    /**
     * Adds a list of links to the forward path of this dataplane tree.
     *
     * @param links a list of links to be added to the forward path
     */
    private void addLinksToFwdPath(List<Link> links) {
        checkNotNull(
            links,
            "Attempted to add NULL list of links to the forward path of dataplane tree"
        );
        this.fwdLinks = links;
    }

    /**
     * Adds a list of links to the backward path of this dataplane tree.
     *
     * @param links a list of links to be added to the backward path
     */
    private void addLinksToBwdPath(List<Link> links) {
        checkNotNull(
            links,
            "Attempted to add NULL list of links to the backward path of dataplane tree"
        );
        this.bwdLinks = links;
    }

    /**
     * Adds nodes to the forward path of this dataplane tree.
     *
     * @param path a path of nodes to be decomposed
     * @return a list of connection points
     */
    private List<ConnectPoint> nodesOfPath(Path path) {
        Set<ConnectPoint> points = Sets.<ConnectPoint>newConcurrentHashSet();

        for (int i = 0; i < path.links().size(); i++) {
            Link link = path.links().get(i);
            points.add(link.src());
            points.add(link.dst());
        }

        return new ArrayList<ConnectPoint>(points);
    }

    /**
     * Add a node to the forward path of this dataplane tree.
     *
     * @param point a node to be added to the forward path
     */
    private void addNodeToFwdPath(ConnectPoint point) {
        checkNotNull(point, "Cannot add a NULL node to forward path");
        if (this.fwdPathNodes == null) {
            this.fwdPathNodes = new ArrayList<ConnectPoint>();
        }

        this.fwdPathNodes.add(point);
    }

    /**
     * Add a node to the backward path of this dataplane tree.
     *
     * @param point a node to be added to the backward path
     */
    private void addNodeToBwdPath(ConnectPoint point) {
        checkNotNull(point, "Cannot add a NULL node to backward path");
        if (this.bwdPathNodes == null) {
            this.bwdPathNodes = new ArrayList<ConnectPoint>();
        }

        this.bwdPathNodes.add(point);
    }

}
