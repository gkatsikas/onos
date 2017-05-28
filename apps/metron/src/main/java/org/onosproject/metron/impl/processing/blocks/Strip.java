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

package org.onosproject.metron.impl.processing.blocks;

import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.impl.processing.ProcessingBlock;
import org.onosproject.metron.impl.processing.ModifierBlock;

import org.onlab.packet.Ethernet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Modifier block that strips a number of bytes
 * from the front side of a packet's header.
 */
public class Strip extends ModifierBlock {

    private int length;

    private static final String LENGTH = "LENGTH";

    /**
     * Default behavior assumes that we strip the Ethernet header.
     */
    public  static final short  ETH_LENGTH = 0;
    public  static final short  IP_LENGTH  = Ethernet.ETHERNET_HEADER_LENGTH;
    private static final short  DEF_LENGTH = IP_LENGTH;

    public Strip(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.length = DEF_LENGTH;
    }

    public Strip(
            String id,
            String conf,
            String confFile,
            int    length) {
        super(id, conf, confFile);

        checkArgument(
            length >= 0,
            "[" + this.processingBlockClass() +
            "] Invalid length " + length
        );

        this.length = length;
    }

    /**
     * Returns the length which we remove from the header.
     *
     * @return header length to be removed
     */
    public int length() {
        return this.length;
    }

    /**
     * Sets the length which we remove from the header.
     *
     * @param length header length to be removed
     */
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.STRIP;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(LENGTH);
        if (val != null) {
            this.setLength(
                Integer.parseInt(val.toString())
            );
        } else {
            this.setLength(DEF_LENGTH);
        }
    }

    @Override
    public String fullConfiguration() {
        return "Strip(" + length() + ")";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new Strip(
            id,
            this.configuration(),
            this.configurationFile(),
            this.length()
        );
    }

}
