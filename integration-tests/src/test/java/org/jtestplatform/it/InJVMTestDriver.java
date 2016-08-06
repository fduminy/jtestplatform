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

import com.google.code.tempusfugit.temporal.MovableClock;
import org.apache.commons.lang3.Validate;
import org.jtestplatform.client.Request;
import org.jtestplatform.client.RequestConsumer;
import org.jtestplatform.client.TestDriver;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.*;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.code.tempusfugit.temporal.Duration.millis;

/**
 * A {@link org.jtestplatform.client.TestDriver} that do everything (including domains) in the same JVM.
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class InJVMTestDriver<T extends InJVMTransportChannel> extends TestDriver {
    private final AtomicInteger timeCounter = new AtomicInteger(1);
    private final InJVMTransportChannelFactory<T> channelFactory;

    public InJVMTestDriver(MovableClock clock, InJVMTransportChannelFactory<T> channelFactory) {
        super(clock);
        Validate.notNull(clock, "clock is null");
        Validate.notNull(channelFactory, "channelFactory is null");

        this.channelFactory = channelFactory;
    }

    @Override
    protected InJVMDomainManager createDomainManager(File cloudConfigFile)
        throws FileNotFoundException, DomainException {
        return new InJVMDomainManager<T>(new FileReader(cloudConfigFile), channelFactory);
    }

    @Override
    protected RequestConsumer createRequestConsumer(BlockingQueue<Request> requests) {
        return new RequestConsumer(requests, clock) {
            @Override
            protected TestResult runTest(TransportHelper transportHelper, Request request, Transport transport)
                throws TransportException {
                ((MovableClock) clock).incrementBy(millis(timeCounter.getAndIncrement()));
                return super.runTest(transportHelper, request, transport);
            }
        };
    }

    static class InJVMDomainManager<T extends InJVMTransportChannel> extends DefaultDomainManager {
        private final List<InJVMDomain> domains = new ArrayList<InJVMDomain>();
        private final InJVMTransportChannelFactory<T> channelFactory;

        public InJVMDomainManager(Reader configReader, InJVMTransportChannelFactory<T> channelFactory)
            throws DomainException {
            super(configReader);
            Validate.notNull(configReader, "configReader is null");
            Validate.notNull(channelFactory, "channelFactory is null");

            this.channelFactory = channelFactory;
        }

        public InJVMTransportChannelFactory<T> getChannelFactory() {
            return channelFactory;
        }

        @Override
        protected Map<String, DomainFactory<? extends Domain>> findKnownFactories() {
            Map<String, DomainFactory<? extends Domain>> result = new HashMap<String, DomainFactory<? extends Domain>>();
            InJVMDomainFactory<T> f = new InJVMDomainFactory<T>(this);
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

    private static class InJVMDomainFactory<T extends InJVMTransportChannel> implements DomainFactory<InJVMDomain> {
        private final AtomicInteger nextDomainID = new AtomicInteger(1);
        private final InJVMDomainManager manager;

        private InJVMDomainFactory(InJVMDomainManager manager) {
            Validate.notNull(manager, "manager is null");

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
            return new InJVMDomain<T>(manager, nextDomainID.getAndIncrement());
        }
    }

}
