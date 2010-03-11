/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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
 * -
 */
/**
 * 
 */
package org.jtestplatform.client.domain.libvirt;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.client.domain.DomainException;
import org.jtestplatform.configuration.Platform;
import org.libvirt.LibvirtException;
import org.libvirt.model.network.Bridge;
import org.libvirt.model.network.DHCP;
import org.libvirt.model.network.Forward;
import org.libvirt.model.network.Host;
import org.libvirt.model.network.IP;
import org.libvirt.model.network.Network;
import org.libvirt.model.network.Range;
import org.libvirt.model.network.io.dom4j.NetworkDom4jReader;
import org.libvirt.model.network.io.dom4j.NetworkDom4jWriter;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class XMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(XMLGenerator.class);
    
    static final String BASE_MAC_ADDRESS = "54:52:00:77:58:";
    static final String BASE_IP_ADDRESS = "192.168.121.";
    static final int MIN_SUBNET_IP_ADDRESS = 2;
    static final int MAX_SUBNET_IP_ADDRESS = 254;

    private XMLGenerator() {        
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
        String result = writer.toString();

        LOGGER.debug("generateNetwork:\n" + result);
        return result;
    }
        
    public static String generateDomain(DomainConfig config, String macAddress, String networkName) {
        Platform platform = config.getPlatform(); //TODO use cpu and wordSize properties
        StringBuilder sb = new StringBuilder(4096);
        
        sb.append("<domain type='kvm' id='1'>");             
        sb.append("  <name>").append(config.getDomainName()).append("</name>");
        sb.append("  <memory>").append(platform.getMemory()).append("</memory>");    
        sb.append("  <currentMemory>").append(platform.getMemory()).append("</currentMemory>"); 
        sb.append("  <vcpu>").append(platform.getNbCores()).append("</vcpu>");
        sb.append("  <os>");
        sb.append("    <type arch='x86_64' machine='pc-0.11'>hvm</type>");
        sb.append("    <boot dev='cdrom'/>                                   ");
        sb.append("  </os>                                                   ");
        sb.append("  <features>                                              ");
        sb.append("    <acpi/>                                               ");
        sb.append("    <apic/>                                               ");
        sb.append("    <pae/>                                                ");
        sb.append("  </features>                                             ");
        sb.append("  <clock offset='utc'/>                                   ");
        sb.append("  <on_poweroff>destroy</on_poweroff>                      ");
        sb.append("  <on_reboot>restart</on_reboot>                          ");
        sb.append("  <on_crash>restart</on_crash>                            ");
        sb.append("  <devices>                                               ");
        sb.append("    <emulator>/usr/bin/kvm</emulator>                     ");
        sb.append("    <disk type='file' device='cdrom'>                     ");
        sb.append("      <source file='").append(platform.getCdrom()).append("'/>");                                                                                        
        sb.append("      <target dev='hdc' bus='ide'/>");
        sb.append("      <readonly/>");
        sb.append("    </disk>");

        sb.append("    <interface type='network'>");
        sb.append("      <mac address='").append(macAddress).append("'/>");
        sb.append("      <source network='").append(networkName).append("'/>");
        sb.append("      <target dev='vnet0'/>");
        sb.append("    </interface>");
        
        sb.append("    <serial type='pty'>");
        sb.append("      <source path='/dev/pts/7'/>");
        sb.append("      <target port='0'/>");
        sb.append("    </serial>");
        sb.append("    <console type='pty' tty='/dev/pts/7'>");
        sb.append("      <source path='/dev/pts/7'/>");
        sb.append("      <target port='0'/>");
        sb.append("    </console>");
        sb.append("    <input type='mouse' bus='ps2'/>");
        sb.append("    <graphics type='vnc' port='5900' autoport='yes' keymap='fr'/>");
        sb.append("    <sound model='es1370'/>");
        sb.append("    <video>");
        sb.append("      <model type='cirrus' vram='9216' heads='1'/>");
        sb.append("    </video>");
        sb.append("  </devices>");
        sb.append("  <seclabel type='dynamic' model='apparmor'>");
        sb.append("    <label>libvirt-1557e204-10f8-3c1f-ac60-3dc6f46e85f5</label>");
        sb.append("    <imagelabel>libvirt-1557e204-10f8-3c1f-ac60-3dc6f46e85f5</imagelabel>");
        sb.append("  </seclabel>");
        sb.append("</domain>");
        
        String result = sb.toString();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("generateDomain: config=" + config + " macAddress=" + macAddress
                    + " networkName=" + networkName + " Result=\n" + result);
        }
        
        return result;
    }

    /**
     * @param valueIndex
     * @param hexadecimalSize
     * @return
     * @throws DomainException 
     */
    public static String toHexString(int valueIndex, int hexadecimalSize) throws DomainException {
        String result = Integer.toHexString(valueIndex);
        if (result.length() > hexadecimalSize) {
            throw new DomainException("unable convert to hexadecimal with a maximum of " + hexadecimalSize + " characters");
        }
        
        if (result.length() < hexadecimalSize) {
            while (result.length() != hexadecimalSize) {
                result = '0' + result;
            }
        }
        return result;
    }
    
    public static String getIPAddress(org.libvirt.Network network, String macAddress) throws IOException, DocumentException, LibvirtException {
        String ipAddress = null;
        org.libvirt.model.network.Network net = new NetworkDom4jReader().read(new StringReader(network.getXMLDesc(0)));
        for (Host host : net.getIp().getDhcp().getHost()) {
            if (macAddress.equals(host.getMac())) {
                ipAddress = host.getIp();
                break;
            }
        }
        return ipAddress;
    }

    /**
     * @param wantedNetworkXML
     * @param network
     * @return
     * @throws LibvirtException 
     * @throws DomainException 
     */
    public static boolean sameNetwork(String wantedNetworkXML, org.libvirt.Network network) throws LibvirtException, DomainException {
        String actualNetworkXML = network.getXMLDesc(0);
        org.libvirt.model.network.Network actualNetwork = toNetwork(actualNetworkXML);                
        org.libvirt.model.network.Network wantedNetwork = toNetwork(wantedNetworkXML);
        return sameNetwork(wantedNetwork, actualNetwork);
    }

    /**
     * @param wantedNetwork
     * @param actualNetwork
     * @return
     */
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
            return new NetworkDom4jReader().read(new StringReader(networkXML));
        } catch (IOException e) {
            throw new DomainException(e);
        } catch (DocumentException e) {
            throw new DomainException(e);
        }
    }
}
