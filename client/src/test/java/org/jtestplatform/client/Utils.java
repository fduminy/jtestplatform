/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.client;

import org.jtestplatform.cloud.configuration.Platform;

public class Utils {
    static final Platform PLATFORM1 = createPlatform("Athlon", 32, 2);
    static final Platform PLATFORM2 = createPlatform("Pentium", 32, 1);

    static {
        PLATFORM1.setCdrom("myCDROM");
        PLATFORM1.setMemory(123L);

        PLATFORM2.setCdrom("myCDROM2");
        PLATFORM2.setMemory(456L);
    }

    static Platform createPlatform(String cpuName, int wordSize, int nbCores) {
        Platform platform = new Platform();
        platform.setCpu(cpuName);
        platform.setWordSize(wordSize);
        platform.setNbCores(nbCores);
        return platform;
    }
}
