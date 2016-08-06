/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.client;

import com.google.code.tempusfugit.temporal.Duration;
import org.apache.commons.lang3.StringUtils;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.ConfigUtils;
import org.jtestplatform.common.TestName;
import org.jtestplatform.common.TestNameTest;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.junitxmlreport.Error;
import org.jtestplatform.junitxmlreport.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.JUnitTestReporter.PLATFORM_PROPERTY_PREFIX;
import static org.jtestplatform.client.JUnitTestReporterTest.TestedClass.Method.method1;
import static org.jtestplatform.client.JUnitTestReporterTest.TestedClass.Method.method2;
import static org.jtestplatform.common.transport.TransportHelperTest.SYSTEM_ERR;
import static org.jtestplatform.common.transport.TransportHelperTest.SYSTEM_OUT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class JUnitTestReporterTest {
    public static final Duration DURATION1 = millis(1);
    public static final Duration DURATION2 = millis(3);

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testGetPlatformProperties() throws Exception {
        List<Property> actualProperties = JUnitTestReporter.getPlatformProperties(Utils.PLATFORM1);

        assertThat(actualProperties.size()).isEqualTo(5);
        assertProperty(actualProperties, 0, "cdrom", Utils.PLATFORM1.getCdrom());
        assertProperty(actualProperties, 1, "cpu", Utils.PLATFORM1.getCpu());
        assertProperty(actualProperties, 2, "nbCores", Utils.PLATFORM1.getNbCores());
        assertProperty(actualProperties, 3, "wordSize", Utils.PLATFORM1.getWordSize());
        assertProperty(actualProperties, 4, "memory", Utils.PLATFORM1.getMemory());
    }

    @Test
    public void testSaveReport() throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult = new TestResult("framework", "testCase1");
        reporter.report(Utils.PLATFORM1, testResult, millis(1));

        reporter.saveReport();

        verify(writer, times(1)).write(any(FileOutputStream.class), eq(reporter.suites));
    }

    @Test
    public void testSaveReport_noCallToReport() throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);

        reporter.saveReport();

        verify(writer, times(1)).write(any(FileOutputStream.class), eq(reporter.suites));
    }

    @Theory
    public void testReport_onePlatform_oneTest(ResultType resultType) throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult = resultType.createTestResult("framework", method1);

        reporter.report(Utils.PLATFORM1, testResult, DURATION1);

        reporter.saveReport(); // triggers computation of suite properties
        Testsuites suites = reporter.suites;
        assertThat(suites).as("created suites").isNotNull();
        assertThat(suites.getTestsuite()).as("number of suites").hasSize(1);
        assertTestSuite(reporter, suites, Utils.PLATFORM1, testResult.getFramework(),
                        new TestReport(testResult, DURATION1));
    }

    @Theory
    public void testReport_onePlatform_twoTests(ResultType resultType) throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult1 = resultType.createTestResult("framework", method1);
        TestResult testResult2 = resultType.createTestResult(testResult1.getFramework(), method2);

        reporter.report(Utils.PLATFORM1, testResult1, DURATION1);
        reporter.report(Utils.PLATFORM1, testResult2, DURATION2);

        reporter.saveReport(); // triggers computation of suite properties
        Testsuites suites = reporter.suites;
        assertThat(suites).as("created suites").isNotNull();
        assertThat(suites.getTestsuite()).as("number of suites").hasSize(1);
        assertTestSuite(reporter, suites, Utils.PLATFORM1, testResult1.getFramework(),
                        new TestReport(testResult1, DURATION1),
                        new TestReport(testResult2, DURATION2));
    }

    @Theory
    public void testReport_twoPlatforms(ResultType resultType) throws Exception {
        JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
        final File reportDir = folder.getRoot();
        MockTestReporter reporter = new MockTestReporter(reportDir, writer);
        TestResult testResult1 = resultType.createTestResult("framework", method1);
        TestResult testResult2 = resultType.createTestResult("framework2", method2);

        reporter.report(Utils.PLATFORM1, testResult1, DURATION1);
        reporter.report(Utils.PLATFORM2, testResult2, DURATION2);

        reporter.saveReport(); // triggers computation of suite properties
        Testsuites suites = reporter.suites;
        assertThat(suites).as("created suites").isNotNull();
        assertThat(suites.getTestsuite()).as("number of suites").hasSize(2);
        assertTestSuite(reporter, suites, Utils.PLATFORM1, testResult1.getFramework(),
                        new TestReport(testResult1, DURATION1));
        assertTestSuite(reporter, suites, Utils.PLATFORM2, testResult2.getFramework(),
                        new TestReport(testResult2, DURATION2));
    }

    private void assertProperty(List<Property> actualProperties, int index, String propertyName, Object propertyValue) {
        Property property = actualProperties.get(index);
        assertThat(property.getName()).isEqualTo(PLATFORM_PROPERTY_PREFIX + propertyName);
        assertThat(property.getValue()).isEqualTo(String.valueOf(propertyValue));
    }

    private void assertTestSuite(JUnitTestReporter reporter, Testsuites suites, Platform platform, String framework,
                                 TestReport... expectedTestReports) throws Exception {
        String suitePackageName = new PlatformKeyBuilder().buildKey(platform);
        String suiteName = suitePackageName + '.' + framework;
        Testsuite testSuite = findTestSuite(suites, suiteName);
        List<Property> expectedProperties = JUnitTestReporter.getPlatformProperties(platform);

        List<TestReport> testReports = new ArrayList<TestReport>(testSuite.getTestcase().size());
        Duration totalDuration = millis(0);
        int nbTests = 0;
        int nbErrors = 0;
        int nbSkipped = 0;
        int nbFailures = 0;
        for (Testcase testCase : testSuite.getTestcase()) {
            Duration duration = millis(0);
            if (!StringUtils.isBlank(testCase.getTime())) {
                duration = stringToDuration(reporter, testCase.getTime());
            }

            totalDuration = totalDuration.plus(duration);
            nbTests++;
            if (!testCase.getError().isEmpty()) {
                nbErrors++;
            }
            if (testCase.getSkipped() != null) {
                nbSkipped++;
            }
            if (!testCase.getFailure().isEmpty()) {
                nbFailures++;
            }

            testReports.add(new TestReport(testCase, duration));
        }

        assertThat(testSuite.getPackage()).as("suite package name").isEqualTo(suitePackageName);
        assertThat(testSuite.getProperties().getProperty()).usingFieldByFieldElementComparator().containsExactly(
            expectedProperties.toArray(new Property[0]));
        assertThat(stringToDuration(reporter, testSuite.getTime())).as("total duration").isEqualTo(totalDuration);
        assertThat(stringToInteger(testSuite.getTests())).as("number of tests").isEqualTo(nbTests);
        assertThat(stringToInteger(testSuite.getErrors())).as("number of errors").isEqualTo(nbErrors);
        assertThat(stringToInteger(testSuite.getSkipped())).as("number of skipped").isEqualTo(nbSkipped);
        assertThat(stringToInteger(testSuite.getFailures())).as("number of failures").isEqualTo(nbFailures);
        assertThat(testReports).as("test reports for suite '" + suiteName + "'").containsExactly(expectedTestReports);
    }

    private static Integer stringToInteger(String integerString) throws ParseException {
        return ConfigUtils.isBlank(integerString) ? null : Integer.parseInt(integerString);
    }

    private static Duration stringToDuration(JUnitTestReporter reporter, String durationString) throws ParseException {
        Duration duration = millis(0);
        if (!StringUtils.isBlank(durationString)) {
            double seconds = reporter.format.parse(durationString).doubleValue();
            duration = millis((long) (seconds * SECONDS.toMillis(1)));
        }
        return duration;
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

    private static class TestReport {
        private final String test;
        private final Duration testDuration;

        private final String failureType;
        private final String failureContent;
        private final String failureMessage;
        private final boolean ignored;
        private final boolean error;
        private final String systemOut;
        private final String systemErr;

        private TestReport(Testcase testCase, Duration testDuration) throws ClassNotFoundException {
            this.test = TestName.create(testCase.getClassname(), testCase.getName()).toString();
            this.testDuration = testDuration;

            List<Failure> failures = testCase.getFailure();
            List<Error> errors = testCase.getError();
            if (!failures.isEmpty()) {
                Failure failure = failures.get(0);
                this.failureType = (failure == null) ? null : failure.getType();
                this.failureContent = (failure == null) ? null : failure.getContent();
                this.failureMessage = (failure == null) ? null : failure.getMessage();
                this.error = false;
                this.ignored = false;
                this.systemOut = StringUtils.join(testCase.getSystemOut().toArray(), '\n');
                this.systemErr = StringUtils.join(testCase.getSystemErr().toArray(), '\n');
            } else if (!errors.isEmpty()) {
                Error error = errors.get(0);
                this.failureType = error.getType();
                this.failureContent = error.getContent();
                this.failureMessage = error.getMessage();
                this.error = true;
                this.ignored = false;
                this.systemOut = StringUtils.join(testCase.getSystemOut().toArray(), '\n');
                this.systemErr = StringUtils.join(testCase.getSystemErr().toArray(), '\n');
            } else {
                this.failureType = null;
                this.failureContent = null;
                this.failureMessage = null;
                this.error = false;
                this.ignored = (testCase.getSkipped() != null);
                this.systemOut = null;
                this.systemErr = null;
            }
        }

        private TestReport(TestResult testResult, Duration testDuration) {
            this.test = testResult.getTest();
            this.testDuration = testDuration;

            this.failureType = testResult.getFailureType();
            this.failureContent = testResult.getFailureContent();
            this.failureMessage = testResult.getFailureMessage();
            this.error = testResult.isError();
            this.ignored = testResult.isIgnored();
            this.systemOut = testResult.getSystemOut();
            this.systemErr = testResult.getSystemErr();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestReport that = (TestReport) o;

            if (ignored != that.ignored) return false;
            if (error != that.error) return false;
            if (test != null ? !test.equals(that.test) : that.test != null) return false;
            if (testDuration != null ? !testDuration.equals(that.testDuration) : that.testDuration != null)
                return false;
            if (failureType != null ? !failureType.equals(that.failureType) : that.failureType != null) return false;
            if (failureContent != null ? !failureContent.equals(that.failureContent) : that.failureContent != null)
                return false;
            if (failureMessage != null ? !failureMessage.equals(that.failureMessage) : that.failureMessage != null)
                return false;
            if (systemOut != null ? !systemOut.equals(that.systemOut) : that.systemOut != null) return false;
            return !(systemErr != null ? !systemErr.equals(that.systemErr) : that.systemErr != null);

        }

        @Override
        public int hashCode() {
            int result = test != null ? test.hashCode() : 0;
            result = 31 * result + (testDuration != null ? testDuration.hashCode() : 0);
            result = 31 * result + (failureType != null ? failureType.hashCode() : 0);
            result = 31 * result + (failureContent != null ? failureContent.hashCode() : 0);
            result = 31 * result + (failureMessage != null ? failureMessage.hashCode() : 0);
            result = 31 * result + (ignored ? 1 : 0);
            result = 31 * result + (error ? 1 : 0);
            result = 31 * result + (systemOut != null ? systemOut.hashCode() : 0);
            result = 31 * result + (systemErr != null ? systemErr.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "test='" + test + '\'' +
                   ", testDuration=" + testDuration +
                   ", failureType='" + failureType + '\'' +
                   ", failureContent='" + failureContent + '\'' +
                   ", failureMessage='" + failureMessage + '\'' +
                   ", systemOut='" + systemOut + '\'' +
                   ", systemErr='" + systemErr + '\'' +
                   ", error=" + error +
                   ", ignored=" + ignored;
        }
    }

    public static enum ResultType {
        SUCCESS,
        FAILURE {
            @Override
            public TestResult createTestResult(String framework, TestedClass.Method method) {
                TestResult testResult = super.createTestResult(framework, method);
                int id = method.ordinal();
                AssertionError failure = new AssertionError("failureMessage" + id);
                testResult.setFailure(failure.getClass().getName(), "failureContent" + id, failure.getMessage(), false);
                testResult.setSystemOut(SYSTEM_OUT);
                testResult.setSystemErr(SYSTEM_ERR);
                return testResult;
            }
        },
        IGNORE {
            @Override
            public TestResult createTestResult(String framework, TestedClass.Method method) {
                TestResult testResult = super.createTestResult(framework, method);
                testResult.setIgnored();
                return testResult;
            }
        },
        ERROR {
            @Override
            public TestResult createTestResult(String framework, TestedClass.Method method) {
                TestResult testResult = super.createTestResult(framework, method);
                int id = method.ordinal();
                Exception error = new Exception("errorMessage" + id);
                testResult.setFailure(error.getClass().getName(), "errorContent" + id, error.getMessage(), true);
                testResult.setSystemOut(SYSTEM_OUT);
                testResult.setSystemErr(SYSTEM_ERR);
                return testResult;
            }
        };

        public TestResult createTestResult(String framework, TestedClass.Method method) {
            return new TestResult(framework, TestName.toString(TestNameTest.class, method.name()));
        }
    }

    public static class TestedClass {
        public static enum Method {
            method1,
            method2;
        }

        public void method1() {
            Assert.fail("failure");
        }

        public void method2() {
            Assert.fail("failure");
        }
    }
}