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

import org.junit.AfterClass;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class UDPTransportTest {
    @DataPoint
    public static final ClientTransport CLIENT_UDP_TRANSPORT;
    @DataPoint
    public static final ServerTransport SERVER_UDP_TRANSPORT;

    public static enum StringMessage {
        MESSAGE("a value"),
        NULL_MESSAGE(null),
        EMPTY_MESSAGE("");
/*
       //TODO handle case of a big message
        A_BIG_MESSAGE(createBigMessage());

        private static String createBigMessage() {
        int size = 10 * 1024 * 1024;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append((char) i);
        }
            return sb.toString();
        }
*/

        private final String value;

        StringMessage(String value) {
            this.value = value;
        }
    }

    static {
        try {
            final int port = 12345;
            DatagramSocket ds = new DatagramSocket();
            ds.connect(InetAddress.getLocalHost(), port);
            CLIENT_UDP_TRANSPORT = new ClientTransport(new UDPTransport(ds));
            SERVER_UDP_TRANSPORT = new ServerTransport(new UDPTransport(new DatagramSocket(port)));
        } catch (SocketException e) {
            throw new Error(e);
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    @AfterClass
    public static void afterClass() {
        try {
            CLIENT_UDP_TRANSPORT.getTransport().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            SERVER_UDP_TRANSPORT.getTransport().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Theory
    public void testSendReceive(ClientTransport client, ServerTransport server, StringMessage message) throws IOException, TransportException {
        client.getTransport().send(message.value);
        String serverMsg = server.getTransport().receive();
        assertEquals(message.value, serverMsg);
    }

    private static class ClientTransport {
        private final Transport transport;

        public ClientTransport(Transport transport) {
            this.transport = transport;
        }

        public Transport getTransport() {
            return transport;
        }
    }

    private static class ServerTransport {
        private final Transport transport;

        public ServerTransport(Transport transport) {
            this.transport = transport;
        }

        public Transport getTransport() {
            return transport;
        }
    }
}
