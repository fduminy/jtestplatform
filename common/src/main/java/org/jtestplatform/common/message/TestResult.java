/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
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

    private String failureType;
    private String failureContent;
    private String failureMessage;

    public TestResult() {
        // nothing
    }

    public TestResult(String framework, String test) {
        this.framework = framework;
        this.test = test;
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

    public String getFailureType() {
        return failureType;
    }

    public String getFailureContent() {
        return failureContent;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * Get the success of the test.
     * @return The success of the test.
     */
    public boolean isSuccess() {
        return failureType == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWith(Transport transport) throws TransportException {
        transport.send(framework);
        transport.send(test);
        transport.send(failureType);
        if (failureType != null) {
            transport.send(failureContent);
            transport.send(failureMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveFrom(Transport transport) throws TransportException {
        framework = transport.receive();
        test = transport.receive();
        failureType = transport.receive();
        if (failureType != null) {
            failureContent = transport.receive();
            failureMessage = transport.receive();
        }
    }

    public void setFailure(String failureType, String failureContent, String failureMessage) {
        this.failureType = failureType;
        this.failureContent = failureContent;
        this.failureMessage = failureMessage;
    }
}
