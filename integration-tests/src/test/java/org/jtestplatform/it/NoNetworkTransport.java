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
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class NoNetworkTransport implements Transport {
    private final BlockingQueue<String> outBox;
    private final BlockingQueue<String> inBox;

    NoNetworkTransport(BlockingQueue<String> inBox, BlockingQueue<String> outBox) {
        Validate.notNull(inBox, "inBox is null");
        Validate.notNull(outBox, "outBox is null");
        this.outBox = outBox;
        this.inBox = inBox;
    }

    @Override
    public void close() throws IOException {
        inBox.clear();
        outBox.clear();
    }

    @Override
    public void send(String message) throws TransportException {
        message = (message == null) ? TransportLogger.NULL_MESSAGE : message;
        try {
            outBox.put(message);
        } catch (InterruptedException e) {
            throw new TransportException(e.getMessage(), e);
        }
    }

    @Override
    public String receive() throws TransportException {
        try {
            String message = inBox.take();
            return (message == TransportLogger.NULL_MESSAGE) ? null : message;
        } catch (InterruptedException e) {
            throw new TransportException(e.getMessage(), e);
        }
    }
}
