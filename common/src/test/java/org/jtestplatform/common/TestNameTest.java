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
package org.jtestplatform.common;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class TestNameTest {
    public static enum TestNameData {
        WITH_METHOD(TestClass.class.getName() + "#method", TestClass.class.getName(), "method"),
        WITHOUT_METHOD(TestClass.class.getName(), TestClass.class.getName(), null);

        private final String className;
        private final String methodName;
        private final String fullName;

        TestNameData(String fullName, String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.fullName = fullName;
        }
    }

    @Theory
    public void testParse(TestNameData data) throws Exception {
        TestName testName = TestName.parse(data.fullName);

        assertThat(testName.toString()).isEqualTo(data.fullName);
        assertThat(testName.getTestClass()).isEqualTo(data.className);
        assertThat(testName.getMethodName()).isEqualTo(data.methodName);
    }

    @Theory
    public void testCreate(TestNameData data) throws Exception {
        TestName testName = TestName.create(data.className, data.methodName);

        assertThat(testName.toString()).isEqualTo(data.fullName);
        assertThat(testName.getTestClass()).isEqualTo(data.className);
        assertThat(testName.getMethodName()).isEqualTo(data.methodName);
    }

    @Theory
    public void testToString(TestNameData data) throws Exception {
        String fullName = TestName.toString(Class.forName(data.className), data.methodName);

        assertThat(fullName).isEqualTo(data.fullName);
    }

    public static class TestClass {
    }
}