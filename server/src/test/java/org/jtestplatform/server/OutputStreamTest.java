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

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * An abstract class for testing {@link java.io.OutputStream}.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
abstract public class OutputStreamTest<S extends OutputStream> {
    @Test
    public final void testWrite_int() throws Exception {
        new StreamWriteTest(WRITE_INT, 0, 1).test();
    }

    @Test
    public final void testWrite_byteArraySegment() throws Exception {
        new StreamWriteTest(WRITE_BYTE_ARRAY_SEGMENT, 1, 3).test();
    }

    @Test
    public final void testWrite_byteArray() throws Exception {
        new StreamWriteTest(WRITE_BYTE_ARRAY, 0, BYTES.length).test();
    }

    @Test
    public final void testClose() throws IOException {
        StreamActionTest test = createStreamActionTest(CLOSE);
        test.prepare();
        test.execute();
        test.verify();
    }

    @Test
    public final void testFlush() throws IOException {
        StreamActionTest test = createStreamActionTest(FLUSH);
        test.prepare();
        test.execute();
        test.verify();
    }

    protected interface StreamAction<P> {
        void execute(OutputStream stream, P parameter) throws IOException;
    }

    protected static class WriteParameter {
        protected final byte[] bytes;
        protected final int startIndexInclusive;
        protected final int endIndexExclusive;

        WriteParameter(byte[] bytes, int startIndexInclusive, int endIndexExclusive) {
            this.bytes = bytes;
            this.startIndexInclusive = startIndexInclusive;
            this.endIndexExclusive = endIndexExclusive;
        }
    }

    abstract protected class StreamWriteAction implements StreamAction<WriteParameter> {
    }

    protected final StreamWriteAction WRITE_BYTE_ARRAY = new StreamWriteAction() {
        @Override
        public void execute(OutputStream stream, WriteParameter parameter) throws IOException {
            stream.write(parameter.bytes);
        }
    };

    protected final StreamWriteAction WRITE_BYTE_ARRAY_SEGMENT = new StreamWriteAction() {
        @Override
        public void execute(OutputStream stream, WriteParameter parameter) throws IOException {
            stream.write(parameter.bytes, parameter.startIndexInclusive, parameter.endIndexExclusive - parameter.startIndexInclusive);
        }
    };

    protected final StreamWriteAction WRITE_INT = new StreamWriteAction() {
        @Override
        public void execute(OutputStream stream, WriteParameter parameter) throws IOException {
            stream.write(parameter.bytes[0]);
        }
    };

    protected final StreamAction<Void> FLUSH = new StreamAction<Void>() {
        @Override
        public void execute(OutputStream stream, Void parameter) throws IOException {
            stream.flush();
        }
    };

    protected final StreamAction<Void> CLOSE = new StreamAction<Void>() {
        @Override
        public void execute(OutputStream stream, Void parameter) throws IOException {
            stream.close();
        }
    };

    protected static final byte[] BYTES = {'1', '2', '3', '4'};
    protected static final byte[] ALTERNATE_BYTES = {'A', 'B', 'C', 'D'};

    protected StringBuilder backOutput;

    protected class StreamWriteTest {
        protected final int startIndexInclusive;
        protected final int endIndexExclusive;
        protected final String expectedString;
        protected final String alternateExpectedString;

        protected final StreamWriteAction action;
        protected OutputStream backStream;
        protected S stream;

        StreamWriteTest(StreamWriteAction action, int startIndexInclusive, int endIndexExclusive) {
            this.action = action;
            this.expectedString = new String(subarray(BYTES, startIndexInclusive, endIndexExclusive));
            this.alternateExpectedString = new String(subarray(ALTERNATE_BYTES, startIndexInclusive, endIndexExclusive));
            this.startIndexInclusive = startIndexInclusive;
            this.endIndexExclusive = endIndexExclusive;
        }

        protected void prepare() {
            backOutput = new StringBuilder();
            backStream = outputStream(backOutput);
            stream = createOutputStream(backStream);
        }

        public void test() throws Exception {
            prepare();
            action.execute(stream, new WriteParameter(BYTES, startIndexInclusive, endIndexExclusive));
            verify();
        }

        protected void verify() throws IOException {
            backStream.flush();
            assertThat(backOutput.toString()).as("default stream").isEqualTo(expectedString);
        }
    }

    protected class StreamActionTest {
        protected final StreamAction<Void> action;
        protected OutputStream backStream;
        protected S stream;

        protected StreamActionTest(StreamAction<Void> action) {
            this.action = action;
        }

        protected StreamActionTest prepare() {
            backStream = mock(OutputStream.class);
            stream = createOutputStream(backStream);
            return this;
        }

        public final void execute() throws IOException {
            action.execute(stream, null);
        }

        @SuppressWarnings("unchecked")
        protected void verify() throws IOException {
            action.execute(Mockito.verify(backStream, times(1)), null);
            verifyNoMoreInteractions(backStream);
        }
    }

    abstract protected S createOutputStream(OutputStream backOutputStream);

    public StreamActionTest createStreamActionTest(StreamAction<Void> action) {
        return new StreamActionTest(action);
    }

    public static OutputStream outputStream(StringBuilder output) {
        return new WriterOutputStream(new StringBuilderWriter(output), Charset.defaultCharset(), 1024, true);
    }
}
