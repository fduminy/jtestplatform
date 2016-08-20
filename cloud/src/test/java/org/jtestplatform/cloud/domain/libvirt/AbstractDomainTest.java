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

import org.junit.Before;
import org.junit.Rule;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
abstract public class AbstractDomainTest {
    @Mock protected Domain domain1;
    @Mock protected Domain domain2;
    @Mock protected Domain domain3;
    @Mock protected Domain domain4;
    protected List<Domain> domains;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUpDomains() throws Exception {
        mockDomain(domain1, 1);
        mockDomain(domain2, 2);
        mockDomain(domain3, 3);
        mockDomain(domain4, 4);

        domains = Arrays.asList(domain1, domain2, domain3, domain4);
    }

    private static void mockDomain(Domain domain, int id) throws LibvirtException {
        when(domain.getID()).thenReturn(id);
        when(domain.getName()).thenReturn("domain" + id);
    }
}
