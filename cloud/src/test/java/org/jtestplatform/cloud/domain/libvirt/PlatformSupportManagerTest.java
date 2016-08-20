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

import org.jtestplatform.cloud.configuration.Platform;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.libvirt.model.capabilities.Arch;
import org.libvirt.model.capabilities.Capabilities;
import org.libvirt.model.capabilities.Guest;
import org.libvirt.model.capabilities.io.dom4j.CapabilitiesDom4jWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.libvirt.PlatformSupportManagerTest.ValueDifference.MORE;
import static org.jtestplatform.cloud.domain.libvirt.PlatformSupportManagerTest.ValueDifference.SAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
@RunWith(Theories.class)
public class PlatformSupportManagerTest {
    @Theory
    public void support_memory(ValueDifference memoryDifference) throws Exception {
        Platform platform = createPlatform(true, true, false);
        Connect connect = mockConnect(true, true, (int) (platform.getMemory() + memoryDifference.diff * 1024));
        PlatformSupportManager manager = new PlatformSupportManager();

        assertThat(manager.support(platform, connect))
            .isEqualTo((memoryDifference == SAME) || (memoryDifference == MORE));
    }

    @Theory
    public void support_cpu(boolean sameCPU) throws Exception {
        Platform platform = createPlatform(true, true, false);
        Connect connect = mockConnect(sameCPU, true, getMemory(false));
        PlatformSupportManager manager = new PlatformSupportManager();

        assertThat(manager.support(platform, connect)).isEqualTo(sameCPU);
    }

    @Theory
    public void support_wordSize(boolean sameWordSize) throws Exception {
        Platform platform = createPlatform(true, true, false);
        Connect connect = mockConnect(true, sameWordSize, getMemory(false));
        PlatformSupportManager manager = new PlatformSupportManager();

        assertThat(manager.support(platform, connect)).isEqualTo(sameWordSize);
    }

    enum ValueDifference {
        MORE(+1),
        SAME(0),
        LESS(-1);

        private final int diff;

        ValueDifference(int diff) {
            this.diff = diff;
        }
    }

    private static Platform createPlatform(boolean cpu1, boolean wordSize32bits, boolean memoryOneMB) {
        Platform platform = new Platform();
        platform.setCpu(getCpu(cpu1));
        platform.setWordSize(getWordSize(wordSize32bits));
        platform.setMemory(getMemory(memoryOneMB));
        return platform;
    }

    private static int getMemory(boolean memoryOneMB) {
        return memoryOneMB ? 1024 : 2048;
    }

    private static String getCpu(boolean cpu1) {
        return cpu1 ? "cpu1" : "cpu2";
    }

    private static int getWordSize(boolean wordSize32bits) {
        return wordSize32bits ? 32 : 64;
    }

    private static Connect mockConnect(boolean cpu1, boolean wordSize32bits, int memoryInKB)
        throws IOException, LibvirtException {
        StringWriter capabilitiesXML = new StringWriter();
        Capabilities capabilities = new Capabilities();
        Guest guest = new Guest();
        Arch arch = new Arch();
        arch.setWordSize(getWordSize(wordSize32bits));
        arch.setName(getCpu(cpu1));
        guest.setArch(arch);
        capabilities.addGuest(guest);
        new CapabilitiesDom4jWriter().write(capabilitiesXML, capabilities);

        Connect connect = mock(Connect.class);
        when(connect.getCapabilities()).thenReturn(capabilitiesXML.toString());
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.memory = memoryInKB;
        when(connect.nodeInfo()).thenReturn(nodeInfo);
        return connect;
    }
}