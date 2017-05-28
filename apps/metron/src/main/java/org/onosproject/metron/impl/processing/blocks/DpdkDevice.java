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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Terminal block that represents a DPDK-based network device.
 */
public abstract class DpdkDevice extends Device {

    protected short  queue;
    protected short  queuesNb;

    protected static final String PORT     = "PORT";
    protected static final String QUEUE    = "QUEUE";
    protected static final String N_QUEUES = "N_QUEUES";

    protected static final short  DEF_QUEUE      = 0;
    protected static final short  DEF_NB_QUEUES  = -1;  // Queues == threads assigned to this element
    protected static final short  MAX_NB_QUEUES  = 128;
    protected static final short  DEF_DPDK_BURST = 32;

    public DpdkDevice(
            String id,
            String conf,
            String confFile,
            TerminalStage stage) {
        super(id, conf, confFile, stage);

        this.queue    = DEF_QUEUE;
        this.queuesNb = DEF_NB_QUEUES;
    }

    public DpdkDevice(
            String id,
            String conf,
            String confFile,
            TerminalStage stage,
            String devName) {
        super(id, conf, confFile, stage, devName, DEF_DPDK_BURST);

        this.queue    = DEF_QUEUE;
        this.queuesNb = DEF_NB_QUEUES;
    }

    public DpdkDevice(
            String id,
            String conf,
            String confFile,
            TerminalStage stage,
            String devName,
            short  queue,
            short  queuesNb,
            short  burst) {
        super(id, conf, confFile, stage, devName, burst);

        checkArgument(
            (queue >= 0) && (queue < MAX_NB_QUEUES),
            "[" + this.processingBlockClass() +
            "] Invalid queue " + queue
        );

        checkArgument(
            (queuesNb >= 0) && (queuesNb < MAX_NB_QUEUES),
            "[" + this.processingBlockClass() +
            "] Invalid number of queues " + queuesNb
        );

        this.queue    = queue;
        this.queuesNb = queuesNb;
    }

    /**
     * Returns the HW queue number used to read packets.
     *
     * @return queue number
     */
    public short queue() {
        return this.queue;
    }

    /**
     * Sets the HW queue number used to read packets.
     *
     * @param queue queue number
     */
    public void setQueue(short queue) {
        if ((queue < 0) || (queue >= MAX_NB_QUEUES)) {
            // final int max = MAX_NB_QUEUES-1;
            throw new ProcessingBlockException(
                "Queue can be in [0" + (MAX_NB_QUEUES - 1) + "]"
            );
        }
        this.queue = queue;
    }

    /**
     * Returns the number of HW queues to be used to read packets.
     *
     * @return number of queues
     */
    public short queuesNumber() {
        return this.queuesNb;
    }

    /**
     * Sets the number of HW queues to be used to read packets.
     *
     * @param queuesNb number of queues
     */
    public void setQueuesNumber(short queuesNb) {
        if ((queuesNb < -1) || (queuesNb >= MAX_NB_QUEUES)) {
            throw new ProcessingBlockException(
                "Number of queues can be in [-1" + (MAX_NB_QUEUES - 1) + "]"
            );
        }
        this.queuesNb = queuesNb;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(PORT);
        if (val != null) {
            this.setDevName(val.toString());
            this.configurationMap().remove(PORT);
        }

        val = this.configurationMap().get(QUEUE);
        if (val != null) {
            this.setQueue(Short.parseShort(val.toString()));
            this.configurationMap().remove(QUEUE);
        } else {
            this.setQueue(DEF_QUEUE);
        }

        val = this.configurationMap().get(N_QUEUES);
        if (val != null) {
            this.setQueuesNumber(Short.parseShort(val.toString()));
            this.configurationMap().remove(N_QUEUES);
        } else {
            this.setQueuesNumber(DEF_NB_QUEUES);
        }
    }

    @Override
    public String fullConfiguration() {
        return PORT     + " " + devName()      + ", " +
               QUEUE    + " " + queue()        + ", " +
               N_QUEUES + " " + queuesNumber() + ", " +
               super.fullConfiguration();
    }

}
