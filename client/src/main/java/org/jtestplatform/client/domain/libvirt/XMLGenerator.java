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
    
    private static String baseMacAddress = "54:52:00:77:58:";
    static String baseIPAddress = "192.168.121.";
    static int min = 2;
    static int max = 254;
    static IPScanner IP_SCANNER = new IPScanner(baseIPAddress, min, max);
        
//    private static String baseMacAddress = "54:52:00:77:FF:";
//    static String baseIPAddress = "192.168.50.";
    
    public static String generateNetwork(String networkName) throws ConfigurationException {
        return generateDefaultNetwork();
/*        
        StringBuilder sb = new StringBuilder(4096);

        sb.append("<network>");
        sb.append("<name>").append(networkName).append("</name>");
        sb.append("<uuid>4720e2c6-97bc-6703-d109-64d8d561cfc9</uuid>");
        sb.append("<forward mode='nat'/>");
        sb.append("<bridge name='virbr1' stp='on' forwardDelay='0' />");
        sb.append("<ip address='").append(baseIPAddress).append("1' netmask='255.255.255.0'>");
        sb.append("<dhcp>");
        
        int min = 2;
        int max = 254;
        sb.append("<range start='").append(baseIPAddress).append(min).append("' end='").append(baseIPAddress).append(max).append("' />");        
        for (int i = min; i <= max; i++) {
            sb.append("<host mac=\"").append(baseMacAddress).append(Integer.toHexString(i));
            sb.append("\" name=\"jtestplatform").append(i);
            sb.append("\" ip=\"").append(baseIPAddress).append(i).append("\" />");
        }
        
        sb.append("</dhcp>");
        sb.append("</ip>");
        sb.append("</network>");
        
        
        String result = sb.toString();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("generateNetwork: networkName=" + networkName + " Result=\n" + result);
        }
                
        return result;
*/        
    }
    
    private static String generateDefaultNetwork() throws ConfigurationException {
        Network network = new Network();
        network.setName("default");
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
        ip.setAddress(baseIPAddress + '1');
        ip.setNetmask("255.255.255.0");
        network.setIp(ip);

        DHCP dhcp = new DHCP();
        ip.setDhcp(dhcp);

        Range range = new Range();
        range.setStart(baseIPAddress + min);
        range.setEnd(baseIPAddress + max);
        dhcp.setRange(range);

        for (int i = min; i <= max; i++) {
            Host host = new Host();
            host.setMac(baseMacAddress + Integer.toHexString(i));
            host.setIp(baseIPAddress + i);
            dhcp.addHost(host);
        }

        StringWriter writer = new StringWriter();
        try {
            new NetworkDom4jWriter().write(writer, network);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
        String result = writer.toString();

        LOGGER.debug("generateDefaultNetwork:\n" + result);
        return result;
    }
        
    public static String generate(String name, String uuid, String cdromFile, int netDevId, String networkName) {
        StringBuilder sb = new StringBuilder(4096);
/*        
        sb.append("<domain type='").append(type).append("'>");
        sb.append("  <name>").append(name).append("</name>");
        sb.append("  <uuid>4dea24b3-1d52-d8f3-2516-782e98a20000</uuid>");
        sb.append("  <memory>131072</memory>");
        sb.append("  <vcpu>1</vcpu>");
        sb.append("  <os>");
        sb.append("    <type arch=\"i686\">hvm</type>");
        sb.append("  </os>");
        sb.append("  <clock sync=\"localtime\"/>");
        sb.append("  <devices>");
        sb.append("    <emulator>/usr/bin/").append(type).append("</emulator>");
        sb.append("    <disk type='file' device='cdrom'>");
        sb.append("      <source file='").append(cdromFile).append("'/>");
        sb.append("      <target dev='hdc'/>");
        sb.append("      <readonly/>");
        sb.append("    </disk>");
        
        if (network != null) {
            sb.append(network);
        }
        
        sb.append("    <graphics type='vnc' port='-1' keymap='fr'/>");
        sb.append("  </devices>");
        sb.append("</domain>");
*/
        
        sb.append("<domain type='kvm' id='1'>");                                
        sb.append("  <name>").append(name).append("</name>");                                       
        sb.append("  <uuid>").append(uuid).append("</uuid>");       
        sb.append("  <memory>524288</memory>");    
        sb.append("  <currentMemory>524288</currentMemory>"); 
        sb.append("  <vcpu>1</vcpu>");
        sb.append("  <os>");
        sb.append("    <type arch='x86_64' machine='pc-0.11'>hvm</type>");
        sb.append("    <boot dev='cdrom'/>                                   ");
        sb.append("  </os>                                                   ");
        sb.append("<features>                                              ");
        sb.append("<acpi/>                                               ");
        sb.append("<apic/>                                               ");
        sb.append("<pae/>                                                ");
        sb.append("</features>                                             ");
        sb.append("<clock offset='utc'/>                                   ");
        sb.append("<on_poweroff>destroy</on_poweroff>                      ");
        sb.append("<on_reboot>restart</on_reboot>                          ");
        sb.append("        <on_crash>restart</on_crash>                            ");
        sb.append("<devices>                                               ");
        sb.append("<emulator>/usr/bin/kvm</emulator>                     ");
        sb.append("<disk type='file' device='cdrom'>                     ");
        sb.append("  <source file='").append(cdromFile).append("'/>");                                                                                        
        sb.append("  <target dev='hdc' bus='ide'/>                                                                   ");
        sb.append("  <readonly/>                                                                                     ");
        sb.append("</disk>                                                                                           ");

        sb.append("<interface type='network'>");
        sb.append("  <mac address='").append(baseMacAddress).append(Integer.toHexString(netDevId)).append("'/>");
        sb.append("  <source network='").append(networkName).append("'/>");
        sb.append("  <target dev='vnet0'/>");
        sb.append("</interface>");
        
        sb.append("<serial type='pty'>");
        sb.append("<source path='/dev/pts/7'/>");
        sb.append("<target port='0'/>");
        sb.append("</serial>");
        sb.append("<console type='pty' tty='/dev/pts/7'>");
        sb.append("<source path='/dev/pts/7'/>");
        sb.append("<target port='0'/>");
        sb.append("</console>");
        sb.append("<input type='mouse' bus='ps2'/>");
        sb.append("<graphics type='vnc' port='5900' autoport='yes' keymap='fr'/>");
        sb.append("<sound model='es1370'/>");
        sb.append("<video>");
        sb.append("<model type='cirrus' vram='9216' heads='1'/>");
        sb.append("</video>");
        sb.append("</devices>");
        sb.append("<seclabel type='dynamic' model='apparmor'>");
        sb.append("<label>libvirt-1557e204-10f8-3c1f-ac60-3dc6f46e85f5</label>");
        sb.append("<imagelabel>libvirt-1557e204-10f8-3c1f-ac60-3dc6f46e85f5</imagelabel>");
        sb.append("</seclabel>");
        sb.append("</domain>");
        
        String result = sb.toString();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("generate: name=" + name + " cdrom=" + cdromFile + " networkName=" + networkName + " Result=\n" + result);
        }
        
        return result;
    }
}
