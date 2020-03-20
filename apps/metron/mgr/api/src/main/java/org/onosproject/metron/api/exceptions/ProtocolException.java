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

package org.onosproject.metron.api.exceptions;

/**
 * Exceptions related to Controller <--> Server communication.
 */
public class ProtocolException extends RuntimeException {

    /**
     * Default parameterless constructor.
     */
    public ProtocolException() {
    }

    /**
     * Constructor that accepts a message.
     *
     * @param message the message to be thrown
     */
    public ProtocolException(String message) {
        super(message);
    }

    /**
     * Constructor that accepts a throwable.
     *
     * @param cause the throwable cause
     */
    public ProtocolException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor that accepts a throwable and a message.
     *
     * @param message the message to be thrown
     * @param cause the throwable cause
     */
    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

}