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

package org.vividus.xray.cloud.exporter;

import org.apache.commons.lang3.function.FailableBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.model.jbehave.Meta;
import org.vividus.model.jbehave.NotUniqueMetaValueException;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.output.OutputReader;
import org.vividus.output.SyntaxException;
import org.vividus.xray.configuration.XrayExporterOptions;
import org.vividus.xray.exporter.XrayExporter;
import org.vividus.xray.facade.AbstractTestCaseParameters;
import org.vividus.xray.facade.CucumberTestCaseParameters;
import org.vividus.xray.facade.ManualTestCaseParameters;
import org.vividus.xray.facade.XrayFacade;
import org.vividus.xray.factory.TestCaseFactory;
import org.vividus.xray.factory.TestExecutionFactory;
import org.vividus.xray.model.AbstractTestCase;
import org.vividus.xray.model.TestCaseType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.lang.System.lineSeparator;

@Component
public class XrayCloudExporter extends XrayExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(XrayCloudExporter.class);

    @Autowired
    private XrayExporterOptions xrayExporterOptions;
    @Autowired
    private XrayFacade xrayFacade;
    @Autowired
    private TestCaseFactory testCaseFactory;
    @Autowired
    private TestExecutionFactory testExecutionFactory;

    private final List<String> errors = new ArrayList<>();

    private final Map<TestCaseType, Function<AbstractTestCaseParameters, AbstractTestCase>> testCaseFactories = Map.of(
            TestCaseType.MANUAL, p -> testCaseFactory.createManualTestCase((ManualTestCaseParameters) p),
            TestCaseType.CUCUMBER, p -> testCaseFactory.createCucumberTestCase((CucumberTestCaseParameters) p)
    );


    private final Map<TestCaseType, CreateParametersFunction> parameterFactories = Map.of(
            TestCaseType.MANUAL, this::createManualTestCaseParameters,
            TestCaseType.CUCUMBER, (title, scenario) -> createCucumberTestCaseParameters(scenario)
    );

    public void exportResults() throws IOException {
        List<Map.Entry<String, Scenario>> testCases = new ArrayList<>();
        for (Story story : OutputReader.readStoriesFromJsons(xrayExporterOptions.getJsonResultsDirectory())) {
            Set<String> storyLevelTestCaseIds = story.getMetaValues("testCaseId");

            LOGGER.atInfo().addArgument(story::getPath).addArgument(storyLevelTestCaseIds)
                    .log("Exporting scenarios from {} story with testCaseIds {}");

            // prepare story meta for scenario
            Meta storyMeta = new Meta();
            storyMeta.setName("testCaseId");
            storyMeta.setValue(String.join(";", storyLevelTestCaseIds));

            for (Scenario scenario : story.getFoldedScenarios()) {
                if (!storyLevelTestCaseIds.isEmpty()) {
                    TestCaseIdMetaMerger.mergeTestCaseIdMeta(storyMeta, scenario);
                }
//                exportScenario(story.getPath(), scenario).ifPresent(testCases::add);
            }

            addTestCasesToTestExecution(testCases);
//            publishErrors();
        }
    }

    private Optional<Map.Entry<String, Scenario>> exportScenario(String storyTitle, Scenario scenario) {
        String scenarioTitle = scenario.getTitle();

        LOGGER.atInfo().addArgument(scenarioTitle).log("Exporting {} scenario");

        try {
            TestCaseType testCaseType = scenario.isManual() ? TestCaseType.MANUAL : TestCaseType.CUCUMBER;

            Set<String> testCaseIds = scenario.getMetaValues("testCaseId");

            AbstractTestCaseParameters parameters = parameterFactories.get(testCaseType).apply(scenarioTitle, scenario);
            AbstractTestCase testCase = testCaseFactories.get(testCaseType).apply(parameters);

            if (!testCaseIds.isEmpty()) {
                String exampleId = testCaseIds.stream().toList().get(0);
                xrayFacade.updateTestCase(exampleId, testCase);
                createTestsLink(exampleId, scenario);
            }

            return null;
        } catch (IOException | SyntaxException | XrayFacade.NonEditableIssueStatusException
                | NotUniqueMetaValueException | JiraConfigurationException e) {
            String errorMessage = "Story: " + storyTitle + lineSeparator() + "Scenario: " + scenarioTitle
                    + lineSeparator() + "Error: " + e.getMessage();
            errors.add(errorMessage);
            LOGGER.atError().setCause(e).log("Got an error while exporting");
            return Optional.empty();
        }
    }
}
