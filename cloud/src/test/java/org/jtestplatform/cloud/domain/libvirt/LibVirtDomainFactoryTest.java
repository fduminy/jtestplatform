/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.cloud.domain.libvirt;

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

import static com.google.code.tempusfugit.temporal.Duration.minutes;
import static org.jtestplatform.cloud.domain.libvirt.IpAddressFinder.waitReachableOrTimeout;
import static org.jtestplatform.cloud.domain.libvirt.UniqueDomainNameFinder.DOMAIN_NAME_PREFIX;
import static org.junit.Assert.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class LibVirtDomainFactoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibVirtDomainFactoryTest.class);

    private Connection connection;

    private LibVirtDomainFactory factory;
    private Domain domain;

    @Before
    public void setUp() {
        factory = new LibVirtDomainFactory();
        connection = new Connection();
        connection.setUri("qemu:///system");
    }

    @After
    public void tearDown() throws IOException, DomainException {
        boolean error = false;
        if (domain != null) {
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

    @Test
    public void testStartAndStop() throws Throwable {
        int nbDomainsBeforeStart = nbDefinedDomains();

        try {
            //TODO also check createDomain/support methods work well together
            LOGGER.debug("creating domain");
            domain = factory.createDomain(createDomainConfig(), connection);

            LOGGER.debug("starting domain");
            domain.start();

            assertEquals(nbDomainsBeforeStart + 1, nbDefinedDomains());
        } catch (Exception e) {
            LOGGER.error("error in domain", e);
        } finally {
            if (domain != null) {
                LOGGER.debug("stopping domain");
                domain.stop();
            }
        }

        assertEquals(nbDomainsBeforeStart, nbDefinedDomains());
    }

    @Test
    public void testGetIPAddress() throws Throwable {
        try {
            domain = factory.createDomain(createDomainConfig(), connection);
            assertNull(domain.getIPAddress());

            domain.start();
            String ip = domain.getIPAddress();
            assertFalse("domain's ip must not be null, empty or blank", ConfigUtils.isBlank(ip));

            waitReachableOrTimeout(ip, minutes(1));
        } catch (Exception e) {
            LOGGER.error("error in domain", e);
        } finally {
            if (domain != null) {
                domain.stop();
            }
        }
    }

    private static int nbDefinedDomains() {
        File file = new File("/etc/libvirt/qemu");
        String[] files = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOMAIN_NAME_PREFIX);
            }
        });
        return (files == null) ? 0 : files.length;
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
}
