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

package org.vividus.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Hack for updating final static field of external library
 */
public final class FieldUtils
{
    private static final VarHandle MODIFIERS;

    static
    {
        try
        {
            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            throw new RuntimeException("The 'modifiers' field cannot be handled", e);
        }
    }

    private FieldUtils()
    {
    }

    public static void setFinalStatic(Field field, Object value)
    {
        try
        {
            MODIFIERS.set(field, field.getModifiers() & ~Modifier.FINAL);
            field.setAccessible(true);
            field.set(null, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(String.format("The '%s' field cannot be modified", field.getName()), e);
        }
    }
}
