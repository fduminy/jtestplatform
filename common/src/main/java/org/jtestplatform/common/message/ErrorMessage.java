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
/**
 *
 */
package org.jtestplatform.common.message;

import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class ErrorMessage implements Message {
    private String message;

    public ErrorMessage() {
        // nothing
    }

    public ErrorMessage(String message) {
        this.message = message;
    }

    /**
     * @return the framework
     */
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWith(Transport transport) throws TransportException {
        transport.send(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveFrom(Transport transport) throws TransportException {
        message = transport.receive();
    }
}
