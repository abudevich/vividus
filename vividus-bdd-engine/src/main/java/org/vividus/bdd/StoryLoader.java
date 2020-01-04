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

package org.vividus.bdd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FilenameUtils;
import org.jbehave.core.io.LoadFromClasspath;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.vividus.bdd.examples.IExamplesTableLoader;

public class StoryLoader extends LoadFromClasspath
{
    private IExamplesTableLoader examplesTableLoader;
    private ResourcePatternResolver resourcePatternResolver;

    public StoryLoader()
    {
        super(StandardCharsets.UTF_8);
    }

    @Override
    public String loadResourceAsText(String resourcePath)
    {
        if ("table".equals(FilenameUtils.getExtension(resourcePath)))
        {
            return examplesTableLoader.loadExamplesTable(resourcePath);
        }
        return super.loadResourceAsText(resourcePath);
    }

    @Override
    protected InputStream resourceAsStream(String resourcePath)
    {
        try
        {
            return resourcePatternResolver.getResource(resourcePath).getInputStream();
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public void setExamplesTableLoader(IExamplesTableLoader examplesTableLoader)
    {
        this.examplesTableLoader = examplesTableLoader;
    }

    public void setResourcePatternResolver(ResourcePatternResolver resourcePatternResolver)
    {
        this.resourcePatternResolver = resourcePatternResolver;
    }
}
