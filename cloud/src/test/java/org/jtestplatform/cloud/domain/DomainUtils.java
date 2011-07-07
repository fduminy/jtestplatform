/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2011  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.cloud.domain;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import org.jtestplatform.cloud.domain.watchdog.WatchDog;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class DomainUtils {
    private DomainUtils() {        
    }

    private static File getResource(String resource) {
        URL url = DomainUtils.class.getResource("/" + resource);
        File f = new File(url.getFile());
        if (!f.exists()) {
            throw new RuntimeException("file not found : " + f.getAbsolutePath());
        }
        return f;
    }
    
    public static String getCDROM() {
        return getResource("microcore_2.7.iso").getAbsolutePath();
    }
    
    public static File getConfigFile() {
        return getResource("cloud.xml");
    }
    
    public static CustomDomain[] createCustomDomain(long multiple, int nbDomains, WatchDog watchDog, int pollInterval) {
        CustomDomain[] p = new CustomDomain[nbDomains];
        for (int i = 0; i < nbDomains; i++) {
            p[i] = mock(CustomDomain.class);
            p[i].setUnexpectedDeadDelay((int) (multiple * pollInterval));
            doCallRealMethod().when(p[i]).isAlive();
            doCallRealMethod().when(p[i]).start();
            doCallRealMethod().when(p[i]).stop();

            if (watchDog != null) {
                watchDog.watch(p[i]);
            }
        }
        return p;
    }

    public static class CustomDomain implements Domain {
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
        @Override
        public void stop() {
            alive = false;
        }
        @Override
        public String start() {
            alive = true;
            return null;
        }
        @Override
        public boolean isAlive() {
            return alive;
        }
        @Override
        public String getIPAddress() {
            return null;
        }
    }

    public static Domain[] createFixedStateProcesses(final Boolean fixedState, WatchDog watchDog, int nbDomains) throws DomainException {
        Domain[] p = new Domain[nbDomains];
        for (int i = 0; i < nbDomains; i++) {
            p[i] = mock(Domain.class);
            when(p[i].isAlive()).thenAnswer(new Answer<Boolean>() {

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {
                    return fixedState;
                }
            });

            if (watchDog != null) {
                watchDog.watch(p[i]);
            }
        }
        return p;
    }
}
