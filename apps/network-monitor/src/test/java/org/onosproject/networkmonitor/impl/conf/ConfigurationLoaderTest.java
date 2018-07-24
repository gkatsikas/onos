/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.networkmonitor.impl.conf;

import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfiguration;

import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.net.config.InvalidFieldException;
import org.onosproject.net.config.ConfigApplyDelegate;

import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for Network Monitor configuration loader.
 */
public class ConfigurationLoaderTest {

    /**
     * {@link ServiceDirectory} to be used by BaseConfig.
     */
    protected static TestServiceDirectory directory;

    /**
     * No-op ConfigApplyDelegate.
     */
    protected final ConfigApplyDelegate noopDelegate = cfg -> { };

    private static ServiceDirectory original;

    private ApplicationId appId;
    private ObjectMapper mapper;

    /**
     * Some test JSON files.
     */
    private static final String INVALID_CONF_HEADER_FILE         = "invalid-conf-header.json";
    private static final String INVALID_CONF_CONTENT_TYPO_1_FILE = "invalid-conf-content-typo-1.json";
    private static final String INVALID_CONF_CONTENT_TYPO_2_FILE = "invalid-conf-content-typo-2.json";
    private static final String INVALID_CONF_CONTENT_TYPO_3_FILE = "invalid-conf-content-typo-3.json";
    private static final String INVALID_CONF_CONTENT_STRUCT_FILE = "invalid-conf-content-struct.json";
    private static final String VALID_CONF_FILE                  = "valid-conf.json";
    private static final String APPS                             = "apps";

    /**
     * JSON nodes where the above files will be loaded.
     */
    private JsonNode invalidConfHeaderNode;
    private JsonNode invalidConfContentTypoNode;
    private JsonNode invalidConfContentStructNode;
    private JsonNode validConfNode;

    @BeforeClass
    public static void setUpBaseConfigClass() throws TestUtilsException {
        directory = new TestServiceDirectory();

        CodecManager codecService = new CodecManager();
        codecService.activate();
        directory.add(CodecService.class, codecService);

        // Replace service directory used by BaseConfig
        original = TestUtils.getField(BaseConfig.class, "services");
        TestUtils.setField(BaseConfig.class, "services", directory);
    }

    @AfterClass
    public static void tearDownBaseConfigClass() throws TestUtilsException {
        TestUtils.setField(BaseConfig.class, "services", original);
    }

    @Before
    public void setUp() throws Exception {

        appId = new TestApplicationId(0, getClass().toString());

        directory.add(CoreService.class, new CoreServiceAdapter() {
            @Override
            public ApplicationId getAppId(Short id) {
                return appId;
            }

            @Override
            public ApplicationId registerApplication(String name) {
                return appId;
            }
        });

        mapper = testFriendlyMapper();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Returns ObjectMapper configured for ease of testing.
     * <p>
     * It will treat all integral number node as long node.
     *
     * @return mapper
     */
    public static ObjectMapper testFriendlyMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Jackson configuration for ease of Numeric node comparison
        // - treat integral number node as long node
        mapper.enable(DeserializationFeature.USE_LONG_FOR_INTS);
        mapper.setNodeFactory(new JsonNodeFactory(false) {
            @Override
            public NumericNode numberNode(int v) {
                return super.numberNode((long) v);
            }
            @Override
            public NumericNode numberNode(short v) {
                return super.numberNode((long) v);
            }
        });

        return mapper;
    }

    /**
     * Load JSON file from resource.
     *
     * @param filename JSON file name
     * @param mapper to use to read file.
     * @return JSON node
     * @throws JsonProcessingException
     * @throws IOException
     */
    public JsonNode loadJsonFromResource(String filename, ObjectMapper mapper)
                throws JsonProcessingException, IOException {

        InputStream stream = getClass().getResourceAsStream(filename);
        JsonNode tree = mapper.readTree(stream);
        return tree;
    }

    /**
     * Checks whether an input configuration with incorrect
     * header attribute is properly detected.
     *
     * @throws IOException if the JSON file is not loaded
     * @throws JsonProcessingException if the JSON file is not loaded
     * @throws Exception if the invalid configuration header is not caught
     */
    @Test
    public void testInvalidHeaderConfiguration() throws IOException, JsonProcessingException, Exception {
        JsonNode sample = loadJsonFromResource(INVALID_CONF_HEADER_FILE, mapper);
        ConfigurationLoader loader = new ConfigurationLoader();

        try {
            invalidConfHeaderNode = sample.path(APPS)
                        .path(NetworkMonitorConfigurationManager.APP_NAME)
                        .path(NetworkMonitorConfigurationManager.CONFIG_TITLE);

            loader.init(
                appId,
                NetworkMonitorConfigurationManager.CONFIG_TITLE,
                invalidConfHeaderNode,
                mapper,
                noopDelegate
            );
        } catch (Exception ex) {
            // Successfully caught invalid header
            return;
        }

        try {
            loader.isValid();
        } catch (Exception ifEx) {
            // Successfully caught invalid JSON
            return;
        }

        throw new Exception("Unable to detect malformed header attribute");
    }

    /**
     * Checks whether an input configuration with a typo
     * in an attribute is properly detected.
     *
     * @throws IOException if the JSON file is not loaded
     * @throws JsonProcessingException if the JSON file is not loaded
     * @throws Exception if the typo is not caught
     */
    @Test
    public void testInvalidContentTypoOneConfiguration() throws IOException, JsonProcessingException, Exception {
        JsonNode sample = loadJsonFromResource(INVALID_CONF_CONTENT_TYPO_1_FILE, mapper);
        ConfigurationLoader loader = new ConfigurationLoader();

        try {
            invalidConfContentTypoNode = sample.path(APPS)
                        .path(NetworkMonitorConfigurationManager.APP_NAME)
                        .path(NetworkMonitorConfigurationManager.CONFIG_TITLE);

            loader.init(
                appId,
                NetworkMonitorConfigurationManager.CONFIG_TITLE,
                invalidConfContentTypoNode,
                mapper,
                noopDelegate
            );
        } catch (Exception ex) {
            throw new Exception("Unable to load valid JSON header configuration");
        }

        boolean valid = true;

        try {
            valid = loader.isValid();
        } catch (IllegalArgumentException | InvalidFieldException ifEx) {
            // Successfully caught invalid JSON
            return;
        }

        throw new Exception("Unable to detect typos in the configuration");
    }

    /**
     * Checks whether an input configuration with a typo
     * in an attribute is properly detected.
     *
     * @throws IOException if the JSON file is not loaded
     * @throws JsonProcessingException if the JSON file is not loaded
     * @throws Exception if the typo is not caught
     */
    @Test
    public void testInvalidContentTypoTwoConfiguration() throws IOException, JsonProcessingException, Exception {
        JsonNode sample = loadJsonFromResource(INVALID_CONF_CONTENT_TYPO_2_FILE, mapper);
        ConfigurationLoader loader = new ConfigurationLoader();

        try {
            invalidConfContentTypoNode = sample.path(APPS)
                        .path(NetworkMonitorConfigurationManager.APP_NAME)
                        .path(NetworkMonitorConfigurationManager.CONFIG_TITLE);

            loader.init(
                appId,
                NetworkMonitorConfigurationManager.CONFIG_TITLE,
                invalidConfContentTypoNode,
                mapper,
                noopDelegate
            );
        } catch (Exception ex) {
            throw new Exception("Unable to load valid JSON header configuration");
        }

        boolean valid = true;

        try {
            valid = loader.isValid();
        } catch (IllegalArgumentException | InvalidFieldException ifEx) {
            // Successfully caught invalid JSON
            return;
        }

        throw new Exception("Unable to detect typos in the configuration");
    }

    /**
     * Checks whether an input configuration with a typo
     * in an attribute is properly detected.
     *
     * @throws IOException if the JSON file is not loaded
     * @throws JsonProcessingException if the JSON file is not loaded
     * @throws Exception if the typo is not caught
     */
    @Test
    public void testInvalidContentTypoThreeConfiguration() throws IOException, JsonProcessingException, Exception {
        JsonNode sample = loadJsonFromResource(INVALID_CONF_CONTENT_TYPO_3_FILE, mapper);
        ConfigurationLoader loader = new ConfigurationLoader();

        try {
            invalidConfContentTypoNode = sample.path(APPS)
                        .path(NetworkMonitorConfigurationManager.APP_NAME)
                        .path(NetworkMonitorConfigurationManager.CONFIG_TITLE);

            loader.init(
                appId,
                NetworkMonitorConfigurationManager.CONFIG_TITLE,
                invalidConfContentTypoNode,
                mapper,
                noopDelegate
            );
        } catch (Exception ex) {
            throw new Exception("Unable to load valid JSON header configuration");
        }

        boolean valid = true;

        try {
            valid = loader.isValid();
        } catch (IllegalArgumentException | InvalidFieldException ifEx) {
            // Successfully caught invalid JSON
            return;
        }

        throw new Exception("Unable to detect typos in the configuration");
    }

    /**
     * Checks whether an input configuration with an invalid
     * attribute structure is properly detected.
     *
     * @throws IOException if the JSON file is not loaded
     * @throws JsonProcessingException if the JSON file is not loaded
     * @throws Exception if the invalid structure is not caught
     */
    @Test
    public void testInvalidContentStructure() throws IOException, JsonProcessingException, Exception {
        JsonNode sample = loadJsonFromResource(INVALID_CONF_CONTENT_STRUCT_FILE, mapper);
        ConfigurationLoader loader = new ConfigurationLoader();

        try {
            invalidConfContentStructNode = sample.path(APPS)
                        .path(NetworkMonitorConfigurationManager.APP_NAME)
                        .path(NetworkMonitorConfigurationManager.CONFIG_TITLE);

            loader.init(
                appId,
                NetworkMonitorConfigurationManager.CONFIG_TITLE,
                invalidConfContentStructNode,
                mapper,
                noopDelegate
            );
        } catch (Exception ex) {
            throw new Exception("Unable to load valid JSON header configuration");
        }

        boolean valid = true;

        try {
            valid = loader.isValid();
        } catch (IllegalArgumentException | InvalidFieldException ifEx) {
            return;
        }

        // Invalid
        assertFalse(valid);
    }

    /**
     * Checks that a valid input configuration is properly loaded.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the template of a FlowRule
     * @throws Exception if unexpected error occurs
     */
    @Test
    public void testValidConfiguration() throws IOException, IllegalArgumentException, Exception {
        JsonNode sample = loadJsonFromResource(VALID_CONF_FILE, mapper);
        ConfigurationLoader loader = new ConfigurationLoader();

        try {
            validConfNode = sample.path(APPS)
                        .path(NetworkMonitorConfigurationManager.APP_NAME)
                        .path(NetworkMonitorConfigurationManager.CONFIG_TITLE);

            loader.init(
                appId,
                NetworkMonitorConfigurationManager.CONFIG_TITLE,
                validConfNode,
                mapper,
                noopDelegate
            );
        } catch (Exception ex) {
            throw new Exception("Unable to load valid JSON header configuration");
        }

        boolean valid = true;

        try {
            valid = loader.isValid();
        } catch (IllegalArgumentException | InvalidFieldException ifEx) {
            throw ifEx;
        }

        // Valid
        assertTrue(valid);

        // Get a configuration object
        NetworkMonitorConfiguration networkMonitorConf = loader.loadNetworkMonitorConf(appId);
        // This object must not be NULL
        assertThat(networkMonitorConf, notNullValue());
        assertThat(networkMonitorConf.toString(), notNullValue());
    }

    /**
     * Emulates ONOS core application ID.
     */
    public class TestApplicationId extends DefaultApplicationId {
        public TestApplicationId(int id, String name) {
            super(id, name);
        }
    }

}
