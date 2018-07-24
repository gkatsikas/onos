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

package org.onosproject.metron.impl.config;

import org.onosproject.metron.api.exceptions.JsonParseException;
import org.onosproject.metron.api.exceptions.InputConfigurationException;
import org.onosproject.metron.api.config.BasicConfigurationInterface;
import org.onosproject.metron.api.config.RuleConfigurationInterface;
import org.onosproject.metron.api.config.PatternConfigurationInterface;
import org.onosproject.metron.api.config.ServiceChainAddConfigInterface;
import org.onosproject.metron.api.config.TrafficPoint;
import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.graphs.NetworkFunctionEdgeInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionClass;
import org.onosproject.metron.api.networkfunction.NetworkFunctionType;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainScope;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.graphs.ServiceChainEdgeInterface;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import org.onosproject.metron.impl.graphs.NetworkFunctionEdge;
import org.onosproject.metron.impl.graphs.NetworkFunctionGraph;
import org.onosproject.metron.impl.graphs.NetworkFunctionVertex;
import org.onosproject.metron.impl.graphs.ServiceChainVertex;
import org.onosproject.metron.impl.graphs.ServiceChainEdge;
import org.onosproject.metron.impl.graphs.ServiceChainGraph;
import org.onosproject.metron.impl.networkfunction.NetworkFunction;
import org.onosproject.metron.impl.networkfunction.NetworkFunction.NfNameComparator;
import org.onosproject.metron.impl.networkfunction.NetworkFunctionDevice;
import org.onosproject.metron.impl.processing.blocks.IpRewriter;
import org.onosproject.metron.impl.processing.ClassifierBlock;
import org.onosproject.metron.impl.processing.ProcessingBlockLauncher;
import org.onosproject.metron.impl.servicechain.ServiceChain;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.NetworkId;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.attribute.BasicFileAttributes;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

/**
 * Configuration for adding new Metron service chains.
 */
public final class ServiceChainAddConfig
        extends Config<ApplicationId>
        implements ServiceChainAddConfigInterface {

    private final Logger log = getLogger(getClass());

    /**
     * Used in the graph component of the JSON input.
     * Indicates that a source element is connected with
     * a destination element using all of its output ports.
     */
    private static final int ALL_OUTPUT_PORTS = -1;

    /**
     * A service chain is defined using this attribute.
     */
    private static final String SC_TITLE      = "servicechains";

    /**
     * Each service chain should have
     * the following main attributes.
     */
    private static final String SC_NAME          = "name";
    private static final String SC_TYPE          = "type";
    private static final String SC_NETWORK_ID    = "networkId";
    private static final String SC_CPU_CORES     = "cpuCores";
    private static final String SC_MAX_CPU_CORES = "maxCpuCores";
    private static final String SC_SCALE         = "scale";
    private static final String SC_AUTO_SCALE    = "autoScale";
    private static final String SC_SCOPE         = "scope";
    private static final String SC_COMPONENTS    = "components";
    private static final String SC_PROCESSORS    = "processors";
    private static final String SC_TOPOLOGY      = "topology";

    /**
     * Each compoonent in a service chain (i.e., network function)
     * should have the following attributes.
     */
    private static final String COMP_NAME     = "name";
    private static final String COMP_TYPE     = "type";
    private static final String COMP_CLASS    = "class";
    private static final String COMP_ID       = "id";
    private static final String COMP_EL_CONF  = "elementConf";

    /**
     * Each compoonent in a service chain (i.e., network function)
     * should compose its processors using the following attributes.
     */
    private static final String NF_NAME       = COMP_NAME;
    private static final String NF_BLOCKS     = "blocks";
    private static final String NF_GRAPH      = "graph";

    /**
     * Each compoonent in a network function (i.e., processing block)
     * should have these attributes.
     * A block can have either configuration arguments or a config file.
     * |-> In the former case, leave the configFile empty.
     * |-> In the latter case, set the configArgs=FILE and
     * give the file path in configFile.
     */
    private static final String BLOCK_NAME      = "block";
    private static final String BLOCK_INST      = "instance";
    private static final String BLOCK_CONF_ARGS = "configArgs";
    private static final String BLOCK_CONF_FILE = "configFile";
    private static final String BLOCK_PORT      = "port";

    private static final String FILE_CONFIG_INDICATOR = "FILE";

    /**
     * Each processor instance in a service chain should be
     * a block array.
     * In this array, there must be a graph array with two
     * attributes, a source and destination.
     */
    private static final String PROC_GRAPH_SRC = "src";
    private static final String PROC_GRAPH_DST = "dst";

    /**
     * Each compoonent in a service chain (i.e., network function)
     * might have elements that require configuration.
     * Currently we support the configuration for following list of elements.
     * Note that the LookupIPRouteMP belongs to a large list lookup elements,
     * all of which use the same classification patterns. Use this element because
     * it is thread safe.
     */
    private static final String EL_SIMPLE_CLASSIFIER   =
        ProcessingBlockClass.SIMPLE_ETHERNET_CLASSIFIER.toString();
    private static final String EL_IP_FILTER           =
        ProcessingBlockClass.IP_FILTER.toString();
    private static final String EL_IP_CLASSIFIER       =
        ProcessingBlockClass.IP_CLASSIFIER.toString();
    private static final String EL_LOOKUP_CLASSIFIER   =
        ProcessingBlockClass.LOOKUP_IP_ROUTE_MP.toString();
    private static final String EL_OPENFLOW_CLASSIFIER =
        ProcessingBlockClass.OPENFLOW_CLASSIFIER.toString();

    /**
     * Each topology instance in a service chain
     * should have a server and network attribute.
     * The server attribute contains a service chain's
     * topology inside a server (connections between NFs).
     * The network attribute contains the ingress and egress
     * points (where traffic enters/exits) of the service chain.
     */
    private static final String TOPO_SERVER  = "server";
    private static final String TOPO_NETWORK = "network";

    /**
     * Each server instance in a topology should have
     * a source and destination attribute.
     * These two attributes define an NF connection.
     */
    private static final String TOPO_SERVER_SRC = "src";
    private static final String TOPO_SERVER_DST = "dst";

    /**
     * Each src or dst instance in a server topology
     * is represented by entity and interface names.
     */
    private static final String TOPO_SERVER_ENTITY = "entity";
    private static final String TOPO_SERVER_IFACE  = "interface";

    /**
     * Each network instance in a topology should have
     * two lists. One with ingress and another with egress points.
     */
    private static final String TOPO_NETWORK_INGRESS_POINTS = "ingressPoints";
    private static final String TOPO_NETWORK_EGRESS_POINTS  = "egressPoints";

    /**
     * Each ingress or egress point in a network topology should have
     * a device (dpid) and a port (number) ID.
     */
    private static final String TOPO_NETWORK_DEVICE_ID = "deviceId";
    private static final String TOPO_NETWORK_PORT_LIST = "portIds";
    private static final String TOPO_NETWORK_PORT_ID   = "portId";

    private ApplicationId appId = null;

    /**
     * Keep the number of service chains that have been loaded.
     * This number is ever increasing and it is used to create unique chain IDs.
     */
    private static int serviceChainCounter = 0;

    @Override
    public boolean isValid() {
        boolean result = hasOnlyFields(SC_TITLE);

        if (object.get(SC_TITLE) == null || object.get(SC_TITLE).size() < 1) {
            final String msg = "No service chain is present";
            throw new IllegalArgumentException(msg);
        }

        // Multiple service chains can be loaded
        for (JsonNode node : object.get(SC_TITLE)) {
            ObjectNode scNode = (ObjectNode) node;

            // Check for the main attributes of a service chain
            result &= hasOnlyFields(
                scNode,
                SC_NAME,
                SC_TYPE,
                SC_NETWORK_ID,
                SC_CPU_CORES,
                SC_MAX_CPU_CORES,
                SC_SCALE,
                SC_AUTO_SCALE,
                SC_SCOPE,
                SC_COMPONENTS,
                SC_PROCESSORS,
                SC_TOPOLOGY
            );

            result &= isString(scNode, SC_NAME, MANDATORY);
            result &= isString(scNode, SC_TYPE, MANDATORY);
            result &= isNumber(scNode, SC_NETWORK_ID, MANDATORY);
            result &= isNumber(scNode, SC_CPU_CORES, OPTIONAL);
            result &= isNumber(scNode, SC_MAX_CPU_CORES, MANDATORY);
            result &= isBoolean(scNode, SC_SCALE, MANDATORY);
            result &= isBoolean(scNode, SC_AUTO_SCALE, OPTIONAL);
            result &= isString(scNode, SC_SCOPE, MANDATORY);

            JsonNode compNode = scNode.path(SC_COMPONENTS);
            result &= (!compNode.isMissingNode() && compNode.isArray());

            // Verify component configuration
            try {
                result &= this.verifyComponentConf(compNode);
            } catch (IllegalArgumentException iaEx) {
                throw iaEx;
            }

            JsonNode procNode = scNode.path(SC_PROCESSORS);
            result &= (!procNode.isMissingNode() && procNode.isArray());

            // Verify processor configuration
            try {
                result &= this.verifyProcessorConf(procNode);
            } catch (IllegalArgumentException iaEx) {
                throw iaEx;
            }

            JsonNode topoNode = scNode.path(SC_TOPOLOGY);
            result &= (!topoNode.isMissingNode());

            // Verify topology configuration
            try {
                result &= this.verifyTopologyConf(topoNode);
            } catch (IllegalArgumentException iaEx) {
                throw iaEx;
            }
        }

        return result;
    }

    /**
     * Verifies the structure of the component configuration.
     *
     * @param compNode JSON node with component configuration
     * @return boolean verification status
     * @throws IllegalArgumentException
     */
    private boolean verifyComponentConf(JsonNode compNode) {
        if (compNode == null) {
            final String msg = "Component configuration is mandatory";
            throw new IllegalArgumentException(msg);
        }

        checkArgument(
            compNode.isArray(),
            "Service chain components are organized in a JSON array"
        );

        boolean result = true;

        for (JsonNode cn : compNode) {
            ObjectNode cnObjNode = (ObjectNode) cn;

            result &= hasOnlyFields(
                cnObjNode,
                COMP_NAME,
                COMP_TYPE,
                COMP_CLASS,
                COMP_ID
            );

            result &= isString(cnObjNode, COMP_NAME,  MANDATORY);
            result &= isString(cnObjNode, COMP_TYPE,  MANDATORY);
            result &= isString(cnObjNode, COMP_CLASS, MANDATORY);
            result &= isString(cnObjNode, COMP_ID,    MANDATORY);
        }

        if (!result) {
            log.error("Invalid component configuration");
        }

        return result;
    }

    /**
     * Verifies the structure of the processor configuration.
     *
     * @param procNode JSON node with processor configuration
     * @return boolean verification status
     * @throws IllegalArgumentException
     */
    private boolean verifyProcessorConf(JsonNode procNode) {
        if (procNode == null) {
            final String msg = "Processor configuration is mandatory";
            throw new IllegalArgumentException(msg);
        }

        checkArgument(
            procNode.isArray(),
            "Service chain processors are organized in a JSON array"
        );

        boolean result = true;

        for (JsonNode pn : procNode) {
            ObjectNode pnObjNode = (ObjectNode) pn;

            result &= hasOnlyFields(
                pnObjNode,
                NF_NAME,
                NF_BLOCKS,
                NF_GRAPH
            );

            result &= isString(pnObjNode, NF_NAME, MANDATORY);

            JsonNode jNfBlocks = pn.path(NF_BLOCKS);
            result &= (!jNfBlocks.isMissingNode() && jNfBlocks.isArray());

            JsonNode jNfGraph  = pn.path(NF_GRAPH);
            result &= (!jNfGraph.isMissingNode() && jNfGraph.isArray());
        }

        if (!result) {
            log.error("Invalid processor configuration");
        }

        return result;
    }

    /**
     * Verifies the structure of the topology configuration.
     *
     * @param topoNode JSON node with topology configuration
     * @return boolean verification status
     * @throws IllegalArgumentException
     */
    private boolean verifyTopologyConf(JsonNode topoNode) {
        if (topoNode == null) {
            final String msg = "Topology configuration is mandatory";
            throw new IllegalArgumentException(msg);
        }

        boolean result = true;

        // Check for the 2 main attributes of the configuration
        ObjectNode topoObjNode = (ObjectNode) topoNode;
        result &= hasOnlyFields(
            topoObjNode,
            TOPO_NETWORK,
            TOPO_SERVER
        );

        // A non-NULL JSON node is expected
        JsonNode networkNode = topoObjNode.path(TOPO_NETWORK);
        result &= (!networkNode.isMissingNode());

        // Two non-NULL JSON arrays are expected inside the network node
        JsonNode ingressPointsNode = networkNode.path(TOPO_NETWORK_INGRESS_POINTS);
        result &= (!ingressPointsNode.isMissingNode() && ingressPointsNode.isArray());

        checkArgument(
            ingressPointsNode.isArray(),
            "Service chain's ingress points are organized in a JSON array"
        );

        JsonNode egressPointsNode = networkNode.path(TOPO_NETWORK_EGRESS_POINTS);
        result &= (!egressPointsNode.isMissingNode() && egressPointsNode.isArray());

        checkArgument(
            egressPointsNode.isArray(),
            "Service chain's egress points are organized in a JSON array"
        );

        // A non-NULL JSON array is expected
        JsonNode serverNode = topoObjNode.path(TOPO_SERVER);
        result &= (!serverNode.isMissingNode() && serverNode.isArray());

        checkArgument(
            serverNode.isArray(),
            "Service chain's server topology is organized in a JSON array"
        );

        if (!result) {
            log.error("Invalid topology configuration");
        }

        return result;
    }

    @Override
    public Set<ServiceChainInterface> loadServiceChains(ApplicationId appId)
            throws IOException, InputConfigurationException {
        this.appId = appId;

        Set<ServiceChainInterface> scs = Sets.<ServiceChainInterface>newConcurrentHashSet();

        log.info("Loading service chains...");

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

            int scCores = scNode.path(SC_CPU_CORES).asInt();
            int scMaxCores = scNode.path(SC_MAX_CPU_CORES).asInt();
            checkArgument(
                scMaxCores > 0,
                "Please specify a positive number of maximum CPU cores " +
                "to be allocated for this service chain"
            );

            // If invalid input, use maximum
            if (scCores <= 0) {
                scCores = scMaxCores;
            }
            checkArgument(
                scCores <= scMaxCores,
                "The number of CPU cores to be allocated for this service chain " +
                "must not exceed the maximum number of CPU cores"
            );

            boolean scScale = scNode.path(SC_SCALE).asBoolean();
            boolean scAutoScale = false;
            if (scNode.path(SC_AUTO_SCALE).isBoolean()) {
                scAutoScale = scNode.path(SC_AUTO_SCALE).asBoolean();
            }
            if (!scScale && scAutoScale) {
                log.warn("Scale=false and autoScale=true are contradictive. Auto-setting scale=true!");
                scScale = true;
            }

            // Deployed on a single server (server-level) or across the network (network-wide)
            ServiceChainScope scScope = Common.<ServiceChainScope>enumFromString(
                ServiceChainScope.class,
                scNode.path(SC_SCOPE).asText().toLowerCase()
            );
            checkNotNull(
                scScope,
                "Invalid service chain scope. Choose one in: " + Common.<ServiceChainScope>
                enumTypesToString(
                    ServiceChainScope.class
                )
            );

            /**
             * A. Parse the components of the service chain
             */
            JsonNode compNode = scNode.path(SC_COMPONENTS);
            Map<String, Triple<String, String, String>> nfComponents =
                this.buildServiceChainComponents(compNode);

            /**
             * B. Parse the processors of the service chain.
             */
            JsonNode procNode = scNode.path(SC_PROCESSORS);
            Map<String, NetworkFunctionGraphInterface> nfGraphs =
                this.buildNfProcessors(procNode);

            // Ensure that the components (i.e., NFs) agree with the processors
            if (!Sets.difference(nfComponents.keySet(), nfGraphs.keySet()).isEmpty()) {
                throw new InputConfigurationException(
                    "Components do not agree with processors, check your JSON file"
                );
            }

            /**
             * C. Build the NFs of this service chain
             */
            Map<String, NetworkFunctionInterface> nfs =
                new ConcurrentHashMap<String, NetworkFunctionInterface>();

            for (Map.Entry<String, NetworkFunctionGraphInterface> entry : nfGraphs.entrySet()) {
                String nfName = entry.getKey();
                NetworkFunctionGraphInterface nfGraph = entry.getValue();

                Triple<String, String, String> compTriple = nfComponents.get(nfName);

                // Convert the type to an object
                NetworkFunctionType compType = Common.<NetworkFunctionType>enumFromString(
                    NetworkFunctionType.class, compTriple.getLeft()
                );
                checkNotNull(
                    compType,
                    "Invalid network function type. Choose one in: " + Common.<NetworkFunctionType>
                    enumTypesToString(
                        NetworkFunctionType.class
                    )
                );

                // Convert the NF class to an object
                NetworkFunctionClass compClass = Common.<NetworkFunctionClass>enumFromString(
                    NetworkFunctionClass.class, compTriple.getMiddle()
                );
                checkNotNull(
                    compClass,
                    "Invalid network function class. Choose one in: " + Common.<NetworkFunctionClass>
                    enumTypesToString(
                        NetworkFunctionClass.class
                    )
                );

                // Build the network function
                NetworkFunction.Builder nfBuilder = NetworkFunction.builder()
                    .networkFunctionGraph(nfGraph)
                    .id(compTriple.getRight())
                    .type(compType)
                    .nfClass(compClass)
                    .name(nfName);

                nfs.put(nfName, nfBuilder.build());
            }

            // We want the NF map sorted by key (ascending order)
            NfNameComparator bvc = new NfNameComparator(nfs);
            Map<String, NetworkFunctionInterface> sortedNfs =
                new ConcurrentSkipListMap<String, NetworkFunctionInterface>(bvc);
            sortedNfs.putAll(nfs);

            if (sortedNfs.size() != nfs.size()) {
                throw new InputConfigurationException(
                    "Error while sorting the NFs of service chain " + scName
                );
            }

            /**
             * D. Parse the topology of the service chain
             * and build the service chain.
             */
            JsonNode topoNode = scNode.path(SC_TOPOLOGY);
            ServiceChain.Builder scBuilder = buildServiceChainTopology(
                topoNode,
                sortedNfs,
                scName,
                scType,
                scScope,
                scNetId,
                scCores,
                scMaxCores,
                scScale,
                scAutoScale
            );

            // Add this service chain to the set to be returned
            scs.add(scBuilder.build());
        }

        log.info("Loaded {} service chain(s): {}", scs.size(), scs.toString());

        return scs;
    }

    /**
     * Handles the 'components' attribute of the JSON configuration.
     *
     * @param compNode the JSON node that holds the input configuration
     * @return a map of NF names to a triple of NF type + function + ID
     */
    private Map<String, Triple<String, String, String>>
            buildServiceChainComponents(JsonNode compNode) {
        Map<String, Triple<String, String, String>> nfComponents =
            new ConcurrentHashMap<String, Triple<String, String, String>>();

        for (JsonNode cn : compNode) {
            ObjectNode cnNode = (ObjectNode) cn;

            String compName     = cnNode.path(COMP_NAME).asText().toLowerCase();
            String compTypeStr  = cnNode.path(COMP_TYPE).asText().toLowerCase();
            String compClassStr = cnNode.path(COMP_CLASS).asText().toLowerCase();
            String compID       = cnNode.path(COMP_ID).asText().toLowerCase();

            checkArgument(
                !Strings.isNullOrEmpty(compName),
                "Please specify a name for this network function"
            );
            checkArgument(
                !Strings.isNullOrEmpty(compTypeStr),
                "Please specify a type for this network function"
            );
            checkArgument(
                !Strings.isNullOrEmpty(compClassStr),
                "Please specify a class for this network function"
            );
            checkArgument(
                !Strings.isNullOrEmpty(compID),
                "Please specify an ID for this network function"
            );

            nfComponents.put(compName, new ImmutableTriple(compTypeStr, compClassStr, compID));
        }

        return nfComponents;
    }

    /**
     * Handles the 'processors' attribute of the JSON configuration.
     *
     * @param procNode the JSON node that holds the input configuration
     * @return a map of NF names to NF processing graphs
     */
    private Map<String, NetworkFunctionGraphInterface> buildNfProcessors(JsonNode procNode) {
        // Stores the processing graph of each NF
        Map<String, NetworkFunctionGraphInterface> nfGraphs =
            new ConcurrentHashMap<String, NetworkFunctionGraphInterface>();

        for (JsonNode pn : procNode) {
            ObjectNode pnNode = (ObjectNode) pn;

            String   nfName    = pnNode.path(NF_NAME).asText().toLowerCase();
            JsonNode jNfBlocks = pnNode.path(NF_BLOCKS);
            JsonNode jNfGraph  = pnNode.path(NF_GRAPH);

            checkArgument(
                !Strings.isNullOrEmpty(nfName),
                "Please specify a valid network function name to host this processor"
            );

            Set<NetworkFunctionEdgeInterface>   nfEdges    =
                    Sets.<NetworkFunctionEdgeInterface>newConcurrentHashSet();
            Set<NetworkFunctionVertexInterface> nfVertices =
                    Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();
            Set<String> nfInstances = new ConcurrentSkipListSet<String>();
            Map<String, Integer> multiportMap = new ConcurrentSkipListMap<String, Integer>();

            /**
             * First parse the 'blocks' attribute of the JSON file
             * and populate relevant information into the input data
             * structures.
             * This method returns a complete set of vertices.
             */
            this.parseBlocks(jNfBlocks, nfVertices, nfInstances, multiportMap);

            /**
             * Then parse the 'graph' attribute of the JSON file
             * and populate relevant information into the input data
             * structures.
             * This method returns a complete set of edges.
             */
            this.parseGraph(jNfGraph, nfVertices, nfEdges, nfInstances, multiportMap);

            /**
             * Now we have both the vertices and edges.
             * We are ready to create the processing graph of this NF.
             */
            NetworkFunctionGraphInterface nfGraph = new NetworkFunctionGraph(nfVertices, nfEdges);
            if (nfGraph == null) {
                throw new InputConfigurationException(
                    "Failed to build the processing graph for NF " + nfName
                );
            }

            nfGraphs.put(nfName, nfGraph);
        }

        return nfGraphs;
    }

    /**
     * Handles the 'blocks' attribute of a processor.
     *
     * @param jNfBlocks the JSON node that holds the NF blocks
     * @param nfVertices an empty set of NF vertices to be filled
     * @param nfInstances an empty set of NF instances to be filled
     * @param multiportMap a map of instances to their ports
     */
    private void parseBlocks(
            JsonNode jNfBlocks,
            Set<NetworkFunctionVertexInterface> nfVertices,
            Set<String> nfInstances,
            Map<String, Integer> multiportMap) {
        // Iterate through all the blocks of this NF and create vertices
        for (JsonNode jBlock : jNfBlocks) {
            ObjectNode blNode = (ObjectNode) jBlock;

            String blockNameStr = blNode.path(BLOCK_NAME).asText();
            String blockInstStr = blNode.path(BLOCK_INST).asText();
            String blockConfArgsStr = blNode.path(BLOCK_CONF_ARGS).asText();
            String blockConfFileStr = blNode.path(BLOCK_CONF_FILE).asText();

            ProcessingBlockClass blockClass = ProcessingBlockClass.getByName(blockNameStr);
            if (blockClass == null) {
                throw new InputConfigurationException(
                    "There is no processing block with name " + blockNameStr +
                    "; cannot proceed with the processor"
                );
            }

            ProcessingBlockInterface blockInstance = ProcessingBlockLauncher.getInstance(
                blockClass, blockInstStr, blockConfArgsStr, blockConfFileStr
            );
            if (blockInstance == null) {
                throw new InputConfigurationException(
                    "Unable to retrieve an instance of " + blockNameStr
                );
            }

            // If there is additional external configuration, take care of it
            this.loadExternalConfiguration(
                blockInstance, blockNameStr,
                blockInstStr, blockConfArgsStr,
                blockConfFileStr, multiportMap
            );

            /**
             * After loading the configuration, it is time to populate it
             * to the inheriting processing blocks.
             */
            blockInstance.populateConfiguration();

            NetworkFunctionVertexInterface blockVertex = new NetworkFunctionVertex(blockInstance);
            nfVertices.add(blockVertex);
            nfInstances.add(blockInstStr);
        }
    }

    /**
     * Handles the 'graph' attribute of a processor.
     *
     * @param jNfGraph the JSON node that holds the NF graph
     * @param nfVertices a set of vertices in the NF graph
     * @param nfEdges a set of edges in the NF graph
     * @param nfInstances a set of NF instances
     * @param multiportMap a map of instances to their ports
     */
    private void parseGraph(
            JsonNode jNfGraph,
            Set<NetworkFunctionVertexInterface> nfVertices,
            Set<NetworkFunctionEdgeInterface> nfEdges,
            Set<String> nfInstances,
            Map<String, Integer> multiportMap) {
        Map<String, Set<String>> replicas = new ConcurrentSkipListMap<String, Set<String>>();

        // Iterate the graph of this NF and create links between vertices
        for (JsonNode jBlock : jNfGraph) {
            JsonNode srcNode    = jBlock.path(PROC_GRAPH_SRC);
            ObjectNode srcGNode = (ObjectNode) srcNode;
            String srcInstance  = srcGNode.path(BLOCK_INST).asText();
            int origSrcPortInt  = srcGNode.path(BLOCK_PORT).asInt();
            int srcPortInt      = 0;
            int replPorts       = -1;

            if (!nfInstances.contains(srcInstance)) {
                throw new InputConfigurationException(
                    "Block instance " + srcInstance + " does no appear as a processor"
                );
            }

            NetworkFunctionVertexInterface srcVertex = this.findNfVertexByName(nfVertices, srcInstance);
            if (srcVertex == null) {
                throw new InputConfigurationException(
                    "Block instance " + srcInstance + " does no appear as a processor"
                );
            }

            /**
             * Some source elements might link all of their output
             * ports to the next element. Find the number of ports
             * and use this number instead of ALL_OUTPUT_PORTS
             */
            if (origSrcPortInt == ALL_OUTPUT_PORTS) {
                if (multiportMap.get(srcInstance) > 1) {
                    replPorts = multiportMap.get(srcInstance);
                } else {
                    srcPortInt = 0;
                }
            } else if (origSrcPortInt < 0) {
                throw new InputConfigurationException(
                    "Source block instance " + srcInstance +
                    " has a negative output port that is not -1 (means all)"
                );
            } else {
                srcPortInt = origSrcPortInt;
            }

            log.debug("\tSrc Block Instance  {}", srcInstance);
            log.debug("\tSrc Port  Number    {}", srcPortInt);
            log.debug("\tNumber of replicas  {}", replPorts);

            JsonNode dstNode    = jBlock.path(PROC_GRAPH_DST);
            ObjectNode dstGNode = (ObjectNode) dstNode;
            String dstInstance  = dstGNode.path(BLOCK_INST).asText();
            int dstPortInt      = dstGNode.path(BLOCK_PORT).asInt();

            if (!nfInstances.contains(dstInstance)) {
                throw new InputConfigurationException(
                    "Block instance " + dstInstance + " does no appear as a processor"
                );
            }

            NetworkFunctionVertexInterface dstVertex = this.findNfVertexByName(nfVertices, dstInstance);
            if (dstVertex == null) {
                throw new InputConfigurationException(
                    "Block instance " + dstVertex + " does no appear as a processor"
                );
            }

            if (dstPortInt < 0) {
                throw new InputConfigurationException(
                    "Destination block instance " + dstInstance + " has a negative input port"
                );
            }

            log.debug("\tDst Block Instance  {}", dstInstance);
            log.debug("\tDst Port  Number    {}", dstPortInt);

            // Set up the port bindings between the two blocks
            srcVertex.addOutputBlock(srcPortInt, dstVertex.processingBlock());
            dstVertex.addInputBlock(dstPortInt,  srcVertex.processingBlock());

            // A single connection between them
            nfEdges.add(new NetworkFunctionEdge(srcVertex, dstVertex));

            /**
             * This block has multiple output ports that lead to the same node.
             * We need to replicate the destination vertex and keep track of this
             * action.
             */
            if (replPorts > 0) {
                Set<String> repl = new ConcurrentSkipListSet<String>();

                for (short i = 1; i < replPorts; i++) {
                    // Clone the destination vertex
                    NetworkFunctionVertexInterface dstVertexRepl = dstVertex.clone();
                    nfVertices.add(dstVertexRepl);

                    // Add bindings with the source
                    srcVertex.addOutputBlock(i, dstVertexRepl.processingBlock());
                    dstVertexRepl.addInputBlock(i, srcVertex.processingBlock());

                    // Create a new edge in the graph
                    nfEdges.add(new NetworkFunctionEdge(srcVertex, dstVertexRepl));

                    /**
                     * Store this replica locally; we will need this information when we
                     * meet this node in the future.
                     */
                    repl.add(dstVertexRepl.processingBlock().id());
                }

                // Now we know the replicas of this vertex
                replicas.put(dstInstance, repl);
            }

            /**
             * In the case that a destination vertex is replicated (see above),
             * when this vertex becomes a source, it also needs multiple connections
             * (i.e., one for each replica) with the following vertices.
             */
            Set<String> replicasOfThisSrcBlock = replicas.get(srcInstance);
            if (replicasOfThisSrcBlock != null) {
                // For each replica create a new connection to the next vertex
                for (String r : replicasOfThisSrcBlock) {
                    NetworkFunctionVertexInterface srcRep = this.findNfVertexByName(nfVertices, r);

                    // Set up the port bindings between the two blocks
                    srcRep.addOutputBlock(srcPortInt, dstVertex.processingBlock());
                    dstVertex.addInputBlock(dstPortInt, srcRep.processingBlock());

                    // A single connection between them
                    nfEdges.add(new NetworkFunctionEdge(srcRep, dstVertex));
                }
            }

            log.debug("");
        }
    }

    /**
     * Handles the 'topology' attribute of the JSON configuration.
     *
     * @param topoNode the JSON node that holds the input configuration
     * @param nfs a map of NF names to NF processing graphs
     * @param scName the name of the service chain
     * @param scType the type of the service chain
     * @param scScope the scope of the service chain
     * @param scNetId the network ID of the service chain
     * @param scCpuCores the number of CPU cores for the service chain
     * @param scMaxCpuCores the maximum number of CPU cores for the service chain
     * @param scScale the scaling flag for the service chain
     * @param scAutoScale the auto scaling flag for the service chain
     * @return a service chain object
     */
    private ServiceChain.Builder buildServiceChainTopology(
            JsonNode                              topoNode,
            Map<String, NetworkFunctionInterface> nfs,
            String                                scName,
            String                                scType,
            ServiceChainScope                     scScope,
            NetworkId                             scNetId,
            int                                   scCpuCores,
            int                                   scMaxCpuCores,
            boolean                               scScale,
            boolean                               scAutoScale) {
        Set<ServiceChainVertexInterface> scVertices = Sets.<ServiceChainVertexInterface>newConcurrentHashSet();
        Set<ServiceChainEdgeInterface> scEdges = Sets.<ServiceChainEdgeInterface>newConcurrentHashSet();

        // Network attribute
        JsonNode networkNode = topoNode.path(TOPO_NETWORK);
        JsonNode ingressPointsNode = networkNode.path(TOPO_NETWORK_INGRESS_POINTS);
        JsonNode egressPointsNode  = networkNode.path(TOPO_NETWORK_EGRESS_POINTS);

        // Read ingress and egress points
        Set<TrafficPoint> ingressPoints = this.loadTrafficPoints(ingressPointsNode, "ingress");
        Set<TrafficPoint> egressPoints  = this.loadTrafficPoints(egressPointsNode,  "egress");

        // Server attribute
        JsonNode serverNode = topoNode.path(TOPO_SERVER);

        int nfsNumber = nfs.size();
        String lastNfName = "";

        for (JsonNode ts : serverNode) {
            JsonNode srcNode    = ts.path(TOPO_SERVER_SRC);
            String srcEntity    = srcNode.path(TOPO_SERVER_ENTITY).asText().toLowerCase();
            String srcInterface = srcNode.path(TOPO_SERVER_IFACE).asText().toLowerCase();

            checkArgument(
                !Strings.isNullOrEmpty(srcEntity),
                "Please specify a valid source entity for the service chain graph"
            );
            checkArgument(
                !Strings.isNullOrEmpty(srcInterface),
                "Please specify a valid source interface for the service chain graph"
            );

            ServiceChainVertexInterface srcVertex = null;
            ServiceChainVertexInterface dstVertex = null;

            NetworkFunctionDevice srcNfDev = null;
            NetworkFunctionDevice dstNfDev = null;

            boolean trafficSrc = false;
            boolean trafficDst = false;

            // Check if this name is an NF
            NetworkFunctionInterface srcNf = nfs.get(srcEntity);

            if (srcNf != null) {
                // Add the interface to the NF
                srcNfDev = new NetworkFunctionDevice(
                    srcInterface,
                    scNetId,
                    DeviceId.deviceId(srcEntity + ":" + srcInterface)
                );
                srcNf.addDevice(srcNfDev);

                /**
                 * In case that this service chain consists of
                 * only one NF, we keep its name here and use it
                 * later on.
                 */
                lastNfName = srcEntity;
            } else {
                // This is a traffic source
                trafficSrc = true;
            }

            JsonNode dstNode    = ts.path(TOPO_SERVER_DST);
            String dstEntity    = dstNode.path(TOPO_SERVER_ENTITY).asText().toLowerCase();
            String dstInterface = dstNode.path(TOPO_SERVER_IFACE).asText().toLowerCase();

            checkArgument(
                !Strings.isNullOrEmpty(dstEntity),
                "Please specify a valid destination entity for the service chain graph"
            );
            checkArgument(
                !Strings.isNullOrEmpty(dstInterface),
                "Please specify a valid destination interface for the service chain graph"
            );

            // Check if this name is an NF
            NetworkFunctionInterface dstNf = nfs.get(dstEntity);

            // If so, it must be different from the src NF (to prevent loops).
            if ((dstNf != null) && (srcNf != dstNf)) {
                dstNfDev = new NetworkFunctionDevice(
                    dstInterface,
                    scNetId,
                    DeviceId.deviceId(dstEntity + ":" + dstInterface)
                );
                dstNf.addDevice(dstNfDev);

                if (trafficSrc) {
                    dstNf.addEntryDevice(dstNfDev);
                }
            } else {
                // This is a traffic destination
                trafficDst = true;
            }

            // The source NF will get a new exit device
            if (trafficDst) {
                if (srcNf != null) {
                    srcNf.addExitDevice(srcNfDev);
                }
            }

            // Create an edge only when both vertices are set
            if ((srcNf != null) && (dstNf != null) && (srcNf != dstNf)) {
                // Set the peering information
                srcNf.addPeerNF(srcNfDev, dstNf);
                dstNf.addPeerNF(dstNfDev, srcNf);
                srcNf.addPeerInterface(srcNfDev, dstInterface);
                dstNf.addPeerInterface(dstNfDev, srcInterface);

                // Wrap these NFs using graph vertices
                srcVertex = new ServiceChainVertex(srcNf);
                dstVertex = new ServiceChainVertex(dstNf);

                // Add them into the graph
                scVertices.add(srcVertex);
                scVertices.add(dstVertex);

                // Link them with an edge
                ServiceChainEdgeInterface edge = new ServiceChainEdge(srcVertex, dstVertex);
                scEdges.add(edge);
            }
        }

        // This is the only case we need to handle separately (Uni-NF chain)
        if (nfsNumber == 1) {
            ServiceChainVertexInterface onlyVertex = new ServiceChainVertex(
                nfs.get(lastNfName)
            );
            scVertices.add(onlyVertex);
        }

        ServiceChainGraphInterface scGraph = new ServiceChainGraph(scVertices, scEdges);

        // Build the service chain
        ServiceChain.Builder scBuilder = ServiceChain.builder()
            .name(scName)
            .type(scType)
            .scope(scScope)
            .id("sc:" + scName + ":" + scType + ":" +  scNetId.toString())
            .cpuCores(scCpuCores)
            .maxCpuCores(scMaxCpuCores)
            .withScalingAbility(scScale)
            .withAutoScalingAbility(scAutoScale)
            .serviceChainGraph(scGraph)
            .ingressPoints(ingressPoints)
            .egressPoints(egressPoints);

        serviceChainCounter++;

        log.info(
            "Service chain with {} vertices and {} edges is created",
            scVertices.size(),
            scEdges.size()
        );

        return scBuilder;
    }

    /**
     * Read traffic points of a given type (ingress or egress) from a JSON node.
     *
     * @param trafficPointsNode input JSON node with traffic points
     * @param tpType the type of this traffic point (ingress or egress)
     * @return set of TrafficPoint objects
     */
    private Set<TrafficPoint> loadTrafficPoints(JsonNode trafficPointsNode, String tpType) {
        Set<TrafficPoint> trafficPoints = Sets.<TrafficPoint>newConcurrentHashSet();

        // Iterate the JSON array of traffic points
        for (JsonNode tp : trafficPointsNode) {
            ObjectNode tpNode = (ObjectNode) tp;

            // Each entry of the array has device ID
            String tpDev = tpNode.path(TOPO_NETWORK_DEVICE_ID).asText();

            // And an array of ports
            JsonNode tpPortsNode = tpNode.path(TOPO_NETWORK_PORT_LIST);
            checkArgument(
                tpPortsNode.isArray(),
                TOPO_NETWORK_PORT_LIST + " attribute is an array"
            );

            Set<Long> ports = Sets.<Long>newConcurrentHashSet();

            for (JsonNode tpPort : tpPortsNode) {
                ObjectNode tpPortNode = (ObjectNode) tpPort;
                long port  = tpPortNode.path(TOPO_NETWORK_PORT_ID).asLong();
                ports.add(new Long(port));
            }

            // Build a traffic point
            TrafficPoint.Builder trafficPointsBuilder = TrafficPoint.builder()
                .type(tpType)
                .deviceId(tpDev)
                .portIds(ports);

            // We have a traffic point
            trafficPoints.add(trafficPointsBuilder.build());
        }

        return trafficPoints;
    }

    /**
     * Search into the set of input vertices to find one with the given name.
     *
     * @param nfVertices set of NF vertices
     * @param nfVertexName the name of the block withing this vertex
     * @return NetworkFunctionVertexInterface the vertex with the given name
     */
    private NetworkFunctionVertexInterface findNfVertexByName(
            Set<NetworkFunctionVertexInterface> nfVertices, String nfVertexName) {
        for (NetworkFunctionVertexInterface ver : nfVertices) {
            ProcessingBlockInterface block = ver.processingBlock();
            if (block.id().equals(nfVertexName)) {
                return ver;
            }
        }

        return null;
    }

    /**
     * Depending on the type of the block, check whether there is
     * available configuration in an external file and load it.
     *
     * @param block the processing block
     * @param blockName the name of the block
     * @param blockInstance the name of this instance
     * @param blockConfArgs the basic configuration of the block
     * @param blockConfFile the external configuration is in this file
     */
    private void loadExternalConfiguration(
            ProcessingBlockInterface block,
            String blockName,
            String blockInstance,
            String blockConfArgs,
            String blockConfFile,
            Map<String, Integer> multiportMap) {

        // TODO: Allow IPRewriters to be configured externally
        // Only these block types have external configuration
        // if (!ProcessingBlockClass.isClassifier(blockName) &&
        //     !ProcessingBlockClass.isRewriter(blockName)) {
        //     return;
        // }
        if (!ProcessingBlockClass.isClassifier(blockName)) {
            return;
        }

        /**
         * These two block types require:
         * |-> configArgs=FILE
         * |-> configFile=path-to-file
         */
        if (!blockConfArgs.equals(FILE_CONFIG_INDICATOR)) {
            throw new InputConfigurationException(
                blockName + " must have configArgs=FILE"
            );
        }

        // Check the file
        this.checkForValidFile(blockConfFile, blockName);

        BasicConfigurationInterface baseConf = null;

        // Classifiers
        if (block instanceof ClassifierBlock) {
            baseConf = this.readRules(blockConfFile, blockName, blockInstance);
            RuleConfigurationInterface ruleConf = (RuleConfigurationInterface) baseConf;

            // Now insert this configuration into the block
            ClassifierBlock clBlockInstance = (ClassifierBlock) block;
            clBlockInstance.setRuleConf(ruleConf);

            multiportMap.put(blockInstance, new Integer(clBlockInstance.outputPortsNumber()));
        // vs Rewriters
        } else if (block instanceof IpRewriter) {
            baseConf = this.readPatterns(blockConfFile, blockInstance);
            PatternConfigurationInterface patConf = (PatternConfigurationInterface) baseConf;

            // Now insert this configuration into the block
            IpRewriter rwBlockInstance = (IpRewriter) block;
            rwBlockInstance.setPatternConf(patConf);

            multiportMap.put(blockInstance, new Integer(rwBlockInstance.outputPortsNumber()));
        }
    }

    /**
     * Check whether the given file is valid.
     *
     * @param blockConfFile the external configuration is in this file
     * @param blockName the name of the block
     * @throws InputConfigurationException if the file does not exist or is not loaded
     */
    private void checkForValidFile(String blockConfFile, String blockName) {
        if (blockConfFile.isEmpty()) {
            throw new InputConfigurationException(
                blockName + " must have configFile set properly"
            );
        }

        // Attempt to read the file
        try {
            Path filePath = Paths.get(blockConfFile);
            BasicFileAttributes basicAttr = Files.readAttributes(
                filePath, BasicFileAttributes.class
            );
            if (!basicAttr.isRegularFile()) {
                throw new InputConfigurationException(
                    "Configuration file for " + blockName + " does not exist"
                );
            }
        } catch (IllegalArgumentException | UnsupportedOperationException |
                FileSystemNotFoundException | SecurityException | IOException ex) {
            throw new InputConfigurationException(
                "Problem while loading file " + blockConfFile
            );
        }

        return;
    }

    @Override
    public BasicConfigurationInterface readRules(
            String blockConfFile, String blockName, String blockInstance) {
        RuleConfigurationInterface ruleConf = null;

        try {
            log.info("Reading rules from file: {}", blockConfFile);
            if        (blockName.equals(EL_SIMPLE_CLASSIFIER)) {
                ruleConf = new ClassifierRuleConfiguration(this.appId, blockInstance, blockConfFile);
            } else if (blockName.equals(EL_LOOKUP_CLASSIFIER)) {
                ruleConf = new LookupRuleConfiguration(this.appId, blockInstance, blockConfFile);
            } else if (blockName.equals(EL_IP_CLASSIFIER)) {
                ruleConf = new IpClassifierRuleConfiguration(this.appId, blockInstance, blockConfFile);
            } else if (blockName.equals(EL_OPENFLOW_CLASSIFIER)) {
                ruleConf = new OpenFlowRuleConfiguration(this.appId, blockInstance, blockConfFile);
            } else if (blockName.equals(EL_IP_FILTER)) {
                ruleConf = new IpFilterRuleConfiguration(this.appId, blockInstance, blockConfFile);
            }

            ruleConf.loadConfiguration();
            ruleConf.loadRules();
            // ruleConf.printRules();
        } catch (IOException | JsonParseException ex) {
            throw new InputConfigurationException(
                "Problem while reading rules from file: " + blockConfFile
            );
        }

        return ruleConf;
    }

    @Override
    public BasicConfigurationInterface readPatterns(
            String blockConfFile, String blockInstance) {
        PatternConfigurationInterface patConf = null;

        try {
            log.info("Reading patterns from file: {}", blockConfFile);
            patConf = new PatternConfiguration(this.appId, blockInstance, blockConfFile);

            patConf.loadConfiguration();
            patConf.loadPatterns();
            // patConf.printPatterns();

        } catch (IOException | JsonParseException ex) {
            throw new InputConfigurationException(
                "Problem while reading patterns from file: " + blockConfFile
            );
        }

        return patConf;
    }

}
