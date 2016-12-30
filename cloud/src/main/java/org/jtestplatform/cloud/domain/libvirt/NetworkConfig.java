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

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class NetworkConfig {
    private final String networkName;
    private final String baseMacAddress;
    private final String baseIPAddress;
    private final int minSubNetIpAddress;
    private final int maxSubNetIpAddress;

    NetworkConfig(String networkName, String baseMacAddress, String baseIPAddress, int minSubNetIpAddress,
                  int maxSubNetIpAddress) {
        this.networkName = networkName;
        this.baseMacAddress = baseMacAddress;
        this.baseIPAddress = baseIPAddress;
        this.minSubNetIpAddress = minSubNetIpAddress;
        this.maxSubNetIpAddress = maxSubNetIpAddress;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getBaseMacAddress() {
        return baseMacAddress;
    }

    public String getBaseIPAddress() {
        return baseIPAddress;
    }

    public int getMinSubNetIpAddress() {
        return minSubNetIpAddress;
    }

    public int getMaxSubNetIpAddress() {
        return maxSubNetIpAddress;
    }

    public int size() {
        return maxSubNetIpAddress - minSubNetIpAddress + 1;
    }
}