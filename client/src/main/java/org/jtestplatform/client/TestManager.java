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
package org.jtestplatform.client;

import java.util.Collection;
import java.util.concurrent.Future;

import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.TransportException;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public interface TestManager {
    /**
     * @param message
     * @param transportProvider
     * @param platform
     * @return The result of the test run.
     * @throws Exception
     */
    Future<TestResult> runTest(Message message,
            TransportProvider transportProvider, Platform platform)
            throws Exception;

    void shutdown();

    /**
     * @param testFramework The name of the framework.
     * @return The collection of tests for the given framework.
     * @throws TransportException
     */
    Collection<String> getFrameworkTests(String testFramework, TransportProvider transportProvider, Platform platform) throws TransportException;
}
