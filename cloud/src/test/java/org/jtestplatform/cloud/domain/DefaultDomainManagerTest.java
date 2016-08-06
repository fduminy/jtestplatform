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
package org.jtestplatform.cloud.domain;

import org.dom4j.DocumentException;
import org.jtestplatform.cloud.configuration.*;
import org.jtestplatform.cloud.configuration.io.dom4j.ConfigurationDom4jReader;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.UDPTransport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.Utils.FixedState.ALWAYS_ALIVE;
import static org.junit.Assert.assertNotNull;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class DefaultDomainManagerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testReadConfigFile() throws IOException, DocumentException {
        // preparation
        ConfigurationDom4jReader dom4jReader = new ConfigurationDom4jReader();

        // test
        Configuration config = dom4jReader.read(new FileReader(Utils.getConfigFile()));

        // verifications
        assertThat(config).isNotNull();
        assertThat(config.getWatchDogPollInterval()).isEqualTo(30000);
        assertThat(config.getTimeout()).isEqualTo(12345);

        assertThat(config.getPlatforms()).hasSize(1);
        Platform platform = config.getPlatforms().get(0);
        assertThat(platform).isNotNull();
        assertThat(platform.getCpu()).isEqualTo("phenom");
        assertThat(platform.getNbCores()).isEqualTo(4);
        assertThat(platform.getWordSize()).isEqualTo(64);
        assertThat(platform.getMemory()).isEqualTo(524288);

        assertThat(config.getDomains()).isNotNull();
        assertThat(config.getDomains().getMax()).isEqualTo(2);
        assertThat(config.getDomains().getFactories()).hasSize(1);

        Factory factory = config.getDomains().getFactories().get(0);
        assertThat(factory).isNotNull();
        assertThat(factory.getType()).isEqualTo("libvirt");
        assertThat(factory.getConnections()).hasSize(1);
        assertThat(factory.getConnections().get(0)).isNotNull();
        assertThat(factory.getConnections().get(0).getUri()).isEqualTo("qemu:///system");
    }

    @Test
    public void testConstructor_withoutDomains() throws ConfigurationException {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("domains is not defined");

        Configuration config = new Configuration();

        createDomainManager(config, true, null);
    }

    @Test
    public void testConstructor_withoutKnownFactory() throws ConfigurationException {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("No known factory");

        Configuration config = new Configuration();

        createDomainManager(config, false, null);
    }

    @Test
    public void testConstructor_withoutFactory() throws ConfigurationException {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("No factory has been defined");

        Configuration config = new Configuration();
        config.setDomains(new Domains());

        createDomainManager(config, true, null);
    }

    @Test
    public void testConstructor_withWrongFactoryType() throws ConfigurationException {
        Domains domains = new Domains();
        Factory factory = new Factory();
        factory.setType("aWrongType");
        domains.addFactory(factory);
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("No DomainFactory for type(s) " + factory.getType());

        Configuration config = new Configuration();
        config.setDomains(domains);

        createDomainManager(config, true, null);
    }

    @Test
    public void testConstructor_withAFactoryWithoutConnection() throws ConfigurationException {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("No connection for type(s) " + CustomDomainFactory.TYPE);

        Domains domains = new Domains();
        Factory factory = new Factory();
        factory.setType(CustomDomainFactory.TYPE);
        domains.addFactory(factory);

        Configuration config = new Configuration();
        config.setDomains(domains);

        createDomainManager(config, true, null);
    }

    @Test
    public void testCreateUDPTransport() throws Exception {
        DefaultDomainManager domainManager = createDomainManager(createConfiguration(), true, null);

        Transport transport = domainManager.createTransport(InetAddress.getLocalHost(), 1234, -1);

        assertThat(transport).isExactlyInstanceOf(UDPTransport.class);
    }

    @Test
    public void testGetTransport_noTimeout() throws Exception {
        testGetTransport(0);
    }

    @Test
    public void testGetTransport_timeout() throws Exception {
        testGetTransport(10000);
    }

    private void testGetTransport(int timeout) throws Exception {
        List<DatagramSocket> sockets = new ArrayList<DatagramSocket>();
        final Configuration configuration = createConfiguration();
        if (timeout > 0) {
            configuration.setTimeout(timeout);
        }
        DomainManager domainManager = createDomainManager(configuration, true, sockets);

        UDPTransport transport = (UDPTransport) domainManager.get(new Platform());

        assertNotNull(transport);
        assertThat(sockets).hasSize(1);
        final DatagramSocket socket = sockets.get(0);
        assertThat(socket).isNotNull();
        assertThat(socket.getSoTimeout()).as("timeout").isEqualTo(timeout);
        assertThat(transport.getAddress()).as("address").isEqualTo(InetAddress.getLocalHost());
        assertThat(transport.getPort()).as("port").isEqualTo(configuration.getServerPort());
    }

    private Configuration createConfiguration() {
        Connection connection = new Connection();
        connection.setUri("anURI");

        Factory factory = new Factory();
        factory.setType(CustomDomainFactory.TYPE);
        factory.addConnection(connection);

        Domains domains = new Domains();
        domains.addFactory(factory);

        Configuration config = new Configuration();
        config.setDomains(domains);
        return config;
    }

    private DefaultDomainManager createDomainManager(final Configuration config, boolean withKnownFactories,
                                                     final List<DatagramSocket> sockets) throws ConfigurationException {
        final Map<String, DomainFactory<? extends Domain>> knownFactories = new HashMap<String, DomainFactory<? extends Domain>>();
        if (withKnownFactories) {
            knownFactories.put(CustomDomainFactory.TYPE, new CustomDomainFactory());
        }

        return new DefaultDomainManager(null) {
            @Override
            protected Map<String, DomainFactory<? extends Domain>> findKnownFactories() {
                return knownFactories;
            }

            @Override
            Configuration read(Reader reader) {
                return config;
            }

            @Override
            protected Transport createTransport(InetAddress address, int port, int timeout) throws TransportException {
                if (sockets == null) {
                    return super.createTransport(address, port, timeout);
                }

                return new UDPTransport(address, port, timeout) {
                    @Override
                    protected DatagramSocket createDatagramSocket() throws SocketException {
                        DatagramSocket socket = super.createDatagramSocket();
                        sockets.add(socket);
                        return socket;
                    }
                };
            }
        };
    }

    private static class CustomDomainFactory implements DomainFactory<Domain> {
        private static final String TYPE = "test";

        @Override
        public String getType() {
            return TYPE;
        }

        @Override
        public Domain createDomain(DomainConfig config, Connection connection)
            throws DomainException {
            try {
                return Utils.createFixedStateDomain(ALWAYS_ALIVE, InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                throw new DomainException(e);
            }
        }

        @Override
        public boolean support(Platform platform, Connection connection) {
            return true;
        }
    }
}
