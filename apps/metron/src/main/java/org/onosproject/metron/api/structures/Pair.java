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

package org.onosproject.metron.api.structures;

import java.util.Map;

public class Pair<K, V> implements Map.Entry<K, V> {

    private K key;
    private V value;

    public Pair() {
        this.key   = null;
        this.value = null;
    }

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Sets the key of this pair.
     *
     * @param key the key of the pair
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * Returns the key of this pair.
     *
     * @return key of the pair
     */
    @Override
    public K getKey() {
        return this.key;
    }

    /**
     * Sets the value of this pair.
     *
     * @param value the value of the pair
     * @return updated value of the pair
     */
    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }

    /**
     * Returns the value of this pair.
     *
     * @return value
     */
    @Override
    public V getValue() {
        return this.value;
    }

    /**
     * Returns whether the key is empty.
     *
     * @return boolean is empty or not
     */
    public boolean emptyKey() {
        return (this.key == null);
    }

    /**
     * Returns whether the value is empty.
     *
     * @return boolean is empty or not
     */
    public boolean emptyValue() {
        return (this.value == null);
    }

    @Override
    public String toString() {
        return "[" + this.getKey() + ": " + this.getValue() + "]";
    }

}