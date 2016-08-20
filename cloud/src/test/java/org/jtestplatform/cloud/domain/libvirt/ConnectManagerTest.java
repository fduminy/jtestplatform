/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.configuration.Connection;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.libvirt.ConnectManager.Command;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ConnectManager.class)
public class ConnectManagerTest {
    @Rule
    private ExpectedException thrown = ExpectedException.none();

    @Mock
    private Command<String> command;
    @Mock
    private Connect connect;

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private final Exception exception = new Exception("an error message");

    private ConnectManager manager;
    private Connection connection;

    @Before
    public void setUp() {
        manager = new ConnectManager();
        connection = new Connection();
        connection.setUri("anURI");
    }

    @Test
    public void execute() throws Exception {
        String expected = "resultOfCommand";
        when(command.execute(any(Connect.class))).thenReturn(expected);
        whenNew(Connect.class).withArguments(eq(connection.getUri()), eq(false)).thenReturn(connect);

        String actual = manager.execute(connection, command);

        assertThat(actual).isEqualTo(expected);
        verifyNew(Connect.class).withArguments(eq(connection.getUri()), eq(false));
        verify(connect).close();
    }

    @Test
    public void execute_errorInCommand() throws Exception {
        when(command.execute(any(Connect.class))).thenThrow(exception);
        whenNew(Connect.class).withArguments(eq(connection.getUri()), eq(false)).thenReturn(connect);
        thrown.expect(DomainException.class);
        thrown.expectMessage(exception.getMessage());
        thrown.expectCause(is(exception));

        try {
            manager.execute(connection, command);
        } finally {
            verify(connect).close();
        }
    }

    @Test
    public void execute_errorInConnect() throws Exception {
        whenNew(Connect.class).withArguments(eq(connection.getUri()), eq(false)).thenThrow(exception);
        thrown.expect(DomainException.class);
        thrown.expectMessage(exception.getMessage());
        thrown.expectCause(is(exception));

        manager.execute(connection, command);
    }
}