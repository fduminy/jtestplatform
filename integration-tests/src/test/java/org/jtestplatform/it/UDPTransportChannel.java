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
package org.jtestplatform.it;

import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.UDPTransport;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class UDPTransportChannel extends InJVMTransportChannel<UDPTransport> {
    private static final int FIRST_TEST_SERVER_PORT = 10000;

    private final int serverPort;

    UDPTransportChannel(int domainID) throws TransportException {
        super(domainID);
        this.serverPort = FIRST_TEST_SERVER_PORT + domainID;
    }

    @Override
    protected UDPTransport createServerTransport() throws TransportException {
        return new UDPTransport(serverPort);
    }

    @Override
    protected UDPTransport createClientTransport() throws TransportException {
        try {
            return new UDPTransport(InetAddress.getLocalHost(), serverPort, 0);
        } catch (UnknownHostException e) {
            throw new TransportException(e.getMessage(), e);
        }
    }
}
