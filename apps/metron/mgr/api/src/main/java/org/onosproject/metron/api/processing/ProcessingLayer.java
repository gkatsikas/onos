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

package org.onosproject.metron.api.processing;

import org.onosproject.metron.api.classification.trafficclass.HeaderField;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rough representation of the network stack.
 */
public enum ProcessingLayer {

    PHYSICAL("PHYSICAL"),
    LINK("LINK"),
    NETWORK("NETWORK"),
    TRANSPORT("TRANSPORT"),
    APPLICATION("APPLICATION"),
    UNKNOWN("UNKNOWN");

    private String processingLayer;

    // Statically maps names to their processing layers
    private static final Map<String, ProcessingLayer> MAP =
        new ConcurrentHashMap<String, ProcessingLayer>();

    static {
        for (ProcessingLayer layer : ProcessingLayer.values()) {
            MAP.put(layer.toString(), layer);
        }
    }

    // Statically maps processing layers to their weights
    private static final Map<ProcessingLayer, Integer> WEIGHT =
        new ConcurrentHashMap<ProcessingLayer, Integer>();

    static {
        WEIGHT.put(PHYSICAL,    5);
        WEIGHT.put(LINK,        4);
        WEIGHT.put(NETWORK,     3);
        WEIGHT.put(TRANSPORT,   2);
        WEIGHT.put(APPLICATION, 1);
        WEIGHT.put(UNKNOWN,     0);
    }

    private ProcessingLayer(String processingLayer) {
        this.processingLayer = processingLayer;
    }

    /**
     * Returns a ProcessingLayer object created by an
     * input processing layer name.
     *
     * @param nameStr the name of the block to be created
     * @return ProcessingLayer object
     */
    public static ProcessingLayer getByName(String nameStr) {
        return MAP.get(nameStr);
    }

    /**
     * Creates a processing layer based upon the
     * input header field.
     *
     * @param headerField header field out of which a
     *        processing layer is created
     * @return ProcessingLayer created
     */
    public static ProcessingLayer createByHeaderField(HeaderField headerField) {
        if (HeaderField.isEthernet(headerField) ||
            HeaderField.isArp(headerField)      ||
            HeaderField.isVlan(headerField)) {
            return LINK;
        }

        if (HeaderField.isIp(headerField) ||
            HeaderField.isIcmp(headerField)) {
            return NETWORK;
        }

        if (HeaderField.isTcp(headerField) ||
            HeaderField.isUdp(headerField) ||
            HeaderField.isAmbiguous(headerField)) {
            return TRANSPORT;
        }

        if (HeaderField.isPayload(headerField)) {
            return APPLICATION;
        }

        return UNKNOWN;
    }

    /**
     * Compute an updated processing layer based upon
     * the current layer and the layer it will be merged with.
     *
     * @param currentLayer the current processing layer
     * @param newLayer the processing layer to be merged with
     * @return the derived processing layer
     */
    public static ProcessingLayer updateLayer(
            ProcessingLayer currentLayer, ProcessingLayer newLayer) {
        if (currentLayer == null) {
            return newLayer;
        }

        if (newLayer == null) {
            return currentLayer;
        }

        int currentWeight = WEIGHT.get(currentLayer).intValue();
        int newWeight = WEIGHT.get(newLayer).intValue();

        return (currentWeight >= newWeight) ? currentLayer : newLayer;
    }

    @Override
    public String toString() {
        return this.processingLayer;
    }

}
