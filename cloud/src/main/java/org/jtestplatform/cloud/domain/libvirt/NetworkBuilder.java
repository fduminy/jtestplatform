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

import org.dom4j.DocumentException;
import org.jtestplatform.cloud.domain.DomainException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.model.network.DHCP;
import org.libvirt.model.network.Host;
import org.libvirt.model.network.IP;
import org.libvirt.model.network.Range;
import org.libvirt.model.network.io.dom4j.NetworkDom4jReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.jtestplatform.cloud.domain.libvirt.LibVirtUtils.STRICT;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class NetworkBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkBuilder.class);

    private final NetworkXMLBuilder networkXMLBuilder;

    NetworkBuilder(NetworkXMLBuilder networkXMLBuilder) {
        this.networkXMLBuilder = networkXMLBuilder;
    }

    Network build(Connect connect, NetworkConfig networkConfig) throws DomainException {
        return null;
    }

    void ensureNetworkExist(Connect connect, NetworkConfig networkConfig) throws LibvirtException, DomainException {
        //TODO create our own network

        String wantedNetworkXML = networkXMLBuilder.build(networkConfig);

        // synchronize because multiple threads might check/destroy/create the network concurrently
        synchronized ((connect.getHostName() + "_ensureNetworkExist").intern()) {
            Network network = null;

            try {
                network = networkLookupByName(connect, networkConfig.getNetworkName());
                if (network != null) {
                    if (sameNetwork(wantedNetworkXML, network)) {
                        LOGGER.debug("network '{}' already exists with proper characteristics",
                                     networkConfig.getNetworkName());
                    } else {
                        network.destroy();
                        //network.undefine();
                        LOGGER.debug("destroyed network '{}'", networkConfig.getNetworkName());
                        network = null;
                    }
                }

                if (network == null) {
                    network = connect.networkCreateXML(wantedNetworkXML);
                    LOGGER.debug("created network '{}'", networkConfig.getNetworkName());
                }
            } finally {
                if (network != null) {
                    network.free();
                }
            }
        }
    }

    private static Network networkLookupByName(Connect connect, String networkName) throws LibvirtException {
        Network network = null;
        if (Arrays.asList(connect.listDefinedNetworks()).contains(networkName) || Arrays.asList(connect.listNetworks())
                                                                                        .contains(networkName)) {
            network = connect.networkLookupByName(networkName);
        }
        return network;
    }

    private static boolean sameNetwork(String wantedNetworkXML, org.libvirt.Network network)
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
}