/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.cloud.domain.watchdog;

import com.google.code.tempusfugit.temporal.Clock;
import com.google.code.tempusfugit.temporal.Sleeper;
import com.google.code.tempusfugit.temporal.StopWatch;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * That utility class is used to watch a list of {@link Domain} and 
 * check regularly that they are alive.
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class WatchDog {
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchDog.class);
    
    /**
     * The list of running {@link Domain}s.
     */
    private final Collection<Domain> runningDomains = new Vector<Domain>();
    
    /**
     * Indicate the time at which a {@link Domain} has started to be a zombie.
     */
    private final Map<Domain, StopWatch> zombieStartTime = new HashMap<Domain, StopWatch>();

    /**
     * Is the WatchDog actually watching the {@link Domain}s ?
     */
    private final AtomicBoolean watch = new AtomicBoolean(false);

    /**
     * The strategy to use when a domain is dead. 
     */
    final WatchDogStrategy strategy;
    
    private final Collection<WatchDogListener> listeners = new Vector<WatchDogListener>();

    private WatchDogThread watchDogThread;

    private final Sleeper sleeper;
    private final Clock clock;

    /**
     * Create a WatchDog with the provided configuration.
     */
    public WatchDog(Sleeper sleeper, WatchDogStrategy strategy, Clock clock) {
        this.strategy = strategy;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    public void start() {
        if (watchDogThread == null) {
            watchDogThread = createWatchDogThread(sleeper);
        }
    }

    /**
     * Add a listener.
     */
    public void addWatchDogListener(WatchDogListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener.
     */
    public void removeWatchDogListener(WatchDogListener listener) {
        listeners.remove(listener);
    }

    /**
     * Watch a domain.
     */
    public void watch(Domain domain) {
        runningDomains.add(domain);
    }

    /**
     * Unwatch a domain.
     */
    public void unwatch(Domain domain) {
        runningDomains.remove(domain);
    }

    /**
     * Start watching the {@link Domain}.
     */
    public void startWatching() {
        watch.set(true);
    }

    /**
     * Stop watching the {@link Domain}.
     */
    public void stopWatching() {
        watch.set(false);
    }

    WatchDogThread createWatchDogThread(Sleeper sleeper) {
        return new WatchDogThread(this, sleeper);
    }

    void watchDomains() {
        List<Domain> deadDomains = findDeadDomains();

        runningDomains.removeAll(deadDomains);
        for (Domain deadProcess : deadDomains) {
            notifyDomainDied(deadProcess);
        }
    }

    boolean isWatching() {
        return watch.get();
    }

    /**
     * Checks if the given domain is alive.
     * @param domain The domain to check.
     * @return true if the domain is alive.
     */
    private boolean domainIsAlive(Domain domain) {
        try {
            return domain.isAlive();
        } catch (DomainException e) {
            LOGGER.error("error while checking if alive", e);
            return true;
        }
    }

    private void notifyDomainDied(Domain domain) {
        for (WatchDogListener l : listeners) {
            l.domainDied(domain);
        }
    }

    private StopWatch getZombieStartTime(Domain domain) {
        StopWatch stopWatch = zombieStartTime.get(domain);
        if (stopWatch == null) {
            // the domain was alive at previous verification
            stopWatch = StopWatch.start(clock);
            zombieStartTime.put(domain, stopWatch);
        }
        return stopWatch;
    }

    private List<Domain> findDeadDomains() {
        // copy the list to avoid ConcurrentModification while iterating over it.
        List<Domain> domainList = new ArrayList<Domain>(runningDomains);

        List<Domain> deadDomains = new ArrayList<Domain>();
        
        for (Domain domain : domainList) {
            if (domainIsAlive(domain)) {
                zombieStartTime.remove(domain);
            } else {
                if (watch.get()) {
                    // should try to resurrect the domain
                    boolean isReallyDead = strategy.domainDead(domain, getZombieStartTime(domain));
                    if (isReallyDead) {
                        deadDomains.add(domain);
                    }
                }
            }

            if (!watch.get()) {
                break;
            }
        }

        return deadDomains;
    }
}
