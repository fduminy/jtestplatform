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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.Domain;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.DomainInfo.DomainState;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestLibVirt {
    private LibVirtDomainFactory factory;
    private Domain p;
    
    @Before
    public void setUp() throws ConfigurationException {
        factory = new LibVirtDomainFactory();
    }
    
    public void tearDown() throws IOException, ConfigurationException {
        if (p != null) {
            p.stop();
        }
    }
    
    @Test
    public void testStart() throws ConfigurationException, IOException {
        p = factory.createDomain(getVMConfig());
        p.start();
    }
    
    @Test
    public void testStop() throws ConfigurationException, IOException {
        p = factory.createDomain(getVMConfig());
        p.start();
        p.stop();
    }
    
    @Test
    public void testNodeInfo() throws LibvirtException {
        String uri = "qemu:///session"; 
        Connect connection = new Connect(uri, false);
        System.out.println("infos=" + connection.nodeInfo().toString());
        System.out.println("cores=" + connection.nodeInfo().cores);
        System.out.println("cpus=" + connection.nodeInfo().cpus);
        System.out.println("memory=" + connection.nodeInfo().memory);
        System.out.println("mhz=" + connection.nodeInfo().mhz);
        System.out.println("model=" + connection.nodeInfo().model);
        System.out.println("nodes=" + connection.nodeInfo().nodes);
        System.out.println("sockets=" + connection.nodeInfo().sockets);
        System.out.println("threads=" + connection.nodeInfo().threads);
    }

    @Ignore
    public void testXMLGenerator() throws ConfigurationException, UnknownHostException, IOException {
        String ipBase = XMLGenerator.baseIPAddress;        
        //int begin = 19;
        int begin = 3;

        //pingAll(ipBase);
        IPScanner scanner = new IPScanner(ipBase, 2, 254);
        scanner.computeDelta();
        
        try {
            String uri = "qemu:///system"; // for system wide domains
            //String uri = "qemu:///session"; // for user wide domains
            Connect connect = new Connect(uri, false);

//            for (String net : connect.listDefinedNetworks()) {
//                System.out.println("destroying network " + net);
//                connect.networkLookupByName(net).undefine();
//            }
//            for (String itf : connect.listDefinedInterfaces()) {
//                System.out.println("destroying interface " + itf);
//                connect.interfaceLookupByName(itf).destroy();
//            }
            
//            String networkName = "default";
//            //String networkName = "jtestplatform-network";
//            Network network = networkLookupByName(connect, networkName);
//            if (network != null) {
//                try {
//                    network.destroy();
//                } catch (LibvirtException lve) {
//                    // ignore
//                }
//                network.undefine();
//                System.out.println("destroyed network " + networkName);
//                
//                String net = XMLGenerator.generateDefaultNetwork();
//                network = connect.networkDefineXML(net);
//                network.create();
//                System.out.println("created network " + networkName);
//            }
                        
            Network network = null; //TODO get it
            org.libvirt.Domain d1 = createDomain(connect, ipBase + begin, 1, network);
            System.out.println("created domain 1");
            org.libvirt.Domain d2 = createDomain(connect, ipBase + (begin + 1), 2, network);
            System.out.println("created domain 2");
        } catch (LibvirtException e) {
            System.err.println(e.getMessage());            
            throw new ConfigurationException("failed to connect with libvirt", e);
        }

        // wait a bit because tinycore is waiting for boot options
        System.out.println("waiting start");
        try {
            Thread.sleep(40000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        System.out.println("pinging domains");
        List<String> delta = scanner.computeDelta();
        for (String ip : delta) {
            System.out.println("new domain at " + ip);
        }
    }

    private org.libvirt.Domain createDomain(Connect connect, String ipAddress, int id, Network network) throws ConfigurationException {
        org.libvirt.Domain domain = null;
        try {
            //Network n = null;
            
//            try {
//                //n = connect.networkLookupByName("default");
//                //n.create();
//            } catch (LibvirtException lve) {
//                //n = connect.networkCreateXML(XMLGenerator.generateNetwork(networkName, "virbr2", ipAddress));
//            }
            
            //assertNotNull(n);
            //Network n = connection.networkLookupByUUIDString("50d17ad3-3c52-045e-9513-c75455f3a78d");
            
            LibVirtDomainConfig cfg = getVMConfig();
                            
            String domainName = "testFabien" + id;
            domain = domainLookupByName(connect, domainName);            
            if (domain != null) {
                if (DomainState.VIR_DOMAIN_RUNNING.equals(domain.getInfo().state)) {
                    domain.shutdown();
                    domain.destroy();
                    domain.free();
                }
            }
            
            String networkName = network.getName();
            String xml = XMLGenerator.generate(cfg.getType(), domainName, "1557e204-10f8-3c1f-ac60-3dc6f46e85f" + id, cfg.getCdrom(), id, networkName);            
            domain = connect.domainDefineXML(xml);
            //domain.destroy();
            if (domain.getInfo().state == DomainState.VIR_DOMAIN_RUNNING) {
                domain.destroy();
            }
            domain.create();
            
            assertTrue(domain.getInfo().state == DomainState.VIR_DOMAIN_RUNNING);            
        } catch (LibvirtException e) {
            System.err.println(e.getMessage());
            throw new ConfigurationException("failed to connect with libvirt", e);
        }
        return domain;
    }
    
    private static org.libvirt.Domain domainLookupByName(Connect connect, String domainName) throws LibvirtException {
        org.libvirt.Domain domain = null;
        if (Arrays.asList(connect.listDefinedDomains()).contains(domainName) || Arrays.asList(connect.listDomains()).contains(domainName)) {
            domain = connect.domainLookupByName(domainName);
        }
        return domain;
    }

//    private static Network getOrCreateNetwork(Connect connect, String name, String ip) throws LibvirtException {        
//        Network network = null;
//        for (String n : connect.listDefinedNetworks()) {
//            System.out.println(n);
//            if (n.equals(name)) {
//                network = connect.networkLookupByName(name);
//                network.undefine();
//                //network.destroy();
//                network = null;
//            }
//        }
//        
//        if (network == null) {
//            network = connect.networkDefineXML(XMLGenerator.generateNetwork(name, name, ip));
//        }
//        
//        System.out.println(network.getXMLDesc(0));
//        return network;
//    }

    /**
     * @return
     */
    private LibVirtDomainConfig getVMConfig() {
        LibVirtDomainConfig config = new LibVirtDomainConfig();
        LibVirtDomainFactory factory = new LibVirtDomainFactory();
        config.setFactory(factory);
        config.setType(factory.getType());
        
        config.setType("kvm");
        config.setVmName("test");
        //config.setCdrom(ConfigurationUtils.expandValue(properties, "${config.dir}/microcore_2.7.iso"));
        return config;
    }    
}
