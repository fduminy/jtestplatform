/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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
/**
 * 
 */
package org.jtestplatform.common.transport;

import static org.junit.Assert.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jtestplatform.common.message.Message;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TransportHelperTest {
    @DataPoint
    public static final Message EMPTY_MESSAGE = new EmptyMessage();
    @DataPoint
    public static final Message TEST_MESSAGE = new TestMessage("a message");
    @DataPoint
    public static final Message ENUM_MESSAGE = EnumMessage.A_MESSAGE;

    @Theory
    public void testSendReceive(Message message) throws TransportException {
        Transport transport = new ArrayTransport();
        TransportHelper helper = new TransportHelper();

        helper.send(transport, message);

        Message serverMsg = helper.receive(transport);
        assertNotNull(serverMsg);
        assertEquals(message.getClass(), serverMsg.getClass());
    }

    private static class ArrayTransport implements Transport {
        private final List<String> messages = new ArrayList<String>();
        
        @Override
        public void send(String message) throws TransportException {
            messages.add(message);
        }

        @Override
        public String receive() throws TransportException {
            return messages.remove(0);
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class TestMessage implements Message {
        private String message;
        
        public TestMessage() {
            
        }
        
        public TestMessage(String message) {
            this.message = message;
        }
        
        /**
         * @return
         */
        public String getMessage() {
            return message;
        }

        @Override
        public void sendWith(Transport t) throws TransportException {
            t.send(message);
        }

        @Override
        public void receiveFrom(Transport t) throws TransportException {
            message = t.receive();
        }
    }

    private static class EmptyMessage implements Message {
        public EmptyMessage() {

        }

        @Override
        public void sendWith(Transport t) throws TransportException {
        }

        @Override
        public void receiveFrom(Transport t) throws TransportException {
        }
    }
    
    private static enum EnumMessage implements Message {
        A_MESSAGE,
        ANOTHER_MESSAGE;

        @Override
        public void sendWith(Transport t) throws TransportException {
        }

        @Override
        public void receiveFrom(Transport t) throws TransportException {
        }
    }
}
