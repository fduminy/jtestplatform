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
package org.jtestplatform.server.utils;

import org.jtestplatform.server.TestFramework;
import org.jtestplatform.server.TestFrameworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JarResources;

import java.io.*;
import java.util.*;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestList {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestList.class);

    private static final String CLASS_EXTENSION = ".class";
    private static final String JAR_EXTENSION = ".jar";
    private static final String SEPARATOR = ":";

    private final Set<String> tests = new TreeSet<String>();
    private final List<TestSource> testSources = new ArrayList<TestSource>();

    public void addTestSource(File directory, String... includeClassNames) {
        testSources.add(new TestSource(directory, includeClassNames));
    }

    public void collectTests() throws FileNotFoundException {
        collectTests((String[]) null);
    }

    public void collectTests(String... testFrameworkNames) throws FileNotFoundException {
        // find test frameworks to use
        final TestFrameworkManager testManager = TestFrameworkManager.getInstance();
        Collection<TestFramework> frameworks;
        if ((testFrameworkNames == null) || (testFrameworkNames.length == 0)) {
            frameworks = new ArrayList<TestFramework>(testManager.getTestFrameworks().size());
            for (String name : testManager.getTestFrameworks()) {
                frameworks.add(testManager.getTestFramework(name));
            }
        } else {
            frameworks = new ArrayList<TestFramework>(testFrameworkNames.length);
            for (String name : testFrameworkNames) {
                frameworks.add(testManager.getTestFramework(name));
            }
        }

        // init class loader
        JarClassLoader jcl = new JarClassLoader();
        jcl.getSystemLoader().setOrder(1);
        jcl.getLocalLoader().setOrder(2);
        jcl.getParentLoader().setOrder(3);
        jcl.getThreadLoader().setOrder(4);
        jcl.getCurrentLoader().setOrder(5);

        for (TestSource testSource : testSources) {
            jcl.add(testSource.getFile().getAbsolutePath());
        }

        // find all test classes in the classpath
        for (TestSource testSource : testSources) {
            collectTests(jcl, testSource.getFile(), testSource, frameworks);
        }
    }

    public void writeTestList(OutputStream outputStream) {
        PrintStream ps = new PrintStream(outputStream);
        for (String test : tests) {
            ps.println(test);
        }
        ps.flush();
    }

    public static void readTestList(InputStream inputStream) {
        TestFrameworkManager manager = TestFrameworkManager.getInstance();
        Scanner scanner = new Scanner(inputStream).useDelimiter(SEPARATOR);
        while (scanner.hasNextLine()) {
            String framework = scanner.next();
            TestFramework testFramework = manager.getTestFramework(framework);

            String test = scanner.next();
            try {
                Class<?> clazz = Class.forName(test);
                testFramework.addTestClass(clazz);
            } catch (Throwable t) {
                LOGGER.error("Error while adding test class " + test, t);
            }

            scanner.nextLine();
        }
    }

    private void collectTests(JarClassLoader jcl, File baseDirectory, TestSource testSource, Collection<TestFramework> frameworks) throws FileNotFoundException {
        final File directory = testSource.getFile();
        LOGGER.debug("collecting test classes from {}", directory.getAbsolutePath());

        File[] files;
        if (directory.isFile() && directory.getName().endsWith(JAR_EXTENSION)) {
            files = new File[]{directory};
        } else {
            files = directory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isDirectory()
                        || pathname.getName().endsWith(CLASS_EXTENSION)
                        || pathname.getName().endsWith(JAR_EXTENSION);
                }
            });
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectTests(jcl, baseDirectory, testSource, frameworks);
            } else {
                List<String> paths = new ArrayList<String>();
                if (f.getName().endsWith(JAR_EXTENSION)) {
                    JarResources jarResources = new JarResources();
                    jarResources.loadJar(new FileInputStream(f));
                    for (String path : jarResources.getResources().keySet()) {
                        if (path.endsWith(CLASS_EXTENSION)) {
                            paths.add(path);
                        }
                    }
                } else {
                    String path = f.getAbsolutePath();
                    String baseDir = baseDirectory.getAbsolutePath();
                    path = path.substring(baseDir.length() + 1);
                }
                collectTests(jcl, paths, frameworks, testSource);
            }
        }
    }

    private void collectTests(JarClassLoader jcl, List<String> paths,
            Collection<TestFramework> frameworks, TestSource testSource) {
        for (String path : paths) {
            path = path.replace(File.separatorChar, '.');
            String className = path.substring(0, path.length() - CLASS_EXTENSION.length());
            if (accept(testSource, className)) {
                try {
                    Class<?> cls = Class.forName(className, true, jcl);
                    for (TestFramework framework : frameworks) {
                        Set<String> testList = framework.getTests(cls);
                        if ((testList != null) && !testList.isEmpty()) {
                            for (String test : testList) {
                                tests.add(framework.getName() + SEPARATOR + test);
                            }
                            break;
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.error("Error collecting tests for class " + className, t);
                }
            }
        }
    }

    private boolean accept(TestSource testSource, String className) {
        boolean accept = false;
        for (String include : testSource.getIncludeClassNames()) {
            if (className.startsWith(include)) {
                accept = true;
                break;
            }
        }
        return accept;
    }

    private static class TestSource {
        private final File file;
        private final String[] includeClassNames;
        public TestSource(File file, String[] includeClassNames) {
            this.file = file;
            this.includeClassNames = includeClassNames;
        }
        public File getFile() {
            return file;
        }
        public String[] getIncludeClassNames() {
            return includeClassNames;
        }
    }
}
