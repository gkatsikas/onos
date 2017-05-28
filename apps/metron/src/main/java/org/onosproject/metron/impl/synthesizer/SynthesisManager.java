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

package org.onosproject.metron.impl.synthesizer;

// Metron libraries
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.exceptions.SynthesisException;
import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.graphs.ServiceChainVertexInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionEdgeInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.monitor.MonitorService;
import org.onosproject.metron.api.monitor.WallClockNanoTimestamp;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionDeviceInterface;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;
import org.onosproject.metron.api.processing.TerminalStage;
import org.onosproject.metron.api.servicechain.ServiceChainEvent;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;
import org.onosproject.metron.api.servicechain.ServiceChainService;
import org.onosproject.metron.api.servicechain.ServiceChainListenerInterface;
import org.onosproject.metron.api.synthesizer.SynthesisService;

import org.onosproject.metron.impl.dataplane.NfvDataplaneBlock;
import org.onosproject.metron.impl.dataplane.NfvDataplaneTree;
import org.onosproject.metron.impl.processing.Blocks;
import org.onosproject.metron.impl.processing.blocks.Device;
import org.onosproject.metron.impl.processing.blocks.FromBlackboxDevice;
import org.onosproject.metron.impl.processing.blocks.ToBlackboxDevice;

// ONOS libraries
import org.onlab.util.Tools;
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;

// Apache libraries
import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Other libraries
import org.slf4j.Logger;
import com.google.common.collect.Sets;

// Java libraries
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A service that converts a constructed service chain (by an application)
 * into a highly optimized counterpart, by synthesizing its internal
 * read and write operations (state CONSTRUCTED --> SYNTHESIZED).
 * After the synthesis, the service chain goes to state READY.
 * If this component is disabled, it directly moves a CONSTRUCTED state to
 * the READY state.
 */
@Component(immediate = true)
@Service
public final class SynthesisManager
        implements SynthesisService {

    private static final Logger log = getLogger(SynthesisManager.class);

    /**
     * Application ID for the Metron service chain Synthesizer.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".servicechain.synthesizer";
    private static final String COMPONET_LABEL = "Metron Synthesizer";

    /**
     * Members:
     * |-> An application ID is necessary to register with the core.
     * |-> A set of service chains (fetched by the ServiceChainManager) ready to synthesize.
     */
    private ApplicationId appId = null;
    private Set<ServiceChainInterface> serviceChainsToSynthesize = null;

    /**
     * Listen to events dispatched by the Service Chain Manager.
     * These events are related to the state of the service chains
     * that reside in the Service Chain Store.
     */
    private final ServiceChainListenerInterface serviceChainListener =
        new InternalServiceChainListener();

    /**
     * A dedicated thread pool to synthesize multiple Metron service chains concurrently.
     */
    private static final int SYNTHESIZER_THREADS_NO = 3;
    private final ExecutorService synthesizerExecutor = newFixedThreadPool(
        SYNTHESIZER_THREADS_NO,
        groupedThreads(this.getClass().getSimpleName(), "sc-synthesizer", log)
    );

    /**
     * Component properties to be adjusted by the operator.
     * The operator can select whether the synthesizer will be
     * involved in the formation of the traffic classes or not.
     * By default the synthesizer is enabled, so every CONSTRUCTED
     * service chain will be translated into a highly optimized
     * equivalent, before it becomes READY.
     */
    @Property(
        name = "enableSynthesizer", boolValue = true,
        label = "Enable the synthesizer component; default is true"
    )
    private boolean enableSynthesizer = true;

    /**
     * The Metron Synthesizer requires the ONOS core service to register
     * and the Service Chain Manager to fetch the list of active
     * Metron service chains. Such service chains must be in the
     * CONSTRUCTED state.
     * The Metron Synthesizer undertakes to change the state of the CONSTRUCTED
     * service chains to SYNTHESIZED.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ServiceChainService serviceChainService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MonitorService monitoringService;

    public SynthesisManager() {
        this.serviceChainsToSynthesize = Sets.<ServiceChainInterface>newConcurrentHashSet();
    }

    @Activate
    protected void activate(ComponentContext context) {
        // Register the Metron Synthesizer with the core.
        this.appId = coreService.registerApplication(APP_NAME);

        // Register the component configuration
        cfgService.registerProperties(getClass());

        // Add a listener to catch events coming from the service chain manager.
        serviceChainService.addListener(serviceChainListener);

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        // Unregister the component configuration
        cfgService.unregisterProperties(getClass(), false);

        // Remove the listener for the events coming from the service chain manager.
        serviceChainService.removeListener(serviceChainListener);

        this.serviceChainsToSynthesize.clear();

        this.synthesizerExecutor.shutdown();

        log.info("[{}] Stopped", label());
    }

    @Modified
    public void modified(ComponentContext context) {
        this.readComponentConfiguration(context);
    }

    /**
     * Return the application ID of the service.
     *
     * @return application ID
     */
    public ApplicationId applicationId() {
        return this.appId;
    }

    @Override
    public Set<ServiceChainInterface> serviceChainsToSynthesize() {
        return this.serviceChainsToSynthesize;
    }

    private int numberOfServiceChainsToSynthesize() {
        return this.serviceChainsToSynthesize.size();
    }

    private boolean hasServiceChainsToSynthesize() {
        return this.serviceChainsToSynthesize.size() > 0;
    }

    @Override
    public void addServiceChainToSynthesizer(ServiceChainInterface sc) {
        // Proceed only if the component is active
        if (!this.isAllowedToProceed()) {
            return;
        }

        checkNotNull(sc, "[" + label() + "] NULL service chain");

        this.serviceChainsToSynthesize.add(sc);
    }

    @Override
    public void markServiceChainAsSynthesized(ServiceChainInterface sc) {
        // Proceed only if the component is active
        if (!isAllowedToProceed()) {
            return;
        }

        checkNotNull(
            sc,
            "[" + label() + "] NULL service chain."
        );

        // Update the state to SYNTHESIZED
        serviceChainService.processSynthesizedState(sc);
    }

    @Override
    public void markServiceChainAsReady(ServiceChainInterface sc) {
        checkNotNull(
            sc,
            "[" + label() + "] NULL service chain."
        );

        // Update the state to READY
        serviceChainService.processReadyState(sc);
    }

    @Override
    public boolean synthesize() {
        // Proceed only if the component is active
        if (!this.isAllowedToProceed()) {
            return true;
        }

        boolean status = true;

        log.info(Constants.STDOUT_BARS);
        log.info("=== Service Chain Synthesis");
        log.info(Constants.STDOUT_BARS);

        Iterator<ServiceChainInterface> iter;
        for (
                iter = this.serviceChainsToSynthesize().iterator();
                iter.hasNext();
            ) {
            ServiceChainInterface sc = iter.next();

            // Synthesize one by one
            if (!this.synthesizeServiceChain(sc)) {
                status = false;
            } else {
                /**
                 * State transition for the service chain
                 * From SYNTHESIZED --> READY
                 */
                this.markServiceChainAsReady(sc);
            }

            iter.remove();
        }

        checkArgument(
            !this.hasServiceChainsToSynthesize(),
            "[" + label() + "] Not all the service chains are synthesized"
        );

        log.info(Constants.STDOUT_BARS);

        return status;
    }

    /**
     * Performs synthesis of one service chain.
     *
     * @param sc the target service chain
     * @return boolean synthesis status
     */
    private boolean synthesizeServiceChain(ServiceChainInterface sc) {
        // Proceed only if the component is active
        if (!this.isAllowedToProceed()) {
            return true;
        }

        if (sc == null) {
            return false;
        }

        // Not a valid state
        if (sc.state() != ServiceChainState.CONSTRUCTED) {
            return true;
        }

        // The graph of service chains components
        ServiceChainGraphInterface scGraph = sc.serviceChainGraph();
        if (scGraph == null) {
            log.error(
                "[{}] Problematic graph of network functions of the service chain with ID {}",
                label(),
                sc.id()
            );
            return false;
        }

        // The vertices of this graph
        Set<ServiceChainVertexInterface> scVertices = scGraph.getVertexes();
        if (scVertices == null) {
            return false;
        }

        // Keep the number of NFs in this service chain graph
        int serviceChainLength = scVertices.size();

        // Auxiliary stack for DFS to keep the already traversed NFs
        Set<NetworkFunctionInterface> traversedNfs = Sets.<NetworkFunctionInterface>newConcurrentHashSet();

        log.info("[{}] Synthesizing service chain with ID {}", label(), sc.id());

        /**
         * Measure the time it takes to synthesize this service chain.
         */
        // START
        WallClockNanoTimestamp startSynthMon = new WallClockNanoTimestamp();

        for (ServiceChainVertexInterface scVertex : scVertices) {
            // We always start from an entry point in the graph
            if (!scVertex.isEntryPoint()) {
                continue;
            }

            // Each service chain component contains a network function
            NetworkFunctionInterface nf = scVertex.networkFunction();
            if (nf == null) {
                log.error(
                    "[{}] Problematic graph of network functions of the service chain with ID {}",
                    label(),
                    sc.id()
                );
                return false;
            }

            log.debug(
                "[{}] NF {} is an entry point",
                label(),
                nf.id()
            );

            traversedNfs.add(nf);

            // Retrieve the packet processing graph of this NF
            NetworkFunctionGraphInterface nfGraph = nf.networkFunctionGraph();

            // Start form the entry packet processing blocks of this NF
            Set<ProcessingBlockInterface> entryBlocks = nf.entryPointProcessingBlocks();
            if (entryBlocks == null) {
                throw new SynthesisException(
                    "An entry point NF must contain entry point packet processing blocks"
                );
            }

            // For each of the entry packet processing blocks
            for (ProcessingBlockInterface entryBlock : entryBlocks) {
                // Find the vertex of the NF graph associated with this processing block
                NetworkFunctionVertexInterface nfVertex = nfGraph.getVertexWithBlock(entryBlock);
                if (nfVertex == null) {
                    throw new SynthesisException(
                        "Block " + entryBlock.id() + " corresponds to a non-existent NF"
                    );
                }

                ProcessingBlockInterface block = nfVertex.processingBlock();
                if (block != entryBlock) {
                    throw new SynthesisException(
                        "Inconsistency in entry block " + entryBlock.id()
                    );
                }

                // Retrieve the network interface associated with this entry block
                Device devBlock = (Device) block;
                String devName  = devBlock.devName();

                log.info(
                    "[{}] \tEntry block {} with interface {} that belongs to NF {}",
                    label(),
                    entryBlock.id(),
                    devName,
                    nf.id()
                );

                /**
                 * Wrap this block with additional information
                 * that makes it a dataplane block.
                 */
                NfvDataplaneBlock dataplaneBlock = new NfvDataplaneBlock(nf, block);

                log.info(
                    "[{}] \t============ DFS to compose the packet processing graph",
                    label()
                );

                /**
                 * Staring from this entry processing block,
                 * go DFS until the end of the entire service
                 * chain is reached.
                 */
                dfsServiceChainGraphBuilder(
                    scGraph,
                    nfGraph,
                    traversedNfs,
                    scVertex,
                    nfVertex,
                    nf,
                    dataplaneBlock,
                    block,
                    new AtomicReference(devName)
                );

                /**
                 ************************************************************
                 * Core SNF logic lies inside this tree
                 ************************************************************
                 * Now build the traffic classes of the NFV dataplane nodes,
                 * by organizing them into a tree and traversing the tree
                 * from any possible input to any possible output.
                 ************************************************************
                 */
                NfvDataplaneTreeInterface dataplaneTree = new NfvDataplaneTree(
                    dataplaneBlock,
                    devName,
                    label()
                );

                // Push this dataplane tree to the distributed data store
                serviceChainService.addRunnableServiceChain(
                    sc.id(), devName, dataplaneTree
                );

                log.info(
                    "[{}] \t============ Packet processing graph has been successfully composed\n",
                    label()
                );
            }
        }

        // Verify that DFS has terminated successfully
        checkArgument(
            serviceChainLength == traversedNfs.size(),
            "[" + label() + "] DFS for service chain " + sc.id() + " failed. " +
            "Only " + traversedNfs.size() + " out of " + serviceChainLength + " NFs were traversed"
        );

        // STOP
        WallClockNanoTimestamp endSynthMon = new WallClockNanoTimestamp();

        // This is the time difference
        float synthesisDelay = (float) (endSynthMon.unixTimestamp() - startSynthMon.unixTimestamp());

        // Store it
        monitoringService.addSynthesisDelayOfServiceChain(sc.id(), synthesisDelay);

        /**
         * State transition for the service chain
         * From CONSTRUCTED --> SYNTHESIZED
         */
        this.markServiceChainAsSynthesized(sc);

        return true;
    }

    /**
     * Recursive depth first search function to visit all vertices from 'vertex'.
     * The vertices can also belong to different graph, so in reality,
     * this is a recursive graph composition function.
     *
     * @param scGraph service chain graph (vertex = NF)
     * @param nfGraph network function graph (vertex = PacketProcessingBlock)
     * @param traversedNfs set of currently travesed network functions
     * @param scVertex current service chain vertex in the service chain graph
     * @param nfVertex current network function vertex in the network function graph
     * @param nf current network function (part of the nfVertex)
     * @param dataplaneBlock an NFV dataplane block that stores the full path and
     *        the filters, while traversing the tree.
     * @param block the network function's current processing block
     * @param inputDevName the network interface of the entry block (e.g., FromDevice(em1))
     */
    private static void dfsServiceChainGraphBuilder(
            ServiceChainGraphInterface     scGraph,
            NetworkFunctionGraphInterface  nfGraph,
            Set<NetworkFunctionInterface>  traversedNfs,
            ServiceChainVertexInterface    scVertex,
            NetworkFunctionVertexInterface nfVertex,
            NetworkFunctionInterface       nf,
            NfvDataplaneBlock              dataplaneBlock,
            ProcessingBlockInterface       block,
            AtomicReference                inputDevName) {
        // Retrieve the list of neighbors of this block
        List<NetworkFunctionEdgeInterface> neighboringEdges = nfGraph.getSortedEdgesFrom(nfVertex);
        int neighborsNumber = neighboringEdges.size();

        // This node is a leaf --> end point
        if ((neighboringEdges == null) || (neighboringEdges.isEmpty())) {
            ProcessingBlockInterface thisBlock = block;

            // Can be discard
            if (Blocks.DROP_ELEMENTS.contains(thisBlock.getClass())) {
                log.info(
                    "[{}] \t\t-----> DROP point {} in Network Function {}\n",
                    label(), thisBlock.processingBlockClass(), nf.name().toUpperCase()
                );
                return;
            }

            // Otherwise it is a device element, get its network interface
            Device thisDevBlock = (Device) thisBlock;
            String thisDevName  = thisDevBlock.devName();
            // And the device object associated with this interface
            NetworkFunctionDeviceInterface thisDev = nf.getDeviceByInterfaceName(thisDevName);

            // Find which NF peers with this device. (null implies an exit point!)
            NetworkFunctionInterface nextNf = nf.peersWithNF(thisDev);
            String expectedNextDevName = nf.peersWithInterface(thisDev);

            /**
             * This NF is an exit point and the interface name of this exit point
             * is different from the interface name that we started our search
             * (hence there is no chance to have a loop). You have reached a way out!
             */
            if (nf.isExitPoint() && !inputDevName.get().equals(thisDevName)) {
                log.info(
                    "[{}] \t\t-----> EXIT point {}({}) in Network Function {}\n",
                    label(), thisBlock.processingBlockClass(), thisDevName, nf.name().toUpperCase()
                );
                return;
            /**
             * This NF is an exit point but the interface name of this exit point
             * is the same with the interface name that we started our search --> Loop!
             */
            } else if (nf.isExitPoint() && inputDevName.get().equals(thisDevName)) {
                log.info(
                    "[{}] \t\t-----> LOOP {}({}) in Network Function {}\n",
                    label(), thisBlock.processingBlockClass(), thisDevName, nf.name().toUpperCase()
                );
                return;
            } else {
                // This path leads to the clif but we need to know which traffic classes can be dropped early ;)
                if (Blocks.DROP_ELEMENTS.contains(thisBlock.getClass())) {
                    log.info(
                        "[{}] \t\t-----> DROP\n", label()
                    );
                    return;
                // This is a connection point between this NF and a subsequent one.
                } else if (Blocks.OUTPUT_ELEMENTS.contains(thisBlock.getClass())) {
                    checkNotNull(
                        nextNf, "[" + label() + "] Next hop network function is unexpectedly NULL"
                    );

                    /**
                     * Fetch all the terminal blocks of the next NF that belong to stage INPUT.
                     * One of them must be the successor we are looking for.
                     */
                    Set<NetworkFunctionVertexInterface> inputVertices =
                        nextNf.getTerminalVerticesByStage(TerminalStage.INPUT);
                    checkNotNull(
                        inputVertices, "No way to jump from " + nf.name().toUpperCase() +
                        " to " + nextNf.name().toUpperCase()
                    );

                    ProcessingBlockInterface nextBlock = null;
                    NetworkFunctionVertexInterface nextNfVertex = null;

                    // Iterate through the list of input processing blocks of the next NF
                    boolean found = false;
                    for (NetworkFunctionVertexInterface nextVer : inputVertices) {
                        nextNfVertex = nextVer;
                        nextBlock = nextVer.processingBlock();

                        // Verify that this is actually a terminal block with stage INPUT
                        if (!Blocks.INPUT_ELEMENTS.contains(nextBlock.getClass())) {
                            throw new SynthesisException(
                                "[" + label() + "] Input terminal block was expected instead of " +
                                nextBlock.id() + "(" + nextBlock.processingBlockClass() + ")"
                            );
                        }

                        // Keep the network interface of this block
                        Device nextDevBlock = (Device) nextBlock;
                        String nextDevName = nextDevBlock.devName();

                        // If the network interfaces match, we got it!
                        if (expectedNextDevName.equals(nextDevName)) {
                            found = true;

                            log.info(
                                "[{}] \t\t-----> JUMP FROM {} -> {} bridging {}({}) with {}({})\n",
                                label(), nf.name().toUpperCase(), nextNf.name().toUpperCase(),
                                thisBlock.processingBlockClass(), thisDevName,
                                nextBlock.processingBlockClass(), nextDevName
                            );
                            break;
                        }
                    }

                    // This should never happen
                    if (!found || (nextBlock == null) || (nextNfVertex == null)) {
                        throw new SynthesisException(
                            "[" + label() + "] Unable to find the next jump after " +
                            thisBlock.id() + ", although there is one"
                        );
                    }

                    // Set the next hop information (crucial for proper recursion)
                    traversedNfs.add(nextNf);
                    nfVertex = nextNfVertex;
                    nf       = nextNf;
                    nfGraph  = nf.networkFunctionGraph();
                    scVertex = scGraph.getVertexWithNetworkFunction(nf);
                    block    = nextBlock;
                    inputDevName.set(expectedNextDevName);
                }
            }
        }

        // This time the neighbors might have changed because of a jump above!
        neighboringEdges = nfGraph.getSortedEdgesFrom(nfVertex);

        int count = 0;
        // Go through the successors of this block in a recursive manner
        for (NetworkFunctionEdgeInterface nfEdge : neighboringEdges) {
            // This block
            NetworkFunctionVertexInterface src = nfEdge.src();
            // .. and its processing block
            ProcessingBlockInterface srcBlock = src.processingBlock();

            // Check consistency
            checkArgument(
                src.equals(nfVertex),
                "[" + label() + "] Inconsistency in the packet processing graph."
            );

            // A successor
            NetworkFunctionVertexInterface dst = nfEdge.dst();
            // .. and its processing block
            ProcessingBlockInterface dstBlock = dst.processingBlock();

            // Blackbox integration must be consistent
            if (ProcessingBlockClass.isBlackbox(srcBlock.processingBlockClass().toString())) {
                // E.g., FromBlackboxDevice must be strictly followed by ToBlackboxDevice
                checkArgument(ProcessingBlockClass.isBlackbox(dstBlock.processingBlockClass().toString()));

                // Peer these two blocks
                ToBlackboxDevice devBlock = (ToBlackboxDevice) dstBlock;
                devBlock.setPeerDevice((FromBlackboxDevice) srcBlock);
            }

            log.info(
                "[{}] \t\t From {} ({}) To {} ({})",
                label(), src.processingBlock().processingBlockClass(), src.processingBlock().id(),
                dst.processingBlock().processingBlockClass(), dst.processingBlock().id()
            );

            // Wrap the child NFV dataplane block
            NfvDataplaneBlock child = new NfvDataplaneBlock(nf, dstBlock);
            dataplaneBlock.setChild(child, count++);

            // Recursive DFS
            dfsServiceChainGraphBuilder(
                scGraph, nfGraph, traversedNfs, scVertex, dst, nf, child, dstBlock, inputDevName
            );
        }

        return;
    }

    /**
     * If the state of this component has changed based upon external
     * triggers, then on activation the synthesizer has to retrieve
     * the list of CONSTRUCTED service chains from the service chain
     * manager, and synthesize those service chains.
     */
    private void serveWaitingServiceChains() {
        Set<ServiceChainInterface> constructedServiceChains =
            serviceChainService.serviceChainsInState(
                ServiceChainState.CONSTRUCTED
            );

        this.serviceChainsToSynthesize.addAll(constructedServiceChains);

        // Nothing found
        if (!this.hasServiceChainsToSynthesize()) {
            return;
        }

        // Our list is not empty, synthesize!
        if (!this.synthesize()) {
            log.error(
                "[{}] Failed to synthesize the waiting service chains",
                label()
            );
        }
    }

    /**
     * Returns whether the synthesizer is allowed to proceed,
     * according to the component configuration.
     *
     * @return boolean status of the synthesizer
     */
    private boolean isAllowedToProceed() {
        return this.enableSynthesizer;
    }

    /**
     * Returns a label with the Metron Synthesizer's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        // Before the change
        boolean previousState = this.enableSynthesizer;

        // Read the given value
        Boolean enableSynthesizerGiven =
                Tools.isPropertyEnabled(properties, "enableSynthesizer");

        // Not actually given
        if (enableSynthesizerGiven == null) {
            log.info(
                "Synthesizer is not configured; " +
                 "using current value of {}", this.enableSynthesizer
            );
        } else {
            this.enableSynthesizer = enableSynthesizerGiven;
            log.info(
                "Configured! Synthesizer state is {}",
                this.enableSynthesizer ? "enabled" : "disabled"
            );
        }

        // After the change (if any)
        boolean currentState = this.enableSynthesizer;

        /**
         * If state has changed and the previous state was disabled,
         * we need to manually go through the list of service chains
         * in the store, to identify whether there is any CONSTRUCTED
         * service chain, waiting for synthesis.
         */
        if ((previousState != currentState) && (!previousState)) {
            this.serveWaitingServiceChains();
        }
    }

    /**
     * Handles events related to the service chains that reside in the service chain store.
     * The synthesizer cares about service chains that are in state CONSTRUCTED.
     * This state comes after INIT and ensures that a service chain contains a
     * complete description of its network functions, which in turn contain
     * a complete description of their packet processing elements.
     */
    protected class InternalServiceChainListener implements ServiceChainListenerInterface {
        @Override
        public void event(ServiceChainEvent event) {
            synthesizerExecutor.execute(() -> {

                // Parse the event to identify the service chain and its state
                ServiceChainState state  = event.type();
                ServiceChainInterface sc = event.subject();

                // Proceed only if the component is active
                if (!isAllowedToProceed()) {
                    log.debug(
                        "The Metron Synthesizer is disabled. " +
                        "The service chain with ID {} transitions directly to the READY state",
                        sc.id()
                    );

                    // Do the transition
                    markServiceChainAsReady(sc);

                    return;
                }

                // Filter out the events we do care about.
                if (!this.isConstructed(state)) {
                    return;
                }

                log.info("");
                log.info(
                    "[{}] Service chain with ID {} is in state {}",
                    label(),
                    sc.id(),
                    state
                );

                // Add this service chain to the list of "ready to synthesize" service chains
                addServiceChainToSynthesizer(sc);

                // Perform the synthesis
                if (!synthesize()) {
                    log.error(
                        "[{}] Failed to synthesize the service chain with ID {}",
                        label(),
                        sc.id()
                    );
                }
            });
        }

        /**
         * Returns whether this is a relevant state for a service chain.
         * The synthesizer cares about service chains in state CONSTRUCTED.
         *
         * @return boolean interest
         */
        private boolean isConstructed(ServiceChainState state) {
            return isValidState(state) && (state == ServiceChainState.CONSTRUCTED);
        }

        /**
         * Service chain events must strictly exhibit states
         * specified in ServiceChainState.
         *
         * @param state the state of the service chain
         * @return boolean validity
         */
        private boolean isValidState(ServiceChainState state) {
            return ArrayUtils.contains(ServiceChainState.values(), state);
        }
    }

}
