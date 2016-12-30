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

import org.dom4j.DocumentException;
import org.jtestplatform.cloud.configuration.Platform;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.model.capabilities.Arch;
import org.libvirt.model.capabilities.Capabilities;
import org.libvirt.model.capabilities.Guest;
import org.libvirt.model.capabilities.io.dom4j.CapabilitiesDom4jReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

import static org.jtestplatform.cloud.domain.libvirt.LibVirtUtils.STRICT;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class PlatformSupportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSupportManager.class);

    public boolean support(Platform platform, Connect connect) throws LibvirtException, IOException, DocumentException {
        LOGGER.trace("begin support");

        boolean support;
        try {
            support = (connect.nodeInfo().memory >= platform.getMemory());
        } catch (LibvirtException lve) {
            // an exception might be thrown if the function is not supported by the hypervisor
            // then, we assume there is enough memory

            LOGGER.error("Error while calling nodeInfo()", lve);
            support = true;
        }

        if (support) {

            support = false;
            outloop:
            for (Guest guest : getCapabilities(connect).getGuests()) {
                Arch arch = guest.getArch();

                boolean supportCPU = arch.getName().equals(platform.getCpu());
                boolean supportWordSize = (arch.getWordSize() == platform.getWordSize());
                if (supportCPU && supportWordSize) {
                    support = true;
                    break;
                    // FIXME add support for nbCores (using virConnectGetMaxVcpus(virConnectGetType) which doesn't seem accessible from java binding)
                    //                    for (Domain domain : guest.getArch().getDomains()) {
                    //                        boolean supportNbCores = (platform.getNbCores() <= connect.getMaxVcpus(domain.getType()));
                    //                        if (supportNbCores) {
                    //                            support = true;
                    //                            break outloop;
                    //                        }
                    //                    }
                }
            }
        }

        LOGGER.trace("end support");
        return support;
    }

    static Capabilities getCapabilities(Connect connect) throws LibvirtException, IOException, DocumentException {
        String capabilitiesXML = connect.getCapabilities();

        // FIXME when STRICT is set to true the tag 'uuid' throws an exception
        return new CapabilitiesDom4jReader().read(new StringReader(capabilitiesXML), STRICT);
    }
}
