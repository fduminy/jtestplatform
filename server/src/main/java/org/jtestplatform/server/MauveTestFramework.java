/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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

import gnu.testlet.Testlet;
import gnu.testlet.runner.CheckResult;
import gnu.testlet.runner.Filter;
import gnu.testlet.runner.Mauve;
import gnu.testlet.runner.RunResult;
import gnu.testlet.runner.Filter.LineProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class MauveTestFramework implements TestFramework {
    private static final Logger LOGGER = Logger.getLogger(MauveTestFramework.class);

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
    public boolean runTest(String test) throws UnknownTestException {
        if (!getTests().contains(test)) {
            throw new UnknownTestException(test);
        }

        JTSMauve m = new JTSMauve();
        RunResult result = m.runTest(test);

        /*
        int cc = result.getCheckCount();
        int cc2 = result.getCheckCount(true);
        int cc3 = result.getCheckCount(false);
        PackageResult pr = (PackageResult) result.getPackageIterator().next();
        ClassResult cr = (ClassResult) pr.getClassIterator().next();
        TestResult tr = (TestResult) cr.getTestIterator().next();
        */

        return result.getCheckCount(false) == 0;
    }

    private class JTSMauve extends Mauve {
        public RunResult runTest(String testName) {
            // save the default locale, some tests change the default
            Locale savedLocale = Locale.getDefault();

            result = new RunResult("Mauve Test Run");
            addSystemProperties(result);
            currentCheck = new CheckResult(0, false);

            executeLine("", testName);

            // restore the default locale
            Locale.setDefault(savedLocale);

            return getResult();
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
