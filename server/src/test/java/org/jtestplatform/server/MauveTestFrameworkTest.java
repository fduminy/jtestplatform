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

import gnu.testlet.TestHarness;
import gnu.testlet.Testlet;

import static org.jtestplatform.common.transport.TransportHelperTest.SYSTEM_ERR;
import static org.jtestplatform.common.transport.TransportHelperTest.SYSTEM_OUT;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class MauveTestFrameworkTest extends TestFrameworkTest<MauveTestFramework> {
    public MauveTestFrameworkTest() throws Exception {
        super(new MauveTestFramework());

        addSucceedingTest(MauveTestClass.class, null);
        addFailingTest(MauveFailingTestClass.class, null, AssertionError.class.getName(), "got true but expected false",
                       null);
        addTestWithError(MauveTestClassWithError.class, null);
    }

    public static void addTestsTo(MauveTestFramework testFramework) throws Exception {
        testFramework.addTestClass(MauveTestClass.class);
        testFramework.addTestClass(MauveFailingTestClass.class);
        testFramework.addTestClass(MauveTestClassWithError.class);
    }

    public static class MauveTestClass implements Testlet {

        @Override
        public void test(TestHarness harness) {
            harness.check(true, true);
        }
    }

    public static class MauveFailingTestClass implements Testlet {

        @Override
        public void test(TestHarness harness) {
            System.out.println(SYSTEM_OUT);
            System.err.println(SYSTEM_ERR);
            harness.check(true, false); // will fail
        }
    }

    public static class MauveTestClassWithError implements Testlet {

        @Override
        public void test(TestHarness harness) {
            System.out.println(SYSTEM_OUT);
            System.err.println(SYSTEM_ERR);
            throw ERROR;
        }
    }
}
