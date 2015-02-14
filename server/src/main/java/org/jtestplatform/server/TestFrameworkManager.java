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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public final class TestFrameworkManager {
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
     * @return The test framework matching the given name.
     */
    public TestFramework getTestFramework(String framework) throws UnknownTestFrameworkException {
        final TestFramework testFramework = testFrameworks.get(framework);
        if (testFramework == null) {
            throw new UnknownTestFrameworkException(framework);
        }
        return testFramework;
    }

    /**
     * @return The set of test framework names.
     */
    public Set<String> getTestFrameworks() {
        return testFrameworks.keySet();
    }
}
