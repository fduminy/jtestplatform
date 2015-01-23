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

import org.apache.commons.lang3.mutable.MutableObject;
import org.jtestplatform.common.message.*;
import org.jtestplatform.common.message.Shutdown;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.reflections.Reflections;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TransportHelperTest {

    @Theory
    public void testSend(MessageData data) throws TransportException, InstantiationException, IllegalAccessException {
        // prepare
        Message message = spy(data.createMessage());
        Transport transport = mock(Transport.class);
        TransportHelper helper = new TransportHelper();

        // test
        helper.send(transport, message);

        // verify
        InOrder inOrder = inOrder(transport, message);
        inOrder.verify(transport, times(1)).send(eq(message.getClass().getName()));
        inOrder.verify(message, times(1)).sendWith(eq(transport));
        for (int i = 0; i < data.expectedParts.length; i++) {
            inOrder.verify(transport, times(1)).send(eq(data.expectedParts[i]));
        }
        inOrder.verifyNoMoreInteractions();
    }

    @Theory
    public void testCreateMessage(MessageData data) throws Exception {
        TransportHelper helper = new TransportHelper();

        Message message = helper.createMessage(data.messageClass);

        assertThat(message).isExactlyInstanceOf(data.messageClass);
    }

    @Theory
    public void testReceive(MessageData data) throws Exception {
        // prepare
        final MutableObject<Message> messageWrapper = new MutableObject<Message>();
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(data.messageClass.getName(), data.expectedParts);
        TransportHelper helper = new TransportHelper() {
            @Override
            Message createMessage(Class<? extends Message> clazz) throws InstantiationException, IllegalAccessException {
                Message message = spy(super.createMessage(clazz));
                messageWrapper.setValue(message);
                return message;
            }
        };

        // test
        Message actualMessage = helper.receive(transport);

        // verify
        assertNotNull(actualMessage);
        Message message = messageWrapper.getValue();
        assertEquals(message.getClass(), actualMessage.getClass());
        InOrder inOrder = inOrder(transport, message);
        inOrder.verify(transport, times(1)).receive();
        inOrder.verify(message, times(1)).receiveFrom(eq(transport));
        inOrder.verify(transport, times(data.expectedParts.length)).receive();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void verifyAllMessagesInMessageData() {
        List<Class<? extends Message>> actualClasses = new ArrayList<Class<? extends Message>>();
        for (MessageData data : MessageData.values()) {
            if (!(actualClasses.contains(data.messageClass) && data.duplicateMessageClass)) {
                actualClasses.add(data.messageClass);
            }
        }
        Collections.sort(actualClasses, CLASS_COMPARATOR);

        Reflections reflections = new Reflections(Message.class.getPackage().getName());
        List<Class<? extends Message>> expectedClasses = new ArrayList<Class<? extends Message>>(reflections.getSubTypesOf(Message.class));
        Collections.sort(expectedClasses, CLASS_COMPARATOR);

        assertThat(actualClasses).containsExactly(expectedClasses.toArray(new Class[0]));
    }

    private static final Comparator<Class<?>> CLASS_COMPARATOR = new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    @SuppressWarnings("all")
    public static enum MessageData {
        RUNTEST(RunTest.class, "framework", "test") {
            @Override
            Message createMessage() {
                return new RunTest(expectedParts[0], expectedParts[1]);
            }
        },
        SHUTDOWN(Shutdown.class) {
            @Override
            Message createMessage() {
                return Shutdown.INSTANCE;
            }
        },
        TESTFRAMEWORKS(TestFrameworks.class, "2", "framework1", "framework2") {
            @Override
            Message createMessage() {
                return new TestFrameworks(new HashSet<String>(Arrays.asList(expectedParts).subList(1, expectedParts.length)));
            }
        },
        TESTRESULT_SUCCESS(TestResult.class, true, "framework", "test", TransportHelper.TRUE) {
            @Override
            Message createMessage() {
                return new TestResult(expectedParts[0], expectedParts[1], TransportHelper.TRUE.equals(expectedParts[2]));
            }
        },
        TESTRESULT_FAILURE(TestResult.class, true, "framework", "test", TransportHelper.FALSE) {
            @Override
            Message createMessage() {
                return new TestResult(expectedParts[0], expectedParts[1], TransportHelper.TRUE.equals(expectedParts[2]));
            }
        },
        GETTESTFRAMEWORKS(GetTestFrameworks.class) {
            @Override
            Message createMessage() {
                return GetTestFrameworks.INSTANCE;
            }
        },
        GETFRAMEWORKTESTS(GetFrameworkTests.class, "framework") {
            @Override
            Message createMessage() {
                return new GetFrameworkTests(expectedParts[0]);
            }
        },
        FRAMEWORKTESTS(FrameworkTests.class, "2", "test1", "test2") {
            @Override
            Message createMessage() {
                return new FrameworkTests(Arrays.asList(expectedParts).subList(1, expectedParts.length));
            }
        };

        private final Class<? extends Message> messageClass;
        protected final String[] expectedParts;
        private final boolean duplicateMessageClass;

        MessageData(Class<? extends Message> messageClass, String... parts) {
            this(messageClass, false, parts);
        }

        MessageData(Class<? extends Message> messageClass, boolean duplicateMessageClass, String... parts) {
            this.messageClass = messageClass;
            this.duplicateMessageClass = duplicateMessageClass;
            expectedParts = parts;
        }

        abstract Message createMessage();

        @Override
        public String toString() {
            return messageClass.getSimpleName();
        }
    }
}
