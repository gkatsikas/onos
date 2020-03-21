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

package org.onosproject.metron.dataplane;

import org.onosproject.metron.api.dataplane.NfvDataplaneNodeInterface;
import org.onosproject.metron.api.dataplane.NfvDataplaneBlockInterface;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;

import org.onosproject.metron.classification.trafficclass.TrafficClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Overlay class of an NFV dataplane block together with a traffic class.
 */
public class NfvDataplaneNode implements NfvDataplaneNodeInterface {
    NfvDataplaneBlockInterface block;
    TrafficClassInterface trafficClass;

    public NfvDataplaneNode(NfvDataplaneBlockInterface block) {
        checkNotNull(
            block,
            "Cannot construct an NFV dataplane node using a NULL block."
        );

        this.block = block;
        this.trafficClass = new TrafficClass();
    }

    public NfvDataplaneNode(NfvDataplaneBlockInterface block, TrafficClassInterface tc) {
        checkNotNull(
            block,
            "Cannot construct an NFV dataplane node using a NULL block."
        );

        checkNotNull(
            tc,
            "Cannot construct an NFV dataplane node using a NULL traffic class."
        );

        this.block = block;
        this.trafficClass = tc;
    }

    /**
     * Returns the dataplane block of this NFV dataplane node.
     *
     * @return NfvDataplaneBlockInterface the dataplane block of this NFV
     *         dataplane node
     */
    public NfvDataplaneBlockInterface block() {
        return this.block;
    }

    /**
     * Returns the traffic class of this NFV dataplane node.
     *
     * @return TrafficClassInterface the traffic class of this NFV
     *         dataplane node
     */
    public TrafficClassInterface trafficClass() {
        return this.trafficClass;
    }

}
