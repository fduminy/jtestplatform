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
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class DomainCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DomainCache.class);

    private final UniqueMacAddressFinder macAddressFinder;
    private final UniqueDomainNameFinder domainNameFinder;

    DomainCache(NetworkConfig networkConfig) {
        this.macAddressFinder = new UniqueMacAddressFinder(networkConfig);
        this.domainNameFinder = new UniqueDomainNameFinder(networkConfig);
    }

    @Nullable
    Entry findFreeEntry(Connect connect) {
        Entry entry = null;

        try {
            List<Domain> domains = listAllDomains(connect);
            String domainName = domainNameFinder.findUniqueDomainName(domains);
            entry = new Entry(domainName, macAddressFinder.findUniqueMacAddress(domains));
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
                    entry = new Entry(domainName, UniqueMacAddressFinder.getMacAddress(domain));
                    break;
                }
            }
        } catch (LibvirtException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return entry;
    }

    static class Entry {
        private final String domainName;
        private final String macAddress;

        Entry(String domainName, String macAddress) {
            this.domainName = domainName;
            this.macAddress = macAddress;
        }

        String getDomainName() {
            return domainName;
        }

        String getMacAddress() {
            return macAddress;
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
}
