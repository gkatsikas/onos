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

import org.onosproject.metron.api.common.GenericId;

import java.net.URI;

/**
 * Immutable representation of a service chain's identity.
 */
public final class ServiceChainId extends GenericId {

    /**
     * Private constructors.
     */
    private ServiceChainId() {
        super();
    }

    private ServiceChainId(URI uri) {
        super(uri);
    }

    /**
     * Creates a service chain ID using the supplied URI.
     *
     * @param uri URI
     * @return GenericId
     */
    public static GenericId id(URI uri) {
        return new ServiceChainId(uri);
    }

    /**
     * Creates a service chain ID using the supplied URI string.
     *
     * @param uriString URI string
     * @return GenericId
     */
    public static GenericId id(String uriString) {
        return id(URI.create(uriString));
    }

}