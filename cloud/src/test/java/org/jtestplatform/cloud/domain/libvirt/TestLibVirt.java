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
 * -
 */
/**
 * 
 */
package org.jtestplatform.cloud.domain.libvirt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.apache.log4j.Logger;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainUtils;
import org.jtestplatform.common.ConfigUtils;
import org.jtestplatform.cloud.configuration.Configuration;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
    public static final Integer _1_DOMAIN = Integer.valueOf(1);
    @DataPoint
    public static final Integer _10_DOMAINS = Integer.valueOf(10);
    
    private Connection connection;
    
    private LibVirtDomainFactory factory;
    private List<Domain> domains;
    private List<String> ipList;
    
    @Before
    public void setUp() {
        DomainUtils.initLog4j();
        
        factory = new LibVirtDomainFactory();
        connection = new Connection();
        connection.setUri("qemu:///system");
        
        // must be a synchronized List since we run multiple threads
        domains = new Vector<Domain>();
        ipList = new Vector<String>();
    }
    
    @After
    public void tearDown() throws IOException, DomainException {
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
        private final String name;
        private final int estimatedBootTime;
        
        public TestStartAndStop(int index, int estimatedBootTime) {
            this.name = "TestStartAndStop[" + index + ']';
            this.estimatedBootTime = estimatedBootTime;
        }
        
        @Override
        public void runTest() throws Throwable {
            try {
                //TODO also check createDomain/support methods work well together
                LOGGER.debug(name + ": creating domain");
                Domain domain = factory.createDomain(createDomainConfig(), connection);
                LOGGER.debug(name + ": domain created");
                String ip = domain.getIPAddress();
                org.junit.Assert.assertNull(ip);
                
                domains.add(domain);
                
                // start
                LOGGER.debug(name + ": starting domain");
                ip = domain.start();                
                sleep(estimatedBootTime); // wait a bit that the system has started
                
                org.junit.Assert.assertFalse("ip must not be null, empty or blank", ConfigUtils.isBlank(ip));
                org.junit.Assert.assertEquals("domain must be pingable", NB_PINGS, ping(ip));
                org.junit.Assert.assertEquals("domain.start() must return same ip address as domain.getIpAddress()", ip, domain.getIPAddress());
                ipList.add(ip);
                
                // stop
                LOGGER.debug(name + ": stopping domain");
                domain.stop();
                org.junit.Assert.assertNull(domain.getIPAddress());
                org.junit.Assert.assertEquals("after stop, domain must not be pingable", 0, ping(ip));
            } catch (Throwable t) {
                LOGGER.error("error in " + name, t);
                throw t;
            }
        }
    }
    
    @Theory
    public void testStartAndStop(Integer nbDomains) throws Throwable {
        final int estimatedBootTime = 40000;
        TestStartAndStop[] tests = new TestStartAndStop[nbDomains];
        for (int i = 0; i < nbDomains; i++) {
            tests[i] = new TestStartAndStop(i, estimatedBootTime);
        }

        // run each test in its own thread
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(tests);
        mttr.runTestRunnables(600000); // 10 minutes
        
        // now assert that the list of IPs is valid
        assertEquals("each domain must have an IP address", tests.length, ipList.size());
        for (int i = 0; i < ipList.size(); i++) {
            boolean unique = true;
            for (int j = 0; j < ipList.size(); j++) {
                if ((i != j) && ipList.get(i).equals(ipList.get(j))) {
                    unique = false;
                    break;
                }
            }
            assertTrue("each domain must have a unique IP address", unique);
        }
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
        Platform platform = new Platform();
        platform.setMemory(32L * 1024L);
        platform.setCdrom(DomainUtils.getCDROM());
        
        DomainConfig cfg = new DomainConfig();
        cfg.setPlatform(platform);
        cfg.setDomainName(null); // null => will be defined automatically
        
        return cfg;
    }
    
    private static int ping(String host) throws IOException {
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
