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

package org.onosproject.networkmonitor.impl.monitor;

import org.apache.commons.lang.ArrayUtils;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onosproject.networkmonitor.api.common.Constants;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration;
import org.onosproject.networkmonitor.api.conf.DatabaseConfiguration.RetentionPolicy;
import org.onosproject.networkmonitor.api.conf.IngressPoint;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfiguration;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationEvent;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationListenerInterface;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationService;
import org.onosproject.networkmonitor.api.conf.NetworkMonitorConfigurationState;
import org.onosproject.networkmonitor.api.exception.DatabaseException;
import org.onosproject.networkmonitor.api.monitor.MonitorService;
import org.onosproject.networkmonitor.api.metrics.LoadMetric;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Series;
import org.influxdb.dto.Point;
import org.influxdb.impl.InfluxDBResultMapper;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Component(immediate = true, service = MonitorService.class)
public class MonitorManager implements MonitorService {

    private final Logger log = getLogger(getClass());

    /**
     * ONOS application information.
     */
    public static final String APP_NAME = Constants.SYSTEM_PREFIX + ".monitor";

    /**
     * Print ID.
     */
    private static final String COMPONET_LABEL = "   Monitor Manager";

    /**
     * Time interval to schedule the thread that
     * stores network load data to the database.
     */
    private static final int MONITORING_ENTRIES_PER_SEC = 1;

    /**
     * Number of threads for sending network load
     * information into the dabatase.
     */
    private static final int LOAD_SEND_THREADS_NB = 8;

    /**
     * Application ID for MonitorManager.
     */
    private static ApplicationId appId;

    /**
     * Unique database handler.
     */
    private static volatile InfluxDB influxDB;

    /**
     * Maps database results to custom classes.
     */
    private static InfluxDBResultMapper resultMapper;

    /**
     * MonitorManager requires a NetworkMonitorConfiguration object with:
     * |-> Database configuration.
     * |-> Set of ingress points to be monitored.
     */
    private static NetworkMonitorConfiguration networkMonitorConfiguration = null;
    private static DatabaseConfiguration       databaseConfiguration       = null;
    private static Set<IngressPoint>           ingressPoints               = null;

    /**
     * Database name.
     */
    private static String databaseName;

    /**
     * Database table to host load statistics.
     */
    private static String loadTable;

    /**
     * Database retention policy.
     */
    private static RetentionPolicy retentionPolicy;

    /**
     * Indicates database readiness.
     */
    private static AtomicBoolean databaseReady;

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
     * Services used by the Monitor manager.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkMonitorConfigurationService networkMonitorConfigurationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    /**
     * Schedule threads to store the monitoring data.
     */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(LOAD_SEND_THREADS_NB);

    /**
     * Listen to events dispatched by the NetworkMonitor configuration manager.
     * These events are related to the state of the NetworkMonitor configuration.
     */
    private final NetworkMonitorConfigurationListenerInterface networkMonitorConfigurationListener =
        new InternalNetworkMonitorConfigurationListener();

    public MonitorManager() {
        this.databaseName    = null;
        this.retentionPolicy = null;
        this.loadTable       = null;
        this.setDatabaseReady(false);
        this.setScheduled(false);

        this.resultMapper = new InfluxDBResultMapper();
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
        unscheduleLoadStatisticsSender();

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
    public InfluxDB connectToInfluxDB(
            String entity, DatabaseConfiguration databaseConfiguration) {
        checkArgument(
            !Strings.isNullOrEmpty(entity),
            "A NULL entity asks for a connection to the database"
        );

        if (databaseConfiguration == null) {
            log.error(
                "[{}] Unable to connect to database: No configuration available",
                entity
            );
            return null;
        }

        InfluxDB influxDB;
        try {
            // Attempt to connect
            influxDB = InfluxDBFactory.connect(
                "http://" + databaseConfiguration.ip().toString() +
                ":" + databaseConfiguration.port().toString(),
                databaseConfiguration.username(),
                databaseConfiguration.password()
            );
        } catch (IllegalArgumentException iaEx) {
            throw new DatabaseException(iaEx.toString());
        }

        log.info("[{}] [InfluxDB CONNECT] Connected", entity);

        return influxDB;
    }

    @Override
    public boolean createDatabase(
            String  databaseName,
            String  retentionPolicyName,
            String  retentionPolicyParam,
            boolean withBatchWrite,
            short   batchWriteSize) {
        if (this.influxDB == null) {
            log.error(
                "[{}] Unable to create database: No connection to the database",
                label()
            );
            return false;
        }

        // Check if database already exists
        if (this.influxDB.databaseExists(databaseName)) {
            log.info("[{}] [InfluxDB CREATE {}] Skipped ... Already exists", label(), databaseName);
            // FIX: When the existing retention policy is different from the new one
        } else {
            try {
                // Otherwise create it, along with the corresponding retention policy
                this.influxDB.createDatabase(databaseName);
                log.info("[{}] [InfluxDB CREATE {}] Done", label(), databaseName);

                // Create a default retention policy
                this.influxDB.createRetentionPolicy(
                    retentionPolicyName,
                    databaseName,
                    retentionPolicyParam,
                    1, true
                );

                // Drop InfluxDB automatically generated retention policy, called "autogen"
                this.influxDB.dropRetentionPolicy("autogen", databaseName);
            } catch (Exception ex) {
                throw new DatabaseException(ex.toString());
            }
        }

        // Set retention policy and consistency level
        try {
            this.influxDB.setConsistency(ConsistencyLevel.ALL);
            this.influxDB.setRetentionPolicy(retentionPolicyName);
        } catch (Exception ex) {
            throw new DatabaseException(ex.toString());
        }

        // Enable/Disable batch writes
        if (withBatchWrite) {
            int flushDurationMilli = DatabaseConfiguration.DEF_DB_FLUSH_DURATION_MSEC;

            try {
                // Flush every batchWriteSize points, at least every flushDurationMilli ms
                this.influxDB.enableBatch(
                    BatchOptions.DEFAULTS
                        .actions(batchWriteSize)
                        .flushDuration(flushDurationMilli)
                );
            } catch (Exception ex) {
                throw new DatabaseException(ex.toString());
            }

            log.info(
                "[{}] [InfluxDB CREATE {}] Batch writes {} with flush duration {} ms",
                label(), databaseName, batchWriteSize, flushDurationMilli
            );
        } else {
            this.influxDB.disableBatch();
        }

        log.info(
            "[{}] [InfluxDB CREATE {}] Retention policy is {}",
            label(), databaseName, retentionPolicyName
        );

        return true;
    }

    @Override
    public boolean isReady() {
        if (this.isDatabaseReady() &&
            networkMonitorConfigurationService.isNetworkMonitorConfigurationReady()) {
            return true;
        }

        return false;
    }

    @Override
    public void runLoadMonitoring(
            Set<IngressPoint> ingressPoints,
            String            databaseName,
            String            tableName,
            String            retentionPolicy) throws DatabaseException {
        if (!isReady()) {
            log.warn("[{}] Database not ready", label());
            return;
        }

        for (IngressPoint ingressPoint: ingressPoints) {
            DeviceId deviceId = ingressPoint.deviceId();
            Set<PortNumber> portIds = ingressPoint.portIds();

            /**
             * Need to check if the device and port combo exists,
             * otherwise code execution hangs.
             */
            if (!deviceService.isAvailable(deviceId)) {
                continue;
            }

            Device ingDevice = deviceService.getDevice(deviceId);
            List<Port> ports = deviceService.getPorts(ingDevice.id());

            // Unique timestamp across all ports of a given device
            long currentTime = Instant.now().getEpochSecond();

            for (Port port : ports) {
                PortNumber portId = port.number();

                // Not interested in this port
                if (!portIds.contains(portId)) {
                    continue;
                }

                PortStatistics portStats = deviceService.getDeltaStatisticsForPort(
                    deviceId, portId
                );

                if (portStats == null) {
                    log.warn(
                        "[{}] [Device {} - Port {}] No port statistics",
                        label(), deviceId, portId
                    );
                    continue;
                }

                log.debug(
                    "[{}] [Device {} - Port {}] Under inspection",
                    label(), deviceId, portId
                );

                long rxBits    = portStats.bytesReceived() * 8;
                long rxPackets = portStats.packetsReceived();
                long rxDropped = portStats.packetsRxDropped();

                // Insert device load information into the database
                if (!insertIntoInfluxDB(
                        databaseName,
                        tableName,
                        retentionPolicy,
                        currentTime,
                        deviceId,
                        portId,
                        rxBits,
                        rxPackets,
                        rxDropped)) {
                    throw new DatabaseException(
                        "[" + label() + "] [InfluxDB INSERT INTO DB " + databaseName +
                        " TABLE " + tableName + "] [Device " + deviceId +
                        " - Port " + portId + "] Failed"
                    );
                }
            }
        }
    }

    @Override
    public boolean insertIntoInfluxDB(
            String     databaseName,
            String     tableName,
            String     retentionPolicy,
            long       timestamp,
            DeviceId   deviceId,
            PortNumber portId,
            long       rxBits,
            long       rxPackets,
            long       rxDropped) {
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
        checkNotNull(deviceId, "Device ID is NULL");
        checkNotNull(portId,   "Port ID is NULL");
        checkArgument(timestamp  > 0, "Timestamp must be positive");
        checkArgument(rxBits    >= 0, "Rx bits must not be negative");
        checkArgument(rxPackets >= 0, "Rx packets must not be negative");
        checkArgument(rxDropped >= 0, "Rx dropped packets must not be negative");

        try {
            this.influxDB.write(
                databaseName,
                retentionPolicy,
                Point.measurement(tableName)
                    .time(timestamp, TimeUnit.SECONDS)
                    .tag(LOAD_TABLE_PRIM_KEY_DEV,  deviceId.toString())
                    .tag(LOAD_TABLE_PRIM_KEY_PORT, portId.toString())
                    .addField(LOAD_TABLE_ATTRIBUTE_BITS,    rxBits)
                    .addField(LOAD_TABLE_ATTRIBUTE_PACKETS, rxPackets)
                    .addField(LOAD_TABLE_ATTRIBUTE_DROPS,   rxDropped)
                    .build()
            );
        } catch (Exception ex) {
            return false;
        }

        // Show only when there is some load
        if ((rxBits > 0) && (rxPackets > 0)) {
            log.info(
                "[{}] [InfluxDB INSERT INTO {}] [Device {} - Port {}] " +
                "Rx Bits {} - Packets {} - Drops {}",
                label(), tableName, deviceId.toString(), portId.toString(),
                rxBits, rxPackets, rxDropped
            );
        }

        return true;
    }

    @Override
    public List<LoadMetric> queryDeviceLoad(
            String     databaseName,
            String     tableName,
            DeviceId   deviceId) {
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkNotNull(deviceId, "Device ID is NULL");

        String queryStr =
            "SELECT sum(\"" + LOAD_TABLE_ATTRIBUTE_BITS + "\") AS " + LOAD_TABLE_ATTRIBUTE_BITS + ", " +
            "sum(\"" + LOAD_TABLE_ATTRIBUTE_PACKETS + "\") AS " + LOAD_TABLE_ATTRIBUTE_PACKETS + ", " +
            "sum(\"" + LOAD_TABLE_ATTRIBUTE_DROPS + "\") AS " + LOAD_TABLE_ATTRIBUTE_DROPS + " " +
            "FROM \""  + tableName + "\" " +
            "WHERE " + LOAD_TABLE_PRIM_KEY_DEV + "='" + deviceId.toString() + "' " +
            "GROUP BY time(" + monitoringFrequency() + "s)," +
            LOAD_TABLE_PRIM_KEY_DEV +
            " fill(" + LOAD_TABLE_AGGREGATE_FILL + ")";

        QueryResult result = queryInfluxDB(databaseName, queryStr);

        if (result == null) {
            return null;
        }

        // printRawLoadStatistics(result.getResults().get(0));

        return resultMapper.toPOJO(result, LoadMetric.class);
    }

    @Override
    public List<LoadMetric> queryDeviceLoadOnPort(
            String     databaseName,
            String     tableName,
            DeviceId   deviceId,
            PortNumber portId) {
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkNotNull(deviceId, "Device ID is NULL");
        checkNotNull(portId,   "Port ID is NULL");

        String queryStr =
            "SELECT * " +
            "FROM \""  + tableName + "\" " +
            "WHERE " + LOAD_TABLE_PRIM_KEY_DEV  + "='" + deviceId.toString() + "' AND " +
            "      " + LOAD_TABLE_PRIM_KEY_PORT + "='" + portId.toString() + "'";

        QueryResult result = queryInfluxDB(databaseName, queryStr);

        if (result == null) {
            return null;
        }

        // printRawLoadStatistics(result.getResults().get(0));

        return resultMapper.toPOJO(result, LoadMetric.class);
    }

    @Override
    public List<LoadMetric> queryDeviceLoadTimeLimited(
            String     databaseName,
            String     tableName,
            DeviceId   deviceId,
            long       timeWindowSize) {
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkNotNull(deviceId, "Device ID is NULL");
        checkArgument(timeWindowSize > 0, "Time window must be positive");

        String queryStr =
            "SELECT sum(\"" + LOAD_TABLE_ATTRIBUTE_BITS + "\") AS " + LOAD_TABLE_ATTRIBUTE_BITS + ", " +
            "sum(\"" + LOAD_TABLE_ATTRIBUTE_PACKETS + "\") AS " + LOAD_TABLE_ATTRIBUTE_PACKETS + ", " +
            "sum(\"" + LOAD_TABLE_ATTRIBUTE_DROPS + "\") AS " + LOAD_TABLE_ATTRIBUTE_DROPS + " " +
            "FROM \""  + tableName + "\" " +
            "WHERE " + LOAD_TABLE_PRIM_KEY_DEV + "='" + deviceId.toString() + "' " +
            "AND \"time\" > now()-" + timeWindowSize + "s " +
            "GROUP BY time(" + monitoringFrequency() + "s)," +
            LOAD_TABLE_PRIM_KEY_DEV +
            " fill(" + LOAD_TABLE_AGGREGATE_FILL + ")";

        QueryResult result = queryInfluxDB(databaseName, queryStr);

        if (result == null) {
            return null;
        }

        // printRawLoadStatistics(result.getResults().get(0));

        return resultMapper.toPOJO(result, LoadMetric.class);
    }

    @Override
    public List<LoadMetric> queryDeviceLoadTimeLimitedManual(
            String     databaseName,
            String     tableName,
            DeviceId   deviceId,
            long       timeWindowSize) throws DatabaseException {
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkNotNull(deviceId, "Device ID is NULL");
        checkArgument(timeWindowSize > 0, "Time window must be positive");

        String queryStr =
            "SELECT * " +
            "FROM \""  + tableName + "\" " +
            "WHERE " + LOAD_TABLE_PRIM_KEY_DEV  + "='" + deviceId.toString() + "' " +
            "AND \"time\" > now()-" + timeWindowSize + "s ";

        QueryResult queryResult = queryInfluxDB(databaseName, queryStr);
        if (queryResult == null) {
            return null;
        }

        List<LoadMetric> loadList = resultMapper.toPOJO(queryResult, LoadMetric.class);
        log.info("[{}] Query result length is {}", label(), loadList.size());

        List<LoadMetric> result = new ArrayList<LoadMetric>();

        // Grouping per timestamp
        long timestamp = 0;
        LoadMetric deviceAggregate = new LoadMetric();

        for (LoadMetric load: loadList) {
            if (load.timeEpochMilli() > timestamp) {
                if (timestamp != 0) {
                    result.add(deviceAggregate);
                }
                deviceAggregate = load;
                timestamp = load.timeEpochMilli();
            } else if (load.timeEpochMilli() == timestamp) {
                deviceAggregate.setBits(deviceAggregate.bits() + load.bits());
                deviceAggregate.setPackets(deviceAggregate.packets() + load.packets());
                deviceAggregate.setDrops(deviceAggregate.drops() + load.drops());
            } else {
                throw new DatabaseException(
                    "[" + label() + "] Query results are not sorted by increasing time"
                );
            }
        }
        result.add(deviceAggregate);

        log.info("[{}] Aggregate query result length is {}", label(), result.size());

        return result;
    }

    @Override
    public List<LoadMetric> queryDeviceLoadOnPortTimeLimited(
            String     databaseName,
            String     tableName,
            DeviceId   deviceId,
            PortNumber portId,
            long       timeWindowSize) {
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(tableName),
            "Database table is NULL or empty"
        );
        checkNotNull(deviceId, "Device ID is NULL");
        checkNotNull(portId,   "Port ID is NULL");
        checkArgument(timeWindowSize > 0, "Time window must be positive");

        String queryStr =
            "SELECT * " +
            "FROM \""  + tableName + "\" " +
            "WHERE " + LOAD_TABLE_PRIM_KEY_DEV  + "='" + deviceId.toString() + "' AND " +
            "      " + LOAD_TABLE_PRIM_KEY_PORT + "='" + portId.toString() + "' " +
            "AND \"time\" > now()-" + timeWindowSize + "s";

        QueryResult result = queryInfluxDB(databaseName, queryStr);

        if (result == null) {
            return null;
        }

        // printRawLoadStatistics(result.getResults().get(0));

        return resultMapper.toPOJO(result, LoadMetric.class);
    }

    @Override
    public QueryResult queryInfluxDB(
            String databaseName,
            String queryStr) {
        checkArgument(
            !Strings.isNullOrEmpty(databaseName),
            "Database name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(queryStr),
            "Database query is NULL or empty"
        );

        Query query = new Query(queryStr, databaseName);
        QueryResult result = null;

        try {
            result = this.influxDB.query(query);
        } catch (Exception ex) {
            log.error(
                "[{}] [InfluxDB QUERY {}] Failed with error: {}",
                label(), queryStr, ex.toString()
            );
            return null;
        }

        if ((result == null) ||
            (result.getResults() == null)) {
            log.error("[{}] [InfluxDB QUERY {}] No results", label(), queryStr);
            return null;
        }

        log.info("[{}] [InfluxDB QUERY {}] Done", label(), queryStr);

        return result;
    }

    @Override
    public int monitoringFrequency() {
        return MONITORING_ENTRIES_PER_SEC;
    }

    @Override
    public long numberOfRecordsGivenTime(long timeWindowSize) {
        checkArgument(timeWindowSize >= 0, "Time window must be positive");
        return timeWindowSize * monitoringFrequency();
    }

    @Override
    public void printLoadMetrics(List<LoadMetric> loadList) {
        // Default upper bound
        printLoadMetrics(loadList, 50);
    }

    /**
     * Print load metrics objects with an upper bound.
     *
     * @param loadList list of NetworkMonitor metrics fetched from the database
     * @param upperBound prints the first upperBound number of entries
     */
    private void printLoadMetrics(List<LoadMetric> loadList, long upperBound) {
        checkNotNull(loadList, "List of load metrics to print is NULL");
        checkArgument(upperBound >= 0, "Maximum number of entries to print must be non-negative");

        log.info("[{}] Printing load information from database: ", label());

        long count = 0;
        long previousTimestamp = loadList.get(0).timeEpochSec() - monitoringFrequency();
        for (LoadMetric lm : loadList) {
            /**
             * Current timestamp must be the previous timestamp
             * incremented by the monitoring frequency.
             */
            checkArgument(
                (lm.timeEpochSec() == previousTimestamp + monitoringFrequency()),
                "Invalid order of timestamps in query result"
            );
            // Update previous timestamp
            previousTimestamp = lm.timeEpochSec();

            log.info(
                "[{}] [Time {}] [Device {} - Port {}] Bits {} - Packets {} - Drops {}",
                label(), lm.time().toString(), lm.deviceId(), lm.portId(),
                lm.bits(), lm.packets(), lm.drops()
            );

            if ((++count) == upperBound) {
                break;
            }
        }
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

        this.databaseName    = this.databaseConfiguration.databaseName();
        this.retentionPolicy = this.databaseConfiguration.retentionPolicy();
        this.loadTable       = this.databaseConfiguration.loadTable();

        // DB connection and creation is performed once
        if (isReady()) {
            log.info(
                "[{}] Configuration update does affect the already created database",
                label()
            );
            return;
        }

        boolean status = true;

        try {
            this.influxDB = connectToInfluxDB(label(), this.databaseConfiguration);
            if (this.influxDB != null) {
                status &= createDatabase(
                    this.databaseName,
                    this.retentionPolicy.toString(),
                    this.retentionPolicy.getInfluxDBParameter(),
                    this.databaseConfiguration.batchWrite(),
                    this.databaseConfiguration.batchWriteSize()
                );
            }
        } catch (Exception ex) {
            log.error(
                "[{}] Problem while connecting to the database",
                label()
            );
            // Prevent database from becoming ready
            status = false;
        }

        if (!status) {
            return;
        }

        // Proper database interaction has been ensured
        setDatabaseReady(true);

        // Schedule threads to store network load data
        if (!this.isScheduled()) {
            scheduleLoadStatisticsSender();
            this.setScheduled(true);
        }
    }

    /**
     * Sets the readiness status of the database.
     *
     * @param ready readiness status to set
     */
    private void setDatabaseReady(boolean ready) {
        if (this.databaseReady == null) {
            this.databaseReady = new AtomicBoolean();
        }
        this.databaseReady.set(ready);
    }

    /**
     * Returns the readiness status of the database.
     *
     * @return readiness status of the database
     */
    private boolean isDatabaseReady() {
        return this.databaseReady.get();
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
    private void scheduleLoadStatisticsSender() {
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    runLoadMonitoring(
                        ingressPoints, databaseName,
                        loadTable, retentionPolicy.toString()
                    );
                } catch (DatabaseException dEx) {
                    throw dEx;
                }
            }
        }, 1, monitoringFrequency(), TimeUnit.SECONDS);
    }

    /**
     * Unschedule the thread that periodically pushes
     * network load data into the database.
     */
    private void unscheduleLoadStatisticsSender() {
        this.scheduler.shutdown();
    }

    /**
     * Print raw load statistics from the database.
     *
     * @param queryResult queried load object
     */
    private void printRawLoadStatistics(QueryResult.Result queryResult) {
        log.info("[{}] Printing raw load information from database: ", label());

        for (Series series : queryResult.getSeries()) {
            log.info("[{}] Series name {}", label(), series.getName());
            log.info("[{}] Series tags {}", label(), series.getTags());
            log.info("[{}] Series cols {}", label(), series.getColumns());
            log.info("[{}] Series data {}", label(), series.getValues().toString());
        }
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

            // Update Monitor manager and let it start
            setActive(networkMonitorConf);
        }

        /**
         * The Monitor manager cares about active configurations.
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
