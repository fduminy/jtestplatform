/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
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

import org.jtestplatform.server.TestedClass.InnerTestedClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.server.TestedClass.methodLevel1;
import static org.jtestplatform.server.TestedClass.methodLevel2;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class ServerUtilsTest {
    @Test
    public void testPrintStackTrace_nullClass() {
        Exception e = new Exception("message");

        String actualValue = ServerUtils.printStackTrace(e, (String) null);

        assertThat(actualValue).isEqualTo("java.lang.Exception: message\n" +
                                          "\tat org.jtestplatform.server.ServerUtilsTest.testPrintStackTrace_nullClass(ServerUtilsTest.java:37)");
    }

    @Test
    public void testPrintStackTrace_classNotFound() {
        Exception e = new Exception("message");

        String actualValue = ServerUtils.printStackTrace(e, "unknownClass");

        assertThat(actualValue).isEqualTo("java.lang.Exception: message\n" +
                                          "\tat org.jtestplatform.server.ServerUtilsTest.testPrintStackTrace_classNotFound(ServerUtilsTest.java:47)");
    }

    @Test
    public void testPrintStackTrace_classSingle() {
        Exception e = methodLevel1();

        String actualValue = ServerUtils.printStackTrace(e, TestedClass.class.getName());

        assertThat(actualValue).isEqualTo("java.lang.Exception: message\n" +
                                          "\tat org.jtestplatform.server.TestedClass.methodLevel1(TestedClass.java:32)");
    }

    @Test
    public void testPrintStackTrace_classDuplicate() {
        Exception e = methodLevel2();

        String actualValue = ServerUtils.printStackTrace(e, TestedClass.class.getName());

        assertThat(actualValue).isEqualTo("java.lang.Exception: message\n" +
                                          "\tat org.jtestplatform.server.TestedClass.methodLevel1(TestedClass.java:32)\n"
                                          +
                                          "\tat org.jtestplatform.server.TestedClass.methodLevel2(TestedClass.java:36)");
    }

    @Test
    public void testPrintStackTrace_class_and_methodSingle() {
        Exception e = TestedClass.methodLevel1();

        String actualValue = ServerUtils.printStackTrace(e, TestedClass.class.getName(), "methodLevel1");

        assertThat(actualValue).isEqualTo("java.lang.Exception: message\n" +
                                          "\tat org.jtestplatform.server.TestedClass.methodLevel1(TestedClass.java:32)");
    }

    @Test
    public void testPrintStackTrace_class_and_methodDuplicate() {
        Exception e = InnerTestedClass.methodLevel1();

        String actualValue = ServerUtils.printStackTrace(e, InnerTestedClass.class.getName(), "methodLevel1");

        assertThat(actualValue).isEqualTo("java.lang.Exception: message\n" +
                                          "\tat org.jtestplatform.server.TestedClass.methodLevel1(TestedClass.java:32)\n"
                                          +
                                          "\tat org.jtestplatform.server.TestedClass$InnerTestedClass.methodLevel1(TestedClass.java:41)");
    }
}