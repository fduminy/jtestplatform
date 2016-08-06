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
/**
 *
 */
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.domain.DomainException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
class ConnectManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectManager.class);

    private final Map<org.jtestplatform.cloud.configuration.Connection, ConnectData> connections =
        new Hashtable<org.jtestplatform.cloud.configuration.Connection, ConnectData>();

    ConnectManager() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                org.jtestplatform.cloud.configuration.Connection[] connections = ConnectManager.this.connections
                    .keySet().toArray(
                        new org.jtestplatform.cloud.configuration.Connection[ConnectManager.this.connections.size()]);
                if (connections.length > 0) {
                    LOGGER.warn("There are {} unclosed connections", connections.length);
                }

                for (org.jtestplatform.cloud.configuration.Connection connection : connections) {
                    closeConnect(connection);
                }
            }
        });
    }

    final <T> T execute(org.jtestplatform.cloud.configuration.Connection connection, Command<T> command)
        throws DomainException {
        try {
            return command.execute(getConnect(connection));
        } catch (Exception e) {
            throw new DomainException(e);
        } finally {
            releaseConnect(connection);
        }
    }

    static interface Command<T> {
        T execute(Connect connect) throws Exception;
    }

    private Connect getConnect(org.jtestplatform.cloud.configuration.Connection connection) throws DomainException {
        synchronized (getLock(connection)) {
            ConnectData connectData = connections.get(connection);
            if (connectData == null) {
                try {
                    Connect connect = new Connect(connection.getUri(), false);
                    connectData = new ConnectData(connect);
                } catch (LibvirtException e) {
                    throw new DomainException(e);
                }
                connections.put(connection, connectData);
            }
            connectData.incrementReferenceCounter();
            return connectData.getConnect();
        }
    }

    void releaseConnect(org.jtestplatform.cloud.configuration.Connection connection) {
        synchronized (getLock(connection)) {
            ConnectData connectData = connections.get(connection);
            if (connectData != null) {
                int counter = connectData.decrementReferenceCounter();
                if (counter == 0) {
                    closeConnect(connection);
                }
            }
        }
    }

    private void closeConnect(org.jtestplatform.cloud.configuration.Connection connection) {
        ConnectData connectData = connections.remove(connection);
        if (connectData != null) {
            try {
                LOGGER.info("closing connection to {}", connection.getUri());
                if (connectData.getReferenceCounter() > 0) {
                    LOGGER.warn("The connection to {} has {} unreleased references", connection.getUri(),
                                connectData.getReferenceCounter());
                }
                // note : return -1 on error, or >= 0 on success where the value is the number of references to the connection
                int result = connectData.getConnect().close();
                LOGGER.debug("Connect.close() returned {}", result);
                LOGGER.info("closed connection to {}", connection.getUri());
            } catch (LibvirtException e) {
                LOGGER.error("failed to close connection to " + connection.getUri(), e);
            }
        }
    }

    private static Object getLock(org.jtestplatform.cloud.configuration.Connection connection) {
        // use the interned String for the uri because it's warranted to be a unique reference
        // for a given String content.
        return connection.getUri().intern();
    }

    private static class ConnectData {
        private final Connect connect;
        private int referenceCounter;

        private ConnectData(Connect connect) {
            this.connect = connect;
        }

        public int getReferenceCounter() {
            return referenceCounter;
        }

        public Connect getConnect() {
            return connect;
        }

        public int incrementReferenceCounter() {
            return ++referenceCounter;
        }

        public int decrementReferenceCounter() {
            if (referenceCounter > 0) {
                --referenceCounter;
            } else {
                LOGGER.warn("decrementing counter already equals to zero");
            }

            return referenceCounter;
        }
    }
}
