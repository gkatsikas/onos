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

package org.onosproject.metron.api.networkfunction;

import org.onosproject.metron.api.graphs.NetworkFunctionGraphInterface;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;
import org.onosproject.metron.api.processing.ProcessingBlockType;
import org.onosproject.metron.api.processing.TerminalStage;

import java.util.Map;
import java.util.Set;

/**
 * The interface of a network function.
 */
public interface NetworkFunctionInterface extends Comparable {

    /**
     * Returns the name of this network function.
     *
     * @return network function's name
     */
    String name();

    /**
     * Sets the name of this network function.
     *
     * @param name network function's name
     */
    void setName(String name);

    /**
     * Returns the ID of this network function.
     *
     * @return network function's id
     */
    NetworkFunctionId id();

    /**
     * Sets the ID of this network function.
     *
     * @param id network function's id
     */
    void setId(NetworkFunctionId id);

    /**
     * Returns the type of this network function.
     *
     * @return network function's type
     */
    NetworkFunctionType type();

    /**
     * Sets the type of this network function.
     *
     * @param type network function's type
     */
    void setType(NetworkFunctionType type);

    /**
     * Returns the class of this network function.
     *
     * @return network function's class
     */
    NetworkFunctionClass nfClass();

    /**
     * Sets the class of this network function.
     *
     * @param nfClass network function's class
     */
    void setNfClass(NetworkFunctionClass nfClass);

    /**
     * Returns the state of this network function.
     *
     * @return network function's state
     */
    NetworkFunctionState state();

    /**
     * Sets the state of this network function.
     *
     * @param state network function's state
     */
    void setState(NetworkFunctionState state);

    /**
     * Returns the set of (virtual) devices
     * associated with this network function.
     *
     * @return set of network function's devices
     */
    Set<NetworkFunctionDeviceInterface> devices();

    /**
     * Returns the set of (virtual) entry devices
     * associated with this network function.
     *
     * @return set of network function's entry devices
     */
    Set<NetworkFunctionDeviceInterface> entryDevices();

    /**
     * Returns the set of (virtual) exit devices
     * associated with this network function.
     *
     * @return set of network function's exit devices
     */
    Set<NetworkFunctionDeviceInterface> exitDevices();

    /**
     * Returns the (virtual) device associated with
     * the input network interface.
     *
     * @param interfName interface name
     * @return network function's device
     */
    NetworkFunctionDeviceInterface getDeviceByInterfaceName(String interfName);

    /**
     * Returns the entry (virtual) device associated with
     * the input network interface.
     *
     * @param interfName interface name
     * @return network function's entry device
     */
    NetworkFunctionDeviceInterface getEntryDeviceByInterfaceName(String interfName);

    /**
     * Returns the exit (virtual) device associated with
     * the input network interface.
     *
     * @param interfName interface name
     * @return network function's exit device
     */
    NetworkFunctionDeviceInterface getExitDeviceByInterfaceName(String interfName);

    /**
     * Adds one device to the network function.
     *
     * @param device network function's device
     */
    void addDevice(NetworkFunctionDeviceInterface device);

    /**
     * Adds one entry device to the network function.
     *
     * @param device network function's entry device
     */
    void addEntryDevice(NetworkFunctionDeviceInterface device);

    /**
     * Adds one exit device to the network function.
     *
     * @param device network function's exit device
     */
    void addExitDevice(NetworkFunctionDeviceInterface device);

    /**
     * Sets the network function's devices.
     *
     * @param devices set of devices
     */
    void setDevices(Set<NetworkFunctionDeviceInterface> devices);

    /**
     * Returns the processing graph of this network function.
     *
     * @return network function's processing graph
     */
    NetworkFunctionGraphInterface networkFunctionGraph();

    /**
     * Sets the processing graph of this network function.
     *
     * @param nfGraph network function's processing graph
     */
    void setNetworkFunctionGraph(NetworkFunctionGraphInterface nfGraph);

    /**
     * Returns the peer entities of this network function
     * mapped to the NF's devices.
     *
     * @return a map between the devices of this NF and peering NFs
     */
    Map<NetworkFunctionDeviceInterface, NetworkFunctionInterface> peerNFs();

    /**
     * Returns the peer NF mapped to the input device of this NF.
     *
     * @param devIface network function's device
     * @return peering NF mapped to the input device
     */
    NetworkFunctionInterface peersWithNF(NetworkFunctionDeviceInterface devIface);

    /**
     * Adds a new peer NF for this NF.
     *
     * @param devIface network function's device
     * @param nf peer network function or NULL (if it is an end point)
     */
    void addPeerNF(NetworkFunctionDeviceInterface devIface, NetworkFunctionInterface nf);

    /**
     * Returns a map of peer network interfaces mapped to the devices of this NF.
     *
     * @return a map between the devices of this NF and peering NF interfaces
     */
    Map<NetworkFunctionDeviceInterface, String> peerInterfaces();

    /**
     * Returns the peer interface mapped to the input device of ths NF.
     *
     * @param devIface network function's device
     * @return peering NF interface  mapped to the input device
     */
    String peersWithInterface(NetworkFunctionDeviceInterface devIface);

    /**
     * Adds a new peer interface for this NF.
     *
     * @param devIface network function's device
     * @param iface peer network function interface or NULL (if it is an end point)
     */
    void addPeerInterface(NetworkFunctionDeviceInterface devIface, String iface);

    /**
     * Returns whether this NF is an end point or not.
     * Such an NF contains a network interface that is connected to the outside world
     * (i.e., a traffic src/sink).
     *
     * @return boolean end point status
     */
    boolean isEndPoint();

    /**
     * Returns whether this NF is an entry point or not.
     * Such an NF contains an input (i.e., read) network interface.
     *
     * @return boolean entry point status
     */
    boolean isEntryPoint();

    /**
     * Returns whether this NF is an exit point or not.
     * Such an NF contains an output (i.e., write) network interface.
     *
     * @return boolean exit point status
     */
    boolean isExitPoint();

    /**
     * Returns a set of packet processing nodes
     * (i.e., vertices in the packet processing graph),
     * which act as traffic end points.
     *
     * E.g., NF1 -> NF2 is an example service chain.
     *       NF1: FromDevice -> Classifier -> ToDevice
     *       NF2: FromDevice -> Counter -> ToDevice
     *
     * In this case, FromDevice is an end point of NF1
     * and ToDevice is an end point of NF2, as they are
     * both connected to the outside world.
     *
     * @return set of end point processing nodes of this NF
     */
    Set<NetworkFunctionVertexInterface> endPointVertices();

    /**
     * Returns a set of packet processing nodes,
     * which act as traffic entry ppoints.
     *
     * E.g., NF1 -> NF2 is an example service chain.
     *       NF1: FromDevice -> Classifier -> ToDevice
     *       NF2: FromDevice -> Counter -> ToDevice
     *
     * In this case, FromDevice is an entry point of NF1.
     *
     * @return set of entry point processing nodes of this NF
     */
    Set<NetworkFunctionVertexInterface> entryPointVertices();

    /**
     * Returns a set of packet processing nodes,
     * which act as traffic exit ppoints.
     *
     * E.g., NF1 -> NF2 is an example service chain.
     *       NF1: FromDevice -> Classifier -> ToDevice
     *       NF2: FromDevice -> Counter -> ToDevice
     *
     * In this case, ToDevice is an exit point of NF2.
     *
     * @return set of exit point processing nodes of this NF
     */
    Set<NetworkFunctionVertexInterface> exitPointVertices();

    /**
     * Returns the internal packet processing blocks
     * of the end point vertices above.
     *
     * @return set of end point packet processing blocks of this NF
     */
    Set<ProcessingBlockInterface> endPointProcessingBlocks();

    /**
     * Returns the internal packet processing blocks
     * of the end point vertices that are entry points.
     * An entry point is an end point that acts as a packet reader.
     * E.g., FromDevice and FromDpdkDevice can act as entry points.
     *
     * @return set of end point packet processing blocks that act as entry points
     */
    Set<ProcessingBlockInterface> entryPointProcessingBlocks();

    /**
     * Returns the internal packet processing blocks
     * of the end point vertices that are exit points.
     * An exit point is an end point that acts as a packet writer.
     * E.g., ToDevice, ToDpdkDevice, and Discard can act as exit points.
     *
     * @return set of end point packet processing blocks that act as exit points
     */
    Set<ProcessingBlockInterface> exitPointProcessingBlocks();

    /**
     * Returns a set of packet processing nodes,
     * whose processing blocks are of type blockType.
     *
     * @param blockType type of packet processing block
     * @return set of processing nodes of type
     */
    Set<NetworkFunctionVertexInterface> getVerticesByType(ProcessingBlockType blockType);

    /**
     * Returns a set of terminal packet processing nodes,
     * whose stage matches the input stage:
     * If stage = INPUT, a set of From(Dpdk)Device elements is returned.
     * If stage = OUTPUT, a set of To(Dpdk)Device elements is returned.
     * If stage = DROP, a set of Discard elements is returned.
     *
     * @param stage of a terminal packet processing block
     * @return set of processing nodes of type
     */
    Set<NetworkFunctionVertexInterface> getTerminalVerticesByStage(TerminalStage stage);

    /**
     * Returns a set of terminal packet processing nodes,
     * whose stage matches any of the input stages.
     *
     * @param stages of a terminal packet processing block
     * @return set of processing nodes of type
     */
    Set<NetworkFunctionVertexInterface> getTerminalVerticesByStagesGroup(Set<TerminalStage> stages);

}
