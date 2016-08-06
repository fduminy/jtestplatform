/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.cloud.domain.libvirt;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.Utils;
import org.jtestplatform.common.ConfigUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.code.tempusfugit.concurrency.ExecutorServiceShutdown.shutdown;
import static com.google.code.tempusfugit.temporal.Duration.minutes;
import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.libvirt.LibVirtDomainFactory.DOMAIN_NAME_PREFIX;
import static org.junit.Assert.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class LibVirtTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibVirtTest.class);

    private static final String ERROR_TAG = "ERROR: ";

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
        private final int domainId;

        public TestStartAndStop(int index) {
            domainId = index;
        }

        @Override
        public String call() throws Exception {
            Domain domain = null;
            String ip;
            try {
                //TODO also check createDomain/support methods work well together
                LOGGER.debug("creating domain #{}", domainId);
                domain = factory.createDomain(createDomainConfig(), connection);
                LOGGER.debug("domain #{} created", domainId);
                ip = domain.getIPAddress();
                assertNull(ip);

                domains.add(domain);

                // start
                LOGGER.debug("starting domain #{}", domainId);
                ip = domain.start();
                assertFalse("domain #" + domainId + "'s ip must not be null, empty or blank", ConfigUtils.isBlank(ip));

                waitOrTimeout(reachable(InetAddress.getByName(ip), seconds(1)), timeout(minutes(1)));
                assertEquals("domain #" + domainId + " start() must return same ip address as getIpAddress()", ip,
                             domain.getIPAddress());
                return ip;
            } catch (Exception e) {
                LOGGER.error("error in domain #" + domainId, e);
                return ERROR_TAG + e.getMessage();
            } finally {
                if (domain != null) {
                    // stop
                    LOGGER.debug("stopping domain #{}", domainId);
                    domain.stop();
                }
            }
        }
    }

    @Test
    public void testStartAndStop_1_domain() throws Throwable {
        testStartAndStop(1);
    }

    @Test
    public void testStartAndStop_3_domains() throws Throwable {
        testStartAndStop(3);
    }

    private void testStartAndStop(int nbDomains) throws Throwable {
        assertThat(nbDefinedDomains()).as("number of defined domains at begin").isEqualTo(0);

        List<TestStartAndStop> tasks = new ArrayList<TestStartAndStop>(nbDomains);
        for (int i = 0; i < nbDomains; i++) {
            tasks.add(new TestStartAndStop(i));
        }

        // run each test in its own thread
        ExecutorService executorService = Executors.newFixedThreadPool(nbDomains);

        // invokeAll() blocks until all tasks have run...
        List<Future<String>> ipAddresses = executorService.invokeAll(tasks);
        assertThat(ipAddresses).hasSize(nbDomains);

        shutdown(executorService);
        StringBuilder errorMessage = new StringBuilder();
        Set<String> uniqueIpAddresses = new HashSet<String>();
        int i = 0;
        for (Future<String> ipFuture : ipAddresses) {
            if (ipFuture.get().startsWith(ERROR_TAG)) {
                errorMessage.append("domain[").append(i++).append("]: ").append(ipFuture.get()).append('\n');
            } else {
                uniqueIpAddresses.add(ipFuture.get());
            }
        }
        if (errorMessage.length() > 0) {
            fail(errorMessage.toString());
        }

        assertThat(uniqueIpAddresses).hasSize(nbDomains);
        assertThat(nbDefinedDomains()).as("number of defined domains at end").isEqualTo(0);
    }

    private static int nbDefinedDomains() {
        File file = new File("/etc/libvirt/qemu");
        String[] files = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOMAIN_NAME_PREFIX);
            }
        });
        return files.length;
    }

    private static DomainConfig createDomainConfig() {
        Platform platform = new Platform();
        platform.setMemory(32L * 1024L);
        platform.setCdrom(Utils.getCDROM());

        DomainConfig cfg = new DomainConfig();
        cfg.setPlatform(platform);
        cfg.setDomainName(null); // null => will be defined automatically

        return cfg;
    }

    private static Condition reachable(final InetAddress address, final Duration timeOut) {
        return new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return address.isReachable((int) timeOut.inMillis());
                } catch (IOException e) {
                    return false;
                }
            }
        };
    }
}
