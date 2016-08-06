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
package org.jtestplatform.server;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link java.io.OutputStream} writing to a {@link java.lang.StringBuilder}.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class StringOutputStream extends OutputStream {
    private final Appendable backOutput;

    public StringOutputStream(Appendable backOutput) {
        this.backOutput = backOutput;
    }

    @Override
    public void write(int b) throws IOException {
        backOutput.append((char) (b & 0xFF));
    }
}
