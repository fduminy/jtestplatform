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

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.libvirt.DomainXMLBuilder.BEGIN_TAG;
import static org.jtestplatform.cloud.domain.libvirt.DomainXMLBuilder.END_TAG;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class DomainXMLBuilderTest {
    @Test
    public void build_domain1() throws DomainException {
        build("domain1", "cdrom1", 12, 34, "12:34", "network1");
    }

    @Test
    public void build_domain2() throws DomainException {
        build("domain2", "cdrom2", 56, 78, "56:78", "network2");
    }

    private void build(String domainName, String cdrom, long memory, int nbCores, String macAddress, String networkName)
        throws DomainException {
        DomainConfig config = new DomainConfig();
        Platform platform = new Platform();
        platform.setMemory(memory);
        platform.setNbCores(nbCores);
        platform.setCdrom(cdrom);
        config.setPlatform(platform);
        config.setDomainName(domainName);
        DomainXMLBuilder builder = new DomainXMLBuilder();

        String actual = builder.build(config, macAddress, networkName);

        assertThat(actual).isNotNull().isNotEmpty().doesNotContain(Character.toString(BEGIN_TAG))
                          .doesNotContain(Character.toString(END_TAG));
        assertThat(actual).contains(macAddress).contains(networkName);
    }
}