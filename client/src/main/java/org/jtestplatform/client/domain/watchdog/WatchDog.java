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
package org.jtestplatform.client.domain.watchdog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jtestplatform.client.Config;
import org.jtestplatform.client.domain.Domain;

/**
 * That utility class is used to watch a list of {@link Domain} and 
 * check regularly that they are alive.
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class WatchDog {
    private static final Logger LOGGER = Logger.getLogger(WatchDog.class);
    
    private static final long MAX_ZOMBIE_TIME_MILLIS = 10000;
        
    /**
     * The list of running {@link Domain}s.
     */
    private final Collection<Domain> runningDomains = new Vector<Domain>();
    
    /**
     * Indicate the time at which a {@link Domain} has started to be a zombie.
     */
    private final Map<Domain, Long> zombieStartTime = new HashMap<Domain, Long>();

    /**
     * Configuration of the WatchDog.
     */
    private final Config config;
    
    /**
     * Is the WatchDog actually watching the {@link Domain}s ? 
     */
    private boolean watch = false;
    
    /**
     * The thread used to watch domains.
     */
    private final WatchDogThread thread = new WatchDogThread();
    
    /**
     * The strategy to use when a domain is dead. 
     */
    private final WatchDogStrategy strategy;
    
    private final Collection<WatchDogListener> listeners = new Vector<WatchDogListener>();
    
    /**
     * Create a WatchDog for the given {@link Domain}, 
     * with the provided configuration.
     * @param config
     */
    public WatchDog(Config config) {
        this(config, null);
    }
    
    /**
     * Create a WatchDog with the provided configuration.
     * @param config
     * @param strategy
     */
    public WatchDog(Config config, WatchDogStrategy strategy) {
        this.strategy = (strategy == null) ? new DefaultWatchDogStrategy(MAX_ZOMBIE_TIME_MILLIS) : strategy;                       
        this.config = config;
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Add a listener.
     * @param listener
     */
    public void addWatchDogListener(WatchDogListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener.
     * @param listener
     */
    public void removeWatchDogListener(WatchDogListener listener) {
        listeners.remove(listener);
    }

    /**
     * Watch a domain.
     * @param domain
     */
    public void watch(Domain domain) {
        runningDomains.add(domain);
    }

    /**
     * Unwatch a domain.
     * @param domain
     */
    public void unwatch(Domain domain) {
        runningDomains.remove(domain);
    }
    
    /**
     * Start watching the {@link Domain}
     */
    public void startWatching() {
        watch = true;
    }

    /**
     * Stop watching the {@link Domain}
     */
    public void stopWatching() {
        watch = false;
    }
        
    /**
     * Sleep for the amount of time specified in the configuration.
     */
    private void goSleep() {
        try {
            Thread.sleep(config.getWatchDogPollInterval());
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    /**
     * Checks if the given domain is alive.
     * @param domain The domain to check.
     * @return true if the domain is alive.
     */
    private boolean domainIsAlive(Domain domain) {
        try {
            return domain.isAlive();
        } catch (IOException e) {
            LOGGER.error("error while checking if alive", e);
            return true;
        }
    }
    
    private void notifyDomainDied(Domain domain) {
        for (WatchDogListener l : listeners) {
            l.domainDied(domain);
        }
    }
    
    private Long getZombieStartTime(Domain domain) {
        Long startTime = zombieStartTime.get(domain);
        if (startTime == null) {
            // the domain was alive at previous verification 
            startTime = Long.valueOf(System.currentTimeMillis());
            zombieStartTime.put(domain, startTime);
        }
        return startTime;
    }
    
    private List<Domain> watchProcesses() {
        // copy the list to avoid ConcurrentModification while iterating over it.
        List<Domain> domainList = new ArrayList<Domain>(runningDomains);
        
        List<Domain> deadProcesses = new ArrayList<Domain>();
        
        for (Domain domain : domainList) {
            if (domainIsAlive(domain)) {
                zombieStartTime.remove(domain);
            } else {                            
                if (watch) {
                    // should try to resurrect the domain
                    boolean isReallyDead = strategy.domainDead(domain, getZombieStartTime(domain));
                    if (isReallyDead) {
                        deadProcesses.add(domain);
                    }
                }
            }
            
            if (!watch) {
                break;
            }
        }
        
        return deadProcesses;
    }
    
    /**
     * Thread class used to watch a list of domains.
     * @author Fabien DUMINY (fduminy@jnode.org)
     *
     */
    private class WatchDogThread extends Thread {
        private WatchDogThread() {
            super("WatchDogThread");
        }
        
        /**
         * Manage the activity of the WatchDog and notify when a domain is dead.
         */
        @Override
        public void run() {
            while (true) {
                if (watch) {
                    List<Domain> deadProcesses = watchProcesses();
                    
                    runningDomains.removeAll(deadProcesses);
                    for (Domain deadProcess : deadProcesses) {
                        notifyDomainDied(deadProcess);
                    }
                }
                
                goSleep();
            }
        }
    }    
}
