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

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.jtestplatform.server.ServerUtils.printStackTrace;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class JUnitTestFrameworkTest extends TestFrameworkTest<JUnitTestFramework> {
    private static final AssertionError FAILURE = new AssertionError("a failure");

    public JUnitTestFrameworkTest() throws Exception {
        super(new JUnitTestFramework());

        addFailingTest(ParameterizedTestClass.class, "aFailingTest");
        addSucceedingTest(ParameterizedTestClass.class, "aPassingTest");
        addTestWithError(ParameterizedTestClass.class, "aTestThrowingAnError");

        addFailingTest(TestClass.class, "aFailingTest");
        addSucceedingTest(TestClass.class, "aPassingTest");
        addTestWithError(TestClass.class, "aTestThrowingAnError");
        addIgnoredTest(TestClass.class, "anIgnoredTest");

        addFailingTest(JUnit3TestClassTest.class, "testThatFails");
        addSucceedingTest(JUnit3TestClassTest.class, "testThatPasses");
        addTestWithError(JUnit3TestClassTest.class, "testThatThrowsAnError");
    }

    protected void addFailingTest(Class<?> testClass, String method) throws Exception {
        String stackTrace = printStackTrace(FAILURE, testClass.getName(), method);
        addFailingTest(testClass, method, FAILURE.getClass().getName(), stackTrace, FAILURE.getMessage());
    }

    public static void addTestsTo(JUnitTestFramework testFramework) throws Exception {
        testFramework.addTestClass(ParameterizedTestClass.class);
        testFramework.addTestClass(TestClass.class);
        testFramework.addTestClass(JUnit3TestClassTest.class);
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTestClass {
        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{{1L}, {1L}});
        }

        private long param;

        public ParameterizedTestClass(long param) {
            this.param = param;
        }

        @Test
        public void aPassingTest() {
        }

        @Test
        public void aFailingTest() {
            throw FAILURE;
        }

        @Test
        public void aTestThrowingAnError() {
            throw ERROR;
        }
    }

    public static class TestClass {
        @Test
        public void aPassingTest() {
        }

        @Test
        @Ignore
        public void anIgnoredTest() {
            throw FAILURE; // should never be executed
        }

        @Test
        public void aFailingTest() {
            throw FAILURE;
        }

        @Test
        public void aTestThrowingAnError() {
            throw ERROR;
        }
    }

    public static class JUnit3TestClassTest extends TestCase {
        public void testThatPasses() {
        }

        public void testThatFails() {
            throw FAILURE;
        }

        public void testThatThrowsAnError() {
            throw ERROR;
        }
    }

/*
    //TODO add TestSuite and "static TestSuite suite()" to JUnit tests
    public static class TestSuiteClass extends TestSuite {
    }
*/
}
