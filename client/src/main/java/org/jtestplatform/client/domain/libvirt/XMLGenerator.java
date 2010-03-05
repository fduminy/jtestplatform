/**
 * JTestPlatform is a client/server framework for testing any JVM implementation.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/**
 * 
 */
package org.jtestplatform.client.domain.libvirt;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.ConfigurationException;
import org.libvirt.model.Bridge;
import org.libvirt.model.DHCP;
import org.libvirt.model.Forward;
import org.libvirt.model.Host;
import org.libvirt.model.IP;
import org.libvirt.model.Network;
import org.libvirt.model.Range;
import org.libvirt.model.io.dom4j.NetworkDom4jWriter;


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
        
    public static String generateNetwork(String networkName) throws ConfigurationException {
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
            throw new ConfigurationException(e);
        }
        String result = writer.toString();

        LOGGER.debug("generateNetwork:\n" + result);
        return result;
    }
        
    public static String generateDomain(String name, String cdromFile, String macAddress, String networkName) {
        StringBuilder sb = new StringBuilder(4096);
        
        sb.append("<domain type='kvm' id='1'>");             
        sb.append("  <name>").append(name).append("</name>");
        sb.append("  <memory>524288</memory>");    
        sb.append("  <currentMemory>524288</currentMemory>"); 
        sb.append("  <vcpu>1</vcpu>");
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
        sb.append("      <source file='").append(cdromFile).append("'/>");                                                                                        
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
            LOGGER.debug("generateDomain: name=" + name + " cdrom=" + cdromFile + " networkName=" + networkName + " Result=\n" + result);
        }
        
        return result;
    }

    /**
     * @param valueIndex
     * @param hexadecimalSize
     * @return
     * @throws ConfigurationException 
     */
    public static String toHexString(int valueIndex, int hexadecimalSize) throws ConfigurationException {
        String result = Integer.toHexString(valueIndex);
        if (result.length() > hexadecimalSize) {
            throw new ConfigurationException("unable convert to hexadecimal with a maximum of " + hexadecimalSize + " characters");
        }
        
        if (result.length() < hexadecimalSize) {
            while (result.length() != hexadecimalSize) {
                result = '0' + result;
            }
        }
        return result;
    }
}
