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

import org.onosproject.metron.api.exceptions.ProcessingBlockException;
import org.onosproject.metron.api.processing.TerminalStage;

import org.onosproject.metron.impl.processing.TerminalBlock;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Terminal block that represents a network device.
 */
public abstract class Device extends TerminalBlock {

    protected String devName;
    protected short  burst;

    protected static final String BURST = "BURST";

    protected static final short DEF_BURST = 1;

    public Device(
            String id,
            String conf,
            String confFile,
            TerminalStage stage) {
        super(id, conf, confFile, stage);
    }

    public Device(
            String id,
            String conf,
            String confFile,
            TerminalStage stage,
            String devName) {
        super(id, conf, confFile, stage);

        checkArgument(
            !Strings.isNullOrEmpty(devName),
            "[" + this.processingBlockClass() +
            "] Invalid device name"
        );

        this.devName = devName;
        this.burst   = DEF_BURST;

        this.addPort();
    }

    public Device(
            String id,
            String conf,
            String confFile,
            TerminalStage stage,
            String devName,
            short  burst) {
        super(id, conf, confFile, stage);

        checkArgument(
            !Strings.isNullOrEmpty(devName),
            "[" + this.processingBlockClass() +
            "] Invalid device name"
        );

        checkArgument(
            burst > 0,
            "[" + this.processingBlockClass() +
            "] Invalid burst " + burst
        );

        this.devName = devName;
        this.burst   = burst;

        this.addPort();
    }

    /**
     * Returns the name of the device.
     *
     * @return device name
     */
    public String devName() {
        return this.devName;
    }

    /**
     * Sets the name of the device.
     *
     * @param devName device name
     */
    public void setDevName(String devName) {
        this.devName = devName;
    }

    /**
     * Returns the number packets to read at once.
     *
     * @return number of packets per read
     */
    public short burst() {
        return this.burst;
    }

    /**
     * Sets the number packets to read at once.
     *
     * @param burst number of packets per read
     */
    public void setBurst(short burst) {
        if (burst <= 0) {
            throw new ProcessingBlockException(
                "Burst cannot be a non-positive number"
            );
        }
        this.burst = burst;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(BURST);
        if (val != null) {
            this.setBurst(Short.parseShort(val.toString()));
            this.configurationMap().remove(BURST);
        } else {
            this.setBurst(DEF_BURST);
        }
    }

    @Override
    public String fullConfiguration() {
        return BURST + " " + burst();
    }

}
