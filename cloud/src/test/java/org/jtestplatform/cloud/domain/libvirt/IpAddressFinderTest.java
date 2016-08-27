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

import com.google.code.tempusfugit.temporal.Duration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jtestplatform.cloud.domain.DomainException;
import org.junit.Rule;
import org.junit.Test;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static java.net.InetAddress.getLocalHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.libvirt.NetworkBuilderTest.NETWORK_CONFIG;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class IpAddressFinderTest {
    private static final String UNREACHABLE_IP_ADDRESS = "255.255.128.3";
    private static String[] IP_ADDRESSES;

    static {
        IP_ADDRESSES = new String[NETWORK_CONFIG.getMaxSubNetIpAddress() - NETWORK_CONFIG.getMinSubNetIpAddress() + 1];
        for (int i = 0; i < IP_ADDRESSES.length; i++) {
            IP_ADDRESSES[i] = NETWORK_CONFIG.getBaseIPAddress() + (NETWORK_CONFIG.getMinSubNetIpAddress() + i);
        }
    }

    @Rule
    public JUnitSoftAssertions soft = new JUnitSoftAssertions();

    @Test
    public void findIpAddress() throws Exception {
        for (final String ipAddress : IP_ADDRESSES) {
            soft.assertThat(findIpAddress(ipAddress)).isEqualTo(ipAddress);
        }
    }

    @Test
    public void findIpAddress_unreachable() throws Exception {
        assertThat(findIpAddress(UNREACHABLE_IP_ADDRESS)).isNull();
    }

    private String findIpAddress(final String ipAddress) throws DomainException {
        IpAddressFinder ipAddressFinder = new IpAddressFinder() {
            @Override boolean ping(String host, Duration timeout) {
                return host.equals(ipAddress);
            }
        };

        return ipAddressFinder.findIpAddress(NETWORK_CONFIG);

    }

    @Test
    public void ping() throws Exception {
        IpAddressFinder ipAddressFinder = new IpAddressFinder();

        boolean pong = ipAddressFinder.ping(getLocalHost().getHostAddress(), seconds(3));

        assertThat(pong).isTrue();
    }

    @Test
    public void ping_unreachable() throws Exception {
        IpAddressFinder ipAddressFinder = new IpAddressFinder();

        boolean pong = ipAddressFinder.ping(UNREACHABLE_IP_ADDRESS, seconds(3));

        assertThat(pong).isFalse();
    }
}