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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Theories.class)
public class TestLoadBalancer {
    @DataPoint
    public static final Integer NB_THREADS1 = Integer.valueOf(1);
    @DataPoint
    public static final Integer NB_THREADS2 = Integer.valueOf(10);
    @DataPoint
    public static final Integer NB_THREADS3 = Integer.valueOf(100);
    
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
    public void testGetNextWithEmptyList(Integer nbElements) {
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
    public void testGetNextWithNonEmptyList(Integer nbElements) {
        String[] elements = new String[nbElements];
        Arrays.fill(elements, "str");
        final LoadBalancer<String> loadBalancer = createLoadBalancer(elements);
        Thread[] threads = createThreads(loadBalancer, nbElements);
        
        sleep(100);
        assertEquals("must not block when the list is not empty", nbElements.intValue(), countGotValue(threads));
    }
    
    @Theory
    public void testRemoveLastElement() {
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

    @Theory
    public void testRemoveNotLastElement() {
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
        for (int i = 0; i < elements.length; i++) {
            loadBalancer.add(elements[i]);
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
