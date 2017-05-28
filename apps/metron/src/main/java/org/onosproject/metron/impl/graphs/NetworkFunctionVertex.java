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

package org.onosproject.metron.impl.graphs;

import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.graphs.NetworkFunctionVertexInterface;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of a network function's vertex (i.e., a processing block).
 */
public class NetworkFunctionVertex implements NetworkFunctionVertexInterface {

    private int inputPortsNumber;
    private int outputPortsNumber;

    /**
     * The driving force of a network function.
     */
    private ProcessingBlockInterface processingBlock = null;

    /**
     * Each input/output port of this block leads to previous/subsequent blocks.
     */
    private Map<Integer, ProcessingBlockInterface> inputPortToBlock;
    private Map<Integer, ProcessingBlockInterface> outputPortToBlock;

    /**
     * Creates a vertex for a network function.
     *
     * @param processingBlock the pillar of this vertex
     */
    public NetworkFunctionVertex(ProcessingBlockInterface processingBlock) {
        this.inputPortsNumber  = 0;
        this.outputPortsNumber = 0;

        this.processingBlock   = processingBlock;

        this.inputPortToBlock  = new ConcurrentHashMap<Integer, ProcessingBlockInterface>();
        this.outputPortToBlock = new ConcurrentHashMap<Integer, ProcessingBlockInterface>();
    }

    /**
     * When we clone an NF vertex from an existing one,
     * we want the input part of this vertex to be clean,
     * as we will connect it to our existing pipeline again.
     * On the other hand, the output part has to reamin because
     * we do not want to lose the bindings with the rest of the pipeline.
     *
     * @param other the NF vertex to be cloned
     */
    public NetworkFunctionVertex(NetworkFunctionVertexInterface other) {
        checkNotNull(
            other,
            "Cannot construct an NF vertex out of a NULL one"
        );

        // Clean input part
        this.inputPortsNumber  = 0;
        this.inputPortToBlock  = new ConcurrentHashMap<Integer, ProcessingBlockInterface>();

        // Inherited output part
        this.outputPortsNumber = other.outputPortsNumber();
        this.outputPortToBlock = new ConcurrentHashMap<Integer, ProcessingBlockInterface>(
            other.outputPortToBlockMap()
        );

        this.processingBlock   = other.processingBlock().clone();
    }

    @Override
    public int inputPortsNumber() {
        return this.inputPortsNumber;
    }

    @Override
    public int outputPortsNumber() {
        return this.outputPortsNumber;
    }

    @Override
    public ProcessingBlockInterface processingBlock() {
        return this.processingBlock;
    }

    @Override
    public void setProcessingBlock(ProcessingBlockInterface pb) {
        this.processingBlock = pb;
    }

    @Override
    public Map<Integer, ProcessingBlockInterface> inputPortToBlockMap() {
        return this.inputPortToBlock;
    }

    @Override
    public void addInputBlock(int inputPort, ProcessingBlockInterface pb) {
        checkArgument(
            (inputPort >= 0) && (inputPort <= Constants.MAX_PIPELINE_PORT_NB),
            "Invalid input port for processing block " + this.processingBlock.id()
        );

        checkNotNull(
            pb,
            "Invalid preceding block for processing block " + this.processingBlock.id()
        );

        this.inputPortToBlock.put(new Integer(inputPort), pb);
        this.inputPortsNumber++;
    }

    @Override
    public ProcessingBlockInterface getInputBlockFromPort(int inputPort) {
        checkArgument(
            (inputPort >= 0) && (inputPort <= Constants.MAX_PIPELINE_PORT_NB),
            "Invalid input port for processing block " + this.processingBlock.id()
        );

        return this.inputPortToBlock.get(new Integer(inputPort));
    }

    @Override
    public Map<Integer, ProcessingBlockInterface> outputPortToBlockMap() {
        return this.outputPortToBlock;
    }

    @Override
    public void addOutputBlock(int outputPort, ProcessingBlockInterface pb) {
        checkArgument(
            (outputPort >= 0) && (outputPort <= Constants.MAX_PIPELINE_PORT_NB),
            "Invalid output port for processing block " + this.processingBlock.id()
        );

        checkNotNull(
            pb,
            "Invalid next block for processing block " + this.processingBlock.id()
        );

        this.outputPortToBlock.put(new Integer(outputPort), pb);
        this.outputPortsNumber++;
    }

    @Override
    public ProcessingBlockInterface getOutputBlockFromPort(int outputPort) {
        checkArgument(
            (outputPort >= 0) && (outputPort <= Constants.MAX_PIPELINE_PORT_NB),
            "Invalid output port for processing block " + this.processingBlock.id()
        );

        return this.outputPortToBlock.get(new Integer(outputPort));
    }

    @Override
    public NetworkFunctionVertexInterface clone() {
        return new NetworkFunctionVertex(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.inputPortsNumber,
            this.outputPortsNumber,
            this.processingBlock,
            this.inputPortToBlock,
            this.outputPortToBlock
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetworkFunctionVertex) {
            final NetworkFunctionVertex other = (NetworkFunctionVertex) obj;

            if (Objects.equals(this.inputPortsNumber,  other.inputPortsNumber)  &&
                Objects.equals(this.outputPortsNumber, other.outputPortsNumber) &&
                Objects.equals(this.processingBlock,   other.processingBlock)   &&
                Objects.equals(this.inputPortToBlock,  other.inputPortToBlock)  &&
                Objects.equals(this.outputPortToBlock, other.outputPortToBlock)
                ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.processingBlock.toString();
    }

}
