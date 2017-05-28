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

package org.onosproject.metron.impl.processing;

import org.onosproject.metron.api.common.UUIdGenerator;
import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockType;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of an abstract packet processing block.
 */
public abstract class ProcessingBlock implements ProcessingBlockInterface {

    protected static final Logger log = getLogger(ProcessingBlock.class);

    protected static final String FILE_CONFIG_INDICATOR = "FILE";

    protected String id;
    protected int portCount;
    protected List<Integer> ports = new ArrayList<Integer>();
    protected ProcessingBlockType type;

    protected String configuration;
    protected String configurationFile;
    protected Map<String, Object> configurationMap = new ConcurrentHashMap<String, Object>();

    protected boolean cloned = false;
    protected ProcessingBlockInterface original = null;

    protected ProcessingBlock(String id, String conf, String confFile) {
        checkArgument(
            !Strings.isNullOrEmpty(id),
            "Cannot construct processing block with NULL or empty ID"
        );

        checkNotNull(
            conf,
            "Cannot construct processing block with NULL configuration"
        );

        this.id = id;
        this.portCount = 0;
        this.configuration = conf;
        this.configurationFile = confFile;

        this.parseConfiguration();
    }

    public static ProcessingBlockInterface copy(ProcessingBlock other) {
        ProcessingBlock clone = other.spawn(other.id());
        return clone;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public List<Integer> ports() {
        return this.ports;
    }

    @Override
    public ProcessingBlockType processingBlockType() {
        return this.type;
    }

    public void setProcessingBlockType(ProcessingBlockType type) {
        this.type = type;
    }

    @Override
    public Map<String, Object> configurationMap() {
        return this.configurationMap;
    }

    @Override
    public String configuration() {
        return this.configuration;
    }

    public void setConfiguration(String conf) {
        checkArgument(!Strings.isNullOrEmpty(conf));
        this.configuration = conf;
    }

    @Override
    public String configurationFile() {
        return this.configurationFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ProcessingBlock) {
            ProcessingBlock that = (ProcessingBlock) obj;
            if (Objects.equals(this.id,               that.id) &&
                Objects.equals(this.portCount,        that.portCount) &&
                Objects.equals(this.type,             that.type) &&
                Objects.equals(this.ports,            that.ports) &&
                Objects.equals(this.configuration,    that.configuration) &&
                Objects.equals(this.configurationMap, that.configurationMap)
            ) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.id,
            this.portCount,
            this.ports,
            this.type,
            this.configuration,
            this.configurationMap
        );
    }

    @Override
    public ProcessingBlockInterface clone() {
        String newId;
        if (this.cloned) {
            newId = String.format(
                "%s_%s", this.id().substring(0, this.id().indexOf("_")),
                UUIdGenerator.getSystemInstance().getUUId().toString()
            );
        } else {
            newId = String.format(
                "%s_%s", this.id(), UUIdGenerator.getSystemInstance().getUUId().toString()
            );
        }

        ProcessingBlock clone = this.spawn(newId);
        if (this.cloned) {
            clone.original = this.original;
        } else {
            clone.original = this;
        }

        clone.cloned = true;

        return clone;
    }

    @Override
    public boolean isClone() {
        return this.cloned;
    }

    @Override
    public ProcessingBlockInterface originalInstance() {
        return this.original;
    }

    /**
     * Parses a block's configuration line, looking for a specific string.
     * Upon success, the value associated with this string is returned.
     *
     * @param line the configuration line to be parsed
     * @param param the keyword we are looking for
     * @return the value associated with the keyword
     */
    protected String searchLineForParameter(String line, String param) {
        if (!line.contains(param)) {
            return "";
        }

        String value = line.substring(line.indexOf(param) + 1, line.length());

        return value.trim();
    }

    /**
     * Splits the block's comma-separated configuration into tokens.
     * Each token is a separate configuration parameter.
     *
     * @return array of tokens that comprise the configuration
     */
    protected String[] tokenizeConfiguration() {
        return this.configuration.split("\\s*,\\s*");
    }

    /**
     * Splits the block's configuration parameter into tokens.
     *
     * @param line to be split in tokens
     * @return array of tokens that comprise the configuration parameter
     */
    protected String[] tokenizeConfigurationLine(String line) {
        if (line.isEmpty()) {
            return new String[0];
        }

        return line.split("\\s+");
    }

    @Override
    public void parseConfiguration() {
        String[] tokens = this.tokenizeConfiguration();
        if (tokens.length == 0) {
            return;
        }

        for (String line : tokens) {

            log.debug("Configuration line: {}", line);

            if (line.isEmpty() || line.trim().equals(FILE_CONFIG_INDICATOR)) {
                continue;
            }

            String[] paramTokens = tokenizeConfigurationLine(line);

            // Typical PARAM value style
            if (paramTokens.length == 2) {
                String param = paramTokens[0];
                String value = paramTokens[1];

                log.debug("\tConfiguration parameter {} with value {}", param, value);

                this.configurationMap.put(param, value);
            // This is a stateful IP/TCP/UDPRewriter
            } else if (paramTokens.length == 7) {
                // Do nothing, it is ready for parsing
                continue;
            // Blackbox NFs might have custom configuration
            } else {
                // Keep the first token as identifier (i.e., ARGS)
                String param = paramTokens[0];
                // Consider the remaining tokens as arguments
                String args = "";
                for (short i = 1; i < paramTokens.length; i++) {
                    args += " " + paramTokens[i];
                }

                log.debug("\tConfiguration parameter {} with  args {}", param, args);

                this.configurationMap.put(param, args);
            }
        }
    }

    @Override
    public String toString() {
        return this.processingBlockClass() + "(" + this.configuration + ")";
    }

    protected int addPort() {
        this.portCount++;
        this.ports.add(this.portCount);

        return this.portCount;
    }

    /**
     * Returns the class of this processing block.
     *
     * @return the class of this processing block
     */
    public abstract ProcessingBlockClass processingBlockClass();
    /**
     * Populates the configuration to the inheriting blocks' members.
     */
    public abstract void populateConfiguration();
    /**
     * Spawns a new processing block.
     *
     * @param id the id of the block
     * @return a new processing block
     */
    protected abstract ProcessingBlock spawn(String id);

    public abstract static class Builder implements ProcessingBlockInterface.Builder {

        protected String id;
        protected int portCount;
        protected List<Integer> ports = new ArrayList<Integer>();
        protected ProcessingBlockType type;
        protected Map<String, Object> configurationMap = new ConcurrentHashMap<String, Object>();
        protected String configuration;
        protected String configurationFile;

        protected boolean cloned = false;
        protected ProcessingBlockInterface original = null;

        protected Builder() {
            this.id = "";
            this.portCount = 0;
            this.configuration = "";
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder addPort() {
            this.portCount++;
            this.ports.add(this.portCount);
            return this;
        }

        public Builder setConfiguration(String conf) {
            this.configuration = conf;
            return this;
        }

        public Builder setConfigurationFile(String confFile) {
            this.configurationFile = confFile;
            return this;
        }

        public abstract ProcessingBlock build();

    }

}
