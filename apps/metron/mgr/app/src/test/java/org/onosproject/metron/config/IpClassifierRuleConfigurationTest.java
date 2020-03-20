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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.onosproject.net.NetTestTools.APP_ID;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.Matchers.notNullValue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Unit test for IpClassifierRuleConfiguration.
 */
public class IpClassifierRuleConfigurationTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String NON_EXISTENT_RULE_FILE         = "foo.json";
    private static final String INVALID_RULE_HEADER_FILE       = "ipclassifier/invalid-rule-header.json";
    private static final String INVALID_RULE_CONTENT_TYPO_FILE = "ipclassifier/invalid-rule-content-typo.json";
    private static final String INVALID_RULE_CONTENT_INVL_FILE = "ipclassifier/invalid-rule-content-invl.json";
    private static final String VALID_RULE_FILE                = "ipclassifier/valid-rule.json";

    private String elemId;
    private ApplicationId appId;

    private File invalidCfgHdrFile;
    private File invalidCfgConTypoFile;
    private File invalidCfgConInvlFile;
    private File validCfgFile;

    private IpClassifierRuleConfiguration badCfgLoader;
    private IpClassifierRuleConfiguration invalidCfgHdrLoader;
    private IpClassifierRuleConfiguration invalidCfgConTypoLoader;
    private IpClassifierRuleConfiguration invalidCfgConInvlLoader;
    private IpClassifierRuleConfiguration validCfgLoader;

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
        badCfgLoader            = null;
        invalidCfgHdrLoader     = null;
        invalidCfgConTypoLoader = null;
        invalidCfgConInvlLoader = null;
        validCfgLoader          = null;

        elemId = "FakeElement";
        appId = new TestApplicationId(0, "IpClassifierRuleConfigurationTest");

        InputStream jsonStream;

        invalidCfgHdrFile = testFolder.newFile("temp1.json");
        jsonStream = IpClassifierRuleConfigurationTest.class.getResourceAsStream(
            INVALID_RULE_HEADER_FILE
        );
        FileUtils.copyInputStreamToFile(jsonStream, invalidCfgHdrFile);

        invalidCfgConTypoFile = testFolder.newFile("temp2.json");
        jsonStream = IpClassifierRuleConfigurationTest.class.getResourceAsStream(
            INVALID_RULE_CONTENT_TYPO_FILE
        );
        FileUtils.copyInputStreamToFile(jsonStream, invalidCfgConTypoFile);

        invalidCfgConInvlFile = testFolder.newFile("temp3.json");
        jsonStream = IpClassifierRuleConfigurationTest.class.getResourceAsStream(
            INVALID_RULE_CONTENT_INVL_FILE
        );
        FileUtils.copyInputStreamToFile(jsonStream, invalidCfgConInvlFile);

        validCfgFile = testFolder.newFile("temp4.json");
        jsonStream = IpClassifierRuleConfigurationTest.class.getResourceAsStream(
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
            badCfgLoader = new IpClassifierRuleConfiguration(
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
        invalidCfgHdrLoader = new IpClassifierRuleConfiguration(
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
     * but typos in a rule is properly detected by loadRules.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the FlowRule template
     * @throws Exception if the RuntimeException is not caught
     */
    @Test
    public void testInvalidContentTypoConfiguration() throws IOException, IllegalArgumentException, Exception {
        invalidCfgConTypoLoader = new IpClassifierRuleConfiguration(
            appId, elemId, invalidCfgConTypoFile
        );
        assertNotNull("Incorrect construction", invalidCfgConTypoLoader);

        try {
            invalidCfgConTypoLoader.loadConfiguration();
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Load rules should not work because of typos in the syntax
        try {
            invalidCfgConTypoLoader.loadRules();
        } catch (RuntimeException rtEx) {
            log.info(
                "Successfully caught typo in a rule in {}",
                invalidCfgConTypoFile.getPath()
            );
            return;
        }

        // If the exception is not caught, then the test fails.
        throw new Exception("Unable to detect typos in rule");
    }

    /**
     * Checks whether an input rule configurations with correct header attribute,
     * but invalid combination of header fields is properly detected by loadRules.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the FlowRule template
     * @throws Exception if the RuntimeException is not caught
     */
    @Test
    public void testInvalidContentInvlConfiguration() throws IOException, IllegalArgumentException, Exception {
        invalidCfgConInvlLoader = new IpClassifierRuleConfiguration(
            appId, elemId, invalidCfgConInvlFile
        );
        assertNotNull("Incorrect construction", invalidCfgConInvlLoader);

        try {
            invalidCfgConInvlLoader.loadConfiguration();
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Load rules should not work because of bad combination of header fields
        try {
            invalidCfgConInvlLoader.loadRules();
        } catch (RuntimeException rtEx) {
            log.info(
                "Successfully caught invalid combination of header fields in a rule in {}",
                invalidCfgConInvlFile.getPath()
            );
            return;
        }

        // If the exception is not caught, then the test fails.
        throw new Exception("Unable to detect invalid combinations of header fields in rule");
    }

    /**
     * Checks that a valid IpClassifierRuleConfiguration
     * is properly constructed and loaded.
     *
     * @throws IOException if the configuration cannot be loaded
     * @throws IllegalArgumentException if the configuration does
     *         not follow the template of a FlowRule
     */
    @Test
    public void testValidConfiguration() throws IOException, IllegalArgumentException {
        validCfgLoader = new IpClassifierRuleConfiguration(
            appId, elemId, validCfgFile
        );
        assertNotNull("Incorrect construction", validCfgLoader);

        // Load configuration.
        try {
            validCfgLoader.loadConfiguration();
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        // Load rules should work without problem, since the rules are following the syntax.
        try {
            validCfgLoader.loadRules();
        } catch (RuntimeException rtEx) {
            throw rtEx;
        }

        log.info(
            "Successfully loaded the configuration and rules of the valid file {}",
            validCfgFile.getPath()
        );
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
