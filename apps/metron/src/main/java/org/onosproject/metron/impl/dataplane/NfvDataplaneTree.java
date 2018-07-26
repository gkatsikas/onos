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

package org.onosproject.metron.impl.dataplane;

import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassType;
import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.dataplane.NfvDataplaneNodeInterface;
import org.onosproject.metron.api.dataplane.NfvDataplaneTreeInterface;
import org.onosproject.metron.api.dataplane.PathEstablisherInterface;
import org.onosproject.metron.api.dataplane.TagService;
import org.onosproject.metron.api.exceptions.SynthesisException;
import org.onosproject.metron.api.exceptions.DeploymentException;
import org.onosproject.metron.api.monitor.WallClockNanoTimestamp;
import org.onosproject.metron.api.networkfunction.NetworkFunctionType;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.servicechain.ServiceChainId;

import org.onosproject.metron.impl.classification.trafficclass.TrafficClass;
import org.onosproject.metron.impl.processing.Blocks;

import org.onosproject.core.ApplicationId;

import org.onosproject.drivers.server.BasicServerDriver;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.FlowRxFilterValue;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowRule;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

import org.slf4j.Logger;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A tree of NFV packet processing blocks to model
 * end-to-end paths in a service chain.
 */
public class NfvDataplaneTree implements NfvDataplaneTreeInterface {

    private static final Logger log = getLogger(NfvDataplaneTree.class);

    /**
     * Verbosity flag.
     */
    private static final boolean VERBOSE = true;

    /**
     * Pointer to the root node of the tree.
     */
    private NfvDataplaneNodeInterface root;

    /**
     * The type of this service chain.
     * Depends upon the types of its NFs.
     * When Click NFs are only present --> type=click
     * When standalone NFs are only present --> type=standalone
     * When Click NFs are chained with standalone NFS --> type=mixed
     */
    private NetworkFunctionType type;

    /**
     * The name of the input network interface
     * (the FromDevice elements where the tree begins).
     */
    private String inputInterfaceName;

    /**
     * Label inherited by the overlay class.
     * Serves for printing.
     */
    private static String label;

    /**
     * The set of traffic classes associated with this tree of processing elements.
     */
    private Map<Integer, TrafficClassInterface> trafficClasses =
        new ConcurrentHashMap<Integer, TrafficClassInterface>();

    /**
     * The set of traffic classes translated to hardware and/or software configuration.
     */
    private Map<Integer, String> softwareConfiguration = new ConcurrentHashMap<Integer, String>();
    private Map<URI, Set<FlowRule>> hardwareConfiguration = new ConcurrentHashMap<URI, Set<FlowRule>>();

    /**
     * This data structure folds a set of traffic classes together.
     * We assign a virtual traffic class ID per group.
     * This gives Metron the abiity to decompose the group into smaller subgroups in
     * the case of a load imbalance in the future..
     */
    private Map<URI, Set<TrafficClassInterface>> groupedTrafficClasses =
        new ConcurrentHashMap<URI, Set<TrafficClassInterface>>();
    // Keeps a map between virtual group IDs and their logical CPU core
    private Map<URI, Integer> groupTrafficClassToCoreMap = new ConcurrentHashMap<URI, Integer>();

    /**
     * After the traffic classes of a service chain are computed,
     * we must associate them with I/O elements.
     * If some I/O elements are not used by the service chain,
     * then they must be declared as IDLE.
     */
    private String idleInterfaceConfiguration;

    /**
     * Entity for managing the paths of this tree.
     */
    private PathEstablisherInterface pathEstablisher = null;

    /**
     * After the traffic classes of a service chain are computed,
     * we must associate them with I/O elements.
     * Here we keep the number of NICs required by this traffic class.
     */
    private AtomicInteger numberOfNics;

    /**
     * A convenient way to acquire services.
     */
    private ServiceDirectory directory;

    public NfvDataplaneTree(NfvDataplaneBlock block, String inputInterfaceName, String label) {
        checkNotNull(
            block,
            "Cannot construct an NFV dataplane tree using a NULL root block."
        );

        checkArgument(
            !Strings.isNullOrEmpty(inputInterfaceName),
            "Cannot construct an NFV dataplane tree using a NULL or empty input interface"
        );

        checkArgument(
            !Strings.isNullOrEmpty(label),
            "Cannot construct an NFV dataplane tree using a NULL or empty label"
        );

        this.root = new NfvDataplaneNode(block);
        this.type = null;
        this.inputInterfaceName = inputInterfaceName;
        this.label = label;
        this.idleInterfaceConfiguration = "";

        this.init();
        this.buildTrafficClasses();
    }

    public NfvDataplaneTree(NfvDataplaneBlock block, String label) {
        checkNotNull(
            block,
            "Cannot construct an NFV dataplane tree using a NULL root block."
        );

        checkArgument(
            !Strings.isNullOrEmpty(label),
            "Cannot construct an NFV dataplane tree using a NULL or empty label"
        );

        this.root = new NfvDataplaneNode(block);
        this.type = null;
        this.inputInterfaceName = "";
        this.label = label;
        this.idleInterfaceConfiguration = "";

        this.init();
        this.buildTrafficClasses();
    }

    protected void init() {
        this.numberOfNics = new AtomicInteger(0);
        this.directory = new DefaultServiceDirectory();
    }

    /**
     * Returns an implementation of the specified service class.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return implementation class
     * @throws org.onlab.osgi.ServiceNotFoundException if no implementation found
     */
    protected <T> T get(Class<T> serviceClass) {
        return directory.get(serviceClass);
    }

    @Override
    public NfvDataplaneNodeInterface root() {
        return this.root;
    }

    @Override
    public NetworkFunctionType type() {
        return this.type;
    }

    @Override
    public void setRoot(NfvDataplaneNodeInterface root) {
        checkNotNull(
            root,
            "Cannot construct an NFV dataplane tree using a NULL root block"
        );
        this.root = root;
    }

    @Override
    public PathEstablisherInterface createPathEstablisher(
            Path fwdPath, Path bwdPath,
            long ingressPort, long egressPort, boolean withServer) {
        this.pathEstablisher = new PathEstablisher(
            fwdPath, bwdPath, ingressPort, egressPort, withServer
        );
        return this.pathEstablisher;
    }

    @Override
    public PathEstablisherInterface createPathEstablisher(
            ConnectPoint point1, ConnectPoint point2,
            long ingressPort, long egressPort,
            boolean withServer) {
        this.pathEstablisher = new PathEstablisher(
            point1, point2, ingressPort, egressPort, withServer
        );
        return this.pathEstablisher;
    }

    @Override
    public PathEstablisherInterface pathEstablisher() {
        return this.pathEstablisher;
    }

    @Override
    public String inputInterfaceName() {
        return this.inputInterfaceName;
    }

    @Override
    public void setInputInterfaceName(String inputInterfaceName) {
        this.inputInterfaceName = inputInterfaceName;
    }

    @Override
    public Map<Integer, TrafficClassInterface> trafficClasses() {
        return this.trafficClasses;
    }

    @Override
    public TrafficClassInterface trafficClassOnCore(int core) {
        checkArgument(core >= 0, "Cannot get traffic class from negative core.");
        return this.trafficClasses.get(Integer.valueOf(core));
    }

    @Override
    public void addTrafficClassOnCore(int core, TrafficClassInterface trafficClass) {
        checkArgument(core >= 0, "Cannot add traffic class to negative core.");
        checkNotNull(trafficClass, "Cannot add a NULL traffic class.");

        this.trafficClasses.put(Integer.valueOf(core), trafficClass);
    }

    @Override
    public Map<URI, Set<TrafficClassInterface>> groupedTrafficClasses() {
        return this.groupedTrafficClasses;
    }

    @Override
    public Set<TrafficClassInterface> getGroupedTrafficClassesWithID(URI groupId) {
        checkNotNull(groupId, "Cannot get the group of traffic classes using a NULL group ID.");

        return this.groupedTrafficClasses.get(groupId);
    }

    @Override
    public Map<URI, Integer> groupTrafficClassToCoreMap() {
        return this.groupTrafficClassToCoreMap;
    }

    @Override
    public void pinGroupTrafficClassToCore(URI groupId, int core) {
        checkNotNull(groupId, "Cannot add a NULL group traffic class ID.");
        checkArgument(core >= 0, "Cannot add group traffic class to negative core.");

        this.groupTrafficClassToCoreMap.put(groupId, new Integer(core));
    }

    @Override
    public synchronized URI groupTrafficClassIdOnCore(int core) {
        checkArgument(core >= 0, "Cannot get the group traffic class ID of a negative core.");

        for (Map.Entry<URI, Integer> entry : this.groupTrafficClassToCoreMap.entrySet()) {
            int c = entry.getValue().intValue();

            // We found the desired logical CPU core
            if (core == c) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Override
    public boolean hasTrafficClass(URI tcId) {
        for (URI id : this.groupedTrafficClasses.keySet()) {
            if (id.equals(tcId)) {
                return true;
            }
        }

        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            if (tc.id().equals(tcId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String idleInterfaceConfiguration() {
        return this.idleInterfaceConfiguration;
    }

    @Override
    public synchronized int numberOfNics() {
        return this.numberOfNics.get();
    }

    @Override
    public synchronized void setNumberOfNics(int nics) {
        checkArgument(nics >= 0, "Traffic class must use zero or more NICs.");
        this.numberOfNics.set(nics);
    }

    @Override
    public boolean isTotallyOffloadable() {
        int count = 0;
        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            if (tc.isTotallyOffloadable()) {
                count++;
            }
        }

        // All the traffic classes are completely offloadbale
        if (count == this.trafficClasses.values().size()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isPartiallyOffloadable() {
        // At least one traffic class is partially offloadbale
        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            if (tc.isPartiallyOffloadable()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized Map<Integer, String> softwareConfiguration(
            RestServerSBDevice server, boolean withHwOffloading) {
        if (this.softwareConfiguration.size() == 0) {
            try {
                this.generateSoftwareConfiguration(server, withHwOffloading);
            } catch (DeploymentException dEx) {
                return null;
            }
        }

        return this.softwareConfiguration;
    }

    @Override
    public String softwareConfigurationOnCore(int core) {
        checkArgument(core >= 0, "CPU core number must not be negative");
        return this.softwareConfiguration.get(Integer.valueOf(core));
    }

    @Override
    public void setSoftwareConfigurationOnCore(int core, String conf) {
        checkArgument(core >= 0, "CPU core number must not be negative");
        checkArgument(!conf.isEmpty(), "Cannot set empty configuration on core " + core);

        // Append the idle configuration information to make the configuration runnable
        conf += "\n" + this.idleInterfaceConfiguration();

        this.softwareConfiguration.put(Integer.valueOf(core), conf);
    }

    @Override
    public Map<URI, Set<FlowRule>> hardwareConfiguration() {
        return this.hardwareConfiguration;
    }

    @Override
    public Set<FlowRule> hardwareConfigurationToSet() {
        Set<FlowRule> scRules = Sets.<FlowRule>newConcurrentHashSet();

        for (Map.Entry<URI, Set<FlowRule>> entry : this.hardwareConfiguration.entrySet()) {
            Set<FlowRule> rules = entry.getValue();

            if (rules != null) {
                scRules.addAll(rules);
            }
        }

        return scRules;
    }

    @Override
    public Set<FlowRule> hardwareConfigurationOfTrafficClass(URI tcId) {
        return this.hardwareConfiguration.get(tcId);
    }

    @Override
    public void setHardwareConfigurationOfTrafficClass(URI tcId, Set<FlowRule> rules) {
        checkNotNull(tcId, "Traffic class ID is NULL");
        checkNotNull(rules, "Hardware configuration of traffic class is NULL");

        this.hardwareConfiguration.put(tcId, rules);
    }

    @Override
    public void buildAndCompressBinaryTree() {
        // Buid the traffic classes by creating their binary classification tree
        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            tc.buildBinaryTree();
        }

        /**
         * Compress the traffic classes of the entire service chain.
         * TODO: Reduce the comnplexity of this task.
         */
        this.compressTrafficClasses();

        // Go through the compressed set of traffic classes and generate their filters
        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            tc.generatePacketFilters();
        }
    }

    @Override
    public void generateSoftwareConfiguration(
            RestServerSBDevice server, boolean withHwOffloading)
            throws DeploymentException {
        // The software configuration is already computed
        if (this.softwareConfiguration.size() > 0) {
            return;
        }

        /**
         * Compose the configuration of each traffic class,
         * considering that it will be realized in software.
         * Hence we set doHwOffloading = false
         */
        this.composeTrafficClassesConfiguration(server, false);

        /**
         * To optimize the CPU utilization of the target NFV server,
         * group traffic classes that exhibit the same write operations.
         * In case of a load imbalance, this will also give us the opportunity
         * to scale out, by re-grouping (splitting into subgroups) the traffic classes.
         */
        this.groupTrafficClasses();

        int tcCount = 0;

        // Each group of traffic classes has its own individual configuration
        for (Map.Entry<URI, Set<TrafficClassInterface>> entry : this.groupedTrafficClasses.entrySet()) {
            Set<TrafficClassInterface> tcSet = entry.getValue();

            /**
             * Get the first traffic class of this group.
             * This traffic class has the same input/output configuration
             * and the same write operations with any other traffic class in the group.
             */
            TrafficClassInterface tc = tcSet.iterator().next();

            // The traffic class configuration step-by-step
            String tcConf = "";

            /**
             * Standalone NFs have an isolated pipeline.
             * This does not hold for mixed configurations (Click + Blackbox).
             */
            if (tc.isSolelyOwnedByBlackbox()) {
                tcConf += tc.blackboxOperationsAsString();
                this.setSoftwareConfigurationOnCore(tcCount++, tcConf);
                continue;
            }

            // A. Input part
            tcConf += this.generateInputOperations(tc, "");

            // Decide the fate of this group of traffic classes
            String action = "allow";
            if (tc.isDiscarded()) {
                action = "deny";
            }

            // Name the filter element of this group (if to be used)
            String filterName = "filter" + tcCount;

            // B. Read part
            String readOps = "";

            try {
                readOps = this.generateReadOperations(
                    tcSet, filterName, action, withHwOffloading
                );
                tcConf += readOps + " ";
            } catch (DeploymentException dEx) {
                throw dEx;
            }

            // C. Stateful Write part
            tcConf += this.generateWriteOperations(tc, filterName);

            // D. Post-routing operations
            tcConf += tc.postRoutingPipeline();

            // E. Output part
            tcConf += tc.outputOperationsAsString();

            // B. continued: A filter always has a second output for the drops
            if (!readOps.isEmpty()) {
                tcConf += " " + filterName + "[1] -> Discard;";
            }

            // Assign traffic class configuration to CPU core
            this.setSoftwareConfigurationOnCore(tcCount++, tcConf);
        }

        // Print the software configuration
        if (VERBOSE) {
            this.printSoftwareConfiguration();
        }
    }

    @Override
    public Map<URI, RxFilterValue> generateHardwareConfiguration(
            ServiceChainId  scId,
            ApplicationId   appId,
            DeviceId        deviceId,
            long            inputPort,
            long            queuesNumber,
            long            outputPort,
            boolean         tagging,
            Map<URI, Float> tcCompDelay)
            throws DeploymentException {
        // The hardware configuration is already computed
        if (this.hardwareConfiguration.size() > 0) {
            return null;
        }

        Map<URI, RxFilterValue> tagMap = new ConcurrentHashMap<URI, RxFilterValue>();

        // Reserve an array to store the timestamps
        if (tcCompDelay == null) {
            tcCompDelay = new ConcurrentHashMap<URI, Float>();
        }

        /**
         * Compose the configuration of each traffic class,
         * considering that it can be offloaded in hardware.
         * Hence we set doHwOffloading = true
         */
        this.composeTrafficClassesConfiguration(null, true);

        this.groupTrafficClasses();

        int tcCount = 0;

        for (Map.Entry<URI, Set<TrafficClassInterface>> entry : this.groupedTrafficClasses.entrySet()) {
            URI tcGroupId = entry.getKey();
            Set<TrafficClassInterface> tcSet = entry.getValue();

            // Get the tagging information for the entire group
            RxFilter rxFilter = null;
            RxFilterValue rxFilterValue = null;
            if (tagging) {
                rxFilter = this.tagService().getTaggingMechanismOfTrafficClassGroup(tcGroupId);
                rxFilterValue = this.tagService().getFirstUsedTagOfTrafficClassGroup(tcGroupId);
                log.info(
                    "[{}] \t Traffic class {} --> Method {} with Tag {}",
                    label(), tcGroupId, rxFilter, rxFilterValue
                );

                if ((rxFilter != null) && (rxFilterValue != null)) {
                    tagMap.put(tcGroupId, rxFilterValue);
                }
            }

            /**
             * Measure the time it takes to compute the
             * OpenFlow rules for this group of traffic classes.
             */
            // START
            WallClockNanoTimestamp startTcMon = new WallClockNanoTimestamp();

            // Store the rules of this group of traffic classes
            Set<FlowRule> rules = null;

            try {
                rules = convertTrafficClassSetToOpenFlowRules(
                    tcSet, tcGroupId, appId, deviceId,
                    inputPort, queuesNumber, outputPort,
                    rxFilter, rxFilterValue, tagging
                );
            } catch (DeploymentException depEx) {
                throw depEx;
            }
            checkNotNull(rules, "Failed to generate hardware configuration");

            // STOP
            WallClockNanoTimestamp endTcMon = new WallClockNanoTimestamp();

            // This is the time difference
            tcCompDelay.put(tcGroupId, new Float(endTcMon.unixTimestamp() - startTcMon.unixTimestamp()));

            // Add them to the hardware configuration
            this.setHardwareConfigurationOfTrafficClass(tcGroupId, rules);

            /**
             * Now we need the software-part of this group of traffic classes.
             * Just one traffic class of the group is enough to give us this information
             * because within a group, all traffic classes have the same write operations.
             */
            TrafficClassInterface tc = tcSet.iterator().next();

            // This traffic class is totally offlodable, hence no software is required
            if (tc.isTotallyOffloadable()) {
                continue;
            }

            // The software part of the traffic class configuration step-by-step
            String tcConf = "";

            // A. Input part
            String inputInst = "in" + tcCount;
            tcConf += this.generateInputOperations(tc, inputInst);

            // B. Stateful Write part
            tcConf += this.generateWriteOperations(tc, inputInst);

            // C. Post-routing operations
            tcConf += tc.postRoutingPipeline();

            // D. Output part
            if (tc.isDiscarded()) {
                throw new DeploymentException(
                    "A traffic class that leads to a drop operation must be totally offloadable."
                );
            } else {
                tcConf += tc.outputOperationsAsString();
            }

            // Assign traffic class to CPU core
            this.setSoftwareConfigurationOnCore(tcCount++, tcConf);
        }

        // Print the hardware and software (if any) configuration
        if (VERBOSE) {
            this.printHardwareConfiguration();
            this.printSoftwareConfiguration();
        }

        return tagMap;
    }

    /**
     * Translates a set of TC objects into hardware rules.
     *
     * @param tcSet the set of traffic classes to be translated
     * @param tcGroupId the traffic class ID of this group
     * @param appId the application ID that demands
     *        this hardware configuration
     * @param deviceId the device where the hardware
     *        configuration will be installed
     * @param inputPort the input port where packet arrived
     * @param queuesNumber the number of input queues to spread
     *        the traffic across
     * @param outputPort the port of the device where the
     *        hardware configuration will be sent out
     * @param rxFilter runtime information about the Rx tagging
     *        mechanism used by this set of traffic classes
     * @param rxFilterValue runtime information about the Rx taggging
     *        value used by this set of traffic classes
     * @param tagging indicates that tagging needs to be associated
     *        with the generated rules
     * @return set of rules for this traffic class
     * @throws DeploymentException if the translation to rules fails
     */
    public static Set<FlowRule> convertTrafficClassSetToOpenFlowRules(
            Set<TrafficClassInterface> tcSet,
            URI                        tcGroupId,
            ApplicationId              appId,
            DeviceId                   deviceId,
            long                       inputPort,
            long                       queuesNumber,
            long                       outputPort,
            RxFilter                   rxFilter,
            RxFilterValue              rxFilterValue,
            boolean                    tagging) throws DeploymentException {
        // Store the rules of this group of traffic classes
        Set<FlowRule> rules = Sets.<FlowRule>newConcurrentHashSet();

        long queueIndex = 0;
        for (TrafficClassInterface tc : tcSet) {

            if (tc.inputInterface().isEmpty()) {
                tc.setInputInterface(BasicServerDriver.findNicInterfaceWithPort(deviceId, inputPort));
            }

            if (tc.outputInterface().isEmpty()) {
                tc.setOutputInterface(BasicServerDriver.findNicInterfaceWithPort(deviceId, outputPort));
            }

            // A tag is directly provided
            if (rxFilter == RxFilter.FLOW) {
                queueIndex = ((FlowRxFilterValue) rxFilterValue).value();
            // Round-robin across the available queues
            } else {
                queueIndex = (queuesNumber > 0) ? (queueIndex++ % queuesNumber) : -1;
            }

            // Compute the rules of this traffic class
            try {
                rules.addAll(
                    tc.toOpenFlowRules(
                        appId, deviceId, tcGroupId,
                        inputPort, queueIndex, outputPort,
                        rxFilter, rxFilterValue, tagging
                    )
                );
            } catch (DeploymentException depEx) {
                throw depEx;
            }
        }

        return rules;
    }

    @Override
    public TagService tagService() {
        return this.directory.get(TagService.class);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.root,
            this.inputInterfaceName,
            this.trafficClasses
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof NfvDataplaneTree))) {
            return false;
        }

        NfvDataplaneTree other = (NfvDataplaneTree) obj;

        return  Objects.equals(this.root, other.root) &&
                Objects.equals(this.trafficClasses, other.trafficClasses) &&
                this.inputInterfaceName.equals(other.inputInterfaceName);
    }

    @Override
    public String toString() {
        String result = "";

        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            result += tc.toString() + "\n";
        }

        result += groupedTrafficClasses().size() + " groups of traffic classes";

        return result;
    }

    /************************************** Internal services. ***********************************/
    /**
     * Generate the input operations of a traffic class and
     * associate them with a new pipeline.
     *
     * @param tc a traffic class with write operations
     * @param pipelineInstance an instance of a pipeline to associate these input operations with
     */
    private String generateInputOperations(TrafficClassInterface tc, String pipelineInstance) {
        String inputOps = tc.inputOperationsAsString();
        if (pipelineInstance.isEmpty()) {
            return inputOps + " -> ";
        }

        int pos = inputOps.indexOf(">");
        return inputOps.substring(0, pos + 1) + " " + pipelineInstance + " :: " + inputOps.substring(pos + 2) + "; ";
    }

    /**
     * Given a set of traffic classes of a service chain, generate the
     * software-based read  operations of this set.
     * These operations can contain the entire classifier in software, or
     * a prt of this classifier complemented by an already configured hardware
     * classifier.
     *
     * @param tcSet a set of traffic classes
     * @param filterName the name of the software-based classifier
     * @param action the action taken by this classifier (i.e., allow or drop)
     * @param withHwOffloading flag that indicates potential hardware classification
     *        before the software classification
     * @throws DeploymentException if cannot decide which operations to offload
     */
    private String generateReadOperations(
            Set<TrafficClassInterface> tcSet,
            String filterName,
            String action,
            boolean withHwOffloading) throws DeploymentException {
        // All read operations will be executed in software
        boolean allReadOperationsInSoft = !withHwOffloading;

        // Indicates the presence of at least one rule
        boolean atLeastOne = false;

        String tcConf = filterName + " :: IPFilter(";

        // Get all the traffic classes of this group
        for (TrafficClassInterface trafCl : tcSet) {
            // Get the read operation of this traffic class
            String readOps = allReadOperationsInSoft ?
                trafCl.readOperationsAsString() : trafCl.softReadOperationsAsString();

            // Proceed if you have non-empty reads
            if (readOps.isEmpty()) {
                continue;
            }

            atLeastOne = true;
            tcConf += action + " " + readOps + ", ";
        }

        // Remove the trailing ', '
        if (atLeastOne) {
            tcConf = tcConf.substring(0, tcConf.length() - 2);
        // No rules found, match everything
        } else {
            tcConf += "allow any";
        }

        // Drop whatever does not match above
        tcConf += ", deny all);";

        return tcConf;
    }

    /**
     * Generate the software-based write operations of a traffic class and
     * connect them with the rest of a pipeline.
     *
     * @param tc a traffic class with write operations
     * @param pipelineInstance an instance of a pipeline to connect these write operations with
     */
    private String generateWriteOperations(TrafficClassInterface tc, String pipelineInstance) {
        String result = "";
        String writeOps = tc.writeOperationsAsString();
        boolean withIpMapper = writeOps.contains(ProcessingBlockClass.ROUND_ROBIN_IP_MAPPER.toString());

        // Squeeze the pipeline between IPMapper definition and the write part
        if (withIpMapper) {
            int pos = writeOps.indexOf(";");
            result = writeOps.substring(0, pos + 1) + " " + pipelineInstance + "[0] -> " + writeOps.substring(pos + 2);
        } else {
            result = pipelineInstance + " -> " + writeOps;
        }
        result += " -> ";

        return result;
    }

    /**
     * Given the current NF in the service chain,
     * update the service chain's type accordingly.
     *
     * @param currentBlock the current network function block
     */
    private void updateDataplaneTreeType(NfvDataplaneBlockInterface currentBlock) {
        if (currentBlock.networkFunction() == null) {
            return;
        }

        // Fetch the type of this NF block
        NetworkFunctionType nfType = currentBlock.networkFunction().type();

        checkNotNull(
            nfType, "Network function type of block " +
            currentBlock.name() + " is NULL"
        );

        if (this.type == null) {
            this.type = nfType;
            return;
        }

        if (this.type == NetworkFunctionType.MIXED) {
            // Whatever type this new NF has, type mixed covers it (generic)
            return;
        }

        // No change to report
        if (this.type == nfType) {
            return;
        }

        /**
         * The current type is different from the input one.
         * This is possible when:
         * |-> Current type is CLICK and input one is STANDALONE
         * |-> Current type is STANDALONE and input one is CLICK
         * in either case, the result type is MIXED.
         */
        this.type = NetworkFunctionType.MIXED;
    }

    /**
     * Iterative depth first search (DFS) of the NFV dataplane tree
     * to build the traffic classes of the service chain.
     */
    private void buildTrafficClasses() {
        ConcurrentLinkedDeque<NfvDataplaneNodeInterface> nodesToVisit =
            new ConcurrentLinkedDeque<NfvDataplaneNodeInterface>();
        nodesToVisit.push(this.root);

        log.info(
            "[{}] \t============ DFS to compose the service chain's traffic classes",
            label()
        );

        NfvDataplaneNodeInterface  currentNode  = null;
        NfvDataplaneBlockInterface currentBlock = null;
        TrafficClassInterface currentTrafficClass = null;

        int tcNumber = 0;

        // DFS starting from the root
        while (!nodesToVisit.isEmpty()) {
            // Pop the top of the stack
            currentNode = nodesToVisit.peek();
            nodesToVisit.pop();

            // Extract the NFV dataplane block
            currentBlock = currentNode.block();
            currentTrafficClass = currentNode.trafficClass();

            // Update the type of the service chain given this current NF
            updateDataplaneTreeType(currentBlock);

            log.info(
                "[{}] \t\t Block {} ({}) with {} output ports and conf {}",
                label(), currentBlock.blockClass(), currentBlock.name(), currentBlock.portsNumber(),
                currentBlock.basicConfiguration()
            );

            // For each output class of this block
            for (int i = 0; i < currentBlock.outputClasses().size(); i++) {
                TrafficClassInterface nextTrafficClass = new TrafficClass(currentTrafficClass);
                log.debug(
                    "[{}] \t\t\t Traffic class (Before this block): {}",
                    label(), nextTrafficClass.readOperationsAsString()
                );

                // Counts the number of write opeations of this traffic class
                Integer writeOperationsNb = new Integer(0);

                boolean success = nextTrafficClass.addBlock(currentBlock, i, writeOperationsNb);
                log.debug(
                    "[{}] \t\t\t Traffic class (After  this block): {}",
                    label(), nextTrafficClass.readOperationsAsString()
                );

                // Enlarge the output class by adding the children's classes
                if (success) {
                    NfvDataplaneBlockInterface child = currentBlock.outputClasses().get(i).child();

                    if (child == null) {
                        /**
                         * If this is not an Output terminal block (i.e., ToDevice, Discard, etc),
                         * then something is wrong.
                         */
                        if (!Blocks.OUTPUT_ELEMENTS.contains(currentBlock.processor().getClass())) {
                            throw new SynthesisException(
                                "Processing block without successors must only be an Output terminal block." +
                                "Instead, we found " + currentBlock.processor().getClass()
                            );
                        }

                        // Mark this block as a leaf
                        currentBlock.setPortsNumber(0);

                        // Add it to complete the path
                        currentTrafficClass.addBlock(currentBlock, -1, new Integer(0));

                        // Set auxiliary information
                        currentTrafficClass.computeReadOperations(true);
                        currentTrafficClass.computeWriteOperations();
                        currentTrafficClass.postRoutingPipeline();

                        // Add this traffic class
                        this.addTrafficClassOnCore(tcNumber++, currentTrafficClass);

                        log.debug("Trafic class added: {}", currentTrafficClass);

                        continue;
                    }

                    log.debug("[{}] \t\t\t Child Block {}", label(), child.blockClass());

                    // Prepare the next hop
                    NfvDataplaneNode nextNode = new NfvDataplaneNode(child, nextTrafficClass);

                    log.debug(
                        "[{}] \t\t\t Found transition from {} to {}",
                        label(),
                        currentBlock.blockClass(),
                        child.blockClass()
                    );

                    // Add a new node to be visited
                    nodesToVisit.push(nextNode);
                // This is possible in case of NATing
                } else {
                    log.error("[{}] \t\t\t Failed to add block {}", label(), currentBlock.blockClass());
                }


                log.debug(
                    "[{}] \t\t\t Number of write operations for this traffic class: {}",
                    label(), writeOperationsNb.intValue()
                );
                log.debug(
                    "[{}] \t\t\t Number of output classes for this traffic class: {}",
                    label(), currentBlock.outputClasses().size()
                );

                /**
                 * Attempt to take a decision (i.e., read using IPLookup) on
                 * re-written IP fields by stateful elements.
                 * This can happen e.g., when we try to pass a NAT/Proxy from the outside.
                 */
                // if (writeOperationsNb.intValue() == currentBlock.outputClasses().size()) {
                //     this.setBehindProxy(true);
                // }
            }
        }

        log.info(
            "[{}] \t\t Successfully composed {} traffic classes",
            label(),
            this.trafficClasses.size()
        );

        // Eliminate the empty traffic classes
        this.purgeTrafficClasses();

        // Print the trafic classes
        if (VERBOSE) {
            this.printTrafficClasses();
        }
    }

    /**
     * Goes through the set of computed traffic classes and purges the
     * ones with empty packet filters, conditions, and operations.
     */
    private void purgeTrafficClasses() {
        short purgedTrafficClasses = 0;

        Iterator<Map.Entry<Integer, TrafficClassInterface>> iterator =
            this.trafficClasses.entrySet().iterator();

        while (iterator.hasNext()) {
            TrafficClassInterface tc = iterator.next().getValue();

            /**
             * This traffic class has some meat.
             */
            if (!tc.isEmpty()) {
                continue;
            }

            // Print the purged traffic class
            log.debug(tc.toString());

            /**
             * This traffic class has empty header fields,
             * conditions, and blackbox configuration.
             * We can safely remove this traffic class!
             */
            iterator.remove();
            purgedTrafficClasses++;
        }

        log.info("[{}] \t\t Purged {} traffic classes", label(), purgedTrafficClasses);
    }

    /**
     * Goes through the set of computed traffic classes and compresses them.
     * Compression is achieved by merging traffic classes of non conflicting types
     * with the same write operations or by eliminating traffic classes that are
     * fully covered by other traffic classes.
     *
     * @return boolean compression status
     */
    private boolean compressTrafficClasses() {
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.info("[{}] Compressing traffic classes", label());
        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);

        // Here we store the new map of the compressed traffic classes
        Map<Integer, TrafficClassInterface> newTcs =
            new ConcurrentHashMap<Integer, TrafficClassInterface>();

        // Keep a copy of the original traffic classes
        Map<Integer, TrafficClassInterface> copiedTcs =
            new ConcurrentHashMap<Integer, TrafficClassInterface>(this.trafficClasses);

        // Mark the indices of the traffic classes that get merged
        Set<Integer> deletedIndices = Sets.<Integer>newConcurrentHashSet();

        int outIndex   = 0;
        int newTcIndex = 0;
        int initialNumberOfTcs = copiedTcs.size();

        // Go through the original map of traffic classes
        Iterator<Map.Entry<Integer, TrafficClassInterface>> iterator =
            this.trafficClasses.entrySet().iterator();

        while (iterator.hasNext()) {
            TrafficClassInterface outTc = iterator.next().getValue();

            log.debug("[{}] Traffic class {}", label(), outIndex);

            if (deletedIndices.contains(new Integer(outIndex))) {
                log.debug("[{}] \t Traffic class {} is already Deleted", label(), outIndex);
                outIndex++;
                continue;
            }

            String outReadOps = outTc.readOperationsAsString();

            // Check this traffic class against the original (copy of the) set
            int inIndex = 0;
            boolean isMerged = false;
            for (Map.Entry<Integer, TrafficClassInterface> inEntry : copiedTcs.entrySet()) {
                Integer inCore = inEntry.getKey();
                TrafficClassInterface inTc = inEntry.getValue();

                log.debug("[{}] \t Attempting to merge with traffic class {}", label(), inIndex);

                // Avoid double checking TC1 - TC2 and then TC2 - TC1
                if (outIndex >= inIndex) {
                    log.debug("[{}] \t\t Already checked", label());
                    inIndex++;
                    continue;
                }

                // We do not mess with deleted traffic classes
                if (deletedIndices.contains(new Integer(inIndex))) {
                    log.debug("[{}] \t\t Already deleted", label());
                    inIndex++;
                    continue;
                }

                // Skip identity (We want to avoid comparing a TC with itself)
                if (outTc.id().equals(inTc.id())) {
                    log.debug("[{}] \t\t Identity avoided", label());
                    inIndex++;
                    continue;
                }

                String inReadOps = inTc.readOperationsAsString();

                // Traffic classes of different 'post IP protocol' types cannot be merged
                if (outTc.type()  != inTc.type() &&
                   ((outTc.type() != null) && (inTc.type() != null)) &&
                   ((outTc.type() != TrafficClassType.NEUTRAL) || (inTc.type() != TrafficClassType.NEUTRAL))) {
                    log.debug(
                        "[{}] \t\t Conflicting header space between {} and {}",
                        label(), outReadOps, inReadOps
                    );
                    inIndex++;
                    continue;
                }

                /**
                 * If one traffic class fully covers another one, there is
                 * no point to keep the small one.
                 */
                if (outTc.covers(inTc)) {
                    log.debug("[{}] \t\t {} fully covers {}", label(), outReadOps, inReadOps);
                    deletedIndices.add(new Integer(inIndex));
                    inIndex++;
                    continue;
                }

                if (inTc.covers(outTc)) {
                    log.debug("[{}] \t\t {} fully covers {}", label(), inReadOps, outReadOps);
                    deletedIndices.add(new Integer(outIndex));
                    break;
                }

                /**
                 * Let's now check if they have conflicting header fields.
                 */
                if (outTc.conflictsWith(inTc)) {
                    log.debug("[{}] \t\t {} conflicts with {}", label(), outReadOps, inReadOps);
                    inIndex++;
                    continue;
                }

                // Last condition is that the two traffic classes must have identical write operations.
                if (outTc.operation().equals(inTc.operation())) {

                    log.debug(
                        "[{}] \t\t Merging {} with {}", label(),
                        outReadOps, inReadOps
                    );

                    // Mark these indices as deleted
                    deletedIndices.add(new Integer(outIndex));
                    deletedIndices.add(new Integer(inIndex));

                    // Merge the packet filters and conditions of these 2 traffic classes
                    outTc.packetFilter().addPacketFilter(inTc.packetFilter());
                    outTc.conditionMap().addConditionMap(inTc.conditionMap());

                    // Add the merged traffic class into the new map
                    newTcs.put(new Integer(newTcIndex++), new TrafficClass(outTc));

                    isMerged = true;
                    break;
                }

                log.debug("[{}] \t\t Different write operations", label());

                inIndex++;
            }

            /**
             * This traffic class could not merge with anyone.
             * Keep it in the new set if it is not already marked for deletion.
             */
            if (!isMerged && !deletedIndices.contains(outIndex)) {
                newTcs.put(new Integer(newTcIndex++), new TrafficClass(outTc));
                deletedIndices.add(new Integer(outIndex));
            }

            // Next traffic class please
            outIndex++;
        }

        // Update the traffic classes with the compressed list
        this.trafficClasses = newTcs;

        // Find how many of them we compressed
        int compressedTrafficClasses = initialNumberOfTcs - this.trafficClasses.size();

        log.debug("");
        log.info(
            "[{}] Compressed {} traffic classes",
            label(), compressedTrafficClasses
        );

        log.info("[{}] {}", label(), Constants.STDOUT_BARS_SUB);
        log.debug("");

        return compressedTrafficClasses > 0;
    }

    /**
     * For software based configurations,
     * aggregate traffic classes into a single IPClassifier.
     * The requirement is that all such traffic classes will later
     * exhibit the same write operations.
     */
    private void groupTrafficClasses() {
        if (this.groupedTrafficClasses.size() > 0) {
            return;
        }

        Map<Integer, TrafficClassInterface> clonedTrafficClasses =
            new ConcurrentHashMap<Integer, TrafficClassInterface>(this.trafficClasses);
        Map<Integer, Set<TrafficClassInterface>> readersToWriters =
            new ConcurrentHashMap<Integer, Set<TrafficClassInterface>>();

        Set<Integer> grouped = Sets.<Integer>newConcurrentHashSet();

        // Go through the original map of traffic classes
        Iterator<Map.Entry<Integer, TrafficClassInterface>> iterator =
            clonedTrafficClasses.entrySet().iterator();

        int tcCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<Integer, TrafficClassInterface> pair = iterator.next();

            int outLogCore = pair.getKey();
            TrafficClassInterface outTc = pair.getValue();

            if (grouped.contains(outLogCore)) {
                log.debug(
                    "[{}] Traffic class {} is already grouped",
                    label(), outTc.readOperationsAsString()
                );
                continue;
            }

            log.debug("[{}] Traffic class {}", label(), outTc.readOperationsAsString());

            Set<TrafficClassInterface> group = Sets.<TrafficClassInterface>newConcurrentHashSet();

            for (Map.Entry<Integer, TrafficClassInterface> inEntry : this.trafficClasses.entrySet()) {

                int inLogCore = inEntry.getKey();
                TrafficClassInterface inTc = inEntry.getValue();

                // Skip identity
                if (inLogCore == outLogCore) {
                    continue;
                }

                // Already grouped traffic class
                if (grouped.contains(inLogCore)) {
                    continue;
                }

                log.debug("[{}] \t with traffic class {}", label(), inTc.readOperationsAsString());

                // Check whether their write operations match
                // TODO: Verify <===========================
                String outWriteOps = outTc.writeOperationsAsString();
                String inWriteOps  = inTc.writeOperationsAsString();
                if (inWriteOps.equals(outWriteOps)) {
                    log.debug("[{}] \t Matching write operations: {}", label(), inWriteOps);
                    String outReadOps = outTc.readOperationsAsString();
                    String inReadOps  = inTc.readOperationsAsString();

                    // Add this traffic class to the group
                    group.add(inTc);
                    // .. and mark it as grouped
                    grouped.add(new Integer(inLogCore));
                }
            }

            // Add also the external traffic class into the group
            group.add(outTc);

            // Remove it to avoid duplicate checks
            grouped.add(new Integer(outLogCore));
            iterator.remove();

            // Create a virtual group ID
            URI groupId = null;
            try {
                groupId = new URI(UUID.randomUUID().toString());
            } catch (URISyntaxException sEx) {
                throw new SynthesisException(
                    "Failed to create a unique group traffic class ID."
                );
            }

            // Add this group into the memory
            this.groupedTrafficClasses.put(groupId, group);

            // and pin it to a logical CPU core
            this.pinGroupTrafficClassToCore(groupId, tcCount++);
        }

        log.info(
            "[{}] {} traffic class groups are formed",
            label(), this.groupedTrafficClasses.size()
        );

        // Print the grouped traffic classes
        if (VERBOSE) {
            this.printGroupedTrafficClasses();
        }
    }

    /**
     * Goes through the set of computed traffic classes and generates
     * information related to the configuration of each traffic class.
     *
     * @param doHwOffloading flag that forces/bypasses HW offloading
     */
    private void composeTrafficClassesConfiguration(
            RestServerSBDevice server, boolean doHwOffloading) {
        checkNotNull(
            this.pathEstablisher, "Cannot compose software configuration without a path establisher");

        /**
         * Each traffic class has two ends.
         * These ends can be identical or different.
         * This means that each traffic class has a
         * maximum of 2 NICs (one per end).
         */
        Map<String, Long> inputNicsUsage  = new ConcurrentHashMap<String, Long>();
        Map<String, Long> outputNicsUsage = new ConcurrentHashMap<String, Long>();

        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            if (tc.isTotallyOffloadable() && doHwOffloading) {
                log.debug("Traffic class {} is totally offloadable", tc.toString());
                continue;
            }

            // Get the input information of this traffic class
            String inputInterface = tc.findInputInterface();
            long inputPort = this.pathEstablisher.serverInressPort();

            // Add it in the map if not already there
            if (!inputNicsUsage.containsKey(inputInterface)) {
                inputNicsUsage.put(inputInterface, inputPort);
            }

            // Get the ouput information of this traffic class
            String outputInterface = tc.findOutputInterface();
            long outputPort = this.pathEstablisher.serverEgressPort();

            // Input JSON might have virtual interface names to abstract data plan information
            // from users, but here we need the real ones.
            if (server != null) {
                String realInputInterface = server.portNameFromNumber(inputPort);
                tc.setInputInterface(realInputInterface);

                String realOutputInterface = server.portNameFromNumber(outputPort);
                tc.setOutputInterface(realOutputInterface);
            }

            // If it is identical to the input, fetch it from there
            if (outputInterface.equals(inputInterface)) {
                outputNicsUsage.put(outputInterface, inputNicsUsage.get(outputInterface));
            // Create a new entry if not already there
            } else {
                if (!outputNicsUsage.containsKey(outputInterface)) {
                    outputNicsUsage.put(outputInterface, outputPort);
                }
            }

            // Compute the input configuration
            tc.computeInputOperations(inputNicsUsage.get(inputInterface).longValue());

            // Compute the output configuration
            tc.computeOutputOperations(outputNicsUsage.get(outputInterface).longValue());
        }

        // This is how many NICs we currently use
        this.numberOfNics.set(inputNicsUsage.size());

        // Let's also find the idle NICs
        this.idleInterfaceConfiguration = this.configureIdleInterfaces(
            inputNicsUsage, outputNicsUsage
        );

        return;
    }

    /**
     * Finds the network interfaces that have Idle operations and
     * declares them properly.
     * The policy is to pair idle interfaces together
     *
     * @param map of input interfaces
     * @param map of output interfaces
     * @return idle configuration as a string
     */
    private String configureIdleInterfaces(
            Map<String, Long> inputNicsUsage, Map<String, Long> outputNicsUsage) {
        String output = "";

        for (Map.Entry<String, Long> outEntry : outputNicsUsage.entrySet()) {
            long outInterface = outEntry.getValue().longValue();

            // If it does not appear as an input, then it is a candidate
            if (!inputNicsUsage.containsValue(outInterface)) {
                // Increase the number of NICs used by this service chain
                this.numberOfNics.incrementAndGet();

                Iterator<Map.Entry<String, Long>> inIterator =
                    inputNicsUsage.entrySet().iterator();

                while (inIterator.hasNext()) {
                    long inInterface = inIterator.next().getValue().longValue();

                    // This interface does not appear as an output, we can pair them
                    if (!outputNicsUsage.containsValue(inInterface)) {
                        output += "input[" + outInterface + "] -> [" + inInterface + "]output; ";

                        // Now remove this interface to avoid duplicate declarations
                        inIterator.remove();
                    }
                }
            }
        }

        return output;
    }

    /**
     * Finds the network interfaces that have Idle operations and
     * declares them properly.
     *
     * @param map of input interfaces
     * @param map of output interfaces
     * @return idle configuration as a string
     */
    private String makeIdleInterfaces(
            Map<String, Short> inputNicsUsage, Map<String, Short> outputNicsUsage) {
        String output = "";
        for (Map.Entry<String, Short> outEntry : outputNicsUsage.entrySet()) {
            short outInterface = outEntry.getValue().shortValue();

            // If it does not appear as an input, then make it idle
            if (!inputNicsUsage.containsValue(outInterface)) {
                // Increase the number of NICs used by this service chain
                this.numberOfNics.incrementAndGet();
                output += "input[" + outInterface + "] -> Idle;";
            }
        }

        // Leave some space
        if (!output.isEmpty()) {
            output += " ";
        }

        for (Map.Entry<String, Short> inEntry : inputNicsUsage.entrySet()) {
            short inInterface = inEntry.getValue().shortValue();

            // If it does not appear as an output, then make it idle
            if (!outputNicsUsage.containsValue(inInterface)) {
                output += "Idle -> [" + inInterface + "]output;";
            }
        }

        return output;
    }

    /**
     * Prints the groups of traffic classes.
     */
    private void printGroupedTrafficClasses() {
        for (Map.Entry<URI, Set<TrafficClassInterface>> entry : this.groupedTrafficClasses.entrySet()) {
            URI groupId = entry.getKey();
            log.debug("Group: {}", groupId);

            for (TrafficClassInterface tc : entry.getValue()) {
                log.debug("\t {}", tc.id());
            }
        }
    }

    /**
     * Prints the set of computed traffic classes.
     */
    public void printTrafficClasses() {
        log.debug("############################################################");
        for (TrafficClassInterface tc : this.trafficClasses.values()) {
            log.debug("[{}] \t\t {}", label(), tc.toString());
        }
        log.debug("############################################################\n");
    }

    /**
     * Prints the software configuration of this traffic class.
     */
    private void printSoftwareConfiguration() {
        if (this.softwareConfiguration.size() == 0) {
            return;
        }

        log.info("[{}] \t Software configuration: ", label());
        for (Map.Entry<Integer, String> entry : this.softwareConfiguration.entrySet()) {
            int core = entry.getKey().intValue();
            String conf = entry.getValue();

            // Keep the printed information reasonably small
            String shortConf = "";
            if (conf.length() > 300) {
                shortConf = conf.substring(0, 300) + "...";
            } else {
                shortConf = conf;
            }

            log.info("[{}] \t\t Logical Core {} --> {}", label(), core, shortConf);
        }
    }

    /**
     * Prints the hardware configuration of this traffic class.
     */
    private void printHardwareConfiguration() {
        if (this.hardwareConfiguration.size() == 0) {
            return;
        }

        for (Map.Entry<URI, Set<FlowRule>> entry : this.hardwareConfiguration.entrySet()) {

            URI tcId = entry.getKey();
            Set<FlowRule> rules = entry.getValue();

            log.info("[{}] \t Hardware configuration of traffic class {}: ", label(), tcId);

            for (FlowRule rule : rules) {
                log.info("[{}] \t\t {}", label(), rule.toString());
            }
        }
    }

    /**
     * Returns a label with the NFV dataplane tree's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return label;
    }

}
