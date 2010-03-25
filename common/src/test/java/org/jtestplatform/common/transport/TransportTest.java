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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TransportTest {
    @DataPoint
    public static final ClientTransport CLIENT_UDP_TRANSPORT;
    @DataPoint
    public static final ServerTransport SERVER_UDP_TRANSPORT;

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

    @Theory
    public void testSendReceive(ClientTransport client, ServerTransport server) throws IOException, TransportException {        
        try {
            String clientMsg = "a message";
            client.getTransport().send(clientMsg);
            String serverMsg = server.getTransport().receive();
            assertEquals(clientMsg, serverMsg);
        } finally {
            client.getTransport().close();
            server.getTransport().close();
        }
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
