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

/**
 * A set of metrics to be projected.
 */
public enum MetricType {

    /**
     * CPU Cores of an NFV server.
     * We assume that maximum is 16.
     */
    CPU_0,
    CPU_1,
    CPU_2,
    CPU_3,
    CPU_4,
    CPU_5,
    CPU_6,
    CPU_7,
    CPU_8,
    CPU_9,
    CPU_10,
    CPU_11,
    CPU_12,
    CPU_13,
    CPU_14,
    CPU_15,

    /**
     * Time to synthesize a service chain.
     */
    SYNTHESIS_TIME,

    /**
     * Time to deploy a service chain.
     */
    DEPLOYMENT_TIME,

    /**
     * Time to monitor a service chain.
     */
    MONITORING_TIME,

    /**
     * Time to reconfigure a service chain.
     */
    RECONFIGURATION_TIME

}
