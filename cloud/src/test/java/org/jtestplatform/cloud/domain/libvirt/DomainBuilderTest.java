package org.jtestplatform.cloud.domain.libvirt;

import org.assertj.core.api.JUnitSoftAssertions;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
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
@RunWith(Theories.class)
public class DomainBuilderTest {
    private static final String MAC_ADDRESS = "12:34:56:78";
    private static final String NETWORK_NAME = "networkName";
    private static final String DOMAIN_XML = "domainXML";
    private static final String EXPECTED_DOMAIN_NAME = "domain5";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public JUnitSoftAssertions soft = new JUnitSoftAssertions();

    @Mock
    private DomainXMLBuilder domainXMLBuilder;
    @Mock
    private UniqueMacAddressFinder macAddressFinder;
    @Mock
    private UniqueDomainNameFinder domainNameFinder;
    @Mock
    private Connect connect;
    @Mock
    private Domain domain1;
    @Mock
    private Domain domain2;
    @Mock
    private Domain domain3;
    @Mock
    private Domain domain4;
    @Mock
    private Domain expectedDomain;
    @Mock
    private NetworkConfig networkConfig;

    private DomainConfig domainConfig;
    private List<Domain> domains;

    @Before
    public void setUp() throws Exception {
        mockDomain(domain1, 1);
        mockDomain(domain2, 2);
        mockDomain(domain3, 3);
        mockDomain(domain4, 4);

        domains = Arrays.asList(domain1, domain2, domain3, domain4);
        domainConfig = new DomainConfig();

        when(connect.listDefinedDomains()).thenReturn(new String[] { "domain1", "domain2" });
        when(connect.domainLookupByName("domain1")).thenReturn(domain1);
        when(connect.domainLookupByName("domain2")).thenReturn(domain2);
        when(connect.listDomains()).thenReturn(new int[] { 3, 4 });
        when(connect.domainLookupByID(3)).thenReturn(domain3);
        when(connect.domainLookupByID(4)).thenReturn(domain4);
        when(macAddressFinder.findUniqueMacAddress(domains)).thenReturn(MAC_ADDRESS);
        when(networkConfig.getNetworkName()).thenReturn(NETWORK_NAME);
        when(domainXMLBuilder.build(domainConfig, MAC_ADDRESS, NETWORK_NAME)).thenReturn(DOMAIN_XML);
        when(connect.domainDefineXML(DOMAIN_XML)).thenReturn(expectedDomain);
    }

    @Theory
    public void defineDomain(boolean nameWasDefined) throws Exception {
        if (nameWasDefined) {
            domainConfig.setDomainName(EXPECTED_DOMAIN_NAME);
        } else {
            when(domainNameFinder.findUniqueDomainName(domains)).thenReturn(EXPECTED_DOMAIN_NAME);
        }
        DomainBuilder builder = new DomainBuilder(domainXMLBuilder, macAddressFinder, domainNameFinder);

        Domain domain = builder.defineDomain(connect, domainConfig, networkConfig);

        soft.assertThat(domain).as("domain").isSameAs(expectedDomain);
        soft.assertThat(domainConfig.getDomainName()).as("domainConfig.domainName").isEqualTo(EXPECTED_DOMAIN_NAME);
    }

    private static void mockDomain(Domain domain, int id) throws LibvirtException {
        when(domain.getID()).thenReturn(id);
        when(domain.getName()).thenReturn("domain" + id);
    }

}