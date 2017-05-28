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

package org.onosproject.metron.api.graphs;

import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import org.onlab.graph.Vertex;

import java.util.Map;

/**
 * Represents a vertex in a network function.
 * Such a vertex in a packet processing block.
 */
public interface NetworkFunctionVertexInterface extends Vertex {

    /**
     * Returns the number of input ports of the
     * processing block associated with the
     * network function.
     *
     * @return processing block's input ports number
     */
    int inputPortsNumber();

    /**
     * Returns the number of output ports of the
     * processing block associated with the
     * network function.
     *
     * @return processing block's output ports number
     */
    int outputPortsNumber();

    /**
     * Returns the processing block
     * associated with the network function.
     *
     * @return processing block
     */
    ProcessingBlockInterface processingBlock();

    /**
     * Associates a processing block with a network function.
     *
     * @param pb processing block
     */
    void setProcessingBlock(ProcessingBlockInterface pb);

    /**
     * Returns the map of input ports associated with processing blocks.
     *
     * @return map of input ports associated with processing blocks
     */
    Map<Integer, ProcessingBlockInterface> inputPortToBlockMap();

    /**
     * Associates this processing block with an input port
     * and the block that precedes.
     *
     * @param inputPort of this processing block
     * @param pb previous processing block
     */
    void addInputBlock(int inputPort, ProcessingBlockInterface pb);

    /**
     * Returns the processing block connected to this input port.
     *
     * @param inputPort of this processing block
     * @return ProcessingBlockInterface previous processing block
     */
    ProcessingBlockInterface getInputBlockFromPort(int inputPort);

    /**
     * Returns the map of output ports associated with processing blocks.
     *
     * @return map of output ports associated with processing blocks
     */
    Map<Integer, ProcessingBlockInterface> outputPortToBlockMap();

    /**
     * Associates this processing block with an output port
     * and the block that follows.
     *
     * @param outputPort of this processing block
     * @param pb next processing block
     */
    void addOutputBlock(int outputPort, ProcessingBlockInterface pb);

    /**
     * Returns the processing block connected to this output port.
     *
     * @param outputPort of this processing block
     * @return ProcessingBlockInterface next processing block
     */
    ProcessingBlockInterface getOutputBlockFromPort(int outputPort);

    /**
     * Replicates this NF vertex.
     *
     * @return NetworkFunctionVertexInterface replica
     */
    NetworkFunctionVertexInterface clone();

}
