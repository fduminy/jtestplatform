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


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class XMLGenerator {
    public static String generate(String type, String name, String cdromFile, String networkName) {
        return generate(type, name, "1557e204-10f8-3c1f-ac60-3dc6f46e85f9", cdromFile, 0, networkName);
    }
    
    private static String baseMacAddress = "54:52:00:77:58:";
    static String baseIPAddress = "192.168.122.";
    static int min = 2;
    static int max = 254;
    static IPScanner IP_SCANNER = new IPScanner(baseIPAddress, min, max);
        
//    private static String baseMacAddress = "54:52:00:77:FF:";
//    static String baseIPAddress = "192.168.50.";
    
    public static String generateNetwork(String networkName) {
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
        
        
        return sb.toString();
    }
    
    public static String generateDefaultNetwork() {
        StringBuilder sb = new StringBuilder(4096);
        
        sb.append("<network>");
        sb.append("<name>default</name>");
        sb.append("<uuid>ec6f8ce7-ad0f-eea7-d9ed-dc460cf47023</uuid>");
        sb.append("<forward mode='nat'/>");
        sb.append("<bridge name='virbr1' stp='on' forwardDelay='0' />");
        sb.append("<ip address='").append(baseIPAddress).append("1' netmask='255.255.255.0'>");
        sb.append("<dhcp>");
        sb.append("<range start='").append(baseIPAddress).append(min).append("' end='").append(baseIPAddress).append(max).append("' />");        
        //sb.append("<range start='192.168.122.2' end='192.168.122.3' />");
        sb.append("</dhcp>");
        sb.append("</ip>");
        sb.append("</network>");        
            
      return sb.toString();
    }
    
    public static String generate(String type, String name, String uuid, String cdromFile, int netDevId, String networkName) {
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
        sb.append("        <name>").append(name).append("</name>");                                       
        sb.append("<uuid>").append(uuid).append("</uuid>");       
        sb.append("<memory>524288</memory>                             ");    
        sb.append("<currentMemory>524288</currentMemory>                  "); 
        sb.append("<vcpu>1</vcpu>                                          ");
        sb.append("<os>                                                    ");
        sb.append("<type arch='x86_64' machine='pc-0.11'>hvm</type>      ");
        sb.append("<boot dev='cdrom'/>                                   ");
        sb.append("</os>                                                   ");
        sb.append("<features>                                              ");
        sb.append("<acpi/>                                               ");
        sb.append("<apic/>                                               ");
        sb.append("          <pae/>                                                ");
        sb.append("</features>                                             ");
        sb.append("<clock offset='utc'/>                                   ");
        sb.append("<on_poweroff>destroy</on_poweroff>                      ");
        sb.append("<on_reboot>restart</on_reboot>                          ");
        sb.append("        <on_crash>restart</on_crash>                            ");
        sb.append("<devices>                                               ");
        sb.append("<emulator>/usr/bin/kvm</emulator>                     ");
        sb.append("<disk type='file' device='cdrom'>                     ");
        sb.append("<source file='/home/fabien/data/Projets/jtestplatform/client/src/test/resources/home/config/microcore_2.7.iso'/>");                                                                                        
        sb.append(" <target dev='hdc' bus='ide'/>                                                                   ");
        sb.append("<readonly/>                                                                                     ");
        sb.append("</disk>                                                                                           ");
//        sb.append("<interface type='network'>                                                                        ");
//        sb.append("<mac address='54:52:00:77:58:88'/>                                                              ");
//        sb.append("<source network='default'/>                                                                     ");
//        sb.append("<target dev='vnet0'/>                                                                           ");
//        sb.append("</interface>");
//        if (network != null) {
//            sb.append(network);
//        }

        sb.append("        <interface type='network'>");
        sb.append("        <mac address='").append(baseMacAddress).append(Integer.toHexString(netDevId)).append("'/>");
        sb.append("<source network='").append(networkName).append("'/>");
        sb.append("<target dev='vnet0'/>");
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
        
        return sb.toString();        
    }
}
