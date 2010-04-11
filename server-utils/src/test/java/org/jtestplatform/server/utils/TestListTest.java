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
package org.jtestplatform.server.utils;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestListTest {
    //FIXME
    @Ignore
    @Test
    public void testCollectTests() {
/*
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
*/
    }

    //FIXME
    @Test
    public void test() {
/*
        TestList t = new TestList();
        t.addTestSource(new File("/home/fabien/data/Projets/JNode/projects/svn/all/build/plugins"),
                "org.jnode.", "gnu.");
        t.collectTests();

        System.out.println("tests found:");
        t.writeTestList(System.out);

        if (t.tests.isEmpty()) {
            System.out.println("none");
        }
*/
    }
}
