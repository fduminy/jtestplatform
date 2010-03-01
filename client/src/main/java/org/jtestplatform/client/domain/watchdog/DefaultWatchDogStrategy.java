/**
 * JTestPlatform is a client/server framework for testing any JVM implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/**
 * 
 */
package org.jtestplatform.client.domain.watchdog;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.Domain;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class DefaultWatchDogStrategy implements WatchDogStrategy {
    private static final Logger LOGGER = Logger.getLogger(DefaultWatchDogStrategy.class);
    
    private final long maxZombieTimeMillis;
    public DefaultWatchDogStrategy(long maxZombieTimeMillis) {
        this.maxZombieTimeMillis = maxZombieTimeMillis;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean domainDead(Domain domain, Long sinceTimeMillis) {
        boolean isReallyDead = ((System.currentTimeMillis() - sinceTimeMillis) > maxZombieTimeMillis);
        
        if (!isReallyDead) {
            LOGGER.warn("domain is dead. restarting it.");
            try {
                domain.start();
            } catch (IOException e) {
                LOGGER.error("error while restarting", e);
            } catch (ConfigurationException e) {
                LOGGER.error("error while restarting", e);
            }
        } else {
            LOGGER.warn("domain is dead. restarting it.");
        }
        
        return isReallyDead;        
    }
}
