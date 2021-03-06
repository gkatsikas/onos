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
package org.onosproject.cli.net.completer;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.net.AnnotationKeys;

/**
 * Completer for annotation keys declared in {@link AnnotationKeys}.
 */
@Service
public class AnnotationKeysCompleter extends AbstractChoicesCompleter {


    @Override
    protected List<String> choices() {

        return Arrays.asList(AnnotationKeys.class.getFields())
            .stream()
            .filter(f -> f.getType() == String.class)
            .filter(f -> Modifier.isStatic(f.getModifiers()))
            .map(f -> {
                try {
                    return (String) f.get(null);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

}
