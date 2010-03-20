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
 */
/**
 * 
 */
package org.jtestplatform.client.domain.libvirt;

import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.DomainException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
class ConnectManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectManager.class);
    
    private static final Map<org.jtestplatform.configuration.Connection, ConnectData> CONNECTIONS =
            new Hashtable<org.jtestplatform.configuration.Connection, ConnectData>();
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                org.jtestplatform.configuration.Connection[] connections = CONNECTIONS.keySet().toArray(new org.jtestplatform.configuration.Connection[CONNECTIONS.size()]);
                if (connections.length > 0) {
                    LOGGER.warn("There are unclosed " + connections.length + " connections");
                }
                
                for (org.jtestplatform.configuration.Connection connection : connections) {
                    closeConnect(connection);
                }
            } 
        });
    }

    static Connect getConnect(org.jtestplatform.configuration.Connection connection) throws DomainException {
        synchronized (getLock(connection)) {
            ConnectData connectData = CONNECTIONS.get(connection);
            if (connectData == null) {
                try {
                    Connect connect = new Connect(connection.getUri(), false);
                    connectData = new ConnectData(connect);
                } catch (LibvirtException e) {
                    throw new DomainException(e);
                }
                CONNECTIONS.put(connection, connectData);
            }
            connectData.incrementReferenceCounter();
            return connectData.getConnect();
        }
    }
    
    static void releaseConnect(org.jtestplatform.configuration.Connection connection) {
        synchronized (getLock(connection)) {
            ConnectData connectData = CONNECTIONS.get(connection);
            if (connectData != null) {
                int counter = connectData.decrementReferenceCounter();
                if (counter == 0) {
                    closeConnect(connection);
                }
            }
        }
    }
    
    private static void closeConnect(org.jtestplatform.configuration.Connection connection) {
        ConnectData connectData = CONNECTIONS.remove(connection);
        if (connectData != null) {
            try {
                LOGGER.info("closing connection to " + connection.getUri());
                if (connectData.getReferenceCounter() > 0) {
                    LOGGER.warn("The connection to " + connection.getUri() + " has " + connectData.getReferenceCounter() + " unreleased references");
                }
                // note : return -1 on error, or >= 0 on success where the value is the number of references to the connection
                int result = connectData.getConnect().close();
                LOGGER.debug("Connect.close() returned " + result);
                LOGGER.info("closed connection to " + connection.getUri());
            } catch (LibvirtException e) {
                LOGGER.error("failed to close connection to " + connection.getUri(), e);
            }
        }
    }

    private static Object getLock(org.jtestplatform.configuration.Connection connection) {
        // use the interned String for the uri because it's warranted to be a unique reference
        // for a given String content.
        return connection.getUri().intern();
    }
    
    private ConnectManager() {        
    }
    
    private static class ConnectData {
        private final Connect connect;
        private int referenceCounter;
        
        private ConnectData(Connect connect) {
            this.connect = connect;
        }
        
        /**
         * @return
         */
        public int getReferenceCounter() {
            return referenceCounter;
        }

        /**
         * @return the connect
         */
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
