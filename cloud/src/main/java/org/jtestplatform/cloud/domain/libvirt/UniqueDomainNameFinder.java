/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.domain.DomainException;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class UniqueDomainNameFinder extends UniqueValueFinder {

    static final String DOMAIN_NAME_PREFIX = "JTestPlatform_";

    /**
     * Automatically define the domain name. It must be unique for the connection.
     * @throws LibvirtException
     * @throws DomainException
     */
    String findUniqueDomainName(List<Domain> domains) throws LibvirtException, DomainException {
        List<String> domainNames = new ArrayList<String>(domains.size());
        for (Domain domain : domains) {
            domainNames.add(domain.getName());
        }

        return findUniqueValue(domainNames, "domain name", DOMAIN_NAME_PREFIX, 0, 0xFF, 2);
    }
}
