/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
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

import com.sun.jna.Pointer;
import org.dom4j.DocumentException;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainFactory;
import org.jtestplatform.common.ConfigUtils;
import org.libvirt.*;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.jna.Libvirt.VirErrorCallback;
import org.libvirt.jna.virError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;

/**
 * Implementation of {@link DomainFactory} for <a href="http://libvirt.org/">libvirt</a>.
 * 
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class LibVirtDomainFactory implements DomainFactory<LibVirtDomain> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibVirtDomainFactory.class);

    private static final String NETWORK_NAME = "default";
    static final String DOMAIN_NAME_PREFIX = "JTestPlatform_";
    //private static final String NETWORK_NAME = "jtestplatform-network";
    private final ConnectManager connectManager = new ConnectManager();

    static {
        try {
            Connect.setErrorCallback(new VirErrorCallback() {           
                @Override
                public void errorCallback(Pointer pointer, virError error) {
                    LOGGER.error("pointer={} error={}", pointer, error);
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
    public boolean support(final Platform platform, final Connection connection) throws DomainException {
        return execute(connection, new ConnectManager.Command<Boolean>() {
            @Override
            public Boolean execute(Connect connect) throws Exception {
                return LibVirtModelFacade.support(platform, connect);
            }
        });
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

    final <T> T execute(org.jtestplatform.cloud.configuration.Connection connection, ConnectManager.Command<T> command) throws DomainException {
        return connectManager.execute(connection, command);
    }

    /**
     * @param domain
     * @throws DomainException 
     */
    String start(Domain domain) throws DomainException {
        String ipAddress = null;
        Network network = null;
        try {
            domain.create();
            
            String macAddress = getMacAddress(domain);
            if (macAddress == null) {
                throw new DomainException("unable to get mac address");
            }
            
            network = domain.getConnect().networkLookupByName(NETWORK_NAME);
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
        } finally {
            if (network != null) {
                try {
                    network.free();
                } catch (LibvirtException e) {
                    LOGGER.error("failed to free network", e);        
                }
            }
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
            Network network = null;
            
            try {
                network = networkLookupByName(connect, NETWORK_NAME);
                if (network != null) {
                    if (LibVirtModelFacade.sameNetwork(wantedNetworkXML, network)) {
                        LOGGER.debug("network '{}' already exists with proper characteristics", NETWORK_NAME);
                    } else {
                        network.destroy();
                        //network.undefine();
                        LOGGER.debug("destroyed network '{}'", NETWORK_NAME);
                        network = null; 
                    }
                }
                
                if (network == null) {
                    network = connect.networkCreateXML(wantedNetworkXML);
                    LOGGER.debug("created network '{}'", NETWORK_NAME);            
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
                return connect.domainDefineXML(xml);
            }
        } catch (LibvirtException e) {
            throw new DomainException(e);
        }
    }

    void releaseConnect(Connection connection) {
        connectManager.releaseConnect(connection);
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

        return findUniqueValue(domainNames, "domain name", DOMAIN_NAME_PREFIX, 0, MAX_VALUE, 8);
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
        
        LOGGER.debug("found a unique {} : {}", valueName, value);
        return value;
    }
    
    private List<Domain> listAllDomains(Connect connect) throws LibvirtException {
        List<Domain> domains = new ArrayList<Domain>();
    
        // get defined but inactive domains
        for (String name : connect.listDefinedDomains()) {
            LOGGER.debug("name={}", name);
            
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
