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

package org.onosproject.metron.processing.blocks;

import org.onosproject.metron.api.processing.ProcessingBlockClass;

import org.onosproject.metron.processing.ProcessingBlock;

/**
 * Transparent block that sets the IP annotation of each packet
 * and checks for the validity of the checksum.
 */
public class CheckIpHeader extends MarkIpHeader {

    private boolean checksum;

    private static final String CHECKSUM = "CHECKSUM";

    private static final boolean DEF_CHECKSUM = true;

    public CheckIpHeader(
            String id,
            String conf,
            String confFile) {
        super(id, conf, confFile);

        this.checksum = DEF_CHECKSUM;
    }

    public CheckIpHeader(
            String  id,
            String  conf,
            String  confFile,
            int     offset,
            boolean checksum) {
        super(id, conf, confFile, offset);

        this.checksum = checksum;
    }

    /**
     * Returns whether this element checks the
     * validity of the IP checksum.
     *
     * @return boolean checksum check
     */
    public boolean checksum() {
        return this.checksum;
    }

    /**
     * Returns whether this element checks the
     * validity of the IP checksum.
     *
     * @param checksum boolean checksum check
     */
    public void setChecksum(boolean checksum) {
        this.checksum = checksum;
    }

    @Override
    public ProcessingBlockClass processingBlockClass() {
        return ProcessingBlockClass.CHECK_IP_HEADER;
    }

    @Override
    public void populateConfiguration() {
        super.populateConfiguration();

        Object val = this.configurationMap().get(CHECKSUM);
        if (val != null) {
            this.setChecksum(Boolean.valueOf(val.toString()));
            this.configurationMap().remove(CHECKSUM);
        } else {
            this.setChecksum(DEF_CHECKSUM);
        }
    }

    @Override
    public String fullConfiguration() {
        return "CheckIPHeader(" +
                    OFFSET   + " " + offset() + ", " +
                    CHECKSUM + " " + (this.checksum() ? "true" : "false") +
                ")";
    }

    @Override
    protected ProcessingBlock spawn(String id) {
        return new CheckIpHeader(
            id,
            this.configuration(),
            this.configurationFile(),
            this.offset(),
            this.checksum()
        );
    }

}