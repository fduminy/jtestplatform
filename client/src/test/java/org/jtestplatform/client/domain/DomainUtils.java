/**
 * 
 */
package org.jtestplatform.client.domain;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jtestplatform.client.domain.watchdog.WatchDog;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class DomainUtils {
    
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
         * @see org.jtestplatform.client.domain.Domain#getIPAddress()
         */
        @Override
        public String getIPAddress() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    public static Domain[] createFixedStateProcesses(final Boolean fixedState, WatchDog watchDog, int nbDomains) throws IOException {
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
