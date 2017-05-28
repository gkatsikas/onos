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

package org.onosproject.metron.impl.networkfunction;

import org.onosproject.metron.api.exceptions.NetworkFunctionException;
import org.onosproject.metron.api.networkfunction.NetworkFunctionId;
import org.onosproject.metron.api.networkfunction.NetworkFunctionClass;
import org.onosproject.metron.api.networkfunction.NetworkFunctionType;
import org.onosproject.metron.api.networkfunction.NetworkFunctionState;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.networkfunction.NetworkFunctionDeviceInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;
import org.onosproject.metron.api.processing.ProcessingBlockType;
import org.onosproject.metron.api.processing.TerminalStage;

import org.onosproject.metron.impl.processing.Blocks;
import org.onosproject.metron.impl.processing.TerminalBlock;
import org.onosproject.metron.impl.processing.blocks.Device;

import com.google.common.base.Strings;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ArrayUtils;

import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of a Metron Network Function.
 */
public class NetworkFunction implements NetworkFunctionInterface, Comparable {

    private static final Logger log = getLogger(NetworkFunction.class);

    /**
     * The name of this NF.
     */
    private String name;

    /**
     * A unique NF indentifier.
     */
    private NetworkFunctionId id;

    /**
     * The type of this NF.
     */
    private NetworkFunctionType type;

    /**
     * The class of this NF.
     */
    private NetworkFunctionClass nfClass;

    /**
     * The NF state.
     */
    private NetworkFunctionState state;

    /**
     * Set of (virtual) devices used by the NF.
     */
    private Set<NetworkFunctionDeviceInterface> devices;

    /**
     * Out of all the devices, this set contains the entry point ones
     * (i.e., those that are connected with traffic sources).
     */
    private Set<NetworkFunctionDeviceInterface> entryDevices;

    /**
     * Out of all the devices, this set contains the exit point ones
     * (i.e., those that are connected with traffic sinks).
     */
    private Set<NetworkFunctionDeviceInterface> exitDevices;

    /**
     * A graph of packet processing elements that specify
     * the logic of this NF.
     */
    private NetworkFunctionGraphInterface networkFunctionGraph;

    /**
     * Each interface of this NF is associated with a peer entity.
     * This entity can be either another NF or a traffic end point.
     * In the latter case, the value is null.
     */
    private Map<NetworkFunctionDeviceInterface, NetworkFunctionInterface> peerNFs;
    private Map<NetworkFunctionDeviceInterface, String> peerInterfaces;

    private NetworkFunction(String name) {
        checkArgument(
            !Strings.isNullOrEmpty(name),
            "Please specify a name for this network function"
        );
        this.name    = name;
        this.id      = null;
        this.type    = null;
        this.nfClass = null;
        this.state   = null;

        this.devices      = Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();
        this.entryDevices = Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();
        this.exitDevices  = Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();

        this.networkFunctionGraph = null;
        this.peerNFs = new ConcurrentHashMap<NetworkFunctionDeviceInterface, NetworkFunctionInterface>();
        this.peerInterfaces = new ConcurrentHashMap<NetworkFunctionDeviceInterface, String>();
    }

    protected NetworkFunction(
            String name,
            NetworkFunctionId id,
            NetworkFunctionType  type,
            NetworkFunctionClass nfClass,
            NetworkFunctionState state,
            NetworkFunctionGraphInterface networkFunctionGraph) {
        // Sanity checks
        checkArgument(
            !Strings.isNullOrEmpty(name),
            "Please specify a name for this network function"
        );
        checkArgument(
            !Strings.isNullOrEmpty(id.toString()),
            "Please specify an ID for this network function. Ensure that this ID is unique."
        );
        checkNotNull(
            type,
            "Incompatible network function type."
        );
        if (!NetworkFunctionType.isValid(type)) {
            throw new IllegalArgumentException("Invalid network function type: " + String.valueOf(type));
        }
        checkNotNull(
            nfClass,
            "Incompatible network function class."
        );
        checkNotNull(
            state
        );
        if (!ArrayUtils.contains(NetworkFunctionState.values(), state)) {
            throw new IllegalArgumentException("Invalid network function state: " + String.valueOf(state));
        }
        checkArgument(
            (networkFunctionGraph != null) && !networkFunctionGraph.isEmpty(),
            "NULL or empty NF graph."
        );

        this.name    = name;
        this.id      = id;
        this.type    = type;
        this.nfClass = nfClass;
        this.state   = state;

        this.devices      = Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();
        this.entryDevices = Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();
        this.exitDevices  = Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();

        this.networkFunctionGraph = null;
        this.networkFunctionGraph = networkFunctionGraph;
        this.peerNFs = new ConcurrentHashMap<NetworkFunctionDeviceInterface, NetworkFunctionInterface>();
        this.peerInterfaces = new ConcurrentHashMap<NetworkFunctionDeviceInterface, String>();
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public NetworkFunctionId id() {
        return this.id;
    }

    @Override
    public void setId(NetworkFunctionId id) {
        this.id = id;
    }

    @Override
    public NetworkFunctionType type() {
        return this.type;
    }

    @Override
    public NetworkFunctionClass nfClass() {
        return this.nfClass;
    }

    @Override
    public void setType(NetworkFunctionType type) {
        this.type = type;
    }

    @Override
    public void setNfClass(NetworkFunctionClass nfClass) {
        this.nfClass = nfClass;
    }

    @Override
    public NetworkFunctionState state() {
        return this.state;
    }

    @Override
    public void setState(NetworkFunctionState state) {
        this.state = state;
    }

    @Override
    public Set<NetworkFunctionDeviceInterface> devices() {
        return this.devices;
    }

    @Override
    public Set<NetworkFunctionDeviceInterface> entryDevices() {
        return this.entryDevices;
    }

    @Override
    public Set<NetworkFunctionDeviceInterface> exitDevices() {
        return this.exitDevices;
    }

    @Override
    public NetworkFunctionDeviceInterface getDeviceByInterfaceName(String interfName) {
        for (NetworkFunctionDeviceInterface dev : this.devices) {
            if (dev.name().equals(interfName)) {
                return dev;
            }
        }

        return null;
    }

    @Override
    public NetworkFunctionDeviceInterface getEntryDeviceByInterfaceName(String interfName) {
        for (NetworkFunctionDeviceInterface dev : this.entryDevices) {
            if (dev.name().equals(interfName)) {
                return dev;
            }
        }

        return null;
    }

    @Override
    public NetworkFunctionDeviceInterface getExitDeviceByInterfaceName(String interfName) {
        for (NetworkFunctionDeviceInterface dev : this.exitDevices) {
            if (dev.name().equals(interfName)) {
                return dev;
            }
        }

        return null;
    }

    @Override
    public void addDevice(NetworkFunctionDeviceInterface device) {
        if (device == null) {
            return;
        }
        this.devices.add(device);
    }

    @Override
    public void addEntryDevice(NetworkFunctionDeviceInterface device) {
        if (device == null) {
            return;
        }
        this.entryDevices.add(device);
    }

    @Override
    public void addExitDevice(NetworkFunctionDeviceInterface device) {
        if (device == null) {
            return;
        }
        this.exitDevices.add(device);
    }

    @Override
    public void setDevices(Set<NetworkFunctionDeviceInterface> devices) {
        if (devices == null) {
            return;
        }
        this.devices = devices;
    }

    @Override
    public NetworkFunctionGraphInterface networkFunctionGraph() {
        return this.networkFunctionGraph;
    }

    @Override
    public void setNetworkFunctionGraph(NetworkFunctionGraphInterface nfGraph) {
        this.networkFunctionGraph = nfGraph;
    }

    @Override
    public Map<NetworkFunctionDeviceInterface, NetworkFunctionInterface> peerNFs() {
        return this.peerNFs;
    }

    @Override
    public NetworkFunctionInterface peersWithNF(NetworkFunctionDeviceInterface devIface) {
        checkNotNull(
            devIface,
            "Network function's device is NULL"
        );

        return this.peerNFs.get(devIface);
    }

    @Override
    public void addPeerNF(NetworkFunctionDeviceInterface devIface, NetworkFunctionInterface nf) {
        checkNotNull(
            devIface,
            "Network function's device is NULL"
        );

        this.peerNFs.put(devIface, nf);
    }

    @Override
    public Map<NetworkFunctionDeviceInterface, String> peerInterfaces() {
        return this.peerInterfaces;
    }

    @Override
    public String peersWithInterface(NetworkFunctionDeviceInterface devIface) {
        checkNotNull(
            devIface,
            "Network function's device is NULL"
        );

        return this.peerInterfaces.get(devIface);
    }

    @Override
    public void addPeerInterface(NetworkFunctionDeviceInterface devIface, String iface) {
        checkNotNull(
            devIface,
            "Network function's device is NULL"
        );

        this.peerInterfaces.put(devIface, iface);
    }

    @Override
    public boolean isEndPoint() {
        // Iterate through the list of devices
        for (NetworkFunctionDeviceInterface dev : this.devices()) {
            // Fetch the peering NF of this device (if any)
            NetworkFunctionInterface peer = this.peersWithNF(dev);

            // This is an end point
            if (peer == null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEntryPoint() {
        // First, it has to be an end point
        if (!this.isEndPoint()) {
            return false;
        }

        // If it has at least one entry point vertex, then it is entry point
        return !(this.entryPointVertices() == null);
    }

    public boolean isExitPoint() {
        // First, it has to be an end point
        if (!this.isEndPoint()) {
            return false;
        }

        // If it has at least one exit point vertex, then it is exit point
        return !(this.exitPointVertices() == null);
    }

    @Override
    public Set<NetworkFunctionVertexInterface> endPointVertices() {
        if (!this.isEndPoint()) {
            return null;
        }

        // Get the processing graph of this NF
        NetworkFunctionGraphInterface nfGraph = this.networkFunctionGraph();
        if (nfGraph == null) {
            return null;
        }

        // Here we put the end point vertices
        Set<NetworkFunctionVertexInterface> endPoints =
            Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();

        boolean atLeastOne = false;

        // For each processing block of this graph
        for (NetworkFunctionVertexInterface nfVer : nfGraph.getVertexes()) {
            ProcessingBlockInterface pb = nfVer.processingBlock();
            if (pb == null) {
                continue;
            }

            /**
             * Only terminal blocks have a chance to be end points.
             */
            if (pb.processingBlockType() == ProcessingBlockType.BLOCK_TYPE_TERMINAL) {

                String devName = "";

                // Discard elements are always end points
                if (Blocks.DROP_ELEMENTS.contains(pb.getClass())) {
                    endPoints.add(nfVer);
                // Get the name of this terminal device
                } else if (Blocks.DEVICE_ELEMENTS.contains(pb.getClass())) {
                    Device d = (Device) pb;
                    devName = d.devName();
                }

                if (devName.isEmpty()) {
                    continue;
                }

                NetworkFunctionDeviceInterface targetDev = null;

                // Search in the devices memory to find a device object with this name
                for (NetworkFunctionDeviceInterface dev : this.devices()) {
                    if (devName.equals(dev.name())) {
                        targetDev = dev;
                        break;
                    }
                }

                if (targetDev != null) {
                    // Fetch the peer NF of this device
                    NetworkFunctionInterface peer = this.peersWithNF(targetDev);

                    // If no peer exists, this is an end point
                    if (peer == null) {
                        endPoints.add(nfVer);
                        atLeastOne = true;
                    }
                }
            }
        }

        /**
         * Since this NF is an end point, we must find at least one terminal
         * processing block with the correct interface name. If not, it means
         * that there is a disagreement between the application elements and
         * their configuration (input file).
         */
        if (!atLeastOne) {
            throw new NetworkFunctionException(
                "The network interfaces specified in the configuration file " +
                "do not agree with the ones specified by the application"
            );
        }

        return endPoints;
    }

    @Override
    public Set<NetworkFunctionVertexInterface> entryPointVertices() {
        // Retrieve the end point vertices
        Set<NetworkFunctionVertexInterface> endVertices = this.endPointVertices();
        if (endVertices == null) {
            return null;
        }

        Set<NetworkFunctionVertexInterface> entryVertices =
            Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();

        // Filter those that act as entry points
        for (NetworkFunctionVertexInterface endVertex : endVertices) {
            ProcessingBlockInterface pb = endVertex.processingBlock();
            if (pb == null) {
                continue;
            }

            /**
             * Only Read terminal elements can be entry points.
             */
            if  (Blocks.INPUT_ELEMENTS.contains(pb.getClass())) {
                entryVertices.add(endVertex);
            }
        }

        if (entryVertices.isEmpty()) {
            return null;
        }

        return entryVertices;
    }

    @Override
    public Set<NetworkFunctionVertexInterface> exitPointVertices() {
        // Retrieve the end point vertices
        Set<NetworkFunctionVertexInterface> endVertices = this.endPointVertices();
        if (endVertices == null) {
            return null;
        }

        Set<NetworkFunctionVertexInterface> exitVertices =
            Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();

        // Filter those that act as entry points
        for (NetworkFunctionVertexInterface endVertex : endVertices) {
            ProcessingBlockInterface pb = endVertex.processingBlock();
            if (pb == null) {
                continue;
            }

            /**
             * Only Write or Discard terminal elements can be exit points.
             */
            if  (Blocks.OUTPUT_ELEMENTS.contains(pb.getClass()) ||
                 Blocks.DROP_ELEMENTS.contains(pb.getClass())) {
                exitVertices.add(endVertex);
            }
        }

        if (exitVertices.isEmpty()) {
            return null;
        }

        return exitVertices;
    }

    @Override
    public Set<ProcessingBlockInterface> endPointProcessingBlocks() {
        // Retrieve the end point vertices
        Set<NetworkFunctionVertexInterface> endVertices = this.endPointVertices();
        if (endVertices == null) {
            return null;
        }

        Set<ProcessingBlockInterface> endPointBlocks =
            Sets.<ProcessingBlockInterface>newConcurrentHashSet();

        // Extract their packet processing blocks
        for (NetworkFunctionVertexInterface endVertex : endVertices) {
            endPointBlocks.add(endVertex.processingBlock());
        }

        return endPointBlocks;
    }

    @Override
    public Set<ProcessingBlockInterface> entryPointProcessingBlocks() {
        // Retrieve the entry point vertices
        Set<NetworkFunctionVertexInterface> entryVertices = this.entryPointVertices();
        if (entryVertices == null) {
            return null;
        }

        Set<ProcessingBlockInterface> entryPointBlocks =
            Sets.<ProcessingBlockInterface>newConcurrentHashSet();

        // Extract their packet processing blocks
        for (NetworkFunctionVertexInterface entryVertex : entryVertices) {
            entryPointBlocks.add(entryVertex.processingBlock());
        }

        return entryPointBlocks;
    }

    @Override
    public Set<ProcessingBlockInterface> exitPointProcessingBlocks() {
        // Retrieve the exit point vertices
        Set<NetworkFunctionVertexInterface> exitVertices = this.exitPointVertices();
        if (exitVertices == null) {
            return null;
        }

        Set<ProcessingBlockInterface> exitPointBlocks =
            Sets.<ProcessingBlockInterface>newConcurrentHashSet();

        // Extract their packet processing blocks
        for (NetworkFunctionVertexInterface exitVertex : exitVertices) {
            exitPointBlocks.add(exitVertex.processingBlock());
        }

        return exitPointBlocks;
    }

    @Override
    public Set<NetworkFunctionVertexInterface> getVerticesByType(ProcessingBlockType blockType) {
        // Get the processing graph of this NF
        NetworkFunctionGraphInterface nfGraph = this.networkFunctionGraph();
        if (nfGraph == null) {
            return null;
        }

        // Here we put the desired vertices
        Set<NetworkFunctionVertexInterface> targetVertices =
            Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();

        // For each processing block of this graph
        for (NetworkFunctionVertexInterface nfVer : nfGraph.getVertexes()) {
            ProcessingBlockInterface pb = nfVer.processingBlock();
            if (pb == null) {
                continue;
            }

            // We found it
            if (pb.processingBlockType() == blockType) {
                targetVertices.add(nfVer);
            }
        }

        if (targetVertices.isEmpty()) {
            return null;
        }

        return targetVertices;
    }

    @Override
    public Set<NetworkFunctionVertexInterface> getTerminalVerticesByStage(TerminalStage stage) {
        Set<NetworkFunctionVertexInterface> terminalVertices =
            this.getVerticesByType(ProcessingBlockType.BLOCK_TYPE_TERMINAL);

        // Here we put the desired vertices
        Set<NetworkFunctionVertexInterface> targetVertices =
            Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();

        for (NetworkFunctionVertexInterface nfVer : terminalVertices) {
            ProcessingBlockInterface pb = nfVer.processingBlock();
            if (pb == null) {
                continue;
            }

            // Turn this block into a terminal block
            TerminalBlock terminalBlock = (TerminalBlock) pb;

            if (terminalBlock.stage() == stage) {
                targetVertices.add(nfVer);
            }
        }

        if (targetVertices.isEmpty()) {
            return null;
        }

        return targetVertices;
    }

    @Override
    public Set<NetworkFunctionVertexInterface> getTerminalVerticesByStagesGroup(Set<TerminalStage> stages) {
        checkNotNull(stages, "Input list of stages is NULL");

        // Here we put the desired vertices
        Set<NetworkFunctionVertexInterface> targetVertices =
            Sets.<NetworkFunctionVertexInterface>newConcurrentHashSet();

        for (TerminalStage stage : stages) {
            Set<NetworkFunctionVertexInterface> retrievedStages = this.getTerminalVerticesByStage(stage);

            if (retrievedStages != null) {
                targetVertices.addAll(retrievedStages);
            }
        }

        if (targetVertices.isEmpty()) {
            return null;
        }

        return targetVertices;
    }

    /**
     * Returns a label with the module's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return "Network Function";
    }

    /**
     * Compares two NF IDs lexicographically.
     *
     * @param other a network function to compare against this one
     * @return comparison status
     */
    @Override
    public int compareTo(Object other) {
        if (this == other) {
            return 0;
        }

        if (other == null) {
            return -1;
        }

        if (other instanceof NetworkFunction) {
            NetworkFunction otherNf = (NetworkFunction) other;

            // Extract the digits out of the ID
            String thisStr  = this.id().toString().replaceAll("\\D+", "");
            String otherStr = otherNf.id().toString().replaceAll("\\D+", "");

            // Fall back to the default string compare if no digits are present
            if (thisStr.isEmpty() || otherStr.isEmpty()) {
                return this.id().toString().compareToIgnoreCase(
                    otherNf.id().toString()
                );
            // Sort based upon the digits (such that NF1 < NF10)
            } else {
                Integer thisNum  = Integer.parseInt(thisStr);
                Integer otherNum = Integer.parseInt(otherStr);
                return thisNum.compareTo(otherNum);
            }
        }

        return -1;
    }

    /**
     * Compares two network functions.
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NetworkFunction) {
            NetworkFunction that = (NetworkFunction) obj;
            if (Objects.equals(this.name, that.name) &&
                Objects.equals(this.id,   that.id) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.nfClass, that.nfClass) &&
                Objects.equals(this.devices, that.devices) &&
                Objects.equals(this.peerNFs, that.peerNFs) &&
                Objects.equals(
                    this.networkFunctionGraph, that.networkFunctionGraph)
                ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.id, this.type, this.nfClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name",  name)
                .add("id",    id.toString())
                .add("type",  type)
                .add("class", nfClass)
                .add("state", state.name())
                .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return network function builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of network function entities.
     */
    public static final class Builder {
        private String name;
        private NetworkFunctionId id;
        private NetworkFunctionType  type;
        private NetworkFunctionClass nfClass;
        private NetworkFunctionState state = NetworkFunctionState.INIT;
        private NetworkFunctionGraphInterface networkFunctionGraph = null;
        private Set<NetworkFunctionDeviceInterface> devices =
            Sets.<NetworkFunctionDeviceInterface>newConcurrentHashSet();
        private Map<NetworkFunctionDeviceInterface, NetworkFunctionInterface> peerNFs =
            new ConcurrentHashMap<NetworkFunctionDeviceInterface, NetworkFunctionInterface>();
        private Map<NetworkFunctionDeviceInterface, String> peerInterfaces =
            new ConcurrentHashMap<NetworkFunctionDeviceInterface, String>();

        private Builder() {
        }

        public NetworkFunction build() {
            checkArgument(!Strings.isNullOrEmpty(name));
            checkArgument(!Strings.isNullOrEmpty(id.toString()));
            checkNotNull(type);
            checkNotNull(nfClass);
            checkNotNull(networkFunctionGraph);
            checkNotNull(devices);
            checkNotNull(peerNFs);
            checkNotNull(peerInterfaces);

            return new NetworkFunction(
                name, id, type, nfClass, state, networkFunctionGraph
            );
        }

        /**
         * Returns network function builder with the name.
         *
         * @param name name
         * @return network function builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Returns network function builder with the ID.
         *
         * @param id network function ID
         * @return network function builder
         */
        public Builder id(String id) {
            this.id = (NetworkFunctionId) NetworkFunctionId.id(id);
            return this;
        }

        /**
         * Returns network function builder with the type.
         *
         * @param type network function type
         * @return network function builder
         */
        public Builder type(NetworkFunctionType type) {
            this.type = type;
            return this;
        }

        /**
         * Returns network function builder with the class.
         *
         * @param nfClass network function class
         * @return network function builder
         */
        public Builder nfClass(NetworkFunctionClass nfClass) {
            this.nfClass = nfClass;
            return this;
        }

        /**
         * Returns network function builder with the devices.
         *
         * @param devices network function's devices
         * @return network function builder
         */
        public Builder devices(Set<NetworkFunctionDeviceInterface> devices) {
            this.devices = devices;
            return this;
        }

        /**
         * Returns network function builder with the processing graph.
         *
         * @param nfGraph network function's processing graph
         * @return network function builder
         */
        public Builder networkFunctionGraph(NetworkFunctionGraphInterface nfGraph) {
            this.networkFunctionGraph = nfGraph;
            return this;
        }
    }

    public static class NfNameComparator implements Comparator<String> {
        Map<String, NetworkFunctionInterface> base;

        public NfNameComparator(Map<String, NetworkFunctionInterface> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with
        // equals.
        public int compare(String a, String b) {
            // Extract only the digits out of the ID
            String aStr = a.replaceAll("\\D+", "");
            String bStr = b.replaceAll("\\D+", "");

            // Fall back to the default string compare if no digits are present
            if (aStr.isEmpty() || bStr.isEmpty()) {
                return a.compareToIgnoreCase(b);
            } else {
                Integer aNum = Integer.parseInt(aStr);
                Integer bNum = Integer.parseInt(bStr);
                return aNum.compareTo(bNum);
            }
        }
    }

}