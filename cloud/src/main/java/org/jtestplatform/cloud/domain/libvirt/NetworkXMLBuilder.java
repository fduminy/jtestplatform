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
import org.libvirt.model.network.*;
import org.libvirt.model.network.io.dom4j.NetworkDom4jWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.jtestplatform.cloud.domain.libvirt.LibVirtUtils.toHexString;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class NetworkXMLBuilder {
    String build(NetworkConfig networkConfig) throws DomainException {
        Network network = new Network();
        network.setName(networkConfig.getNetworkName());
        network.setUuid("ec6f8ce7-ad0f-eea7-d9ed-dc460cf47023");

        Forward forward = new Forward();
        forward.setMode("nat");
        network.setForward(forward);

        Bridge bridge = new Bridge();
        bridge.setName("virbr1");
        bridge.setStp("on");
        bridge.setForwardDelay(1);
        network.setBridge(bridge);

        IP ip = new IP();
        ip.setAddress(networkConfig.getBaseIPAddress() + '1');
        ip.setNetmask("255.255.255.0");
        network.setIp(ip);

        DHCP dhcp = new DHCP();
        ip.setDhcp(dhcp);

        Range range = new Range();
        range.setStart(networkConfig.getBaseIPAddress() + networkConfig.getMinSubNetIpAddress());
        range.setEnd(networkConfig.getBaseIPAddress() + networkConfig.getMaxSubNetIpAddress());
        dhcp.setRange(range);

        for (int i = networkConfig.getMinSubNetIpAddress(); i <= networkConfig.getMaxSubNetIpAddress(); i++) {
            Host host = new Host();
            host.setMac(networkConfig.getBaseMacAddress() + toHexString(i, 2));
            host.setIp(networkConfig.getBaseIPAddress() + i);
            dhcp.addHost(host);
        }

        StringWriter writer = new StringWriter();
        try {
            new NetworkDom4jWriter().write(writer, network);
        } catch (IOException e) {
            throw new DomainException(e);
        }
        return writer.toString();
    }
}
