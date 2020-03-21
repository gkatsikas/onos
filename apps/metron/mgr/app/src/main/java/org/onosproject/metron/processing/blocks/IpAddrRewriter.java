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

package org.onosproject.metron.processing.blocks;

import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.processing.ProcessingBlock;

/**
 * Modifier block that statefuly modifies src IP addresses.
 */
public class IpAddrRewriter extends IpRewriter {

    public IpAddrRewriter(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.IP_ADDR_REWRITER;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();
    }

    @Override
    public String fullConfiguration() {
        return super.fullConfiguration();
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new IpAddrRewriter(
            id,
            this.configuration(),
            this.configurationFile()
        );
    }

}
