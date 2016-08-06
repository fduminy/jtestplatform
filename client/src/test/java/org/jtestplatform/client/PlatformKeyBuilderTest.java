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
package org.jtestplatform.client;

import org.jtestplatform.cloud.configuration.Platform;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class PlatformKeyBuilderTest {
    @Test
    public void test_nullPlatform() {
        PlatformKeyBuilder builder = new PlatformKeyBuilder();

        String actualKey = builder.buildKey(null);

        assertThat(actualKey).isNull();
    }

    @Test
    public void test_singleCore() {
        PlatformKeyBuilder builder = new PlatformKeyBuilder();
        Platform platform = Utils.createPlatform("myCpu", 32, 1);

        String actualKey = builder.buildKey(platform);

        assertThat(actualKey).isEqualTo("myCpu_32bits_1core");
    }

    @Test
    public void test_dualCore() {
        PlatformKeyBuilder builder = new PlatformKeyBuilder();
        Platform platform = Utils.createPlatform("myCpu", 32, 2);

        String actualKey = builder.buildKey(platform);

        assertThat(actualKey).isEqualTo("myCpu_32bits_2cores");
    }

}