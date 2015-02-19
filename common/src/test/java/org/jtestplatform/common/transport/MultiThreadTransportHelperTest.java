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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.min;
import static java.util.Arrays.deepEquals;
import static org.jtestplatform.common.transport.Utils.verifyEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 */
@RunWith(Enclosed.class)
public class MultiThreadTransportHelperTest {
    private static final int NB_THREADS = 100;
    private static final int NB_REPEATS = 10;
    private static final int NB_CALLS = NB_REPEATS * NB_THREADS;

    public static class SendMethodTest {
        private static final TransportHelper HELPER = new TransportHelper();

        private static final Vector<String> EXPECTED_MESSAGE_PARTS = new Vector<String>();
        private static final Transport TRANSPORT = mock(Transport.class);
        private static final ArgumentCaptor<String> MESSAGE_PARTS = ArgumentCaptor.forClass(String.class);

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @BeforeClass
        public static void beforeClass() throws TransportException {
            EXPECTED_MESSAGE_PARTS.clear();
            Mockito.reset(TRANSPORT);
            doNothing().when(TRANSPORT).send(MESSAGE_PARTS.capture());
        }

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testSend() throws TransportException {
            MockMessage message = new MockMessage();
            EXPECTED_MESSAGE_PARTS.addAll(message.getMessageParts());
            HELPER.send(TRANSPORT, message);
        }

        @AfterClass
        public static void afterClass() {
            List<String> actualMessageParts = new ArrayList<String>(MESSAGE_PARTS.getAllValues());
            String errors = verifyEquals("send", "size", NB_CALLS * (1 + MockMessage.PART_COUNT), actualMessageParts.size());
            if (errors == null) {
                while (!actualMessageParts.isEmpty()) {
                    List<String> actualParts = actualMessageParts.subList(0, MockMessage.PART_COUNT);

                    String messagePart = actualParts.get(1); // skip message class name (at index 0)
                    int index = EXPECTED_MESSAGE_PARTS.indexOf(messagePart);
                    index = (index > 0) ? (index - 1) : -1; // compute message class name index
                    if (index < 0) {
                        errors = "message part not found : " + messagePart;
                        break;
                    }
                    List<String> expectedParts = EXPECTED_MESSAGE_PARTS.subList(index, min(index + MockMessage.PART_COUNT, EXPECTED_MESSAGE_PARTS.size()));

                    if (!deepEquals(actualParts.toArray(), expectedParts.toArray())) {
                        errors = "message parts in wrong order";
                        break;
                    }

                    actualMessageParts = actualMessageParts.subList(1 + MockMessage.PART_COUNT, actualMessageParts.size());
                }
            }
            assertTrue("Thread safety test has failed : " + errors, errors == null);
        }

    }

/*
    public static class ReceiveMethodTest {
        private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveMethodTest.class);
        private static final TransportHelper HELPER = new TransportHelper() {
            @Override
            Class<? extends Message> receiveMessageClass(Transport transport) throws TransportException, ClassNotFoundException {
                return MockMessage.class;
            }
        };

        private static final Vector<String> EXPECTED_MESSAGE_PARTS = new Vector<String>();
        private static final AtomicInteger PARTS_INDEX = new AtomicInteger(0);
        private static final Transport TRANSPORT = mock(Transport.class);
        private static final Vector<String> ACTUAL_MESSAGE_PARTS = new Vector<String>();

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @BeforeClass
        public static void beforeClass() throws TransportException {
            EXPECTED_MESSAGE_PARTS.clear();
            Mockito.reset(TRANSPORT);
            PARTS_INDEX.set(0);

            for (int i = 0; i < NB_CALLS; i++) {
                MockMessage message = new MockMessage();
                EXPECTED_MESSAGE_PARTS.addAll(message.getMessageParts());
            }
            when(TRANSPORT.receive()).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                    final String messagePart = EXPECTED_MESSAGE_PARTS.get(PARTS_INDEX.getAndIncrement());
                    LOGGER.debug(messagePart);
                    return messagePart;
                }
            });
        }

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testReceive() throws TransportException {
            MockMessage message = (MockMessage)HELPER.receive(TRANSPORT);
            ACTUAL_MESSAGE_PARTS.addAll(message.getMessageParts());
        }

        @AfterClass
        public static void afterClass() {
            List<String> actualMessageParts = ACTUAL_MESSAGE_PARTS;
            String errors = verifyEquals("receive", "size", NB_CALLS * (1 + MockMessage.PART_COUNT), actualMessageParts.size());
            if (errors == null) {
                while (!actualMessageParts.isEmpty()) {
                    List<String> actualParts = actualMessageParts.subList(0, MockMessage.PART_COUNT);

                    String messagePart = actualParts.get(1); // skip message class name (at index 0)
                    int index = EXPECTED_MESSAGE_PARTS.indexOf(messagePart);
                    index = (index > 0) ? (index - 1) : -1; // compute message class name index
                    if (index < 0) {
                        errors = "message part not found : " + messagePart;
                        break;
                    }
                    List<String> expectedParts = EXPECTED_MESSAGE_PARTS.subList(index, min(index + MockMessage.PART_COUNT, EXPECTED_MESSAGE_PARTS.size()));

                    if (!deepEquals(actualParts.toArray(), expectedParts.toArray())) {
                        errors = "message parts in wrong order";
                        break;
                    }

                    actualMessageParts = actualMessageParts.subList(1 + MockMessage.PART_COUNT, actualMessageParts.size());
                }
            }
            assertTrue("Thread safety test has failed : " + errors, errors == null);
        }
    }
*/
}
