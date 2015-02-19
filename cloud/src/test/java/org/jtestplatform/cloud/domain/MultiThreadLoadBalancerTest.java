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

import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.google.code.tempusfugit.concurrency.annotations.Repeating;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.synchronizedList;
import static org.jtestplatform.cloud.domain.Utils.generateStringList;
import static org.jtestplatform.common.transport.Utils.verifyEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 */
@RunWith(Enclosed.class)
public class MultiThreadLoadBalancerTest {
    private static final int NB_THREADS = 100;
    private static final int NB_REPEATS = 100;
    private static final int NB_CALLS = NB_REPEATS * NB_THREADS;

    public static class ClearMethodTest {
        private static final LoadBalancer<String> loadBalancer = new LoadBalancer<String>();

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testClear() {
            loadBalancer.add("element");
            loadBalancer.clear();
        }

        @AfterClass
        public static void afterClass() {
            String errors = verifyEquals("clear", "size", 0, loadBalancer.size());
            assertTrue("Thread safety test has failed : " + errors, errors == null);
        }
    }

    public static class AddMethodTest {
        private static final LoadBalancer<String> loadBalancer = new LoadBalancer<String>();
        private static final AtomicInteger counter = new AtomicInteger(0);

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testAdd() {
            String element = "ELEMENT" + counter.getAndIncrement();
            loadBalancer.add(element);
        }

        @AfterClass
        public static void afterClass() {
            String errors = verifyEquals("add", "size", NB_CALLS, loadBalancer.size());
            assertTrue("Thread safety test has failed : " + errors, errors == null);
        }
    }

    public static class GetNextMethodTest {
        private static final List<String> elements = generateStringList(2);
        private static List<AtomicInteger> counters;
        private static LoadBalancer<String> loadBalancer;

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @BeforeClass
        public static void beforeClass() {
            counters = new ArrayList<AtomicInteger>();
            for (int i = 0; i < elements.size(); i++) {
                counters.add(new AtomicInteger(0));
            }
            loadBalancer = new LoadBalancer<String>(elements);
        }

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testGetNext() {
            String value = loadBalancer.getNext();
            int index = elements.indexOf(value);
            counters.get(index).incrementAndGet();
        }

        @AfterClass
        public static void afterClass() {
            int expectedCount = NB_CALLS / counters.size();
            StringBuilder errors = new StringBuilder();
            int i = 0;
            for (AtomicInteger counter : counters) {
                String error = verifyEquals("getNext", "counter(\"" + elements.get(i) + "\"", expectedCount, counter.get());
                if (error != null) {
                    errors.append(error);
                }
                i++;
            }
            assertTrue("Thread safety test has failed : " + errors, errors.length() == 0);
        }
    }


    public static class RemoveMethodTest {
        private static final List<String> elements = synchronizedList(generateStringList(NB_CALLS));
        private static final LoadBalancer<String> loadBalancer = new LoadBalancer<String>(elements);

        @Rule
        public final ConcurrentRule concurrentRule = new ConcurrentRule();
        @Rule
        public final RepeatingRule repeatingRule = new RepeatingRule();

        @Test
        @Concurrent(count = NB_THREADS)
        @Repeating(repetition = NB_REPEATS)
        public void testRemove() {
            loadBalancer.remove(elements.remove(0));
        }

        @AfterClass
        public static void afterClass() {
            String errors = verifyEquals("remove", "size", 0, loadBalancer.size());
            assertTrue("Thread safety test has failed : " + errors, errors == null);
        }
    }
}
