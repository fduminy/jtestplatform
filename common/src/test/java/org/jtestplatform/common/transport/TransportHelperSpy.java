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
package org.jtestplatform.common.transport;

import java.util.Vector;

import static org.jtestplatform.common.transport.Utils.verifyEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
abstract class TransportHelperSpy extends TransportHelper {
    static final int NB_THREADS = 100;
    static final int NB_REPEATS = 10;
    static final int NB_CALLS = NB_REPEATS * NB_THREADS;

    private final Vector<Long> threadIds = new Vector<Long>(NB_CALLS * 2);
    private final String method;

    TransportHelperSpy(String method) {
        this.method = method;
    }

    void spyMethodCall() {
        final long threadId = Thread.currentThread().getId();

        threadIds.add(threadId);
        Thread.yield();
        threadIds.add(threadId);
    }

    final Transport transport = mock(Transport.class);

    public void verifyThreadSafety() {
        String errors = verifyEquals(method, "size", NB_CALLS * 2, threadIds.size());
        if (errors == null) {
            for (int i = 0; i < threadIds.size(); i += 2) {
                errors = verifyEquals(method, "threadId", threadIds.get(i), threadIds.get(i + 1));
                if (errors != null) {
                    break;
                }
            }
        }
        assertTrue("Thread safety test has failed : " + errors, errors == null);
    }
}
