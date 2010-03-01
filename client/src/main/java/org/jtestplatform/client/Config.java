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
package org.jtestplatform.client;

import java.io.File;
import java.util.List;

import org.jtestplatform.client.domain.DomainConfig;

/**
 * Class containing various parameters used to configure JTestServer.
 *  
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class Config {

    /**
     * Client side timeout for connection with the server.
     */
    private int clientTimeout;
    
    /**
     * Name of the server. <br>Examples : server.mydomain.org, 123.456.789.1
     */
    private String serverName;
    
    /**
     * Port of the server.
     */
    private int serverPort;
    
    /**
     * Work directory.
     */
    private File workDir;
    
    /**
     * Excluding filters, used to remove some tests from a list.
     */
    private String[] excludingFilters;
    
    /**
     * Time between 2 checks of the WatchDog.
     */
    private int watchDogPollInterval;
    
    /**
     * Configuration of the VM in which tests will actually be run.
     */
    private List<DomainConfig> vmConfigs;

    public int getClientTimeout() {
        return clientTimeout;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }
    
    public File getWorkDir() {
        return workDir;
    }
    
    public String[] getExcludingFilters() {
        return excludingFilters;
    }

    public int getWatchDogPollInterval() {
        return watchDogPollInterval;
    }

    public List<DomainConfig> getVMConfigs() {
        return vmConfigs;
    }

    public List<DomainConfig> getVmConfigs() {
        return vmConfigs;
    }

    public void setVmConfigs(List<DomainConfig> vmConfigs) {
        this.vmConfigs = vmConfigs;
    }

    public void setClientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    public void setExcludingFilters(String[] excludingFilters) {
        this.excludingFilters = excludingFilters;
    }

    public void setWatchDogPollInterval(int watchDogPollInterval) {
        this.watchDogPollInterval = watchDogPollInterval;
    }
}
