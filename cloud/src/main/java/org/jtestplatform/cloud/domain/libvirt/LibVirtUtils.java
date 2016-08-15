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
/**
 *
 */
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.domain.DomainException;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
class LibVirtUtils {
    static final boolean STRICT = false; // when set to true, some tags throws an exception

    private LibVirtUtils() {
    }

    static String toHexString(int valueIndex, int hexadecimalSize) throws DomainException {
        String result = Integer.toHexString(valueIndex);
        if (result.length() > hexadecimalSize) {
            throw new DomainException(
                "unable convert to hexadecimal with a maximum of " + hexadecimalSize + " characters");
        }

        if (result.length() < hexadecimalSize) {
            while (result.length() != hexadecimalSize) {
                result = '0' + result;
            }
        }
        return result;
    }
}
