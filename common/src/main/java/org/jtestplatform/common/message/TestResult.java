/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.common.message;

import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;

import static org.jtestplatform.common.transport.TransportHelper.receiveBoolean;
import static org.jtestplatform.common.transport.TransportHelper.sendBoolean;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class TestResult implements Message {
    private String framework;
    private String test;

    private boolean ignored;
    private boolean error;
    private String failureType;
    private String failureContent;
    private String failureMessage;
    private String systemOut;
    private String systemErr;

    public TestResult() {
        // nothing
    }

    public TestResult(String framework, String test) {
        this.framework = framework;
        this.test = test;
    }

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

    public boolean isError() {
        return error;
    }

    public String getSystemOut() {
        return systemOut;
    }

    public String getSystemErr() {
        return systemErr;
    }

    public void setSystemErr(String systemErr) {
        this.systemErr = systemErr;
    }

    public void setSystemOut(String systemOut) {
        this.systemOut = systemOut;
    }

    public boolean isSuccess() {
        return !ignored && (failureType == null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWith(Transport transport) throws TransportException {
        transport.send(framework);
        transport.send(test);
        TransportHelper.sendBoolean(transport, ignored);
        if (!ignored) {
            transport.send(failureType);
            if (failureType != null) {
                transport.send(failureContent);
                transport.send(failureMessage);
                sendBoolean(transport, error);
                transport.send(systemOut);
                transport.send(systemErr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveFrom(Transport transport) throws TransportException {
        framework = transport.receive();
        test = transport.receive();
        ignored = TransportHelper.receiveBoolean(transport);
        if (!ignored) {
            failureType = transport.receive();
            if (failureType != null) {
                failureContent = transport.receive();
                failureMessage = transport.receive();
                error = receiveBoolean(transport);
                systemOut = transport.receive();
                systemErr = transport.receive();
            }
        }
    }

    public void setFailure(String failureType, String failureContent, String failureMessage, boolean error) {
        this.failureType = failureType;
        this.failureContent = failureContent;
        this.failureMessage = failureMessage;
        this.error = error;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored() {
        ignored = true;
    }
}
