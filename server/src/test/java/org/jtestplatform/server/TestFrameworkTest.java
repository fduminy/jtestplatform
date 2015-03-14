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
import org.jtestplatform.common.message.TestResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.server.ServerUtils.printStackTrace;
import static org.jtestplatform.server.TestFrameworkTest.TestResultType.*;
import static org.jtestplatform.server.Utils.contains;
import static org.junit.Assert.*;


/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
abstract public class TestFrameworkTest<T extends TestFramework> {
    static final NullPointerException ERROR = new NullPointerException("an unexpected error");

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
            TestFailure failure = data.getTestFailure(aTest);
            if (!data.isIgnored(aTest) && (failure == null)) {
                // prepare
                TestResult testResult = createTestResult(aTest);

                // test
                testFramework.runTest(testResult);

                // verify
                assertThat(testResult.getFailureType()).as("failureType").isNull();
                assertThat(testResult.getFailureContent()).as("failureContent").isNull();
                assertThat(testResult.getFailureMessage()).as("failureMessage").isNull();
                assertThat(testResult.isSuccess()).as("success").isTrue();
                assertThat(testResult.isError()).as("error").isFalse();
                assertThat(testResult.isIgnored()).as("ignored").isFalse();
            }
        }
    }

    @Test
    public void testRunFailingTest() throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            TestFailure failure = data.getTestFailure(aTest);
            if ((failure != null) && !failure.error) {
                // prepare
                TestResult testResult = createTestResult(aTest);

                // test
                testFramework.runTest(testResult);

                // verify
                assertThat(testResult.getFailureType()).as("failureType").isEqualTo(failure.failureType);
                assertThat(testResult.getFailureContent()).as("failureContent").isEqualTo(failure.failureContent);
                assertThat(testResult.getFailureMessage()).as("failureMessage").isEqualTo(failure.failureMessage);
                assertThat(testResult.isSuccess()).as("success").isFalse();
                assertThat(testResult.isError()).as("error").isFalse();
                assertThat(testResult.isIgnored()).as("ignored").isFalse();
            }
        }
    }

    @Test
    public void testRunTestWithAnError() throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            TestFailure failure = data.getTestFailure(aTest);
            if ((failure != null) && failure.error) {
                // prepare
                TestResult testResult = createTestResult(aTest);

                // test
                testFramework.runTest(testResult);

                // verify
                assertThat(testResult.getFailureType()).as("failureType").isEqualTo(failure.failureType);
                assertThat(testResult.getFailureContent()).as("failureContent").isEqualTo(failure.failureContent);
                assertThat(testResult.getFailureMessage()).as("failureMessage").isEqualTo(failure.failureMessage);
                assertThat(testResult.isSuccess()).as("success").isFalse();
                assertThat(testResult.isError()).as("error").isTrue();
                assertThat(testResult.isIgnored()).as("ignored").isFalse();
            }
        }
    }

    @Test
    public void testRunIgnoredTest() throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (data.isIgnored(aTest)) {
                // prepare
                TestResult testResult = createTestResult(aTest);

                // test
                testFramework.runTest(testResult);

                // verify
                assertThat(testResult.getFailureType()).as("failureType").isNull();
                assertThat(testResult.getFailureContent()).as("failureContent").isNull();
                assertThat(testResult.getFailureMessage()).as("failureMessage").isNull();
                assertThat(testResult.isSuccess()).as("success").isFalse();
                assertThat(testResult.isError()).as("error").isFalse();
                assertThat(testResult.isIgnored()).as("ignored").isTrue();
            }
        }
    }

    @Test(expected=UnknownTestException.class)
    public void testRunUnknownTest() throws UnknownTestException {
        testFramework.runTest(createTestResult("AnUnknownTest"));
    }

    private TestResult createTestResult(String test) {
        return new TestResult(testFramework.getClass().getSimpleName(), test);
    }

    protected final void addSucceedingTest(Class<?> testClass, String method) throws Exception {
        addTest(testClass, SUCCESS, method, null, null, null);
    }

    protected final void addIgnoredTest(Class<?> testClass, String method) throws Exception {
        addTest(testClass, IGNORED, method, null, null, null);
    }

    protected final void addFailingTest(Class<?> testClass, String method,
                                        String failureType, String failureContent, String failureMessage) throws Exception {
        addTest(testClass, FAILURE, method, failureType, failureContent, failureMessage);
    }

    protected void addTestWithError(Class<?> testClass, String method) throws Exception {
        String stackTrace = printStackTrace(ERROR, testClass.getName(), method);
        addTest(testClass, TestResultType.ERROR, method, ERROR.getClass().getName(), stackTrace, ERROR.getMessage());
    }

    private void addTest(Class<?> testClass, TestResultType resultType, String method,
                         String failureType, String failureContent, String failureMessage) throws Exception {
        data.addTest(testClass, resultType, method, failureType, failureContent, failureMessage);
    }

    private static class TestFrameworkData {
        private final TestFramework framework;
        private final Map<String, TestFailure> testFailures = new HashMap<String, TestFailure>();
        private final Set<String> ignoredTests = new HashSet<String>();
        private final Map<Class<?>, List<String>> tests = new HashMap<Class<?>, List<String>>();

        public TestFrameworkData(TestFramework framework) {
            this.framework = framework;
        }

        public List<String> getExpectedTests(Class<?> testClass) {
            return tests.get(testClass);
        }

        public Collection<Class<?>> getTestClasses() {
            return tests.keySet();
        }

        public boolean isIgnored(String testName) {
            return ignoredTests.contains(testName);
        }

        public TestFailure getTestFailure(String testName) {
            if (!isIgnored(testName) && !testFailures.containsKey(testName)) {
                throw new AssertionError("unexpected test : " + testName);
            }

            return testFailures.get(testName);
        }

        public void addTest(Class<?> testClass, TestResultType resultType, String method,
                            String failureType, String failureContent, String failureMessage) throws Exception {
            String testName = TestName.toString(testClass, method);

            framework.addTestClass(testClass);

            List<String> testsForClass = tests.get(testClass);
            if (testsForClass == null) {
                testsForClass = new ArrayList<String>();
                tests.put(testClass, testsForClass);
            }
            testsForClass.add(testName);

            switch (resultType) {
                case IGNORED:
                    ignoredTests.add(testName);
                    break;
                case SUCCESS:
                    testFailures.put(testName, null);
                    break;
                default:
                    boolean error = resultType == TestResultType.ERROR;
                    testFailures.put(testName, new TestFailure(error, failureType, failureContent, failureMessage));
            }

            Assert.assertThat(framework.getTests(), contains(testClass, method));
        }
    }

    static enum TestResultType {
        SUCCESS,
        FAILURE,
        ERROR,
        IGNORED;
    }

    private static class TestFailure {
        private final boolean error;
        private final String failureType;
        private final String failureContent;
        private final String failureMessage;

        private TestFailure(boolean error, String failureType, String failureContent, String failureMessage) {
            this.error = error;
            this.failureType = failureType;
            this.failureContent = failureContent;
            this.failureMessage = failureMessage;
        }
    }
}
