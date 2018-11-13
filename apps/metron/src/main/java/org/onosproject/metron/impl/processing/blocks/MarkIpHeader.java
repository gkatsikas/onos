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
 * Modifier block that sets the IP annotation of each packet.
 */
public class MarkIpHeader extends ModifierBlock {

    protected int offset;

    protected static final String OFFSET = "OFFSET";

    public    static final short  ETH_OFFSET = 0;
    public    static final short  IP_OFFSET  = Ethernet.ETHERNET_HEADER_LENGTH;
    protected static final short  DEF_OFFSET = ETH_OFFSET;

    public MarkIpHeader(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.offset = DEF_OFFSET;
    }

    public MarkIpHeader(
            String id,
            String conf,
            String confFile,
            int    offset) {
        super(id, conf, confFile);

        checkArgument(
            offset >= 0,
            "[" + this.processingBlockClass() +
            "] Invalid offset " + offset
        );

        this.offset = offset;
    }

    /**
     * Returns the offset after which the IP header starts.
     *
     * @return IP header offset
     */
    public int offset() {
        return this.offset;
    }

    /**
     * Sets the offset after which the IP header starts.
     *
     * @param offset IP header offset
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.MARK_IP_HEADER;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(OFFSET);
        if (val != null) {
            this.setOffset(
                Short.parseShort(val.toString())
            );
        } else {
            this.setOffset(DEF_OFFSET);
        }
    }

    @Override
    public String fullConfiguration() {
        // TODO
        return "";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new MarkIpHeader(
            id,
            this.configuration(),
            this.configurationFile(),
            this.offset()
        );
    }

}
