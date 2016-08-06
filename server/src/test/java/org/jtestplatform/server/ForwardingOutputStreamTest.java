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
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;

import static com.google.code.tempusfugit.condition.Conditions.is;
import static com.google.code.tempusfugit.temporal.Duration.minutes;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static java.lang.Thread.State.TERMINATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class ForwardingOutputStreamTest extends OutputStreamTest<ForwardingOutputStream> {
    @Test
    public void testWrite_int_specificThread() throws Exception {
        new StreamWriteTestInThread(WRITE_INT, 0, 1).test();
    }

    @Test
    public void testWrite_byteArraySegment_specificThread() throws Exception {
        new StreamWriteTestInThread(WRITE_BYTE_ARRAY_SEGMENT, 1, 3).test();
    }

    @Test
    public void testWrite_byteArray_specificThread() throws Exception {
        new StreamWriteTestInThread(WRITE_BYTE_ARRAY, 0, BYTES.length).test();
    }

    @Test
    public void testClose_forward() throws IOException {
        testStreamAction(CLOSE, true, false);
    }

    @Test
    public void testClose_forward_unForward() throws IOException {
        testStreamAction(CLOSE, true, true);
    }

    @Test
    public void testFlush_forward() throws IOException {
        testStreamAction(FLUSH, true, false);
    }

    @Test
    public void testFlush_forward_unForward() throws IOException {
        testStreamAction(FLUSH, true, true);
    }

    private void testStreamAction(final StreamAction<Void> action, final boolean forward, final boolean unForward)
        throws IOException {
        final StreamActionTest test = new StreamActionTest(action) {
            OutputStream threadOutput;

            @Override
            protected StreamActionTest prepare() {
                super.prepare();
                if (forward) {
                    threadOutput = mock(OutputStream.class);
                    stream.forward(Thread.currentThread(), threadOutput);
                    if (unForward) {
                        stream.forward(Thread.currentThread(), null);
                    }
                }
                return this;
            }

            @Override
            protected void verify() throws IOException {
                super.verify();
                if (forward) {
                    action.execute(Mockito.verify(threadOutput, times(unForward ? 0 : 1)), null);
                    verifyNoMoreInteractions(threadOutput);
                }
            }
        };
        test.prepare();
        test.execute();
        test.verify();
    }

    @Override
    protected ForwardingOutputStream createOutputStream(OutputStream backOutputStream) {
        return new ForwardingOutputStream(backOutputStream);
    }

    private class StreamWriteTestInThread extends StreamWriteTest {
        private StringBuilder threadOutput;
        private OutputStream threadStream;

        protected StreamWriteTestInThread(StreamWriteAction action, int startIndexInclusive, int endIndexExclusive) {
            super(action, startIndexInclusive, endIndexExclusive);
        }

        @Override
        public void test() throws Exception {
            prepare();
            testInThread(stream, threadStream);
            verify();
        }

        @Override
        protected void verify() throws IOException {
            super.verify();
            threadStream.flush();
            assertThat(threadOutput.toString()).as("thread stream").isEqualTo(alternateExpectedString);
        }

        @Override
        protected void prepare() {
            super.prepare();
            threadOutput = new StringBuilder();
            threadStream = outputStream(threadOutput);
        }

        private void testInThread(final ForwardingOutputStream stream, final OutputStream threadOutputStream)
            throws Exception {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        stream.forward(Thread.currentThread(), threadOutputStream);
                        action.execute(stream,
                                       new WriteParameter(ALTERNATE_BYTES, startIndexInclusive, endIndexExclusive));
                        stream.forward(Thread.currentThread(), null);
                        action.execute(stream, new WriteParameter(BYTES, startIndexInclusive, endIndexExclusive));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            thread.start();
            waitOrTimeout(is(thread, TERMINATED), timeout(minutes(10)));
        }
    }
}