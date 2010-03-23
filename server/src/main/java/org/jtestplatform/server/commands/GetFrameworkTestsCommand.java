/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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
 * -
 */
package org.jtestplatform.server.commands;

import java.util.List;

import org.jtestplatform.common.message.FrameworkTests;
import org.jtestplatform.common.message.GetFrameworkTests;
import org.jtestplatform.server.TestServer;
import org.jtestplatform.server.TestServerCommand;

/**
 *
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class GetFrameworkTestsCommand implements TestServerCommand<GetFrameworkTests, FrameworkTests> {
    private final TestServer<?> testServer;

    public GetFrameworkTestsCommand(TestServer<?> testServer) {
        this.testServer = testServer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FrameworkTests execute(GetFrameworkTests message) throws Exception {
        List<String> tests = testServer.getTestFramework(message.getFramework()).getTests();
        return new FrameworkTests(tests);
    }
}
