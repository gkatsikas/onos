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
 * Block for standalone NFs to read packets from a Linux device.
 */
public class FromBlackboxDevice extends LinuxDevice {

    /**
     * A blackbox NF has an executable program
     * that receives some input arguments.
     */
    protected String exec;
    protected String args;

    /**
     * Multiple traffic classes might end up in a blackbox NF.
     * This counter helps to create a correct dataplane configuration.
     */
    protected short peerInstancesNb;

    public static final String EXEC = "EXEC";
    public static final String ARGS = "ARGS";

    private static final String DEF_EXEC = "";
    private static final String DEF_ARGS = "";

    public FromBlackboxDevice(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile, TerminalStage.INPUT);

        this.exec = DEF_EXEC;
        this.args = DEF_ARGS;
        this.peerInstancesNb = 0;
    }

    public FromBlackboxDevice(
            String id,
            String conf,
            String confFile,
            String devName) {
        super(id, conf, confFile, TerminalStage.INPUT, devName);

        this.exec = DEF_EXEC;
        this.args = DEF_ARGS;
        this.peerInstancesNb = 0;
    }

    public FromBlackboxDevice(
            String id,
            String conf,
            String confFile,
            String devName,
            String method,
            short  burst,
            String exec,
            String args) {
        super(id, conf, confFile, TerminalStage.INPUT, devName, method, burst);

        this.exec = exec;
        this.args = args;
        this.peerInstancesNb = 0;
    }

    /**
     * Returns the executable of this blackbox device.
     *
     * @return blackbox device executable
     */
    public String executable() {
        return this.exec;
    }

    /**
     * Sets the executable of this blackbox device.
     *
     * @param exec blackbox device executable
     */
    public void setExecutable(String exec) {
        this.exec = exec;
    }

    /**
     * Returns the arguments of this blackbox device.
     *
     * @return blackbox device arguments
     */
    public String arguments() {
        return this.args;
    }

    /**
     * Sets the arguments of this blackbox device.
     *
     * @param args blackbox device arguments
     */
    public void setArguments(String args) {
        this.args = args;
    }

    /**
     * Returns the number of peering ToBlackboxDevice devices.
     *
     * @return number of peering ToBlackboxDevice devices
     */
    public short numberOfPeers() {
        return this.peerInstancesNb;
    }

    /**
     * Increments (by one) the number of peering ToBlackboxDevice devices.
     */
    public void incrementNumberOfPeers() {
        this.peerInstancesNb++;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.FROM_BLACKBOX_DEVICE;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(EXEC);
        if (val != null) {
            this.setExecutable(val.toString());
        }

        val = this.configurationMap().get(ARGS);
        if (val != null) {
            this.setArguments(val.toString());
        }
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new FromBlackboxDevice(
            id,
            this.configuration(),
            this.configurationFile(),
            this.devName(),
            this.method(),
            this.burst(),
            this.executable(),
            this.arguments()
        );
    }

}
