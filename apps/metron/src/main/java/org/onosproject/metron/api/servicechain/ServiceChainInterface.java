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

package org.onosproject.metron.api.servicechain;

import org.onosproject.metron.api.config.TrafficPoint;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;

import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * The interface of a Metron service chain.
 */
public interface ServiceChainInterface {

    /**
     * Returns the name of this service chain.
     *
     * @return service chain's name
     */
    String name();

    /**
     * Sets the name of this service chain.
     *
     * @param name service chain's name
     */
    void setName(String name);

    /**
     * Returns the type of this service chain.
     *
     * @return service chain's type
     */
    String type();

    /**
     * Sets the type of this service chain.
     *
     * @param name service chain's type
     */
    void setType(String name);

    /**
     * Returns the scope of this service chain.
     *
     * @return service chain's scope
     */
    ServiceChainScope scope();

    /**
     * Sets the scope of this service chain.
     *
     * @param scope service chain's scope
     */
    void setScope(ServiceChainScope scope);

    /**
     * Returns the ID of this service chain.
     *
     * @return service chain's id
     */
    ServiceChainId id();

    /**
     * Sets the ID of this service chain.
     *
     * @param id service chain's id
     */
    void setId(ServiceChainId id);

    /**
     * Returns the number of desired CPU cores for this service chain.
     *
     * @return service chain's number of CPU cores
     */
    int cpuCores();

    /**
     * Sets the number of desired CPU cores for this service chain.
     *
     * @param cpuCores service chain's number of CPU cores
     */
    void setCpuCores(int cpuCores);

    /**
     * Returns the maximum number of CPU cores for this service chain.
     *
     * @return service chain's maximum number of CPU cores
     */
    int maxCpuCores();

    /**
     * Sets the maximum number of CPU cores for this service chain.
     *
     * @param cpuCores service chain's maximum number of CPU cores
     */
    void setMaxCpuCores(int maxCpuCores);

    /**
     * Returns whether this service chain can scale or not.
     *
     * @return service chain's scaling ability
     */
    boolean scale();

    /**
     * Sets the scaling ability of this service chain.
     *
     * @param scale service chain's scaling ability
     */
    void setScale(boolean scale);

    /**
     * Returns whether this service chain can auto-scale or not.
     *
     * @return service chain's auto-scaling ability
     */
    boolean autoScale();

    /**
     * Sets the auto-scaling ability of this service chain.
     *
     * @param autoScale service chain's auto-scaling ability
     */
    void setAutoScale(boolean autoScale);

    /**
     * Returns the number of NICs required for this service chain.
     *
     * @return service chain's number of NICs
     */
    int nics();

    /**
     * Returns the state of this service chain.
     *
     * @return service chain's state
     */
    ServiceChainState state();

    /**
     * Sets the state of this service chain.
     *
     * @param state service chain's state
     */
    void setState(ServiceChainState state);

    /**
     * Returns the graph of network functions (i.e., service chain graph)
     * associated with this service chain.
     *
     * @return service chain's network functions' graph
     */
    ServiceChainGraphInterface serviceChainGraph();

    /**
     * Sets the graph of network functions (i.e., service chain graph)
     * associated with this service chain.
     *
     * @param scGraph service chain's network functions' graph
     */
    void setServiceChainGraph(ServiceChainGraphInterface scGraph);

    /**
     * Returns the set of ingress points of this service chain.
     *
     * @return service chain's set of ingress points
     */
    Set<TrafficPoint> ingressPoints();

    /**
     * Sets the set of ingress points of this service chain.
     *
     * @param ingressPoints service chain's set of ingress points
     */
    void setIngressPoints(Set<TrafficPoint> ingressPoints);

    /**
     * Returns the set of egress points of this service chain.
     *
     * @return service chain's set of egress points
     */
    Set<TrafficPoint> egressPoints();

    /**
     * Sets the set of egress points of this service chain.
     *
     * @param egressPoints service chain's set of egress points
     */
    void setEgressPoints(Set<TrafficPoint> egressPoints);

    /**
     * Checks whether the input ingress point belongs to the set
     * of ingress points of this service chain.
     *
     * @param ingressPoint input ingress point
     * @return boolean status (belongs or not)
     */
    boolean isIngressPoint(TrafficPoint ingressPoint);

    /**
     * Checks whether the input device belongs to the set
     * of ingress points of this service chain.
     *
     * @param deviceId input device ID
     * @return boolean status (belongs or not)
     */
    boolean isIngressPoint(DeviceId deviceId);

    /**
     * Checks whether the input egress point belongs to the set
     * of egress points of this service chain.
     *
     * @param egressPoint input egress point
     * @return boolean status (belongs or not)
     */
    boolean isEgressPoint(TrafficPoint egressPoint);

    /**
     * Checks whether the input device belongs to the set
     * of egress points of this service chain.
     *
     * @param deviceId input device ID
     * @return boolean status (belongs or not)
     */
    boolean isEgressPoint(DeviceId deviceId);

    /**
     * Returns the ingress point that corresponds to the
     * input device ID.
     *
     * @param deviceId device ID that is a potential ingress point
     * @return TrafficPoint object that corresponds to input device
     */
    TrafficPoint ingressPointOfDevice(DeviceId deviceId);

    /**
     * Returns the egress point that corresponds to the
     * input device ID.
     *
     * @param deviceId device ID that is a potential egress point
     * @return TrafficPoint object that corresponds to input device
     */
    TrafficPoint egressPointOfDevice(DeviceId deviceId);

    /**
     * Returns whether a service chain's scope is server-level or not.
     *
     * @return boolean scope status
     */
    boolean isServerLevel();

    /**
     * Returns whether a service chain's scope is network-wide or not.
     *
     * @return boolean scope status
     */
    boolean isNetworkWide();

    /**
     * Returns whether a service chain's scope is software-based or not.
     *
     * @return boolean scope status
     */
    boolean isSoftwareBased();

    /**
     * Returns whether a service chain's scope is hardware-based or not.
     *
     * @return boolean scope status
     */
    boolean isHardwareBased();
}
