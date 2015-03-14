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

import com.google.code.tempusfugit.temporal.Conditions;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static com.google.code.tempusfugit.concurrency.CountDownLatchWithTimeout.await;
import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static java.lang.Thread.State.TERMINATED;
import static java.lang.Thread.State.WAITING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
@RunWith(Theories.class)
public class LoadBalancerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerTest.class);

    @DataPoint
    public static final String[] NO_ELEMENTS = {};
    @DataPoint
    public static final String[] ONE_ELEMENT = {"1"};
    @DataPoint
    public static final String[] TWO_ELEMENTS = {"1", "2"};

    @Test
    public void testConstructor_default() {
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>();
        assertThat(loadBalancer.size()).isEqualTo(0);
    }

    @Theory
    public void testConstructor_fromList(String[] elements) {
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>(Arrays.asList(elements));
        assertThat(loadBalancer.size()).isEqualTo(elements.length);
    }

    @Theory
    public void testSize(String[] elements) {
        // prepare
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>();
        for (String element : elements) {
            loadBalancer.add(element);
        }

        // test and verify
        assertThat(loadBalancer.size()).isEqualTo(elements.length);
    }

    @Test
    public void testClear() {
        // prepare
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>(Arrays.asList(TWO_ELEMENTS));

        // test
        loadBalancer.clear();

        // verify
        assertThat(loadBalancer.size()).isEqualTo(0);
    }

    @Test
    public void testGetNext_emptyList() throws TimeoutException, InterruptedException {
        // prepare
        final LoadBalancer<String> loadBalancer = new LoadBalancer<String>();
        final CountDownLatch startup = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(1);

        // test
        Thread thread = new Thread() {
            @Override
            public void run() {
                startup.countDown();
                loadBalancer.getNext();
                end.countDown();
            }
        };
        thread.start();
        await(startup).with(seconds(1));

        // verify
        try {
            await(end).with(seconds(1));
            fail("must block when the list is empty");
        } catch (TimeoutException e) {
            assertThat(thread.getState()).as("thread state").isEqualTo(WAITING);
            LOGGER.info("Thread is blocked : OK");
        }

        loadBalancer.add("element"); // this must unblock the thread
        await(end).with(seconds(1));
        LOGGER.info("Thread was unblocked : OK");

        waitOrTimeout(Conditions.is(thread, TERMINATED), timeout(seconds(1)));
    }

    @Test
    public void testGetNext_nonEmptyList() {
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>(Arrays.asList(TWO_ELEMENTS));

        assertThat(loadBalancer.getNext()).isSameAs(TWO_ELEMENTS[0]);
        assertThat(loadBalancer.getNext()).isSameAs(TWO_ELEMENTS[1]);
    }

    @Test
    public void testRemove_lastElement() {
        String firstElement = "firstElement";
        String lastElement = "lastElement";
        List<String> elements = Arrays.asList(firstElement, "2", lastElement);
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>(elements);
        
        // get all elements except the last one
        for (int i = 0; i < elements.size() - 1; i++) {
            loadBalancer.getNext();
        }
        
        // remove the last element
        boolean removed = loadBalancer.remove(lastElement);
        
        // check we get the first element instead of the last element
        assertThat(removed).isTrue();
        assertThat(loadBalancer.getNext()).isEqualTo(firstElement);
    }

    @Test
    public void testRemove_notLastElement() {
        String firstElement = "firstElement";
        String secondElement = "secondElement";
        String thirdElement = "thirdElement";
        List<String> elements = Arrays.asList(firstElement, secondElement, thirdElement);
        LoadBalancer<String> loadBalancer = new LoadBalancer<String>(elements);
        
        // get the first element
        loadBalancer.getNext();
        
        // remove the second element
        boolean removed = loadBalancer.remove(secondElement);
        
        // check we get the third element instead of the second element
        assertThat(removed).isTrue();
        assertThat(loadBalancer.getNext()).isEqualTo(thirdElement);
    }
}
