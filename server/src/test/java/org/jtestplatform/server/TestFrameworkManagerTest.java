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
package org.jtestplatform.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;


/**
 *
 */

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestFrameworkManagerTest {
    @Test
    public void testCollectTests() {
        TestFrameworkManager manager = TestFrameworkManager.getInstance();
        String path = '/' + getClass().getName().replace('.', '/') + ".class";
        URL url = getClass().getResource(path);
        String p = url.getPath();
        File directory = new File(p.substring(0, p.length() - path.length()));
        Set<String> tests = manager.collectTests(directory);

        assertTrue(tests.contains(getClass().getName() + "#testCollectTests"));
        assertTrue(tests.contains(getClass().getName() + "#testAddAndGetTestFramework"));
        for (String test : TestFrameworkTest.getExpectedTests()) {
            assertTrue(tests.contains(test));
        }
    }

    @Test
    public void testAddAndGetTestFramework() {
        TestFrameworkManager manager = TestFrameworkManager.getInstance();
        int initNumberOfFrameworks = manager.getTestFrameworks().size();


        // add a framework
        String name = "mock";
        TestFramework testFramework = Mockito.mock(TestFramework.class);
        when(testFramework.getName()).thenReturn(name);
        manager.addTestFramework(testFramework);

        // assert it's in manager's frameworks
        Set<String> frameworks = manager.getTestFrameworks();
        assertEquals("wrong number of frameworks", initNumberOfFrameworks + 1, frameworks.size());
        assertTrue("framework is not registered", frameworks.contains(name));

        // assert it's gettable by name
        TestFramework framework = manager.getTestFramework(name);
        assertEquals(testFramework, framework);
    }
}
