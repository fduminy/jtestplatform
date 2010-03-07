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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

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
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TestLibVirt {
    private static final Logger LOGGER = Logger.getLogger(TestLibVirt.class);
    
    private static final int NB_PINGS = 5;

    
    @DataPoint
    public static final Integer NB_DOMAINS1 = Integer.valueOf(1);
    @DataPoint
    public static final Integer NB_DOMAINS3 = Integer.valueOf(15);
    
    private Connection connection;
    
    private LibVirtDomainFactory factory;
    private List<Domain> domains;
    private Configuration config;
    
    @Before
    public void setUp() throws ConfigurationException {
        config = new ConfigReader().read(); // will initialize log4j
        
        factory = new LibVirtDomainFactory();
        connection = config.getDomains().getFactories().get(0).getConnections().get(0);
        
        // must be a synchronized List since we run multiple threads
        domains = new Vector<Domain>(); 
    }
    
    @After
    public void tearDown() throws IOException, ConfigurationException {
        boolean error = false;
        for (Domain domain : domains) {
            try {
                domain.stop();
            } catch (Throwable t) {
                LOGGER.error(t);
                error = true;
            }
        }
        if (error) {
            throw new RuntimeException("at least one error happened. look at logs");
        }
    }
    
    private class TestStartAndStop extends TestRunnable {
        private final int estimatedBootTime;
        
        public TestStartAndStop(int estimatedBootTime) {
            this.estimatedBootTime = estimatedBootTime;
        }
        
        @Override
        public void runTest() throws Throwable {
            try {
                Domain domain = factory.createDomain(createDomainConfig(), connection);
                String ip = domain.getIPAddress();
                org.junit.Assert.assertNull(ip);
                
                domains.add(domain);
                
                // start
                ip = domain.start();                
                sleep(estimatedBootTime); // wait a bit that the system has started
                
                org.junit.Assert.assertFalse("ip must not be null, empty or blank", ConfigUtils.isBlank(ip));
                org.junit.Assert.assertEquals("domain must be pingable", NB_PINGS, ping(ip));
                org.junit.Assert.assertEquals("domain.start() must return same ip address as domain.getIpAddress()", ip, domain.getIPAddress());
    
                // stop
                domain.stop();
                org.junit.Assert.assertNull(domain.getIPAddress());
                org.junit.Assert.assertEquals("after stop, domain must not be pingable", 0, ping(ip));
            } catch (Throwable t) {
                LOGGER.error(t);
                throw t;
            }
        }
    }
    
    @Theory
    public void testStartAndStop(Integer nbDomains) throws Throwable {
        final int estimatedBootTime = 40000;
        TestStartAndStop[] tests = new TestStartAndStop[nbDomains];
        for (int i = 0; i < nbDomains; i++) {
            tests[i] = new TestStartAndStop(estimatedBootTime);
        }
        
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(tests);

        // kickstarts the MTTR & fires off threads
        mttr.runTestRunnables();
    }

    private void sleep(long timeMillis) {
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    /**
     * @return
     */
    private DomainConfig createDomainConfig() {
        DomainConfig cfg = new DomainConfig();
        cfg.setDomainName(null); // null => will be defined automatically
        cfg.setMemory(32L * 1024L);
        cfg.setCdrom(new File(config.getWorkDir()).getParent() + File.separatorChar + "config" + File.separatorChar + "microcore_2.7.iso");
        return cfg;
    }
    
    private static int ping(String host) throws UnknownHostException, IOException {
        LOGGER.debug("pinging " + host);
        
        final int timeOut = 600000;
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
