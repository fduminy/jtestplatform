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
