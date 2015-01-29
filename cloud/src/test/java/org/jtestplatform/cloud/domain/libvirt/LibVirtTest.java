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
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainUtils;
import org.jtestplatform.common.ConfigUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.jtestplatform.cloud.domain.libvirt.LibVirtDomainFactory.DOMAIN_NAME_PREFIX;
import static org.junit.Assert.*;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class LibVirtTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibVirtTest.class);

    private static final String ERROR_TAG = "ERROR: ";
    private static final int NB_PINGS = 5;

    
    @DataPoint
    public static final Integer _1_DOMAIN = 1;
    @DataPoint
    public static final Integer _3_DOMAINS = 3;
    
    private Connection connection;
    
    private LibVirtDomainFactory factory;
    private List<Domain> domains;

    @Before
    public void setUp() {
        factory = new LibVirtDomainFactory();
        connection = new Connection();
        connection.setUri("qemu:///system");
        
        // must be a synchronized List since we run multiple threads
        domains = new Vector<Domain>();
    }
    
    @After
    public void tearDown() throws IOException, DomainException {
        boolean error = false;
        for (Domain domain : domains) {
            try {
                domain.stop();
            } catch (Throwable t) {
                LOGGER.error("Error while stopping domain", t);
                error = true;
            }
        }
        if (error) {
            throw new RuntimeException("at least one error happened. look at logs");
        }
    }

    private class TestStartAndStop implements Callable<String> {
        private final String name;

        public TestStartAndStop(int index) {
            this.name = "TestStartAndStop[" + index + ']';
        }
        
        @Override
        public String call() throws Exception {
            Domain domain = null;
            String ip;
            try {
                //TODO also check createDomain/support methods work well together
                LOGGER.debug("{}: creating domain", name);
                domain = factory.createDomain(createDomainConfig(), connection);
                LOGGER.debug("{}: domain created", name);
                ip = domain.getIPAddress();
                assertNull(ip);
                
                domains.add(domain);
                
                // start
                LOGGER.debug("{}: starting domain", name);
                ip = domain.start();
                assertFalse("ip must not be null, empty or blank", ConfigUtils.isBlank(ip));

                assertEquals("domain must be pingable", NB_PINGS, ping(ip));
                assertEquals("domain.start() must return same ip address as domain.getIpAddress()", ip, domain.getIPAddress());
                return ip;
            } catch (Exception e) {
                LOGGER.error("error in " + name, e);
                return ERROR_TAG + e.getMessage();
            } finally {
                if (domain != null) {
                    // stop
                    LOGGER.debug("{}: stopping domain", name);
                    domain.stop();
                }
            }
        }
    }

    @Theory
    public void testStartAndStop(Integer nbDomains) throws Throwable {
        assertEquals("no domain must be defined at begin", 0, nbDefinedDomains());

        List<TestStartAndStop> tasks = new ArrayList<TestStartAndStop>(nbDomains);
        for (int i = 0; i < nbDomains; i++) {
            tasks.add(new TestStartAndStop(i));
        }

        // run each test in its own thread
        ExecutorService executorService = Executors.newFixedThreadPool(nbDomains);

        // invokeAll() blocks until all tasks have run...
        List<Future<String>> ipAddresses = executorService.invokeAll(tasks);
        assertThat(ipAddresses.size(), is(nbDomains));
        assertThat(new HashSet<Future<String>>(ipAddresses).size(), is(nbDomains));

        StringBuilder errorMessage = new StringBuilder();
        for (Future<String> ipFuture : ipAddresses) {
            if (ipFuture.get().startsWith(ERROR_TAG)) {
                errorMessage.append("domain[").append(0).append("]: ").append(ipFuture.get()).append('\n');
            }
        }
        if (errorMessage.length() > 0) {
            fail(errorMessage.toString());
        }

        assertEquals("no domain must be defined at end", 0, nbDefinedDomains());
    }

    private int nbDefinedDomains() {
        File file = new File("/etc/libvirt/qemu");
        String[] files = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOMAIN_NAME_PREFIX);
            }
        });
        return files.length;
    }

    private static void sleep(long timeMillis) {
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
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
        LOGGER.debug("pinging {}", host);

        final int timeOut = 1000;
        final InetAddress addr = InetAddress.getByName(host);

        LOGGER.info("waiting domain {} has started", host);
        long start = System.currentTimeMillis();
        boolean pong = false;
        while (((System.currentTimeMillis() - start) < 60000) && !pong) {
            pong = addr.isReachable(timeOut);
            sleep(500);
        }
        if (!pong) {
            LOGGER.info("domain {} NOT started", host);
            return 0;
        }

        LOGGER.info("domain {} started", host);

        int counter = 0;
        while (counter < NB_PINGS) {
            if (addr.isReachable(timeOut)) {
                counter++;
                LOGGER.debug("received pong from {}", host);
            }
        }

        return counter;
    }
}
