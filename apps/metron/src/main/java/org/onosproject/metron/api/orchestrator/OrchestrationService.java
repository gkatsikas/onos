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

package org.onosproject.metron.api.orchestrator;

import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import org.onosproject.net.DeviceId;

import java.net.URI;
import java.util.Set;
import java.util.Iterator;

/**
 * Services provided by Metron's service chain orchestrator.
 */
public interface OrchestrationService {

    /**
     * Adds a service chain to the set of "active" service chains.
     *
     * @param sc an "active" service chain
     */
    void sendServiceChainToOrchestrator(ServiceChainInterface sc);

    /**
     * Returns the set of active (i.e., deployed) service chains.
     *
     * @return set of active service chains
     */
    Set<ServiceChainInterface> activeServiceChains();

    /**
     * Returns the active service chain with the given ID.
     *
     * @param scId service chain ID
     * @return active service chain
     */
    ServiceChainInterface activeServiceChainWithId(ServiceChainId scId);

    /**
     * Undeploy an active service chain and update the corresponding memories.
     *
     * @param sc the service chain to undeploy
     * @param scIterator the iterator of this memory to be used for safe removal
     */
    void markServiceChainAsUndeployed(
        ServiceChainInterface sc,
        Iterator<ServiceChainInterface> scIterator
    );

    /**
     * Iterate through the set of "deployed" service chains
     * and manage their runtime performance.
     */
    void manage();

    /**
     * When a "deployed" service chain exhbits high load,
     * react by balancing the load.
     *
     * @param scId the ID of the overloaded service chain
     * @param tcId the ID of the overloaded traffic class
     * @param deviceId the ID of the device where the overload occured
     * @param overLoadedCpu the CPU core that exhibits overload
     * @param maxCpus the new maximum number of CPUs you can currently have
     * @param limitedReconfiguration the device to reconfigure has limited
              reconfiguration abilities (e.g., RSS mode requires to simply
              increase the number of queues)
     */
    void deflateLoad(
        ServiceChainId scId,
        URI            tcId,
        DeviceId       deviceId,
        int            overLoadedCpu,
        int            maxCpus,
        boolean        limitedReconfiguration
    );

    /**
     * When a "deployed" service chain exhbits low load,
     * react by balancing the load.
     *
     * @param scId the ID of the overloaded service chain
     * @param tcId the ID of the overloaded traffic class
     * @param deviceId the ID of the device where the overload occured
     * @param underLoadedCpu the CPU core that exhibits underload
     * @param maxCpus the new maximum number of CPUs you can currently have
     * @param limitedReconfiguration the device to reconfigure has limited
              reconfiguration abilities (e.g., RSS mode requires to simply
              decrease the number of queues)
     */
    void inflateLoad(
        ServiceChainId scId,
        URI            tcId,
        DeviceId       deviceId,
        int            underLoadedCpu,
        int            maxCpus,
        boolean        limitedReconfiguration
    );

}
