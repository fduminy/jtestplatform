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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a stream that dispatch its output to another one specified by calling {@link #forward(Thread, java.io.OutputStream)}
 * or the default one given to the constructor.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class ForwardingOutputStream extends OutputStream {
    private final OutputStream defaultOutput;
    private final Map<WeakReference<Thread>, OutputStream> streams = new HashMap<WeakReference<Thread>, OutputStream>();

    public ForwardingOutputStream(OutputStream defaultOutput) {
        if (defaultOutput == null) {
            throw new NullPointerException("defaultOutput is null");
        }
        this.defaultOutput = defaultOutput;
    }

    @Override
    public void write(int b) throws IOException {
        getOutputStream().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        getOutputStream().write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        getOutputStream().write(b);
    }

    @Override
    public void close() throws IOException {
        defaultOutput.close();
        for (OutputStream stream : streams.values()) {
            stream.close();
        }
    }

    @Override
    public void flush() throws IOException {
        defaultOutput.flush();
        for (OutputStream stream : streams.values()) {
            stream.flush();
        }
    }

    public void forward(Thread thread, OutputStream threadOutputStream) {
        WeakReference<Thread> key = new WeakReference<Thread>(thread);
        synchronized (streams) {
            if (threadOutputStream == null) {
                Map.Entry<WeakReference<Thread>, OutputStream> entry = getOutputStreamEntry(thread);
                if (entry != null) {
                    streams.remove(entry.getKey());
                }
            } else {
                streams.put(key, threadOutputStream);
            }
        }
    }

    private OutputStream getOutputStream() {
        Thread thread = Thread.currentThread();
        OutputStream stream;

        synchronized (streams) {
            Map.Entry<WeakReference<Thread>, OutputStream> entry = getOutputStreamEntry(thread);
            stream = (entry == null) ? null : entry.getValue();
        }

        return (stream == null) ? defaultOutput : stream;
    }

    private Map.Entry<WeakReference<Thread>, OutputStream> getOutputStreamEntry(Thread thread) {
        for (Map.Entry<WeakReference<Thread>, OutputStream> entry : streams.entrySet()) {
            if (thread.equals(entry.getKey().get())) {
                return entry;
            }
        }
        return null;
    }
}
