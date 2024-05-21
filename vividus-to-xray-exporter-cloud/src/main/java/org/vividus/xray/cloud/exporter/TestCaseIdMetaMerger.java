package org.vividus.xray.cloud.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.model.jbehave.Meta;
import org.vividus.model.jbehave.Scenario;

import java.util.List;
import java.util.Set;

public final class TestCaseIdMetaMerger
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseIdMetaMerger.class);

    public static void mergeTestCaseIdMeta (Meta storyMeta, Scenario scenario)
    {
        Set<String> scenarioLevelTestCaseIds;
        List<Meta> initialMeta = scenario.getMeta();
        if (initialMeta == null)
        {
            scenario.setMeta(List.of(storyMeta));
        }
        else
        {
            scenarioLevelTestCaseIds = scenario.getMetaValues("testCaseId");
            System.out.println("Initial scenario meta: " + scenarioLevelTestCaseIds);
            scenario.getMeta().add(storyMeta);
        }
        System.out.println("Result scenario meta: " + String.join(";", scenario.getMetaValues("testCaseId")));
    }
}
