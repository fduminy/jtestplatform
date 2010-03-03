package org.jtestplatform.client.domain;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jtestplatform.client.ConfigReader;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.Connection;
import org.jtestplatform.configuration.Domains;
import org.jtestplatform.configuration.Factory;
import org.jtestplatform.configuration.Platform;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestDomainManager {
    @Before
    public void setUp() throws ConfigurationException {
        new ConfigReader().read(); // force init of log4j        
    }
    
    @Test
    public void testConstructorWithoutDomains() {
        try {
            Configuration config = new Configuration();
            
            createDomainManager(config, true);
            fail("must throw an exception");
        } catch (ConfigurationException ce) {
            // success
        }
    }

    @Test
    public void testConstructorWithoutKnownFactory() {
        try {
            Configuration config = new Configuration();
            
            createDomainManager(config, false);
            fail("must throw an exception");
        } catch (ConfigurationException ce) {
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
        } catch (ConfigurationException ce) {
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
        } catch (ConfigurationException ce) {
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
        } catch (ConfigurationException ce) {
            // success
        }
    }
    
    @Test
    public void testGetTransport() throws TransportException, ConfigurationException {
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
        Transport transport = domainManager.get();
        assertNotNull(transport);
    }
    
    private DomainManager createDomainManager(Configuration config, boolean withKnownFactories) throws ConfigurationException {        
        Platform platform = new Platform();
        
        Map<String, DomainFactory<? extends Domain>> knownFactories = new HashMap<String, DomainFactory<? extends Domain>>();
        if (withKnownFactories) {
            knownFactories.put(CustomDomainFactory.TYPE, new CustomDomainFactory());
        }
        
        return new DomainManager(config, platform, knownFactories);                    
    }
    
    private static class CustomDomainFactory implements DomainFactory<Domain> {
        private static final String TYPE = "test";
        @Override
        public String getType() {
            return TYPE;
        }

        @Override
        public Domain createDomain(DomainConfig config, Connection connection)
            throws ConfigurationException {
            try {
                return DomainUtils.createFixedStateProcesses(Boolean.TRUE, null, 1)[0];
            } catch (IOException e) {
                throw new ConfigurationException("failed to create a domain", e);
            }
        }        
    }
}
