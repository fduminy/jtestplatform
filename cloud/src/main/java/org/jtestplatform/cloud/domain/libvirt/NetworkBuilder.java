/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.domain.DomainException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Network;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class NetworkBuilder {
    private final NetworkXMLBuilder networkXMLBuilder;

    NetworkBuilder(NetworkXMLBuilder networkXMLBuilder) {
        this.networkXMLBuilder = networkXMLBuilder;
    }

    Network build(Connect connect, NetworkConfig networkConfig) throws DomainException {
        try {
            return connect.networkDefineXML(networkXMLBuilder.build(networkConfig));
        } catch (LibvirtException e) {
            throw new DomainException(e);
        }
    }
}