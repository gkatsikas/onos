/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.metron.impl;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String ENABLE_SYNTHESIZER = "enableSynthesizer";
    public static final boolean ENABLE_SYNTHESIZER_DEFAULT = true;

    public static final String ENABLE_HW_OFFLOADING = "enableHwOffloading";
    public static final boolean ENABLE_HW_OFFLOADING_DEFAULT = true;

    public static final String ENABLE_AUTOSCALE = "enableAutoScale";
    public static final boolean ENABLE_AUTOSCALE_DEFAULT = false;

    public static final String SCALE_DOWN_LOAD_THRESHOLD = "scaleDownLoadThreshold";
    public static final float SCALE_DOWN_LOAD_THRESHOLD_DEFAULT = 0.25f;

    public static final String SCALE_UP_LOAD_THRESHOLD = "scaleUpLoadThreshold";
    public static final float SCALE_UP_LOAD_THRESHOLD_DEFAULT = 0.75f;

    public static final String MONITORING_PERIOD_MS = "monitoringPeriodMilli";
    public static final int MONITORING_PERIOD_MS_DEFAULT = 100;
}
