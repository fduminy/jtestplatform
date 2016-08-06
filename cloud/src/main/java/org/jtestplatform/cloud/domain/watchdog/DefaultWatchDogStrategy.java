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

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.StopWatch;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class DefaultWatchDogStrategy implements WatchDogStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWatchDogStrategy.class);

    private final Duration maxZombieDuration;

    public DefaultWatchDogStrategy(Duration maxZombieDuration) {
        this.maxZombieDuration = maxZombieDuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean domainDead(Domain domain, StopWatch stopWatch) {
        stopWatch.lap();
        boolean isReallyDead = stopWatch.elapsedTime().greaterThan(maxZombieDuration);

        if (!isReallyDead) {
            try {
                domain.start();
            } catch (DomainException e) {
                LOGGER.error("Error while starting domain", e);
            }
        }

        return isReallyDead;
    }
}
