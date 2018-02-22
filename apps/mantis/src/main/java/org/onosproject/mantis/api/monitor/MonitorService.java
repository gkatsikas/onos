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

package org.onosproject.mantis.api.monitor;

import org.onosproject.mantis.api.exception.DatabaseException;
import org.onosproject.mantis.api.conf.DatabaseConfiguration;
import org.onosproject.mantis.api.conf.IngressPoint;
import org.onosproject.mantis.api.metrics.LoadMetric;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import org.influxdb.InfluxDB;
import org.influxdb.dto.QueryResult;

import java.util.List;
import java.util.Set;

/**
 * Services provided by a Monitor manager.
 */
public interface MonitorService {

    /**
     * Attempt to connect to InfluxDB.
     *
     * @param entity the name of the module that requests a connection
     * @param databaseConfiguration database configuration
     * @return database connection handler
     * @throws DatabaseException if connection fails
     */
    InfluxDB connectToInfluxDB(
        String                entity,
        DatabaseConfiguration databaseConfiguration
    ) throws DatabaseException;

    /**
     * Create a database.
     *
     * @param databaseName the name of the database to be created
     * @param retentionPolicyName the database retention policy
     * @param retentionPolicyParam the database retention policy parameter
     * @param withBatchWrite enables/disables batch write operations
     * @param batchWriteSize the size of batch write operations (if enabled)
     * @return boolean status
     * @throws DatabaseException if any database operation fails
     */
    boolean createDatabase(
        String  databaseName,
        String  retentionPolicyName,
        String  retentionPolicyParam,
        boolean withBatchWrite,
        short   batchWriteSize
    ) throws DatabaseException;

    /**
     * Returns whether the manager is ready to monitor.
     * This occurs when a Mantis configuration is received
     * and the database is configured accordingly.
     *
     * @return boolean readiness status
     */
    boolean isReady();

    /**
     * Run network load monitoring for a designated
     * set of ingress points and store the results into
     * a database.
     *
     * @param ingressPoints set of ingress points to monitor
     * @param databaseName the name of the database to store the load
     * @param tableName the name of the table with load info
     * @param retentionPolicy the database retention policy
     * @throws DatabaseException if load insertion fails
     */
    void runLoadMonitoring(
        Set<IngressPoint> ingressPoints,
        String            databaseName,
        String            tableName,
        String            retentionPolicy
    ) throws DatabaseException;

    /**
     * Insertion of load information into the database
     * related to a specific device.
     * This method works for both batch-style and sunchronous insertions.
     *
     * @param databaseName the name of the database
     * @param tableName the name of the database's table
     * @param retentionPolicy the database retention policy
     * @param timestamp the timestamp of this operation
     * @param deviceId the ID of the device being monitored
     * @param portId the ID of the device's port being monitored
     * @param rxBits the number of bits received at this port
     * @param rxPackets the number of packets received at this port
     * @param rxDropped the number of dropped packets at this port
     * @return insertion status
     */
    boolean insertIntoInfluxDB(
        String     databaseName,
        String     tableName,
        String     retentionPolicy,
        long       timestamp,
        DeviceId   deviceId,
        PortNumber portId,
        long       rxBits,
        long       rxPackets,
        long       rxDropped
    );

    /**
     * Query a device to fetch its network load
     * across all of the ingress ports.
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @return list of LoadMetric objects
     */
    List<LoadMetric> queryDeviceLoad(
        String     databaseName,
        String     tableName,
        DeviceId   deviceId
    );

    /**
     * Query a specific port of a device to fetch its network load.
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param portId the ID of the device's port to be queried
     * @return list of LoadMetric objects
     */
    List<LoadMetric> queryDeviceLoadOnPort(
        String     databaseName,
        String     tableName,
        DeviceId   deviceId,
        PortNumber portId
    );

    /**
     * Query a device to fetch its network load
     * across all of the ingress ports
     * for the last 'timeWindowSize' seconds.
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param timeWindowSize the query duration in seconds
     * @return list of LoadMetric objects
     */
    List<LoadMetric> queryDeviceLoadTimeLimited(
        String     databaseName,
        String     tableName,
        DeviceId   deviceId,
        long       timeWindowSize
    );

    /**
     * Query a device to fetch its network load
     * across all of the ingress ports
     * for the last 'timeWindowSize' seconds.
     * This method does NOT use InfluxDB aggregate function sum()
     * but instead performs aggregation itself (i.e., java code).
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param timeWindowSize the query duration in seconds
     * @return list of LoadMetric objects
     * @throws DatabaseException is query results are not sorted
     */
    List<LoadMetric> queryDeviceLoadTimeLimitedManual(
        String     databaseName,
        String     tableName,
        DeviceId   deviceId,
        long       timeWindowSize
    );

    /**
     * Query a specific port of a device to fetch its network load
     * for the last 'timeWindowSize' seconds.
     *
     * @param databaseName the name of the database to query
     * @param tableName the name of the table with load info
     * @param deviceId the ID of the device to be queried
     * @param portId the ID of the device's port to be queried
     * @param timeWindowSize the query duration in seconds
     * @return list of LoadMetric objects
     */
    List<LoadMetric> queryDeviceLoadOnPortTimeLimited(
        String     databaseName,
        String     tableName,
        DeviceId   deviceId,
        PortNumber portId,
        long       timeWindowSize
    );

    /**
     * Generic SQL-like query from a table of a database.
     *
     * @param databaseName the name of the database to query
     * @param queryStr the query to be sent
     * @return a query result object or null
     */
    QueryResult queryInfluxDB(
        String databaseName,
        String queryStr
    );

    /**
     * Returns the frequency (in seconds) that this manager
     * interacts with the database.
     * E.g., if we query the ingress points 2 times/sec, then
     * monitoring frequency is 2, hence we store 2 entries per
     * second in the database.
     *
     * @return monitoring frequency in seconds
     */
    int monitoringFrequency();

    /**
     * Returns the expected number of records in the database
     * for a given time window in seconds.
     * E.g., if the manager stores load information 1 times/sec,
     * (i.e., monitoring frequency is 1), then the number of entries
     * in the database during 60 seconds will be 60*1 = 60 entries.
     *
     * @param timeWindowSize the query duration in seconds
     * @return expected number of database records
     *         for a given time window
     */
    long numberOfRecordsGivenTime(long timeWindowSize);

    /**
     * Print load metrics objects.
     * TODO: Remove when debugging is over.
     *
     * @param loadList list of Mantis metrics fetched from the database
     */
    void printLoadMetrics(List<LoadMetric> loadList);

}
