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
/**
 * 
 */
package org.jtestplatform.client.domain.libvirt;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.Domain;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.configuration.Connection;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.DomainInfo.DomainState;

/**
 * Implementation of {@link ServerProcess} for <a href="http://www.virtualbox.org/">VirtualBox</a>
 * 
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
class LibVirtDomain implements Domain {
    private static final Logger LOGGER = Logger.getLogger(LibVirtDomain.class);
    
    /**
     * Configuration of machine to run with libvirt.
     */
    private final DomainConfig config;

    private final LibVirtDomainFactory factory;
    private final Connection connection;
    
    private org.libvirt.Domain domain;
    private Connect connect;
    
    private String ipAddress;
    
    /**
     * 
     * @param config configuration of the machine to run with libvirt.
     * @param connect1
     * @throws ConfigurationException 
     */
    LibVirtDomain(DomainConfig config, LibVirtDomainFactory factory, Connection connection) {
        this.config = config;        
        this.factory = factory;
        this.connection = connection;
    }
        
    /**
     * {@inheritDoc}
     * @throws ConfigurationException 
     */
    @Override
    public synchronized String start() throws ConfigurationException {
        try {
            if (connect == null) {
                if ((connection.getUri() == null) || connection.getUri().trim().isEmpty()) {
                    throw new ConfigurationException("connection's URI not specified");
                }
                
                connect = new Connect(connection.getUri(), false);
                factory.ensureNetworkExist(connect);
            }
                            
            if (!isAlive()) {
                ipAddress = null;
                domain = factory.defineDomain(connect, config);
                if (!isAlive()) {
                    ipAddress = factory.start(domain);
                }
            }
        } catch (LibvirtException lve) {
            throw new ConfigurationException("failed to start", lve);
        } catch (IOException e) {
            throw new ConfigurationException("failed to start", e);
        }
        
        return ipAddress;
    }
    
    /**
     * {@inheritDoc}
     * @throws ConfigurationException 
     */
    @Override
    public synchronized void stop() throws IOException, ConfigurationException {
        if (isAlive()) {
            factory.stop(domain, ipAddress);
            domain = null;
            ipAddress = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (connect != null) {
            try {
                connect.close();
            } catch (LibvirtException e) {
                LOGGER.error(e);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlive() throws IOException {
        boolean isRunning = false;
        
        if (domain != null) {
            try {
                DomainState state = domain.getInfo().state;
                isRunning = DomainState.VIR_DOMAIN_RUNNING.equals(state);
            } catch(Exception e) {
                throw new IOException(e);
            }
        }
        
        return isRunning;
    }
    
    public String getIPAddress() {
        return ipAddress;
    }
}
