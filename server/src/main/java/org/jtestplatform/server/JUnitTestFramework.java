/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
/**
 *
 */
package org.jtestplatform.server;

import org.jtestplatform.common.TestName;
import org.jtestplatform.common.message.TestResult;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.FrameworkMethod;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

import static org.jtestplatform.server.ServerUtils.printStackTrace;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class JUnitTestFramework implements TestFramework {
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
            tests.put(TestName.toString(testClass, method), new TestData(testClass, method));
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
    public void runTest(TestResult testResult) throws UnknownTestException {
        if (!getTests().contains(testResult.getTest())) { // will also find the tests
            throw new UnknownTestException(testResult.getTest());
        }

        final TestData t = tests.get(testResult.getTest());
        final JUnitCore core = new JUnitCore();

        final Request request = Request.aClass(t.getTestClass()).filterWith(new Filter() {
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

                return match;
            }

            @Override
            public String describe() {
                return TestName.toString(t.getTestClass(), t.getTestMethod());
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

        ForwardingSystemOutputStreams streams = new ForwardingSystemOutputStreams();
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        try {
            Result result = streams.forwardOutputStreams(new Callable<Result>() {
                @Override
                public Result call() {
                    return core.run(request);
                }
            }, out, err);

            if (result.getIgnoreCount() > 0) {
                testResult.setIgnored();
            } else if (result.getFailureCount() != 0) {
                Throwable failure = result.getFailures().get(0).getException();
                boolean error = !(failure instanceof AssertionError);
                testResult.setFailure(failure.getClass().getName(), printStackTrace(failure, testResult),
                                      failure.getMessage(), error);
                if (out.length() > 0) {
                    testResult.setSystemOut(out.toString());
                }
                if (err.length() > 0) {
                    testResult.setSystemErr(err.toString());
                }
            }
        } catch (Exception e) {
            testResult.setFailure(e.getClass().getName(), printStackTrace(e, testResult), e.getMessage(), true);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getTests(Class<?> testClass) {
        Set<String> tests = null;

        Set<String> methods = getMethods(testClass);
        if (methods != null) {
            tests = new HashSet<String>(methods.size());
            for (String method : methods) {
                tests.add(TestName.toString(testClass, method));
            }
        }

        return tests;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getMethods(Class<?> testClass) {
        final Set<String> methods = new HashSet<String>();

        if (junit.framework.TestCase.class.isAssignableFrom(testClass)) {
            for (Method method : testClass.getMethods()) {
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
}
