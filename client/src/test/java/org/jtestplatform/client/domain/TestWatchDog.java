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
package org.jtestplatform.client.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jtestplatform.client.Config;
import org.jtestplatform.client.domain.watchdog.DefaultWatchDogStrategy;
import org.jtestplatform.client.domain.watchdog.WatchDog;
import org.jtestplatform.client.domain.watchdog.WatchDogListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(value = Parameterized.class)
public class TestWatchDog {
    private int pollInterval;
    private int nbDomains;
    private long maxZombieTimeMillis;
    
    private WatchDog watchDog;

    @Parameters
    public static Collection<?> parameters() {
        return Arrays.asList(new Object[][]{
            // nbProcesses, pollInterval, maxZombieTimeMillis
            {1, 10, 60000l},
            {1, 100, 60000l},
            {10, 10, 60000l},
            {10, 100, 60000l},
            {100, 10, 60000l},
            {100, 100, 60000l},
            {1000, 10, 60000l},
            {1000, 100, 60000l},
        });
    }
    
    public TestWatchDog(Integer nbProcesses, Integer pollInterval, Long maxZombieTimeMillis) {
        this.nbDomains = nbProcesses;
        this.pollInterval = pollInterval;
        this.maxZombieTimeMillis = maxZombieTimeMillis;
    }
    
    @Before
    public void setUp() {
        Config config = new Config();
        DefaultWatchDogStrategy strategy = new DefaultWatchDogStrategy(maxZombieTimeMillis); 
        config.setWatchDogPollInterval(pollInterval);
        watchDog = new WatchDog(config, strategy);
        watchDog.startWatching();
    }
    
    @After
    public void tearDown() {
        watchDog.stopWatching();
        watchDog = null;
    }
    
    @Test
    public void testAlwaysDead() throws IOException, ConfigurationException {
        Domain[] p = createFixedStateProcesses(Boolean.FALSE, null);
        
        for (int i = 0; i < nbDomains; i++) {        
            verify(p[i], atLeastOnce()).isAlive();
            assertFalse("must be dead", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();
        }
    }

    @Test
    public void testAlwaysAlive() throws IOException, ConfigurationException {
        Domain[] p = createFixedStateProcesses(Boolean.TRUE, null);
        
        for (int i = 0; i < nbDomains; i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], never()).start();
            verify(p[i], never()).stop();
        }
    }

    @Test
    public void testNormal() {
        CustomDomain[] p = createCustomDomain(0, null);
        
        for (int i = 0; i < nbDomains; i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();            
        }
    }

    @Test
    public void testUnexpectedDead() throws IOException {
        CustomDomain[] p = createCustomDomain(0, null);
        
        for (int i = 0; i < nbDomains; i++) {
            verify(p[i], atLeastOnce()).isAlive();
            assertTrue("domain must be alive", p[i].isAlive());
            
            verify(p[i], atLeastOnce()).start();
            verify(p[i], never()).stop();            
        }
    }
    
    public void testAddRemoveListener() throws IOException {
        testAddRemoveListener(Boolean.FALSE);
        testAddRemoveListener(Boolean.TRUE);
    }

    public void testAddRemoveListener(Boolean fixedState) throws IOException {
        final Set<Domain> called = new HashSet<Domain>(); 
        WatchDogListener listener = new WatchDogListener() {
            public void domainDied(Domain domain) {
                called.add(domain);
            }
        };
        Domain[] p = createFixedStateProcesses(fixedState, listener);
        
        for (int i = 0; i < nbDomains; i++) {
            if (Boolean.TRUE.equals(fixedState)) {
                assertFalse("always alive => listener must never be called", called.contains(p[i]));
            } else {
                assertTrue("always dead => listener must be called", called.contains(p[i]));
            }
        }

        watchDog.removeWatchDogListener(listener);
        
        if (Boolean.FALSE.equals(fixedState)) {
            waitNextPoll(10);
            for (int i = 0; i < nbDomains; i++) {
                assertFalse("always dead => listener must never be called after removal", called.contains(p[i]));
            }
        }
    }
    
    private Domain[] createFixedStateProcesses(final Boolean fixedState, WatchDogListener listener) throws IOException {
        if (listener != null) {
            watchDog.addWatchDogListener(listener);
        }
        
        Domain[] p = new Domain[nbDomains];
        for (int i = 0; i < nbDomains; i++) {
            p[i] = mock(Domain.class);
            when(p[i].isAlive()).thenAnswer(new Answer<Boolean>() {

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {
                    return fixedState;                
                }                
            });
            
            watchDog.watch(p[i]);
        }
        waitNextPoll(5);
        return p;
    }
    
    private CustomDomain[] createCustomDomain(long multiple, WatchDogListener listener) {
        if (listener != null) {
            watchDog.addWatchDogListener(listener);
        }
        
        CustomDomain[] p = new CustomDomain[nbDomains];
        for (int i = 0; i < nbDomains; i++) {
            p[i] = mock(CustomDomain.class);
            p[i].setUnexpectedDeadDelay((int) (multiple * pollInterval));
            doCallRealMethod().when(p[i]).isAlive();
            doCallRealMethod().when(p[i]).start();
            doCallRealMethod().when(p[i]).stop();
            
            watchDog.watch(p[i]);
        }
        waitNextPoll(10);
        return p;
    }        
    
    private void waitNextPoll(int multiple) {
        try {
            Thread.sleep(pollInterval * multiple);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    private class CustomDomain implements Domain {
        private boolean alive = false;
        
        public CustomDomain() {
        }
        public void setUnexpectedDeadDelay(final int unexpectedDeadDelay) {
            if (unexpectedDeadDelay > 0) {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(unexpectedDeadDelay);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        alive = false;
                    }
                }.start();
            }
        }
        public void stop() {
            alive = false;                
        }
        public String start() {
            alive = true;
            return null;
        }
        public boolean isAlive() {
            return alive;
        }
        
        /* (non-Javadoc)
         * @see org.jtestserver.client.domain.Domain#getIPAddress()
         */
        @Override
        public String getIPAddress() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
