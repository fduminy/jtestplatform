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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.client.domain.DomainFactory;
import org.jtestplatform.common.ConfigUtils;
import org.jtestplatform.configuration.Connection;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.jna.virError;
import org.libvirt.jna.Libvirt.VirErrorCallback;
import org.libvirt.model.Host;
import org.libvirt.model.io.dom4j.NetworkDom4jReader;

import com.sun.jna.Pointer;

/**
 * Implementation of {@link DomainFactory} for <a href="http://libvirt.org/">libvirt</a>
 * 
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class LibVirtDomainFactory implements DomainFactory<LibVirtDomain> {
    private static final Logger LOGGER = Logger.getLogger(LibVirtDomainFactory.class);

    private static final String NETWORK_NAME = "default";
    //private static final String NETWORK_NAME = "jtestplatform-network";
    
    static {
        try {
            Connect.setErrorCallback(new VirErrorCallback() {           
                @Override
                public void errorCallback(Pointer pointer, virError error) {
                    LOGGER.error("pointer=" + pointer + " error=" + error);
                }
            });
        } catch (LibvirtException e) {
            LOGGER.error("failed to initialize error callback", e);
        }
    }
    
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
    public LibVirtDomain createDomain(DomainConfig config, Connection connection) throws ConfigurationException {
        return new LibVirtDomain(config, this, connection); 
    }

    /**
     * @param domain
     * @throws ConfigurationException 
     */
    String start(Domain domain) throws ConfigurationException {
        String ipAddress = null;
        try {
            domain.create();
            
            String macAddress = getMacAddress(domain);
            if (macAddress == null) {
                throw new ConfigurationException("unable to get mac address");
            }
            
            Network network = domain.getConnect().networkLookupByName(NETWORK_NAME);
            org.libvirt.model.Network net = new NetworkDom4jReader().read(new StringReader(network.getXMLDesc(0)));
            for (Host host : net.getIp().getDhcp().getHost()) {
                if (macAddress.equals(host.getMac())) {
                    ipAddress = host.getIp();
                    break;
                }
            }
            if (ipAddress == null) {
                throw new ConfigurationException("unable to get mac address");
            }
        } catch (LibvirtException e) {
            throw new ConfigurationException(e);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        } catch (DocumentException e) {
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
            domain.destroy();
            
            DomainInfo info = domain.getInfo();
            while (info.state != DomainState.VIR_DOMAIN_SHUTOFF) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            
            domain.undefine();
        } catch (LibvirtException e) {
            throw new ConfigurationException(e);
        }
    }
    
    void ensureNetworkExist(Connect connect) throws LibvirtException, ConfigurationException {
        //TODO create our own network
        
        Network network = networkLookupByName(connect, NETWORK_NAME);
        if (network != null) {
            try {
                //TODO avoid destroying it if it has already the desired characteristics
                network.destroy();
            } catch (LibvirtException lve) {
                // ignore
            }
            //network.undefine();
            LOGGER.debug("destroyed network " + NETWORK_NAME);
        }
        
        String net = XMLGenerator.generateNetwork(NETWORK_NAME);
        network = connect.networkCreateXML(net);
        //network = connect.networkDefineXML(net);
        //network.create();
        LOGGER.debug("created network " + NETWORK_NAME);
    }
    
    private static Network networkLookupByName(Connect connect, String networkName) throws LibvirtException {
        Network network = null;
        if (Arrays.asList(connect.listDefinedNetworks()).contains(networkName) || Arrays.asList(connect.listNetworks()).contains(networkName)) {
            network = connect.networkLookupByName(networkName);        
        }        
        return network;
    }
    
    /**
     * @param config
     * @return
     * @throws ConfigurationException 
     */
    Domain defineDomain(Connect connect, DomainConfig config) throws ConfigurationException {
        try {
            List<Domain> domains = listAllDomains(connect);
            
            if (ConfigUtils.isBlank(config.getDomainName())) {
                // automatically define the domain name
                // it must be unique for the connection
                config.setDomainName(findUniqueDomainName(domains));
            }
            
            String macAddress = findUniqueMacAddress(domains);
            String xml = XMLGenerator.generateDomain(config.getDomainName(), config.getCdrom(), macAddress, NETWORK_NAME);
            return connect.domainDefineXML(xml);
        } catch (LibvirtException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Automatically define the domain name. It must be unique for the connection.
     * @param domains
     * @return
     * @throws LibvirtException
     * @throws ConfigurationException 
     */
    private String findUniqueDomainName(List<Domain> domains) throws LibvirtException, ConfigurationException {        
        List<String> domainNames = new ArrayList<String>(domains.size());
        for (Domain domain : domains) {
            domainNames.add(domain.getName());
        }
        String prefix = "JTestPlatform_";
        
        return findUniqueValue(domainNames, "domain name", prefix, (int) System.currentTimeMillis(), Integer.MAX_VALUE, 8);
    }

    /**
     * Automatically define the mac address. It must be unique for the connection.
     * @param domains
     * @return
     * @throws LibvirtException
     * @throws ConfigurationException 
     */
    private String findUniqueMacAddress(List<Domain> domains) throws LibvirtException, ConfigurationException {
        List<String> macAddresses = new ArrayList<String>();        
        for (Domain domain : domains) {
            String addr = getMacAddress(domain);
            if (addr != null) {
                macAddresses.add(addr);
            }
        }
        
        String prefix = XMLGenerator.BASE_MAC_ADDRESS;
        
        return findUniqueValue(macAddresses, "mac address", prefix, XMLGenerator.MIN_SUBNET_IP_ADDRESS, XMLGenerator.MAX_SUBNET_IP_ADDRESS, 2);
    }
    
    private String getMacAddress(Domain domain) throws LibvirtException {
        String macAddress = null;
        
        String xml = domain.getXMLDesc(0);
        
        //TODO it's bad, we should use an xml parser. create and add it in the libvirt-model project.
        String begin = "<mac address='";
        int idx = xml.indexOf(begin);
        if (idx >= 0) {
            idx += begin.length();
            int idx2 = xml.indexOf('\'', idx);
            if (idx2 >= 0) {
                macAddress = xml.substring(idx, idx2);
            }
        }
        
        return macAddress;
    }

    /**
     * Find a unique value that is not yet in the given list of values.
     * @param values
     * @param valueName
     * @param valuePrefix
     * @param valueIndex
     * @param maxValueIndex
     * @throws ConfigurationException 
     */
    private String findUniqueValue(List<String> values, String valueName, String valuePrefix, int valueIndex, int maxValueIndex, int hexadecimalSize) throws ConfigurationException {
        String value = null;
        for (; valueIndex <= maxValueIndex; valueIndex++) {
            String indexStr = XMLGenerator.toHexString(valueIndex, hexadecimalSize);
            
            value = valuePrefix + indexStr;
            if (!values.contains(value)) {
                break;
            }
        }
        
        if ((maxValueIndex > maxValueIndex) || (value == null)) {
            throw new ConfigurationException("unable to find a unique " + valueName);
        }
        
        LOGGER.debug("found a unique " + valueName + " : " + value);
        return value;
    }
    
    private List<Domain> listAllDomains(Connect connect) throws LibvirtException {
        List<Domain> domains = new ArrayList<Domain>();
    
        // get defined but inactive domains
        for (String name : connect.listDefinedDomains()) {
            domains.add(connect.domainLookupByName(name));
        }
        
        // get active domains
        for (int id : connect.listDomains()) {
            domains.add(connect.domainLookupByID(id));
        }
        
        return domains;
    }
}
