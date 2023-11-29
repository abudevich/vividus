/*
 * Copyright 2019-2023 the original author or authors.
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

package org.vividus.util.json;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.internal.EvaluationContext;
import com.jayway.jsonpath.internal.PathRef;
import com.jayway.jsonpath.internal.function.Parameter;
import com.jayway.jsonpath.internal.function.PathFunction;

/**
 * Take the unique items from collection of JSONArray
 * <p>
 * Created by PavelSakharchuk on 21/09/23
 */
public class Distinct implements PathFunction
{
    @Override
    public Object invoke(
            String currentPath, PathRef parent, Object model, EvaluationContext ctx, List<Parameter> parameters)
    {
        if (ctx.configuration().jsonProvider().isArray(model))
        {
            Iterable<?> objects = ctx.configuration().jsonProvider().toIterable(model);
            return StreamSupport.stream(objects.spliterator(), false).distinct().collect(Collectors.toList());
        }
        throw new JsonPathException("Aggregation function attempted unique values using non array");
    }
}
