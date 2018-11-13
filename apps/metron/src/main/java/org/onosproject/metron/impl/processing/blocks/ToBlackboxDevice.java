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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Block for standalone NFs to write packets to a Linux device.
 */
public class ToBlackboxDevice extends LinuxDevice {

    /**
     * The other end of this blackbox NF.
     */
    protected FromBlackboxDevice peer;

    public ToBlackboxDevice(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile, TerminalStage.OUTPUT);

        this.peer = null;
    }

    public ToBlackboxDevice(
            String id,
            String conf,
            String confFile,
            String devName) {
        super(id, conf, confFile, TerminalStage.OUTPUT, devName);

        this.peer = null;
    }

    /**
     * Returns the peer device of this blackbox device.
     *
     * @return peering FromBlackboxDevice
     */
    public FromBlackboxDevice peerDevice() {
        return this.peer;
    }

    /**
     * Sets the peer device of this blackbox device.
     *
     * @param peer a peering FromBlackboxDevice
     */
    public void setPeerDevice(FromBlackboxDevice peer) {
        checkNotNull(
            peer,
            this.getClass() + " element peers with a null FromBlackboxDevice element"
        );
        this.peer = peer;

        // Update the number of peers of our peer
        peer.incrementNumberOfPeers();
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.TO_BLACKBOX_DEVICE;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();
    }

    @Override
    public String fullConfiguration() {
        // TODO
        return "";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new ToBlackboxDevice(
            id,
            this.configuration(),
            this.configurationFile(),
            this.devName()
        );
    }

}
