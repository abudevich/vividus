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

package org.vividus.xray.cloud;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.xray.cloud.exporter.XrayCloudExporter;
import org.vividus.xray.configuration.XrayExporterOptions;
import org.vividus.xray.exporter.XrayExporter;
import org.vividus.xray.facade.XrayFacade;
import org.vividus.xray.factory.TestCaseFactory;
import org.vividus.xray.factory.TestExecutionFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith({ MockitoExtension.class, TestLoggerFactoryExtension.class })
class XrayExporterCloudTests
{
    private static final Path ROOT = Paths.get("path");

    @Spy
    private XrayExporterOptions xrayExporterOptions;
    @Mock
    private TestCaseFactory testCaseFactory;
    @Mock private XrayFacade xrayFacade;
    @Mock private TestExecutionFactory testExecutionFactory;
    @InjectMocks
    private XrayCloudExporter xrayCloudExporter;

    private final TestLogger logger = TestLoggerFactory.getTestLogger(XrayExporterCloudTests.class);

    @BeforeEach
    void beforeEach()
    {
        xrayExporterOptions.setTestCaseUpdatesEnabled(true);
        xrayExporterOptions.setTestExecutionKey("NUC-123");
//        xrayExporterOptions.setTestExecutionAttachments(List.of(ROOT));
        xrayExporterOptions.setJsonResultsDirectory(Path.of("/Users/Aliona_Budzevich/Desktop/story_level/jbehave"));
    }

    @Test
    void test() throws IOException {
        // configure exporter
        this.xrayCloudExporter.exportResults();
    }
}
