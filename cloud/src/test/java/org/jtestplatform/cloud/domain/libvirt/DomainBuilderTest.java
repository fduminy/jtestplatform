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
import org.jtestplatform.cloud.domain.DomainConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class DomainBuilderTest extends AbstractDomainTest {
    private static final String MAC_ADDRESS = "12:34:56:78";
    private static final String NETWORK_NAME = "networkName";
    private static final String DOMAIN_XML = "domainXML";
    private static final String EXPECTED_DOMAIN_NAME = "domain5";

    @Rule
    public JUnitSoftAssertions soft = new JUnitSoftAssertions();

    @Mock
    private DomainXMLBuilder domainXMLBuilder;
    @Mock
    private UniqueMacAddressFinder macAddressFinder;
    @Mock
    private UniqueDomainNameFinder domainNameFinder;
    @Mock
    private Connect connect;
    @Mock
    private Domain expectedDomain;
    @Mock
    private NetworkConfig networkConfig;

    private DomainConfig domainConfig;

    @Before
    public void setUp() throws Exception {
        domainConfig = new DomainConfig();

        when(connect.listDefinedDomains()).thenReturn(new String[] { "domain1", "domain2" });
        when(connect.domainLookupByName("domain1")).thenReturn(domain1);
        when(connect.domainLookupByName("domain2")).thenReturn(domain2);
        when(connect.listDomains()).thenReturn(new int[] { 3, 4 });
        when(connect.domainLookupByID(3)).thenReturn(domain3);
        when(connect.domainLookupByID(4)).thenReturn(domain4);
        when(macAddressFinder.findUniqueMacAddress(domains)).thenReturn(MAC_ADDRESS);
        when(networkConfig.getNetworkName()).thenReturn(NETWORK_NAME);
        when(domainXMLBuilder.build(domainConfig, MAC_ADDRESS, NETWORK_NAME)).thenReturn(DOMAIN_XML);
        when(connect.domainDefineXML(DOMAIN_XML)).thenReturn(expectedDomain);
    }

    @Theory
    public void defineDomain(boolean nameWasDefined) throws Exception {
        if (nameWasDefined) {
            domainConfig.setDomainName(EXPECTED_DOMAIN_NAME);
        } else {
            when(domainNameFinder.findUniqueDomainName(domains)).thenReturn(EXPECTED_DOMAIN_NAME);
        }
        DomainBuilder builder = new DomainBuilder(domainXMLBuilder, macAddressFinder, domainNameFinder);

        org.jtestplatform.cloud.domain.libvirt.DomainInfo domainInfo = builder
            .defineDomain(connect, domainConfig, networkConfig);

        soft.assertThat(domainInfo).as("domainInfo").isNotNull();
        if (domainInfo != null) {
            soft.assertThat(domainInfo.getDomain()).as("domainInfo.domain").isSameAs(expectedDomain);
            soft.assertThat(domainInfo.getMacAddress()).as("domainInfo.macAddress").isSameAs(MAC_ADDRESS);
        }
        soft.assertThat(domainConfig.getDomainName()).as("domainConfig.domainName").isEqualTo(EXPECTED_DOMAIN_NAME);
    }

}