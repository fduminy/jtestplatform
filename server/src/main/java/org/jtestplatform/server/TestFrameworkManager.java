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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestFrameworkManager {
    private static final TestFrameworkManager INSTANCE = new TestFrameworkManager();

    public static TestFrameworkManager getInstance() {
        return INSTANCE; 
    }

    private final Map<String, TestFramework> testFrameworks;

    private TestFrameworkManager() {
        testFrameworks = new HashMap<String, TestFramework>();
        addTestFramework(new JUnitTestFramework());
        addTestFramework(new MauveTestFramework());
    }

    public void addTestFramework(TestFramework framework) {
        testFrameworks.put(framework.getName(), framework);
    }

    /**
     * @param framework
     * @return
     */
    public TestFramework getTestFramework(String framework) {
        return testFrameworks.get(framework);
    }

    /**
     * @param framework
     * @return
     */
    public Set<String> getTestFrameworks() {
        return testFrameworks.keySet();
    }

    public Set<String> collectTests(File directory) {
        return collectTests(directory, (String[]) null);
    }

    public Set<String> collectTests(File directory, String... testFrameworkNames) {
        final Collection<TestFramework> frameworks;
        if ((testFrameworkNames == null) || (testFrameworkNames.length == 0)) {
            frameworks = testFrameworks.values();
        } else {
            List<String> names = Arrays.asList(testFrameworkNames);
            frameworks = new ArrayList<TestFramework>();
            for (TestFramework f : testFrameworks.values()) {
                if (names.contains(f.getName())) {
                    frameworks.add(f);
                }
            }
        }

        Set<String> tests = new HashSet<String>();
        collectTests(directory, directory, tests, frameworks);
        return tests;
    }

    private void collectTests(File baseDirectory, File directory, Set<String> tests, Collection<TestFramework> frameworks) {
        final String extension = ".class";
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.getName().endsWith(extension);
            }
        });
        for (File f : files) {
            if (f.isDirectory()) {
                collectTests(baseDirectory, f, tests, frameworks);
            } else {
                String path = f.getAbsolutePath();
                String baseDir = baseDirectory.getAbsolutePath();
                path = path.substring(baseDir.length() + 1).replace(File.separatorChar, '.');
                String className = path.substring(0, path.length() - extension.length());
                try {
                    Class<?> cls = Class.forName(className);
                    for (TestFramework framework : frameworks) {
                        Set<String> testList = framework.getTests(cls);
                        if ((testList != null) && !testList.isEmpty()) {
                            tests.addAll(testList);
                            break;
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
