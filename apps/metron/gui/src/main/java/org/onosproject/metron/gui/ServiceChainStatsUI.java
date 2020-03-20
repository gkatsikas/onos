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

package org.onosproject.metron.gui;

import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import java.util.List;

import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.GlyphConstants.CHAIN;

/**
 * Mechanism to stream service chain statistics to the GUI.
 */
@Component(immediate = true, service = ServiceChainStatsUI.class)
public class ServiceChainStatsUI {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The label of this component.
     */
    private static final String COMPONET_LABEL = "Metron Service Chains Stats UI";

    /**
     * GUI Information.
     */
    private static final String SC_STATS_ID = "scstat";
    private static final String SC_STATS_TEXT = "Service Chains-Stats";
    private static final String RES_PATH = "gui";
    private static final ClassLoader CL = ServiceChainStatsUI.class.getClassLoader();

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(new ServiceChainStatsViewMessageHandler());

    // List of application views
    private final List<UiView> views = ImmutableList.of(
            new UiView(NETWORK, SC_STATS_ID, SC_STATS_TEXT, CHAIN)
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
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
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
