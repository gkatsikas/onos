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

package org.onosproject.metron.impl.drivers.provider;

import org.onosproject.metron.api.common.Constants;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.net.Link.Type.DIRECT;

/**
 * Ancillary provider to activate/deactivate NFV links as their respective
 * devices go online or offline.
 */
@Component(immediate = true)
public class NfvLinkProvider extends AbstractProvider implements LinkProvider {

    private static final Logger log = LoggerFactory.getLogger(NfvLinkProvider.class);

    /**
     * Application name and label for the NFV Link Provider.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".drivers.provider";
    private static final String COMPONET_LABEL = "NFV Link Provider";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    private LinkProviderService linkProviderService;
    private DeviceListener deviceListener = new InternalDeviceListener();
    private LinkListener linkListener = new InternalLinkListener();

    public NfvLinkProvider() {
        super(new ProviderId("nfv", APP_NAME));
    }

    @Activate
    protected void activate() {
        // Listen for devices
        deviceService.addListener(deviceListener);

        // ... and links
        linkService.addListener(linkListener);

        // Register as a new link provider
        linkProviderService = linkRegistry.register(this);

        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        // Tear down the listeners
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);

        // Unregister from the link registry
        linkRegistry.unregister(this);

        log.info("[{}] Stopped", this.label());
    }

    /**
     * Returns a label with the link provider's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

    // Listens to device events and processes their links.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            Device device = event.subject();
            if (type == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                type == DeviceEvent.Type.DEVICE_ADDED ||
                type == DeviceEvent.Type.DEVICE_UPDATED) {
                log.info("[{}] Detected device {}", label(), device.id());

                processDeviceLinks(device);
            } else if (type == DeviceEvent.Type.PORT_UPDATED) {
                Port port = event.port();
                log.debug(
                    "[{}] Detected update on port {} of device {}",
                    label(), port.number().toLong(), device.id()
                );
                processPortLinks(device, port);
            }
        }
    }

    // Listens to link events and processes the link additions.
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if ((event.type() == LinkEvent.Type.LINK_ADDED) ||
                (event.type() == LinkEvent.Type.LINK_UPDATED)) {
                Link link = event.subject();
                processLink(event.subject());
            }
        }
    }

    private void processDeviceLinks(Device device) {
        for (Link link : linkService.getDeviceLinks(device.id())) {
            processLink(link);
        }
    }

    private void processPortLinks(Device device, Port port) {
        ConnectPoint connectPoint = new ConnectPoint(device.id(), port.number());
        for (Link link : linkService.getLinks(connectPoint)) {
            processLink(link);
        }
    }

    private void processLink(Link link) {
        DeviceId srcId = link.src().deviceId();
        DeviceId dstId = link.dst().deviceId();
        Port srcPort = deviceService.getPort(srcId, link.src().port());
        Port dstPort = deviceService.getPort(dstId, link.dst().port());

        // FIXME: Remove this in favor of below TODO
        if (srcPort == null || dstPort == null) {
            return;
        }

        // TODO: Should an update be queued if src or dst port is null?
        boolean active = deviceService.isAvailable(srcId) &&
                deviceService.isAvailable(dstId) &&
                srcPort.isEnabled() && dstPort.isEnabled();

        log.info(
            "Detected link between {}/{} -- {}/{} - Status {}",
            srcId, srcPort.number(), dstId, dstPort.number(), active ? "ACTIVE" : "INACTIVE"
        );

        // Create a new link description for the core
        LinkDescription desc = new DefaultLinkDescription(link.src(), link.dst(), DIRECT);
        if (active) {
            linkProviderService.linkDetected(desc);
        } else {
            linkProviderService.linkVanished(desc);
        }
    }

}
