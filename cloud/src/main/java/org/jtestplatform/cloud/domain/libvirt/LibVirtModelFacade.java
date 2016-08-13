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
/**
 *
 */
package org.jtestplatform.cloud.domain.libvirt;

import org.dom4j.DocumentException;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.model.capabilities.Arch;
import org.libvirt.model.capabilities.Capabilities;
import org.libvirt.model.capabilities.Domain;
import org.libvirt.model.capabilities.Guest;
import org.libvirt.model.capabilities.io.dom4j.CapabilitiesDom4jReader;
import org.libvirt.model.network.*;
import org.libvirt.model.network.io.dom4j.NetworkDom4jReader;
import org.libvirt.model.network.io.dom4j.NetworkDom4jWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class LibVirtModelFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibVirtModelFacade.class);

    private static final boolean STRICT = false; // when set to true, some tags throws an exception

    static final String BASE_MAC_ADDRESS = "54:52:00:77:58:";
    static final String BASE_IP_ADDRESS = "192.168.121.";
    static final int MIN_SUBNET_IP_ADDRESS = 2;
    static final int MAX_SUBNET_IP_ADDRESS = 254;

    private LibVirtModelFacade() {
    }

    public static String generateNetwork(String networkName) throws DomainException {
        Network network = new Network();
        network.setName(networkName);
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
        ip.setAddress(BASE_IP_ADDRESS + '1');
        ip.setNetmask("255.255.255.0");
        network.setIp(ip);

        DHCP dhcp = new DHCP();
        ip.setDhcp(dhcp);

        Range range = new Range();
        range.setStart(BASE_IP_ADDRESS + MIN_SUBNET_IP_ADDRESS);
        range.setEnd(BASE_IP_ADDRESS + MAX_SUBNET_IP_ADDRESS);
        dhcp.setRange(range);

        for (int i = MIN_SUBNET_IP_ADDRESS; i <= MAX_SUBNET_IP_ADDRESS; i++) {
            Host host = new Host();
            host.setMac(BASE_MAC_ADDRESS + toHexString(i, 2));
            host.setIp(BASE_IP_ADDRESS + i);
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

    public static String toHexString(int valueIndex, int hexadecimalSize) throws DomainException {
        String result = Integer.toHexString(valueIndex);
        if (result.length() > hexadecimalSize) {
            throw new DomainException(
                "unable convert to hexadecimal with a maximum of " + hexadecimalSize + " characters");
        }

        if (result.length() < hexadecimalSize) {
            while (result.length() != hexadecimalSize) {
                result = '0' + result;
            }
        }
        return result;
    }

    public static String getIPAddress(org.libvirt.Network network, String macAddress)
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

    public static boolean sameNetwork(String wantedNetworkXML, org.libvirt.Network network)
        throws LibvirtException, DomainException {
        String actualNetworkXML = network.getXMLDesc(0);
        org.libvirt.model.network.Network actualNetwork = toNetwork(actualNetworkXML);
        org.libvirt.model.network.Network wantedNetwork = toNetwork(wantedNetworkXML);
        return sameNetwork(wantedNetwork, actualNetwork);
    }

    private static boolean sameNetwork(org.libvirt.model.network.Network wantedNetwork,
                                       org.libvirt.model.network.Network actualNetwork) {

        IP wantedIP = wantedNetwork.getIp();
        IP actualIP = actualNetwork.getIp();
        boolean sameNetwork = (wantedIP.getAddress().equals(actualIP.getAddress()));
        sameNetwork &= (wantedIP.getNetmask().equals(actualIP.getNetmask()));

        if (sameNetwork) {
            DHCP wantedDHCP = wantedIP.getDhcp();
            DHCP actualDHCP = actualIP.getDhcp();
            for (Host wantedHost : wantedDHCP.getHost()) {
                boolean sameHost = false;
                for (Host actualHost : actualDHCP.getHost()) {
                    if (wantedHost.getMac().equals(actualHost.getMac())
                        && wantedHost.getIp().equals(actualHost.getIp())) {
                        sameHost = true;
                        break;
                    }
                }
                sameNetwork &= sameHost;

                if (!sameNetwork) {
                    break;
                }
            }

            if (sameNetwork) {
                Range wantedRange = wantedDHCP.getRange();
                Range actualRange = actualDHCP.getRange();
                sameNetwork &= wantedRange.getStart().equals(actualRange.getStart());
                sameNetwork &= wantedRange.getEnd().equals(actualRange.getEnd());
            }
        }

        return sameNetwork;
    }

    private static org.libvirt.model.network.Network toNetwork(String networkXML) throws DomainException {
        try {
            // FIXME when STRICT is set to true, the tag 'nat' throws an exception
            return new NetworkDom4jReader().read(new StringReader(networkXML), STRICT);
        } catch (IOException e) {
            throw new DomainException(e);
        } catch (DocumentException e) {
            throw new DomainException(e);
        }
    }

    public static boolean support(Platform platform, Connect connect)
        throws LibvirtException, IOException, DocumentException {
        LOGGER.trace("begin support");

        boolean support;
        try {
            support = (connect.nodeInfo().memory >= platform.getMemory());
        } catch (LibvirtException lve) {
            // an exception might be thrown if the function is not supported by the hypervisor
            // then, we assume there is enough memory

            LOGGER.error("Error while calling nodeInfo()", lve);
            support = true;
        }

        if (support) {
            String capabilitiesXML = connect.getCapabilities();

            // FIXME when STRICT is set to true the tag 'uuid' throws an exception
            Capabilities capabilities = new CapabilitiesDom4jReader().read(new StringReader(capabilitiesXML), STRICT);

            outloop:
            for (Guest guest : capabilities.getGuests()) {
                Arch arch = guest.getArch();

                boolean supportCPU = arch.getName().equals(platform.getCpu());
                boolean supportWordSize = (arch.getWordSize() == platform.getWordSize());
                if (supportCPU && supportWordSize) {
                    for (Domain domain : guest.getArch().getDomains()) {

                        boolean supportNbCores = (platform.getNbCores() <= connect.getMaxVcpus(domain.getType()));
                        if (supportNbCores) {
                            support = true;
                            break outloop;
                        }
                    }
                }
            }
        }

        LOGGER.trace("end support");
        return support;
    }
}
