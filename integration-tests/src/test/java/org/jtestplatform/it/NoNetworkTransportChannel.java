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
package org.jtestplatform.it;

import org.jtestplatform.common.transport.TransportException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class NoNetworkTransportChannel extends InJVMTransportChannel<NoNetworkTransport> {
    private final BlockingQueue<String> serverToClient = new LinkedBlockingDeque<String>();
    private final BlockingQueue<String> clientToServer = new LinkedBlockingDeque<String>();

    NoNetworkTransportChannel(int domainID) throws TransportException {
        super(domainID);
    }

    @Override
    protected NoNetworkTransport createServerTransport() {
        return new NoNetworkTransport(clientToServer, serverToClient);
    }

    @Override
    protected NoNetworkTransport createClientTransport() {
        return new NoNetworkTransport(serverToClient, clientToServer);
    }
}
