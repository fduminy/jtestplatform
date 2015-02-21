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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jtestplatform.common.message.*;
import org.jtestplatform.common.message.Shutdown;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.reflections.Reflections;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Mockito.*;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TransportHelperTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSendRequest() throws TransportException {
        // prepare
        MessageData requestData = MessageData.GETTESTFRAMEWORKS;
        MessageData answerData = MessageData.TESTFRAMEWORKS;
        final Message requestMessage = spy(requestData.createMessage());
        final Message answerMessage = spy(answerData.createMessage());
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(answerData.messageClass.getName(), answerData.expectedParts);
        final MutableObject<Boolean> sendImplCalled = new MutableObject<Boolean>(Boolean.FALSE);
        final MutableObject<Boolean> receiveImplCalled = new MutableObject<Boolean>(Boolean.FALSE);
        TransportHelper helper = new TransportHelper() {
            @Override
            protected void sendImpl(Transport transport, Message message) throws TransportException {
                sendImplCalled.setValue(Boolean.TRUE);
                super.sendImpl(transport, message);
            }

            @Override
            protected Message receiveImpl(Transport transport) throws TransportException {
                receiveImplCalled.setValue(Boolean.TRUE);
                return super.receiveImpl(transport);
            }

            @Override
            Message createMessage(Class<? extends Message> clazz) throws InstantiationException, IllegalAccessException {
                return answerMessage;
            }
        };

        // test
        Message actualAnswerMessage = helper.sendRequest(transport, requestMessage);

        // verify
        assertThat(sendImplCalled.getValue()).as("sendImpl called").isEqualTo(TRUE);
        assertThat(receiveImplCalled.getValue()).as("receiveImpl called").isEqualTo(TRUE);
        assertThat(actualAnswerMessage).as("actual answer").isSameAs(answerMessage);
    }

    @Theory
    public void testSend(MessageData data) throws TransportException, InstantiationException, IllegalAccessException {
        // prepare
        Message message = spy(data.createMessage());
        Transport transport = mock(Transport.class);
        final MutableObject<Boolean> called = new MutableObject<Boolean>(Boolean.FALSE);
        TransportHelper helper = new TransportHelper() {
            @Override
            protected void sendImpl(Transport transport, Message message) throws TransportException {
                called.setValue(Boolean.TRUE);
                super.sendImpl(transport, message);
            }
        };

        // test
        helper.send(transport, message);

        // verify
        assertThat(called.getValue()).as("sendImpl called").isEqualTo(TRUE);
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

    @Test
    public void testReceive_ErrorMessage() throws Exception {
        // prepare
        final String expectedErrorMessage = "errorMessage";
        thrown.expect(TransportException.class);
        thrown.expect(new BaseMatcher<Throwable>() {
            private final String exceptionMessage = "Received Error : " + expectedErrorMessage;

            @Override
            public void describeTo(Description description) {
                description.appendText("a TransportException with ErrorMessage(" + exceptionMessage + ")");
            }

            @Override
            public boolean matches(Object item) {
                if (!(item instanceof TransportException)) {
                    return false;
                }

                TransportException exception = (TransportException) item;
                if (!equalTo(exceptionMessage).matches(exception.getMessage())) {
                    return false;
                }
                ErrorMessage errorMessage = exception.getErrorMessage();
                return (errorMessage != null) && equalTo(expectedErrorMessage).matches(errorMessage.getMessage());
            }
        });

        final MutableObject<Message> messageWrapper = new MutableObject<Message>();
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(ErrorMessage.class.getName(), expectedErrorMessage);
        TransportHelper helper = new TransportHelper() {
            @Override
            Message createMessage(Class<? extends Message> clazz) throws InstantiationException, IllegalAccessException {
                Message message = spy(super.createMessage(clazz));
                messageWrapper.setValue(message);
                return message;
            }
        };

        // test
        helper.receive(transport);
    }

    @Theory
    public void testReceive(MessageData data) throws Exception {
        // prepare
        assumeFalse(ErrorMessage.class.isAssignableFrom(data.messageClass));
        final MutableObject<Message> messageWrapper = new MutableObject<Message>();
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(data.messageClass.getName(), data.expectedParts);
        final MutableObject<Boolean> called = new MutableObject<Boolean>(Boolean.FALSE);
        TransportHelper helper = new TransportHelper() {
            @Override
            protected Message receiveImpl(Transport transport) throws TransportException {
                called.setValue(Boolean.TRUE);
                return super.receiveImpl(transport);
            }

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
        assertThat(called.getValue()).as("receiveImpl called").isEqualTo(TRUE);
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

    @Test
    public void testReceiveInt() throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn("1");

        // test
        int actualValue = TransportHelper.receiveInt(transport);

        // verify
        assertThat(actualValue).isEqualTo(1);
    }

    @Test(expected = TransportException.class)
    public void testReceiveInt_wrongValue() throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn("A");

        // test
        TransportHelper.receiveInt(transport);
    }

    @Test
    public void testSendInt() throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);

        // test
        TransportHelper.sendInt(transport, 1);

        // verify
        verify(transport, times(1)).send("1");
    }

    @Test
    public void testReceiveList() throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn("1", "A");

        // test
        Collection<String> actualValue = TransportHelper.receiveList(transport);

        // verify
        assertThat(actualValue).isEqualTo(Arrays.asList("A"));
    }

    @Test
    public void testSendList() throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);
        Collection<String> actualValue = Arrays.asList("A");

        // test
        TransportHelper.sendList(transport, actualValue);

        // verify
        InOrder inOrder = Mockito.inOrder(transport);
        inOrder.verify(transport, times(1)).send("1");
        inOrder.verify(transport, times(1)).send("A");
        inOrder.verifyNoMoreInteractions();
    }

    @Theory
    public void testReceiveBoolean(boolean expectedValue) throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(expectedValue ? TransportHelper.TRUE : TransportHelper.FALSE);

        // test
        boolean actualValue = TransportHelper.receiveBoolean(transport);

        // verify
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test(expected = TransportException.class)
    public void testReceiveBoolean_wrongValue() throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn("A");

        // test
        TransportHelper.receiveBoolean(transport);
    }

    @Theory
    public void testSendBoolean(boolean expectedValue) throws TransportException {
        // prepare
        Transport transport = mock(Transport.class);

        // test
        TransportHelper.sendBoolean(transport, expectedValue);

        // verify
        verify(transport, times(1)).send(expectedValue ? TransportHelper.TRUE : TransportHelper.FALSE);
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
        },
        ERRORMESSAGE(ErrorMessage.class, "errorMessage") {
            @Override
            Message createMessage() {
                return new ErrorMessage(expectedParts[0]);
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
