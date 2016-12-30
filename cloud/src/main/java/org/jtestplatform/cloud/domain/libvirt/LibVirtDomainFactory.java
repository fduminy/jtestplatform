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
/**
 *
 */
package org.jtestplatform.cloud.domain.libvirt;

import com.sun.jna.Pointer;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainFactory;
import org.jtestplatform.cloud.domain.libvirt.ConnectManager.Command;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.jna.Libvirt.VirErrorCallback;
import org.libvirt.jna.virError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DomainFactory} for <a href="http://libvirt.org/">libvirt</a>.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class LibVirtDomainFactory implements DomainFactory<LibVirtDomain> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibVirtDomainFactory.class);

    private static final String NETWORK_NAME = "jtestplatform-network";

    private static final String BASE_MAC_ADDRESS = "54:52:00:77:58:";
    private static final String BASE_IP_ADDRESS = "192.168.121.";
    private static final int MIN_SUBNET_IP_ADDRESS = 2;
    private static final int MAX_SUBNET_IP_ADDRESS = 254;

    private final ConnectManager connectManager = new ConnectManager();

    private final NetworkXMLBuilder networkXMLBuilder = new NetworkXMLBuilder();
    private final NetworkBuilder networkBuilder = new NetworkBuilder(networkXMLBuilder);

    private final DomainXMLBuilder domainXMLBuilder = new DomainXMLBuilder();
    private final PlatformSupportManager supportManager = new PlatformSupportManager();

    private NetworkConfig networkConfig;
    private DomainCache domainCache;
    private DomainBuilder domainBuilder;

    static {
        try {
            Connect.setErrorCallback(new VirErrorCallback() {
                @Override
                public void errorCallback(Pointer pointer, virError error) {
                    LOGGER.error("pointer={} error={}", pointer, error);
                }
            });
        } catch (LibvirtException e) {
            LOGGER.error("failed to initialize error callback", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "libvirt";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean support(final Platform platform, final Connection connection) throws DomainException {
        return execute(connection, new Command<Boolean>() {
            @Override
            public Boolean execute(Connect connect) throws Exception {
                return supportManager.support(platform, connect);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public LibVirtDomain createDomain(DomainConfig config, Connection connection) throws DomainException {
        if (!support(config.getPlatform(), connection)) {
            throw new DomainException("Unsupported platform :\n" + config.getPlatform()
                                      + "\n. You should call support(Platform, Connection) before.");
        }
        if (networkConfig == null) {
            networkConfig = createNetwork(connection);
            domainCache = new DomainCache(networkConfig);
            domainBuilder = new DomainBuilder(domainXMLBuilder, domainCache);
        }
        return new LibVirtDomain(config, this, connection, domainBuilder, networkConfig);
    }

    private NetworkConfig createNetwork(Connection connection) throws DomainException {
        final NetworkConfig config = new NetworkConfig(NETWORK_NAME, BASE_MAC_ADDRESS, BASE_IP_ADDRESS,
                                                       MIN_SUBNET_IP_ADDRESS, MAX_SUBNET_IP_ADDRESS);
        execute(connection, new Command<Object>() {
            @Override
            public Object execute(Connect connect) throws Exception {
                Network net = networkBuilder.build(connect, config);
                net.setAutostart(true);
                if (net.isActive() == 0) {
                    net.create();
                }
                return null;
            }
        });
        return config;
    }

    final <T> T execute(org.jtestplatform.cloud.configuration.Connection connection, Command<T> command)
        throws DomainException {
        return connectManager.execute(connection, command);
    }

    void releaseConnect(Connection connection) {
        connectManager.releaseConnect(connection);
    }
}
