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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.libvirt.LibvirtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class UniqueMacAddressFinderTest extends AbstractDomainTest {
    static final String BASE_MAC_ADDRESS = "12:34:56:";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void findUniqueMacAddress_contiguousUsedValues() throws Exception {
        setUpMacAddresses(1, 2, 3, 4);
        UniqueMacAddressFinder finder = new UniqueMacAddressFinder(new NetworkConfig("", BASE_MAC_ADDRESS, "", 1, 5));

        String actual = finder.findUniqueMacAddress(domains);

        assertThat(actual).isEqualTo(BASE_MAC_ADDRESS + "05");
    }

    @Test
    public void findUniqueMacAddress_nonContiguousUsedValues() throws Exception {
        setUpMacAddresses(1, 2, 4, 5);
        UniqueMacAddressFinder finder = new UniqueMacAddressFinder(new NetworkConfig("", BASE_MAC_ADDRESS, "", 1, 5));

        String actual = finder.findUniqueMacAddress(domains);

        assertThat(actual).isEqualTo(BASE_MAC_ADDRESS + "03");
    }

    @Test
    public void findUniqueMacAddress_noFreeValues() throws Exception {
        setUpMacAddresses(1, 2, 3, 4);
        UniqueMacAddressFinder finder = new UniqueMacAddressFinder(new NetworkConfig("", BASE_MAC_ADDRESS, "", 1, 4));
        thrown.expect(DomainException.class);
        thrown.expectMessage("unable to find a unique mac address");

        finder.findUniqueMacAddress(domains);
    }

    private void setUpMacAddresses(int suffix1, int suffix2, int suffix3, int suffix4) throws LibvirtException {
        DomainXMLBuilder builder = new DomainXMLBuilder();
        DomainConfig config = new DomainConfig();
        config.setPlatform(new Platform());
        when(domain1.getXMLDesc(anyInt())).thenReturn(builder.build(config, BASE_MAC_ADDRESS + "0" + suffix1, ""));
        when(domain2.getXMLDesc(anyInt())).thenReturn(builder.build(config, BASE_MAC_ADDRESS + "0" + suffix2, ""));
        when(domain3.getXMLDesc(anyInt())).thenReturn(builder.build(config, BASE_MAC_ADDRESS + "0" + suffix3, ""));
        when(domain4.getXMLDesc(anyInt())).thenReturn(builder.build(config, BASE_MAC_ADDRESS + "0" + suffix4, ""));
    }

}