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
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
abstract class UniqueValueFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniqueValueFinder.class);

    /**
     * Find a unique value that is not yet in the given list of values.
     */
    protected final String findUniqueValue(List<String> values, String valueName, String valuePrefix, int valueIndex,
                                           int maxValueIndex, int hexadecimalSize) throws DomainException {
        String value = null;
        for (; valueIndex <= maxValueIndex; valueIndex++) {
            String indexStr = LibVirtUtils.toHexString(valueIndex, hexadecimalSize);

            value = valuePrefix + indexStr;
            if (!values.contains(value)) {
                break;
            }
        }

        if ((valueIndex > maxValueIndex) || (value == null)) {
            throw new DomainException("unable to find a unique " + valueName);
        }

        LOGGER.debug("found a unique {} : {}", valueName, value);
        return value;
    }
}
