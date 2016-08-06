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
package org.jtestplatform.common.transport;

import org.jtestplatform.common.message.ErrorMessage;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
@SuppressWarnings("serial")
public class TransportException extends Exception {
    private final ErrorMessage errorMessage;

    public TransportException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = null;
    }

    public TransportException(String message) {
        super(message);
        this.errorMessage = null;
    }

    public TransportException(ErrorMessage errorMessage) {
        super("Received Error : " + errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
