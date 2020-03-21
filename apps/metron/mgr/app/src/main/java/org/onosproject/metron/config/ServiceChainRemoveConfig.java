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

package org.onosproject.metron.config;

import org.onosproject.metron.api.exceptions.InputConfigurationException;
import org.onosproject.metron.api.config.ServiceChainRemoveConfigInterface;
import org.onosproject.metron.api.servicechain.ServiceChainId;

import org.onosproject.net.config.Config;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.NetworkId;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;

import java.util.Set;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;

/**
 * Configuration for removing Metron service chains.
 */
public final class ServiceChainRemoveConfig
        extends Config<ApplicationId>
        implements ServiceChainRemoveConfigInterface {

    private final Logger log = getLogger(getClass());

    /**
     * A service chain is defined using this attribute.
     */
    private static final String SC_TITLE = "serviceChains";

    /**
     * Each service chain should have
     * the following main attributes.
     */
    private static final String SC_NAME       = "name";
    private static final String SC_TYPE       = "type";
    private static final String SC_NETWORK_ID = "networkId";

    private ApplicationId appId = null;

    @Override
    public boolean isValid() {
        boolean result = hasOnlyFields(SC_TITLE);

        if (object.get(SC_TITLE) == null || object.get(SC_TITLE).size() < 1) {
            final String msg = "No service chain is present";
            throw new IllegalArgumentException(msg);
        }

        // Multiple service chains can be managed
        for (JsonNode node : object.get(SC_TITLE)) {
            ObjectNode scNode = (ObjectNode) node;

            // Check for the main attributes of a service chain
            result &= hasOnlyFields(
                scNode,
                SC_NAME,
                SC_TYPE,
                SC_NETWORK_ID
            );
            checkArgument(result, "Mandatory argument(s) missing from JSON configuration");

            result &= isString(scNode, SC_NAME, MANDATORY);
            checkArgument(result, "Service chain name must be a string");
            result &= isString(scNode, SC_TYPE, MANDATORY);
            checkArgument(result, "Service chain type must be a string");
            result &= isNumber(scNode, SC_NETWORK_ID, MANDATORY);
            checkArgument(result, "Service chain network ID must be a number");
        }

        return result;
    }

    @Override
    public Set<ServiceChainId> undeployServiceChains(ApplicationId appId)
            throws IOException, InputConfigurationException {
        this.appId = appId;

        Set<ServiceChainId> scIds = Sets.<ServiceChainId>newConcurrentHashSet();

        log.info("Removing service chains...");

        for (JsonNode node : object.get(SC_TITLE)) {
            ObjectNode scNode = (ObjectNode) node;

            String scName = scNode.path(SC_NAME).asText().toLowerCase();
            checkArgument(
                !Strings.isNullOrEmpty(scName),
                "Please specify a name for this service chain"
            );
            String scType = scNode.path(SC_TYPE).asText().toLowerCase();
            checkArgument(
                !Strings.isNullOrEmpty(scType),
                "Please specify a type for this service chain"
            );

            int scNetIdInt = scNode.path(SC_NETWORK_ID).asInt();
            checkArgument(
                scNetIdInt > 0,
                "Please specify a network ID greater than 0"
            );
            NetworkId scNetId = NetworkId.networkId(scNetIdInt);

            ServiceChainId id = (ServiceChainId) ServiceChainId.id(
                "sc:" + scName + ":" + scType + ":" +  scNetId.toString()
            );
            checkNotNull(id, "Service chain ID is NULL");

            scIds.add(id);
        }

        log.info("Will tear down {} service chain(s): {}", scIds.size(), scIds.toString());

        return scIds;
    }

}
