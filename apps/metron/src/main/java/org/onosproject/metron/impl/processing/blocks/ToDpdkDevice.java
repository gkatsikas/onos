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
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.TerminalStage;

import org.onosproject.metron.impl.processing.ProcessingBlock;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Terminal block that writes packets to a DPDK device.
 */
public class ToDpdkDevice extends DpdkDevice {

    private short   timeout;
    private short   intQueueSize;
    private boolean blocking;

    private static final String TIMEOUT        = "TIMEOUT";
    private static final String INT_QUEUE_SIZE = "IQUEUE";
    private static final String BLOCKING       = "BLOCKING";

    private static final short   DEF_TIMEOUT        = 0;
    private static final short   DEF_INT_QUEUE_SIZE = 1024;
    private static final boolean DEF_BLOCKING       = true;

    public ToDpdkDevice(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile, TerminalStage.OUTPUT);

        this.timeout      = DEF_TIMEOUT;
        this.intQueueSize = DEF_INT_QUEUE_SIZE;
        this.blocking     = DEF_BLOCKING;
    }

    public ToDpdkDevice(
            String id,
            String conf,
            String confFile,
            String devName) {
        super(id, conf, confFile, TerminalStage.OUTPUT, devName);

        this.timeout      = DEF_TIMEOUT;
        this.intQueueSize = DEF_INT_QUEUE_SIZE;
        this.blocking     = DEF_BLOCKING;
    }

    public ToDpdkDevice(
            String  id,
            String  conf,
            String  confFile,
            String  devName,
            short   queue,
            short   queuesNb,
            short   burst,
            short   timeout,
            short   intQueueSize,
            boolean blocking) {
        super(id, conf, confFile, TerminalStage.OUTPUT, devName, queue, queuesNb, burst);

        checkArgument(
            timeout >= -1,
            "[" + this.processingBlockClass() +
            "] Invalid timeout in milliseconds " + timeout
        );

        checkArgument(
            intQueueSize > 0,
            "[" + this.processingBlockClass() +
            "] Invalid internal queue size " + intQueueSize
        );

        this.timeout      = timeout;
        this.intQueueSize = intQueueSize;
        this.blocking     = blocking;
    }

    /**
     * Returns the timeout to flush the internal queue.
     *
     * @return timeout in milliseconds
     */
    public short timeout() {
        return this.timeout;
    }

    /**
     * Sets the timeout to flush the internal queue.
     *
     * @param timeout timeout in milliseconds
     */
    public void setTimeout(short timeout) {
        if (timeout < -1) {
            throw new ProcessingBlockException(
                "Timeout must be greater or equal than -1"
            );
        }
        this.timeout = timeout;
    }

    /**
     * Returns the size of the internal queue.
     *
     * @return internal queue size
     */
    public short internalQueueSize() {
        return this.intQueueSize;
    }

    /**
     * Sets the size of the internal queue.
     *
     * @param intQueueSize internal queue size
     */
    public void setInternalQueueSize(short intQueueSize) {
        if (intQueueSize <= 0) {
            throw new ProcessingBlockException(
                "Internal queue size must be a positive number"
            );
        }
        this.intQueueSize = intQueueSize;
    }

    /**
     * Returns whether the device is in blocking mode.
     *
     * @return boolean blocking mode
     */
    public boolean isBlocking() {
        return this.blocking;
    }

    /**
     * (Un)Sets the device in blocking mode.
     *
     * @param blocking boolean blocking mode
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.TO_DPDK_DEVICE;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(TIMEOUT);
        if (val != null) {
            this.setTimeout(
                Short.parseShort(val.toString())
            );
        } else {
            this.setTimeout(DEF_TIMEOUT);
        }

        val = this.configurationMap().get(INT_QUEUE_SIZE);
        if (val != null) {
            this.setInternalQueueSize(
                Short.parseShort(val.toString())
            );
        } else {
            this.setInternalQueueSize(DEF_INT_QUEUE_SIZE);
        }

        val = this.configurationMap().get(BLOCKING);
        if (val != null) {
            this.setBlocking(
                Boolean.valueOf(val.toString())
            );
        } else {
            this.setBlocking(DEF_BLOCKING);
        }
    }

    @Override
    public String fullConfiguration() {
        // TODO
        return "";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new ToDpdkDevice(
            id,
            this.configuration(),
            this.configurationFile(),
            this.devName(),
            this.queue(),
            this.queuesNumber(),
            this.burst(),
            this.timeout(),
            this.internalQueueSize(),
            this.isBlocking()
        );
    }

}
