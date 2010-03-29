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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.FrameworkMethod;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class JUnitTestFramework implements TestFramework {
    private static final Logger LOGGER = Logger.getLogger(JUnitTestFramework.class);
    
    private final Map<String, TestData> tests = new HashMap<String, TestData>();

    public void addTestClass(Class<?> testClass) throws Exception {
        for (String method : getTestMethods(testClass)) {
            String name = testClass.getName() + '#' + method;
            tests.put(name, new TestData(testClass, method));
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

                boolean match = description.getClassName().equals(wantedClass) &&
                    extractRealMethodName(description).equals(wantedMethod);

                for (Description d : description.getChildren()) {
                    match |= d.getClassName().equals(wantedClass) &&
                        extractRealMethodName(d).equals(wantedMethod);
                    if (match) {
                        break;
                    }
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(JUnitTestFramework.this.toString(description) + " match=" + match);
                }
                return match;
            }

            @Override
            public String describe() {
                return t.getTestClass().getName() + '#' + t.getTestMethod();
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

    // TODO put that code in JNode to collect its junit tests in a file
/*
    private Map<String, TestData> findTests() throws Exception {
        Map<String, TestData> result = new HashMap<String, TestData>();

        String path = "/home/fabien/data/Projets/jtestplatform/server/target/test-classes";

        for (Class<?> cls : getClasses(new File(path))) {
            addTestClass(cls);
        }

        return result;
    }

    private Set<Class<?>> getClasses(File directory) {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        getClasses(directory, directory, classes);
        return classes;
    }

    private void getClasses(File baseDirectory, File directory, Set<Class<?>> classes) {
        final String extension = ".class";
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.getName().endsWith(extension);
            }
        });
        for (File f : files) {
            if (f.isDirectory()) {
                getClasses(baseDirectory, f, classes);
            } else {
                String path = f.getAbsolutePath();
                String baseDir = baseDirectory.getAbsolutePath();
                path = path.substring(baseDir.length() + 1).replace(File.separatorChar, '.');
                String className = path.substring(0, path.length() - extension.length());
                try {
                    Class<?> cls = Class.forName(className);
                    if (cls.getConstructors().length == 1) {
                        classes.add(cls);
                    }
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
*/

    @SuppressWarnings("unchecked")
    private Set<String> getTestMethods(Class<?> testClass) throws Exception {
        final Set<String> methods = new HashSet<String>();
        if (junit.framework.TestCase.class.isAssignableFrom(testClass)) {
            for (Method method : testClass.getMethods()) {
                if (LOGGER.isDebugEnabled() && method.getName().startsWith("test")) {
                    LOGGER.debug("class=" + testClass.getName() + " method=" + method.getName());
                }

                if (method.getName().startsWith("test") && (method.getParameterTypes().length == 0)) {
                    methods.add(method.getName());
                }
            }
        } else {
            org.junit.runners.model.TestClass tc = new org.junit.runners.model.TestClass(testClass);
            for (FrameworkMethod method : tc.getAnnotatedMethods(Test.class)) {
                methods.add(method.getName());
            }
            for (FrameworkMethod method : tc.getAnnotatedMethods(Theory.class)) {
                methods.add(method.getName());
            }
        }

        if (methods.isEmpty()) {
            throw new Exception("no test method in class " + testClass.getName());
        }

        return methods;
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
        return "Description[className=" + description.getClassName() +
        " methodName=" + description.getMethodName() +
        " displayName=" + description.getDisplayName() + ']';
    }
}
