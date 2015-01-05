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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@linkplain org.jtestplatform.client.TestReporter} generating a report in JUnit format.
 */
public class JUnitTestReporter implements TestReporter {
    static final String PLATFORM_PROPERTY_PREFIX = "platform.";
    private static final PlatformKeyBuilder PLATFORM_KEY_BUILDER = new PlatformKeyBuilder();

    private final Object reportLock = new Object();

    private final File reportDirectory;
    private Testsuites suites;

    public JUnitTestReporter(File reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    @Override
    public void report(Platform platform, TestResult testResult) throws Exception {
        synchronized (reportLock) {
            if (suites == null) {
                suites = createTestSuites();
            }

            String suitePackageName = PLATFORM_KEY_BUILDER.buildKey(platform);
            String suiteName = suitePackageName + '.' + testResult.getFramework();
            Testsuite suite = findTestSuite(suites, suiteName);
            if (suite == null) {
                suite = new Testsuite();
                suite.setPackage(suitePackageName);
                suite.setName(suiteName);
                final Properties properties = new Properties();
                properties.getProperty().addAll(getPlatformProperties(platform));
                suite.setProperties(properties);
                suites.getTestsuite().add(suite);
            }
            Testcase testCase = new Testcase();
            testCase.setName(testResult.getTest());
            suite.getTestcase().add(testCase);
        }
    }

    @Override
    public void saveReport() throws Exception {
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

    private static Property createProperty(String propertyName, Object propertyValue) {
        Property property = new Property();
        property.setName(propertyName);
        property.setValue(String.valueOf(propertyValue));
        return property;
    }
}
