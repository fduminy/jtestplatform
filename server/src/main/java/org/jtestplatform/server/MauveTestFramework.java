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
/**
 *
 */
package org.jtestplatform.server;

import gnu.testlet.ResourceNotFoundException;
import gnu.testlet.TestHarness;
import gnu.testlet.Testlet;
import gnu.testlet.runner.Filter;
import gnu.testlet.runner.Filter.LineProcessor;
import org.jtestplatform.common.message.TestResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

import static org.jtestplatform.server.ServerUtils.printStackTrace;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class MauveTestFramework implements TestFramework {
    private final List<String> testList;

    /**
     * @throws IOException
     *
     */
    public MauveTestFramework() {
        testList = new ArrayList<String>();
    }

    public void addDefaultTests() throws IOException {
        testList.addAll(readCompleteList());
    }

    /**
     * {@inheritDoc}
     * @param testClass
     * @throws Exception
     */
    @Override
    public void addTestClass(Class<?> testClass) throws Exception {
        Set<String> tests = getTests(testClass);
        if (tests == null) {
            throw new Exception("no test method in class " + testClass.getName());
        }

        for (String test : tests) {
            testList.add(testClass.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "mauve";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getTests() {
        return testList;
    }

    /**
     * {@inheritDoc}
     * @throws UnknownTestException
     */
    @Override
    public void runTest(TestResult testResult) throws UnknownTestException {
        if (!getTests().contains(testResult.getTest())) {
            throw new UnknownTestException(testResult.getTest());
        }

        JTSMauve m = new JTSMauve();
        try {
            m.runTest(testResult.getTest());
            if ((m.testPassed != null) && !m.testPassed) {
                testResult.setFailure(AssertionError.class.getName(), m.debugLog.toString(), null, false);
            }
        } catch (Exception e) {
            testResult.setFailure(e.getClass().getName(), printStackTrace(e, testResult), e.getMessage(), true);
        }
    }

    private class JTSMauve extends TestHarness {
        private final StringBuilder debugLog = new StringBuilder();
        private Boolean testPassed;

        public void runTest(String testName) throws Exception {
            // save the default locale, some tests change the default
            Locale savedLocale = Locale.getDefault();

            try {
                Class<?> testClass = Class.forName(testName);
                Testlet testlet = (Testlet) testClass.newInstance();
                testlet.test(this);
            } finally {
                // restore the default locale
                Locale.setDefault(savedLocale);
            }
        }

        @Override
        public void check(boolean testPassed) {
            this.testPassed = testPassed;
        }

        @Override
        public Reader getResourceReader(String s) throws ResourceNotFoundException {
            return null;
        }

        @Override
        public InputStream getResourceStream(String s) throws ResourceNotFoundException {
            return null;
        }

        @Override
        public File getResourceFile(String s) throws ResourceNotFoundException {
            return null;
        }

        @Override
        public void checkPoint(String s) {
        }

        @Override
        public void verbose(String s) {
        }

        @Override
        public void debug(String s) {
            debugLog.append(s);
        }

        @Override
        public void debug(String s, boolean b) {
            debugLog.append(s + ' ' + b);
        }

        @Override
        public void debug(Throwable throwable) {
            debugLog.append(throwable);
        }

        @Override
        public void debug(Object[] objects, String s) {
            debugLog.append(objects + " " + s);
        }
    }

    /**
     * Read the mauve tests list but don't take lines containing '['
     * and also apply additional filters specified in configuration.
     * @return
     * @throws IOException
     */
    private List<String> readCompleteList() throws IOException {
        final List<String> list = new ArrayList<String>();
        Filter.readTestList(new LineProcessor() {

            @Override
            public void processLine(StringBuffer buf) {
                String line = buf.toString();
                if (!line.contains("[")) {
                    list.add(line);
                }
            }

        });
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getTests(Class<?> testClass) {
        Set<String> tests = null;

        if (Testlet.class.isAssignableFrom(testClass)) {
            tests = Collections.singleton(testClass.getName());
        }

        return tests;
    }
}
