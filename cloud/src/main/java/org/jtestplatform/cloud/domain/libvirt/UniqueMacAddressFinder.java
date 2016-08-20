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
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class UniqueMacAddressFinder extends UniqueValueFinder {
    private final NetworkConfig networkConfig;

    UniqueMacAddressFinder(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    /**
     * Automatically define the mac address. It must be unique for the connection.
     * @throws LibvirtException
     * @throws DomainException
     */
    String findUniqueMacAddress(List<Domain> domains) throws LibvirtException, DomainException {
        List<String> macAddresses = new ArrayList<String>();
        for (Domain domain : domains) {
            String addr = getMacAddress(domain);
            if (addr != null) {
                macAddresses.add(addr);
            }
        }

        return findUniqueValue(macAddresses, "mac address", networkConfig.getBaseMacAddress(),
                               networkConfig.getMinSubNetIpAddress(),
                               networkConfig.getMaxSubNetIpAddress(), 2);
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
}
