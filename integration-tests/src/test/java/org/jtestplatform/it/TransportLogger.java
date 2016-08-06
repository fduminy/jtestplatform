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
package org.jtestplatform.it;

import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An implementation of {@link org.jtestplatform.common.transport.Transport} that logging its messages.
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class TransportLogger implements Transport {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransportLogger.class);
    static final String NULL_MESSAGE = "null";

    private final String owner;
    private final Transport wrapped;

    public static TransportLogger wrap(String owner, Transport wrapped) {
        return new TransportLogger(owner, wrapped);
    }

    public TransportLogger(String owner, Transport wrapped) {
        if (wrapped == null) {
            throw new NullPointerException("wrapped Transport is null");
        }

        this.owner = owner;
        logMessage("#CREATED#", null);
        this.wrapped = wrapped;
    }

    @Override
    public void send(String message) throws TransportException {
        logMessage("SEND", (message == null) ? NULL_MESSAGE : message);
        wrapped.send(message);
    }

    @Override
    public String receive() throws TransportException {
        String received = wrapped.receive();
        logMessage("RECEIVE", (received == null) ? NULL_MESSAGE : received);
        return received;
    }

    @Override
    public void close() throws IOException {
        logMessage("#CLOSED#", null);
        wrapped.close();
    }

    protected void logMessage(String messageType, String message) {
        if (message == null) {
            LOGGER.info("{}: {}", owner, messageType);
        } else {
            LOGGER.info("{}: {} {}", owner, messageType, message);
        }
    }
}
