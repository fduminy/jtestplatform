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
package org.jtestplatform.common.transport;

import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.google.code.tempusfugit.concurrency.annotations.Repeating;
import org.jtestplatform.common.message.Message;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.jtestplatform.common.transport.TransportHelperSpy.NB_REPEATS;
import static org.jtestplatform.common.transport.TransportHelperSpy.NB_THREADS;


/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Enclosed.class)
public class MultiThreadTransportHelperTest {
    public static class SendRequestMethodTest {
        private static final TransportHelperSpy HELPER = new TransportHelperSpy("sendRequest") {
            @Override
            Message sendRequestImpl(Transport transport, Message message) throws TransportException {
                spyMethodCall();
                return null;
            }
        };

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testSendRequest() throws TransportException {
            HELPER.sendRequest(HELPER.transport, new MockMessage());
        }

        @AfterClass
        public static void afterClass() {
            HELPER.verifyThreadSafety();
        }
    }

    public static class SendMethodTest {
        private static final TransportHelperSpy HELPER = new TransportHelperSpy("send") {
            @Override
            protected void sendImpl(Transport transport, Message message) throws TransportException {
                spyMethodCall();
            }
        };

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testSend() throws TransportException {
            HELPER.send(HELPER.transport, new MockMessage());
        }

        @AfterClass
        public static void afterClass() {
            HELPER.verifyThreadSafety();
        }
    }

    public static class ReceiveMethodTest {
        private static final TransportHelperSpy HELPER = new TransportHelperSpy("receive") {
            @Override
            protected Message receiveImpl(Transport transport) throws TransportException {
                spyMethodCall();
                return null;
            }
        };

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testReceive() throws TransportException {
            HELPER.receive(HELPER.transport);
        }

        @AfterClass
        public static void afterClass() {
            HELPER.verifyThreadSafety();
        }
    }
}
