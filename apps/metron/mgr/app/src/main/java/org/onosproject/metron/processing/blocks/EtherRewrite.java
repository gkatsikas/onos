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

package org.onosproject.metron.processing.blocks;

import org.onosproject.metron.api.exceptions.ParseException;
import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.processing.ProcessingBlock;
import org.onosproject.metron.processing.ModifierBlock;

import org.onlab.packet.MacAddress;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Modifier block that modifies the Ethernet addresses of incoming frames.
 */
public class EtherRewrite extends ModifierBlock {

    /**
     * Source Ethernet address to be written in the header.
     */
    private MacAddress srcMac;
    /**
     * Destination Ethernet address to be written in the header.
     */
    private MacAddress dstMac;

    private static final String SRC_MAC = "SRC";
    private static final String DST_MAC = "DST";

    public EtherRewrite(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);
    }

    public EtherRewrite(
            String id,
            String conf,
            String confFile,
            String srcMacStr,
            String dstMacStr) {
        super(id, conf, confFile);

        checkArgument(
            !Strings.isNullOrEmpty(srcMacStr),
            "[" + this.processingBlockClass() +
            "] Source Ethernet address is NULL or empty"
        );

        checkArgument(
            !Strings.isNullOrEmpty(dstMacStr),
            "[" + this.processingBlockClass() +
            "] Destination Ethernet address is NULL or empty"
        );

        this.srcMac = MacAddress.valueOf(srcMacStr);
        this.dstMac = MacAddress.valueOf(dstMacStr);
    }

    /**
     * Returns the source Ethernet address to be written in the header
     * as a MacAddress.
     *
     * @return source Ethernet address
     */
    public MacAddress srcMac() {
        return this.srcMac;
    }

    /**
     * Returns the source Ethernet address to be written in the header
     * as a string.
     *
     * @return string-based source Ethernet address
     */
    public String srcMacStr() {
        return this.srcMac.toString();
    }

    /**
     * Sets the source Ethernet address to be written in the header.
     *
     * @param srcMacStr source Ethernet address to be written in the header
     */
    public void setSrcMac(String srcMacStr) {
        this.srcMac = MacAddress.valueOf(srcMacStr);
    }

    /**
     * Returns the destination Ethernet address to be written in the header
     * as a MacAddress.
     *
     * @return destination Ethernet address
     */
    public MacAddress dstMac() {
        return this.dstMac;
    }

    /**
     * Returns the destination Ethernet address to be written in the header
     * as a string.
     *
     * @return string-based destination Ethernet address
     */
    public String dstMacStr() {
        return this.dstMac.toString();
    }

    /**
     * Sets the destination Ethernet address to be written in the header.
     *
     * @param dstMacStr destination Ethernet address to be written in the header
     */
    public void setDstMac(String dstMacStr) {
        this.dstMac = MacAddress.valueOf(dstMacStr);
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.ETHER_REWRITE;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(SRC_MAC);
        if (val != null) {
            this.setSrcMac(val.toString());
            this.configurationMap().remove(SRC_MAC);
        } else {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Source Ethernet address is not given"
            );
        }

        val = this.configurationMap().get(DST_MAC);
        if (val != null) {
            this.setDstMac(val.toString());
            this.configurationMap().remove(DST_MAC);
        } else {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Destination Ethernet address is not given"
            );
        }
    }

    @Override
    public String fullConfiguration() {
        return "EtherRewrite(SRC " + this.srcMacStr() + ", DST " + this.dstMacStr() + ")";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new EtherRewrite(
            id,
            this.configuration(),
            this.configurationFile(),
            this.srcMacStr(),
            this.dstMacStr()
        );
    }

}
