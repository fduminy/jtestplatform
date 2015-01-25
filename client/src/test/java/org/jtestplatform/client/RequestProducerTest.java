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
package org.jtestplatform.client;

import org.assertj.core.util.Objects;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainManager;
import org.jtestplatform.common.message.GetFrameworkTests;
import org.jtestplatform.common.message.GetTestFrameworks;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.Utils.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
public class RequestProducerTest {
    @Test
    public void testCreateTransportHelper() throws Exception {
        BlockingQueue<Request> requests = mock(BlockingQueue.class);
        RequestProducer producer = new RequestProducer(requests);

        TransportHelper helper = producer.createTransportHelper();

        assertThat(helper).isExactlyInstanceOf(TransportHelper.class);
    }

    @Test
    public void testProduce() throws Exception {
        // preparation
        BlockingQueue<Request> requests = mock(BlockingQueue.class);
        Transport transport = mock(Transport.class);
        DomainManager domainManager = mock(DomainManager.class);
        when(domainManager.getPlatforms()).thenReturn(Arrays.asList(PLATFORM1, PLATFORM2));
        when(domainManager.get(refEq(PLATFORM1))).thenReturn(transport);
        when(domainManager.get(refEq(PLATFORM2))).thenReturn(transport);
        final TransportHelper transportHelper = spy(new Utils.MockTransportHelper(transport));
        RequestProducer producer = new RequestProducer(requests) {
            @Override
            TransportHelper createTransportHelper() {
                return transportHelper;
            }
        };

        // test
        producer.produce(domainManager);

        // verifications
        verify(domainManager, times(1)).getPlatforms();
        for (Platform platform : PLATFORMS) {
            verify(domainManager, times(1)).get(refEq(platform));
        }
        final int nbFrameworks = FRAMEWORKS.size();
        final int nbPlatforms = PLATFORMS.size();
        verify(transportHelper, times(nbPlatforms)).send(refEq(transport), refEq(GetTestFrameworks.INSTANCE));
        verify(transportHelper, times(nbPlatforms)).send(refEq(transport), eqMessage(new GetFrameworkTests(FRAMEWORK1)));
        verify(transportHelper, times(nbPlatforms)).send(refEq(transport), eqMessage(new GetFrameworkTests(FRAMEWORK2)));
        verify(transportHelper, times(nbPlatforms + nbFrameworks * nbPlatforms)).receive(refEq(transport));
        List<Request> expectedRequests = new ArrayList<Request>();
        for (String test : FRAMEWORK1_TESTS) {
            expectedRequests.add(new Request(PLATFORM1, FRAMEWORK1, test));
        }
        for (String test : FRAMEWORK1_TESTS) {
            expectedRequests.add(new Request(PLATFORM2, FRAMEWORK1, test));
        }
        for (String test : FRAMEWORK2_TESTS) {
            expectedRequests.add(new Request(PLATFORM2, FRAMEWORK2, test));
        }
        for (String test : FRAMEWORK2_TESTS) {
            expectedRequests.add(new Request(PLATFORM1, FRAMEWORK2, test));
        }
        expectedRequests.add(Request.END);
        ArgumentCaptor<Request> actualRequests = ArgumentCaptor.forClass(Request.class);
        verify(requests, times(expectedRequests.size())).put(actualRequests.capture());
        assertThat(actualRequests.getAllValues()).usingElementComparator(new RequestComparator()).as("requests").containsOnlyOnce(expectedRequests.toArray(new Request[0]));
        verifyNoMoreInteractions(requests, domainManager, transport, transportHelper);
    }

    @Theory
    public void testProduce_failure(FailureType failure) throws Exception {
        // preparation
        BlockingQueue<Request> requests = mock(BlockingQueue.class);
        failure.applyTo(requests);
        Transport transport = mock(Transport.class);
        DomainManager domainManager = mock(DomainManager.class);
        when(domainManager.getPlatforms()).thenReturn(Arrays.asList(PLATFORM1, PLATFORM2));
        when(domainManager.get(refEq(PLATFORM1))).thenReturn(transport);
        when(domainManager.get(refEq(PLATFORM2))).thenReturn(transport);
        failure.applyTo(domainManager);
        final TransportHelper transportHelper = spy(failure.applyTo(new MockTransportHelper(transport)));
        RequestProducer producer = new RequestProducer(requests) {
            @Override
            TransportHelper createTransportHelper() {
                return transportHelper;
            }
        };

        // test
        try {
            producer.produce(domainManager);
            fail("a failure was expected");
        } catch (Exception e) {
            failure.assertExpected(e);
        }

        // verifications
        ArgumentCaptor<Request> actualRequests = ArgumentCaptor.forClass(Request.class);
        verify(requests, atLeastOnce()).put(actualRequests.capture());
        final List<Request> allRequests = actualRequests.getAllValues();
        assertThat(allRequests).endsWith(Request.END);
        verifyNoMoreInteractions(requests);
    }

    private GetFrameworkTests eqMessage(GetFrameworkTests getFrameworkTests) {
        return argThat(new GetFrameworkTestsMatcher(getFrameworkTests));
    }

    static class RequestComparator implements Comparator<Request> {
        @Override
        public int compare(Request o1, Request o2) {
            if (!Objects.areEqual(o1.getPlatform(), o2.getPlatform())) {
                return -1; // could be +1 too
            }

            int result = o1.getTestFramework().compareTo(o2.getTestFramework());
            if (result != 0) {
                return result;
            }

            return o1.getTestName().compareTo(o2.getTestName());
        }
    }

    static class GetFrameworkTestsMatcher extends BaseMatcher<GetFrameworkTests> {
        private final GetFrameworkTests request;

        GetFrameworkTestsMatcher(GetFrameworkTests request) {
            this.request = request;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof GetFrameworkTests)) {
                return false;
            }

            GetFrameworkTests otherRequest = (GetFrameworkTests) o;
            return Objects.areEqual(otherRequest.getFramework(), request.getFramework());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("GetFrameworkTests(");
            description.appendText(request.getFramework()).appendText(")");
        }
    }

    private static enum FailureType {
        QUEUE_InterruptedException(InterruptedException.class, false),
        QUEUE_ClassCastException(ClassCastException.class, false),
        QUEUE_NullPointerException(NullPointerException.class, false),
        QUEUE_IllegalArgumentException(IllegalArgumentException.class, false),
        DOMAIN_MANAGER(null, true),
        TRANSPORT_HELPER_SEND(null, true),
        TRANSPORT_HELPER_RECEIVE(null, true);

        private final Exception queueError;
        private final TransportException domainError;
        private final TransportException sendError;
        private final TransportException receiveError;

        private FailureType(Class<? extends Exception> queueFailureClass, boolean domainFailure) {
            this(queueFailureClass, domainFailure, false, false);
        }

        private FailureType(Class<? extends Exception> queueFailureClass, boolean domainFailure,
                            boolean sendFailure, boolean receiveFailure) {
            queueError = newException(queueFailureClass);
            domainError = domainFailure ? newException(TransportException.class) : null;
            sendError = sendFailure ? newException(TransportException.class) : null;
            receiveError = receiveFailure ? newException(TransportException.class) : null;
        }

        private static <T extends Exception> T newException(Class<T> failureClass) {
            if (failureClass == null) {
                return null;
            }

            try {
                return failureClass.getConstructor(String.class).newInstance("Simulated failure");
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public void applyTo(BlockingQueue<Request> requests) {
            if (queueError != null) {
                try {
                    doThrow(queueError).when(requests).put(any(Request.class));
                } catch (InterruptedException e) {
                    // we should never go here
                }
            }
        }

        public void applyTo(DomainManager domainManager) {
            if (domainError != null) {
                try {
                    doThrow(domainError).when(domainManager).get(any(Platform.class));
                } catch (TransportException e) {
                    // we should never go here
                }
            }
        }

        public void assertExpected(Exception actualException) {
            if (queueError != null) {
                assertThat(actualException).isEqualTo(queueError);
            } else if (domainError != null) {
                assertThat(actualException).isEqualTo(domainError);
            } else if (sendError != null) {
                assertThat(actualException).isEqualTo(sendError);
            } else if (receiveError != null) {
                assertThat(actualException).isEqualTo(receiveError);
            }
        }

        public MockTransportHelper applyTo(MockTransportHelper mockTransportHelper) {
            mockTransportHelper.setSendError(sendError);
            mockTransportHelper.setReceiveError(receiveError);
            return mockTransportHelper;
        }
    }
}