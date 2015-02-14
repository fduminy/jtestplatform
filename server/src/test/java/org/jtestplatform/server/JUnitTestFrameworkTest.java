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
import junit.framework.TestSuite;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 */
public class JUnitTestFrameworkTest extends TestFrameworkTest<JUnitTestFramework> {
    public JUnitTestFrameworkTest() throws Exception {
        super(new JUnitTestFramework());

        addFailingTest(ParameterizedTestClass.class, "aFailingTest");
        addSucceedingTest(ParameterizedTestClass.class, "aTest");
        addFailingTest(TestClass.class, "aFailingTest");
        addSucceedingTest(TestClass.class, "aTest");
        addFailingTest(JUnit3TestClassTest.class, "testThatFails");
        addSucceedingTest(JUnit3TestClassTest.class, "testThatWorks");
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
}
