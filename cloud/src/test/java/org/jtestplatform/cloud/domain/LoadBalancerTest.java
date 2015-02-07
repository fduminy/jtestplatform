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
package org.jtestplatform.cloud.domain;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class LoadBalancerTest {
    @DataPoint
    public static final Integer NB_THREADS1 = 1;
    @DataPoint
    public static final Integer NB_THREADS2 = 10;
    @DataPoint
    public static final Integer NB_THREADS3 = 100;
    
    @DataPoint
    public static final String[] ELEMENTS1 = {};
    @DataPoint
    public static final String[] ELEMENTS2 = {"1"};
    @DataPoint
    public static final String[] ELEMENTS3 = {"1", "2"};
    
    @Theory
    public void testSize(String[] elements) {
        LoadBalancer<String> loadBalancer = createLoadBalancer(elements);        
        assertEquals(elements.length, loadBalancer.size());
    }
    
    @Theory
    public void testGetNext_emptyList(Integer nbElements) {
        final LoadBalancer<String> loadBalancer = new LoadBalancer<String>();
        Thread[] threads = createThreads(loadBalancer, nbElements);
        
        assertEquals("must block when the list is empty", 0, countGotValue(threads));

        for (int i = 0; i < nbElements; i++) {
            loadBalancer.add("str");
            sleep(100);
            assertEquals("must be unblocked when the list become non empty", i + 1, countGotValue(threads));
        }
    }

    @Theory
    public void testGetNext_nonEmptyList(Integer nbElements) {
        String[] elements = new String[nbElements];
        Arrays.fill(elements, "str");
        final LoadBalancer<String> loadBalancer = createLoadBalancer(elements);
        Thread[] threads = createThreads(loadBalancer, nbElements);
        
        sleep(100);
        assertEquals("must not block when the list is not empty", nbElements.intValue(), countGotValue(threads));
    }

    @Test
    public void testRemove_lastElement() {
        String firstElement = "firstElement";
        String lastElement = "lastElement";
        String[] elements = new String[]{firstElement, "2", lastElement};
        final LoadBalancer<String> loadBalancer = createLoadBalancer(elements);
        
        // get all elements except the last one
        for (int i = 0; i < elements.length - 1; i++) {
            loadBalancer.getNext();
        }
        
        // remove the last element
        loadBalancer.remove(lastElement);
        
        // check we get the first element instead of the last element
        assertEquals(firstElement, loadBalancer.getNext());
    }

    @Test
    public void testRemove_notLastElement() {
        String firstElement = "firstElement";
        String secondElement = "secondElement";
        String thirdElement = "thirdElement";
        String[] elements = new String[]{firstElement, secondElement, thirdElement};
        final LoadBalancer<String> loadBalancer = createLoadBalancer(elements);
        
        // get the first element
        loadBalancer.getNext();
        
        // remove the second element
        loadBalancer.remove(secondElement);
        
        // check we get the third element instead of the second element
        assertEquals(thirdElement, loadBalancer.getNext());
    }
    
    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    private LoadBalancer<String> createLoadBalancer(String... elements) {
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>();
        for (String element : elements) {
            loadBalancer.add(element);
        }
        return loadBalancer;
    }
    
    private Thread[] createThreads(LoadBalancer<String> loadBalancer, int nbThreads) {
        Thread[] result = new Thread[nbThreads];
        for (int i = 0; i < nbThreads; i++) {
            result[i] = new Thread(loadBalancer);
        }

        // be sure all threads have started
        boolean oneNotStarted = false;
        do {
            sleep(100);

            for (Thread t : result) {
                if(!t.started.get()) {
                    oneNotStarted = true;
                    break;
                }
            }
        } while (oneNotStarted);
        
        return result;
    }
    
    private int countGotValue(Thread[] threads) {
        int count = 0;
        for (Thread t : threads) {
            if (t.gotValue.get()) {
                count++;
            }
        }
        return count;
    }
    
    private static class Thread extends java.lang.Thread {
        final AtomicBoolean started = new AtomicBoolean(false);
        final AtomicBoolean gotValue = new AtomicBoolean(false);
        final LoadBalancer<String> loadBalancer;
        
        public Thread(LoadBalancer<String> loadBalancer) {
            this.loadBalancer = loadBalancer;
            start();
        }
        
        @Override
        public void run() {
            started.set(true);
            loadBalancer.getNext();
            gotValue.set(true);
        }
    }
}
