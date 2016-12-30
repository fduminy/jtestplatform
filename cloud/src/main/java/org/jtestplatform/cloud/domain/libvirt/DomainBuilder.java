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

import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.libvirt.DomainCache.Entry;
import org.jtestplatform.common.ConfigUtils;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class DomainBuilder {
    private final DomainXMLBuilder domainXMLBuilder;
    private final DomainCache domainCache;
    private final Map<String, Object> locks = new HashMap<String, Object>();

    DomainBuilder(DomainXMLBuilder domainXMLBuilder, DomainCache domainCache) {
        this.domainXMLBuilder = domainXMLBuilder;
        this.domainCache = domainCache;
    }

    DomainInfo defineDomain(Connect connect, DomainConfig config, NetworkConfig networkConfig) throws DomainException {
        try {
            Entry entry = getEntry(connect, config);

            config.setDomainName(entry.getDomainName());
            String macAddress = entry.getMacAddress();
            String xml = domainXMLBuilder.build(config, macAddress, networkConfig.getNetworkName());
            return new DomainInfo(connect.domainDefineXML(xml), macAddress);
        } catch (LibvirtException e) {
            throw new DomainException(e);
        }
    }

    private Entry getEntry(Connect connect, DomainConfig config) throws LibvirtException {
        boolean undefinedDomainName = ConfigUtils.isBlank(config.getDomainName());
        Entry entry;
        synchronized (getLock(connect.getHostName())) {
            if (undefinedDomainName) {
                // automatically define the domain name
                // it must be unique for the connection
                entry = domainCache.findFreeEntry(connect);
            } else {
                entry = domainCache.findEntry(connect, config.getDomainName());
            }
        }
        return entry;
    }

    @Nonnull
    private Object getLock(String hostName) {
        Object lock;
        synchronized (locks) {
            lock = locks.get(hostName);
            if (lock == null) {
                lock = new Object();
                locks.put(hostName, lock);
            }
            return lock;
        }
    }
}