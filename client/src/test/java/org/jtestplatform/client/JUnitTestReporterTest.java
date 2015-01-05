/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 *
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.client;

import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.junitxmlreport.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.JUnitTestReporter.PLATFORM_PROPERTY_PREFIX;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class JUnitTestReporterTest {
    static final Platform PLATFORM1 = PlatformKeyBuilderTest.createPlatform("Athlon", 32, 2);

    static {
        PLATFORM1.setCdrom("myCDROM");
        PLATFORM1.setMemory(123L);
    }

    static final Platform PLATFORM2 = PlatformKeyBuilderTest.createPlatform("Pentium", 32, 1);

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testGetPlatformProperties() throws Exception {
        List<Property> actualProperties = JUnitTestReporter.getPlatformProperties(PLATFORM1);

        assertThat(actualProperties.size()).isEqualTo(5);
        assertProperty(actualProperties, 0, "cdrom", PLATFORM1.getCdrom());
        assertProperty(actualProperties, 1, "cpu", PLATFORM1.getCpu());
        assertProperty(actualProperties, 2, "nbCores", PLATFORM1.getNbCores());
        assertProperty(actualProperties, 3, "wordSize", PLATFORM1.getWordSize());
        assertProperty(actualProperties, 4, "memory", PLATFORM1.getMemory());
    }

    @Test
    public void testSaveReport() throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult = new TestResult("framework", "testCase1", true);
        reporter.report(PLATFORM1, testResult);

        reporter.saveReport();

        verify(writer, times(1)).write(any(FileOutputStream.class), eq(reporter.suites));
    }

    @Test
    public void testReport_onePlatform_oneTest() throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult = new TestResult("framework", "testCase1", true);

        reporter.report(PLATFORM1, testResult);

        Testsuites suites = reporter.suites;
        assertThat(suites).as("created suites").isNotNull();
        assertThat(suites.getTestsuite()).as("number of suites").hasSize(1);
        assertTestSuite(suites, PLATFORM1, testResult.getFramework(), testResult.getTest());
    }

    @Test
    public void testReport_onePlatform_twoTests() throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult1 = new TestResult("framework", "testCase1", true);
        TestResult testResult2 = new TestResult(testResult1.getFramework(), "testCase2", true);

        reporter.report(PLATFORM1, testResult1);
        reporter.report(PLATFORM1, testResult2);

        Testsuites suites = reporter.suites;
        assertThat(suites).as("created suites").isNotNull();
        assertThat(suites.getTestsuite()).as("number of suites").hasSize(1);
        assertTestSuite(suites, PLATFORM1, testResult1.getFramework(), testResult1.getTest(), testResult2.getTest());
    }

    @Test
    public void testReport_twoPlatforms() throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult1 = new TestResult("framework", "testCase1", true);
        TestResult testResult2 = new TestResult("framework2", "testCase2", true);

        reporter.report(PLATFORM1, testResult1);
        reporter.report(PLATFORM2, testResult2);

        Testsuites suites = reporter.suites;
        assertThat(suites).as("created suites").isNotNull();
        assertThat(suites.getTestsuite()).as("number of suites").hasSize(2);
        assertTestSuite(suites, PLATFORM1, testResult1.getFramework(), testResult1.getTest());
        assertTestSuite(suites, PLATFORM2, testResult2.getFramework(), testResult2.getTest());
    }

    private void assertProperty(List<Property> actualProperties, int index, String propertyName, Object propertyValue) {
        Property property = actualProperties.get(index);
        assertThat(property.getName()).isEqualTo(PLATFORM_PROPERTY_PREFIX + propertyName);
        assertThat(property.getValue()).isEqualTo(String.valueOf(propertyValue));
    }

    private void assertTestSuite(Testsuites suites, Platform platform, String framework, String... expectedTestCaseNames) {
        String suitePackageName = new PlatformKeyBuilder().buildKey(platform);
        String suiteName = suitePackageName + '.' + framework;
        Testsuite testSuite = findTestSuite(suites, suiteName);
        List<Property> expectedProperties = JUnitTestReporter.getPlatformProperties(platform);
        List<String> testCaseNames = new ArrayList<String>(testSuite.getTestcase().size());
        for (Testcase testCase : testSuite.getTestcase()) {
            testCaseNames.add(testCase.getName());
        }

        assertThat(testSuite.getPackage()).as("suite package name").isEqualTo(suitePackageName);
        assertThat(testSuite.getProperties().getProperty()).usingFieldByFieldElementComparator().containsExactly(expectedProperties.toArray(new Property[0]));
        assertThat(testCaseNames).as("testCases for suite '" + suiteName + "'").containsExactly(expectedTestCaseNames);
    }

    private Testsuite findTestSuite(Testsuites suites, String suiteName) {
        Testsuite testSuite = JUnitTestReporter.findTestSuite(suites, suiteName);
        if (testSuite == null) {
            throw new IllegalArgumentException("can't find testSuite '" + suiteName + "'");
        }
        return testSuite;
    }

    static class MockTestReporter extends JUnitTestReporter {
        private final JUnitXMLReportWriter writer;
        private Testsuites suites;

        public MockTestReporter(File reportDirectory, JUnitXMLReportWriter writer) {
            super(reportDirectory);
            this.writer = writer;
        }

        @Override
        Testsuites createTestSuites() {
            suites = super.createTestSuites();
            return suites;
        }

        @Override
        JUnitXMLReportWriter createJUnitXMLReportWriter() {
            return writer;
        }

        public Testsuites getSuites() {
            return suites;
        }
    }
}