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
import java.io.Reader;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.client.domain.DomainFactory;
import org.jtestplatform.client.utils.ConfigurationUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.DomainInfo.DomainState;

/**
 * Implementation of {@link DomainFactory} for <a href="http://libvirt.org/">libvirt</a>
 * 
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class LibVirtDomainFactory implements DomainFactory<LibVirtDomainConfig> {
    private static final Logger LOGGER = Logger.getLogger(LibVirtDomainFactory.class);

    private static final String NETWORK_NAME = "default";
    //private static final String NETWORK_NAME = "jtestplatform-network";
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "libvirt";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LibVirtDomainConfig readConfig(Properties properties, Reader reader) throws ConfigurationException {
        LibVirtDomainConfig config = new LibVirtDomainConfig();
        config.setFactory(this);
        //TODO read from file
        config.setType("kvm");
        config.setVmName("test");
        config.setCdrom(ConfigurationUtils.expandValue(properties, "${config.dir}/microcore_2.7.iso"));
        return config;
    }

    public LibVirtDomain createDomain(DomainConfig config) throws ConfigurationException {
        return new LibVirtDomain((LibVirtDomainConfig) config, this); 
    }

    /**
     * @param domain
     * @throws ConfigurationException 
     */
    String start(Domain domain) throws ConfigurationException {
        String ipAddress = null;
        try {
            domain.create();
            List<String> delta = XMLGenerator.IP_SCANNER.computeDelta();
            if (delta.isEmpty()) {
                throw new ConfigurationException("no new domain has been found");
            } else if (delta.size() == 1) {
                ipAddress = delta.get(0);
            } else {
                StringBuilder sb = new StringBuilder("found more than one new domain at following IP addresses : ");
                for (String ip : delta) {
                    sb.append(ip).append(',');
                }
                throw new ConfigurationException(sb.toString());
            }
        } catch (UnknownHostException e) {
            throw new ConfigurationException(e);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        } catch (LibvirtException e) {
            throw new ConfigurationException(e);
        }
        
        return ipAddress;
    }

    /**
     * @param domain
     * @param ipAddress
     * @throws ConfigurationException 
     */
    void stop(Domain domain, String ipAddress) throws ConfigurationException {
        try {
            //domain.shutdown(); //FIXME : doesn't work
            domain.destroy(); // destroy doesn't shutdown the VM
            
            DomainInfo info = domain.getInfo(); 
            while (info.state != DomainState.VIR_DOMAIN_SHUTOFF) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }            
        } catch (LibvirtException e) {
            throw new ConfigurationException(e);
        }
    }
    
    private void ensureNetworkExist(Connect connect) throws LibvirtException {
        //TODO create our own network
        
        Network network = networkLookupByName(connect, NETWORK_NAME);
        if (network != null) {
            try {
                //TODO avoid destroying it if it has already the desired characteristics
                network.destroy();
            } catch (LibvirtException lve) {
                // ignore
            }
            network.undefine();
            LOGGER.debug("destroyed network " + NETWORK_NAME);
        }
        
        String net = XMLGenerator.generateDefaultNetwork();
        network = connect.networkDefineXML(net);
        network.create();
        LOGGER.debug("created network " + NETWORK_NAME);
    }
    
    private static Network networkLookupByName(Connect connect, String networkName) throws LibvirtException {
        Network network = null;
        if (Arrays.asList(connect.listDefinedNetworks()).contains(networkName) || Arrays.asList(connect.listNetworks()).contains(networkName)) {
            network = connect.networkLookupByName(networkName);        
        }
        return network;
    }
    
    //TODO get it from config :
    //private List<String> uris = Collections.singletonList("qemu:///session"); // doesn't allow creation of a network
    private List<String> uris = Collections.singletonList("qemu:///system"); 
    
    private Map<String, Connect> connections = new HashMap<String, Connect>(uris.size());
    private int nextConnect = 0;
    private Connect getNextConnect() throws LibvirtException {
        //TODO implement pluggable strategy
        
        // simple round robin
        String uri = uris.get(nextConnect);       
        Connect connect = connections.get(uri);
        if (connect == null) {
            connect = new Connect(uri, false);
            connections.put(uri, connect);
            
            ensureNetworkExist(connect);
        }
                
        return connect;
    }
    
    protected void finalize() {
        for (Connect connect : connections.values()) {
            try {
                connect.close();
            } catch (LibvirtException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * @param type
     * @param config
     * @return
     * @throws ConfigurationException 
     */
    Domain defineDomain(String type, LibVirtDomainConfig config) throws ConfigurationException {
        try {
            Connect connect = getNextConnect();
            String xml = XMLGenerator.generate(type, config.getVmName(), config.getCdrom(), NETWORK_NAME);
            return connect.domainDefineXML(xml);
        } catch (LibvirtException e) {
            throw new ConfigurationException(e);
        }
    }    
}
