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

import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.DefaultServiceDirectory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;

import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A codec for handling rules.
 */
public class RuleCodecContext implements CodecContext {

    protected static final Logger log = getLogger(RuleCodecContext.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private static ServiceDirectory services = new DefaultServiceDirectory();

    @Override
    public ObjectMapper mapper() {
        return this.mapper;
    }

    @Override
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return getService(CodecService.class).getCodec(entityClass);
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return this.services.get(serviceClass);
    }

    protected <T> T decode(String json, Class<T> entityClass)
            throws IOException {
        try {
            return decode(mapper().readTree(json), entityClass);
        } catch (IOException ioEx) {
            throw ioEx;
        }
    }

}
