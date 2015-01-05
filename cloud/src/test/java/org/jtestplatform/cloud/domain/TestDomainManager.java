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

import org.dom4j.DocumentException;
import org.jtestplatform.cloud.configuration.*;
import org.jtestplatform.cloud.configuration.io.dom4j.ConfigurationDom4jReader;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestDomainManager {
    @Test
    public void testReadConfigFile() throws FileNotFoundException, IOException, DocumentException {
        ConfigurationDom4jReader dom4jReader = new ConfigurationDom4jReader();            
        Configuration config = dom4jReader.read(new FileReader(DomainUtils.getConfigFile()));
        assertNotNull(config);
    }
    
    @Test
    public void testConstructorWithoutDomains() {
        try {
            Configuration config = new Configuration();
            
            createDomainManager(config, true);
            fail("must throw an exception");
        } catch (DomainException ce) {
            // success
        }
    }

    @Test
    public void testConstructorWithoutKnownFactory() {
        try {
            Configuration config = new Configuration();
            
            createDomainManager(config, false);
            fail("must throw an exception");
        } catch (DomainException ce) {
            // success
        }
    }

    @Test
    public void testConstructorWithoutFactory() {
        try {
            Configuration config = new Configuration();
            config.setDomains(new Domains());
            
            createDomainManager(config, true);
            fail("must throw an exception");
        } catch (DomainException ce) {
            // success
        }
    }

    @Test
    public void testConstructorWithWrongFactoryType() {
        try {
            Domains domains = new Domains();
            Factory factory = new Factory();
            factory.setType("aWrongType");
            domains.addFactory(factory);
            
            Configuration config = new Configuration();
            config.setDomains(domains);
            
            createDomainManager(config, true);
            fail("must throw an exception");
        } catch (DomainException ce) {
            // success
        }
    }
    
    @Test
    public void testConstructorWithAFactoryWithoutConnection() {
        try {
            Domains domains = new Domains();
            Factory factory = new Factory();
            factory.setType(CustomDomainFactory.TYPE);
            domains.addFactory(factory);
            
            Configuration config = new Configuration();
            config.setDomains(domains);
            
            createDomainManager(config, true);
            fail("must throw an exception");
        } catch (DomainException ce) {
            // success
        }
    }
    
    @Test
    public void testGetTransport() throws TransportException, DomainException {
        Connection connection = new Connection();
        connection.setUri("anURI");
        
        Factory factory = new Factory();
        factory.setType(CustomDomainFactory.TYPE);
        factory.addConnection(connection);
        
        Domains domains = new Domains();
        domains.addFactory(factory);
        
        Configuration config = new Configuration();
        config.setDomains(domains);
        
        DomainManager domainManager = createDomainManager(config, true);
        Platform platform = new Platform();
        Transport transport = domainManager.get(platform);
        assertNotNull(transport);
    }
    
    private DomainManager createDomainManager(final Configuration config, boolean withKnownFactories) throws DomainException {        
        final Map<String, DomainFactory<? extends Domain>> knownFactories = new HashMap<String, DomainFactory<? extends Domain>>();
        if (withKnownFactories) {
            knownFactories.put(CustomDomainFactory.TYPE, new CustomDomainFactory());
        }

        return new DefaultDomainManager(null) {
            @Override
            Map<String, DomainFactory<? extends Domain>> findKnownFactories() {
                return knownFactories;
            }
            
            @Override
            Configuration read(Reader reader) throws DomainException {
                return config;
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
            return DomainUtils.createFixedStateProcesses(Boolean.TRUE, null, 1)[0];
        }

        @Override
        public boolean support(Platform platform, Connection connection) {
            return true;
        }        
    }
}
