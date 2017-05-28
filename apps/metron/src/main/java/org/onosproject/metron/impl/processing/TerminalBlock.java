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

package org.onosproject.metron.impl.processing;

import org.onosproject.metron.api.common.Common;
import org.onosproject.metron.api.processing.TerminalStage;
import org.onosproject.metron.api.processing.ProcessingBlockType;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class TerminalBlock extends ProcessingBlock {

    protected TerminalStage stage;

    protected TerminalBlock(
            String id,
            String conf,
            String confFile,
            TerminalStage stage) {
        super(id, conf, confFile);

        checkNotNull(
            stage,
            "Incompatible terminal block stage. Choose one in: " +
            Common.<TerminalStage>
            enumTypesToString(
                TerminalStage.class
            )
        );

        this.stage = stage;
        this.type = ProcessingBlockType.BLOCK_TYPE_TERMINAL;
    }

    public TerminalStage stage() {
        return this.stage;
    }

    public void setStage(TerminalStage stage) {
        checkNotNull(
            stage,
            "Incompatible terminal block stage. Choose one in: " +
            Common.<TerminalStage>
            enumTypesToString(
                TerminalStage.class
            )
        );

        this.stage = stage;
    }

}
