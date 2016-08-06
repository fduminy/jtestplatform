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
package org.jtestplatform.client;

import com.google.code.tempusfugit.temporal.Clock;
import com.google.code.tempusfugit.temporal.MovableClock;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.common.message.RunTest;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportHelper;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.JUnitTestReporterTest.DURATION1;
import static org.jtestplatform.client.JUnitTestReporterTest.DURATION2;
import static org.jtestplatform.client.Utils.PLATFORM1;
import static org.jtestplatform.client.Utils.PLATFORM2;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class RequestConsumerTest {
    @Test
    public void testCreateTransportHelper() throws Exception {
        RequestConsumer consumer = new RequestConsumer(mock(BlockingQueue.class), mock(Clock.class));

        TransportHelper helper = consumer.createTransportHelper();

        assertThat(helper).isExactlyInstanceOf(TransportHelper.class);
    }

    @Test
    public void testConsume_noEndRequest() throws Exception {
        // preparation
        BlockingQueue<Request> requests = new ArrayBlockingQueue<Request>(1);
        requests.put(new Request(PLATFORM1, "framework1", "test1"));
        final TestReporter reporter = mock(TestReporter.class);
        final TransportProvider transportProvider = mock(TransportProvider.class);
        final TransportHelper transportHelper = mock(TransportHelper.class);
        final RequestConsumer consumer = new RequestConsumer(requests, mock(Clock.class)) {
            @Override
            TransportHelper createTransportHelper() {
                return transportHelper;
            }
        };

        // test
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        final AtomicBoolean terminated = new AtomicBoolean(false);
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                consumer.consume(transportProvider, reporter);

                // we should never go there (because we don't send Request.END).
                terminated.set(true);
                return null;
            }
        });
        assertFalse(executorService.awaitTermination(1, SECONDS));

        assertThat(executorService.isTerminated()).as("terminated without Request.END").isFalse();
        assertThat(terminated.get()).as("terminated without Request.END").isFalse();
    }

    @Test
    public void testConsume() throws Exception {
        // preparation
        BlockingQueue<Request> requests = new ArrayBlockingQueue<Request>(3);
        Request request1 = new Request(PLATFORM1, "framework1", "test1");
        requests.put(request1);
        Request request2 = new Request(PLATFORM2, "framework2", "test5");
        requests.put(request2);
        requests.put(Request.END);
        TestReporter reporter = mock(TestReporter.class);
        TransportProvider transportProvider = mock(TransportProvider.class);
        Transport transport1 = mock(Transport.class);
        Transport transport2 = mock(Transport.class);
        when(transportProvider.get(refEq(PLATFORM1))).thenReturn(transport1);
        when(transportProvider.get(refEq(PLATFORM2))).thenReturn(transport2);
        MovableClock clock = new MovableClock();
        Utils.MockTransportHelper mockTransportHelper = new Utils.MockTransportHelper(transport1, transport2);
        mockTransportHelper.setTestDurations(clock, DURATION1, DURATION2);
        final TransportHelper transportHelper = spy(mockTransportHelper);
        RequestConsumer consumer = new RequestConsumer(requests, clock) {
            @Override
            TransportHelper createTransportHelper() {
                return transportHelper;
            }
        };

        // test
        consumer.consume(transportProvider, reporter);

        // verifications
        List<TestResult> receivedMessages = mockTransportHelper.getTestResults();
        TestResult testResult1 = testResult(request1);
        TestResult testResult2 = testResult(request2);
        List<TestResult> expectedMessages = Arrays.asList(testResult1, testResult2);
        assertThat(receivedMessages).usingElementComparator(new TestResultComparator()).as("testResults")
                                    .containsOnlyOnce(expectedMessages.toArray(new TestResult[0]));
        assertThat(requests).as("requests").isEmpty();
        verify(transportProvider, times(1)).get(refEq(PLATFORM1));
        verify(transportProvider, times(1)).get(refEq(PLATFORM2));
/*
        verify(transportHelper, times(1)).send(refEq(transport1), eqMessage(runTest(request1)));
        verify(transportHelper, times(1)).send(refEq(transport2), eqMessage(runTest(request2)));
        ArgumentCaptor<Transport> actualTransports = ArgumentCaptor.forClass(Transport.class);
        verify(transportHelper, times(2)).receive(actualTransports.capture());
        assertThat(actualTransports.getAllValues()).as("transports").containsOnlyOnce(new Transport[]{transport1, transport2});
*/
        verify(reporter, times(1)).report(refEq(PLATFORM1), eqTestResult(testResult1), eq(DURATION1));
        verify(reporter, times(1)).report(refEq(PLATFORM2), eqTestResult(testResult2), eq(DURATION2));
        verifyNoMoreInteractions(transportProvider, transport1, transport2, /*transportHelper,*/ reporter);
    }

    private static RunTest runTest(Request request) {
        return new RunTest(request.getTestFramework(), request.getTestName());
    }

    private static TestResult testResult(Request request) {
        return new TestResult(request.getTestFramework(), request.getTestName());
    }

    static class TestResultComparator implements Comparator<TestResult> {
        @Override
        public int compare(TestResult o1, TestResult o2) {
            int result = o1.getFramework().compareTo(o2.getFramework());
            if (result != 0) {
                return result;
            }

            result = o1.getTest().compareTo(o2.getTest());
            if (result != 0) {
                return result;
            }

            return Boolean.valueOf(o1.isSuccess()).compareTo(o2.isSuccess());
        }
    }

    private static TestResult eqTestResult(TestResult testResult) {
        return argThat(new TestResultMatcher(testResult));
    }

    static class TestResultMatcher extends BaseMatcher<TestResult> {
        private final TestResult result;

        TestResultMatcher(TestResult result) {
            this.result = result;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof TestResult)) {
                return false;
            }

            return new TestResultComparator().compare((TestResult) o, result) == 0;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("TestResult(");
            description.appendText(result.getFramework()).appendText(",");
            description.appendText(result.getTest()).appendText(",");
            description.appendText(Boolean.toString(result.isSuccess())).appendText(")");
        }
    }

    private static RunTest eqMessage(RunTest request) {
        return argThat(new RunTestMatcher(request));
    }

    static class RunTestMatcher extends BaseMatcher<RunTest> {
        private final RunTest request;

        RunTestMatcher(RunTest request) {
            this.request = request;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof RunTest)) {
                return false;
            }

            RunTest otherRequest = (RunTest) o;
            return otherRequest.getFramework().equals(request.getFramework()) &&
                   otherRequest.getTest().equals(request.getTest());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("RunTest(");
            description.appendText(request.getFramework()).appendText(",");
            description.appendText(request.getTest()).appendText(")");
        }
    }
}