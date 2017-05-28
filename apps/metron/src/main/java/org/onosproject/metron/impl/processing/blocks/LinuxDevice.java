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

package org.onosproject.metron.impl.processing.blocks;

import org.onosproject.metron.api.processing.TerminalStage;

import com.google.common.base.Strings;

import java.util.List;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Terminal block that represents a Linux-based network device.
 */
public abstract class LinuxDevice extends Device {

    protected String method;

    protected static final String DEV_NAME = "DEVNAME";
    protected static final String METHOD   = "METHOD";

    protected static final String DEF_DEV_NAME = "eno1";
    protected static final String DEF_METHOD   = "LINUX";

    private static final List<String> SUPPORTED_METHODS = Arrays.asList(
        "LINUX", "PCAP"
    );

    public LinuxDevice(
            String id,
            String conf,
            String confFile,
            TerminalStage stage) {
        super(id, conf, confFile, stage);

        this.method = DEF_METHOD;
    }

    public LinuxDevice(
            String id,
            String conf,
            String confFile,
            TerminalStage stage,
            String devName) {
        super(id, conf, confFile, stage, devName);

        this.method = DEF_METHOD;
    }

    public LinuxDevice(
            String id,
            String conf,
            String confFile,
            TerminalStage stage,
            String devName,
            String method,
            short  burst) {
        super(id, conf, confFile, stage, devName, burst);

        checkArgument(
            !Strings.isNullOrEmpty(method) && isValidMethod(method),
            "[" + this.processingBlockClass() +
            "] Invalid input method"
        );

        this.method  = method;
    }

    /**
     * Returns the I/O method of this device.
     *
     * @return I/O method
     */
    public String method() {
        return this.method;
    }

    /**
     * Sets the I/O method of this device.
     *
     * @param method I/O method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns whether the input I/O method is supported or not.
     *
     * @param method I/O method
     * @return boolean status
     */
    public static boolean isValidMethod(String method) {
        return SUPPORTED_METHODS.contains(method.toUpperCase());
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(DEV_NAME);
        if (val != null) {
            this.setDevName(
                val.toString()
            );
        } else {
            this.setDevName(DEF_DEV_NAME);
        }

        val = this.configurationMap().get(METHOD);
        if (val != null) {
            this.setMethod(
                val.toString()
            );
        } else {
            this.setMethod(DEF_METHOD);
        }
    }

}
