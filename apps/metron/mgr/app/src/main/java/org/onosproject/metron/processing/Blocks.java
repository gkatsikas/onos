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

package org.onosproject.metron.processing;

import org.onosproject.metron.processing.blocks.Discard;
import org.onosproject.metron.processing.blocks.Device;
import org.onosproject.metron.processing.blocks.DpdkDevice;
import org.onosproject.metron.processing.blocks.LinuxDevice;
import org.onosproject.metron.processing.blocks.FromBlackboxDevice;
import org.onosproject.metron.processing.blocks.FromDevice;
import org.onosproject.metron.processing.blocks.FromDpdkDevice;
import org.onosproject.metron.processing.blocks.FromSnortDevice;
import org.onosproject.metron.processing.blocks.ToBlackboxDevice;
import org.onosproject.metron.processing.blocks.ToDevice;
import org.onosproject.metron.processing.blocks.ToDpdkDevice;
import org.onosproject.metron.processing.blocks.ToSnortDevice;

import java.util.List;
import java.util.Arrays;

/**
 * Useful classes of packet processing blocks.
 */
public final class Blocks {

    public static final List<?> TERMINAL_ELEMENTS = Arrays.asList(
        Discard.class,
        FromBlackboxDevice.class,
        FromDevice.class,
        FromDpdkDevice.class,
        FromSnortDevice.class,
        ToBlackboxDevice.class,
        ToDevice.class,
        ToDpdkDevice.class,
        ToSnortDevice.class
    );

    public static final List<?> DEVICE_ELEMENTS = Arrays.asList(
        Device.class,
        DpdkDevice.class,
        LinuxDevice.class,
        FromBlackboxDevice.class,
        FromDevice.class,
        FromDpdkDevice.class,
        FromSnortDevice.class,
        ToBlackboxDevice.class,
        ToDevice.class,
        ToDpdkDevice.class,
        ToSnortDevice.class
    );

    public static final List<?> INPUT_ELEMENTS = Arrays.asList(
        FromBlackboxDevice.class,
        FromDevice.class,
        FromDpdkDevice.class,
        FromSnortDevice.class
    );

    public static final List<?> OUTPUT_ELEMENTS = Arrays.asList(
        ToBlackboxDevice.class,
        ToDevice.class,
        ToDpdkDevice.class,
        ToSnortDevice.class
    );

    public static final List<?> DROP_ELEMENTS = Arrays.asList(
        Discard.class
    );

    /**
     * Utility class with private constructor.
     */
    private Blocks() {}

}
