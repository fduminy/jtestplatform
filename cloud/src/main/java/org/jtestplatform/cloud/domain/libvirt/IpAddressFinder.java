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

import org.dom4j.DocumentException;
import org.jtestplatform.cloud.domain.DomainException;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.model.network.Host;
import org.libvirt.model.network.io.dom4j.NetworkDom4jReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

import static org.jtestplatform.cloud.domain.libvirt.LibVirtUtils.STRICT;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class IpAddressFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniqueValueFinder.class);

    private final NetworkConfig networkConfig;

    IpAddressFinder(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    String findIpAddress(Domain domain) throws DomainException {
        String ipAddress = null;
        Network network = null;
        try {
            String macAddress = UniqueMacAddressFinder.getMacAddress(domain);
            if (macAddress == null) {
                throw new DomainException("unable to get mac address");
            }

            network = domain.getConnect().networkLookupByName(networkConfig.getNetworkName());
            ipAddress = getIPAddress(network, macAddress);
            if (ipAddress == null) {
                throw new DomainException("unable to get ip address");
            }
        } catch (LibvirtException e) {
            throw new DomainException(e);
        } catch (IOException e) {
            throw new DomainException(e);
        } catch (DocumentException e) {
            throw new DomainException(e);
        } finally {
            if (network != null) {
                try {
                    network.free();
                } catch (LibvirtException e) {
                    LOGGER.error("failed to free network", e);
                }
            }
        }

        return ipAddress;
    }

    private static String getIPAddress(org.libvirt.Network network, String macAddress)
        throws IOException, DocumentException, LibvirtException {
        String ipAddress = null;
        // FIXME when STRICT is set to true, the tag 'nat' throws an exception
        org.libvirt.model.network.Network net = new NetworkDom4jReader()
            .read(new StringReader(network.getXMLDesc(0)), STRICT);
        for (Host host : net.getIp().getDhcp().getHost()) {
            if (macAddress.equals(host.getMac())) {
                ipAddress = host.getIp();
                break;
            }
        }
        return ipAddress;
    }
}
