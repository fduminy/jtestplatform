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
 * -
 */
package org.jtestplatform.client.domain;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jtestplatform.client.TransportProvider;
import org.jtestplatform.client.domain.watchdog.WatchDog;
import org.jtestplatform.client.domain.watchdog.WatchDogListener;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.UDPTransport;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.Factory;
import org.jtestplatform.configuration.Platform;

public class DomainManager implements TransportProvider {
    private static final Logger LOGGER = Logger.getLogger(DomainManager.class);

    private final LoadBalancer<DomainManagerDelegate> delegates;
    private final WatchDog watchDog;
    private final LoadBalancer<Domain> domains;
    private final int maxNumberOfDomains;
    private final int serverPort;

    public DomainManager(Configuration config, Map<String, DomainFactory<? extends Domain>> knownFactories) throws DomainException {
        checkValid(knownFactories, config);

        maxNumberOfDomains = Math.max(1, config.getDomains().getMax());
        serverPort = config.getServerPort();
        
        watchDog = new WatchDog(config);
        watchDog.addWatchDogListener(new WatchDogListener() {
            @Override
            public void domainDied(Domain domain) {
                DomainManager.this.domainDied(domain);
            }
        });

        domains = new LoadBalancer<Domain>();

        List<DomainManagerDelegate> dmDelegates = new ArrayList<DomainManagerDelegate>();
        for (Factory factory : config.getDomains().getFactories()) {
            DomainFactory<? extends Domain> domainFactory = knownFactories.get(factory.getType());
            DomainManagerDelegate data = new DomainManagerDelegate(domainFactory, factory.getConnections());
            dmDelegates.add(data);
        }

        delegates = new LoadBalancer<DomainManagerDelegate>(dmDelegates);
    }

    /**
     * @param platform
     * @return
     */
    private DomainConfig createDomainConfig(Platform platform) {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setCdrom(platform.getCdrom());
        domainConfig.setMemory(platform.getMemory());
        domainConfig.setDomainName(null); // null => will be defined automatically
        return domainConfig;
    }

    private void domainDied(Domain domain) {
        domains.remove(domain);
    }

    public void start() {
        watchDog.startWatching();
    }

    public void stop() {
        watchDog.stopWatching();

        for (Domain domain : domains.clear()) {
            // stop the watch dog before actually stop the domain
            watchDog.unwatch(domain);

            LOGGER.info("stopping domains");
            try {
                domain.stop();
            } catch (DomainException e) {
                LOGGER.error("an error happened while stopping", e);
            }
        }
    }        
    
    /**
     * {@inheritDoc}
     * @throws TransportException 
     */
    @Override
    public Transport get(Platform platform) throws TransportException {
        try {
            //TODO put the connection/transport in cache and remove it when the domain stop/die
            DomainConfig domainConfig = createDomainConfig(platform);
            String host = getNextIP(domainConfig);

            DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName(host), serverPort);
            return new UDPTransport(socket);
        } catch (SocketException e) {
            throw new TransportException("failed to create socket", e);
        } catch (UnknownHostException e) {
            throw new TransportException("failed to find host", e);
        }
    }

    private synchronized String getNextIP(DomainConfig domainConfig) throws TransportException {
        if (domains.size() < maxNumberOfDomains) {
            // create a domain if there is less than maximum
            // (it includes the case where there is no domain running)

            try {
                DomainManagerDelegate delegate = delegates.getNext();

                Domain domain = delegate.createDomain(domainConfig);
                domains.add(domain);
                watchDog.watch(domain);
            } catch (DomainException ce) {
                throw new TransportException("unable to create a new domain", ce);
            }
        }

        // TODO ensure that the ip address correspond to a domain with the appropriate platform
        return domains.getNext().getIPAddress();
    }

    private void checkValid(Map<String, DomainFactory<? extends Domain>> knownFactories, Configuration config) throws DomainException {
        if ((knownFactories == null) || knownFactories.isEmpty()) {
            throw new DomainException("no known factory");
        }
        if (config.getDomains() == null) {
            throw new DomainException("domains is not defined");
        }
        if ((config.getDomains().getFactories() == null) || config.getDomains().getFactories().isEmpty()) {
            throw new DomainException("no factory has been defined");
        }

        StringBuilder wrongTypes = new StringBuilder();
        StringBuilder typesWithoutConnection = new StringBuilder();
        for (Factory factory : config.getDomains().getFactories()) {
            DomainFactory<? extends Domain> domainFactory = knownFactories.get(factory.getType());
            if (domainFactory == null) {
                LOGGER.error("no DomainFactory for type " + factory.getType());

                if (wrongTypes.length() != 0) {
                    wrongTypes.append(", ");
                }
                wrongTypes.append(factory.getType());
                continue;
            }

            if ((factory.getConnections() == null) || factory.getConnections()
                    .isEmpty()) {
                LOGGER.error("no connection for type " + factory.getType());

                if (typesWithoutConnection.length() != 0) {
                    typesWithoutConnection.append(", ");
                }
                typesWithoutConnection.append(factory.getType());
                continue;
            }
        }
        if ((wrongTypes.length() != 0) || (typesWithoutConnection.length() != 0)) {
            StringBuilder message = new StringBuilder("Invalid configuration:\n");

            if (wrongTypes.length() != 0) {
                message.append("\tno DomainFactory for types ").append(wrongTypes).append('\n');
            }

            if (typesWithoutConnection.length() != 0) {
                message.append("\tno connection for types ").append(typesWithoutConnection);
            }

            throw new DomainException(wrongTypes.toString());
        }
    }
}
