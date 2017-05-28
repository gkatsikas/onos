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

package org.onosproject.metron.api.common;

import java.util.UUID;
import java.util.Random;

public class UUIdGenerator {

    public static final boolean DEBUG_MODE = true;
    private Random rand;

    public UUIdGenerator() {
        rand = new Random();
    }

    public UUIdGenerator(long seed) {
        rand = new Random(seed);
    }

    private static UUIdGenerator instance;

    public static UUIdGenerator getSystemInstance() {
        if (instance == null) {
            synchronized (UUIdGenerator.class) {
                if (instance == null) {
                    instance = (DEBUG_MODE ? new UUIdGenerator(1) : new UUIdGenerator());
                }
            }
        }
        return instance;
    }

    public UUID getUUId() {
        return new UUID(rand.nextLong(), rand.nextLong());
    }

}
