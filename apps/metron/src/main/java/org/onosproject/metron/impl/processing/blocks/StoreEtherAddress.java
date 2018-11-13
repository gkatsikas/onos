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

import org.onosproject.metron.api.exceptions.ParseException;
import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.impl.processing.ProcessingBlock;
import org.onosproject.metron.impl.processing.ModifierBlock;

import org.onlab.packet.MacAddress;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Modifier block that sets the source/destination
 * Ethernet address of a frame.
 */
public class StoreEtherAddress extends ModifierBlock {

    /**
     * Ethernet address to be written in the header.
     */
    private MacAddress mac;
    /**
     * Offset where the address will be written.
     */
    private String offset;

    private static final String ADDRESS = "ADDR";
    private static final String OFFSET  = "OFFSET";

    /**
     * Default offset is for destination MAC.
     * This is equivalent to 6 bytes after
     * the first bytes of the header.
     */
    public  static final String DST_OFFSET = "dst";
    public  static final String SRC_OFFSET = "src";
    private static final String DEF_OFFSET = DST_OFFSET;

    public StoreEtherAddress(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.offset = DEF_OFFSET;
    }

    public StoreEtherAddress(
            String id,
            String conf,
            String confFile,
            String macStr,
            String offset) {
        super(id, conf, confFile);

        checkArgument(
            !Strings.isNullOrEmpty(macStr),
            "[" + this.processingBlockClass() +
            "] Ethernet address is NULL or empty"
        );

        checkArgument(
            !Strings.isNullOrEmpty(offset),
            "[" + this.processingBlockClass() +
            "] Offset is NULL or empty"
        );

        if (!offset.equals(SRC_OFFSET) || !offset.equals(DST_OFFSET)) {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Invalid offset is given: " + offset
            );
        }

        this.mac = MacAddress.valueOf(macStr);
        this.offset = offset;
    }

    /**
     * Returns the Ethernet address to be written in the header
     * as a MacAddress.
     *
     * @return Ethernet address
     */
    public MacAddress mac() {
        return this.mac;
    }

    /**
     * Returns the Ethernet address to be written in the header
     * as a string.
     *
     * @return string-based Ethernet address
     */
    public String macStr() {
        return this.mac.toString();
    }

    /**
     * Sets the Ethernet address to be written in the header.
     *
     * @param macStr Ethernet address to be written in the header
     */
    public void setMac(String macStr) {
        this.mac = MacAddress.valueOf(macStr);
    }

    /**
     * Returns the offset where the address will be writen.
     *
     * @return offset where the address will be writen
     */
    public String offset() {
        return this.offset;
    }

    /**
     * Sets the offset where the address will be writen.
     *
     * @param offset where the address will be writen
     */
    public void setOffset(String offset) {
        if (!offset.equals(SRC_OFFSET) && !offset.equals(DST_OFFSET)) {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Invalid offset is given: " + offset
            );
        }

        this.offset = offset;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.STORE_ETHER_ADDRESS;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(ADDRESS);
        if (val != null) {
            this.setMac(val.toString());
        } else {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Ethernet address is not given"
            );
        }

        val = this.configurationMap().get(OFFSET);
        if (val != null) {
            this.setOffset(val.toString());
        } else {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Offset is not given"
            );
        }
    }

    @Override
    public String fullConfiguration() {
        return "StoreEtherAddress(" + this.macStr() + ", " + this.offset() + ")";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new StoreEtherAddress(
            id,
            this.configuration(),
            this.configurationFile(),
            this.macStr(),
            this.offset()
        );
    }

}
