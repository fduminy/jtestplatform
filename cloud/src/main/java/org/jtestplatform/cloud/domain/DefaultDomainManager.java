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
package org.jtestplatform.cloud.domain;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.RealClock;
import com.google.code.tempusfugit.temporal.Sleeper;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import org.dom4j.DocumentException;
import org.jtestplatform.cloud.configuration.Configuration;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Factory;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.configuration.io.dom4j.ConfigurationDom4jReader;
import org.jtestplatform.cloud.domain.libvirt.LibVirtDomainFactory;
import org.jtestplatform.cloud.domain.watchdog.DefaultWatchDogStrategy;
import org.jtestplatform.cloud.domain.watchdog.WatchDog;
import org.jtestplatform.cloud.domain.watchdog.WatchDogListener;
import org.jtestplatform.cloud.domain.watchdog.WatchDogStrategy;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.UDPTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static com.google.code.tempusfugit.temporal.Duration.seconds;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class DefaultDomainManager implements DomainManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDomainManager.class);
    private static final Duration MAX_ZOMBIE_DURATION = seconds(10);

    private final Configuration config;
    private final LoadBalancer<DomainManagerDelegate> delegates;
    private final WatchDog watchDog;
    private final LoadBalancer<Domain> domains;
    private final int maxNumberOfDomains;
    private final int serverPort;

    public DefaultDomainManager(Reader configReader) throws ConfigurationException {
        config = read(configReader);
        Map<String, DomainFactory<? extends Domain>> knownFactories = findKnownFactories();
        checkValid(knownFactories, config);

        maxNumberOfDomains = Math.max(1, config.getDomains().getMax());
        serverPort = config.getServerPort();
        watchDog = createWatchDog();
        domains = new LoadBalancer<Domain>();

        List<DomainManagerDelegate> dmDelegates = new ArrayList<DomainManagerDelegate>();
        for (Factory factory : config.getDomains().getFactories()) {
            DomainFactory<? extends Domain> domainFactory = knownFactories.get(factory.getType());
            DomainManagerDelegate data = new DomainManagerDelegate(domainFactory, factory.getConnections());
            dmDelegates.add(data);
        }

        delegates = new LoadBalancer<DomainManagerDelegate>(dmDelegates);
    }

    private WatchDog createWatchDog() {
        WatchDogStrategy strategy = new DefaultWatchDogStrategy(MAX_ZOMBIE_DURATION);
        Sleeper sleeper = new ThreadSleep(millis(config.getWatchDogPollInterval()));
        WatchDog watchDog = new WatchDog(sleeper, strategy, RealClock.now());
        watchDog.addWatchDogListener(new WatchDogListener() {
            @Override
            public void domainDied(Domain domain) {
                DefaultDomainManager.this.domainDied(domain);
            }
        });
        return watchDog;
    }

    @Override
    public List<Platform> getPlatforms() {
        // TODO maybe we shouldn't expose our list of platforms 
        return config.getPlatforms();
    }

    private DomainConfig createDomainConfig(Platform platform) {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomainName(null); // null => will be defined automatically
        domainConfig.setPlatform(platform);
        return domainConfig;
    }

    private void domainDied(Domain domain) {
        domains.remove(domain);
    }

    @Override
    public void start() {
        watchDog.startWatching();
    }

    @Override
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
     *
     * @throws TransportException
     */
    @Override
    public Transport get(Platform platform) throws TransportException {
        //TODO put the connection/transport in cache and remove it when the domain stop/die
        String host = getNextIP(platform);

        return createTransport(host, serverPort, config.getTimeout());
    }

    protected Transport createTransport(String host, int port, int timeout) throws TransportException {
        try {
            InetAddress address = InetAddress.getByName(host);
            return createTransport(address, port, timeout);
        } catch (UnknownHostException e) {
            throw new TransportException("failed to find host", e);
        }
    }

    protected Transport createTransport(InetAddress address, int port, int timeout) throws TransportException {
        return new UDPTransport(address, port, timeout);
    }

    private synchronized String getNextIP(Platform platform) throws TransportException {
        if (domains.size() < maxNumberOfDomains) {
            // create a domain if there is less than maximum
            // (it includes the case where there is no domain running)

            try {
                DomainManagerDelegate delegate = null;
                Connection connection = null;
                for (int i = 0; i < delegates.size(); i++) {
                    DomainManagerDelegate d = delegates.getNext();
                    connection = d.getConnectionFor(platform);
                    if (connection != null) {
                        delegate = d;
                        break;
                    }
                }

                if (connection == null) {
                    throw new TransportException("That platform is not supported :\n" + platform);
                }

                DomainConfig domainConfig = createDomainConfig(platform);
                Domain domain = delegate.createDomain(domainConfig, connection);
                domains.add(domain);
                watchDog.watch(domain);
            } catch (DomainException ce) {
                throw new TransportException("unable to create a new domain", ce);
            }
        }

        // TODO ensure that the ip address correspond to a domain with the appropriate platform
        return domains.getNext().getIPAddress();
    }

    private void checkValid(Map<String, DomainFactory<? extends Domain>> knownFactories, Configuration config) throws ConfigurationException {
        if ((knownFactories == null) || knownFactories.isEmpty()) {
            throw new ConfigurationException("No known factory");
        }
        if (config.getDomains() == null) {
            throw new ConfigurationException("domains is not defined");
        }
        if ((config.getDomains().getFactories() == null) || config.getDomains().getFactories().isEmpty()) {
            throw new ConfigurationException("No factory has been defined");
        }

        StringBuilder wrongTypes = new StringBuilder();
        StringBuilder typesWithoutConnection = new StringBuilder();
        for (Factory factory : config.getDomains().getFactories()) {
            DomainFactory<? extends Domain> domainFactory = knownFactories.get(factory.getType());
            if (domainFactory == null) {
                appendErrorMessage("DomainFactory", wrongTypes, factory);
                continue;
            }

            if ((factory.getConnections() == null) || factory.getConnections().isEmpty()) {
                appendErrorMessage("connection", typesWithoutConnection, factory);
            }
        }
        if ((wrongTypes.length() != 0) || (typesWithoutConnection.length() != 0)) {
            StringBuilder message = new StringBuilder("Invalid configuration:\n");

            if (wrongTypes.length() != 0) {
                message.append("\tNo DomainFactory for type(s) ").append(wrongTypes).append('\n');
            }

            if (typesWithoutConnection.length() != 0) {
                message.append("\tNo connection for type(s) ").append(typesWithoutConnection);
            }

            throw new ConfigurationException(message.toString());
        }
    }

    private void appendErrorMessage(String missingObjectName, StringBuilder wrongTypes, Factory factory) {
        LOGGER.error("No {} for type {}", missingObjectName, factory.getType());

        if (wrongTypes.length() != 0) {
            wrongTypes.append(", ");
        }
        wrongTypes.append(factory.getType());
    }

    Configuration read(Reader reader) throws ConfigurationException {
        ConfigurationDom4jReader dom4jReader = new ConfigurationDom4jReader();
        Configuration config;
        try {
            config = dom4jReader.read(reader);
        } catch (IOException e) {
            throw new ConfigurationException("can't read config", e);
        } catch (DocumentException e) {
            throw new ConfigurationException("can't read config", e);
        }

        return config;
    }

    protected Map<String, DomainFactory<? extends Domain>> findKnownFactories() {
        //TODO get it from ServiceLoader
        Map<String, DomainFactory<? extends Domain>> result = new HashMap<String, DomainFactory<? extends Domain>>();
        LibVirtDomainFactory f = new LibVirtDomainFactory();
        result.put(f.getType(), f);
        return result;
    }

}
