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

package org.vividus.bdd.spring;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import com.google.common.base.Suppliers;

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.embedder.StoryControls;
import org.jbehave.core.io.StoryLoader;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.model.TableTransformers;
import org.jbehave.core.model.TableTransformers.TableTransformer;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.reporters.ViewGenerator;
import org.jbehave.core.steps.DelegatingStepMonitor;
import org.jbehave.core.steps.ParameterConverters.ChainableParameterConverter;
import org.jbehave.core.steps.StepMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.vividus.bdd.IPathFinder;
import org.vividus.bdd.batch.BatchResourceConfiguration;
import org.vividus.bdd.steps.ExpressionAdaptor;
import org.vividus.bdd.steps.ParameterAdaptor;
import org.vividus.bdd.steps.ParameterConvertersDecorator;

public class ExtendedConfiguration extends Configuration
{
    private IPathFinder pathFinder;
    private ParameterAdaptor parameterAdaptor;
    private ExpressionAdaptor expressionAdaptor;
    private List<ChainableParameterConverter<?, ?>> customConverters;
    private List<StepMonitor> stepMonitors;
    private Map<String, TableTransformer> customTableTransformers;
    private String compositePaths;
    private StoryControls storyControls;
    private String examplesTableHeaderSeparator;
    private String examplesTableValueSeparator;

    public void init() throws IOException
    {
        initKeywords();
        initCompositePaths();
        useParameterConverters(new ParameterConvertersDecorator(this, parameterAdaptor, expressionAdaptor)
                .addConverters(customConverters));
        useStoryParser(new RegexStoryParser(keywords(), examplesTableFactory()));
        TableTransformers transformers = tableTransformers();
        customTableTransformers.forEach(transformers::useTransformer);
        useStepMonitor(new DelegatingStepMonitor(stepMonitors));
        useStoryControls(storyControls);
    }

    private void initKeywords()
    {
        Map<String, String> keywords = Keywords.defaultKeywords();
        keywords.put(Keywords.EXAMPLES_TABLE_HEADER_SEPARATOR, examplesTableHeaderSeparator);
        keywords.put(Keywords.EXAMPLES_TABLE_VALUE_SEPARATOR, examplesTableValueSeparator);
        useKeywords(new Keywords(keywords));
    }

    private void initCompositePaths() throws IOException
    {
        BatchResourceConfiguration compositeStepsBatch = new BatchResourceConfiguration();
        compositeStepsBatch.setResourceLocation("/");
        compositeStepsBatch.setResourceIncludePatterns(compositePaths);
        compositeStepsBatch.setResourceExcludePatterns(null);
        useCompositePaths(new HashSet<>(pathFinder.findPaths(compositeStepsBatch)));
    }

    public Supplier<ExamplesTableFactory> getExamplesTableFactory()
    {
        return Suppliers.memoize(this::examplesTableFactory);
    }

    public void setPathFinder(IPathFinder pathFinder)
    {
        this.pathFinder = pathFinder;
    }

    public void setStoryLoader(StoryLoader storyLoader)
    {
        useStoryLoader(storyLoader);
    }

    public void setStoryReporterBuilder(StoryReporterBuilder storyReporterBuilder)
    {
        useStoryReporterBuilder(storyReporterBuilder);
    }

    @Autowired(required = false)
    public void setViewGenerator(ViewGenerator viewGenerator)
    {
        useViewGenerator(viewGenerator);
    }

    @Autowired
    public void setStepMonitors(List<StepMonitor> stepMonitors)
    {
        this.stepMonitors = Collections.unmodifiableList(stepMonitors);
    }

    public void setCompositePaths(String compositePaths)
    {
        this.compositePaths = compositePaths;
    }

    public void setParameterAdaptor(ParameterAdaptor parameterAdaptor)
    {
        this.parameterAdaptor = parameterAdaptor;
    }

    @Inject
    public void setCustomConverters(List<ChainableParameterConverter<?, ?>> customConverters)
    {
        this.customConverters = Collections.unmodifiableList(customConverters);
    }

    @Autowired
    public void setCustomTableTransformers(Map<String, TableTransformer> customTableTransformers)
    {
        this.customTableTransformers = customTableTransformers;
    }

    public void setExpressionAdaptor(ExpressionAdaptor expressionAdaptor)
    {
        this.expressionAdaptor = expressionAdaptor;
    }

    public void setStoryControls(StoryControls storyControls)
    {
        this.storyControls = storyControls;
    }

    public void setExamplesTableHeaderSeparator(String examplesTableHeaderSeparator)
    {
        this.examplesTableHeaderSeparator = examplesTableHeaderSeparator;
    }

    public void setExamplesTableValueSeparator(String examplesTableValueSeparator)
    {
        this.examplesTableValueSeparator = examplesTableValueSeparator;
    }
}
