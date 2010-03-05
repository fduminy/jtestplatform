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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.jtestplatform.client.ConfigReader;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.Domain;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.common.ConfigUtils;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestLibVirt {
    private static final Logger LOGGER = Logger.getLogger(TestLibVirt.class);
    
    private static final int NB_PINGS = 5;
    
    private Connection connection;
    
    private LibVirtDomainFactory factory;
    private Domain domain;
    private Configuration config;
    
    @Before
    public void setUp() throws ConfigurationException {
        config = new ConfigReader().read(); // will initialize log4j
        
        factory = new LibVirtDomainFactory();
        connection = config.getDomains().getFactories().get(0).getConnections().get(0);
    }
    
    @After
    public void tearDown() throws IOException, ConfigurationException {
        if (domain != null) {
            domain.stop();
        }
    }
    
    @Test
    public void testStart() throws ConfigurationException, IOException {
        domain = factory.createDomain(createDomainConfig(), connection);
        start(domain);
    }
    
    @Test
    public void testStop() throws ConfigurationException, IOException {
        domain = factory.createDomain(createDomainConfig(), connection);
        String ip = start(domain);
        
        domain.stop();
        assertNull(domain.getIPAddress());
        assertEquals("after stop, domain must not be pingable", 0, ping(ip));
    }
    
    private String start(Domain domain) throws IOException, ConfigurationException {
        String ip = domain.getIPAddress();
        assertNull(ip);
        
        ip = domain.start();
        
        // wait a bit that the system has started
        try {
            Thread.sleep(40000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        assertFalse("ip must not be null, empty or blank", ConfigUtils.isBlank(ip));
        assertEquals("domain must be pingable", NB_PINGS, ping(ip));
        assertEquals("domain.start() must return same ip address as domain.getIpAddress()", ip, domain.getIPAddress());
        
        return ip;
    }

    /**
     * @return
     */
    private DomainConfig createDomainConfig() {
        DomainConfig cfg = new DomainConfig();
        cfg.setDomainName(null); // null => will be defined automatically
        cfg.setCdrom(new File(config.getWorkDir()).getParent() + File.separatorChar + "config" + File.separatorChar + "microcore_2.7.iso");
        return cfg;
    }
    
    private int ping(String host) throws UnknownHostException, IOException {
        LOGGER.debug("pinging " + host);
        
        final int timeOut = 1000;
        final InetAddress addr = InetAddress.getByName(host);
        
        int counter = 0;
        for (int i = 0; i < NB_PINGS; i++) {
            if (addr.isReachable(timeOut)) {
                counter++;
                LOGGER.debug("received pong from " + host);
            }
        }
        
        return counter;
    }    
}
