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

package org.onosproject.metron.gui;

import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.GlyphConstants.CHAIN;

/**
 * Mechanism to stream service chain information to the GUI.
 */
@Component(immediate = true, enabled = true)
@Service(value = ServiceChainInfoUI.class)
public class ServiceChainInfoUI {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The label of this component.
     */
    private static final String COMPONET_LABEL = "Metron Service Chains Info UI";

    /**
     * GUI Information.
     */
    private static final String SC_INFO_ID = "scinfo";
    private static final String SC_INFO_TEXT = "Service Chains-Info";
    private static final String RES_PATH = "gui";
    private static final ClassLoader CL = ServiceChainInfoUI.class.getClassLoader();

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(new ServiceChainInfoViewMessageHandler());

    // List of application views
    private final List<UiView> views = ImmutableList.of(
            new UiView(NETWORK, SC_INFO_ID, SC_INFO_TEXT, CHAIN)
    );

    // Application UI extension
    private final UiExtension uiExtension =
            new UiExtension.Builder(CL, views)
                    .messageHandlerFactory(messageHandlerFactory)
                    .resourcePath(RES_PATH)
                    .build();

    /**
     * Interact with ONOS.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    @Activate
    protected void activate() {
        uiExtensionService.register(uiExtension);
        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(uiExtension);
        log.info("[{}] Stopped", this.label());
    }

    /**
     * Returns a label with the module's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}
