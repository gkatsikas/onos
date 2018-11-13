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

import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.impl.processing.ProcessingBlock;

/**
 * Block for injecting packets into Snort DPI.
 */
public class FromSnortDevice extends FromBlackboxDevice {

    /**
     * Snort requires ring buffers to
     * communicate with DPDK.
     */
    private String fromRing;
    private String toRing;
    private String toReverseRing;

    public static final String FROM_RING = "FROM_RING";
    public static final String TO_RING   = "TO_RING";
    public static final String TO_REVERSE_RING = "TO_REVERSE_RING";

    public FromSnortDevice(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.fromRing = "";
        this.toRing   = "";
        this.toReverseRing = "";
    }

    public FromSnortDevice(
            String id,
            String conf,
            String confFile,
            String devName,
            String method,
            short  burst,
            String exec,
            String args,
            String fromRing,
            String toRing,
            String toReverseRing) {
        super(id, conf, confFile, devName, method, burst, exec, args);

        this.fromRing = fromRing;
        this.toRing   = toRing;
        this.toReverseRing = toReverseRing;
    }

    /**
     * Returns the forward ring name.
     *
     * @return forward ring name
     */
    public String fromRing() {
        return this.fromRing;
    }

    /**
     * Sets the forward ring name.
     *
     * @param fromRing forward ring name
     */
    public void setFromRing(String fromRing) {
        this.fromRing = fromRing;
    }

    /**
     * Returns the backward ring name.
     *
     * @return backward ring name
     */
    public String toRing() {
        return this.toRing;
    }

    /**
     * Sets the backward ring name.
     *
     * @param toRing backward ring name
     */
    public void setToRing(String toRing) {
        this.toRing = toRing;
    }

    /**
     * Returns the reverse backward ring name.
     *
     * @return reverse backward ring name
     */
    public String toReverseRing() {
        return this.toReverseRing;
    }

    /**
     * Sets the reverse backward ring name.
     *
     * @param toReverseRing reverse backward ring name
     */
    public void setToReverseRing(String toReverseRing) {
        this.toReverseRing = toReverseRing;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.FROM_SNORT_DEVICE;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(FROM_RING);
        if (val != null) {
            this.setFromRing(val.toString());
        }

        val = this.configurationMap().get(TO_RING);
        if (val != null) {
            this.setToRing(val.toString());
        }

        val = this.configurationMap().get(TO_REVERSE_RING);
        if (val != null) {
            this.setToReverseRing(val.toString());
        }
    }

    @Override
    public String fullConfiguration() {
        return super.fullConfiguration();
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new FromSnortDevice(
            id,
            this.configuration(),
            this.configurationFile(),
            this.devName(),
            this.method(),
            this.burst(),
            this.executable(),
            this.arguments(),
            this.fromRing(),
            this.toRing(),
            this.toReverseRing()
        );
    }

}
