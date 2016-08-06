/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.cloud.domain.watchdog;

import com.google.code.tempusfugit.temporal.Sleeper;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class WatchDogThread extends Thread {
    private final WatchDog watchDog;
    private final Sleeper sleeper;

    WatchDogThread(WatchDog watchDog, Sleeper sleeper) {
        super("WatchDogThread");
        this.watchDog = watchDog;
        this.sleeper = sleeper;

        setDaemon(true);
        start();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            if (watchDog.isWatching()) {
                watchDog.watchDomains();
            }

            try {
                sleeper.sleep();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
