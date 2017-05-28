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

import org.onosproject.metron.impl.processing.ShaperBlock;
import org.onosproject.metron.impl.processing.ProcessingBlock;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Shaper block that queues packets.
 */
public class Queue extends ShaperBlock {

    private int capacity;

    private static final String CAPACITY = "CAPACITY";

    private static final int DEF_CAPACITY = 1024;

    public Queue(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.capacity = DEF_CAPACITY;
    }

    public Queue(
            String id,
            String conf,
            String confFile,
            int    capacity) {
        super(id, conf, confFile);

        checkArgument(
            capacity > 0,
            "[" + this.processingBlockClass() +
            "] Invalid queue capacity " + capacity
        );

        this.capacity = capacity;
    }

    /**
     * Returns the queue capacity.
     *
     * @return queue capacity
     */
    public int capacity() {
        return this.capacity;
    }

    /**
     * Sets the queue capacity.
     *
     * @param capacity queue capacity
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.QUEUE;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(CAPACITY);
        if (val != null) {
            this.setCapacity(
                Integer.parseInt(val.toString())
            );
        } else {
            this.setCapacity(DEF_CAPACITY);
        }
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new Queue(
            id,
            this.configuration(),
            this.configurationFile(),
            this.capacity()
        );
    }

}
