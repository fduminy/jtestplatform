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

import gnu.testlet.TestHarness;
import gnu.testlet.Testlet;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;

import static org.jtestplatform.server.Utils.contains;
import static org.jtestplatform.server.Utils.makeTestName;
import static org.junit.Assert.*;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TestFrameworkTest {
    @DataPoint
    public static final MauveTestFramework MAUVE_TEST_FRAMEWORK = new MauveTestFramework();
    @DataPoint
    public static final JUnitTestFramework JUNIT_TEST_FRAMEWORK = new JUnitTestFramework();

    private static final Map<TestFramework, TestFrameworkData> TESTS = new HashMap<TestFramework, TestFrameworkData>();

    private static final boolean FAIL = false;
    private static final boolean SUCCEED = true;
    private static void addTest(TestFramework testFramework, Class<?> testClass, boolean succeed) throws Exception {
        addTest(testFramework, testClass, succeed, null);
    }
    private static void addTest(TestFramework testFramework, Class<?> testClass, boolean succeed, String method) throws Exception {
        TestFrameworkData data = TESTS.get(testFramework);
        if (data == null) {
            data = new TestFrameworkData(testFramework);
            TESTS.put(testFramework, data);
        }
        data.addTest(testClass, succeed, method);
    }
    private static boolean mustFail(TestFramework framework, String testName) {
        TestFrameworkData data = TESTS.get(framework);
        return data.mustFail(testName);
    }


    /**
     * @param testClass
     * @return The list of expected tests.
     */
    private static List<String> getExpectedTests(TestFramework testFramework, Class<?> testClass) {
        TestFrameworkData data = TESTS.get(testFramework);
        return data.getExpectedTests(testClass);
    }

    public static List<String> getExpectedTests() {
        List<String> tests = new ArrayList<String>();
        for (TestFramework framework : TESTS.keySet()) {
            TestFrameworkData data = TESTS.get(framework);
            for (Class<?> testClass : data.getTestClasses()) {
                tests.addAll(getExpectedTests(framework, testClass));
            }
        }
        return tests;
    }

    /**
     * @param testFramework
     * @return The collection of test classes.
     */
    private static Collection<Class<?>> getTestClasses(TestFramework testFramework) {
        TestFrameworkData data = TESTS.get(testFramework);
        return data.getTestClasses();
    }

    static {
        try {
            // junit test framework
            addTest(JUNIT_TEST_FRAMEWORK, ParameterizedTestClass.class, FAIL, "aFailingTest");
            addTest(JUNIT_TEST_FRAMEWORK, ParameterizedTestClass.class, SUCCEED, "aTest");
            addTest(JUNIT_TEST_FRAMEWORK, TestClass.class, FAIL, "aFailingTest");
            addTest(JUNIT_TEST_FRAMEWORK, TestClass.class, SUCCEED, "aTest");
            addTest(JUNIT_TEST_FRAMEWORK, JUnit3TestClassTest.class, FAIL, "testThatFails");
            addTest(JUNIT_TEST_FRAMEWORK, JUnit3TestClassTest.class, SUCCEED, "testThatWorks");

            // mauve test framework
            addTest(MAUVE_TEST_FRAMEWORK, MauveTestClass.class, SUCCEED);
            addTest(MAUVE_TEST_FRAMEWORK, MauveFailingTestClass.class, FAIL);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getMessage());
        }
    }

    @Theory
    public void testGetName(TestFramework testFramework) {
        String name = testFramework.getName();
        assertNotNull(name);
        assertFalse("test framework name can't be blank or empty", name.trim().isEmpty());
    }

    @Theory
    public void testGetTests(TestFramework testFramework) {
        Collection<String> tests = testFramework.getTests();
        assertNotNull(tests);
        assertFalse("test list can't be blank or empty", tests.isEmpty());

        for (String test : tests) {
            assertNotNull(test);
            assertFalse("test name can't be blank or empty", test.trim().isEmpty());
        }
    }

    @Theory
    public void testGetTestsForClass(TestFramework testFramework) {
        for (Class<?> testClass : getTestClasses(testFramework)) {
            String[] expectedTests = getExpectedTests(testFramework, testClass).toArray(new String[0]);
            String[] tests = testFramework.getTests(testClass).toArray(new String[0]);
            Arrays.sort(expectedTests);
            Arrays.sort(tests);
            assertArrayEquals("invalid test list for class " + testClass.getName(), expectedTests, tests);
        }
    }

    @Theory()
    public void testGetTestsForWrongClass(TestFramework testFramework) {
        Class<?> aWrongClass = String.class; // must not be a valid test class for the framework
        Set<String> tests = testFramework.getTests(aWrongClass);
        assertNull("list of tests must be null for a wrong class", tests);
    }

    @Theory
    public void testRunTest(TestFramework testFramework) throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (!mustFail(testFramework, aTest)) {
                boolean success = testFramework.runTest(aTest);
                assertTrue("The test '" + aTest + "' must succeed", success);
            }
        }
    }

    @Theory
    public void testRunFailingTest(TestFramework testFramework) throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (mustFail(testFramework, aTest)) {
                boolean success = testFramework.runTest(aTest);
                assertFalse("The test '" + aTest + "' must fail", success);
            }
        }
    }

    @Theory
    @Test(expected=UnknownTestException.class)
    public void testRunUnknownTest(TestFramework testFramework) throws UnknownTestException {
        testFramework.runTest("AnUnknownTest");
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTestClass {

        @Parameters
        public static Collection<Object[]> data() {
                return Arrays.asList(new Object[][]{{1L}, {1L}});
        }

        private long param;

        public ParameterizedTestClass(long param) {
            this.param = param;
        }

        @Test
        public void aTest() {

        }

        @Test
        public void aFailingTest() {
            Assert.fail("a failure");
        }
    }

    public static class TestClass {
        @Test
        public void aTest() {

        }

        @Test
        public void aFailingTest() {
            Assert.fail("a failure");
        }
    }

    public static class JUnit3TestClassTest extends TestCase {
        public JUnit3TestClassTest() {
            super();
        }

        public void testThatWorks() {

        }

        public void testThatFails() {
            Assert.fail("a failure");
        }
    }

    //TODO add TestSuite and "static TestSuite suite()" to JUnit tests
    public static class TestSuiteClass extends TestSuite {
    }

    public static class MauveTestClass implements Testlet {

        @Override
        public void test(TestHarness harness) {
            harness.check(true, true);
        }
    }

    public static class MauveFailingTestClass implements Testlet {

        @Override
        public void test(TestHarness harness) {
            harness.check(true, false); // will fail
        }
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
            String testName = makeTestName(testClass, method);

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
