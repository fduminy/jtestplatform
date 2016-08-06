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
package org.jtestplatform.server;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class TestFrameworkManagerTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetTestFramework_nullString() throws UnknownTestFrameworkException {
        testGetTestFramework(null);
    }

    @Test
    public void testGetTestFramework_emptyString() throws UnknownTestFrameworkException {
        testGetTestFramework("");
    }

    @Test
    public void testGetTestFramework_blankString() throws UnknownTestFrameworkException {
        testGetTestFramework("  ");
    }

    private void testGetTestFramework(String framework) throws UnknownTestFrameworkException {
        thrown.expect(UnknownTestFrameworkException.class);
        thrown.expectMessage("Unknown test framework : " + framework);
        TestFrameworkManager manager = TestFrameworkManager.getInstance();

        manager.getTestFramework(framework);
    }

    @Test
    public void testAddAndGetTestFramework() throws UnknownTestFrameworkException {
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
