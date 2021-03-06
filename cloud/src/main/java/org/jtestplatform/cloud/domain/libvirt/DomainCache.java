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
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class DomainCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DomainCache.class);
    static final String DOMAIN_NAME_PREFIX = "JTestPlatform_";

    private final NetworkConfig networkConfig;

    DomainCache(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    @Nullable
    Entry findFreeEntry(Connect connect) {
        Entry entry = null;

        try {
            Integer domainId = findFreeDomainId(listAllDomains(connect));
            if (domainId != null) {
                entry = createEntry(domainId);
            }
        } catch (LibvirtException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (DomainException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return entry;
    }

    @Nullable
    Entry findEntry(Connect connect, String domainName) {
        Entry entry = null;

        try {
            for (Domain domain : listAllDomains(connect)) {
                if (domain.getName().equals(domainName)) {
                    Integer domainId = findDomainId(domain);
                    if (domainId != null) {
                        entry = createEntry(domainId);
                        break;
                    }
                }
            }
        } catch (LibvirtException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (DomainException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return entry;
    }

    @Nullable
    private Integer findDomainId(Domain domain) throws LibvirtException {
        String macAddress = getMacAddress(domain);
        if (macAddress == null) {
            return null;
        }
        return Integer.parseInt(macAddress.substring(networkConfig.getBaseMacAddress().length()), 16);
    }

    static class Entry {
        private final String domainName;
        private final String macAddress;
        private final String ipAddress;

        Entry(String domainName, String macAddress, String ipAddress) {
            this.domainName = domainName;
            this.macAddress = macAddress;
            this.ipAddress = ipAddress;
        }

        String getDomainName() {
            return domainName;
        }

        String getMacAddress() {
            return macAddress;
        }

        public String getIpAddress() {
            return ipAddress;
        }
    }

    private List<Domain> listAllDomains(Connect connect) throws LibvirtException {
        List<Domain> domains = new ArrayList<Domain>();

        // get defined but inactive domains
        for (String name : connect.listDefinedDomains()) {
            if (name != null) {
                domains.add(connect.domainLookupByName(name));
            }
        }

        // get active domains
        for (int id : connect.listDomains()) {
            domains.add(connect.domainLookupByID(id));
        }

        return domains;
    }

    @Nullable
    private Integer findFreeDomainId(List<Domain> domains) throws LibvirtException {
        Set<Integer> domainIds = new HashSet<Integer>(domains.size());
        for (Domain domain : domains) {
            if (domain.getName().startsWith(DOMAIN_NAME_PREFIX)) {
                domainIds.add(Integer.parseInt(domain.getName().substring(DOMAIN_NAME_PREFIX.length())));
            }
        }

        Integer domainId = null;
        for (int id = networkConfig.getMinSubNetIpAddress(); id <= networkConfig.getMaxSubNetIpAddress(); id++) {
            if (!domainIds.contains(id)) {
                domainId = id;
                break;
            }
        }
        return domainId;
    }

    private Entry createEntry(Integer domainId) throws DomainException {
        String macAddress = formatHexadecimalValue(networkConfig.getBaseMacAddress(), domainId);
        String ipAddress = formatDecimalValue(networkConfig.getBaseIPAddress(), domainId);
        return new Entry(formatDecimalValue(DOMAIN_NAME_PREFIX, domainId), macAddress, ipAddress);
    }

    private static String getMacAddress(Domain domain) throws LibvirtException {
        String macAddress = null;

        String xml = domain.getXMLDesc(0);

        //TODO it's bad, we should use an xml parser. create and add it in the libvirt-model project.
        String begin = "<mac address='";
        int idx = xml.indexOf(begin);
        if (idx >= 0) {
            idx += begin.length();
            int idx2 = xml.indexOf('\'', idx);
            if (idx2 >= 0) {
                macAddress = xml.substring(idx, idx2);
            }
        }

        return macAddress;
    }

    private static String formatDecimalValue(String valuePrefix, int domainId) throws DomainException {
        return format(valuePrefix + "%d", domainId);
    }

    private static String formatHexadecimalValue(String valuePrefix, int domainId) throws DomainException {
        return format(valuePrefix + "%02x", domainId);
    }
}
