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

import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assume.assumeTrue;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class DomainXMLBuilderTest extends AbstractXMLBuilderTest {
    @Test
    public void build_domain_32bits() throws DomainException {
        build("domain1", "cdrom1", "disk1", 12, 34, "12:34", "network1", 32);
    }

    @Test
    public void build_domain_64bits() throws DomainException {
        build("domain2", "cdrom2", "disk2", 56, 78, "56:78", "network2", 64);
    }

    @Theory
    public void build_domain_nullDrive(boolean nullCdrom, boolean nullDisk) throws DomainException {
        assumeTrue(nullDisk || nullCdrom);
        build("domain2", nullCdrom ? null : "cdrom2", nullDisk ? null : "disk2", 56, 78, "56:78", "network2", 64);
    }

    private void build(String domainName, String cdrom, String disk, long memory, int nbCores, String macAddress,
                       String networkName, int wordSize)
        throws DomainException {
        DomainConfig config = new DomainConfig();
        Platform platform = new Platform();
        platform.setMemory(memory);
        platform.setNbCores(nbCores);
        platform.setCdrom(cdrom);
        platform.setDisk(disk);
        platform.setWordSize(wordSize);
        config.setPlatform(platform);
        config.setDomainName(domainName);
        DomainXMLBuilder builder = new DomainXMLBuilder();

        String actualXML = builder.build(config, macAddress, networkName);

        assertXMLContains(actualXML, domainName, Long.toString(memory), Integer.toString(nbCores),
                          macAddress, networkName, (wordSize == 32) ? "i686" : "x86_64");
        assertXMLPart(actualXML, "device='disk'", disk);
        assertXMLPart(actualXML, "device='cdrom'", cdrom);
    }
}