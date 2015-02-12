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

import com.google.code.tempusfugit.temporal.Sleeper;
import org.jtestplatform.cloud.domain.Utils.MethodAnswer;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
public class WatchDogThreadTest {
    @Theory
    public void testRun(boolean watching) throws Exception {
        // prepare
        MethodAnswer<Object> sleepMethod = new MethodAnswer<Object>();
        Sleeper sleeper = mock(Sleeper.class);
        doAnswer(sleepMethod).when(sleeper).sleep();
        WatchDog watchDog = mock(WatchDog.class);
        when(watchDog.isWatching()).thenReturn(watching);

        // test
        WatchDogThread thread = new WatchDogThread(watchDog, sleeper);
        waitOrTimeout(sleepMethod.called(), timeout(seconds(10)));

        // verify
        assertThat(thread.isAlive()).as("thread.alive").isTrue();
        verify(watchDog, atLeastOnce()).isWatching();
        verify(watchDog, watching ? atLeastOnce() : never()).watchDomains();
    }
}