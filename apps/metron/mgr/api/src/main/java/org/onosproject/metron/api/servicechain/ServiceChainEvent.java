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

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes service chain events.
 */
public class ServiceChainEvent
        extends AbstractEvent<ServiceChainState, ServiceChainInterface> {

    /**
     * Creates an event for the specified service chain,
     * which currently exhibits a given state.
     *
     * @param state service chain event type
     * @param sc event service chain subject
     */
    public ServiceChainEvent(ServiceChainState state, ServiceChainInterface sc) {
        super(state, sc);
    }

    /**
     * Creates an event for the specified service chain,
     * which currently exhibits a given state.
     * The event is associated with a timestamp
     *
     * @param state service chain event type
     * @param sc event service chain subject
     * @param time event timestamp
     */
    public ServiceChainEvent(ServiceChainState state, ServiceChainInterface sc, long time) {
        super(state, sc, time);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("state", type())
                .add("service-chain", subject())
                .toString();
    }

}
