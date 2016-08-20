/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
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

import org.jtestplatform.cloud.domain.DomainException;
import org.junit.Test;

import static java.lang.String.format;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class NetworkXMLBuilderTest extends AbstractXMLBuilderTest {
    @Test
    public void build_network1() throws DomainException {
        build("network1", "12:34:", "255.255.255.", 2);
    }

    @Test
    public void build_network2() throws DomainException {
        build("network2", "56:78:", "255.255.128.", 3);
    }

    private void build(String networkName, String baseMacAddress, String baseIPAddress, int minSubNetIpAddress)
        throws DomainException {
        NetworkConfig config = new NetworkConfig(networkName, baseMacAddress, baseIPAddress, minSubNetIpAddress,
                                                 minSubNetIpAddress + 1);
        NetworkXMLBuilder builder = new NetworkXMLBuilder();

        String actualXML = builder.build(config);

        assertXMLContains(actualXML, networkName, formatMacAddress(baseMacAddress, minSubNetIpAddress),
                          formatMacAddress(baseMacAddress, minSubNetIpAddress + 1),
                          formatIPAddress(baseIPAddress, minSubNetIpAddress),
                          formatIPAddress(baseIPAddress, minSubNetIpAddress + 1));
    }

    private static String formatIPAddress(String baseIPAddress, int value) {
        return format("%s%d", baseIPAddress, value);
    }

    private static String formatMacAddress(String baseMacAddress, int value) {
        return format("%s%02x", baseMacAddress, value);
    }
}