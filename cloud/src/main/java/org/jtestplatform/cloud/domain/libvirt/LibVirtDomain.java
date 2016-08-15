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
/**
 *
 */
package org.jtestplatform.cloud.domain.libvirt;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Sleeper;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.Timeout;
import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.common.ConfigUtils;
import org.libvirt.Connect;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import java.util.concurrent.TimeoutException;

import static com.google.code.tempusfugit.temporal.Duration.minutes;
import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;

/**
 * Implementation for a {@link org.jtestplatform.cloud.domain.Domain} based on <a href="http://www.libvirt.org/">libvirt</a>.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
class LibVirtDomain implements Domain {

    /**
     * Configuration of machine to run with libvirt.
     */
    private final DomainConfig config;
    private final NetworkConfig networkConfig;

    private final LibVirtDomainFactory factory;
    private final Connection connection;

    private org.libvirt.Domain domain;

    private final IpAddressFinder ipAddressFinder;
    private String ipAddress;
    private DomainBuilder domainBuilder;

    /**
     *
     * @param config The configuration of the machine to run with libvirt.
     * @param factory The factory that is creating this domain.
     * @param connection The connection giving the underlying engine URI.
     * @throws DomainException
     */
    LibVirtDomain(DomainConfig config, LibVirtDomainFactory factory, Connection connection,
                  IpAddressFinder ipAddressFinder,
                  DomainBuilder domainBuilder, NetworkConfig networkConfig) throws DomainException {
        this.config = config;
        this.networkConfig = networkConfig;
        this.factory = factory;
        this.connection = connection;
        this.ipAddressFinder = ipAddressFinder;

        this.domainBuilder = domainBuilder;
        if (ConfigUtils.isBlank(connection.getUri())) {
            throw new DomainException("connection's URI not specified");
        }
    }

    /**
     * {@inheritDoc}
     * @throws DomainException
     */
    @Override
    public synchronized void start() throws DomainException {
        factory.execute(connection, new ConnectManager.Command<Void>() {
            @Override
            public Void execute(Connect connect) throws Exception {
                //                factory.ensureNetworkExist(connect);

                if (!isAlive()) {
                    ipAddress = null;
                    if (domain != null) {
                        domain.free();
                    }
                    domain = domainBuilder.defineDomain(connect, config, networkConfig);
                    if (!isAlive()) {
                        try {
                            domain.create();
                        } catch (LibvirtException e) {
                            throw new DomainException(e);
                        }
                    }
                }
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws DomainException
     */
    @Override
    public synchronized void stop() throws DomainException {
        if (isAlive()) {
            Timeout timeout = timeout(minutes(1));
            Sleeper sleeper = new ThreadSleep(seconds(1));
            try {
                domain.destroy();

                waitOrTimeout(new Condition() {
                    @Override
                    public boolean isSatisfied() {
                        try {
                            DomainInfo info = domain.getInfo();
                            return (info.state == DomainState.VIR_DOMAIN_SHUTOFF);
                        } catch (LibvirtException e) {
                            throw new RuntimeException("Can't get domain information", e);
                        }
                    }
                }, timeout, sleeper);

                domain.undefine();
                domain.free();
                closeConnection();
            } catch (LibvirtException e) {
                throw new DomainException(e);
            } catch (InterruptedException e) {
                throw new DomainException(e);
            } catch (TimeoutException e) {
                throw new DomainException(e);
            }
            domain = null;
            ipAddress = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeConnection();
    }

    protected void closeConnection() throws LibvirtException {
        factory.releaseConnect(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlive() throws DomainException {
        boolean isRunning = false;

        if (domain != null) {
            try {
                DomainState state = domain.getInfo().state;
                isRunning = DomainState.VIR_DOMAIN_RUNNING.equals(state);
            } catch (LibvirtException e) {
                throw new DomainException(e);
            }
        }

        return isRunning;
    }

    public String getIPAddress() {
        return ipAddress;
    }
}
