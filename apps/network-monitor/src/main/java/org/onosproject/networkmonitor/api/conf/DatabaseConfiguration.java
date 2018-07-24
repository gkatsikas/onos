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

package org.onosproject.networkmonitor.api.conf;

import org.onosproject.networkmonitor.api.common.Common;
import org.onosproject.networkmonitor.api.exception.DatabaseException;

import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;

import org.influxdb.InfluxDB;

import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Database configuration information.
 */
public final class DatabaseConfiguration {

    private static final Logger log = getLogger(
        DatabaseConfiguration.class
    );

    /**
    * The retention policy of the database.
     */
    public enum RetentionPolicy {
        LAST_01_HOURS("last_01_hours"),
        LAST_24_HOURS("last_24_hours"),
        LAST_48_HOURS("last_48_hours"),
        LAST_72_HOURS("last_72_hours"),
        LAST_01_WEEKS("last_01_weeks"),
        LAST_02_WEEKS("last_02_weeks"),
        LAST_01_MONTHS("last_01_months");

        private String policyName;

        /**
         * Statically maps retention policies to enum types.
         */
        private static final Map<String, RetentionPolicy> MAP =
            new HashMap<String, RetentionPolicy>();

        static {
            for (RetentionPolicy policy : RetentionPolicy.values()) {
                MAP.put(policy.toString().toLowerCase(), policy);
            }
        }

        /**
         * Statically maps retention policies to their InfluxDB parameters.
         */
        private static final Map<RetentionPolicy, String> INFLUXDB_PARAM =
            new HashMap<RetentionPolicy, String>();

        static {
            INFLUXDB_PARAM.put(LAST_01_HOURS,   "1h");
            INFLUXDB_PARAM.put(LAST_24_HOURS,   "24h");
            INFLUXDB_PARAM.put(LAST_48_HOURS,   "48h");
            INFLUXDB_PARAM.put(LAST_72_HOURS,   "72h");
            INFLUXDB_PARAM.put(LAST_01_WEEKS,   "168h");
            INFLUXDB_PARAM.put(LAST_02_WEEKS,   "336h");
            INFLUXDB_PARAM.put(LAST_01_MONTHS,  "672h");
        }

        private RetentionPolicy(String policyName) {
            checkArgument(
                !Strings.isNullOrEmpty(policyName),
                "Retention policy name is NULL or empty"
            );
            this.policyName = policyName;
        }

        /**
         * Returns the InfluxDB parameter of this retention policy.
         *
         * @return a string-based InfluxDB parameter corresponding to this policy
         */
        public String getInfluxDBParameter() {
            return INFLUXDB_PARAM.get(this);
        }

        /**
         * Returns a retention policy object deriving from an input policy name.
         *
         * @param policyName a string-based retention policy name
         * @return a retention policy object
         */
        public static RetentionPolicy getByName(String policyName) {
            if (Strings.isNullOrEmpty(policyName)) {
                return null;
            }

            return MAP.get(policyName.toLowerCase());
        }

        @Override
        public String toString() {
            return this.policyName;
        }
    }

    /**
     * Database connectivity information.
     */
    private IpAddress    ip;
    private TpPort       port;
    private String       username;
    private String       password;

    /**
     * Database structure information.
     */
    private String          databaseName;
    private RetentionPolicy retentionPolicy;
    private boolean         batchWrite;
    private short           batchWriteSize;
    private String          loadTable;
    private String          predictionTable;
    private String          predictionDelayTable;
    private List<String>    tableKeys;
    private List<String>    tableAttributes;

    /**
     * Default database configuration.
     */
    public static final String  DEF_DB_IP   = "127.0.0.1";
    public static final short   DEF_DB_PORT = 8086;
    public static final String  DEF_DB_USER = "root";
    public static final String  DEF_DB_PASS = "root";
    public static final String  DEF_DB_NAME = "network-monitor";
    public static final boolean DEF_DB_BATCH_WRITE         = false;
    public static final short   DEF_DB_BATCH_WRITE_SIZE    = 60;
    public static final String  DEF_LOAD_TABLE             = "load";
    public static final String  DEF_PREDICTION_TABLE       = "load_prediction";
    public static final String  DEF_PREDICTION_DELAY_TABLE = "load_prediction_delay";

    /**
     * Keys in load table.
     */
    public static final String LOAD_TABLE_PRIM_KEY_DEV  = "device_id";
    public static final String LOAD_TABLE_PRIM_KEY_PORT = "port_id";
    public static final List<String> DEF_LOAD_TABLE_PRIM_KEYS = Arrays.asList(
        LOAD_TABLE_PRIM_KEY_DEV,
        LOAD_TABLE_PRIM_KEY_PORT
    );

    /**
     * Attributes in load table.
     */
    public static final String LOAD_TABLE_ATTRIBUTE_BITS    = "bits";
    public static final String LOAD_TABLE_ATTRIBUTE_PACKETS = "packets";
    public static final String LOAD_TABLE_ATTRIBUTE_DROPS   = "drops";
    public static final List<String> DEF_LOAD_TABLE_ATTRIBUTES = Arrays.asList(
        LOAD_TABLE_ATTRIBUTE_BITS,
        LOAD_TABLE_ATTRIBUTE_PACKETS,
        LOAD_TABLE_ATTRIBUTE_DROPS
    );

    /**
     * Other default configuration options.
     */
    public static final int DEF_DB_GROUPING_FREQ_SEC = 1;
    public static final int DEF_DB_FLUSH_DURATION_MSEC = 10000;
    public static final RetentionPolicy DEF_RETENTION_POLICY = RetentionPolicy.LAST_01_HOURS;

    public DatabaseConfiguration() {
        this.ip = IpAddress.valueOf(DEF_DB_IP);
        this.port = TpPort.tpPort((int) DEF_DB_PORT);
        this.username = DEF_DB_USER;
        this.password = DEF_DB_PASS;

        this.databaseName         = DEF_DB_NAME;
        this.retentionPolicy      = DEF_RETENTION_POLICY;
        this.batchWrite           = DEF_DB_BATCH_WRITE;
        this.batchWriteSize       = DEF_DB_BATCH_WRITE_SIZE;
        this.loadTable            = DEF_LOAD_TABLE;
        this.predictionTable      = DEF_PREDICTION_TABLE;
        this.predictionDelayTable = DEF_PREDICTION_DELAY_TABLE;

        this.tableKeys            = DEF_LOAD_TABLE_PRIM_KEYS;
        this.tableAttributes      = DEF_LOAD_TABLE_ATTRIBUTES;
    }

    public DatabaseConfiguration(
            String ip, short port, String username, String password,
            String databaseName, boolean batchWrite, short batchWriteSize,
            RetentionPolicy retentionPolicy, String loadTable,
            String predictionTable, String predictionDelayTable) {
        checkArgument(
            !Strings.isNullOrEmpty(ip),
            "Database IP is NULL or empty"
        );
        checkArgument(
            port >= TpPort.MIN_PORT && port <= TpPort.MAX_PORT,
            "Database port is invalid"
        );
        checkArgument(
            !Strings.isNullOrEmpty(username),
            "Database username is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(password),
            "Database password is NULL or empty"
        );

        this.ip       = IpAddress.valueOf(ip);
        this.port     = TpPort.tpPort((int) port);
        this.username = username;
        this.password = password;

        if (Strings.isNullOrEmpty(databaseName)) {
            this.databaseName = DEF_DB_NAME;
            log.warn(
                "No database name given. Fall back to default: {}",
                this.databaseName
            );
        } else {
            this.databaseName = databaseName;
        }

        if (retentionPolicy == null) {
            this.retentionPolicy = DEF_RETENTION_POLICY;
            log.warn(
                "No retention policy given. Fall back to default: {}",
                this.retentionPolicy.toString()
            );
            log.warn(
                "Valid retention policies: {}",
                Common.<RetentionPolicy>enumTypesToString(RetentionPolicy.class)
            );
        } else {
            this.retentionPolicy = retentionPolicy;
        }

        this.batchWrite = batchWrite;

        if (batchWriteSize <= 0) {
            this.batchWriteSize  = DEF_DB_BATCH_WRITE_SIZE;
            log.warn(
                "Batch write size must be positive. Fall back to default: {}",
                this.batchWriteSize
            );
        } else {
            this.batchWriteSize = batchWriteSize;
        }

        if (Strings.isNullOrEmpty(loadTable)) {
            this.loadTable  = DEF_LOAD_TABLE;
            log.warn(
                "No load table given. Fall back to default: {}",
                this.loadTable
            );
        } else {
            this.loadTable = loadTable;
        }

        if (Strings.isNullOrEmpty(predictionTable)) {
            this.predictionTable = DEF_PREDICTION_TABLE;
            log.warn(
                "No prediction table given. Fall back to default: {}",
                this.predictionTable
            );
        } else {
            this.predictionTable = predictionTable;
        }

        if (Strings.isNullOrEmpty(predictionDelayTable)) {
            this.predictionDelayTable = DEF_PREDICTION_DELAY_TABLE;
            log.warn(
                "No prediction delay table given. Fall back to default: {}",
                this.predictionDelayTable
            );
        } else {
            this.predictionDelayTable = predictionDelayTable;
        }

        // Use fixed columns
        this.tableKeys       = DEF_LOAD_TABLE_PRIM_KEYS;
        this.tableAttributes = DEF_LOAD_TABLE_ATTRIBUTES;
    }

    /**
     * Returns the IP address of the database.
     *
     * @return IP address of the database
     */
    public IpAddress ip() {
        return this.ip;
    }

    /**
     * Returns the port of the database.
     *
     * @return port of the database.
     */
    public TpPort port() {
        return this.port;
    }

    /**
     * Returns the username of the database.
     *
     * @return username of the database
     */
    public String username() {
        return this.username;
    }

    /**
     * Returns the password of the database.
     *
     * @return password of the database
     */
    public String password() {
        return this.password;
    }

    /**
     * Returns the name of the database.
     *
     * @return name of the database
     */
    public String databaseName() {
        return this.databaseName;
    }

    /**
     * Returns the retention policy of the database.
     *
     * @return retention policy of the database
     */
    public RetentionPolicy retentionPolicy() {
        return this.retentionPolicy;
    }

    /**
     * Returns whether batch write is enabled.
     *
     * @return batch write status
     */
    public boolean batchWrite() {
        return this.batchWrite;
    }

    /**
     * Returns the batch write size if batchWrite is enabled.
     *
     * @return batch write size
     */
    public short batchWriteSize() {
        return this.batchWrite ? this.batchWriteSize : 1;
    }

    /**
     * Returns the load table of the database.
     *
     * @return load table of the database
     */
    public String loadTable() {
        return this.loadTable;
    }

    /**
     * Returns the prediction table of the database.
     *
     * @return prediction table of the database
     */
    public String predictionTable() {
        return this.predictionTable;
    }

    /**
     * Returns the prediction delay table of the database.
     *
     * @return prediction delay table of the database
     */
    public String predictionDelayTable() {
        return this.predictionDelayTable;
    }

    /**
     * Returns the list of table keys of the database.
     *
     * @return list of table keys of the database
     */
    public List<String> tableKeys() {
        return this.tableKeys;
    }

    /**
     * Returns the list of table attributes of the database.
     *
     * @return list of table attributes of the database
     */
    public List<String> tableAttributes() {
        return this.tableAttributes;
    }

    /**
     * InfluxDB batching functionality creates an internal
     * thread pool that needs to be shutdown explicitly as
     * part of a graceful application shutdown.
     * This method receives an InfluxDB connector and attempts
     * to join its threads.
     *
     * @param influxDB InfluxDB connector object
     * @param batchWriteEnabled InfluxDB is configured with batching
     * @throws DatabaseException is close operation fails
     */
    public static void stopInfluxDbThreads(
            InfluxDB influxDB, boolean batchWriteEnabled)
            throws DatabaseException {
        if (influxDB != null) {
            if (batchWriteEnabled) {
                try {
                    influxDB.close();
                } catch (Exception ex) {
                    throw new DatabaseException(ex.toString());
                }
            }
        }
    }

    /**
     * Compares two database configurations.
     *
     * @return boolean comparison status
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DatabaseConfiguration) {
            DatabaseConfiguration that = (DatabaseConfiguration) obj;
            if (Objects.equals(this.ip, that.ip) &&
                Objects.equals(this.port, that.port) &&
                Objects.equals(this.username, that.username) &&
                Objects.equals(this.password, that.password) &&
                Objects.equals(this.databaseName, that.databaseName) &&
                Objects.equals(this.retentionPolicy, that.retentionPolicy) &&
                Objects.equals(this.batchWrite, that.batchWrite) &&
                Objects.equals(this.loadTable, that.loadTable) &&
                Objects.equals(this.predictionTable, that.predictionTable) &&
                Objects.equals(this.predictionDelayTable, that.predictionDelayTable) &&
                Objects.equals(this.tableKeys, that.tableKeys) &&
                Objects.equals(this.tableAttributes, that.tableAttributes)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            ip, port, username, password, databaseName,
            retentionPolicy, batchWrite, loadTable,
            predictionTable, predictionDelayTable
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("ip",                   ip.toString())
            .add("port",                 port.toString())
            .add("username",             username)
            .add("password",             password)
            .add("databaseName",         databaseName)
            .add("retentionPolicy",      retentionPolicy.toString())
            .add("batchWrite",           batchWrite ? "enabled" : "disabled")
            .add("batchWriteSize",       Short.valueOf(batchWriteSize))
            .add("loadTable",            loadTable)
            .add("predictionTable",      predictionTable)
            .add("predictionDelayTable", predictionDelayTable)
            .add("tableKeys",            String.join(", ", tableKeys))
            .add("tableAttributes",      String.join(", ", tableAttributes))
            .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return database configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Database configuration builder.
     */
    public static final class Builder {
        private IpAddress       ip;
        private TpPort          port;
        private String          username;
        private String          password;
        private String          databaseName;
        private boolean         batchWrite;
        private short           batchWriteSize;
        private RetentionPolicy retentionPolicy;
        private String          loadTable = DEF_LOAD_TABLE;
        private String          predictionTable = DEF_PREDICTION_TABLE;
        private String          predictionDelayTable = DEF_PREDICTION_DELAY_TABLE;
        private List<String>    tableKeys = DEF_LOAD_TABLE_PRIM_KEYS;
        private List<String>    tableAttributes = DEF_LOAD_TABLE_ATTRIBUTES;

        private Builder() {
        }

        public DatabaseConfiguration build() {
            return new DatabaseConfiguration(
                ip.toString(), (short) port.toInt(),
                username, password, databaseName,
                batchWrite, batchWriteSize, retentionPolicy,
                loadTable, predictionTable, predictionDelayTable
            );
        }

        /**
         * Returns database configuration builder with IP address.
         *
         * @param ipAddress database IP address
         * @return database configuration builder
         */
        public Builder ip(String ipAddress) {
            this.ip = IpAddress.valueOf(ipAddress);
            return this;
        }

        /**
         * Returns database configuration builder with port.
         *
         * @param port database port
         * @return database configuration builder
         */
        public Builder port(short port) {
            this.port = TpPort.tpPort((int) port);
            return this;
        }

        /**
         * Returns database configuration builder with username.
         *
         * @param username database username
         * @return database configuration builder
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Returns database configuration builder with password.
         *
         * @param password database password
         * @return database configuration builder
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Returns database configuration builder with database name.
         *
         * @param databaseName database name
         * @return database configuration builder
         */
        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        /**
         * Returns database configuration builder with batch.
         *
         * @param batchWrite database batch writing policy
         * @return database configuration builder
         */
        public Builder batchWrite(boolean batchWrite) {
            this.batchWrite = batchWrite;
            return this;
        }

        /**
         * Returns database configuration builder with batch size.
         *
         * @param batchWriteSize database batch write size
         * @return database configuration builder
         */
        public Builder batchWriteSize(short batchWriteSize) {
            this.batchWriteSize = batchWriteSize;
            return this;
        }

        /**
         * Returns database configuration builder with retention policy.
         *
         * @param retentionPolicy database retention policy
         * @return database configuration builder
         */
        public Builder retentionPolicy(RetentionPolicy retentionPolicy) {
            this.retentionPolicy = retentionPolicy;
            return this;
        }

        /**
         * Returns database configuration builder with
         * load table.
         *
         * @param loadTable database load table
         * @return database configuration builder
         */
        public Builder loadTable(String loadTable) {
            this.loadTable = loadTable;
            return this;
        }

        /**
         * Returns database configuration builder with
         * prediction table.
         *
         * @param predictionTable database prediction table
         * @return database configuration builder
         */
        public Builder predictionTable(String predictionTable) {
            this.predictionTable = predictionTable;
            return this;
        }

        /**
         * Returns database configuration builder with
         * prediction delay table.
         *
         * @param predictionDelayTable database prediction delay table
         * @return database configuration builder
         */
        public Builder predictionDelayTable(String predictionDelayTable) {
            this.predictionDelayTable = predictionDelayTable;
            return this;
        }

    }

}
