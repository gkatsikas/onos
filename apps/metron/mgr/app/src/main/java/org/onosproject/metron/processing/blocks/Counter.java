/*
 * Copyright 2018-present Open Networking Foundation
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

import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.processing.TransparentBlock;
import org.onosproject.metron.processing.ProcessingBlock;

/**
 * Transparent block that measures packet count and rate.
 */
public class Counter extends TransparentBlock {

    public Counter(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.COUNTER;
    }

    @Override
    public void populateConfiguration() {
        // Do nothing
    }

    @Override
    public String fullConfiguration() {
        return "CounterMP()";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new Counter(
            id,
            this.configuration(),
            this.configurationFile()
        );
    }

}