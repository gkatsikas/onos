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

package org.onosproject.metron.api.processing;

import java.util.List;
import java.util.Map;

/**
 * The interface of a packet processing block.
 */
public interface ProcessingBlockInterface {

    /**
     * Returns the ID of this processing block.
     *
     * @return string processing block ID
     */
    String id();

    /**
     * Returns the type of this processing block.
     * This type is defined by the enumeration ProcessingBlockType.
     *
     * @return ProcessingBlockType type of the processing block
     */
    ProcessingBlockType processingBlockType();

    /**
     * Returns the class of this processing block.
     * This class is defined by the enumeration ProcessingBlockClass.
     *
     * @return ProcessingBlockClass class of the processing block
     */
    ProcessingBlockClass processingBlockClass();

    /**
     * Returns the ports of this processing block.
     *
     * @return list of ports pf this processing block
     */
    List<Integer> ports();

    /**
     * Returns the configuration map of this block.
     *
     * @return map of the block's configuration
     */
    Map<String, Object> configurationMap();

    /**
     * Returns the configuration string of this block
     * as a string.
     *
     * @return the block's configuration as a string
     */
    String configuration();

    /**
     * Returns the configuration file of this block (if any).
     *
     * @return the block's configuration file
     */
    String configurationFile();

    /**
     * Parse the configuration of the block and populate the
     * configuration map.
     */
    void parseConfiguration();

    /**
     * From the configuration map, populate down the line to
     * the members of the inheriting blocks.
     */
    void populateConfiguration();

    /**
     * Returns a clone of this processing block with unique ID.
     *
     * @return cloned processing block
     */
    ProcessingBlockInterface clone();

    /**
     * Returns whether this block has been cloned or not.
     *
     * @return boolean
     */
    boolean isClone();

    /**
     * Returns the original processing block
     * instance of the current one.
     *
     * @return original processing block
     */
    ProcessingBlockInterface originalInstance();

    /**
     * A builder for processing blocks.
     */
    interface Builder {
        Builder addPort();
        ProcessingBlockInterface build();
    }

}
