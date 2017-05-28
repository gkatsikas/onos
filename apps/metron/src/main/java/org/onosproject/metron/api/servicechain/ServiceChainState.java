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

package org.onosproject.metron.api.servicechain;

import org.onosproject.metron.api.networkfunction.NetworkFunctionState;

/**
 * State of a service chain.
 */
public enum ServiceChainState {

    /**
     * Signifies that a new service chain has been initialized.
     */
    INIT {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processInitState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.INIT;
        }
    },
    /**
     * Signifies that a service chain has been constructed
     * (the NFs and their processing graphs are built by the application).
     */
    CONSTRUCTED {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processConstructedState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.CONSTRUCTED;
        }
    },
    /**
     * Signifies that a service chain has been constructed
     * (the NFs and their processing graphs are built by the application).
     */
    SYNTHESIZED {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processSynthesizedState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.SYNTHESIZED;
        }
    },
    /**
     * Signifies that a service chain is ready for placement and deployment.
     * Constructed service chains that do not pass through the synthesizer
     * can be in this state. Synthesized service chains as well.
     */
    READY {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processReadyState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.READY;
        }
    },
    /**
     * Signifies that a service chain has been placed into the network.
     * This holds only when all the NFs of this chain are placed.
     */
    PLACED {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processPlacedState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.PLACED;
        }
    },
    /**
     * Signifies that a service chain has been deployed.
     * This holds only when all the NFs of this chain are deployed.
     */
    DEPLOYED {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processDeployedState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.DEPLOYED;
        }
    },
    /**
     * Signifies that a service chain has been suspended due to some issue.
     */
    SUSPENDED {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processSuspendedState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.SUSPENDED;
        }
    },
    /**
     * Signifies that a service chain has been in transition from one state to another.
     * This holds when not all of the NFs of this service chain are in the same state.
     */
    TRANSIENT {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processTransientState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.TRANSIENT;
        }
    },
    /**
     * Signifies that a service chain has been destroyed.
     * This holds only when all the NFs of this chain are destroyed.
     */
    DESTROYED {
        @Override
        public void process(
                ServiceChainService scService,
                ServiceChainInterface sc) {
            scService.processDestroyedState(sc);
        }

        @Override
        public NetworkFunctionState asNetworkFunctionState() {
            return NetworkFunctionState.DESTROYED;
        }
    };

    /**
     * Processes the state of the service chain.
     * @param scService service chain's service
     * @param sc service chain
     * @retun
     */
    public abstract void process(
        ServiceChainService   scService,
        ServiceChainInterface sc
    );

    /**
     * Maps a network function's state with the
     * state of the service chain that it belongs to.
     *
     * @return network function's state
     */
    public abstract NetworkFunctionState asNetworkFunctionState();

}
