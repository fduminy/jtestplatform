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

import java.io.File;
import java.io.IOException;

import org.jtestplatform.client.ConfigReader;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.Domain;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.Connection;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestLibVirt {
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
    
    public void tearDown() throws IOException, ConfigurationException {
        if (domain != null) {
            domain.stop();
        }
    }
    
    @Test
    public void testStart() throws ConfigurationException, IOException {
        domain = factory.createDomain(createDomainConfig(), connection);
        domain.start();
    }
    
    @Test
    public void testStop() throws ConfigurationException, IOException {
        domain = factory.createDomain(createDomainConfig(), connection);
        domain.start();
        domain.stop();
    }

    /**
     * @return
     */
    private DomainConfig createDomainConfig() {
        DomainConfig cfg = new DomainConfig();
        cfg.setVmName("test");
        cfg.setCdrom(new File(config.getWorkDir()).getParent() + File.separatorChar + "config" + File.separatorChar + "microcore_2.7.iso");
        return cfg;
    }
}
