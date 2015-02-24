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

public class TestName {
    private static final char SEPARATOR = '#';

    private final String testClass;
    private final String methodName;

    public static TestName parse(String name) {
        int index = name.indexOf(SEPARATOR);
        String testClass = (index < 0) ? name : name.substring(0, index);
        String methodName = (index < 0) ? null : name.substring(index + 1);
        return new TestName(testClass, methodName);
    }

    public static String toString(Class<?> testClass, String methodName) {
        return toString(testClass.getName(), methodName);
    }

    private TestName(String testClass, String methodName) {
        this.testClass = testClass;
        this.methodName = methodName;
    }

    private static String toString(String testClass, String methodName) {
        return testClass + ((methodName == null) ? "" : (SEPARATOR + methodName));
    }

    public String getTestClass() {
        return testClass;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return toString(testClass, methodName);
    }
}