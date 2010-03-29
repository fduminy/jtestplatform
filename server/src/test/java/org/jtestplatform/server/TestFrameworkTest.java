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

import static org.junit.Assert.*;
import static org.jtestplatform.server.Utils.*;
import gnu.testlet.TestHarness;
import gnu.testlet.Testlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


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

    private static final Map<TestFramework, List<String>> FAILING_TESTS =
        new HashMap<TestFramework, List<String>>();

    static {
        Utils.initLog4j();
        
        try {
            // junit test framework
            List<String> failingTests = new ArrayList<String>();

            Class<?> testClass = ParameterizedTestClass.class;
            JUNIT_TEST_FRAMEWORK.addTestClass(testClass);
            failingTests.add(makeTestName(testClass, "aFailingTest"));
            assertThat(JUNIT_TEST_FRAMEWORK.getTests(), contains(testClass, "aFailingTest"));
            assertThat(JUNIT_TEST_FRAMEWORK.getTests(), contains(testClass, "aTest"));

            testClass = TestClass.class;
            JUNIT_TEST_FRAMEWORK.addTestClass(testClass);
            failingTests.add(makeTestName(testClass, "aFailingTest"));
            assertThat(JUNIT_TEST_FRAMEWORK.getTests(), contains(testClass, "aFailingTest"));
            assertThat(JUNIT_TEST_FRAMEWORK.getTests(), contains(testClass, "aTest"));

            testClass = JUnit3TestClassTest.class;
            JUNIT_TEST_FRAMEWORK.addTestClass(testClass);
            failingTests.add(JUnit3TestClassTest.class.getName() + "#testThatFails");
            assertThat(JUNIT_TEST_FRAMEWORK.getTests(), contains(testClass, "testThatFails"));
            assertThat(JUNIT_TEST_FRAMEWORK.getTests(), contains(testClass, "testThatWorks"));

            FAILING_TESTS.put(JUNIT_TEST_FRAMEWORK, failingTests);

            // mauve test framework
            failingTests = new ArrayList<String>();

            MAUVE_TEST_FRAMEWORK.replaceDefaultTestList(MauveTestClass.class,
                    MauveFailingTestClass.class);
            failingTests.add(MauveFailingTestClass.class.getName());
            FAILING_TESTS.put(MAUVE_TEST_FRAMEWORK, failingTests);
            assertThat(MAUVE_TEST_FRAMEWORK.getTests(), contains(MauveTestClass.class));
            assertThat(MAUVE_TEST_FRAMEWORK.getTests(), contains(MauveFailingTestClass.class));
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
    public void testRunTest(TestFramework testFramework) throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (!FAILING_TESTS.get(testFramework).contains(aTest)) {
                boolean success = testFramework.runTest(aTest);
                assertTrue("The test '" + aTest + "' must succeed", success);
            }
        }
    }

    @Theory
    public void testRunFailingTest(TestFramework testFramework) throws UnknownTestException {
        for (String aTest : testFramework.getTests()) {
            if (FAILING_TESTS.get(testFramework).contains(aTest)) {
                boolean success = testFramework.runTest(aTest);
                assertFalse("The test '" + aTest + "' must fail", success);
            }
        }
    }

    @Theory
    @Test(expected=UnknownTestException.class)
    public void testRunUnknownTest(TestFramework testFramework) throws UnknownTestException {
        // should not be a valid test name
        String aTest = Long.toString(System.currentTimeMillis());

        testFramework.runTest(aTest);
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
}
