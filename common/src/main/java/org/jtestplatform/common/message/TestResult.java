/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2011  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.common.message;

import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestResult implements Message {
    private String framework;
    private String test;
    private boolean success;

    public TestResult() {
        // nothing
    }

    public TestResult(String framework, String test, boolean success) {
        this.framework = framework;
        this.test = test;
        this.success = success;
    }

    /**
     * @return the framework
     */
    public String getFramework() {
        return framework;
    }

    public String getTest() {
        return test;
    }

    /**
     * Get the success of the test.
     * @return The success of the test.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWith(Transport transport) throws TransportException {
        transport.send(framework);
        transport.send(test);
        transport.send(Boolean.toString(success));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveFrom(Transport transport) throws TransportException {
        framework = transport.receive();
        test = transport.receive();
        success = Boolean.getBoolean(transport.receive());
    }
}
