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

import com.google.code.tempusfugit.temporal.*;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jtestplatform.cloud.domain.Domain;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainUtils;
import org.jtestplatform.cloud.domain.DomainUtils.FixedState;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.google.code.tempusfugit.temporal.Conditions.isAlive;
import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.cloud.domain.DomainUtils.FixedState.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class WatchDogTest {
    private WatchDog watchDog;

    @After
    public void tearDown() {
        watchDog.stopWatching();
        watchDog = null;
    }

    @Test
    public void testStart() throws TimeoutException, InterruptedException {
        // prepare
        MutableObject<WatchDogThread> thread = new MutableObject<WatchDogThread>();
        watchDog = createWatchDog(null, thread);

        // test
        watchDog.start();

        // verify
        assertThat(thread.getValue()).isNotNull();
        waitOrTimeout(isAlive(thread.getValue()), Timeout.timeout(seconds(10)));
    }

    @Theory
    public void testWatchDomains(FixedState state, Boolean unwatch) throws IOException, DomainException {
        // prepare
        watchDog = createWatchDog(state);
        Domain domain = DomainUtils.createFixedStateDomain(state);
        watchDog.watch(domain);
        if (unwatch) {
            watchDog.unwatch(domain);
        }

        // test
        watchDog.watchDomains();

        // verify
        boolean useStrategy = !unwatch && ALWAYS_DEAD.equals(state);
        verify(domain, unwatch ? never() : times(1)).isAlive();
        verify(watchDog.strategy, useStrategy ? times(1) : never()).domainDead(eq(domain), any(StopWatch.class));
        verifyNoMoreInteractions(domain, watchDog.strategy);
        verifyStateOf(domain, state); // must be after verifyNoMoreInteractions
    }

    @Theory
    public void testAddListener(FixedState fixedState) throws DomainException {
        // prepare
        watchDog = createWatchDog(fixedState);

        final ArgumentCaptor<Domain> notifications = ArgumentCaptor.forClass(Domain.class);
        WatchDogListener listener = mock(WatchDogListener.class);
        doNothing().when(listener).domainDied(notifications.capture());
        Domain domain = DomainUtils.createFixedStateDomain(fixedState);
        watchDog.watch(domain);

        // test
        watchDog.addWatchDogListener(listener);
        watchDog.watchDomains();

        // verify
        if (ALWAYS_ALIVE.equals(fixedState)) {
            // always alive => listener must never be called
            assertThat(notifications.getAllValues()).isEmpty();
        } else {
            // always dead => listener must be called
            assertThat(notifications.getAllValues()).containsExactly(domain);
        }
    }

    @Theory
    public void testRemoveListener(FixedState fixedState) throws DomainException {
        // prepare
        watchDog = createWatchDog(fixedState);
        WatchDogListener listener = mock(WatchDogListener.class);
        watchDog.addWatchDogListener(listener);
        Domain domain = DomainUtils.createFixedStateDomain(fixedState);
        watchDog.watch(domain);

        // test
        watchDog.removeWatchDogListener(listener);
        watchDog.watchDomains();

        // verify
        verify(listener, never()).domainDied(eq(domain));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testConstructor() {
        watchDog = new WatchDog(mock(Sleeper.class), mock(WatchDogStrategy.class), mock(Clock.class));

        assertFalse(watchDog.isWatching());
    }

    @Test
    public void testStartWatching() {
        // prepare
        watchDog = new WatchDog(mock(Sleeper.class), mock(WatchDogStrategy.class), mock(Clock.class));

        // test
        watchDog.startWatching();

        // verify
        assertTrue(watchDog.isWatching());
    }

    @Test
    public void testStopWatching() {
        // prepare
        watchDog = new WatchDog(mock(Sleeper.class), mock(WatchDogStrategy.class), mock(Clock.class));
        watchDog.startWatching();

        // test
        watchDog.stopWatching();

        // verify
        assertFalse(watchDog.isWatching());
    }

    private static WatchDog createWatchDog(FixedState fixedState) {
        return createWatchDog(fixedState, null);
    }

    private static WatchDog createWatchDog(FixedState fixedState, final MutableObject<WatchDogThread> thread) {
        final boolean alive = ALWAYS_ALIVE.equals(fixedState);
        Sleeper sleeper = mock(Sleeper.class);
        WatchDogStrategy strategy = mock(WatchDogStrategy.class);
        if (fixedState != null) {
            when(strategy.domainDead(any(Domain.class), any(StopWatch.class))).thenAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return !alive;
                }
            });
        }
        Clock clock = new MovableClock();
        WatchDog watchDog = new WatchDog(sleeper, strategy, clock) {
            @Override
            WatchDogThread createWatchDogThread(Sleeper sleeper) {
                WatchDogThread watchDogThread = super.createWatchDogThread(sleeper);
                if (thread != null) {
                    thread.setValue(watchDogThread);
                }
                return watchDogThread;
            }
        };
        watchDog.startWatching();
        return watchDog; 
    }
}
