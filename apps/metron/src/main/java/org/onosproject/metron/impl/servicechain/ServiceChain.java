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

package org.onosproject.metron.impl.servicechain;

import org.onosproject.metron.api.config.TrafficPoint;
import org.onosproject.metron.api.graphs.ServiceChainGraphInterface;
import org.onosproject.metron.api.servicechain.ServiceChainId;
import org.onosproject.metron.api.servicechain.ServiceChainScope;
import org.onosproject.metron.api.servicechain.ServiceChainState;
import org.onosproject.metron.api.servicechain.ServiceChainInterface;

import org.onosproject.net.DeviceId;

import com.google.common.base.Strings;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang.ArrayUtils;

import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.metron.api.servicechain.ServiceChainState.INIT;

/**
 * Implementation of a Metron service chain.
 */
public class ServiceChain implements ServiceChainInterface {

    private static final Logger log = getLogger(ServiceChain.class);

    /**
     * Default amount of processing resources for a service chain.
     */
    private static final int DEFAULT_CPU_CORES = 1;

    private String                     name;
    private String                     type;
    private ServiceChainScope          scope;
    private ServiceChainId             id;
    private int                        cpuCores;
    private ServiceChainState          state;
    private ServiceChainGraphInterface serviceChainGraph;
    private Set<TrafficPoint>          ingressPoints;
    private Set<TrafficPoint>          egressPoints;

    protected ServiceChain(
            String                     name,
            String                     type,
            ServiceChainScope          scope,
            ServiceChainId             id,
            int                        cpuCores,
            ServiceChainState          state,
            ServiceChainGraphInterface serviceChainGraph,
            Set<TrafficPoint>          ingressPoints,
            Set<TrafficPoint>          egressPoints) {
        // Sanity checks
        checkArgument(
            !Strings.isNullOrEmpty(name),
            "Service chain name is NULL or empty"
        );
        checkArgument(
            !Strings.isNullOrEmpty(type),
            "Service chain type is NULL or empty"
        );
        checkArgument(
            ServiceChainScope.isValid(scope),
            "Service chain scope " + scope + " is invalid"
        );
        checkArgument(
            !Strings.isNullOrEmpty(id.toString()),
            "Service chain ID is NULL or empty"
        );
        checkArgument(
            cpuCores > 0,
            "The number of CPU cores to be allocated for this service chain must be positive"
        );
        checkNotNull(
            state,
            "Service chain state is NULL"
        );
        if (!ArrayUtils.contains(ServiceChainState.values(), state)) {
            throw new IllegalArgumentException(String.valueOf(state));
        }
        checkNotNull(
            serviceChainGraph,
            "Service chain graph is NULL"
        );
        checkArgument(
            (ingressPoints != null) && !ingressPoints.isEmpty(),
            "Service chain's ingress points are NULL or empty"
        );
        checkArgument(
            (egressPoints != null) && !egressPoints.isEmpty(),
            "Service chain's egress points are NULL or empty"
        );

        this.name              = name;
        this.type              = type;
        this.scope             = scope;
        this.id                = id;
        this.cpuCores          = cpuCores;
        this.state             = state;
        this.serviceChainGraph = serviceChainGraph;
        this.ingressPoints     = ingressPoints;
        this.egressPoints      = egressPoints;
    }

    /**
     * Returns a service chain with new state.
     *
     * @param sc service chain
     * @param state service chain init state
     * @return service chain
     */
    public static ServiceChain getUpdatedServiceChain(
            ServiceChain sc,
            ServiceChainState state) {
        return new ServiceChain(
            sc.name,
            sc.type,
            sc.scope,
            sc.id,
            sc.cpuCores,
            state,
            sc.serviceChainGraph,
            sc.ingressPoints,
            sc.egressPoints
        );
    }

    /**
     * Updates a service chain.
     *
     * @param scOld old service chain
     * @param scNew new service chain
     * @return updated service chain
     */
    public static ServiceChainInterface updateServiceChain(
            ServiceChainInterface scOld,
            ServiceChainInterface scNew) {
        scOld.setName(scNew.name());
        scOld.setType(scNew.type());
        scOld.setScope(scNew.scope());
        scOld.setId(scNew.id());
        scOld.setState(scNew.state());
        scOld.setCpuCores(scNew.cpuCores());
        scOld.setServiceChainGraph(scNew.serviceChainGraph());
        scOld.setIngressPoints(scNew.ingressPoints());
        scOld.setEgressPoints(scNew.egressPoints());

        return scOld;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String type() {
        return this.type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public ServiceChainScope scope() {
        return this.scope;
    }

    @Override
    public void setScope(ServiceChainScope scope) {
        this.scope = scope;
    }

    @Override
    public ServiceChainId id() {
        return this.id;
    }

    @Override
    public void setId(ServiceChainId id) {
        this.id = id;
    }

    @Override
    public int cpuCores() {
        return this.cpuCores;
    }

    @Override
    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    @Override
    public ServiceChainState state() {
        return this.state;
    }

    @Override
    public void setState(ServiceChainState state) {
        this.state = state;
    }

    @Override
    public ServiceChainGraphInterface serviceChainGraph() {
        return this.serviceChainGraph;
    }

    @Override
    public void setServiceChainGraph(
            ServiceChainGraphInterface scGraph) {
        this.serviceChainGraph = scGraph;
    }

    @Override
    public Set<TrafficPoint> ingressPoints() {
        return this.ingressPoints;
    }

    @Override
    public void setIngressPoints(Set<TrafficPoint> ingressPoints) {
        checkArgument(
            (ingressPoints != null) && !ingressPoints.isEmpty(),
            "Service chain's ingress points are NULL or empty"
        );
        this.ingressPoints = ingressPoints;
    }

    @Override
    public Set<TrafficPoint> egressPoints() {
        return this.egressPoints;
    }

    @Override
    public void setEgressPoints(Set<TrafficPoint> egressPoints) {
        checkArgument(
            (egressPoints != null) && !egressPoints.isEmpty(),
            "Service chain's egress points are NULL or empty"
        );
        this.egressPoints = egressPoints;
    }

    @Override
    public boolean isIngressPoint(TrafficPoint ingressPoint) {
        checkNotNull(
            ingressPoint, "Ingress point is NULL or empty"
        );

        return this.ingressPoints.contains(ingressPoint);
    }

    @Override
    public boolean isIngressPoint(DeviceId deviceId) {
        checkNotNull(
            deviceId, "Cannot retrieve ingress point of NULL device"
        );

        for (TrafficPoint tp : this.ingressPoints) {
            if (tp.deviceId().equals(deviceId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEgressPoint(TrafficPoint egressPoint) {
        checkNotNull(
            egressPoint, "Egress point is NULL or empty"
        );

        return this.egressPoints.contains(egressPoint);
    }

    @Override
    public boolean isEgressPoint(DeviceId deviceId) {
        checkNotNull(
            deviceId, "Cannot retrieve egress point of NULL device"
        );

        for (TrafficPoint tp : this.egressPoints) {
            if (tp.deviceId().equals(deviceId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public TrafficPoint ingressPointOfDevice(DeviceId deviceId) {
        checkNotNull(
            deviceId, "Cannot retrieve ingress point of NULL device"
        );

        for (TrafficPoint tp : this.ingressPoints) {
            if (tp.deviceId().equals(deviceId)) {
                return tp;
            }
        }

        return null;
    }

    @Override
    public TrafficPoint egressPointOfDevice(DeviceId deviceId) {
        checkNotNull(
            deviceId, "Cannot retrieve egress point of NULL device"
        );

        for (TrafficPoint tp : this.egressPoints) {
            if (tp.deviceId().equals(deviceId)) {
                return tp;
            }
        }

        return null;
    }

    @Override
    public boolean isServerLevel() {
        return ServiceChainScope.isServerLevel(this.scope);
    }

    @Override
    public boolean isNetworkWide() {
        return ServiceChainScope.isNetworkWide(this.scope);
    }

    @Override
    public boolean isSoftwareBased() {
        return ServiceChainScope.isSoftwareBased(this.scope);
    }

    @Override
    public boolean isHardwareBased() {
        return ServiceChainScope.isHardwareBased(this.scope);
    }

    /**
     * Returns a label with the module's identify.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return "Service Chain";
    }

    /**
     * Compares two service chains.
     *
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ServiceChain) {
            ServiceChain that = (ServiceChain) obj;
            if (Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.scope, that.scope) &&
                Objects.equals(this.id,   that.id) &&
                this.cpuCores == that.cpuCores &&
                Objects.equals(
                    this.serviceChainGraph, that.serviceChainGraph
                ) &&
                Objects.equals(
                    this.ingressPoints, that.ingressPoints
                ) &&
                Objects.equals(
                    this.egressPoints, that.egressPoints
                )) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name",          name)
                .add("type",          type)
                .add("scope",         scope.toString())
                .add("id",            id.toString())
                .add("cpuCores",      String.valueOf(cpuCores()))
                .add("state",         state.name())
                .add("graph",         serviceChainGraph.toString())
                .add("ingressPoints", ingressPoints.toString())
                .add("egressPoints",  egressPoints.toString())
                .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return service chain builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of service chain entities.
     */
    public static final class Builder {
        private String                     name;
        private String                     type;
        private ServiceChainScope          scope;
        private ServiceChainId             id;
        private int                        cpuCores;
        private ServiceChainState          state = INIT;
        private ServiceChainGraphInterface serviceChainGraph = null;
        private Set<TrafficPoint>          ingressPoints = null;
        private Set<TrafficPoint>          egressPoints = null;

        private Builder() {
        }

        public ServiceChain build() {
            return new ServiceChain(
                name, type, scope, id, cpuCores, state,
                serviceChainGraph, ingressPoints,
                egressPoints
            );
        }

        /**
         * Returns service chain builder with the name.
         *
         * @param name name
         * @return service chain builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Returns service chain builder with the type.
         *
         * @param type type
         * @return service chain builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Returns service chain builder with the scope.
         *
         * @param scope scope
         * @return service chain builder
         */
        public Builder scope(ServiceChainScope scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Returns service chain builder with the ID.
         *
         * @param id service chain ID
         * @return service chain builder
         */
        public Builder id(String id) {
            this.id = (ServiceChainId) ServiceChainId.id(id);
            return this;
        }

        /**
         * Returns service chain builder with the CPU cores.
         *
         * @param cpuCores the number of CPU cores
         * @return service chain builder
         */
        public Builder cpuCores(int cpuCores) {
            this.cpuCores = cpuCores;
            return this;
        }

        /**
         * Returns service chain builder with the state.
         *
         * @param state state
         * @return service chain builder
         */
        public Builder state(ServiceChainState state) {
            this.state = state;
            return this;
        }

        /**
         * Returns service chain builder with a processing graph.
         *
         * @param scGraph service chain graph
         * @return service chain builder
         */
        public Builder serviceChainGraph(ServiceChainGraphInterface scGraph) {
            this.serviceChainGraph = scGraph;
            return this;
        }

        /**
         * Returns service chain builder with ingress points.
         *
         * @param ingressPoints service chain's ingress points
         * @return service chain builder
         */
        public Builder ingressPoints(Set<TrafficPoint> ingressPoints) {
            this.ingressPoints = ingressPoints;
            return this;
        }

        /**
         * Returns service chain builder with egress points.
         *
         * @param egressPoints service chain's egress points
         * @return service chain builder
         */
        public Builder egressPoints(Set<TrafficPoint> egressPoints) {
            this.egressPoints = egressPoints;
            return this;
        }

    }

}
