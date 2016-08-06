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

import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * An helper class used to redirect {@link java.lang.System#out} and {@link java.lang.System#err}
 * and restore them at the end of a task.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class ForwardingSystemOutputStreams {
    private final ForwardingOutputStream output;
    private final ForwardingOutputStream error;

    public ForwardingSystemOutputStreams() {
        output = new ForwardingOutputStream(System.out);
        error = new ForwardingOutputStream(System.err);
    }

    /**
     * Run the given task and redirect its error and output streams.
     *
     * @param callable The task to run.
     * @param out The redirected messages sent through {@link java.lang.System#out}.
     * @param err The redirected messages sent through {@link java.lang.System#err}.
     * @return of the task.
     */
    public <T> T forwardOutputStreams(Callable<T> callable, Appendable out, Appendable err) throws Exception {
        output.forward(Thread.currentThread(), new StringOutputStream(out));
        error.forward(Thread.currentThread(), new StringOutputStream(err));
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(error));
        try {
            return callable.call();
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
}
