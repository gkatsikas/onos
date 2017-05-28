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
 * Terminal block that reads packets from a DPDK device.
 */
public class FromDpdkDevice extends DpdkDevice {

    private boolean active;
    private boolean promiscuous;

    private static final String ACTIVE      = "ACTIVE";
    private static final String PROMISCUOUS = "PROMISC";

    private static final boolean DEF_ACTIVITY    = true;
    private static final boolean DEF_PROMISCUOUS = false;

    public FromDpdkDevice(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile, TerminalStage.INPUT);

        this.active      = DEF_ACTIVITY;
        this.promiscuous = DEF_PROMISCUOUS;
    }

    public FromDpdkDevice(
            String id,
            String conf,
            String confFile,
            String devName) {
        super(id, conf, confFile, TerminalStage.INPUT, devName);

        this.active      = DEF_ACTIVITY;
        this.promiscuous = DEF_PROMISCUOUS;
    }

    public FromDpdkDevice(
            String  id,
            String  conf,
            String  confFile,
            String  devName,
            short   queue,
            short   queuesNb,
            short   burst,
            boolean promiscuous,
            boolean active) {
        super(id, conf, confFile, TerminalStage.INPUT, devName, queue, queuesNb, burst);

        this.active      = active;
        this.promiscuous = promiscuous;
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

    /**
     * Returns whether the element is active or not.
     *
     * @return boolean activity mode
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * (Un)Sets the activity of the element.
     *
     * @param active boolean activity mode
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.FROM_DPDK_DEVICE;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(ACTIVE);
        if (val != null) {
            this.setActive(
                Boolean.valueOf(val.toString())
            );
        } else {
            this.setActive(DEF_ACTIVITY);
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
        return new FromDpdkDevice(
            id,
            this.configuration(),
            this.configurationFile(),
            this.devName(),
            this.queue(),
            this.queuesNumber(),
            this.burst(),
            this.isPromisc(),
            this.isActive()
        );
    }

}
