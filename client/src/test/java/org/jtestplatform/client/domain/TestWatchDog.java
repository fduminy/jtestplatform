/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 * -
 */
/**
 * 
 */
package org.jtestplatform.client.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jtestplatform.client.ConfigReader;
import org.jtestplatform.client.ConfigurationException;
import org.jtestplatform.client.domain.DomainUtils.CustomDomain;
import org.jtestplatform.client.domain.watchdog.DefaultWatchDogStrategy;
import org.jtestplatform.client.domain.watchdog.WatchDog;
import org.jtestplatform.client.domain.watchdog.WatchDogListener;
import org.jtestplatform.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TestWatchDog {
    @DataPoint
    public static final Long MAX_ZOMBIE_TIME = Long.valueOf(60000L);
    
    public static class NbDomains {
        private final int value;
        public NbDomains(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }    
    @DataPoint
    public static final NbDomains NB_DOMAINS1 = new NbDomains(1);
    @DataPoint
    public static final NbDomains NB_DOMAINS2 = new NbDomains(10);
    @DataPoint
    public static final NbDomains NB_DOMAINS3 = new NbDomains(100);
    @DataPoint
    public static final NbDomains NB_DOMAINS4 = new NbDomains(1000);

    @DataPoint
    public static final Integer POLL_INTERVAL1 = Integer.valueOf(10);
    @DataPoint
    public static final Integer POLL_INTERVAL2 = Integer.valueOf(100);
    
    private WatchDog watchDog;
    
    @Before
    public void setUp() throws ConfigurationException {
        new ConfigReader().read(); // will initialize log4j
    }
    
    @After
    public void tearDown() {
        watchDog.stopWatching();
        watchDog = null;
    }
    
    @Theory
    public void testAlwaysDead(NbDomains nbDomains, Long maxZombieTimeMillis, Integer pollInterval) throws IOException, DomainException {
        watchDog = createWatchDog(pollInterval, maxZombieTimeMillis);
        Domain[] p = createFixedStateProcesses(Boolean.FALSE, null, nbDomains, pollInterval);
        
        for (int i = 0; i < nbDomains.getValue(); i++) {        
            verify(p[i], atLeastOnce()).isAlive();
            assertFalse("must be dead", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();
        }
    }

    @Theory
    public void testAlwaysAlive(NbDomains nbDomains, Long maxZombieTimeMillis, Integer pollInterval) throws IOException, DomainException {
        watchDog = createWatchDog(pollInterval, maxZombieTimeMillis);
        Domain[] p = createFixedStateProcesses(Boolean.TRUE, null, nbDomains, pollInterval);
        
        for (int i = 0; i < nbDomains.getValue(); i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], never()).start();
            verify(p[i], never()).stop();
        }
    }

    @Theory
    public void testNormal(NbDomains nbDomains, Long maxZombieTimeMillis, Integer pollInterval) {
        watchDog = createWatchDog(pollInterval, maxZombieTimeMillis);
        CustomDomain[] p = createCustomDomain(0, null, nbDomains, pollInterval);
        
        for (int i = 0; i < nbDomains.getValue(); i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();            
        }
    }

    @Theory
    public void testUnexpectedDead(NbDomains nbDomains, Long maxZombieTimeMillis, Integer pollInterval) throws IOException {
        watchDog = createWatchDog(pollInterval, maxZombieTimeMillis);
        CustomDomain[] p = createCustomDomain(0, null, nbDomains, pollInterval);
        
        for (int i = 0; i < nbDomains.getValue(); i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();            
        }
    }
    
    @Theory
    public void testAddRemoveListener(NbDomains nbDomains, Integer pollInterval) throws DomainException {
        watchDog = createWatchDog(pollInterval, Long.valueOf(pollInterval * 2));
        testAddRemoveListener(Boolean.FALSE, nbDomains, pollInterval);
        testAddRemoveListener(Boolean.TRUE, nbDomains, pollInterval);
    }

    private void testAddRemoveListener(Boolean fixedState, NbDomains nbDomains, Integer pollInterval) throws DomainException {
        final Set<Domain> called = new HashSet<Domain>(); 
        WatchDogListener listener = new WatchDogListener() {
            public void domainDied(Domain domain) {
                called.add(domain);
            }
        };
        Domain[] p = createFixedStateProcesses(fixedState, listener, nbDomains, pollInterval);
        
        int nbCalled = 0;
        for (int i = 0; i < nbDomains.getValue(); i++) {
            if (called.contains(p[i])) {
                nbCalled++;
            }
        }
        if (Boolean.TRUE.equals(fixedState)) {
            assertEquals("always alive => listener must never be called", 0, nbCalled);
        } else {
            assertEquals("always dead => listener must be called", nbDomains.getValue(), nbCalled);
        }

        watchDog.removeWatchDogListener(listener);
        called.clear();
        
        sleep(10 * pollInterval);
        nbCalled = 0;
        for (int i = 0; i < nbDomains.getValue(); i++) {
            if (called.contains(p[i])) {
                nbCalled++;
            }
        }
        assertEquals("listener must never be called after removal", 0, nbCalled);
    }
    
    private Domain[] createFixedStateProcesses(final Boolean fixedState, WatchDogListener listener, NbDomains nbDomains, Integer pollInterval) throws DomainException {
        if (listener != null) {
            watchDog.addWatchDogListener(listener);
        }
        
        Domain[] p = DomainUtils.createFixedStateProcesses(fixedState, watchDog, nbDomains.getValue());
        sleep(Math.max(5 * pollInterval, (nbDomains.getValue() < 50) ? 100 : 1000));
        return p;
    }
    
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // ignore
        }
    }
        
    private CustomDomain[] createCustomDomain(long multiple, WatchDogListener listener, NbDomains nbDomains, Integer pollInterval) {
        if (listener != null) {
            watchDog.addWatchDogListener(listener);
        }
        
        CustomDomain[] result = DomainUtils.createCustomDomain(multiple, nbDomains.getValue(), watchDog, pollInterval);
        
        sleep(10 * pollInterval);
        return result;
    }
    
    private static WatchDog createWatchDog(Integer pollInterval, Long maxZombieTimeMillis) {        
        Configuration config = new Configuration();
        DefaultWatchDogStrategy strategy = new DefaultWatchDogStrategy(maxZombieTimeMillis); 
        config.setWatchDogPollInterval(pollInterval);
        WatchDog watchDog = new WatchDog(config, strategy);
        watchDog.startWatching();
        return watchDog; 
    }
}
