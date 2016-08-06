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

import org.junit.Test;

import java.io.PrintStream;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.common.transport.TransportHelperTest.SYSTEM_ERR;
import static org.jtestplatform.common.transport.TransportHelperTest.SYSTEM_OUT;
import static org.mockito.Mockito.*;

public class ForwardingSystemOutputStreamsTest {
    @Test
    public void testForwardOutputStreams() throws Exception {
        // prepare
        ForwardingSystemOutputStreams fwdStreams = new ForwardingSystemOutputStreams();
        final String expectedCallResult = "expectedValue";
        Callable<String> task = spy(new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println(SYSTEM_OUT);
                System.err.println(SYSTEM_ERR);
                return expectedCallResult;
            }
        });
        Appendable actualOut = new StringBuilder();
        Appendable actualErr = new StringBuilder();
        PrintStream outBefore = System.out;
        PrintStream errBefore = System.err;

        // test
        System.out.println("out before call");
        System.out.println("err before call");
        String actualCallResult = fwdStreams.forwardOutputStreams(task, actualOut, actualErr);
        System.out.println("out after call");
        System.out.println("err after call");

        // verify
        verify(task, times(1)).call();
        verifyNoMoreInteractions(task);
        assertThat(actualCallResult).as("returnedValue").isEqualTo(expectedCallResult);
        assertThat(actualOut.toString()).as("out stream").isEqualTo(SYSTEM_OUT + '\n');
        assertThat(actualErr.toString()).as("err stream").isEqualTo(SYSTEM_ERR + '\n');
        assertThat(System.out).as("System.out").isEqualTo(outBefore);
        assertThat(System.err).as("System.err").isEqualTo(errBefore);
    }

    @Test
    public void testForwardOutputStreams_errorInCall() throws Exception {
        // prepare
        ForwardingSystemOutputStreams fwdStreams = new ForwardingSystemOutputStreams();
        Callable<String> task = mock(Callable.class);
        Exception expectedException = new Exception("Error in call");
        when(task.call()).thenThrow(expectedException);
        PrintStream outBefore = System.out;
        PrintStream errBefore = System.err;
        Exception actualException = null;

        // test
        try {
            fwdStreams.forwardOutputStreams(task, mock(Appendable.class), mock(Appendable.class));
        } catch (Exception e) {
            actualException = e;
        }

        // verify
        assertThat(actualException).as("thrown exception").isSameAs(expectedException);
        assertThat(System.out).as("System.out").isEqualTo(outBefore);
        assertThat(System.err).as("System.err").isEqualTo(errBefore);
    }
}