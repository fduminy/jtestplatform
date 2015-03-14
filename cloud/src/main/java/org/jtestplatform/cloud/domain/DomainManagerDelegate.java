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
/**
 * 
 */
package org.jtestplatform.cloud.domain;

import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;

import java.util.List;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
class DomainManagerDelegate {
    private final DomainFactory<? extends Domain> domainFactory;
    private final LoadBalancer<Connection> connections;
    
    public DomainManagerDelegate(DomainFactory<? extends Domain> domainFactory,
            List<Connection> connections) {
        this.domainFactory = domainFactory;
        this.connections = new LoadBalancer<Connection>(connections);
    }

    public Connection getConnectionFor(Platform platform) throws DomainException {
        Connection result = null;
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.getNext();
            
            //TODO think about caching the result of support(platform, connection) (to avoid potentially remote connection)
            if (domainFactory.support(platform, connection)) {
                result = connection;
                break;
            }
        }
        return result;
    }
    
    public Domain createDomain(DomainConfig config, Connection connection) throws DomainException {
        return domainFactory.createDomain(config, connection);
    }    
}
