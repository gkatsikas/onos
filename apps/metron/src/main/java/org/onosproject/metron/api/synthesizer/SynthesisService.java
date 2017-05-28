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

package org.onosproject.metron.api.synthesizer;

import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import java.util.Set;

/**
 * Services provided by Metron's service chain synthesizer.
 */
public interface SynthesisService {

    /**
     * Returns the list of "ready to synthesize" service chains.
     *
     * @return set of synthesizable service chains
     */
    Set<ServiceChainInterface> serviceChainsToSynthesize();

    /**
     * Adds a service chain to the list of "ready to synthesize" service chains.
     *
     * @param sc a synthesizable service chain
     */
    void addServiceChainToSynthesizer(ServiceChainInterface sc);

    /**
     * Marks a service chain as synthesized by changing its state.
     *
     * @param sc a synthesized service chain
     */
    void markServiceChainAsSynthesized(ServiceChainInterface sc);

    /**
     * Marks a service chain as ready by changing its state.
     * This occurs if the NF Synthesizer is disabled, so a service chain
     * transitions directly from CONSTRUCTED to READY.
     *
     * @param sc a synthesized service chain
     */
    void markServiceChainAsReady(ServiceChainInterface sc);

    /**
     * The key objective of this module is to synthesize the service chains
     * registered with the Service Chain Manager (and kept into the store).
     * This method iterates the list of "ready to synthesize" service chains
     * and turns them into highly optimized ones (similar to SNF).
     * https://github.com/gkatsikas/snf
     *
     * @return boolean synthesis status
     */
    boolean synthesize();

}