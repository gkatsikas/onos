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
import org.onosproject.metron.api.processing.TerminalStage;

import org.onosproject.metron.impl.processing.ProcessingBlock;

/**
 * Terminal block that reads packets from a Linux-based I/O device.
 */
public class FromDevice extends LinuxDevice {

    private boolean sniffer;
    private boolean promiscuous;

    private static final String SNIFFER     = "SNIFFER";
    private static final String PROMISCUOUS = "PROMISC";

    private static final boolean DEF_SNIFFER     = true;
    private static final boolean DEF_PROMISCUOUS = false;

    public FromDevice(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile, TerminalStage.INPUT);

        this.sniffer     = DEF_SNIFFER;
        this.promiscuous = DEF_PROMISCUOUS;
    }

    public FromDevice(
            String id,
            String conf,
            String confFile,
            String devName) {
        super(id, conf, confFile, TerminalStage.INPUT, devName);

        this.sniffer     = DEF_SNIFFER;
        this.promiscuous = DEF_PROMISCUOUS;
    }

    public FromDevice(
            String  id,
            String  conf,
            String  confFile,
            String  devName,
            String  method,
            short   burst,
            boolean sniffer,
            boolean promiscuous) {
        super(id, conf, confFile, TerminalStage.INPUT, devName, method, burst);

        this.sniffer = sniffer;
        this.promiscuous = promiscuous;
    }

    /**
     * Returns whether this device uses kernel filtering or not.
     * SNIFFER = true means that the kernel inspects all packets.
     *
     * @return boolean kernel filtering
     */
    public boolean isSniffer() {
        return this.sniffer;
    }

    /**
     * (Un)Sets the kernel filtering.
     *
     * @param sniffer boolean kernel filtering
     */
    public void setSniffer(boolean sniffer) {
        this.sniffer = sniffer;
    }

    /**
     * Returns whether the device is in promiscuous mode.
     *
     * @return boolean promiscuous mode
     */
    public boolean isPromisc() {
        return this.promiscuous;
    }

    /**
     * (Un)Sets the device in promiscuous mode.
     *
     * @param promiscuous boolean promiscuous mode
     */
    public void setPromisc(boolean promiscuous) {
        this.promiscuous = promiscuous;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.FROM_DEVICE;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(SNIFFER);
        if (val != null) {
            this.setSniffer(
                Boolean.valueOf(val.toString())
            );
        } else {
            this.setSniffer(DEF_SNIFFER);
        }

        val = this.configurationMap().get(PROMISCUOUS);
        if (val != null) {
            this.setPromisc(
                Boolean.valueOf(val.toString())
            );
        } else {
            this.setPromisc(DEF_PROMISCUOUS);
        }
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new FromDevice(
            id,
            this.configuration(),
            this.configurationFile(),
            this.devName(),
            this.method(),
            this.burst(),
            this.isSniffer(),
            this.isPromisc()
        );
    }

}
