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

package org.onosproject.metron.api.dataplane;

import org.onosproject.metron.api.classification.trafficclass.outputclass.OutputClass;
import org.onosproject.metron.api.networkfunction.NetworkFunctionInterface;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import java.util.List;

/**
 * Interface that represents an NFV dataplane block.
 */
public interface NfvDataplaneBlockInterface {

    /**
     * Returns the network function that owns this dataplane block.
     *
     * @return network function that owns this dataplane block
     */
    public NetworkFunctionInterface networkFunction();

    /**
     * Sets the network function that owns this dataplane block.
     *
     * @param nf network function that owns this dataplane block
     */
    public void setNetworkFunction(NetworkFunctionInterface nf);

    /**
     * Returns the processing element of this NFV block.
     *
     * @return NFV processing block
     */
    public ProcessingBlockInterface processor();

    /**
     * Sets the processing element of this NFV block.
     *
     * @param processor the NFV processing block
     */
    public void setProcessor(ProcessingBlockInterface processor);

    /**
     * Returns the name of this NFV block.
     *
     * @return NFV processing block's name
     */
    public String name();

    /**
     * Returns the class of this NFV block.
     *
     * @return NFV processing block's class
     */
    public ProcessingBlockClass blockClass();

    /**
     * Returns the number of ports of this NFV block.
     *
     * @return NFV processing block's ports' number
     */
    public int portsNumber();

    /**
     * Sets the number of ports of this NFV block.
     *
     * @param portsNumber NFV processing block's ports' number
     */
    public void setPortsNumber(int portsNumber);

    /**
     * Returns the basic configuration of this NFV block.
     *
     * @return NFV processing block's basic configuration
     */
    public String basicConfiguration();

    /**
     * Returns the name of the network function that holds
     * this block.
     *
     * @return NFV processing block's network function name
     */
    public String networkFunctionOfOutIface();

    /**
     * Returns the list of output classes associated with this block.
     *
     * @return list of NFV processing block's output classes
     */
    public List<OutputClass> outputClasses();

    /**
     * Returns whether this block is a leaf in the processing pipeline.
     * This means that the number of output ports is zero.
     *
     * @return boolean leaf status
     */
    public boolean isLeaf();

    /**
     * Adds a new child to the output classes of this block.
     *
     * @param child a child NfvDataplaneBlockInterface
     * @param port the port number of the output class that gets this child
     * @param nextInputPort the port number of the output class after that child
     */
    public void setChild(NfvDataplaneBlockInterface child, int port, int nextInputPort);

}
