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

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Modifier block that encapsulates packets into Ethernet frames.
 */
public class EtherEncap extends ModifierBlock {

    /**
     * Ethernet type of the packet in host order.
     */
    private short  etherType;
    /**
     * Source Ethernet address to be written in the header.
     */
    private MacAddress srcMac;
    /**
     * Destination Ethernet address to be written in the header.
     */
    private MacAddress dstMac;

    private static final String ETHERTYPE = "ETHERTYPE";
    private static final String SRC_MAC   = "SRC";
    private static final String DST_MAC   = "DST";

    /**
     * Default Ethernet type is IPv4 (0x800).
     */
    private static final short DEF_ETHERTYPE = Ethernet.TYPE_IPV4;

    public EtherEncap(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.etherType = DEF_ETHERTYPE;
    }

    public EtherEncap(
            String id,
            String conf,
            String confFile,
            short  etherType,
            String srcMacStr,
            String dstMacStr) {
        super(id, conf, confFile);

        checkArgument(
            etherType >= 0,
            "[" + this.processingBlockClass() +
            "] Invalid Ethernet type " + etherType
        );

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

        this.etherType = etherType;
        this.srcMac = MacAddress.valueOf(srcMacStr);
        this.dstMac = MacAddress.valueOf(dstMacStr);
    }

    /**
     * Returns the Ethernet type of the packet in host order.
     *
     * @return Ethernet type of the packet in host order
     */
    public short etherType() {
        return this.etherType;
    }

    /**
     * Returns the Ethernet type of the packet as a hexadecimal string.
     *
     * @return Ethernet type of the packet as hexadecimal string
     */
    public String etherTypeHexString() {
        return Integer.toHexString((int) this.etherType);
    }

    /**
     * Sets the Ethernet type of the packet in host order.
     *
     * @param etherType Ethernet type of the packet in host order
     */
    public void setEtherType(short etherType) {
        this.etherType = etherType;
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
        return ProcessingBlockClass.ETHER_ENCAP;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(ETHERTYPE);
        if (val != null) {
            String strVal = val.toString();

            // Remove the `0x` prefix of this hex value (if any)
            if (strVal.contains("0x")) {
                strVal = strVal.replace("0x", "");
            }

            this.setEtherType(
                Short.parseShort(strVal, 16)
            );
        } else {
            this.setEtherType(DEF_ETHERTYPE);
        }

        val = this.configurationMap().get(SRC_MAC);
        if (val != null) {
            this.setSrcMac(val.toString());
        } else {
            throw new ParseException(
                "[" + this.id + " (" + this.processingBlockClass() + ")] " +
                "Source Ethernet address is not given"
            );
        }

        val = this.configurationMap().get(DST_MAC);
        if (val != null) {
            this.setDstMac(val.toString());
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
        return new EtherEncap(
            id,
            this.configuration(),
            this.configurationFile(),
            this.etherType(),
            this.srcMacStr(),
            this.dstMacStr()
        );
    }

}