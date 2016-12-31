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
import org.jtestplatform.cloud.domain.libvirt.DomainCache.Entry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.jtestplatform.cloud.domain.libvirt.DomainCache.DOMAIN_NAME_PREFIX;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class DomainCacheTest {
    static final String BASE_MAC_ADDRESS = "12:34:56:";
    static final String BASE_IP_ADDRESS = "101.102.103.";
    private static final NetworkConfig CONFIG = new NetworkConfig("test", BASE_MAC_ADDRESS, BASE_IP_ADDRESS, 14, 18);

    @Rule
    public JUnitSoftAssertions soft = new JUnitSoftAssertions();

    private DomainCache domainCache;
    private Connect connect;

    @Before
    public void setUp() throws LibvirtException {
        connect = mock(Connect.class);
        domainCache = new DomainCache(CONFIG);
    }

    @Theory
    public void findFreeEntry_noEntry(boolean definedDomain) throws Exception {
        Map<Domain, Entry> domainInfos = mockDomains(CONFIG, CONFIG.size()); // "reserve" all domains
        mockConnect(definedDomain, domainInfos);

        Entry entry = domainCache.findFreeEntry(connect);

        assertEntry(null, entry);
    }

    @Theory
    public void findFreeEntry_contiguousUsedValues(boolean definedDomain) throws Exception {
        Map<Domain, Entry> domainInfos = mockDomains(14, 15, 16, 17); // "reserve" one domain
        mockConnect(definedDomain, domainInfos);

        Entry entry = domainCache.findFreeEntry(connect);

        assertEntry(expectedEntry(18), entry);
    }

    @Theory
    public void findFreeEntry_nonContiguousUsedValues(boolean definedDomain) throws Exception {
        Map<Domain, Entry> domainInfos = mockDomains(14, 15, 17, 18); // "reserve" one domain
        mockConnect(definedDomain, domainInfos);

        Entry entry = domainCache.findFreeEntry(connect);

        assertEntry(expectedEntry(16), entry);
    }

    @Theory
    public void findEntry_noEntry(boolean definedDomain) throws Exception {
        String domainName = formatDomainName(CONFIG.getMinSubNetIpAddress());
        mockConnect(definedDomain, Collections.<Domain, Entry>emptyMap());

        Entry entry = domainCache.findEntry(connect, domainName);

        assertEntry(null, entry);
    }

    @Theory
    public void findEntry(boolean definedDomain) throws Exception {
        Map<Domain, Entry> domainInfos = mockDomains(CONFIG, 1);
        Map.Entry<Domain, Entry> domainInfo = domainInfos.entrySet().iterator().next();
        mockConnect(definedDomain, domainInfos);

        Entry entry = domainCache.findEntry(connect, domainInfo.getValue().getDomainName());

        assertEntry(domainInfo.getValue(), entry);
    }

    private void assertEntry(@Nullable Entry expected, @Nullable Entry actual) {
        if (expected == null) {
            soft.assertThat(actual).as("entry").isNull();
        } else {
            soft.assertThat(actual).as("entry").isNotNull();
            if (actual != null) {
                soft.assertThat(actual.getDomainName()).as("entry.domainName").isEqualTo(expected.getDomainName());
                soft.assertThat(actual.getMacAddress()).as("entry.maxAddress").isEqualTo(expected.getMacAddress());
                soft.assertThat(actual.getIpAddress()).as("entry.ipAddress").isEqualTo(expected.getIpAddress());
            }
        }
    }

    private Map<Domain, Entry> mockDomains(NetworkConfig networkConfig, int numberOfDomains) throws LibvirtException {
        int[] usedDomainIds = new int[numberOfDomains];
        int firstIndex = networkConfig.getMinSubNetIpAddress();
        for (int index = 0; index < numberOfDomains; index++) {
            usedDomainIds[index] = firstIndex + index;
        }
        return mockDomains(usedDomainIds);
    }

    private Map<Domain, Entry> mockDomains(int... usedDomainIds) throws LibvirtException {
        Map<Domain, Entry> domains = new HashMap<Domain, Entry>();
        for (int domainId : usedDomainIds) {
            Domain domain = mock(Domain.class);
            String macAddress = formatMacAddress(domainId);
            String ipAddress = formatIpAddress(domainId);
            when(domain.getXMLDesc(anyInt())).thenReturn("<mac address='" + macAddress + "'/>");
            String domainName = formatDomainName(domainId);
            when(domain.getName()).thenReturn(domainName);
            domains.put(domain, new Entry(domainName, macAddress, ipAddress));
        }
        return domains;
    }

    private void mockConnect(boolean definedDomain, Map<Domain, Entry> domainInfos)
        throws LibvirtException {
        String[] domainNames;
        int[] domainIds;
        if (definedDomain) {
            domainNames = new String[domainInfos.size()];
            domainIds = new int[0];
            int index = 0;
            for (Map.Entry<Domain, Entry> domainInfo : domainInfos.entrySet()) {
                domainNames[index] = domainInfo.getValue().getDomainName();
                when(connect.domainLookupByName(domainInfo.getValue().getDomainName())).thenReturn(domainInfo.getKey());
                index++;
            }
        } else {
            domainNames = new String[0];
            domainIds = new int[domainInfos.size()];
            int index = 0;
            for (Map.Entry<Domain, Entry> domainInfo : domainInfos.entrySet()) {
                domainIds[index] = index;
                when(connect.domainLookupByID(index)).thenReturn(domainInfo.getKey());
                index++;
            }
        }
        when(connect.listDefinedDomains()).thenReturn(domainNames);
        when(connect.listDomains()).thenReturn(domainIds);
    }

    private static Entry expectedEntry(int domainId) {
        return new Entry(formatDomainName(domainId), formatMacAddress(domainId), formatIpAddress(domainId));
    }

    static String formatDomainName(int domainId) {
        return format(DOMAIN_NAME_PREFIX + "%d", domainId);
    }

    static String formatMacAddress(int domainId) {
        return format(BASE_MAC_ADDRESS + "%02x", domainId);
    }

    static String formatIpAddress(int domainId) {
        return format(BASE_IP_ADDRESS + "%d", domainId);
    }
}