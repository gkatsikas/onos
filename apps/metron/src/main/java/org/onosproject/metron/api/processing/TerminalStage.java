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
 * Stages of terminal processing blocks:
 * |-> If stage = INPUT,  this is a From(Dpdk)Device element.
 * |-> If stage = OUTPUT, this is a To(Dpdk)Device element.
 * |-> If stage = DROP,   this is a Discard element.
 */
public enum TerminalStage {

    INPUT,
    OUTPUT,
    DROP

}