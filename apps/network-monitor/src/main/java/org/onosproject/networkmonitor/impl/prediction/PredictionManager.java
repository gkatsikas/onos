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

package org.onosproject.networkmonitor.impl.prediction;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onosproject.networkmonitor.api.common.Constants;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.networkmonitor.api.conf.IngressPoint;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfiguration;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationEvent;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationListenerInterface;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationService;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationState;
import org.onosproject.networkmonitor.api.conf.PredictionConfiguration;
import org.onosproject.networkmonitor.api.exception.DatabaseException;
import org.onosproject.networkmonitor.api.exception.PredictionException;
import org.onosproject.networkmonitor.api.monitor.MonitorService;
import org.onosproject.networkmonitor.api.monitor.WallClockNanoTimestamp;
import org.onosproject.networkmonitor.api.prediction.PredictionMechanism;
import org.onosproject.networkmonitor.api.prediction.PredictionMechanism.PredictionMethod;
import org.onosproject.networkmonitor.api.prediction.PredictionService;
import org.onosproject.networkmonitor.api.metrics.LoadMetric;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.PortNumber;

import org.influxdb.InfluxDB;
import org.influxdb.impl.InfluxDBResultMapper;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Component(immediate = false)
@Service
public class PredictionManager implements PredictionService {

    private final Logger log = getLogger(getClass());

    /**
     * ONOS application information.
     */
    public static final String APP_NAME = Constants.SYSTEM_PREFIX + ".prediction";

    /**
     * Print ID.
     */
    private static final String COMPONET_LABEL = "Prediction Manager";

    /**
     * Time interval to schedule the thread that
     * stores network load predictions to the database.
     */
    private static final int STORE_PRED_STATS_INTERVAL_SEC = 1;

    /**
     * Number of threads for sending network load
     * predictions into the dabatase.
     */
    private static final int PRED_SEND_THREADS_NB = 8;

    /**
     * Application ID for PredictionManager.
     */
    private static ApplicationId appId;

    /**
     * Prediction manager needs a database handler too.
     */
    private static volatile InfluxDB influxDB;

    /**
     * Database name.
     */
    private static String databaseName;

    /**
     * Database retention policy.
     */
    private static RetentionPolicy retentionPolicy;

    /**
     * Database tables.
     */
    private static String loadTable;
    private static String predictionTable;
    private static String predictionDelayTable;

    /**
     * Prediction mechanisms used by the Prediction manager.
     */
    private static Set<PredictionMechanism> predictionMechanisms;

    /**
     * PredictionManager receives NetworkMonitorConfiguration object with:
     * |-> Database configuration.
     * |-> Set of ingress points to be monitored.
     * |-> Prediction configuration.
     */
    private static NetworkMonitorConfiguration networkMonitorConfiguration = null;
    private static DatabaseConfiguration       databaseConfiguration       = null;
    private static Set<IngressPoint>           ingressPoints               = null;
    private static PredictionConfiguration     predictionConfiguration     = null;

    /**
     * Maps results to custom classes.
     */
    private static InfluxDBResultMapper resultMapper;

    /**
     * Indicates the state of its threads.
     */
    private static AtomicBoolean isScheduled;

    /**
     * Database tables' structure.
     */
    public static final String LOAD_TABLE_PRIM_KEY_DEV =
        DatabaseConfiguration.LOAD_TABLE_PRIM_KEY_DEV;
    public static final String LOAD_TABLE_PRIM_KEY_PORT =
        DatabaseConfiguration.LOAD_TABLE_PRIM_KEY_PORT;
    public static final String LOAD_TABLE_ATTRIBUTE_BITS =
        DatabaseConfiguration.LOAD_TABLE_ATTRIBUTE_BITS;
    public static final String LOAD_TABLE_ATTRIBUTE_PACKETS =
        DatabaseConfiguration.LOAD_TABLE_ATTRIBUTE_PACKETS;
    public static final String LOAD_TABLE_ATTRIBUTE_DROPS =
        DatabaseConfiguration.LOAD_TABLE_ATTRIBUTE_DROPS;

    /**
     * By default, a GROUP BY time() interval with no data reports NULL as its
     * value in the output column. But fill() changes the value reported for
     * time intervals that have no data. So, fill(null) is the default behavior.
     * Other fill options are: fill(none), fill(previous), fill(linear) and
     * fill(any_numerical_value).
     */
    public static final String LOAD_TABLE_AGGREGATE_FILL = "null";

    /**
     * Stores the name of the prediction table of each
     * prediction mechanism.
     */
    public static Map<PredictionMethod, String> predictionTableMap;

    /**
     * Services used by the Prediction manager.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkMonitorConfigurationService networkMonitorConfigurationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MonitorService monitorService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    /**
     * Schedule threads to store predictions.
     */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(PRED_SEND_THREADS_NB);

    /**
     * Listen to events dispatched by the NetworkMonitor configuration manager.
     * These events are related to the state of the NetworkMonitor configuration.
     */
    private final NetworkMonitorConfigurationListenerInterface networkMonitorConfigurationListener =
        new InternalNetworkMonitorConfigurationListener();

    public PredictionManager() {
        this.influxDB        = null;

        this.databaseName    = null;
        this.retentionPolicy = null;
        this.loadTable       = null;
        this.predictionTable = null;
        this.predictionMechanisms = null;
        this.predictionTableMap = new ConcurrentHashMap<PredictionMethod, String>();

        this.resultMapper = new InfluxDBResultMapper();

        this.setScheduled(false);
    }

    @Activate
    public void activate() {
        this.appId = coreService.registerApplication(APP_NAME);

        // Add a listener to catch events coming from this manager.
        networkMonitorConfigurationService.addListener(networkMonitorConfigurationListener);

        log.info("[{}] Started", label());
    }

    @Deactivate
    public void deactivate() {
        log.info("[{}] Stopped", label());

        // Remove the listener for the events coming from this manager.
        networkMonitorConfigurationService.removeListener(networkMonitorConfigurationListener);

        // Shutdown schedulers before deactivation
        unschedulePredictionsSender();

        // Join internal InfluxDB threads
        try {
            DatabaseConfiguration.stopInfluxDbThreads(
                this.influxDB,
                this.databaseConfiguration.batchWrite()
            );
        } catch (DatabaseException dbEx) {
            throw dbEx;
        }
    }

    @Override
    public void runPredictions(
            Set<IngressPoint>        ingressPoints,
            Set<PredictionMechanism> predictionMechanisms,
            String                   fromDatabaseName,
            String                   fromTableName,
            String                   toDatabaseName,
            String                   retentionPolicy)
            throws DatabaseException, PredictionException {
        if (!isReady()) {
            log.warn("[{}] Database not ready", label());
            return;
        }

        for (IngressPoint ingressPoint : ingressPoints) {
            DeviceId deviceId = ingressPoint.deviceId();

            /**
             * No such device present, skip.
             */
            if (!deviceService.isAvailable(deviceId)) {
                continue;
            }

            for (PredictionMechanism predMechanism : predictionMechanisms) {
                /**
                 * Applies predictions on the aggregate load
                 * across all ingress ports.
                 */
                List<LoadMetric> predictedLoad = null;

                WallClockNanoTimestamp startPred = null, endPred = null;
                long predictionDelay = -1;
                try {
                    // Start measuring the time it takes to predict
                    startPred = new WallClockNanoTimestamp();
                    // Predict
                    predictedLoad = this.predictDeviceLoad(
                        predMechanism, fromDatabaseName, fromTableName, deviceId
                    );
                    // Stop measuring, prediction is over
                    endPred = new WallClockNanoTimestamp();
                    // The difference between the timestamps is the prediction delay
                    predictionDelay = endPred.unixTimestamp() - startPred.unixTimestamp();
                    // Store prediction delay to database
                    insertPredictionDelay(fromDatabaseName, predictionDelayTable, retentionPolicy,
                                          Instant.now().getEpochSecond(), deviceId, predMechanism, predictionDelay);
                } catch (PredictionException predEx) {
                    throw predEx;
                }

                if (predictedLoad == null) {
                    log.warn(
                        "[{}] [Predict - {}] [Device {}] No predictions obtained",
                        label(), predMechanism.method().toString(), deviceId
                    );
                    continue;
                }
                checkArgument(predictionDelay > 0, "Prediction delay must be positive");

                log.info(
                    "[{}] [Predict - {}] [Device {}] Inserting predictions to InfluxDB",
                    label(), predMechanism.method().toString(), deviceId
                );

                // Each method has its own table
                String toPredTableName = this.predictionTableMap.get(predMechanism.method());

                try {
                    this.insertPredictionStatistics(
                        toDatabaseName,
                        toPredTableName,
                        retentionPolicy,
                        predictedLoad
                    );
                } catch (DatabaseException dbEx) {
                    throw dbEx;
                }
            }

            log.info("[{}]", label());
        }
    }

    @Override
    public List<LoadMetric> predictDeviceLoad(
            PredictionMechanism predictionMechanism,
            String              fromDatabaseName,
            String              fromTableName,
            DeviceId            deviceId)
            throws PredictionException {
        commonPrePredictionChecks(
            predictionMechanism, fromDatabaseName, fromTableName, deviceId
        );

        PredictionMethod method = predictionMechanism.method();

        // Get the timeseries data to work on
        List<LoadMetric> inputTimeseries = this.fetchInputTimeseries(
            predictionMechanism,
            fromDatabaseName,
            fromTableName,
            deviceId,
            DatabaseConfiguration.DEF_DB_GROUPING_FREQ_SEC
        );

        // SMA
        if (PredictionMethod.isSma(method)) {
            SmaPredictor.Builder smaBuilder =
                SmaPredictor.builder()
                    .inputTimeseries(inputTimeseries)
                    .dataWindowSize(predictionMechanism.dataWindowSize());

            return smaBuilder.build().predict();
        // EWMA
        } else if (PredictionMethod.isEwma(method)) {
            Float alpha = (Float) predictionMechanism.parameter(EwmaPredictor.EWMA_DECAY_PARAM_ALPHA);

            EwmaPredictor.Builder ewmaBuilder =
                EwmaPredictor.builder()
                    .inputTimeseries(inputTimeseries)
                    .dataWindowSize(predictionMechanism.dataWindowSize())
                    .alphaDecayFactor(alpha);

            return ewmaBuilder.build().predict();
        // SMA or Holt-Winters Built-in
        } else if (PredictionMethod.isBuiltIn(method)) {
            return inputTimeseries;
        } else {
            throw new PredictionException(
                "Unimplemented prediction method: " + method.toString()
            );
        }
    }

    @Override
    public List<LoadMetric> predictDeviceLoadOnPort(
            PredictionMechanism predictionMechanism,
            String              fromDatabaseName,
            String              fromTableName,
            DeviceId            deviceId,
            PortNumber          portId)
            throws PredictionException {
        commonPrePredictionChecks(
            predictionMechanism, fromDatabaseName, fromTableName, deviceId
        );
        checkNotNull(portId, "Port ID is NULL");

        return null;
    }

    @Override
    public void insertPredictionStatistics(
        String           databaseName,
        String           tableName,
        String           retentionPolicy,
        List<LoadMetric> predictedLoad) throws DatabaseException {
        if (!isReady()) {
            return;
        }

        for (LoadMetric predictedMetric: predictedLoad) {
            PortNumber portNb;
            if (predictedMetric.portId() == null) {
                // portId is null when we query for aggregate stats
                portNb = PortNumber.ALL;
            } else {
                portNb = PortNumber.portNumber(predictedMetric.portId());
            }

            // Insert prediction information into the database
            if (!monitorService.insertIntoInfluxDB(
                    databaseName,
                    tableName,
                    retentionPolicy,
                    predictedMetric.timeEpochSec(),
                    DeviceId.deviceId(predictedMetric.deviceId()),
                    portNb,
                    predictedMetric.bits(),
                    predictedMetric.packets(),
                    predictedMetric.drops())) {
                throw new DatabaseException(
                    "[" + label() + "] [InfluxDB INSERT INTO DB " + databaseName +
                    " TABLE " + tableName + "] [Device " + predictedMetric.deviceId() +
                    " - Port " + portNb.toString() + "] Failed"
                );
            }
        }
    }

    /**
     * Insert measured delay of the different Prediction Mechanisms
     * into the database.
     *
     * @param databaseName the name of the prediction database
     * @param tableName the name of the table with load predictions
     * @param retentionPolicy the database retention policy
     * @param timestamp when this prediction was performed
     * @param deviceId the ID of the device
     * @param predictionMechanism the name of the prediction mechanism
     * @param predictionDelay the measured delay to perform this prediction
     * @throws DatabaseException if insertion fails
     */
    public void insertPredictionDelay(
        String              databaseName,
        String              tableName,
        String              retentionPolicy,
        long                timestamp,
        DeviceId            deviceId,
        PredictionMechanism predictionMechanism,
        long                predictionDelay) throws DatabaseException {
        if (!isReady()) {
            return;
        }

        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(retentionPolicy),
            "Database retention policy is NULL or empty"
        );
        checkNotNull(deviceId,             "Device ID is NULL");
        checkNotNull(predictionMechanism,  "Prediction Mechanism is NULL");
        checkArgument(timestamp       > 0, "Timestamp must be positive");
        checkArgument(predictionDelay > 0, "Prediction Delay must be positive");

        // Insert prediction delay info into the database
        try {
            this.influxDB.write(
                databaseName,
                retentionPolicy,
                Point.measurement(tableName)
                    .time(timestamp, TimeUnit.SECONDS)
                    .tag(LOAD_TABLE_PRIM_KEY_DEV,  deviceId.toString())
                    .tag("mechanism", predictionMechanism.method().toString())
                    .addField("delay", predictionDelay)
                    .build()
            );
        } catch (Exception ex) {
            throw new DatabaseException(
                "[" + label() + "] [InfluxDB INSERT INTO DB " + databaseName +
                " TABLE " + tableName + "] [Device " + deviceId.toString() +
                " - Mechanism " + predictionMechanism.method().toString() + "] Failed"
            );
        }
    }

    /**
     * Check the common input arguments of the prediction
     * methods used by this manager.
     *
     * @param predictionMechanism the prediction mechanism to apply
     * @param fromDatabaseName the (source) database to query the load
     * @param fromTableName the (source) database table to query the load
     * @param deviceId the ID of the device
     */
    private void commonPrePredictionChecks(
            PredictionMechanism predictionMechanism,
            String              fromDatabaseName,
            String              fromTableName,
            DeviceId            deviceId) {
        checkNotNull(
            predictionMechanism,
            "Prediction mechanism is NULL"
        );
        checkArgument(
            !Strings.isNullOrEmpty(fromDatabaseName),
            "Source database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(fromTableName),
            "Source database table is NULL or empty"
        );
        checkNotNull(
            deviceId,
            "Device ID is NULL"
        );
    }

    /**
     * A generic timeseries fetcher for any predictor.
     *
     * @param predictionMechanism the prediction mechanism to apply
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param groupingFrequency equals to the monitoring frequency
     *        with which we insert data into the database
     * @return list of LoadMetric objects
     */
    private List<LoadMetric> fetchInputTimeseries(
            PredictionMechanism predictionMechanism,
            String              databaseName,
            String              tableName,
            DeviceId            deviceId,
            int                 groupingFrequency) {
        List<LoadMetric> inputTimeseries = null;

        PredictionMethod method = predictionMechanism.method();
        boolean isBuiltIn   = PredictionMethod.isBuiltIn(method);
        long timeWindowSize = predictionMechanism.timeWindowSize();
        long dataWindowSize = predictionMechanism.dataWindowSize();

        // Built-in methods require a custom query to be translated into LoadMetric
        if (isBuiltIn) {
            String queryStr = null;
            if (PredictionMethod.isSmaInfluxDb(method)) {
                // SMA query using the built-in InfluxDB way
                queryStr =
                    "SELECT MOVING_AVERAGE(SUM(\"" + LOAD_TABLE_ATTRIBUTE_BITS + "\")," +
                        dataWindowSize + ") AS " + LOAD_TABLE_ATTRIBUTE_BITS + ", " +
                    "MOVING_AVERAGE(SUM(\"" + LOAD_TABLE_ATTRIBUTE_PACKETS + "\")," +
                        dataWindowSize + ") AS " + LOAD_TABLE_ATTRIBUTE_PACKETS + ", " +
                    "MOVING_AVERAGE(SUM(\"" + LOAD_TABLE_ATTRIBUTE_DROPS + "\")," +
                        dataWindowSize + ") AS " + LOAD_TABLE_ATTRIBUTE_DROPS + " " +
                    "FROM \""  + tableName + "\" " +
                    "WHERE " + LOAD_TABLE_PRIM_KEY_DEV + "='" + deviceId.toString() + "' " +
                    "AND time > now()-" + timeWindowSize + "s " +
                    "GROUP BY time(" + groupingFrequency + "s)," +
                    LOAD_TABLE_PRIM_KEY_DEV +
                    " fill(" + LOAD_TABLE_AGGREGATE_FILL + ")";
            } else if (PredictionMethod.isHoltWintersInfluxDb(method)) {
                /**
                 * Holt-Winters query using the built-in InfluxDB way.
                 * It computes numberOfPredictedValues number of predicted values
                 * using the Holt-Winters seasonal method. These predicted values
                 * occur during a time interval equal to groupingFrequency.
                 * Parameter seasonal describes how often (i.e.,
                 * multiples of groupingFrequency) the seasonal pattern
                 * occurs. If you do not want to seasonally adjust your predicted
                 * values, set seasonal to 0 or 1.
                 */
                int numberOfPredictedValues = 1;
                int seasonal = ((Integer) predictionMechanism.parameter(
                    HoltWintersPredictor.HOLT_WINTERS_SEASONAL_PARAM_SEC
                )).intValue();

                queryStr =
                    "SELECT HOLT_WINTERS(SUM(\"" + LOAD_TABLE_ATTRIBUTE_BITS + "\")," +
                    numberOfPredictedValues + "," + seasonal + ") AS " +
                    LOAD_TABLE_ATTRIBUTE_BITS + ", " +
                    "HOLT_WINTERS(SUM(\"" + LOAD_TABLE_ATTRIBUTE_PACKETS + "\")," +
                    numberOfPredictedValues + "," + seasonal + ") AS " +
                    LOAD_TABLE_ATTRIBUTE_PACKETS + ", " +
                    "HOLT_WINTERS(SUM(\"" + LOAD_TABLE_ATTRIBUTE_DROPS + "\")," +
                    numberOfPredictedValues + "," + seasonal + ") AS " +
                    LOAD_TABLE_ATTRIBUTE_DROPS + " " +
                    "FROM \""  + tableName + "\" " +
                    "WHERE " + LOAD_TABLE_PRIM_KEY_DEV + "='" + deviceId.toString() + "' " +
                    "AND time > now()-" + timeWindowSize + "s " +
                    "GROUP BY time(" + groupingFrequency + "s)," +
                    LOAD_TABLE_PRIM_KEY_DEV +
                    " fill(" + LOAD_TABLE_AGGREGATE_FILL + ")";
            }

            QueryResult result = monitorService.queryInfluxDB(databaseName, queryStr);
            if (result == null) {
                return null;
            }

            inputTimeseries = resultMapper.toPOJO(result, LoadMetric.class);
        } else {
            inputTimeseries = monitorService.queryDeviceLoadTimeLimited(
                databaseName,
                tableName,
                deviceId,
                timeWindowSize
            );
        }

        return validateInputTimeseries(inputTimeseries);
    }


    /**
     * Validate that the predicted values (i.e., of bits, packets and drops)
     * are correct. For example, they cannot be null or negative.
     *
     * @param inputTimeseries a list of LoadMetric objects
     * @return list of LoadMetric objects
     */
    private List<LoadMetric> validateInputTimeseries(List<LoadMetric> inputTimeseries) {

        for (Iterator<LoadMetric> iter = inputTimeseries.iterator(); iter.hasNext();) {
            LoadMetric load = iter.next();  // Get next element of inputTimeseries list

            if (LOAD_TABLE_AGGREGATE_FILL == "null") {
                /**
                 * Since fill(null), some LoadMetric elements of list inputTimeseries might
                 * have data points with bits == packets == drops == null.
                 * I will discard these elements.
                 *
                 */
                if (load.bits() == null || load.packets() == null || load.drops() == null) {
                    iter.remove();  // Remove this element from inputTimeseries list
                    continue;  // Check next element from inputTimeseries list
                }
            }

            if (load.bits() < 0 || load.packets() < 0 || load.drops() < 0) {
                // Negative prediction.
                // Set everything equal to 0:
                load.setBits(Long.valueOf(0));
                load.setPackets(Long.valueOf(0));
                load.setDrops(Long.valueOf(0));
            }
        }

        return inputTimeseries;
    }

    /**
     * Activates the core functionality of this manager.
     * This can only occur when a NetworkMonitor configuration is
     * stored into ONOS's distributed store.
     *
     * @param networkMonitorConf available NetworkMonitor configuration
     */
    private void setActive(NetworkMonitorConfiguration networkMonitorConf) {
        // Cache important pieces of the received configuration
        this.networkMonitorConfiguration = networkMonitorConf;
        this.databaseConfiguration       = this.networkMonitorConfiguration.databaseConfiguration();
        this.ingressPoints               = this.networkMonitorConfiguration.ingressPoints();
        this.predictionConfiguration     = this.networkMonitorConfiguration.predictionConfiguration();

        this.databaseName         = this.databaseConfiguration.databaseName();
        this.retentionPolicy      = this.databaseConfiguration.retentionPolicy();
        this.loadTable            = this.databaseConfiguration.loadTable();
        this.predictionTable      = this.databaseConfiguration.predictionTable();
        this.predictionDelayTable = this.databaseConfiguration.predictionDelayTable();
        this.predictionMechanisms = this.predictionConfiguration.predictionMechanisms();

        // Create the names of the prediction tables
        for (PredictionMechanism pred : this.predictionMechanisms) {
            PredictionMethod method = pred.method();
            String tableName = this.predictionTable + "_" + pred.method().toString().toLowerCase();
            if (!this.predictionTableMap.containsKey(method)) {
                this.predictionTableMap.put(method, tableName);
            }
        }

        // Get an InfluxDB connection
        if (this.influxDB == null) {
            this.influxDB = monitorService.connectToInfluxDB(
                label(),
                this.databaseConfiguration
            );
        }

        // Schedule threads to store network load data
        if (!this.isScheduled()) {
            this.setScheduled(true);
            schedulePredictionsSender();
        }
    }

    /**
     * Returns whether the manager is ready to predict.
     * This occurs when the Monitor manager is ready,
     * a database connection is established, and
     * a NetworkMonitor configuration is received.
     *
     * @return boolean readiness status
     */
    private boolean isReady() {
        return (
            monitorService.isReady() &&
            this.isScheduled() &&
            this.influxDB != null &&
            this.networkMonitorConfiguration != null
        );
    }

    /**
     * Sets the status of the internal threads.
     *
     * @param schedule schedulers' status to set
     */
    private void setScheduled(boolean schedule) {
        if (this.isScheduled == null) {
            this.isScheduled = new AtomicBoolean();
        }
        this.isScheduled.set(schedule);
    }

    /**
     * Returns the status of the internal threads.
     *
     * @return status of internal threads
     */
    private boolean isScheduled() {
        return this.isScheduled.get();
    }

    /**
     * Schedule a periodic push operation of network load
     * data into the database.
     */
    private void schedulePredictionsSender() {
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    runPredictions(
                        ingressPoints, predictionMechanisms,
                        databaseName, loadTable,
                        databaseName, retentionPolicy.toString()
                    );
                } catch (DatabaseException dbEx) {
                    throw dbEx;
                }

            }
        }, 1, STORE_PRED_STATS_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /**
     * Unschedule the thread that periodically pushes
     * network load data into the database.
     */
    private void unschedulePredictionsSender() {
        this.scheduler.shutdown();
    }

    /**
     * Returns a label with the monitor's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

    /**
     * Handles events related to the NetworkMonitor configuration
     * that resides in the ONOS store.
     * When a new/updated NetworkMonitor configuration is stored there,
     * an event is generated such that this manager can proceed
     * accordingly.
     */
    protected class InternalNetworkMonitorConfigurationListener
            implements NetworkMonitorConfigurationListenerInterface {
        @Override
        public void event(NetworkMonitorConfigurationEvent event) {
            // Parse the event to identify the NetworkMonitor configuration and its state
            NetworkMonitorConfigurationState state = event.type();
            NetworkMonitorConfiguration networkMonitorConf = event.subject();
            checkNotNull(
                networkMonitorConf,
                "Received a NULL NetworkMonitor configuration from the distributed store"
            );

            // Filter out the events we do not care about
            if (!this.isActive(state)) {
                return;
            }

            log.info("");
            log.info(
                "[{}] Received NetworkMonitor configuration with ID {} in state {}",
                label(), networkMonitorConf.id(), state
            );

            // Update Prediction manager and let it start
            setActive(networkMonitorConf);
        }

        /**
         * The Prediction manager cares about active configurations.
         *
         * @return boolean interest
         */
        private boolean isActive(NetworkMonitorConfigurationState state) {
            return isValidState(state) && (state == NetworkMonitorConfigurationState.ACTIVE);
        }

        /**
         * NetworkMonitor configuration events must strictly exhibit states
         * specified in NetworkMonitorConfigurationState.
         *
         * @param state the state of the NetworkMonitor configuration
         * @return boolean validity
         */
        private boolean isValidState(NetworkMonitorConfigurationState state) {
            return ArrayUtils.contains(NetworkMonitorConfigurationState.values(), state);
        }
    }

}
