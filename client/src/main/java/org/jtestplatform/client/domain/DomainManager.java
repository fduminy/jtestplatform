/**
 * JTestPlatform is a client/server framework for testing any JVM implementation.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jtestplatform.client.domain;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.libvirt.LibVirtDomainFactory;
import org.jtestplatform.client.domain.watchdog.WatchDog;
import org.jtestplatform.client.domain.watchdog.WatchDogListener;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportProvider;
import org.jtestplatform.common.transport.UDPTransport;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.Factory;

public class DomainManager implements TransportProvider {
    private static final Logger LOGGER = Logger.getLogger(DomainManager.class);
    
    private final Map<DomainFactory<? extends Domain>, DomainConfig> factories;
    private final WatchDog watchDog;
    private final int minNbDomains;
    private final List<Domain> domains;
    
    @SuppressWarnings("unchecked")
    public DomainManager(Configuration config) throws ConfigurationException {
        factories = new HashMap<DomainFactory<? extends Domain>, DomainConfig>();
        watchDog = new WatchDog(config);
        
        watchDog.addWatchDogListener(new WatchDogListener() {
            @Override
            public void domainDied(Domain domain) {
                DomainManager.this.domainDied(domain);
            }
        });
        
        minNbDomains = config.getDomains().getMin();
        domains = new ArrayList<Domain>(minNbDomains);        
    }

    private void domainDied(Domain domain) {
        if (domains.remove(domain)) {
            maybeStartNewProcess();
        }
    }
    
    private boolean maybeStartNewProcess() throws RuntimeException {
        boolean started = false;
        
        if ((domains.size() < minNbDomains) && !factories.isEmpty()) {
            try {
                DomainFactory<? extends Domain> factory = factories.keySet().iterator().next();
                DomainConfig config = factories.get(factory);
                Domain domain = factory.createDomain(config);
                domains.add(domain);
                watchDog.watch(domain);
                started = true;
            } catch (ConfigurationException ce) {
                throw new RuntimeException(ce);
            }
        } else {
            started = true;
        }
        
        return started;
    }

    public void start() {
        watchDog.startWatching();
        
        boolean started = false;
        do {
            started = maybeStartNewProcess();
        } while (!started);
    }

    public void stop() {
        watchDog.stopWatching();
        
        for (Domain domain : domains) {
            // stop the watch dog before actually stop the domain
            watchDog.unwatch(domain);
    
            LOGGER.info("stopping domains");        
            try {
                domain.stop();
            } catch (IOException e) {
                LOGGER.error("an error happened while stopping", e);
            } catch (ConfigurationException e) {
                LOGGER.error("an error happened while stopping", e);
            }
        }
        
        domains.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transport get() throws TransportException {
        try {
            DatagramSocket socket = new DatagramSocket();
            String host = getNextIP();
            int port = 0; //TODO get port        
            socket.connect(InetAddress.getByName(host), port);
            return new UDPTransport(socket);
        } catch (SocketException e) {
            throw new TransportException("failed to create socket", e);
        } catch (UnknownHostException e) {
            throw new TransportException("failed to find host", e);
        }
    }
    
    private int nextIP = 0;
    private synchronized String getNextIP() throws TransportException {
        //TODO use a pluggable strategy
        
        // simple round robin
        if (domains.isEmpty()) {
            nextIP = 0;
            boolean started = maybeStartNewProcess(); 
            if (!started) {
                throw new TransportException("unable to start a new domain");
            }
        }

        String ipAddress = domains.get(nextIP++).getIPAddress();
        int nbDomains = domains.size();
        nextIP = (nbDomains == 0) ? 0 : (nextIP % nbDomains);
        return ipAddress;
    }
}
