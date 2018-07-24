/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.networkmonitor.api.conf;

import org.onosproject.net.ElementId;

import java.net.URI;
import java.util.Objects;

/**
 * Immutable representation of a Network Monitor configuration identity.
 */
public class NetworkMonitorConfigurationId extends ElementId {

    /**
     * Represents either no ID, or an unspecified ID.
     */
    public static final NetworkMonitorConfigurationId NONE = id("none");

    private final URI    uri;
    private final String str;

    /**
     * Default constructor for serialization.
     */
    protected NetworkMonitorConfigurationId() {
        this.uri = null;
        this.str = null;
    }

    protected NetworkMonitorConfigurationId(URI uri) {
        this.uri = uri;
        this.str = uri.toString().toLowerCase();
    }

    /**
     * Creates a Network Monitor configuration ID using the supplied URI.
     *
     * @param uri URI
     * @return NetworkMonitorConfigurationId
     */
    public static NetworkMonitorConfigurationId id(URI uri) {
        return new NetworkMonitorConfigurationId(uri);
    }

    /**
     * Creates a NetworkMonitor configuration ID using the supplied URI string.
     *
     * @param uriString URI string
     * @return NetworkMonitorConfigurationId
     */
    public static NetworkMonitorConfigurationId id(String uriString) {
        return id(URI.create(uriString));
    }

    /**
     * Returns the backing URI.
     *
     * @return backing URI
     */
    public URI uri() {
        return uri;
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetworkMonitorConfigurationId) {
            final NetworkMonitorConfigurationId that = (NetworkMonitorConfigurationId) obj;
            return  (this.getClass() == that.getClass()) &&
                    (Objects.equals(this.str, that.str));
        }
        return false;
    }

    @Override
    public String toString() {
        return str;
    }

}
