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
package org.jtestplatform.it;

import org.apache.commons.lang3.Validate;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class InJVMDomain<T extends InJVMTransportChannel> implements Domain {
    private final InJVMTestDriver.InJVMDomainManager manager;
    private final int domainID;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final InJVMTransportChannel channel;

    public InJVMDomain(InJVMTestDriver.InJVMDomainManager manager, int domainID) throws DomainException {
        Validate.notNull(manager, "manager is null");
        this.manager = manager;
        this.domainID = domainID;
        manager.register(this);

        try {
            channel = manager.getChannelFactory().create(domainID);
        } catch (TransportException e) {
            throw new DomainException(e);
        }
    }

    @Override
    public String start() throws DomainException {
        if (!isAlive()) {
            running.set(true);
            try {
                channel.open();
            } catch (Exception e) {
                throw new DomainException(e);
            }
        }

        return getIPAddress();
    }

    @Override
    public void stop() throws DomainException {
        if (isAlive()) {
            try {
                channel.close();
            } catch (IOException e) {
                throw new DomainException(e);
            }

            manager.unregister(this);
            running.set(false);
        }
    }

    @Override
    public boolean isAlive() throws DomainException {
        return running.get();
    }

    @Override
    public String getIPAddress() {
        return String.format("0.0.0.%d", domainID);
    }

    @Override
    public String toString() {
        return "Domain #" + domainID;
    }

    public Transport getClientTransport() throws DomainException {
        if (!isAlive()) {
            //throw new DomainException("Domain " + getIPAddress() + " not started");
            start();
        }

        return channel.getClientTransport();
    }
}
