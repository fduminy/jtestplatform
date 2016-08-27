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

import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LibVirtDomainFactory.class)
public class LibVirtDomainTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private Connection connection;
    @Mock private IpAddressFinder ipAddressFinder;
    @Mock private DomainBuilder domainBuilder;
    @Mock private NetworkConfig networkConfig;
    @Mock private Domain domain;

    private LibVirtDomainFactory factory;
    private DomainConfig domainConfig = new DomainConfig();

    @Before
    public void setUp() throws LibvirtException, DomainException {
        factory = mock(LibVirtDomainFactory.class);
        when(domain.getInfo()).thenReturn(new DomainInfo());
        when(factory.execute(eq(connection), any(ConnectManager.Command.class))).then(executeCommand());
        when(domainBuilder.defineDomain(any(Connect.class), eq(domainConfig), eq(networkConfig))).thenReturn(domain);
        when(connection.getUri()).thenReturn("http://fakeURI");
    }

    @Test
    public void start() throws Exception {
        LibVirtDomain libVirtDomain = new LibVirtDomain(domainConfig, factory, connection, ipAddressFinder,
                                                        domainBuilder, networkConfig);

        libVirtDomain.start();

        InOrder inOrder = inOrder(factory, domainBuilder, domain, ipAddressFinder);
        inOrder.verify(factory).execute(eq(connection), any(ConnectManager.Command.class));
        inOrder.verify(domainBuilder).defineDomain(any(Connect.class), eq(domainConfig), eq(networkConfig));
        inOrder.verify(domain).create();
        inOrder.verify(ipAddressFinder).findIpAddress(networkConfig);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void stop() throws Exception {
        domain.getInfo().state = DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF;
        LibVirtDomain libVirtDomain = new LibVirtDomain(domainConfig, factory, connection, ipAddressFinder,
                                                        domainBuilder, networkConfig) {
            @Override public synchronized void start() throws DomainException {
                this.domain = LibVirtDomainTest.this.domain;
            }

            @Override public boolean isAlive() throws DomainException {
                return true;
            }
        };
        libVirtDomain.start();

        libVirtDomain.stop();

        InOrder inOrder = inOrder(domain);
        inOrder.verify(domain).destroy();
        inOrder.verify(domain).undefine();
        inOrder.verify(domain).free();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void isAlive() throws Exception {
        for (DomainInfo.DomainState state : DomainInfo.DomainState.values()) {
            LibVirtDomain libVirtDomain = new LibVirtDomain(domainConfig, factory, connection, ipAddressFinder,
                                                            domainBuilder, networkConfig) {
                @Override public synchronized void start() throws DomainException {
                    this.domain = LibVirtDomainTest.this.domain;
                }
            };
            libVirtDomain.start();
            domain.getInfo().state = state;

            assertThat(libVirtDomain.isAlive()).isEqualTo(state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING);
        }
    }

    @Test
    public void getIPAddress() throws Exception {
        when(ipAddressFinder.findIpAddress(eq(networkConfig))).thenReturn("12.34.56.78");
        LibVirtDomain libVirtDomain = new LibVirtDomain(domainConfig, factory, connection, ipAddressFinder,
                                                        domainBuilder, networkConfig);
        assertThat(libVirtDomain.getIPAddress()).as("IPAddress before start").isNull();

        libVirtDomain.start();
        assertThat(libVirtDomain.getIPAddress()).as("IPAddress after start").isEqualTo("12.34.56.78");
    }

    private static Answer<Object> executeCommand() {
        return new Answer<Object>() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ConnectManager.Command command = invocationOnMock.getArgumentAt(1, ConnectManager.Command.class);
                command.execute(mock(Connect.class));
                return null;
            }
        };
    }
}