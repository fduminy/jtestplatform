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
package org.jtestplatform.junitxmlreport;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import java.io.*;

import static org.apache.commons.lang3.StringUtils.strip;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JUnitXMLReportWriterTest {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testWrite() throws Exception {
        String expectedXML = readResource("/testsuites-full.xml", false);

        File actualFile = folder.newFile("actualFile");
        new JUnitXMLReportWriter().write(new FileOutputStream(actualFile), buildReport());
        String actualXML = readFile(actualFile.getAbsolutePath(), false);

        compareXML(expectedXML, actualXML);
    }

    public static void compareXML(String expectedXML, String actualXML) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        Diff xmlDiff = new Diff(expectedXML, actualXML);
        try {
            assertTrue("pieces of XML are similar " + xmlDiff, xmlDiff.similar());
            assertTrue("but are they identical? " + xmlDiff, xmlDiff.identical());
        } catch (AssertionError ae) {
            System.out.println("--------------- ActualXML ---------------");
            System.out.println(actualXML);
            System.out.println("=========================================");

            System.out.println("-------------- ExpectedXML --------------");
            System.out.println(expectedXML);
            System.out.println("=========================================");
            throw ae;
        }
    }

    private Testsuites buildReport() {
        Testsuites suites = new Testsuites();
        suites.setDisabled("1");
        suites.setErrors("2");
        suites.setFailures("3");
        suites.setName("testsuites-name");
        suites.setTests("4");
        suites.setTime("5");

        Testsuite suite = new Testsuite();
        suite.setName("testsuite1-name");
        suite.setTests("10");
        suite.setDisabled("11");
        suite.setErrors("12");
        suite.setFailures("13");
        suite.setHostname("testsuite1-host");
        suite.setId("14");
        suite.setPackage("testsuite1-package");
        suite.setSkipped("15");
        suite.setTime("16");
        suite.setTimestamp("2014-01-21T16:01:02");
        addProperty(suite, "testsuite1-property1", "testsuite1-value1");
        addProperty(suite, "testsuite1-property2", "testsuite1-value2");
        suite.getTestcase().add(createTestcase2("testsuite1-testcase1"));
        suite.getTestcase().add(createTestcase("testsuite1-testcase2", true));
        suite.setSystemOut("testsuite1-out");
        suite.setSystemErr("testsuite1-err");
        suites.getTestsuite().add(suite);

        Testsuite suite2 = new Testsuite();
        suite2.setName("testsuite2-name");
        suite2.getTestcase().add(createTestcase("testsuite2-testcase1", true));
        suites.getTestsuite().add(suite2);

        return suites;
    }

    private Testcase createTestcase2(String name) {
        Testcase testCase = createTestcase(name, false);
        testCase.setAssertions("100");
        testCase.setClassname("101");
        testCase.setStatus("102");
        testCase.setTime("103");

        Error error = new Error();
        error.setMessage("error-message");
        error.setType("error-type");
        error.setContent("error-line1\nerror-line2");
        testCase.getError().add(error);

        Failure failure = new Failure();
        failure.setMessage("failure-message");
        failure.setType("failure-type");
        failure.setContent("failure-line1\nfailure-line2");
        testCase.getFailure().add(failure);

        testCase.getSystemOut().add("out-line1");
        testCase.getSystemOut().add("out-line2");
        testCase.getSystemErr().add("err-line1");
        testCase.getSystemErr().add("err-line2");
        return testCase;
    }

    public Testcase createTestcase(String name, boolean skipped) {
        Testcase testCase = new Testcase();
        testCase.setName(name);
        if (skipped) {
            testCase.setSkipped("");
        }
        return testCase;
    }

    private void addProperty(Testsuite suite, String name, String value) {
        Property property = new Property();
        property.setName(name);
        property.setValue(value);
        Properties properties = suite.getProperties();
        if (properties == null) {
            properties = new Properties();
            suite.setProperties(properties);
        }
        properties.getProperty().add(property);
    }

    public static String readResource(String name, boolean trimText) throws IOException {
        StringWriter os = new StringWriter();
        final InputStream stream = JUnitXMLReportWriterTest.class.getResourceAsStream(name);
        assertNotNull(name + " not found", stream);
        IOUtils.copy(stream, os);
        return trimText(trimText, os.toString());
    }

    public static String readFile(String file, boolean trimText) throws IOException {
        StringWriter os = new StringWriter();
        IOUtils.copy(new FileInputStream(file), os);
        return trimText(trimText, os.toString());
    }

    private static String trimText(boolean trimText, String content) {
        if (trimText) {
            StringBuilder buffer = new StringBuilder();
            for (String line : content.split(LINE_SEPARATOR)) {
                buffer.append(strip(line)).append(LINE_SEPARATOR);
            }
            content = buffer.toString();
        }
        return content;
    }
}