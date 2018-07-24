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

package org.onosproject.networkmonitor.api.prediction;

import org.onosproject.networkmonitor.api.exception.DatabaseException;
import org.onosproject.networkmonitor.api.exception.PredictionException;
import org.onosproject.networkmonitor.api.conf.IngressPoint;
import org.onosproject.networkmonitor.api.metrics.LoadMetric;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Set;

/**
 * Services provided by a Prediction manager.
 */
public interface PredictionService {

    /**
     * Run a set of prediction techniques on data
     * acquired from designated ingress points.
     *
     * @param ingressPoints set of ingress points to monitor
     * @param predictionMechanisms set of prediction mechanisms to run
     * @param fromDatabaseName the (source) database to query the load
     * @param fromTableName the (source) table to query the load
     * @param toDatabaseName the (destination) database to store the prediction
     * @param retentionPolicy the database retention policy
     * @throws DatabaseException if insertion fails
     * @throws PredictionException if prediction method is unimplemented
     */
    void runPredictions(
        Set<IngressPoint>        ingressPoints,
        Set<PredictionMechanism> predictionMechanisms,
        String                   fromDatabaseName,
        String                   fromTableName,
        String                   toDatabaseName,
        String                   retentionPolicy
    ) throws DatabaseException, PredictionException;

    /**
     * Predict a device's network load across all of its ingress ports.
     *
     * @param predictionMechanism the prediction mechanism to apply
     * @param fromDatabaseName the (source) database to query the load
     * @param fromTableName the (source) table to query the load
     * @param deviceId the ID of the device to be queried
     * @return list of LoadMetric objects
     * @throws PredictionException if prediction method is unimplemented
     */
    List<LoadMetric> predictDeviceLoad(
        PredictionMechanism predictionMechanism,
        String              fromDatabaseName,
        String              fromTableName,
        DeviceId            deviceId
    ) throws PredictionException;

    /**
     * Predict a device's network load on a specific port.
     *
     * @param predictionMechanism the prediction mechanism to apply
     * @param fromDatabaseName the (source) database to query the load
     * @param fromTableName the (source) table to query the load
     * @param deviceId the ID of the device
     * @param portId the ID of the device's port
     * @return list of LoadMetric objects
     * @throws PredictionException if prediction method is unimplemented
     */
    List<LoadMetric> predictDeviceLoadOnPort(
        PredictionMechanism predictionMechanism,
        String              fromDatabaseName,
        String              fromTableName,
        DeviceId            deviceId,
        PortNumber          portId
    ) throws PredictionException;

    /**
     * Insert network load predictions into the prediction database.
     *
     * @param databaseName the name of the prediction database
     * @param tableName the name of the table with load predictions
     * @param retentionPolicy the database retention policy
     * @param predictedLoad list of predicted load metrics to be inserted
     * @throws DatabaseException if insertion fails
     */
    void insertPredictionStatistics(
        String           databaseName,
        String           tableName,
        String           retentionPolicy,
        List<LoadMetric> predictedLoad
    ) throws DatabaseException;

}
