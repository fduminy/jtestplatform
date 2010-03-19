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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.client.domain.DomainException;
import org.jtestplatform.client.domain.DomainFactory;
import org.jtestplatform.common.ConfigUtils;
import org.jtestplatform.configuration.Connection;
import org.jtestplatform.configuration.Platform;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.jna.virError;
import org.libvirt.jna.Libvirt.VirErrorCallback;

import com.sun.jna.Pointer;

/**
 * Implementation of {@link DomainFactory} for <a href="http://libvirt.org/">libvirt</a>.
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
    @Override
    public boolean support(Platform platform, Connection connection) throws DomainException {
        LOGGER.debug("begin support"); //FIXME the trace level doesn't compile !
        boolean support = false;
        
        Connect connect = null;
        try {
            connect = org.jtestplatform.client.domain.libvirt.ConnectManager.getConnect(connection);
            support = LibVirtModelFacade.support(platform, connect);
        } catch (LibvirtException e) {
            throw new DomainException(e);
        } catch (IOException e) {
            throw new DomainException(e);
        } catch (DocumentException e) {
            throw new DomainException(e);
        } finally {
            ConnectManager.releaseConnect(connection);
        }
        
        LOGGER.debug("end support"); //FIXME the trace level doesn't compile !        
        return support;
    }
    
    /**
     * {@inheritDoc}
     */
    public LibVirtDomain createDomain(DomainConfig config, Connection connection) throws DomainException {
        if (!support(config.getPlatform(), connection)) {
            throw new DomainException("Unsupported platform :\n" + config.getPlatform() + "\n. You should call support(Platform, Connection) before.");
        }
        return new LibVirtDomain(config, this, connection); 
    }

    /**
     * @param domain
     * @throws DomainException 
     */
    String start(Domain domain) throws DomainException {
        String ipAddress = null;
        try {
            domain.create();
            
            String macAddress = getMacAddress(domain);
            if (macAddress == null) {
                throw new DomainException("unable to get mac address");
            }
            
            Network network = domain.getConnect().networkLookupByName(NETWORK_NAME);
            ipAddress = LibVirtModelFacade.getIPAddress(network, macAddress);
            if (ipAddress == null) {
                throw new DomainException("unable to get ip address");
            }
        } catch (LibvirtException e) {
            throw new DomainException(e);
        } catch (IOException e) {
            throw new DomainException(e);
        } catch (DocumentException e) {
            throw new DomainException(e);
        }
        
        return ipAddress;
    }

    /**
     * @param domain
     * @param ipAddress
     * @throws DomainException 
     */
    void stop(Domain domain, String ipAddress) throws DomainException {
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
            throw new DomainException(e);
        }
    }
    
    void ensureNetworkExist(Connect connect) throws LibvirtException, DomainException {
        //TODO create our own network

        String wantedNetworkXML = LibVirtModelFacade.generateNetwork(NETWORK_NAME);
            
        // synchronize because multiple threads might check/destroy/create the network concurrently
        synchronized ((connect.getHostName() + "_ensureNetworkExist").intern()) {   
            Network network = networkLookupByName(connect, NETWORK_NAME);
            if (network != null) {
                if (LibVirtModelFacade.sameNetwork(wantedNetworkXML, network)) {
                    LOGGER.debug("network '" + NETWORK_NAME + "' already exists with proper characteristics");
                } else {
                    network.destroy();
                    //network.undefine();
                    LOGGER.debug("destroyed network '" + NETWORK_NAME + "'");
                    network = null; 
                }
            }
            
            if (network == null) {
                network = connect.networkCreateXML(wantedNetworkXML);
                LOGGER.debug("created network '" + NETWORK_NAME + "'");            
            }
        }
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
     * @throws DomainException 
     */
    Domain defineDomain(Connect connect, DomainConfig config) throws DomainException {
        try {
            synchronized ((connect.getHostName() + "_defineDomain").intern()) {
                List<Domain> domains = listAllDomains(connect);
                
                if (ConfigUtils.isBlank(config.getDomainName())) {
                    // automatically define the domain name
                    // it must be unique for the connection
                    config.setDomainName(findUniqueDomainName(domains));
                }
                
                String macAddress = findUniqueMacAddress(domains);
                String xml = LibVirtModelFacade.generateDomain(config, macAddress, NETWORK_NAME);
                return connect.domainDefineXML(xml); //TODO call Domain.free() when no more needed
            }
        } catch (LibvirtException e) {
            throw new DomainException(e);
        }
    }

    /**
     * Automatically define the domain name. It must be unique for the connection.
     * @param domains
     * @return
     * @throws LibvirtException
     * @throws DomainException 
     */
    private String findUniqueDomainName(List<Domain> domains) throws LibvirtException, DomainException {        
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
     * @throws DomainException 
     */
    private String findUniqueMacAddress(List<Domain> domains) throws LibvirtException, DomainException {
        List<String> macAddresses = new ArrayList<String>();        
        for (Domain domain : domains) {
            String addr = getMacAddress(domain);
            if (addr != null) {
                macAddresses.add(addr);
            }
        }
        
        String prefix = LibVirtModelFacade.BASE_MAC_ADDRESS;
        
        return findUniqueValue(macAddresses, "mac address", prefix, LibVirtModelFacade.MIN_SUBNET_IP_ADDRESS, LibVirtModelFacade.MAX_SUBNET_IP_ADDRESS, 2);
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
     * @throws DomainException 
     */
    private String findUniqueValue(List<String> values, String valueName, String valuePrefix, int valueIndex, int maxValueIndex, int hexadecimalSize) throws DomainException {
        String value = null;
        for (; valueIndex <= maxValueIndex; valueIndex++) {
            String indexStr = LibVirtModelFacade.toHexString(valueIndex, hexadecimalSize);
            
            value = valuePrefix + indexStr;
            if (!values.contains(value)) {
                break;
            }
        }
        
        if ((maxValueIndex > maxValueIndex) || (value == null)) {
            throw new DomainException("unable to find a unique " + valueName);
        }
        
        LOGGER.debug("found a unique " + valueName + " : " + value);
        return value;
    }
    
    private List<Domain> listAllDomains(Connect connect) throws LibvirtException {
        List<Domain> domains = new ArrayList<Domain>();
    
        // get defined but inactive domains
        for (String name : connect.listDefinedDomains()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("name=" + name);
            }
            if (name != null) {
                domains.add(connect.domainLookupByName(name));
            }
        }
        
        // get active domains
        for (int id : connect.listDomains()) {
            domains.add(connect.domainLookupByID(id));
        }
        
        return domains;
    }
}
