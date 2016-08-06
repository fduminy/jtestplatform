/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.it;

import org.apache.commons.lang3.Validate;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportFactory;
import org.jtestplatform.server.TestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

import static org.jtestplatform.it.TransportLogger.wrap;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
abstract class InJVMTransportChannel<T extends Transport> implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InJVMTransportChannel.class);

    protected final int domainID;

    private Thread serverThread;
    private TestServer testServer;
    private Transport serverTransport;
    private Transport clientTransport;

    InJVMTransportChannel(int domainID) throws TransportException {
        this.domainID = domainID;
    }

    abstract protected T createServerTransport() throws Exception;

    abstract protected T createClientTransport() throws TransportException;

    public void open() throws Exception {
        Validate.isTrue(serverThread == null, "Server thread already started");

        serverTransport = wrap("server", createServerTransport());
        clientTransport = wrap("client", createClientTransport());

        try {
            testServer = new TestServer(new TransportFactory() {
                @Override
                public Transport create() throws TransportException {
                    return serverTransport;
                }
            });
        } catch (Exception e) {
            throw new TransportException(e.getMessage(), e);
        }

        serverThread = new Thread("server-domain#" + domainID) {
            @Override
            public void run() {
                try {
                    testServer.start();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        };

        serverThread.start();
    }

    @Override
    public void close() throws IOException {
        testServer.requestShutdown();
    }

    public Transport getClientTransport() {
        return clientTransport;
    }
}
