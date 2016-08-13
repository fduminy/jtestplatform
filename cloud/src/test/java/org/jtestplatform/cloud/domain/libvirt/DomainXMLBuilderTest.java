package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.libvirt.DomainXMLBuilder.BEGIN_TAG;
import static org.jtestplatform.cloud.domain.libvirt.DomainXMLBuilder.END_TAG;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class DomainXMLBuilderTest {
    @Test
    public void build_domain1() throws DomainException {
        build("domain1", "cdrom1", 12, 34, "12:34", "network1");
    }

    @Test
    public void build_domain2() throws DomainException {
        build("domain2", "cdrom2", 56, 78, "56:78", "network2");
    }

    private void build(String domainName, String cdrom, long memory, int nbCores, String macAddress, String networkName)
        throws DomainException {
        DomainConfig config = new DomainConfig();
        Platform platform = new Platform();
        platform.setMemory(memory);
        platform.setNbCores(nbCores);
        platform.setCdrom(cdrom);
        config.setPlatform(platform);
        config.setDomainName(domainName);
        DomainXMLBuilder builder = new DomainXMLBuilder();

        String actual = builder.build(config, macAddress, networkName);

        assertThat(actual).isNotNull().isNotEmpty().doesNotContain(Character.toString(BEGIN_TAG))
                          .doesNotContain(Character.toString(END_TAG));
        assertThat(actual).contains(macAddress).contains(networkName);
    }
}