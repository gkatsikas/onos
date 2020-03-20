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

package org.onosproject.metron.api.processing;

/**
 * Type of a processing block.
 */
public enum ProcessingBlockType {

    BLOCK_TYPE_TERMINAL,
    BLOCK_TYPE_TRANSPARENT,
    BLOCK_TYPE_CLASSIFIER,
    BLOCK_TYPE_MODIFIER,
    BLOCK_TYPE_STATEFUL_MODIFIER,
    BLOCK_TYPE_SHAPER,
    BLOCK_TYPE_MONITOR

}
