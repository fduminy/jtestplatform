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
package org.jtestplatform.client.domain;

import java.io.IOException;

/**
 * Interface with a domain.
 * 
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public interface Domain {
    
    /**
     * Start the VM.
     * @return The IP address of the domain. 
     * @throws ConfigurationException 
     */
    String start() throws IOException, ConfigurationException;
    
    /**
     * Stop the VM.
     * @throws IOException
     * @throws ConfigurationException 
     */
    void stop() throws IOException, ConfigurationException;
    
    /**
     * Checks if the VM is alive.
     * @return true if the VM is alive.
     * @throws IOException
     */
    boolean isAlive() throws IOException;
    
    /**
     * Get the IP address of the domain.
     * @return The IP address of the domain.
     */
    String getIPAddress();    
}
