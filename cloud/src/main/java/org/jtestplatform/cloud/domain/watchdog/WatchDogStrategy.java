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
package org.jtestplatform.cloud.domain.watchdog;

import com.google.code.tempusfugit.temporal.StopWatch;
import org.jtestplatform.cloud.domain.Domain;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public interface WatchDogStrategy {
    /**
     * Callback method used to notify that a domain is probably dead.
     * @param domain The {@link Domain} that may be dead.
     * @return true if the domain should be considered as really dead, 
     * otherwise it will be considered as a zombie (it might finally die or resurrect).
     */
    boolean domainDead(Domain domain, StopWatch stopWatch);
}
