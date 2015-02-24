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
package org.jtestplatform.server;

import org.jtestplatform.common.TestName;
import org.junit.Test;

import java.util.*;

import static org.jtestplatform.server.Utils.contains;
import static org.junit.Assert.*;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
abstract public class TestFrameworkTest<T extends TestFramework> {
    private final TestFrameworkData data;
    private final T testFramework;

    public TestFrameworkTest(T testFramework) {
        this.testFramework = testFramework;
        data = new TestFrameworkData(testFramework);
    }

    @Test
    public void testGetName() {
        String name = testFramework.getName();
        assertNotNull(name);
        assertFalse("test framework name can't be blank or empty", name.trim().isEmpty());
    }

    @Test
    public void testGetTests() {
        Collection<String> tests = testFramework.getTests();
        assertNotNull(tests);
        assertFalse("test list can't be blank or empty", tests.isEmpty());

        for (String test : tests) {
            assertNotNull(test);
            assertFalse("test name can't be blank or empty", test.trim().isEmpty());
        }
    }

    @Test
    public void testGetTestsForClass() {
        for (Class<?> testClass : data.getTestClasses()) {
            String[] expectedTests = data.getExpectedTests(testClass).toArray(new String[0]);
            String[] tests = testFramework.getTests(testClass).toArray(new String[0]);
            Arrays.sort(expectedTests);
            Arrays.sort(tests);
            assertArrayEquals("invalid test list for class " + testClass.getName(), expectedTests, tests);
        }
    }

    @Test
    public void testGetTestsForWrongClass() {
        Class<?> aWrongClass = String.class; // must not be a valid test class for the framework
        Set<String> tests = testFramework.getTests(aWrongClass);
        assertNull("list of tests must be null for a wrong class", tests);
    }

    @Test
    public void testRunTest() throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (!data.mustFail(aTest)) {
                boolean success = testFramework.runTest(aTest);
                assertTrue("The test '" + aTest + "' must succeed", success);
            }
        }
    }

    @Test
    public void testRunFailingTest() throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (data.mustFail(aTest)) {
                boolean success = testFramework.runTest(aTest);
                assertFalse("The test '" + aTest + "' must fail", success);
            }
        }
    }

    @Test(expected=UnknownTestException.class)
    public void testRunUnknownTest() throws UnknownTestException {
        testFramework.runTest("AnUnknownTest");
    }

    protected final void addSucceedingTest(Class<?> testClass, String method) throws Exception {
        addTest(testClass, true, method);
    }

    protected final void addFailingTest(Class<?> testClass, String method) throws Exception {
        addTest(testClass, false, method);
    }

    private void addTest(Class<?> testClass, boolean succeed, String method) throws Exception {
        data.addTest(testClass, succeed, method);
    }

    private static class TestFrameworkData {
        private final TestFramework framework;
        private final List<String> failingTests = new ArrayList<String>();
        private final List<String> succeedingTests = new ArrayList<String>();
        private final Map<Class<?>, List<String>> tests = new HashMap<Class<?>, List<String>>();

        public TestFrameworkData(TestFramework framework) {
            this.framework = framework;
        }

        /**
         * @param testClass
         * @return The list of expected tests.
         */
        public List<String> getExpectedTests(Class<?> testClass) {
            return tests.get(testClass);
        }

        /**
         * @return The collections of test classes.
         */
        public Collection<Class<?>> getTestClasses() {
            return tests.keySet();
        }

        /**
         * @param testName
         * @return false if the given test must fail, true otherwise.
         */
        public boolean mustFail(String testName) {
            boolean result;

            if (failingTests.contains(testName)) {
                result = true;
            } else if (succeedingTests.contains(testName)) {
                result = false;
            } else {
                throw new AssertionError("unexpected test : " + testName);
            }

            return result;
        }

        public void addTest(Class<?> testClass, boolean succeed, String method) throws Exception {
            String testName = TestName.toString(testClass, method);

            framework.addTestClass(testClass);

            List<String> testsForClass = tests.get(testClass);
            if (testsForClass == null) {
                testsForClass = new ArrayList<String>();
                tests.put(testClass, testsForClass);
            }
            testsForClass.add(testName);

            if (succeed) {
                succeedingTests.add(testName);
            } else {
                failingTests.add(testName);
            }

            assertThat(framework.getTests(), contains(testClass, method));
        }
    }
}
