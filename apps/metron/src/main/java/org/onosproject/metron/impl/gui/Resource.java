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

package org.onosproject.metron.impl.gui;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static org.onosproject.metron.impl.gui.MetricType.SYNTHESIS_TIME;
import static org.onosproject.metron.impl.gui.MetricType.DEPLOYMENT_TIME;
import static org.onosproject.metron.impl.gui.MetricType.MONITORING_TIME;
import static org.onosproject.metron.impl.gui.MetricType.RECONFIGURATION_TIME;

/**
 * A set of resource types used in control plane.
 */
public final class Resource {

    private Resource() {}

    /**
     * A collection of metrics related to the deployment of service chains.
     */
    public static final Set<MetricType> SERVICE_CHAIN_DEPL_METRICS =
        ImmutableSet.of(
            SYNTHESIS_TIME, DEPLOYMENT_TIME, MONITORING_TIME, RECONFIGURATION_TIME
        );

}
