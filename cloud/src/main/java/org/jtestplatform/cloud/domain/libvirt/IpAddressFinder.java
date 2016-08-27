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

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import org.jtestplatform.cloud.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class IpAddressFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(IpAddressFinder.class);

    IpAddressFinder() {
    }

    String findIpAddress(NetworkConfig networkConfig) throws DomainException {
        for (int i = networkConfig.getMinSubNetIpAddress(); i <= networkConfig.getMaxSubNetIpAddress(); i++) {
            String ipAddress = networkConfig.getBaseIPAddress() + i;
            if (ping(ipAddress, seconds(10))) {
                return ipAddress;
            }
        }
        return null;
    }

    boolean ping(String host, Duration timeout) throws DomainException {
        try {
            waitReachableOrTimeout(host, timeout);
            return true;
        } catch (TimeoutException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    static void waitReachableOrTimeout(String ip, Duration timeout) throws TimeoutException, DomainException {
        try {
            waitOrTimeout(reachable(InetAddress.getByName(ip), seconds(1)), timeout(timeout));
        } catch (InterruptedException e) {
            throw new DomainException(e.getMessage(), e);
        } catch (UnknownHostException e) {
            throw new DomainException(e.getMessage(), e);
        }
    }

    private static Condition reachable(final InetAddress address, final Duration timeOut) {
        return new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return address.isReachable((int) timeOut.inMillis());
                } catch (IOException e) {
                    return false;
                }
            }
        };
    }
}
