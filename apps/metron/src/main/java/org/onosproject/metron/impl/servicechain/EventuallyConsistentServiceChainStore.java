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
import org.onosproject.metron.api.config.RuleConfigurationInterface;
import org.onosproject.metron.api.config.PatternConfigurationInterface;
import org.onosproject.metron.api.common.GenericId;
import org.onosproject.metron.api.common.UUIdGenerator;
import org.onosproject.metron.api.classification.ClassificationSyntax;
import org.onosproject.metron.api.classification.TextualPacketFilter;
import org.onosproject.metron.api.classification.trafficclass.HeaderField;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;
import org.onosproject.metron.api.classification.trafficclass.condition.Condition;
import org.onosproject.metron.api.classification.trafficclass.condition.ConditionMap;
import org.onosproject.metron.api.classification.trafficclass.filter.Filter;
import org.onosproject.metron.api.classification.trafficclass.filter.PacketFilter;
import org.onosproject.metron.api.classification.trafficclass.filter.Segment;
import org.onosproject.metron.api.classification.trafficclass.filter.SegmentNode;
import org.onosproject.metron.api.classification.trafficclass.filter.DisjointSegmentList;
import org.onosproject.metron.api.classification.trafficclass.operation.Operation;
import org.onosproject.metron.api.classification.trafficclass.operation.OperationType;
import org.onosproject.metron.api.classification.trafficclass.operation.FieldOperation;
import org.onosproject.metron.api.classification.trafficclass.operation.OperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.NoOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatelessOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatefulOperationValue;
import org.onosproject.metron.api.classification.trafficclass.operation.StatefulSetOperationValue;
import org.onosproject.metron.api.classification.trafficclass.outputclass.OutputClass;
import org.onosproject.metron.api.config.TrafficPoint;
import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.exceptions.ServiceChainException;
import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.net.ClickFlowRuleAction;
import org.onosproject.metron.api.networkfunction.NetworkFunctionId;
import org.onosproject.metron.api.networkfunction.NetworkFunctionClass;
import org.onosproject.metron.api.networkfunction.NetworkFunctionType;
import org.onosproject.metron.api.networkfunction.NetworkFunctionState;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionDeviceInterface;
import org.onosproject.metron.api.processing.ProcessingBlockType;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;
import org.onosproject.metron.api.processing.TerminalStage;
import org.onosproject.metron.api.server.TrafficClassRuntimeInfo;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainDelegate;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainStoreService;
import org.onosproject.metron.api.structures.Pair;
import org.onosproject.metron.api.structures.TreeNode;
import org.onosproject.metron.api.structures.StackNode;

import org.onosproject.metron.impl.classification.VisualTree;
import org.onosproject.metron.impl.classification.ClassificationTree;
import org.onosproject.metron.impl.classification.trafficclass.TrafficClass;
import org.onosproject.metron.impl.config.RuleConfiguration;
import org.onosproject.metron.impl.config.ClickRuleConfiguration;
import org.onosproject.metron.impl.config.ClassifierRuleConfiguration;
import org.onosproject.metron.impl.config.LookupRuleConfiguration;
import org.onosproject.metron.impl.config.IpClassifierRuleConfiguration;
import org.onosproject.metron.impl.config.IpFilterRuleConfiguration;
import org.onosproject.metron.impl.dataplane.NfvDataplaneBlock;
import org.onosproject.metron.impl.dataplane.NfvDataplaneNode;
import org.onosproject.metron.impl.dataplane.NfvDataplaneTree;
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
import org.onosproject.metron.impl.processing.blocks.EtherEncap;
import org.onosproject.metron.impl.processing.blocks.FromDevice;
import org.onosproject.metron.impl.processing.blocks.FromDpdkDevice;
import org.onosproject.metron.impl.processing.blocks.IpAddrPairRewriter;
import org.onosproject.metron.impl.processing.blocks.IpAddrRewriter;
import org.onosproject.metron.impl.processing.blocks.IpClassifier;
import org.onosproject.metron.impl.processing.blocks.IpFilter;
import org.onosproject.metron.impl.processing.blocks.IpRewriter;
import org.onosproject.metron.impl.processing.blocks.LinuxDevice;
import org.onosproject.metron.impl.processing.blocks.LookupIpRouteMp;
import org.onosproject.metron.impl.processing.blocks.MarkIpHeader;
import org.onosproject.metron.impl.processing.blocks.OpenFlowClassifier;
import org.onosproject.metron.impl.processing.blocks.Queue;
import org.onosproject.metron.impl.processing.blocks.SimpleEthernetClassifier;
import org.onosproject.metron.impl.processing.blocks.StoreEtherAddress;
import org.onosproject.metron.impl.processing.blocks.Strip;
import org.onosproject.metron.impl.processing.blocks.TcpRewriter;
import org.onosproject.metron.impl.processing.blocks.ToDevice;
import org.onosproject.metron.impl.processing.blocks.ToDpdkDevice;
import org.onosproject.metron.impl.processing.blocks.UdpRewriter;
import org.onosproject.metron.impl.processing.blocks.Unstrip;

// ONOS libraries
import org.onlab.graph.Edge;
import org.onlab.graph.Vertex;
import org.onlab.graph.Graph;
import org.onlab.graph.MutableGraph;
import org.onlab.graph.MutableAdjacencyListsGraph;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.MacRxFilterValue;
import org.onosproject.drivers.server.devices.nic.MplsRxFilterValue;
import org.onosproject.drivers.server.devices.nic.VlanRxFilterValue;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ElementId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criteria.DummyCriterion;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onlab.util.KryoNamespace;

// Apache libraries
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

// Other libraries
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.cfg.ContextAttributes.Impl;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.HashMultimap;

// Java libraries
import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the registry of Metron service chains in the ONOS core.
 * Works as an eventually consistent distributed store.
 */
@Component(immediate = true)
@Service
public class EventuallyConsistentServiceChainStore
        extends AbstractStore<ServiceChainEvent, ServiceChainDelegate>
        implements ServiceChainStoreService {

    private static final Logger log = getLogger(
        EventuallyConsistentServiceChainStore.class
    );

    private static final String COMPONET_LABEL = "Distributed Store";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    /**
     * An eventually consistent distributed store with the
     * structure of the initial service chains.
     */
    private EventuallyConsistentMap<ServiceChainId, ServiceChainInterface>
        serviceChainMap;

    /**
     * An eventually consistent distributed store with the runnable service chains.
     * Each runnable service chain contains mappings between entry point interface names
     * and their packet processing trees.
     */
    private EventuallyConsistentMap<ServiceChainId, Map<String, NfvDataplaneTreeInterface>>
        runnableServiceChainMap;

    /**
     * A local cache of the runnable service chains, in case that some data is
     * too large to push to the distributed store.
     */
    private Map<ServiceChainId, Map<String, NfvDataplaneTreeInterface>>
        localRunnableServiceChainMap;

    /**
     * An eventually consistent distributed store with the
     * runtime information of service chains.
     * A service chain ID is mapped to a set of traffic class info objects.
     */
    private EventuallyConsistentMap<ServiceChainId, Set<TrafficClassRuntimeInfo>>
        runtimeServiceChainInfoMap;

    // A listener to catch important events taking place in the store
    private EventuallyConsistentMapListener<ServiceChainId, ServiceChainInterface>
        serviceChainStoreListener = new InternalMapListener();

    @Activate
    protected void activate() {
        /**
         * The serializer of the distributed data store with
         * the the initial service chains.
         * Poor guy, it has to deal with this storm of objects.
         */
        KryoNamespace.Builder scSerializer = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(
                Object.class, Set.class, Map.class,
                LinkedHashMap.class,
                SetMultimap.class,
                HashMultimap.class,
                TreeMap.class,
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
                ProcessingBlockType.class,
                ProcessingBlockClass.class,
                TerminalStage.class,
                RuleConfiguration.class,
                ClickRuleConfiguration.class,
                ClassifierRuleConfiguration.class,
                LookupRuleConfiguration.class,
                IpClassifierRuleConfiguration.class,
                IpFilterRuleConfiguration.class,
                ClickFlowRuleAction.class,
                Pair.class,
                TrafficClassType.class,
                TreeNode.class,
                StackNode.class,
                TextualPacketFilter.class,
                ClassificationSyntax.class,
                ClassificationTree.class,
                VisualTree.class,
                FlowRule.class,
                DummyCriterion.class,
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
                EtherEncap.class,
                FromDevice.class,
                FromDpdkDevice.class,
                IpAddrPairRewriter.class,
                IpAddrRewriter.class,
                IpClassifier.class,
                IpFilter.class,
                IpRewriter.class,
                LinuxDevice.class,
                LookupIpRouteMp.class,
                MarkIpHeader.class,
                OpenFlowClassifier.class,
                Queue.class,
                SimpleEthernetClassifier.class,
                StoreEtherAddress.class,
                Strip.class,
                TcpRewriter.class,
                ToDevice.class,
                ToDpdkDevice.class,
                UdpRewriter.class,
                Unstrip.class,
                NetworkFunctionId.class,
                NetworkFunctionClass.class,
                NetworkFunctionType.class,
                NetworkFunctionState.class,
                NetworkFunctionDevice.class,
                NetworkFunctionGraph.class,
                NetworkFunctionEdge.class,
                NetworkFunctionVertex.class,
                NetworkFunction.class,
                ServiceChainEvent.class,
                ServiceChainDelegate.class,
                ServiceChainId.class,
                ServiceChainState.class,
                ServiceChainVertex.class,
                ServiceChainEdge.class,
                ServiceChainGraph.class,
                TrafficPoint.class,
                ServiceChain.class
            );

        // This is kept more compact as it is expected to be more dynamic!
        KryoNamespace.Builder dpSerializer = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(
                HeaderField.class, SegmentNode.class,
                Segment.class, OperationType.class,
                OperationValue.class,
                NoOperationValue.class,
                StatelessOperationValue.class,
                StatefulOperationValue.class,
                StatefulSetOperationValue.class,
                FieldOperation.class,
                DisjointSegmentList.class,
                Filter.class, Condition.class,
                ConditionMap.class,
                Operation.class, OutputClass.class,
                PacketFilter.class, TrafficClass.class,
                NfvDataplaneBlock.class, NfvDataplaneNode.class,
                NfvDataplaneTree.class, FlowRule.class,
                ApplicationId.class,
                ClickFlowRuleAction.class, TrafficClassType.class,
                Pair.class, TreeNode.class, StackNode.class,
                TextualPacketFilter.class,
                ClassificationSyntax.class,
                ClassificationTree.class,
                VisualTree.class, DummyCriterion.class,
                ProcessingBlock.class,
                ClassifierBlock.class, ModifierBlock.class,
                MonitorBlock.class, ShaperBlock.class,
                StatefulModifierBlock.class, TerminalBlock.class,
                TransparentBlock.class,
                CheckIpHeader.class, Classifier.class,
                DecIpTtl.class, Device.class, Discard.class,
                DpdkDevice.class, EtherEncap.class,
                FromDevice.class, FromDpdkDevice.class,
                IpAddrPairRewriter.class, IpAddrRewriter.class,
                IpClassifier.class, IpFilter.class,
                IpRewriter.class, LinuxDevice.class,
                LookupIpRouteMp.class, MarkIpHeader.class,
                OpenFlowClassifier.class, Queue.class,
                SimpleEthernetClassifier.class,
                StoreEtherAddress.class, Strip.class, TcpRewriter.class,
                ToDevice.class, ToDpdkDevice.class,
                UdpRewriter.class, Unstrip.class
            );

        // Runtime information serializer
        KryoNamespace.Builder rtSerializer = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(
                ServiceChainId.class,
                CpuStatistics.class,
                MonitoringStatistics.class,
                RxFilter.class,
                RxFilterValue.class,
                MacRxFilterValue.class,
                MplsRxFilterValue.class,
                VlanRxFilterValue.class
            );

        // Build the eventually consistent distributed stores
        this.serviceChainMap = storageService.<ServiceChainId, ServiceChainInterface>
            eventuallyConsistentMapBuilder()
                .withName("onos-service-chain-store")
                .withSerializer(scSerializer.build())
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        this.runnableServiceChainMap = storageService.<ServiceChainId, Map<String, NfvDataplaneTreeInterface>>
            eventuallyConsistentMapBuilder()
                .withName("onos-service-chain-dataplane-store")
                .withSerializer(dpSerializer.build())
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        this.localRunnableServiceChainMap =
            new ConcurrentHashMap<ServiceChainId, Map<String, NfvDataplaneTreeInterface>>();

        this.runtimeServiceChainInfoMap = storageService.<ServiceChainId, Set<TrafficClassRuntimeInfo>>
            eventuallyConsistentMapBuilder()
                .withName("onos-service-chain-runtime-store")
                .withSerializer(rtSerializer.build())
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        // Add a listener
        this.serviceChainMap.addListener(serviceChainStoreListener);

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        // Remove the listener before exiting
        this.serviceChainMap.removeListener(serviceChainStoreListener);

        log.info("[{}] Stopped", label());
    }

    /******************************* Services for service chains. *****************************/

    @Override
    public boolean createServiceChain(ServiceChainId scId, ServiceChainInterface sc) {
        if (scId == null || sc == null) {
            return false;
        }

        if (this.serviceChainMap.containsKey(scId)) {
            log.debug("[{}] Unable to create service chain with ID {}, as it is already created",
                label(), scId);
            return true;
        }

        this.serviceChainMap.put(scId, sc);

        log.info("[{}] Service chain with ID {} is created", label(), scId);

        return true;
    }

    @Override
    public boolean updateServiceChain(ServiceChainId scId, ServiceChainInterface sc) {
        if (scId == null || sc == null) {
            return false;
        }

        if (!this.serviceChainMap.containsKey(scId)) {
            log.debug("[{}] Unable to update service chain with ID {}, as it does not exist",
                label(), scId);
            return false;
        }

        this.serviceChainMap.put(scId, sc);

        log.info("[{}] Service chain with ID {} is updated", label(), scId);

        // Check what do we have
        // this.printInternalState(sc);

        return true;
    }

    @Override
    public boolean removeServiceChain(ServiceChainId scId) {
        if ((this.serviceChainMap == null)) {
            return false;
        }

        this.serviceChainMap.remove(scId);

        log.info("[{}] Service chain with ID {} is removed", label(), scId);

        return true;
    }

    @Override
    public boolean removeAllServiceChains() {
        if ((this.serviceChainMap.isEmpty())) {
            log.warn("[{}] Service chains' storage is already empty", label());
            return false;
        }

        this.serviceChainMap.clear();

        return true;
    }

    @Override
    public ServiceChainInterface serviceChain(ServiceChainId scId) {
        if (scId == null) {
            return null;
        }

        // Key does not exist in the map
        if (!this.serviceChainMap.containsKey(scId)) {
            log.debug("[{}] Unable to retrieve the service chain with ID {}, as it does not exist.",
                label(), scId);
            return null;
        }

        return this.serviceChainMap.get(scId);
    }

    @Override
    public Set<ServiceChainInterface> registeredServiceChains() {
        Set<ServiceChainInterface> registeredScs = Sets.<ServiceChainInterface>newConcurrentHashSet();

        for (Map.Entry<ServiceChainId, ServiceChainInterface> entry :
                this.serviceChainMap.entrySet()) {
            registeredScs.add(entry.getValue());
        }

        return registeredScs;
    }

    @Override
    public Set<ServiceChainInterface> serviceChainsInState(ServiceChainState state) {
        Set<ServiceChainInterface> scs = Sets.<ServiceChainInterface>newConcurrentHashSet();

        for (Map.Entry<ServiceChainId, ServiceChainInterface> entry :
                this.serviceChainMap.entrySet()) {
            ServiceChainInterface sc = entry.getValue();
            if (sc.state() == state) {
                scs.add(sc);
            }
        }

        return scs;
    }

    @Override
    public Set<NetworkFunctionInterface> serviceChainNFs(ServiceChainId scId) {
        ServiceChainInterface sc = this.serviceChain(scId);

        if (sc == null) {
            log.warn("[{}] Unable to retrieve the network functions of the service chain with ID {}.",
                label(), scId);
            return null;
        }

        Set<NetworkFunctionInterface> nfs = new ConcurrentSkipListSet<NetworkFunctionInterface>();

        for (ServiceChainVertexInterface scV : sc.serviceChainGraph().getVertexes()) {
            NetworkFunctionInterface nf = scV.networkFunction();
            checkNotNull(nf, "[" + label() + "] " +
                "Network function of service chain " + sc.name() + " is NULL");
            nfs.add(nf);
        }

        log.debug("[{}] Service Chain with {} NFs is returned", label(), nfs.size());

        return nfs;
    }

    @Override
    public void updateServiceChainNFs(
            ServiceChainId scId, Set<NetworkFunctionInterface> nfs) {
        ServiceChainInterface sc = this.serviceChain(scId);

        if (sc == null) {
            log.warn("[{}] Unable to retrieve the network functions of the service chain with ID {}",
                label(), scId);
            return;
        }

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
                    break;
                }
            }

            // No NF was found with the same name and ID. This is a inconsistency.
            if (!updated) {
                throw new ServiceChainException("[" + label() + "] " +
                    "Inconsistency in service chain store");
            }
        }

        // Update the service chain
        this.updateServiceChain(scId, sc);
    }

    @Override
    public void replaceServiceChainGraph(
            ServiceChainId scId, ServiceChainGraphInterface scGraph) {
        ServiceChainInterface sc = this.serviceChain(scId);

        if ((sc == null) || (scGraph == null)) {
            log.warn("[{}] Unable to retrieve the graph of the service chain with ID {}",
                label(), scId);
            return;
        }

        // Replace the current graph with the input one
        sc.setServiceChainGraph(scGraph);

        // Update the service chain
        this.updateServiceChain(scId, sc);

        return;
    }

    @Override
    public void printServiceChainById(ServiceChainId scId) {
        log.info("================================================================");
        log.info("=== Service Chain {}", scId);

        if (scId == null) {
            log.info("=== \t NULL");
            log.info("================================================================");
            return;
        }

        ServiceChainInterface sc = this.serviceChain(scId);
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
    private static void printInternalState(ServiceChainInterface sc) {
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
            log.info("[{}] NF {} with ID {}", label(), nf.name(), nf.id());

            if (nf.isEndPoint()) {
                log.info("[{}] \tNF {} is an end   point network function", label(), nf.name());
            }
            if (nf.isEntryPoint()) {
                log.info("[{}] \tNF {} is an entry point network function", label(), nf.name());
            }
            if (nf.isExitPoint()) {
                log.info("[{}] \tNF {} is an exit  point network function", label(), nf.name());
            }

            for (NetworkFunctionDeviceInterface dev : nf.devices()) {
                // Fetch the peering NF (if any)
                NetworkFunctionInterface peer = nf.peersWithNF(dev);
                if (peer != null) {
                    log.info("[{}] \tDevice {} peers with NF {} and interface {}",
                        label(), dev.name(), peer.id(), nf.peersWithInterface(dev));
                } else {
                    log.info("[{}] \tDevice {} is an end point", label(), dev.name());
                }
            }

            for (NetworkFunctionDeviceInterface dev : nf.entryDevices()) {
                log.info("[{}] \tDevice {} is an entry point", label(), dev.name());
            }

            for (NetworkFunctionDeviceInterface dev : nf.exitDevices()) {
                log.info("[{}] \tDevice {} is an exit point", label(), dev.name());
            }

            // Retrieve the end point blocks of this NF
            Set<ProcessingBlockInterface> endBlocks = nf.endPointProcessingBlocks();
            if (endBlocks != null) {
                for (ProcessingBlockInterface endBlock : endBlocks) {
                    log.info("[{}] \tBlock {} of type {} is an END point",
                        label(), endBlock.id(), endBlock.processingBlockType());
                }
            }

            // Retrieve the entry point blocks of this NF
            Set<ProcessingBlockInterface> entryBlocks = nf.entryPointProcessingBlocks();
            if (entryBlocks != null) {
                for (ProcessingBlockInterface entryBlock : entryBlocks) {
                    log.info("[{}] \tBlock {} of type {} is an ENTRY ppoint",
                        label(), entryBlock.id(), entryBlock.processingBlockType());
                }
            }

            // Retrieve the exit point blocks of this NF
            Set<ProcessingBlockInterface> exitBlocks = nf.exitPointProcessingBlocks();
            if (exitBlocks != null) {
                for (ProcessingBlockInterface exitBlock : exitBlocks) {
                    log.info("[{}] \tBlock {} of type {} is an EXIT point",
                        label(), exitBlock.id(), exitBlock.processingBlockType());
                }
            }

            // for (Map.Entry<String, RuleConfigurationInterface> entry : nf.elementsConf().entrySet()) {
            //     String el = entry.getKey();
            //     log.info("Element {}", el);
            //     entry.getValue().printRules();
            // }

            // Retrieve the packet processing graph of this NF
            NetworkFunctionGraphInterface nfGraph = nf.networkFunctionGraph();
            if (nfGraph == null) {
                log.warn("[{}] \tNo packet processing graph available", label());
                continue;
            }

            // For each processing block of this graph
            for (NetworkFunctionVertexInterface nfVer : nfGraph.getVertexes()) {
                ProcessingBlockInterface pb = nfVer.processingBlock();
                if (pb == null) {
                    continue;
                }
                log.info("[{}] \tProcessing block {} with type {}", label(), pb.id(), pb.processingBlockType());

                // Classifier has rule configurations
                if (pb.processingBlockType() == ProcessingBlockType.BLOCK_TYPE_CLASSIFIER) {
                    ClassifierBlock cb = (ClassifierBlock) pb;
                    RuleConfigurationInterface ruleConf = cb.ruleConf();
                    if (ruleConf == null) {
                        log.info("[{}] \t\tNo rule configuration", label());
                    } else {
                        log.info("[{}] \t\tWith rule configuration", label());
                        // ruleConf.printRules();
                    }
                } else if (pb.processingBlockClass() == ProcessingBlockClass.IP_REWRITER) {
                    IpRewriter rb = (IpRewriter) pb;
                    PatternConfigurationInterface patConf = rb.patternConf();
                    if (patConf == null) {
                        log.info("[{}] \t\tNo pattern configuration", label());
                    } else {
                        log.info("[{}] \t\tWith pattern configuration", label());
                        patConf.printPatterns();
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

    /************************** Services for RUNNABLE service chains. *************************/

    @Override
    public boolean addRunnableServiceChain(
            ServiceChainId scId, String iface, NfvDataplaneTreeInterface tree) {
        if (scId == null || iface.isEmpty() || tree == null) {
            return false;
        }

        // We cannot create a runnable service chain if the primary map does not contain it.
        if (!this.serviceChainMap.containsKey(scId)) {
            log.error(
                "[{}] Unable to create a  dataplane service chain with ID {}; " +
                "This service chain is not registered", label(), scId);
            return false;
        }

        // Check the memory requirements of this service chain
        byte[] bytes = null;
        boolean fits = false;
        try {
            bytes = serialize(tree);
            fits = true;
        } catch (IOException ioEx) {
            log.warn("[{}] Service chain {} will be kept only locally.", label(), scId);
        }

        Map<String, NfvDataplaneTreeInterface> trees = null;
        boolean firstTime = false;

        // Decide whether to store locally or globally, based on the size
        if (this.runnableServiceChainMap.containsKey(scId) && fits) {
            trees = this.runnableServiceChainMap.get(scId);
        } else if (this.localRunnableServiceChainMap.containsKey(scId) && !fits) {
            trees = this.localRunnableServiceChainMap.get(scId);
        } else {
            trees = new ConcurrentHashMap<String, NfvDataplaneTreeInterface>();
            firstTime = true;
        }

        if (trees.containsKey(iface)) {
            log.debug(
                "[{}] Unable to create dataplane service chain with ID {} for interface {}, as it is already created",
                label(), scId, iface);
            return true;
        }

        // Store
        trees.put(iface, tree);

        if (firstTime && fits) {
            this.runnableServiceChainMap.put(scId, trees);
        }

        this.localRunnableServiceChainMap.put(scId, trees);

        log.debug("[{}] Dataplane service chain for interface {} with ID {} is created", label(), iface, scId);

        return true;
    }

    @Override
    public boolean updateRunnableServiceChain(
            ServiceChainId scId, String iface, NfvDataplaneTreeInterface tree) {
        if (scId == null || iface.isEmpty() || tree == null) {
            return false;
        }

        if (!this.runnableServiceChainMap.containsKey(scId) &&
            !this.localRunnableServiceChainMap.containsKey(scId)) {
            log.debug("[{}] There is no runnable service chain with ID {} to update", label(), scId);
            return false;
        }

        Map<String, NfvDataplaneTreeInterface> trees = null;
        if (this.runnableServiceChainMap.containsKey(scId)) {
            trees = this.runnableServiceChainMap.get(scId);
        } else {
            trees = this.localRunnableServiceChainMap.get(scId);
        }
        trees.put(iface, tree);

        log.info("[{}] Runnable service chain with ID {} is updated", label(), scId);

        return true;
    }

    @Override
    public boolean deleteRunnableServiceChain(
            ServiceChainId scId) {
        if (scId == null) {
            return false;
        }

        if (!this.runnableServiceChainMap.containsKey(scId) &&
            !this.localRunnableServiceChainMap.containsKey(scId)) {
            log.debug("[{}] There is no runnable service chain with ID {} to delete", label(), scId);
            return true;
        }

        if (this.runnableServiceChainMap.containsKey(scId)) {
            this.runnableServiceChainMap.remove(scId);
        }

        if (this.localRunnableServiceChainMap.containsKey(scId)) {
            this.localRunnableServiceChainMap.remove(scId);
        }

        log.info("[{}] Runnable service chain with ID {} is deleted", label(), scId);

        return true;
    }

    @Override
    public NfvDataplaneTreeInterface runnableServiceChainOfIface(ServiceChainId scId, String iface) {
        if (scId == null || iface.isEmpty()) {
            return null;
        }

        // Key does not exist in the map
        if (!this.runnableServiceChainMap.containsKey(scId) &&
            !this.localRunnableServiceChainMap.containsKey(scId)) {
            log.debug("[{}] There is no runnable service chain with ID {}", label(), scId);
            return null;
        }

        Map<String, NfvDataplaneTreeInterface> trees = null;
        if (this.runnableServiceChainMap.containsKey(scId)) {
            trees = this.runnableServiceChainMap.get(scId);
        } else {
            trees = this.localRunnableServiceChainMap.get(scId);
        }

        return trees.get(iface);
    }

    @Override
    public NfvDataplaneTreeInterface runnableServiceChainWithTrafficClass(ServiceChainId scId, URI tcId) {
        if (scId == null || tcId == null) {
            return null;
        }

        // Key does not exist in the map
        if (!this.runnableServiceChainMap.containsKey(scId) &&
            !this.localRunnableServiceChainMap.containsKey(scId)) {
            log.debug("[{}] There is no runnable service chain with ID {}", label(), scId);
            return null;
        }

        Map<String, NfvDataplaneTreeInterface> trees = null;
        if (this.runnableServiceChainMap.containsKey(scId)) {
            trees = this.runnableServiceChainMap.get(scId);
        } else {
            trees = this.localRunnableServiceChainMap.get(scId);
        }

        for (Map.Entry<String, NfvDataplaneTreeInterface> entry : trees.entrySet()) {
            NfvDataplaneTreeInterface tree = entry.getValue();

            if (tree.hasTrafficClass(tcId)) {
                return tree;
            }
        }

        return null;
    }

    @Override
    public Map<String, NfvDataplaneTreeInterface> runnableServiceChains(ServiceChainId scId) {
        if (scId == null) {
            return null;
        }

        // Key does not exist in the map
        if (!this.runnableServiceChainMap.containsKey(scId) &&
            !this.localRunnableServiceChainMap.containsKey(scId)) {
            log.debug("[{}] There is no runnable service chain with ID {}", label(), scId);
            return null;
        }

        if (this.runnableServiceChainMap.containsKey(scId)) {
            return this.runnableServiceChainMap.get(scId);
        }

        return this.localRunnableServiceChainMap.get(scId);
    }

    @Override
    public void printRunnableServiceChainById(ServiceChainId scId) {
        log.info("================================================================");
        log.info("=== Runnable Service Chain {}", scId);

        if (scId == null) {
            log.info("=== \t NULL");
            log.info("================================================================");
            return;
        }

        Map<String, NfvDataplaneTreeInterface> trees = null;
        if (this.runnableServiceChainMap.containsKey(scId)) {
            trees = this.runnableServiceChainMap.get(scId);
        } else {
            trees = this.localRunnableServiceChainMap.get(scId);
        }

        if (trees != null) {
            for (Map.Entry<String, NfvDataplaneTreeInterface> entry : trees.entrySet()) {
                String iface = entry.getKey();
                for (TrafficClassInterface tc : entry.getValue().trafficClasses().values()) {
                    log.info(tc.toString());
                }
            }
        }

        log.info("================================================================");
    }

    /***************************** Services for Runtime Information. **************************/

    @Override
    public boolean addRuntimeInformationToServiceChain(
            ServiceChainId scId, Set<TrafficClassRuntimeInfo> scInfo) {
        if (scId == null || scInfo == null) {
            return false;
        }

        /**
         * We cannot create a runtime information etry,
         * if the primary map does not contain the service chain.
         */
        if (!this.serviceChainMap.containsKey(scId)) {
            log.error("[{}] Unable to create runtime information for service chain with ID {}; " +
                "This service chain is not registered", label(), scId);
            return false;
        }

        if (this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.warn(
                "[{}] Unable to create runtime information for service chain with ID {}, as it is already created",
                label(), scId);
            return true;
        }

        this.runtimeServiceChainInfoMap.put(scId, scInfo);

        log.info("[{}] Runtime information for service chain with ID {} is added", label(), scId);

        return true;
    }

    @Override
    public boolean addRuntimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, TrafficClassRuntimeInfo tcInfo) {
        if (scId == null || tcInfo == null) {
            return false;
        }

        /**
         * We cannot create a runtime information etry,
         * if the primary map does not contain the service chain.
         */
        if (!this.serviceChainMap.containsKey(scId)) {
            log.error(
                "[{}] Unable to create runtime information for service chain with ID {}; " +
                "This service chain is not registered", label(), scId);
            return false;
        }

        if (this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.warn(
                "[{}] Unable to create runtime information for service chain with ID {}, as it is already created",
                label(), scId);
            return true;
        }

        if (this.runtimeServiceChainInfoMap.get(scId) == null) {
            this.runtimeServiceChainInfoMap.put(scId, Sets.<TrafficClassRuntimeInfo>newConcurrentHashSet());
        }

        this.runtimeServiceChainInfoMap.get(scId).add(tcInfo);

        log.info("[{}] Runtime information for traffic class {} of service chain {} is added",
            label(), scId);

        return true;
    }

    @Override
    public boolean updateRuntimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, TrafficClassRuntimeInfo tcInfo) {
        if (scId == null || tcInfo == null) {
            return false;
        }

        if (!this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.debug("[{}] There is no runtime information for service chain with ID {} to update",
                label(), scId);
            return false;
        }

        Set<TrafficClassRuntimeInfo> scInfo = this.runtimeServiceChainInfoMap.get(scId);

        boolean deleted = false;
        Iterator<TrafficClassRuntimeInfo> iterator = scInfo.iterator();
        while (iterator.hasNext()) {
            TrafficClassRuntimeInfo exTcInfo = iterator.next();

            // Found
            if (exTcInfo.trafficClassId().equals(tcInfo.trafficClassId())) {
                // Update by removing the old and adding the new one
                iterator.remove();
                deleted = true;
            }
        }

        if (deleted) {
            scInfo.add(tcInfo);
        }

        log.info(
            "[{}] Runtime information for traffic class {} of service chain {} is updated",
            label(), tcInfo.trafficClassId(), scId
        );

        return true;
    }

    @Override
    public boolean deleteRuntimeInformationForServiceChain(ServiceChainId scId) {
        if (scId == null) {
            return false;
        }

        if (!this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.debug("[{}] There is no runtime information for service chain with ID {} to delete",
                label(), scId);
            return true;
        }

        this.runtimeServiceChainInfoMap.remove(scId);

        log.info("[{}] Runtime information for service chain {} is deleted", label(), scId);

        return true;
    }

    @Override
    public boolean deleteRuntimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, TrafficClassRuntimeInfo tcInfo) {
        if (scId == null || tcInfo == null) {
            return false;
        }

        if (!this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.debug("[{}] There is no runtime information for service chain with ID {} to delete",
                label(), scId);
            return true;
        }

        Set<TrafficClassRuntimeInfo> scInfo = this.runtimeServiceChainInfoMap.get(scId);

        boolean deleted = false;
        Iterator<TrafficClassRuntimeInfo> iterator = scInfo.iterator();
        while (iterator.hasNext()) {
            TrafficClassRuntimeInfo exTcInfo = iterator.next();

            // Found
            if (exTcInfo.trafficClassId().equals(tcInfo.trafficClassId())) {
                // Update by removing the old and adding the new one
                iterator.remove();
                deleted = true;
            }
        }

        log.info("[{}] Runtime information for traffic class {} of service chain {} is deleted",
            label(), tcInfo.trafficClassId(), scId);

        return true;
    }

    @Override
    public Set<TrafficClassRuntimeInfo> runtimeInformationForServiceChain(ServiceChainId scId) {
        if (scId == null) {
            return null;
        }

        // Key does not exist in the map
        if (!this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.error(
                "[{}] There is no runtime information for service chain with ID {}",
                label(), scId
            );
            return null;
        }

        return this.runtimeServiceChainInfoMap.get(scId);
    }

    @Override
    public TrafficClassRuntimeInfo runtimeInformationForTrafficClassOfServiceChain(
            ServiceChainId scId, URI trafficClassId) {
        if ((scId == null) || (trafficClassId == null)) {
            return null;
        }

        // Key does not exist in the map
        if (!this.runtimeServiceChainInfoMap.containsKey(scId)) {
            log.error(
                "[{}] There is no runtime information for service chain with ID {}",
                label(), scId
            );
            return null;
        }

        Set<TrafficClassRuntimeInfo> scInfo = this.runtimeServiceChainInfoMap.get(scId);

        for (TrafficClassRuntimeInfo tcInfo : scInfo) {
            if (tcInfo.trafficClassId().equals(trafficClassId)) {
                return tcInfo;
            }
        }

        log.error(
            "[{}] There is no runtime information for traffic class {} of service chain with ID {}.",
            label(), trafficClassId, scId
        );

        return null;
    }

    @Override
    public void printRuntimeInformationForServiceChain(ServiceChainId scId) {
        log.info("================================================================");
        log.info("=== Runtime information for Service Chain {}", scId);

        if (scId == null) {
            log.info("=== \t NULL");
            log.info("================================================================");
            return;
        }

        Set<TrafficClassRuntimeInfo> scInfo = this.runtimeServiceChainInfoMap.get(scId);
        for (TrafficClassRuntimeInfo tcInfo : scInfo) {
            log.info(tcInfo.toString());
        }

        log.info("================================================================");
    }

    /************************************ Internal methods. ***********************************/

    /**
     * Serializes an object.
     *
     * @param obj object to serialize
     * @throws IOException I/O exception
     * @return the byte stream of the serialized object
     */
    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        return baos.toByteArray();
    }

    /**
     * Returns a label with the distributed store's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
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
    private class InternalMapListener
            implements EventuallyConsistentMapListener<ServiceChainId, ServiceChainInterface> {

        @Override
        public void event(EventuallyConsistentMapEvent<ServiceChainId, ServiceChainInterface> event) {

            ServiceChainInterface sc = null;

            switch (event.type()) {
                case PUT:
                    log.info("[{}] Service Chain Store Insert/Update", label());

                    // The new/updated service chain
                    sc = event.value();
                    log.info("[{}] \tInserted/Updated service chain with ID {}", label(), sc.id());

                    // Notification about the inserted/updated instance
                    notifyDelegate(new ServiceChainEvent(sc.state(), sc));

                    break;

                case REMOVE:
                    log.info("[{}] Service Chain Store Remove", label());

                    // The removed service chain
                    sc = event.value();
                    log.info("[{}] \tRemoved service chain with ID {}", label(), sc.id());

                     // Notification about the removed instance
                    notifyDelegate(new ServiceChainEvent(sc.state(), sc));

                    break;

                default:
                    break;
            }

        }

    }

}
