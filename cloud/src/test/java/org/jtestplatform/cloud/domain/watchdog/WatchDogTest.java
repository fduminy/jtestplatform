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
/**
 * 
 */
package org.jtestplatform.cloud.domain.watchdog;

import org.jtestplatform.cloud.configuration.Configuration;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainUtils;
import org.jtestplatform.cloud.domain.DomainUtils.CustomDomain;
import org.junit.After;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class WatchDogTest {
    private static final Long MAX_ZOMBIE_TIME = 60000L;

    @SuppressWarnings("UnusedDeclaration")
    public static enum NbDomains {
        _1(1),
        _10(10),
        _100(100),
        _1000(1000);

        private final int value;

        private NbDomains(int value) {
            this.value = value;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static enum PollInterval {
        _10(10),
        _100(100);

        private final int value;

        private PollInterval(int value) {
            this.value = value;
        }
    }
    
    private WatchDog watchDog;
    
    @After
    public void tearDown() {
        watchDog.stopWatching();
        watchDog = null;
    }
    
    @Theory
    public void testAlwaysDead(NbDomains nbDomains, PollInterval pollInterval) throws IOException, DomainException {
        watchDog = createWatchDog(pollInterval, MAX_ZOMBIE_TIME);
        Domain[] p = createFixedStateProcesses(false, null, nbDomains, pollInterval);

        for (int i = 0; i < nbDomains.value; i++) {        
            verify(p[i], atLeastOnce()).isAlive();
            assertFalse("must be dead", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();
        }
    }

    @Theory
    public void testAlwaysAlive(NbDomains nbDomains, PollInterval pollInterval) throws IOException, DomainException {
        watchDog = createWatchDog(pollInterval, MAX_ZOMBIE_TIME);
        Domain[] p = createFixedStateProcesses(true, null, nbDomains, pollInterval);

        for (int i = 0; i < nbDomains.value; i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], never()).start();
            verify(p[i], never()).stop();
        }
    }

    @Theory
    public void testNormal(NbDomains nbDomains, PollInterval pollInterval) {
        watchDog = createWatchDog(pollInterval, MAX_ZOMBIE_TIME);
        CustomDomain[] p = createCustomDomain(0, null, nbDomains, pollInterval);

        for (int i = 0; i < nbDomains.value; i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();            
        }
    }

    @Theory
    public void testUnexpectedDead(NbDomains nbDomains, PollInterval pollInterval) throws IOException {
        watchDog = createWatchDog(pollInterval, MAX_ZOMBIE_TIME);
        CustomDomain[] p = createCustomDomain(0, null, nbDomains, pollInterval);

        for (int i = 0; i < nbDomains.value; i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();            
        }
    }
    
    @Theory
    public void testAddRemoveListener(NbDomains nbDomains, PollInterval pollInterval) throws DomainException {
        watchDog = createWatchDog(pollInterval, pollInterval.value * 2);
        testAddRemoveListener(false, nbDomains, pollInterval);
        testAddRemoveListener(true, nbDomains, pollInterval);
    }

    private void testAddRemoveListener(boolean fixedState, NbDomains nbDomains, PollInterval pollInterval) throws DomainException {
        final Set<Domain> called = new HashSet<Domain>(); 
        WatchDogListener listener = new WatchDogListener() {
            public void domainDied(Domain domain) {
                called.add(domain);
            }
        };
        Domain[] p = createFixedStateProcesses(fixedState, listener, nbDomains, pollInterval);
        
        int nbCalled = 0;
        for (int i = 0; i < nbDomains.value; i++) {
            if (called.contains(p[i])) {
                nbCalled++;
            }
        }
        if (fixedState) {
            assertEquals("always alive => listener must never be called", 0, nbCalled);
        } else {
            assertEquals("always dead => listener must be called", nbDomains.value, nbCalled);
        }

        watchDog.removeWatchDogListener(listener);
        called.clear();

        sleep(10 * pollInterval.value);
        nbCalled = 0;
        for (int i = 0; i < nbDomains.value; i++) {
            if (called.contains(p[i])) {
                nbCalled++;
            }
        }
        assertEquals("listener must never be called after removal", 0, nbCalled);
    }

    private Domain[] createFixedStateProcesses(final boolean fixedState, WatchDogListener listener, NbDomains nbDomains, PollInterval pollInterval) throws DomainException {
        if (listener != null) {
            watchDog.addWatchDogListener(listener);
        }

        Domain[] p = DomainUtils.createFixedStateProcesses(fixedState, watchDog, nbDomains.value);
        sleep(Math.max(5 * pollInterval.value, (nbDomains.value < 50) ? 100 : 1000));
        return p;
    }
    
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private CustomDomain[] createCustomDomain(long multiple, WatchDogListener listener, NbDomains nbDomains, PollInterval pollInterval) {
        if (listener != null) {
            watchDog.addWatchDogListener(listener);
        }

        CustomDomain[] result = DomainUtils.createCustomDomain(multiple, nbDomains.value, watchDog, pollInterval.value);

        sleep(10 * pollInterval.value);
        return result;
    }

    private static WatchDog createWatchDog(PollInterval pollInterval, long maxZombieTimeMillis) {
        Configuration config = new Configuration();
        DefaultWatchDogStrategy strategy = new DefaultWatchDogStrategy(maxZombieTimeMillis);
        config.setWatchDogPollInterval(pollInterval.value);
        WatchDog watchDog = new WatchDog(config, strategy);
        watchDog.startWatching();
        return watchDog; 
    }
}
