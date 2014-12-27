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

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class JUnitTestFramework implements TestFramework {
    private static final Logger LOGGER = LoggerFactory.getLogger(JUnitTestFramework.class);

    private final Map<String, TestData> tests = new HashMap<String, TestData>();

    /**
     * {@inheritDoc}
     * @param testClass
     * @throws Exception
     */
    @Override
    public void addTestClass(Class<?> testClass) throws Exception {
        Set<String> methods = getMethods(testClass);
        if (methods == null) {
            throw new Exception("no test method in class " + testClass.getName());
        }

        for (String method : methods) {
            tests.put(generateTestName(testClass, method), new TestData(testClass, method));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "junit";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getTests() {
        return tests.keySet();
    }

    /**
     * {@inheritDoc}
     * @throws UnknownTestException
     */
    @Override
    public boolean runTest(String test) throws UnknownTestException {
        if (!getTests().contains(test)) { // will also find the tests
            throw new UnknownTestException(test);
        }

        final TestData t = tests.get(test);
        JUnitCore core = new JUnitCore();

        Request request = Request.aClass(t.getTestClass()).filterWith(new Filter() {
            @Override
            public boolean shouldRun(Description description) {
                String wantedClass = t.getTestClass().getName();
                String wantedMethod = t.getTestMethod();

                boolean match = description.getClassName().equals(wantedClass)
                    && extractRealMethodName(description).equals(wantedMethod);

                for (Description d : description.getChildren()) {
                    match |= d.getClassName().equals(wantedClass)
                        && extractRealMethodName(d).equals(wantedMethod);
                    if (match) {
                        break;
                    }
                }

                LOGGER.debug("{} match={}", LOGGER.isDebugEnabled() ? JUnitTestFramework.this.toString(description) : null, match);
                return match;
            }

            @Override
            public String describe() {
                return generateTestName(t.getTestClass(), t.getTestMethod());
            }

            private String extractRealMethodName(Description d) {
                String methodName = d.getMethodName();
                if (methodName != null) {
                    int idx = methodName.lastIndexOf('[');
                    if (idx >= 0) {
                        methodName = methodName.substring(0, idx);
                    }
                }
                return methodName;
            }
        });

        Result result = core.run(request);
        return result.getFailureCount() == 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getTests(Class<?> testClass) {
        Set<String> tests = null;

        Set<String> methods = getMethods(testClass);
        if (methods != null) {
            tests = new HashSet<String>(methods.size());
            for (String method : methods) {
                tests.add(generateTestName(testClass, method));
            }
        }

        return tests;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getMethods(Class<?> testClass) {
        final Set<String> methods = new HashSet<String>();

        if (junit.framework.TestCase.class.isAssignableFrom(testClass)) {
            for (Method method : testClass.getMethods()) {
                if (method.getName().startsWith("test")) {
                    LOGGER.debug("class={} method={}", testClass.getName(), method.getName());
                }

                if (method.getName().startsWith("test") && (method.getParameterTypes().length == 0)) {
                    methods.add(method.getName());
                }
            }
        } else if (testClass.getConstructors().length == 1) {
            org.junit.runners.model.TestClass tc = new org.junit.runners.model.TestClass(testClass);
            for (FrameworkMethod method : tc.getAnnotatedMethods(Test.class)) {
                methods.add(method.getName());
            }
            for (FrameworkMethod method : tc.getAnnotatedMethods(Theory.class)) {
                methods.add(method.getName());
            }
        }

        return methods.isEmpty() ? null : methods;
    }

    private String generateTestName(Class<?> testClass, String method) {
        return testClass.getName() + '#' + method;
    }

    private static class TestData {
        private final Class<?> testClass;
        private final String testMethod;

        public TestData(Class<?> testClass, String method) {
            this.testClass = testClass;
            this.testMethod = method;
        }

        public Class<?> getTestClass() {
            return testClass;
        }

        public String getTestMethod() {
            return testMethod;
        }
    }

    private String toString(Description description) {
        return "Description[className=" + description.getClassName()
        + " methodName=" + description.getMethodName()
        + " displayName=" + description.getDisplayName() + ']';
    }
}
