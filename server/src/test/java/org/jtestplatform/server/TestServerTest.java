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

import org.jtestplatform.common.message.ErrorMessage;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportFactory;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class TestServerTest {
    @Test
    public void testProcessCommand_noCommandForMessage() throws Exception {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(MockMessage.class.getName());
        TestServer testServer = new TestServer(mock(TransportFactory.class));

        // test
        testServer.processCommand(transport);

        // verify
        verifySentErrorMessage(transport, "No command for message : " + MockMessage.class.getName());
    }

    @Test
    public void testProcessCommand_errorInSendReply() throws Exception {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(MockMessage.class.getName());
        doThrow(new TransportException("error")).when(transport).send(any(String.class));
        TestServer testServer = new TestServer(mock(TransportFactory.class));
        MockCommand command = new MockCommand(null, new MockMessage("message"));
        testServer.addCommand(MockMessage.class, command);

        // test
        testServer.processCommand(transport);

        // verify
        verify(transport, times(1)).receive();
        verify(transport, times(1)).send(eq(command.expectedAnswer.getClass().getName()));
        verifyNoMoreInteractions(transport);
    }

    @Test
    public void testProcessCommand_errorInCommand() throws Exception {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(MockMessage.class.getName());

        TestServer testServer = new TestServer(mock(TransportFactory.class));
        MockCommand command = new MockCommand("Something wrong happened in the command", null);
        testServer.addCommand(MockMessage.class, command);

        // test
        testServer.processCommand(transport);

        // verify
        verifySentErrorMessage(transport,
                               "Error in " + command.getClass().getSimpleName() + " : " + command.expectedError);
    }

    private void verifySentErrorMessage(Transport transport, String errorMessage) throws TransportException {
        verify(transport, times(1)).receive();
        ArgumentCaptor<String> sentMessages = ArgumentCaptor.forClass(String.class);
        verify(transport, times(2)).send(sentMessages.capture());
        assertThat(sentMessages.getAllValues()).containsExactly(ErrorMessage.class.getName(), errorMessage);
        verifyNoMoreInteractions(transport);
    }

    @Theory
    public void testProcessCommand(boolean nullResult) throws Exception {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(MockMessage.class.getName());

        TestServer testServer = new TestServer(mock(TransportFactory.class));
        String expectedMessage = "expectedMessage";
        MockMessage expectedAnswer = nullResult ? null : spy(new MockMessage(expectedMessage));
        MockCommand command = spy(new MockCommand(null, expectedAnswer));
        testServer.addCommand(MockMessage.class, command);

        // test
        testServer.processCommand(transport);

        // verify
        if (!nullResult) {
            ArgumentCaptor<String> sentMessages = ArgumentCaptor.forClass(String.class);
            verify(transport, times(2)).send(sentMessages.capture());
            assertThat(sentMessages.getAllValues())
                .containsExactly(expectedAnswer.getClass().getName(), expectedMessage);
            verify(expectedAnswer, times(1)).sendWith(refEq(transport));
            verifyNoMoreInteractions(expectedAnswer);
        }
        verify(transport, times(1)).receive();
        verify(command, times(1)).execute(any(MockMessage.class));
        verifyNoMoreInteractions(transport, command);
    }

    public static class MockMessage implements Message {
        private final String message;

        public MockMessage() {
            this("");
        }

        public MockMessage(String message) {
            this.message = message;
        }

        @Override
        public void sendWith(Transport t) throws TransportException {
            t.send(message);
        }

        @Override
        public void receiveFrom(Transport t) throws TransportException {
        }
    }

    public static class MockCommand implements TestServerCommand<MockMessage, MockMessage> {
        private final MockMessage expectedAnswer;
        private final String expectedError;

        public MockCommand(String expectedError, MockMessage expectedAnswer) {
            this.expectedError = expectedError;
            this.expectedAnswer = expectedAnswer;
        }

        @Override
        public MockMessage execute(MockMessage message) throws Exception {
            if (expectedError != null) {
                throw new Exception(expectedError);
            }
            return expectedAnswer;
        }
    }
}