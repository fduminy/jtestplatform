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
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.TestName;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.junitxmlreport.Error;
import org.jtestplatform.junitxmlreport.*;
import org.jtestplatform.junitxmlreport.Properties;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A {@linkplain org.jtestplatform.client.TestReporter} generating a report in JUnit format.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class JUnitTestReporter implements TestReporter {
    static final String PLATFORM_PROPERTY_PREFIX = "platform.";
    private static final PlatformKeyBuilder PLATFORM_KEY_BUILDER = new PlatformKeyBuilder();

    private final Object reportLock = new Object();
    final NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);

    private final File reportDirectory;
    private final Map<Testsuite, Stats> testSuiteStats = new HashMap<Testsuite, Stats>();
    private Testsuites suites;

    public JUnitTestReporter(File reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    @Override
    public void report(Platform platform, TestResult testResult, Duration testDuration) throws Exception {
        synchronized (reportLock) {
            ensureTestSuitesCreated();

            String suitePackageName = PLATFORM_KEY_BUILDER.buildKey(platform);
            String suiteName = suitePackageName + '.' + testResult.getFramework();
            Testsuite suite = findTestSuite(suites, suiteName);
            Stats stats;
            if (suite == null) {
                suite = new Testsuite();
                suite.setPackage(suitePackageName);
                suite.setName(suiteName);
                final Properties properties = new Properties();
                properties.getProperty().addAll(getPlatformProperties(platform));
                suite.setProperties(properties);
                suites.getTestsuite().add(suite);

                stats = new Stats(testDuration);
                testSuiteStats.put(suite, stats);
            } else {
                stats = testSuiteStats.get(suite);
                stats.addDuration(testDuration);
            }

            Testcase testCase = new Testcase();
            TestName testName = TestName.parse(testResult.getTest());
            testCase.setClassname(testName.getTestClass());
            testCase.setName(testName.getMethodName());
            testCase.setTime(durationToString(testDuration));
            stats.nbTests++;
            if (testResult.isIgnored()) {
                testCase.setSkipped("");
                stats.nbSkipped++;
            } else if (!testResult.isSuccess()) {
                testCase.getSystemOut().add(testResult.getSystemOut());
                testCase.getSystemErr().add(testResult.getSystemErr());
                if (testResult.isError()) {
                    Error error = new Error();
                    error.setType(testResult.getFailureType());
                    error.setContent(testResult.getFailureContent());
                    error.setMessage(testResult.getFailureMessage());
                    testCase.getError().add(error);
                    stats.nbErrors++;
                } else {
                    Failure failure = new Failure();
                    failure.setType(testResult.getFailureType());
                    failure.setContent(testResult.getFailureContent());
                    failure.setMessage(testResult.getFailureMessage());
                    testCase.getFailure().add(failure);
                    stats.nbFailures++;
                }
            }
            suite.getTestcase().add(testCase);
        }
    }

    private void ensureTestSuitesCreated() {
        if (suites == null) {
            suites = createTestSuites();
        }
    }

    @Override
    public void saveReport() throws Exception {
        ensureTestSuitesCreated();

        for (Testsuite suite : suites.getTestsuite()) {
            Stats stats = testSuiteStats.get(suite);
            suite.setTime(durationToString(stats.duration));
            suite.setTests(Integer.toString(stats.nbTests));
            suite.setSkipped(Integer.toString(stats.nbSkipped));
            suite.setErrors(Integer.toString(stats.nbErrors));
            suite.setFailures(Integer.toString(stats.nbFailures));
        }

        JUnitXMLReportWriter writer = createJUnitXMLReportWriter();
        writer.write(new FileOutputStream(new File(reportDirectory, "tests.xml")), suites);
    }

    Testsuites createTestSuites() {
        return new Testsuites();
    }

    JUnitXMLReportWriter createJUnitXMLReportWriter() {
        return new JUnitXMLReportWriter();
    }

    static List<Property> getPlatformProperties(Platform platform) {
        List<Property> expectedProperties = new ArrayList<Property>();
        expectedProperties.add(createProperty(PLATFORM_PROPERTY_PREFIX + "cdrom", platform.getCdrom()));
        expectedProperties.add(createProperty(PLATFORM_PROPERTY_PREFIX + "cpu", platform.getCpu()));
        expectedProperties.add(createProperty(PLATFORM_PROPERTY_PREFIX + "nbCores", platform.getNbCores()));
        expectedProperties.add(createProperty(PLATFORM_PROPERTY_PREFIX + "wordSize", platform.getWordSize()));
        expectedProperties.add(createProperty(PLATFORM_PROPERTY_PREFIX + "memory", platform.getMemory()));
        return expectedProperties;
    }

    static Testsuite findTestSuite(Testsuites suites, String suiteName) {
        Testsuite testSuite = null;
        for (Testsuite suite : suites.getTestsuite()) {
            if ((suite.getName() != null) && suite.getName().equals(suiteName)) {
                testSuite = suite;
                break;
            }
        }

        return testSuite;
    }

    private String durationToString(Duration testDuration) {
        return format.format(((double) testDuration.inMillis()) / SECONDS.toMillis(1));
    }

    private static Property createProperty(String propertyName, Object propertyValue) {
        Property property = new Property();
        property.setName(propertyName);
        property.setValue(String.valueOf(propertyValue));
        return property;
    }

    private static class Stats {
        private Duration duration;
        private int nbTests = 0;
        private int nbErrors = 0;
        private int nbSkipped = 0;
        private int nbFailures = 0;

        private Stats(Duration duration) {
            this.duration = duration;
        }

        public void addDuration(Duration testDuration) {
            duration = duration.plus(testDuration);
        }
    }
}
