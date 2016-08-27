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

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.libvirt.Connect;
import org.libvirt.Network;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class NetworkBuilderTest extends AbstractDomainTest {
    private static final String EXPECTED_NETWORK_NAME = "networkName";
    private static final String NETWORK_XML = "domainXML";
    static final NetworkConfig NETWORK_CONFIG = new NetworkConfig(EXPECTED_NETWORK_NAME, "12:34:56:", "127.0.0.", 1, 3);

    @Rule
    public JUnitSoftAssertions soft = new JUnitSoftAssertions();

    @Mock
    private Connect connect;
    @Mock
    private Network expectedNetwork;
    @Mock
    private NetworkXMLBuilder networkXMLBuilder;

    @Before
    public void setUp() throws Exception {
        when(networkXMLBuilder.build(NETWORK_CONFIG)).thenReturn(NETWORK_XML);
        when(connect.networkDefineXML(NETWORK_XML)).thenReturn(expectedNetwork);
    }

    @Test
    public void build() throws Exception {
        NetworkBuilder builder = new NetworkBuilder(networkXMLBuilder);

        Network network = builder.build(connect, NETWORK_CONFIG);

        soft.assertThat(network).as("network").isSameAs(expectedNetwork);
    }

}