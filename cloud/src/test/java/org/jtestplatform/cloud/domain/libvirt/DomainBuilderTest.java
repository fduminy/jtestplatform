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
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.libvirt.DomainCache.Entry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.mockito.Mock;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class DomainBuilderTest extends AbstractDomainTest {
    private static final int DOMAIN_ID = 12;
    private static final String MAC_ADDRESS = "12:34:56:" + toHexString(DOMAIN_ID);
    protected static final String IP_ADDRESS = "127.126.125." + Integer.toString(DOMAIN_ID);
    private static final String NETWORK_NAME = "networkName";
    private static final String DOMAIN_XML = "domainXML";
    private static final String EXPECTED_DOMAIN_NAME = "domain5";
    private static final Entry EXPECTED_ENTRY = new Entry(EXPECTED_DOMAIN_NAME, MAC_ADDRESS, IP_ADDRESS);

    @Rule
    public JUnitSoftAssertions soft = new JUnitSoftAssertions();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private DomainXMLBuilder domainXMLBuilder;
    @Mock
    private DomainCache domainCache;
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
        when(networkConfig.getNetworkName()).thenReturn(NETWORK_NAME);
        when(domainXMLBuilder.build(domainConfig, MAC_ADDRESS, NETWORK_NAME)).thenReturn(DOMAIN_XML);
        when(connect.domainDefineXML(DOMAIN_XML)).thenReturn(expectedDomain);
    }

    @Theory
    public void defineDomain(boolean nameWasDefined) throws Exception {
        if (nameWasDefined) {
            domainConfig.setDomainName(EXPECTED_DOMAIN_NAME);
            when(domainCache.findEntry(connect, EXPECTED_DOMAIN_NAME)).thenReturn(EXPECTED_ENTRY);
        } else {
            when(domainCache.findFreeEntry(connect)).thenReturn(EXPECTED_ENTRY);
        }
        DomainBuilder builder = new DomainBuilder(domainXMLBuilder, domainCache);

        DomainInfo domainInfo = builder.defineDomain(connect, domainConfig, networkConfig);

        soft.assertThat(domainInfo).as("domainInfo").isNotNull();
        if (domainInfo != null) {
            soft.assertThat(domainInfo.getDomain()).as("domainInfo.domain").isSameAs(expectedDomain);
            soft.assertThat(domainInfo.getMacAddress()).as("domainInfo.macAddress").isSameAs(MAC_ADDRESS);
            soft.assertThat(domainInfo.getIpAddress()).as("domainInfo.ipAddress").isSameAs(IP_ADDRESS);
        }
        soft.assertThat(domainConfig.getDomainName()).as("domainConfig.domainName").isEqualTo(EXPECTED_DOMAIN_NAME);
    }

    @Theory
    public void defineDomain_noEntry(boolean nameWasDefined) throws Exception {
        if (nameWasDefined) {
            domainConfig.setDomainName(EXPECTED_DOMAIN_NAME);
        }
        thrown.expectMessage(nameWasDefined ?
                                 format("No Entry found for a domain named %s", EXPECTED_DOMAIN_NAME) :
                                 "No free Entry found for a new domain");
        thrown.expect(DomainException.class);

        new DomainBuilder(domainXMLBuilder, domainCache).defineDomain(connect, domainConfig, networkConfig);
    }
}