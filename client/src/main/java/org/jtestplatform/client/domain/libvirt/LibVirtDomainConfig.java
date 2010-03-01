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

import org.jtestplatform.client.domain.DomainConfig;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class LibVirtDomainConfig implements DomainConfig {
    private String cdrom;
    private String name;
    private String type;
    private LibVirtDomainFactory factory;
    
    @Override
    public String getVmName() {
        return name;
    }

    @Override
    public LibVirtDomainFactory getFactory() {
        return factory;
    }

    /**
     * @return
     */
    public String getCdrom() {
        return cdrom;
    }

    /**
     * @return
     */
    public String getType() {
        return type;
    }

    public void setCdrom(String cdrom) {
        this.cdrom = cdrom;
    }

    public void setVmName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFactory(LibVirtDomainFactory factory) {
        this.factory = factory;
    }

    
}
