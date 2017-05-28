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

package org.onosproject.metron.impl.servicechain;

// Metron libraries
import org.onosproject.metron.api.common.GenericId;
import org.onosproject.metron.api.config.RuleConfigurationInterface;
import org.onosproject.metron.api.exceptions.SynthesisException;
import org.onosproject.metron.api.exceptions.ServiceChainException;
import org.onosproject.metron.api.exceptions.NetworkFunctionException;
import org.onosproject.metron.api.exceptions.ProcessingBlockException;
import org.onosproject.metron.api.graphs.NetworkFunctionEdgeInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.graphs.ServiceChainEdgeInterface;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.net.ClickFlowRuleAction;
import org.onosproject.metron.api.networkfunction.NetworkFunctionId;
import org.onosproject.metron.api.networkfunction.NetworkFunctionClass;
import org.onosproject.metron.api.networkfunction.NetworkFunctionType;
import org.onosproject.metron.api.networkfunction.NetworkFunctionState;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionDeviceInterface;
import org.onosproject.metron.api.processing.Priority;
import org.onosproject.metron.api.processing.ProcessingBlockType;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainDelegate;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainStoreInterface;
import org.onosproject.metron.api.servicechain.ServiceChainManagerInterface;
import org.onosproject.metron.api.servicechain.ServiceChainLocationInterface;
import org.onosproject.metron.api.structures.Pair;
import org.onosproject.metron.api.structures.TreeNode;
import org.onosproject.metron.api.structures.StackNode;
import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.ClassificationSyntax;
import org.onosproject.metron.api.topology.ServerLocationInterface;

import org.onosproject.metron.impl.common.UUIdGenerator;
import org.onosproject.metron.impl.config.RuleConfiguration;
import org.onosproject.metron.impl.config.ClickRuleConfiguration;
import org.onosproject.metron.impl.config.ClassifierRuleConfiguration;
import org.onosproject.metron.impl.config.LookupRuleConfiguration;
import org.onosproject.metron.impl.config.IpClassifierRuleConfiguration;
import org.onosproject.metron.impl.graphs.ServiceChainGraph;
import org.onosproject.metron.impl.graphs.ServiceChainEdge;
import org.onosproject.metron.impl.graphs.ServiceChainVertex;
import org.onosproject.metron.impl.graphs.NetworkFunctionEdge;
import org.onosproject.metron.impl.graphs.NetworkFunctionVertex;
import org.onosproject.metron.impl.graphs.NetworkFunctionGraph;
import org.onosproject.metron.impl.networkfunction.NetworkFunction;
import org.onosproject.metron.impl.networkfunction.NetworkFunctionDevice;
import org.onosproject.metron.impl.processing.ProcessingBlock;
import org.onosproject.metron.impl.processing.ClassifierBlock;
import org.onosproject.metron.impl.processing.ModifierBlock;
import org.onosproject.metron.impl.processing.MonitorBlock;
import org.onosproject.metron.impl.processing.ShaperBlock;
import org.onosproject.metron.impl.processing.StatefulModifierBlock;
import org.onosproject.metron.impl.processing.TerminalBlock;
import org.onosproject.metron.impl.processing.TransparentBlock;
import org.onosproject.metron.impl.processing.blocks.CheckIpHeader;
import org.onosproject.metron.impl.processing.blocks.Classifier;
import org.onosproject.metron.impl.processing.blocks.DecIpTtl;
import org.onosproject.metron.impl.processing.blocks.Device;
import org.onosproject.metron.impl.processing.blocks.Discard;
import org.onosproject.metron.impl.processing.blocks.DpdkDevice;
import org.onosproject.metron.impl.processing.blocks.FromDevice;
import org.onosproject.metron.impl.processing.blocks.FromDpdkDevice;
import org.onosproject.metron.impl.processing.blocks.IpClassifier;
import org.onosproject.metron.impl.processing.blocks.LinuxDevice;
import org.onosproject.metron.impl.processing.blocks.LookupIpRouteMp;
import org.onosproject.metron.impl.processing.blocks.MarkIpHeader;
import org.onosproject.metron.impl.processing.blocks.OpenFlowClassifier;
import org.onosproject.metron.impl.processing.blocks.Queue;
import org.onosproject.metron.impl.processing.blocks.SimpleEthernetClassifier;
import org.onosproject.metron.impl.processing.blocks.ToDevice;
import org.onosproject.metron.impl.processing.blocks.ToDpdkDevice;
import org.onosproject.metron.impl.classification.VisualTree;
import org.onosproject.metron.impl.classification.ClassificationTree;

// ONOS libraries
import org.onlab.graph.Edge;
import org.onlab.graph.Vertex;
import org.onlab.graph.Graph;
import org.onlab.graph.MutableGraph;
import org.onlab.graph.MutableAdjacencyListsGraph;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ElementId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criteria.DummyCriterion;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.ConsistentMap;
import org.onlab.util.KryoNamespace;

// Apache libraries
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
// import org.apache.felix.scr.annotations.Service;

// Other libraries
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.cfg.ContextAttributes.Impl;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.MoreObjects;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.HashMultimap;

// Java libraries
import java.io.File;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashMap;
// import java.util.concurrent.ExecutorService;

// import static org.onlab.util.Tools.groupedThreads;
import static com.google.common.base.Preconditions.checkNotNull;
// import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Manages the registry of service chains in the ONOS core.
 * Works as a strongly consistent distributed store.
 */
@Component(immediate = true)
// @Service
public class StronglyConsistentServiceChainStore
        extends AbstractStore<ServiceChainEvent, ServiceChainDelegate>
        implements ServiceChainStoreInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String COMPONET_LABEL = "Strongly-consistent Dist. Store";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // private final ExecutorService eventExecutor =
    //     newSingleThreadScheduledExecutor(groupedThreads("metron/servicechain", "event-handler", log));

    // A strongly consistent distributed store
    private ConsistentMap<ServiceChainId, ServiceChainInterface> serviceChainMap;

    // A listener to catch important events taking place in the store
    private final MapEventListener<ServiceChainId, ServiceChainInterface>
        serviceChainStoreListener = new InternalMapListener();

    @Activate
    protected void activate() {
        /**
         * The serializer of our distributed data store.
         * Poor guy, it has to deal with this storm of objects.
         */
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(
                Set.class,
                Map.class,
                LinkedHashMap.class,
                SetMultimap.class,
                HashMultimap.class,
                MoreObjects.class,
                ObjectMapper.class,
                DeserializationConfig.class,
                Impl.class,
                ArrayNode.class,
                ObjectNode.class,
                TextNode.class,
                IntNode.class,
                JsonNodeFactory.class,
                File.class,
                Edge.class,
                Vertex.class,
                Graph.class,
                MutableGraph.class,
                MutableAdjacencyListsGraph.class,
                NetworkId.class,
                ElementId.class,
                GenericId.class,
                UUIdGenerator.class,
                ListenerService.class,
                Priority.class,
                ProcessingBlockType.class,
                RuleConfiguration.class,
                ClickRuleConfiguration.class,
                ClassifierRuleConfiguration.class,
                LookupRuleConfiguration.class,
                IpClassifierRuleConfiguration.class,
                ClickFlowRuleAction.class,
                SynthesisException.class,
                ServiceChainException.class,
                NetworkFunctionException.class,
                ProcessingBlockException.class,
                Pair.class,
                TreeNode.class,
                StackNode.class,
                TextualPacketFilter.class,
                ClassificationSyntax.class,
                ClassificationTree.class,
                VisualTree.class,
                FlowRule.class,
                DummyCriterion.class,
                ProcessingBlockInterface.class,
                ProcessingBlock.class,
                ClassifierBlock.class,
                ModifierBlock.class,
                MonitorBlock.class,
                ShaperBlock.class,
                StatefulModifierBlock.class,
                TerminalBlock.class,
                TransparentBlock.class,
                CheckIpHeader.class,
                Classifier.class,
                DecIpTtl.class,
                Device.class,
                Discard.class,
                DpdkDevice.class,
                FromDevice.class,
                FromDpdkDevice.class,
                IpClassifier.class,
                LinuxDevice.class,
                LookupIpRouteMp.class,
                MarkIpHeader.class,
                OpenFlowClassifier.class,
                Queue.class,
                SimpleEthernetClassifier.class,
                ToDevice.class,
                ToDpdkDevice.class,
                NetworkFunctionId.class,
                NetworkFunctionClass.class,
                NetworkFunctionType.class,
                NetworkFunctionState.class,
                NetworkFunctionDevice.class,
                NetworkFunctionVertexInterface.class,
                NetworkFunctionEdgeInterface.class,
                NetworkFunctionGraphInterface.class,
                NetworkFunctionGraph.class,
                NetworkFunctionEdge.class,
                NetworkFunctionVertex.class,
                NetworkFunction.class,
                ServiceChainEvent.class,
                ServiceChainDelegate.class,
                ServerLocationInterface.class,
                ServiceChainLocationInterface.class,
                ServiceChainId.class,
                ServiceChainState.class,
                ServiceChainVertexInterface.class,
                ServiceChainEdgeInterface.class,
                ServiceChainGraphInterface.class,
                ServiceChainInterface.class,
                ServiceChainManagerInterface.class,
                ServiceChainVertex.class,
                ServiceChainEdge.class,
                ServiceChainGraph.class,
                ServiceChain.class
            );

        // Build the strongly consistent distributed store
        this.serviceChainMap = this.storageService.<ServiceChainId, ServiceChainInterface>
            consistentMapBuilder()
                .withName("onos-service-chain-store")
                .withSerializer(Serializer.using(serializer.build()))
                .withPurgeOnUninstall()
                .build();

        // Add a listener
        this.serviceChainMap.addListener(serviceChainStoreListener);

        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        // Remove the listener before exiting
        this.serviceChainMap.removeListener(serviceChainStoreListener);

        // Shutdown this thread
        // eventExecutor.shutdown();

        log.info("[{}] Stopped", this.label());
    }

    @Override
    public boolean createServiceChain(ServiceChainId chainId, ServiceChainInterface sc) {
        if (chainId == null || sc == null) {
            return false;
        }

        if (this.serviceChainMap.containsKey(chainId)) {
            log.debug(
                "[{}] Unable to create service chain with ID {}, as it is already created",
                this.label(), chainId
            );
            return true;
        }

        // TODO: Fix this inconsistency issue
        log.info("----------------------- BEFORE PUT");
        sc.serviceChainGraph().print();

        this.serviceChainMap.put(chainId, sc);
        log.info("[{}] Service chain with ID {} is created", this.label(), chainId);

        log.info("----------------------- AFTER PUT");
        ServiceChainInterface scUp = serviceChain(chainId);
        scUp.serviceChainGraph().print();

        return true;
    }

    @Override
    public boolean updateServiceChain(ServiceChainId chainId, ServiceChainInterface sc) {
        if (chainId == null || sc == null) {
            return false;
        }

        if (!this.serviceChainMap.containsKey(chainId)) {
            log.debug(
                "[{}] Unable to update service chain with ID {}, as it does not exist",
                this.label(), chainId
            );
            return false;
        }

        this.serviceChainMap.put(chainId, sc);
        log.info("[{}] Service chain with ID {} is updated", this.label(), chainId);

        // Check what do we have
        this.printInternalState(chainId);

        return true;
    }

    @Override
    public boolean removeServiceChain(ServiceChainId chainId) {
        if (this.serviceChainMap == null) {
            return false;
        }

        this.serviceChainMap.remove(chainId);
        log.info("[{}] Service chain with ID {} is removed", this.label(), chainId);

        return true;
    }

    @Override
    public boolean removeAllServiceChains() {
        if (this.serviceChainMap.isEmpty()) {
            log.warn("[{}] Service chains' storage is already empty", this.label());
            return false;
        }

        this.serviceChainMap.clear();

        return true;
    }

    @Override
    public ServiceChainInterface serviceChain(ServiceChainId chainId) {
        if (chainId == null) {
            return null;
        }

        // Key does not exist in the map
        if (!this.serviceChainMap.containsKey(chainId)) {
            log.debug(
                "[{}] Unable to retrieve the service chain with ID {}, as it does not exist.",
                this.label(), chainId
            );
            return null;
        }

        return this.serviceChainMap.get(chainId).value();
    }

    @Override
    public Set<ServiceChainInterface> registeredServiceChains() {
        Set<ServiceChainInterface> registeredScs = new HashSet<>();

        for (Map.Entry<ServiceChainId, Versioned<ServiceChainInterface>> entry :
                this.serviceChainMap.entrySet()) {
            registeredScs.add(entry.getValue().value());
        }

        return registeredScs;
    }

    @Override
    public Set<ServiceChainInterface> serviceChainsInState(ServiceChainState state) {
        Set<ServiceChainInterface> scs = new HashSet<>();

        for (Map.Entry<ServiceChainId, Versioned<ServiceChainInterface>> entry :
                this.serviceChainMap.entrySet()) {
            ServiceChainInterface sc = entry.getValue().value();
            if (sc.state() == state) {
                scs.add(sc);
            }
        }

        return scs;
    }

    @Override
    public Set<NetworkFunctionInterface> serviceChainNFs(ServiceChainId chainId) {
        ServiceChainInterface sc = this.serviceChain(chainId);

        if (sc == null) {
            log.warn(
                "[{}] Unable to retrieve the network functions of the service chain with ID {}.",
                this.label(), chainId
            );
            return null;
        }

        Set<NetworkFunctionInterface> nfs = new HashSet<>();

        for (ServiceChainVertexInterface scV : sc.serviceChainGraph().getVertexes()) {
            NetworkFunctionInterface nf = scV.networkFunction();
            checkNotNull(
                nf,
                "[" + this.label() + "] " +
                "Network function of service chain " + sc.name() + " is NULL"
            );
            nfs.add(nf);
        }

        log.debug(
            "[{}] Service Chain with {} NFs is returned",
            this.label(), nfs.size()
        );

        return nfs;
    }

    @Override
    public void updateServiceChainNFs(
            ServiceChainId chainId, Set<NetworkFunctionInterface> nfs) {
        ServiceChainInterface sc = this.serviceChain(chainId);

        if (sc == null) {
            log.warn(
                "[{}] Unable to retrieve the network functions of the service chain with ID {}.",
                this.label(), chainId
            );
            return;
        }

        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        log.info("CRITICAL UPDATE");
        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        sc.serviceChainGraph().print();

        // Iterate through the service chain graph
        for (ServiceChainVertexInterface scV : sc.serviceChainGraph().getVertexes()) {
            // Retrieve the NF associated with this vertex
            NetworkFunctionInterface nf = scV.networkFunction();

            // Iterate through the new (input) NFs to find the correct one
            boolean updated = false;
            for (NetworkFunctionInterface nfNew : nfs) {
                // A match on the ID and name imply that we found the NF to update
                if ((nf.id().equals(nfNew.id())) && (nf.name().equals(nfNew.name()))) {
                    scV.setNetworkFunction(nfNew);
                    updated = true;

                    log.info("##############################################################");
                    scV.networkFunction().networkFunctionGraph().print();
                    log.info("##############################################################");

                    break;
                }
            }

            // No NF was found with the same name and ID. This is a inconsistency.
            if (!updated) {
                throw new ServiceChainException(
                    "[" + this.label() + "] " +
                    "Inconsistency in service chain store"
                );
            }
        }

        // Update the service chain
        this.updateServiceChain(chainId, sc);
        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    @Override
    public void replaceServiceChainGraph(
            ServiceChainId chainId, ServiceChainGraphInterface scGraph) {
        ServiceChainInterface sc = this.serviceChain(chainId);

        if ((sc == null) || (scGraph == null)) {
            log.warn(
                "[{}] Unable to retrieve the graph of the service chain with ID {}.",
                this.label(), chainId
            );
            return;
        }

        // Replace the current graph with the input one
        sc.setServiceChainGraph(scGraph);

        // Update the service chain
        this.updateServiceChain(chainId, sc);

        return;
    }

    @Override
    public void printServiceChainById(ServiceChainId chainId) {
        log.info("================================================================");
        log.info("=== Service Chain {}", chainId);

        if (chainId == null) {
            log.info("=== \t NULL");
            log.info("================================================================");
            return;
        }

        ServiceChainInterface sc = this.serviceChainMap.get(chainId).value();
        log.info("=== \t {} with type {} and ID {}",
            sc.name(), sc.type(), sc.id()
        );

        log.info("================================================================");
    }

    @Override
    public void printServiceChainsByState(ServiceChainState state) {
        log.info("================================================================");
        log.info("=== Service Chain in state {}", state);

        Set<ServiceChainInterface> scs = this.serviceChainsInState(state);

        for (ServiceChainInterface sc : scs) {
            log.info("=== \t {} with type {} and ID {}",
                sc.name(), sc.type(), sc.id()
            );
        }

        if (scs.isEmpty()) {
            log.info("=== \t NULL");
        }

        log.info("================================================================");
    }

    @Override
    public void printRegisteredServiceChains() {
        log.info("================================================================");
        log.info("=== Registered Service Chains");

        Set<ServiceChainInterface> regServiceChains = this.registeredServiceChains();

        for (ServiceChainInterface sc : regServiceChains) {
            log.info("=== \t {} with type {} and ID {}",
                sc.name(), sc.type(), sc.id()
            );
        }

        if (regServiceChains.isEmpty()) {
            log.info("=== \t NULL");
        }

        log.info("================================================================");
    }

    /**
     * Parse the service chain and print important
     * information about the internal elements.
     */
    private void printInternalState(ServiceChainId chainId) {
        ServiceChainInterface sc = this.serviceChain(chainId);

        if (sc == null) {
            return;
        }

        log.info("");
        log.info("=============================================================================");
        log.info("=== POST Service Chain Update state");
        log.info("=============================================================================");

        // Iterate through all the NFs of this service chain
        for (ServiceChainVertexInterface scV : sc.serviceChainGraph().getVertexes()) {
            NetworkFunctionInterface nf = scV.networkFunction();
            if (nf == null) {
                continue;
            }
            log.info("[{}] NF {} with ID {}", this.label(), nf.name(), nf.id());

            if (nf.isEndPoint()) {
                log.info("[{}] \tNF {} is an end   point network function", this.label(), nf.name());
            }
            if (nf.isEntryPoint()) {
                log.info("[{}] \tNF {} is an entry point network function", this.label(), nf.name());
            }
            if (nf.isExitPoint()) {
                log.info("[{}] \tNF {} is an exit  point network function", this.label(), nf.name());
            }

            for (NetworkFunctionDeviceInterface dev : nf.devices()) {
                // Fetch the peering NF (if any)
                NetworkFunctionInterface peer = nf.peersWithNF(dev);
                if (peer != null) {
                    log.info(
                        "[{}] \tDevice {} peers with NF {} and interface {}",
                        this.label(), dev.name(), peer.id(), nf.peersWithInterface(dev)
                    );
                } else {
                    log.info(
                        "[{}] \tDevice {} is an end point",
                        this.label(), dev.name()
                    );
                }
            }

            for (NetworkFunctionDeviceInterface dev : nf.entryDevices()) {
                log.info(
                    "[{}] \tDevice {} is an entry point",
                    this.label(), dev.name()
                );
            }

            for (NetworkFunctionDeviceInterface dev : nf.exitDevices()) {
                log.info(
                    "[{}] \tDevice {} is an exit point",
                    this.label(), dev.name()
                );
            }

            // Retrieve the end point blocks of this NF
            Set<ProcessingBlockInterface> endBlocks = nf.endPointProcessingBlocks();
            if (endBlocks != null) {
                for (ProcessingBlockInterface endBlock : endBlocks) {
                    log.info(
                        "[{}] \tBlock {} of type {} is an END point",
                        this.label(), endBlock.id(), endBlock.processingBlockType()
                    );
                }
            }

            // Retrieve the entry point blocks of this NF
            Set<ProcessingBlockInterface> entryBlocks = nf.entryPointProcessingBlocks();
            if (entryBlocks != null) {
                for (ProcessingBlockInterface entryBlock : entryBlocks) {
                    log.info(
                        "[{}] \tBlock {} of type {} is an ENTRY ppoint",
                        this.label(), entryBlock.id(), entryBlock.processingBlockType()
                    );
                }
            }

            // Retrieve the exit point blocks of this NF
            Set<ProcessingBlockInterface> exitBlocks = nf.exitPointProcessingBlocks();
            if (exitBlocks != null) {
                for (ProcessingBlockInterface exitBlock : exitBlocks) {
                    log.info(
                        "[{}] \tBlock {} of type {} is an EXIT ppoint",
                        this.label(), exitBlock.id(), exitBlock.processingBlockType()
                    );
                }
            }

            // Retrieve the packet processing graph of this NF
            NetworkFunctionGraphInterface nfGraph = nf.networkFunctionGraph();
            if (nfGraph == null) {
                log.info("[{}] \tNo packet processing graph available", this.label());
                continue;
            }

            // For each processing block of this graph
            for (NetworkFunctionVertexInterface nfVer : nfGraph.getVertexes()) {
                ProcessingBlockInterface pb = nfVer.processingBlock();
                if (pb == null) {
                    continue;
                }
                log.info(
                    "[{}] \tProcessing block {} with type {}",
                    this.label(), pb.id(), pb.processingBlockType()
                );

                // Check if this is a classifier
                if (pb.processingBlockType() == ProcessingBlockType.BLOCK_TYPE_CLASSIFIER) {
                    ClassifierBlock cb = (ClassifierBlock) pb;
                    RuleConfigurationInterface ruleConf = cb.ruleConf();
                    if (ruleConf == null) {
                        log.info("[{}] \t\tNo rule configuration", this.label());
                    } else {
                        log.info("[{}] \t\tWith rule configuration", this.label());
                        // ruleConf.printRules();
                    }
                }
            }
            log.info("");
        }

        // Print the entire graph
        sc.serviceChainGraph().print();

        log.info("=============================================================================");
        log.info("");
        log.info("");
    }

    /**
     * Returns a label with the distributed store's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private String label() {
        return COMPONET_LABEL;
    }

    /**
     * Triggers a state transition for the input service chain.
     */
    private void stateTransitionTrigger(ServiceChainInterface sc) {
        ServiceChainState state = sc.state();
        // TODO fix this
        // state.process(this, sc);
        log.info("[{}] Processing service chain: {} state: {}", label(), sc.name(), state);
    }

    /**
     * Generate events for any activity in the service chain store.
     */
    private class InternalMapListener implements MapEventListener<ServiceChainId, ServiceChainInterface> {

        @Override
        public void event(MapEvent<ServiceChainId, ServiceChainInterface> event) {

            ServiceChainInterface oldSc = null;
            ServiceChainInterface newSc = null;

            switch (event.type()) {
                case INSERT:
                case UPDATE: {
                    // The new service chain
                    newSc = event.newValue().value();

                    // Indicates an insert operation
                    if (event.oldValue() == null) {
                        log.info("[{}] Service Chain Store Insert", label());
                        log.info("[{}] \tAdded service chain with ID {}", label(), newSc.id());
                    // Indicates an update operation
                    } else {
                        log.info("[{}] Service Chain Store Update", label());
                        // Fetch the old instance of the service chain
                        oldSc = event.oldValue().value();

                        // A trully updated service chain
                        if (!newSc.equals(oldSc)) {
                            log.info("[{}] \tUpdated service chain with ID {}", label(), newSc.id());
                        }
                    }

                    // Cause a state transition
                    // eventExecutor.execute(() -> stateTransitionTrigger(newSc));

                    // The notification deals with the updated instance anyway
                    notifyDelegate(new ServiceChainEvent(newSc.state(), newSc));

                    break;
                }

                case REMOVE:
                    log.info("[{}] Service Chain Store Remove", label());

                    oldSc = event.oldValue().value();
                    log.info("[{}] \tRemoved service chain with ID {}", label(), oldSc.id());

                    // Cause a state transition
                    // eventExecutor.execute(() -> stateTransitionTrigger(newSc));

                     // Notification about the removed instance
                    notifyDelegate(new ServiceChainEvent(oldSc.state(), oldSc));

                    break;

                default:
                    break;
            }

        }

    }

}
