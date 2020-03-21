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

import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.codec.impl.FlowRuleCodec;
import org.onosproject.codec.impl.MockCodecContext;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;

import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.onosproject.net.NetTestTools.APP_ID;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.Matchers.notNullValue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Unit test for OpenFlowRuleConfiguration.
 */
public class OpenFlowRuleConfigurationTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String NON_EXISTENT_RULE_FILE    = "foo.json";
    private static final String INVALID_RULE_HEADER_FILE  = "openflow/invalid-rule-header.json";
    private static final String INVALID_RULE_CONTENT_FILE = "openflow/invalid-rule-content.json";
    private static final String VALID_RULE_FILE           = "openflow/valid-rule.json";

    private String elemId;
    private ApplicationId appId;

    private File invalidCfgHdrFile;
    private File invalidCfgConFile;
    private File validCfgFile;

    private OpenFlowRuleConfiguration badCfgLoader;
    private OpenFlowRuleConfiguration invalidCfgHdrLoader;
    private OpenFlowRuleConfiguration invalidCfgConLoader;
    private OpenFlowRuleConfiguration validCfgLoader;

    private MockCodecContext    context;
    private JsonCodec<FlowRule> flowRuleCodec;
    private final CoreService   mockCoreService = createMock(CoreService.class);

    private final Logger log = getLogger(getClass());

    /**
     * Method to set up the test environment with several test files.
     *
     * @throws IOException if the resources cannot be processed.
     */
    @Before
    public void setUp() throws IOException {
        badCfgLoader        = null;
        invalidCfgHdrLoader = null;
        invalidCfgConLoader = null;
        validCfgLoader      = null;

        elemId = "FakeElement";
        appId = new TestApplicationId(0, "OpenFlowRuleConfigurationTest");

        InputStream jsonStream;

        invalidCfgHdrFile = testFolder.newFile("temp1.json");
        jsonStream = OpenFlowRuleConfigurationTest.class.getResourceAsStream(
            INVALID_RULE_HEADER_FILE
        );
        FileUtils.copyInputStreamToFile(jsonStream, invalidCfgHdrFile);

        invalidCfgConFile = testFolder.newFile("temp2.json");
        jsonStream = OpenFlowRuleConfigurationTest.class.getResourceAsStream(
            INVALID_RULE_CONTENT_FILE
        );
        FileUtils.copyInputStreamToFile(jsonStream, invalidCfgConFile);

        validCfgFile = testFolder.newFile("temp3.json");
        jsonStream = OpenFlowRuleConfigurationTest.class.getResourceAsStream(
            VALID_RULE_FILE
        );
        FileUtils.copyInputStreamToFile(jsonStream, validCfgFile);

        /**
         * Emulate a core service that registers the application
         * and provides a codec context
         */
        context = new MockCodecContext();
        flowRuleCodec = context.codec(FlowRule.class);
        assertThat(flowRuleCodec, notNullValue());
        expect(mockCoreService.registerApplication(FlowRuleCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        expect(mockCoreService.getAppId(anyShort())).andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Checks whether loadConfiguration throws an I/O
     * exception in the case of non existent file.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the FlowRule template (unlikely to be thrown here)
     * @throws Exception if the IOException is not caught
     */
    @Test
    public void badFile() throws IOException, IllegalArgumentException, Exception {
        try {
            badCfgLoader = new OpenFlowRuleConfiguration(
                appId, elemId, NON_EXISTENT_RULE_FILE
            );
            badCfgLoader.loadConfiguration();
        } catch (IOException ioEx) {
            log.info("Successfully caught bad file name {}", NON_EXISTENT_RULE_FILE);
            return;
        } catch (IllegalArgumentException iaEx) {
            /**
             * It should not happen because the file does not exist,
             * hence an I/O exception must be thrown first.
             */
        }

        throw new Exception("Unable to detect non-existent input file");
    }

    /**
     * Checks whether an input rule configurations with incorrect
     * header attribute is properly detected by loadConfiguration.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the FlowRule template
     * @throws Exception if the IllegalArgumentException is not caught
     */
    @Test
    public void testInvalidHeaderConfiguration() throws IOException, IllegalArgumentException, Exception {
        invalidCfgHdrLoader = new OpenFlowRuleConfiguration(
            appId, elemId, invalidCfgHdrFile
        );
        assertNotNull("Incorrect construction", invalidCfgHdrLoader);

        try {
            invalidCfgHdrLoader.loadConfiguration();
        } catch (IOException ioEx) {
            // It should not happen because the file exists
        } catch (IllegalArgumentException iaEx) {
            log.info("Successfully caught format exception for {}", invalidCfgHdrFile.getPath());
            return;
        }

        throw new Exception("Unable to detect malformed header attribute");
    }

    /**
     * Checks whether an input rule configurations with correct header attribute,
     * but incorrect rule structure is properly detected by loadConfiguration.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the FlowRule template
     * @throws Exception if the IllegalArgumentException is not caught
     */
    @Test
    public void testInvalidContentConfiguration() throws IOException, IllegalArgumentException, Exception {
        invalidCfgConLoader = new OpenFlowRuleConfiguration(
            appId, elemId, invalidCfgConFile
        );
        assertNotNull("Incorrect construction", invalidCfgConLoader);

        try {
            invalidCfgConLoader.loadConfiguration();
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Piece of code that emulates loadRules (suggestions to improve are necessary!)
        ArrayNode arrayNode = invalidCfgConLoader.rulesArray();
        assertThat(arrayNode, notNullValue());

        /**
         * Since the content of the rule is malformed,
         * we expect an IllegalArgumentException to be thrown and caught.
         */
        try {
            List<FlowRule> rules = flowRuleCodec.decode(arrayNode, context);
        } catch (IllegalArgumentException iaEx) {
            log.info("Successfully caught format exception for {}", invalidCfgConFile.getPath());
            return;
        }

        // If the exception os not caught, then the test fails.
        throw new Exception("Unable to detect malformed rule");
    }

    /**
     * Checks that a valid OpenFlowRuleConfiguration
     * is properly constructed and loaded.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the template of a FlowRule
     */
    @Test
    public void testValidConfiguration() throws IOException, IllegalArgumentException {
        validCfgLoader = new OpenFlowRuleConfiguration(
            appId, elemId, validCfgFile
        );
        assertNotNull("Incorrect construction", validCfgLoader);

        try {
            validCfgLoader.loadConfiguration();
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Piece of code that emulates loadRules (suggestions to improve are necessary!)
        ArrayNode arrayNode = validCfgLoader.rulesArray();
        assertThat(arrayNode, notNullValue());

        // We expect the rule to be loaded correctly
        try {
            List<FlowRule> rules = flowRuleCodec.decode(arrayNode, context);
            // ...and the list size to be non empty
            assertThat(rules, notNullValue());
            assertFalse("List of rules must not be empty", rules.isEmpty());
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }
    }

    /**
     * Method to clean up the test environment.
     *
     * @throws IOException if the resource cannot be processed.
     */
    @After
    public void cleanUp() throws IOException {
        testFolder.delete();
    }

    public class TestApplicationId extends DefaultApplicationId {
        public TestApplicationId(int id, String name) {
            super(id, name);
        }
    }

}
