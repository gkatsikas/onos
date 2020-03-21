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

package org.onosproject.metron.config;

import org.onosproject.metron.api.config.BasicConfigurationInterface;

import org.onosproject.core.ApplicationId;

import com.google.common.base.Strings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.util.Tools.nullIsIllegal;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Basic component responsible for automatically
 * loading a Metron rule or pattern configuration file.
 */
public abstract class BasicConfiguration implements BasicConfigurationInterface {

    protected static final Logger log = getLogger(BasicConfiguration.class);

    protected File configFile;
    protected String blockId;
    protected ApplicationId appId;

    protected ObjectNode root         = null;
    protected ArrayNode  entriesArray = null;

    protected static final String ENTRIES = "entries";
    protected static final String ENTRIES_ARRAY_REQUIRED = "Entries' array was not specified";

    protected static final String OUTPUT_PORT = "outputPort";
    protected static final String PRIORITY    = "priority";
    protected static final String ACTION      = "action";

    public BasicConfiguration(ApplicationId appId, String blockId, String configFileName) {
        checkNotNull(
            appId,
            "Specify an application ID"
        );

        checkArgument(
            !Strings.isNullOrEmpty(blockId),
            "Specify an block ID that will host the configuration"
        );

        checkArgument(
            !Strings.isNullOrEmpty(configFileName),
            "Specify a filename that contains the configuration"
        );

        this.appId      = appId;
        this.blockId    = blockId;
        this.configFile = new File(configFileName);
    }

    public BasicConfiguration(ApplicationId appId, String blockId, File configFile) {
        checkNotNull(
            appId,
            "Specify an application ID"
        );

        checkArgument(
            !Strings.isNullOrEmpty(blockId),
            "Specify an block ID that will host the configuration"
        );

        checkNotNull(
            configFile,
            "Specify a file that contains the configuration"
        );

        this.appId      = appId;
        this.blockId    = blockId;
        this.configFile = configFile;
    }

    public File configFile() {
        return this.configFile;
    }

    public String blockId() {
        return this.blockId;
    }

    public ApplicationId applicationId() {
        return this.appId;
    }

    public ObjectNode root() {
        return this.root;
    }

    public ArrayNode entriesArray() {
        return this.entriesArray;
    }

    @Override
    public void loadConfiguration() throws IOException, IllegalArgumentException {
        try {
            if (this.configFile().exists()) {
                root = (ObjectNode) new ObjectMapper().readTree(this.configFile);
                log.info("\tLoaded configuration from {}", this.configFile);
            } else {
                throw new IOException("Invalid file");
            }

        } catch (IOException ioEx) {
            log.warn(
                "\tUnable to load configuration from {}",
                this.configFile
            );
            throw ioEx;
        } catch (Exception ex) {
            log.warn(
                "\tException caught while loading {}",
                this.configFile
            );
            throw ex;
        }


        this.entriesArray = nullIsIllegal(
            (ArrayNode) root.get(ENTRIES), ENTRIES_ARRAY_REQUIRED
        );
    }

}
