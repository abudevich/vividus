/*
 * Copyright 2019-2024 the original author or authors.
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

package org.vividus.http;

import java.lang.reflect.Method;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.AfterStory;
import org.jbehave.core.steps.NullStepMonitor;
import org.vividus.http.client.CookieStoreProvider;
import org.vividus.http.client.ThreadedBasicCookieStore;

public class CookieStoreProviderImpl extends NullStepMonitor implements CookieStoreProvider
{
    private final CookieStoreLevel cookieStoreLevel;
    private final CookieStore cookieStore;

    public CookieStoreProviderImpl(CookieStoreLevel cookieStoreLevel)
    {
        this.cookieStoreLevel = cookieStoreLevel;
        this.cookieStore =
                cookieStoreLevel == CookieStoreLevel.GLOBAL ? new BasicCookieStore() : new ThreadedBasicCookieStore();
    }

    @Override
    public CookieStore getCookieStore()
    {
        return cookieStore;
    }

    @Override
    public void afterPerforming(String step, boolean dryRun, Method method)
    {
        if (!dryRun && cookieStoreLevel == CookieStoreLevel.STEP)
        {
            cookieStore.clear();
        }
    }

    @AfterScenario
    public void resetScenarioCookies()
    {
        if (cookieStoreLevel == CookieStoreLevel.SCENARIO)
        {
            cookieStore.clear();
        }
    }

    @AfterStory
    public void resetStoryCookies()
    {
        if (cookieStoreLevel == CookieStoreLevel.STORY)
        {
            cookieStore.clear();
        }
    }
}
