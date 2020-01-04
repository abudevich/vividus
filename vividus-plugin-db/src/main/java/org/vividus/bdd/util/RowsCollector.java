/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.bdd.util;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.hash.HashCode;

import org.apache.commons.lang3.tuple.Pair;

public enum RowsCollector
{
    DISTINCT
    {
        @Override
        public Collector<Pair<HashCode, Map<String, Object>>, ?, Map<Object, Map<String, Object>>> get()
        {
            return Collectors.toMap(Pair::getLeft, Pair::getRight, (e1, e2) -> e1);
        }
    },
    NOOP
    {
        @Override
        public Collector<Pair<HashCode, Map<String, Object>>, ?, Map<Object, Map<String, Object>>> get()
        {
            return Collectors.toMap(Pair::getLeft, Pair::getRight);
        }
    };

    public abstract Collector<Pair<HashCode, Map<String, Object>>, ?, Map<Object, Map<String, Object>>> get();
}
