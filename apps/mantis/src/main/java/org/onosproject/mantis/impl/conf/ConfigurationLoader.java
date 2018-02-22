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

package org.onosproject.mantis.impl.conf;

import org.onosproject.mantis.api.conf.ConfigurationLoaderInterface;
import org.onosproject.mantis.api.conf.DatabaseConfiguration;
import org.onosproject.mantis.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.mantis.api.conf.IngressPoint;
import org.onosproject.mantis.api.conf.MantisConfiguration;
import org.onosproject.mantis.api.conf.PredictionConfiguration;
import org.onosproject.mantis.api.prediction.PredictionMechanism;
import org.onosproject.mantis.api.prediction.PredictionMechanism.PredictionMethod;

import org.onosproject.net.config.Config;
import org.onosproject.core.ApplicationId;

import com.google.common.collect.Sets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

/**
 * Mantis configuration loader.
 */
public final class ConfigurationLoader
        extends Config<ApplicationId>
        implements ConfigurationLoaderInterface {

    private final Logger log = getLogger(getClass());

    /**
     * Mantis database configuration starts with this attribute.
     */
    private static final String DB_TITLE = "database";

    /**
     * Mantis database configuration attributes.
     */
    private static final String DB_ATTR_IP               = "ip";
    private static final String DB_ATTR_PORT             = "port";
    private static final String DB_ATTR_USER             = "username";
    private static final String DB_ATTR_PASS             = "password";
    private static final String DB_ATTR_NAME             = "databaseName";
    private static final String DB_ATTR_BATCH_WRITE      = "batchWrite";
    private static final String DB_ATTR_BATCH_WRITE_SIZE = "batchWriteSize";
    private static final String DB_ATTR_LOAD_TABLE       = "loadTable";
    private static final String DB_ATTR_PRED_TABLE       = "predictionTable";
    private static final String DB_ATTR_PRED_DELAY_TABLE = "predictionDelayTable";
    private static final String DB_ATTR_TABLE_COL        = "attribute";
    private static final String DB_ATTR_RETENTION_POLICY = "retentionPolicy";

    /**
     * Mantis prediction configuration starts with this attribute.
     */
    private static final String PRED_TITLE = "prediction";

    /**
     * Mantis prediction configuration
     * contains the following attributes.
     */
    private static final String PRED_ATTR_INGR_POINTS = "ingressPoints";
    private static final String PRED_ATTR_PRED_CONF   = "predictionConf";

    /**
     * Each ingress point should have
     * the following main attributes.
     */
    private static final String INGRESS_POINT_DEV   = "ingressDevice";
    private static final String INGRESS_POINT_PORTS = "ingressPorts";
    private static final String INGRESS_POINT_PORT  = "port";


    /**
     * Each prediction configuration
     * should have the following attributes.
     */
    private static final String PRED_CONF_MECH_LIST            = "predictionMechanisms";
    private static final String PRED_CONF_METHOD               = "predictionMethod";
    private static final String PRED_CONF_TIME_WIN_SIZE_SEC    = "timeWindowSize";
    private static final String PRED_CONF_DATA_WIN_SIZE_POINTS = "dataWindowSize";
    private static final String PRED_CONF_TRAIN_FRAC           = "trainingFraction";
    private static final String PRED_CONF_EXTRA_PARAMS         = "parameters";

    /**
     * Some prediction mechanisms might have custom configuration.
     */
    private static final String PRED_CONF_EXTRA_PARAM_EWMA_ALPHA   = "alpha";
    private static final String PRED_CONF_EXTRA_PARAM_HOL_WIN_SEAS = "seasonalPatternSec";

    private ApplicationId appId = null;

    @Override
    public boolean isValid() {
        boolean result = hasOnlyFields(DB_TITLE, PRED_TITLE);
        JsonNode dbNode = object.get(DB_TITLE);

        // Verify database configuration
        result &= this.verifyDatabaseConf(dbNode);

        JsonNode predNode = object.get(PRED_TITLE);

        // Verify prediction configuration
        try {
            result &= this.verifyPredictionConf(predNode);
        } catch (IllegalArgumentException iaEx) {
            throw iaEx;
        }

        if (!result) {
            log.error("Invalid configuration");
        }

        return result;
    }

    /**
     * Verifies the structure of the database configuration.
     *
     * @param dbNode JSON node with database configuration
     * @return boolean verification status
     */
    private boolean verifyDatabaseConf(JsonNode dbNode) {
        // Database configuration is not mandatory
        if (dbNode == null) {
            return true;
        }

        boolean result = true;

        // Check for the 4 main attributes of this configuration
        ObjectNode dbObjNode = (ObjectNode) dbNode;
        result &= hasFields(
            dbObjNode,
            DB_ATTR_IP,
            DB_ATTR_PORT,
            DB_ATTR_USER,
            DB_ATTR_PASS
        );
        checkArgument(
            result,
            "Attribute database requires 4 mandatory fields " +
            "(ip, port, username, and password)"
        );

        // If database IP is given, then verify
        JsonNode ipDbNode = dbObjNode.path(DB_ATTR_IP);
        if (ipDbNode != null) {
            result &= isIpAddress(dbObjNode, DB_ATTR_IP, OPTIONAL);
            checkArgument(result, "Invalid database IP");
        }

        // If database port is given, then verify
        JsonNode portDbNode = dbObjNode.path(DB_ATTR_PORT);
        if (portDbNode != null) {
            result &= isTpPort(dbObjNode, DB_ATTR_PORT, OPTIONAL);
            checkArgument(result, "Invalid database port");
        }

        // If database username is given, then verify
        JsonNode userDbNode = dbObjNode.path(DB_ATTR_USER);
        if (userDbNode != null) {
            result &= isString(dbObjNode, DB_ATTR_USER, OPTIONAL);
            checkArgument(result, "Invalid database username");
        }

        // If database password is given, then verify
        JsonNode passDbNode = dbObjNode.path(DB_ATTR_PASS);
        if (passDbNode != null) {
            result &= isString(dbObjNode, DB_ATTR_PASS, OPTIONAL);
            checkArgument(result, "Invalid database password");
        }

        // If database name is given, then verify
        JsonNode nameDbNode = dbObjNode.path(DB_ATTR_NAME);
        if (nameDbNode != null) {
            result &= isString(dbObjNode, DB_ATTR_NAME, OPTIONAL);
            checkArgument(result, "Invalid database name");
        }

        // If batch write is given, then verify
        JsonNode batchWriteDbNode = dbObjNode.path(DB_ATTR_BATCH_WRITE);
        if (batchWriteDbNode != null) {
            result &= isBoolean(dbObjNode, DB_ATTR_BATCH_WRITE, OPTIONAL);
            checkArgument(result, "Invalid database batch write flag");
        }

        // If batch write is given, then verify
        JsonNode batchWriteSizeDbNode = dbObjNode.path(DB_ATTR_BATCH_WRITE_SIZE);
        if (batchWriteSizeDbNode != null) {
            result &= isNumber(dbObjNode, DB_ATTR_BATCH_WRITE_SIZE, OPTIONAL);
            checkArgument(result, "Invalid database btach write size");
        }

        // If database retention policy is given, then verify
        JsonNode retPolicyDbNode = dbObjNode.path(DB_ATTR_RETENTION_POLICY);
        if (retPolicyDbNode != null) {
            result &= isString(dbObjNode, DB_ATTR_RETENTION_POLICY, OPTIONAL);
            checkArgument(result, "Invalid database retention policy");
        }

        // If load table is given, then verify
        JsonNode portStatsTableNode = dbObjNode.path(DB_ATTR_LOAD_TABLE);
        if (portStatsTableNode != null) {
            result &= isString(dbObjNode, DB_ATTR_LOAD_TABLE, OPTIONAL);
            checkArgument(result, "Invalid database load table name");
        }

        // If prediction table prefix is given, then verify
        JsonNode predictionTableNode = dbObjNode.path(DB_ATTR_PRED_TABLE);
        if (predictionTableNode != null) {
            result &= isString(dbObjNode, DB_ATTR_PRED_TABLE, OPTIONAL);
            checkArgument(result, "Invalid database prediction table prefix");
        }

        // If prediction delay table prefix is given, then verify
        JsonNode predictionDelayTableNode = dbObjNode.path(DB_ATTR_PRED_DELAY_TABLE);
        if (predictionDelayTableNode != null) {
            result &= isString(dbObjNode, DB_ATTR_PRED_DELAY_TABLE, OPTIONAL);
            checkArgument(result, "Invalid database prediction delay table prefix");
        }

        return result;
    }

    /**
     * Verifies the structure of the prediction configuration.
     *
     * @param predNode JSON node with prediction configuration
     * @return boolean verification status
     * @throws IllegalArgumentException
     */
    private boolean verifyPredictionConf(JsonNode predNode) {
        if (predNode == null) {
            final String msg = "Prediction configuration is mandatory";
            throw new IllegalArgumentException(msg);
        }

        boolean result = true;

        // Check for the 2 main attributes of the configuration
        ObjectNode predObjNode = (ObjectNode) predNode;
        result &= hasOnlyFields(
            predObjNode,
            PRED_ATTR_INGR_POINTS,
            PRED_ATTR_PRED_CONF
        );
        checkArgument(
            result,
            "Attribute prediction requires 2 mandatory fields " +
            "(list of ingressPoints and predictionConf)"
        );

        // A non-NULL JSON array is expected
        JsonNode ingressPointsNode = predObjNode.path(PRED_ATTR_INGR_POINTS);
        result &= (!ingressPointsNode.isMissingNode() && ingressPointsNode.isArray());
        checkArgument(result, "Invalid list of ingress points");

        // A non-NULL JSON node is expected
        JsonNode predictionConfNode = predObjNode.path(PRED_ATTR_PRED_CONF);
        result &= (!predictionConfNode.isMissingNode());
        checkArgument(result, "Invalid prediction configuration");

        // ... with an array object inside
        JsonNode predMechanisms = predictionConfNode.path(PRED_CONF_MECH_LIST);
        result &= (predMechanisms.isArray());
        checkArgument(result, "Invalid list of prediction mechanisms");

        for (JsonNode pm : predMechanisms) {
            ObjectNode pmNode = (ObjectNode) pm;
            result &= isString(pmNode, PRED_CONF_METHOD, MANDATORY);
            checkArgument(result, "Invalid prediction method name");
            result &= isNumber(pmNode, PRED_CONF_TIME_WIN_SIZE_SEC, MANDATORY);
            checkArgument(result, "Invalid time window size for prediction mechanism");
            result &= isNumber(pmNode, PRED_CONF_DATA_WIN_SIZE_POINTS, MANDATORY);
            checkArgument(result, "Invalid data window size for prediction mechanism");
            result &= isNumber(pmNode, PRED_CONF_TRAIN_FRAC, OPTIONAL);
        }

        return result;
    }

    /**
     * Returns Mantis configuration read from JSON.
     *
     * @return Mantis configuration
     */
    @Override
    public MantisConfiguration loadMantisConf(ApplicationId appId) {
        this.appId = appId;

        log.info("Loading configuration...");

        JsonNode dbNode = object.get(DB_TITLE);
        ObjectNode dbObjNode = (ObjectNode) dbNode;

        /**
         * A. Load the database configuration.
         */
        DatabaseConfiguration dbConf = this.loadDatabaseConf(dbObjNode);

        JsonNode predNode = object.get(PRED_TITLE);
        ObjectNode predObjNode = (ObjectNode) predNode;

        /**
         * B. Load the ingress points.
         */
        JsonNode ingressPointsNode = predObjNode.path(PRED_ATTR_INGR_POINTS);
        Set<IngressPoint> ingressPoints = this.loadIngressPoints(ingressPointsNode);

        /**
         * C. Load the prediction configuration.
         */
        JsonNode predictionConfNode = predObjNode.path(PRED_ATTR_PRED_CONF);
        PredictionConfiguration predConf = this.loadPredictionConf(predictionConfNode);

        /**
         * Build the Mantis configuration.
         */
        URI id = null;
        try {
            id = new URI(UUID.randomUUID().toString());
        } catch (URISyntaxException sEx) {
            return null;
        }
        checkNotNull(id, "Mantis configuration ID is NULL");

        MantisConfiguration.Builder mantisConfBuilder = MantisConfiguration.builder()
            .id(id)
            .databaseConfiguration(dbConf)
            .ingressPoints(ingressPoints)
            .predictionConfiguration(predConf);

        MantisConfiguration mantisConf = mantisConfBuilder.build();

        log.info("Successfuly loaded Mantis configuration: {}", mantisConf.toString());

        return mantisConf;
    }

    /**
     * Parses and outputs the database configuration
     * contained into the input JSON configuration.
     *
     * @param dbNode JSON object with the required info
     * @return database configuration object
     */
    private DatabaseConfiguration loadDatabaseConf(JsonNode dbNode) {
        // Default configuration is generated
        if (dbNode == null) {
            return new DatabaseConfiguration();
        }

        String ip       = dbNode.path(DB_ATTR_IP).asText();
        short  port     = (short) dbNode.path(DB_ATTR_PORT).asInt();
        String username = dbNode.path(DB_ATTR_USER).asText();
        String password = dbNode.path(DB_ATTR_PASS).asText();

        // Optional arguments begin
        String  databaseName        = dbNode.path(DB_ATTR_NAME).asText();
        String  retPolicyStr        = dbNode.path(DB_ATTR_RETENTION_POLICY).asText();
        RetentionPolicy retPolicy   = RetentionPolicy.getByName(retPolicyStr);
        boolean batchWrite          = dbNode.path(DB_ATTR_BATCH_WRITE).asBoolean();
        short   batchWriteSize      = (short) dbNode.path(DB_ATTR_BATCH_WRITE_SIZE).asInt();
        String loadTable            = dbNode.path(DB_ATTR_LOAD_TABLE).asText();
        String predictionTable      = dbNode.path(DB_ATTR_PRED_TABLE).asText();
        String predictionDelayTable = dbNode.path(DB_ATTR_PRED_DELAY_TABLE).asText();

        log.debug("                    IP: {}", ip);
        log.debug("                  Port: {}", port);
        log.debug("              Username: {}", username);
        log.debug("              Password: {}", password);
        log.debug("         Database name: {}", databaseName);
        log.debug("      Retention policy: {}", retPolicyStr);
        log.debug("   Batch Write Enabled: {}", batchWrite);
        log.debug("   Batch Write    Size: {}", batchWriteSize);
        log.debug("            Load table: {}", loadTable);
        log.debug("      Prediction table: {}", predictionTable);
        log.debug("Prediction delay table: {}", predictionDelayTable);

        // Build the database configuration
        DatabaseConfiguration.Builder dbConfBuilder = DatabaseConfiguration.builder()
            .predictionDelayTable(predictionDelayTable)
            .predictionTable(predictionTable)
            .loadTable(loadTable)
            .batchWriteSize(batchWriteSize)
            .batchWrite(batchWrite)
            .retentionPolicy(retPolicy)
            .databaseName(databaseName)
            .password(password)
            .username(username)
            .port(port)
            .ip(ip);

        return dbConfBuilder.build();
    }

    /**
     * Parses and outputs the set of ingress points
     * contained into the input JSON configuration.
     *
     * @param ingressPointsNode JSON array with the required info
     * @return set of IngressPoint objects
     */
    private Set<IngressPoint> loadIngressPoints(JsonNode ingressPointsNode) {
        checkNotNull(ingressPointsNode, PRED_ATTR_INGR_POINTS + " array is NULL");

        Set<IngressPoint> ingressPoints = Sets.<IngressPoint>newConcurrentHashSet();

        // Iterate the JSON array of ingress points
        for (JsonNode in : ingressPointsNode) {
            ObjectNode inNode = (ObjectNode) in;

            // Each entry of the array has ingress device ID
            String ingrDev = inNode.path(INGRESS_POINT_DEV).asText();

            // And an array of ingress ports
            JsonNode inPortsNode = inNode.path(INGRESS_POINT_PORTS);
            checkArgument(
                inPortsNode.isArray(),
                INGRESS_POINT_PORTS + " attribute is an array"
            );

            Set<Long> ports = Sets.<Long>newConcurrentHashSet();

            for (JsonNode inPort : inPortsNode) {
                ObjectNode inPortNode = (ObjectNode) inPort;
                long ingrPort  = inPortNode.path(INGRESS_POINT_PORT).asLong();
                ports.add(new Long(ingrPort));
            }

            // Build an ingress point
            IngressPoint.Builder ingressPointsBuilder = IngressPoint.builder()
                .deviceId(ingrDev)
                .portIds(ports);

            // We have an ingress point
            ingressPoints.add(ingressPointsBuilder.build());
        }

        return ingressPoints;
    }

    /**
     * Parses and outputs the prediction configuration
     * contained into the input JSON configuration.
     *
     * @param predictionConfNode JSON node with the required info
     * @return PredictionConfiguration object
     */
    private PredictionConfiguration loadPredictionConf(JsonNode predictionConfNode) {
        checkNotNull(predictionConfNode, PRED_ATTR_PRED_CONF + " attribute is NULL");

        JsonNode predMechanisms = predictionConfNode.path(PRED_CONF_MECH_LIST);

        Set<PredictionMechanism> mechanisms = Sets.<PredictionMechanism>newConcurrentHashSet();
        if (predMechanisms != null) {
            // Iterate the JSON array of prediction mechanisms
            for (JsonNode pm : predMechanisms) {
                ObjectNode pmNode = (ObjectNode) pm;

                String methodStr      = pmNode.path(PRED_CONF_METHOD).asText();
                long   timeWindowSize = (long)  pmNode.path(PRED_CONF_TIME_WIN_SIZE_SEC).asInt();
                long   dataWindowSize = (long)  pmNode.path(PRED_CONF_DATA_WIN_SIZE_POINTS).asInt();
                float  trainFraction  = (float) pmNode.path(PRED_CONF_TRAIN_FRAC).asDouble();
                JsonNode predParams   = pmNode.path(PRED_CONF_EXTRA_PARAMS);
                Map<String, Object> parameters = new ConcurrentHashMap<String, Object>();

                if (!predParams.isMissingNode()) {
                    ObjectNode predParamsObjNode = (ObjectNode) predParams;
                    PredictionMethod method = PredictionMethod.getByName(methodStr);
                    if (method != null) {
                        log.info("Custom prediction parameters available for mechanism: {}", methodStr);

                        if (PredictionMethod.isEwma(method)) {
                            checkNotNull(
                                predParams.path(PRED_CONF_EXTRA_PARAM_EWMA_ALPHA),
                                "EWMA decay factor alpha is expected"
                            );
                            checkArgument(
                                isNumber(predParamsObjNode, PRED_CONF_EXTRA_PARAM_EWMA_ALPHA, MANDATORY),
                                "EWMA decay factor alpha must be a number"
                            );
                            float alpha = (float) predParams.path(PRED_CONF_EXTRA_PARAM_EWMA_ALPHA).asDouble();
                            parameters.put(PRED_CONF_EXTRA_PARAM_EWMA_ALPHA, alpha);
                        } else if (PredictionMethod.isHoltWintersInfluxDb(method)) {
                            checkNotNull(
                                predParams.path(PRED_CONF_EXTRA_PARAM_HOL_WIN_SEAS),
                                "Holt-Winters seasonal pattern in seconds is expected"
                            );
                            checkArgument(
                                isNumber(predParamsObjNode, PRED_CONF_EXTRA_PARAM_HOL_WIN_SEAS, MANDATORY),
                                "Holt-Winters seasonal pattern in seconds must be a number"
                            );
                            int seasonal = predParams.path(PRED_CONF_EXTRA_PARAM_HOL_WIN_SEAS).asInt();
                            parameters.put(PRED_CONF_EXTRA_PARAM_HOL_WIN_SEAS, seasonal);
                        }
                    }
                }

                PredictionMechanism.Builder mechanismBuilder = PredictionMechanism.builder()
                    .parameters(parameters)
                    .method(methodStr)
                    .timeWindowSize(timeWindowSize)
                    .dataWindowSize(dataWindowSize)
                    .trainingFraction(trainFraction);

                PredictionMechanism mechanism = mechanismBuilder.build();

                if (mechanism != null) {
                    mechanisms.add(mechanism);
                }
            }
        }

        // Build the prediction configuration
        PredictionConfiguration.Builder predConfBuilder = PredictionConfiguration.builder()
            .predictionMechanisms(mechanisms);

        return predConfBuilder.build();
    }

}
