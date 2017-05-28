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

import org.onosproject.metron.impl.processing.ModifierBlock;
import org.onosproject.metron.impl.processing.ProcessingBlock;

/**
 * Modifier block that decrements the
 * IP time to live field of each packet.
 */
public class DecIpTtl extends ModifierBlock {

    private boolean active;
    private boolean calculateChecksum;

    private static final String ACTIVE = "ACTIVE";
    private static final String CALCULATE_CHECKSUM = "CALC_CHECKSUM";

    private static final boolean DEF_ACTIVITY      = true;
    private static final boolean DEF_CALC_CHECKSUM = true;

    public DecIpTtl(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.active = DEF_ACTIVITY;
        this.calculateChecksum = DEF_CALC_CHECKSUM;
    }

    public DecIpTtl(
            String  id,
            String  conf,
            String  confFile,
            boolean active,
            boolean calculateChecksum) {
        super(id, conf, confFile);

        this.active = active;
        this.calculateChecksum = calculateChecksum;
    }

    /**
     * Returns whether the element is active or not.
     *
     * @return boolean activity mode
     */
    public boolean active() {
        return this.active;
    }

    /**
     * (Un)Sets the activity of the element.
     *
     * @param active boolean activity mode
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns whether the element calculates the IP checksum
     * after decrementing the IP TTl field.
     *
     * @return boolean checksum calculation
     */
    public boolean calculateChecksum() {
        return this.calculateChecksum;
    }

    /**
     * (Un)Sets the checksum calculation mode.
     *
     * @param calculateChecksum boolean checksum calculation
     */
    public void setChecksumCalculation(boolean calculateChecksum) {
        this.calculateChecksum = calculateChecksum;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.DEC_IP_TTL;
    }

    @Override
    public void populateConfiguration() {
        Object val = this.configurationMap().get(ACTIVE);
        if (val != null) {
            this.setActive(
                Boolean.valueOf(val.toString())
            );
        } else {
            this.setActive(DEF_ACTIVITY);
        }

        val = this.configurationMap().get(CALCULATE_CHECKSUM);
        if (val != null) {
            this.setChecksumCalculation(
                Boolean.valueOf(val.toString())
            );
        } else {
            this.setChecksumCalculation(DEF_CALC_CHECKSUM);
        }
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new DecIpTtl(
            id,
            this.configuration(),
            this.configurationFile(),
            this.active(),
            this.calculateChecksum()
        );
    }

}