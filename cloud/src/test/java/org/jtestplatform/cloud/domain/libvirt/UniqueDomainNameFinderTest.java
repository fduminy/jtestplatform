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

import org.jtestplatform.cloud.domain.DomainException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.util.ArrayList;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.libvirt.LibVirtDomainFactory.BASE_IP_ADDRESS;
import static org.jtestplatform.cloud.domain.libvirt.UniqueDomainNameFinder.DOMAIN_NAME_PREFIX;
import static org.jtestplatform.cloud.domain.libvirt.UniqueMacAddressFinderTest.BASE_MAC_ADDRESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class UniqueDomainNameFinderTest extends AbstractDomainTest {
    private static final NetworkConfig CONFIG = new NetworkConfig("test", BASE_MAC_ADDRESS, BASE_IP_ADDRESS, 0, 4);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void findUniqueDomainName_contiguousUsedValues() throws Exception {
        setUpDomainNames(0, 1, 2, 3);
        UniqueDomainNameFinder finder = new UniqueDomainNameFinder(CONFIG);

        String actual = finder.findUniqueDomainName(domains);

        assertThat(actual).isEqualTo(formatDomainName(4));
    }

    @Test
    public void findUniqueDomainName_nonContiguousUsedValues() throws Exception {
        setUpDomainNames(0, 1, 3, 4);
        UniqueDomainNameFinder finder = new UniqueDomainNameFinder(CONFIG);

        String actual = finder.findUniqueDomainName(domains);

        assertThat(actual).isEqualTo(formatDomainName(2));
    }

    @Test
    public void findUniqueDomainName_noFreeValues() throws Exception {
        domains = new ArrayList<Domain>();
        for (int i = CONFIG.getMinSubNetIpAddress(); i <= CONFIG.getMaxSubNetIpAddress(); i++) {
            Domain domain = mock(Domain.class);
            when(domain.getName()).thenReturn(formatDomainName(i));
            domains.add(domain);
        }
        UniqueDomainNameFinder finder = new UniqueDomainNameFinder(CONFIG);
        thrown.expect(DomainException.class);
        thrown.expectMessage("unable to find a unique domain name");

        finder.findUniqueDomainName(domains);
    }

    private void setUpDomainNames(int suffix1, int suffix2, int suffix3, int suffix4) throws LibvirtException {
        when(domain1.getName()).thenReturn(formatDomainName(suffix1));
        when(domain2.getName()).thenReturn(formatDomainName(suffix2));
        when(domain3.getName()).thenReturn(formatDomainName(suffix3));
        when(domain4.getName()).thenReturn(formatDomainName(suffix4));
    }

    private String formatDomainName(int i) {
        return format(DOMAIN_NAME_PREFIX + "%02x", i);
    }
}