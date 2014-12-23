/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2011  Fabien DUMINY (fduminy at jnode dot org)
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

import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.common.ConfigUtils;
import org.libvirt.Connect;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

/**
 * Implementation for a {@link org.jtestplatform.cloud.domain.Domain} based on <a href="http://www.libvirt.org/">libvirt</a>.
 * 
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
class LibVirtDomain implements Domain {
    
    /**
     * Configuration of machine to run with libvirt.
     */
    private final DomainConfig config;

    private final LibVirtDomainFactory factory;
    private final Connection connection;
    
    private org.libvirt.Domain domain;
    
    private String ipAddress;
    
    /**
     * 
     * @param config configuration of the machine to run with libvirt.
     * @param connect1
     * @throws DomainException 
     */
    LibVirtDomain(DomainConfig config, LibVirtDomainFactory factory, Connection connection) throws DomainException {
        this.config = config;        
        this.factory = factory;
        this.connection = connection;
        
        if (ConfigUtils.isBlank(connection.getUri())) {
            throw new DomainException("connection's URI not specified");
        }
    }
        
    /**
     * {@inheritDoc}
     * @throws DomainException 
     */
    @Override
    public synchronized String start() throws DomainException {
        return factory.execute(connection, new ConnectManager.Command<String>() {
            @Override
            public String execute(Connect connect) throws Exception {
            factory.ensureNetworkExist(connect);
                            
            if (!isAlive()) {
                ipAddress = null;
                if (domain != null) {
                    domain.free();
                }
                domain = factory.defineDomain(connect, config);
                if (!isAlive()) {
                    ipAddress = factory.start(domain);
                }
            }
        return ipAddress;
    }
        });
    }

    /**
     * {@inheritDoc}
     * @throws DomainException 
     */
    @Override
    public synchronized void stop() throws DomainException {
        if (isAlive()) {
            factory.stop(domain, ipAddress);
            try {
                domain.free();
                closeConnection();
            } catch (LibvirtException e) {
                throw new DomainException(e);
            }
            domain = null;
            ipAddress = null;
        }
    }

    @Override
    protected void finalize() throws LibvirtException {
        closeConnection();
    }

    protected void closeConnection() throws LibvirtException {
        factory.releaseConnect(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlive() throws DomainException {
        boolean isRunning = false;
        
        if (domain != null) {
            try {
                DomainState state = domain.getInfo().state;
                isRunning = DomainState.VIR_DOMAIN_RUNNING.equals(state);
            } catch(LibvirtException e) {
                throw new DomainException(e);
            }
        }
        
        return isRunning;
    }
    
    public String getIPAddress() {
        return ipAddress;
    }
}
