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
package org.jtestplatform.cloud.domain.watchdog;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.MovableClock;
import com.google.code.tempusfugit.temporal.StopWatch;
import com.google.code.tempusfugit.temporal.Timer;
import org.jtestplatform.cloud.domain.Domain;
import org.junit.Test;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class DefaultWatchDogStrategyTest {
    private final Duration MAX_ZOMBIE_DURATION = millis(2);

    @Test
    public void testDomainDead_after_0_Milliseconds() throws Exception {
        testDomainDead(millis(0), false);
    }

    @Test
    public void testDomainDead_after_1_Milliseconds() throws Exception {
        testDomainDead(millis(1), false);
    }

    @Test
    public void testDomainDead_after_2_Milliseconds() throws Exception {
        testDomainDead(MAX_ZOMBIE_DURATION, false);
    }

    @Test
    public void testDomainDead_after_3_Milliseconds() throws Exception {
        testDomainDead(MAX_ZOMBIE_DURATION.plus(millis(1)), true);
    }

    private void testDomainDead(Duration timeIncrement, boolean expectDead) throws Exception {
        DefaultWatchDogStrategy strategy = new DefaultWatchDogStrategy(MAX_ZOMBIE_DURATION);
        Domain domain = mock(Domain.class);
        MovableClock clock = new MovableClock();
        StopWatch stopWatch = new Timer(clock);
        clock.incrementBy(timeIncrement);

        boolean actuallyDead = strategy.domainDead(domain, stopWatch);

        assertThat(actuallyDead).as("actuallyDead").isEqualTo(expectDead);
    }
}