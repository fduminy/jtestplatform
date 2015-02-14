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

import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportFactory;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
public class TestServerTest {
    @Theory
    public void testProcessCommand(boolean nullResult) throws Exception {
        // prepare
        Transport transport = mock(Transport.class);
        when(transport.receive()).thenReturn(MockMessage.class.getName());

        TestServer testServer = new TestServer(mock(TransportFactory.class));
        String expectedMessage = "expectedMessage";
        MockMessage expectedAnswer = nullResult ? null : spy(new MockMessage(expectedMessage));
        MockCommand command = spy(new MockCommand(expectedAnswer));
        testServer.addCommand(MockMessage.class, command);

        // test
        testServer.processCommand(transport);

        // verify
        if (!nullResult) {
            ArgumentCaptor<String> sentMessages = ArgumentCaptor.forClass(String.class);
            verify(transport, times(2)).send(sentMessages.capture());
            assertThat(sentMessages.getAllValues()).containsExactly(expectedAnswer.getClass().getName(), expectedMessage);
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

        public MockCommand(MockMessage expectedAnswer) {
            this.expectedAnswer = expectedAnswer;
        }

        @Override
        public MockMessage execute(MockMessage message) throws Exception {
            return expectedAnswer;
        }
    }
}