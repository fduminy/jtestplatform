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

import com.google.code.tempusfugit.temporal.MovableClock;
import org.jtestplatform.client.Request;
import org.jtestplatform.client.RequestConsumer;
import org.jtestplatform.client.TestDriver;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.*;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportFactory;
import org.jtestplatform.common.transport.TransportHelper;
import org.jtestplatform.server.TestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static org.jtestplatform.it.TransportLogger.wrap;

/**
 * A {@link org.jtestplatform.client.TestDriver} that do everything (including domains) in the same JVM.
 */
public class InJVMTestDriver extends TestDriver {
    private final AtomicInteger timeCounter = new AtomicInteger(1);

    public InJVMTestDriver(MovableClock clock) {
        super(clock);
    }

    @Override
    protected InJVMDomainManager createDomainManager(File cloudConfigFile) throws FileNotFoundException, DomainException {
        return new InJVMDomainManager(new FileReader(cloudConfigFile));
    }

    @Override
    protected RequestConsumer createRequestConsumer(BlockingQueue<Request> requests) {
        return new RequestConsumer(requests, clock) {
            @Override
            protected TestResult runTest(TransportHelper transportHelper, Request request, Transport transport) throws TransportException {
                ((MovableClock) clock).incrementBy(millis(timeCounter.getAndIncrement()));
                return super.runTest(transportHelper, request, transport);
            }
        };
    }

    private static class InJVMDomainManager extends DefaultDomainManager {
        private final List<InJVMDomain> domains = new ArrayList<InJVMDomain>();

        public InJVMDomainManager(Reader configReader) throws DomainException {
            super(configReader);
        }

        @Override
        protected Map<String, DomainFactory<? extends Domain>> findKnownFactories() {
            Map<String, DomainFactory<? extends Domain>> result = new HashMap<String, DomainFactory<? extends Domain>>();
            InJVMDomainFactory f = new InJVMDomainFactory(this);
            result.put(f.getType(), f);

            return result;
        }

        @Override
        protected Transport createTransport(String host, int port, int timeout) throws TransportException {
            try {
                return findDomain(host).getClientTransport();
            } catch (DomainException e) {
                throw new TransportException(e.getMessage(), e);
            }
        }

        private InJVMDomain findDomain(String host) throws TransportException {
            InJVMDomain domain = null;
            for (InJVMDomain d : domains) {
                if (d.getIPAddress().equals(host)) {
                    domain = d;
                    break;
                }
            }

            if (domain == null) {
                throw new TransportException("unable to find domain " + host);
            }
            return domain;
        }

        public void register(InJVMDomain domain) {
            domains.add(domain);
        }

        public void unregister(InJVMDomain domain) {
            domains.remove(domain);
        }
    }

    private static class InJVMDomainFactory implements DomainFactory<InJVMDomain> {
        private final AtomicInteger nextDomainID = new AtomicInteger(1);
        private final InJVMDomainManager manager;

        private InJVMDomainFactory(InJVMDomainManager manager) {
            this.manager = manager;
        }

        @Override
        public String getType() {
            return "inJVM";
        }

        @Override
        public boolean support(Platform platform, Connection connection) throws DomainException {
            return true;
        }

        @Override
        public InJVMDomain createDomain(DomainConfig config, Connection connection) throws DomainException {
            return new InJVMDomain(manager, nextDomainID.getAndIncrement());
        }
    }

    private static class InJVMDomain implements Domain {
        private final InJVMDomainManager manager;
        private final int domainID;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final TransportChannel channel;

        public InJVMDomain(InJVMDomainManager manager, int domainID) throws DomainException {
            this.manager = manager;
            this.domainID = domainID;
            manager.register(this);

            try {
                channel = new TransportChannel(domainID);
            } catch (TransportException e) {
                throw new DomainException(e);
            }
        }

        @Override
        public String start() throws DomainException {
            if (!isAlive()) {
                running.set(true);
                try {
                    channel.open();
                } catch (Exception e) {
                    throw new DomainException(e);
                }
            }

            return getIPAddress();
        }

        @Override
        public void stop() throws DomainException {
            if (isAlive()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    throw new DomainException(e);
                }

                manager.unregister(this);
                running.set(false);
            }
        }

        @Override
        public boolean isAlive() throws DomainException {
            return running.get();
        }

        @Override
        public String getIPAddress() {
            return String.format("0.0.0.%d", domainID);
        }

        @Override
        public String toString() {
            return "Domain #" + domainID;
        }

        public Transport getClientTransport() throws DomainException {
            if (!isAlive()) {
                //throw new DomainException("Domain " + getIPAddress() + " not started");
                start();
            }

            return channel.getClientTransport();
        }
    }

    private static class TransportChannel implements Closeable {
        private static final Logger LOGGER = LoggerFactory.getLogger(TransportChannel.class);

        private final Thread serverThread;
        private final TestServer testServer;
        private final Transport serverTransport;
        private final Transport clientTransport;

        private TransportChannel(int domainID) throws TransportException {
            BlockingQueue<String> serverToClient = new LinkedBlockingDeque<String>();
            BlockingQueue<String> clientToServer = new LinkedBlockingDeque<String>();
            serverTransport = wrap("server", new InJVMTransport(clientToServer, serverToClient));
            clientTransport = wrap("client", new InJVMTransport(serverToClient, clientToServer));

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
        }

        public void open() throws Exception {
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

    private static class InJVMTransport implements Transport {
        private final BlockingQueue<String> outBox;
        private final BlockingQueue<String> inBox;

        private InJVMTransport(BlockingQueue<String> inBox, BlockingQueue<String> outBox) {
            this.outBox = outBox;
            this.inBox = inBox;
        }

        @Override
        public void close() throws IOException {
            inBox.clear();
            outBox.clear();
        }

        @Override
        public void send(String message) throws TransportException {
            try {
                outBox.put(message);
            } catch (InterruptedException e) {
                throw new TransportException(e.getMessage(), e);
            }
        }

        @Override
        public String receive() throws TransportException {
            try {
                return inBox.take();
            } catch (InterruptedException e) {
                throw new TransportException(e.getMessage(), e);
            }
        }
    }
}
