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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainManager;
import org.jtestplatform.common.message.*;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.Utils.*;
import static org.mockito.Mockito.*;

public class RequestProducerTest {
    private static final String FRAMEWORK1 = "framework1";
    private static final String FRAMEWORK2 = "framework2";
    private static final Set<String> FRAMEWORKS = new HashSet<String>(Arrays.asList(FRAMEWORK1, FRAMEWORK2));
    private static final String TEST1 = "test1";
    private static final String TEST2 = "test2";
    private static final String TEST3 = "test3";
    private static final String TEST4 = "test4";
    private static final String TEST5 = "test5";
    private static final Set<String> FRAMEWORK1_TESTS = new HashSet<String>(Arrays.asList(TEST1, TEST2));
    private static final Set<String> FRAMEWORK2_TESTS = new HashSet<String>(Arrays.asList(TEST3, TEST4, TEST5));

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
        when(domainManager.get(eq(PLATFORM1))).thenReturn(transport);
        when(domainManager.get(eq(PLATFORM2))).thenReturn(transport);
        final TransportHelper transportHelper = spy(new MockTransportHelper(transport));
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
            verify(domainManager, times(1)).get(eq(platform));
        }
        final int nbFrameworks = FRAMEWORKS.size();
        final int nbPlatforms = PLATFORMS.size();
        verify(transportHelper, times(nbPlatforms)).send(eq(transport), refEq(GetTestFrameworks.INSTANCE));
        verify(transportHelper, times(nbPlatforms)).send(eq(transport), eqMessage(new GetFrameworkTests(FRAMEWORK1)));
        verify(transportHelper, times(nbPlatforms)).send(eq(transport), eqMessage(new GetFrameworkTests(FRAMEWORK2)));
        verify(transportHelper, times(nbPlatforms + nbFrameworks * nbPlatforms)).receive(eq(transport));
        Set<Request> expectedRequests = new HashSet<Request>();
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
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(requests, times(expectedRequests.size())).put(argument.capture());
        assertThat(argument.getAllValues()).usingElementComparator(new RequestComparator()).as("requests").containsOnlyOnce(expectedRequests.toArray(new Request[0]));
        verifyNoMoreInteractions(requests, domainManager, transport, transportHelper);
    }

    private static class MockTransportHelper extends TransportHelper {
        private static final Logger LOGGER = LoggerFactory.getLogger(MockTransportHelper.class);

        private static enum STATE {
            INIT,
            GET_FRAMEWORKS,
            GET_TESTS;
        }

        ;
        private STATE state = STATE.INIT;
        private String framework;
        private final Transport expectedTransport;

        private MockTransportHelper(Transport expectedTransport) {
            this.expectedTransport = expectedTransport;
        }

        @Override
        public void send(Transport transport, Message message) throws TransportException {
            LOGGER.info("BEGIN send(transport, {}): state={}", message.getClass().getSimpleName(), state);
            if (state != STATE.INIT) {
                throw new IllegalStateException("illegal state: " + state);
            }
            if (transport != expectedTransport) {
                throw new IllegalArgumentException("illegal transport: " + transport);
            }

            if (message instanceof GetTestFrameworks) {
                state = STATE.GET_FRAMEWORKS;
                framework = null;
            } else if (message instanceof GetFrameworkTests) {
                framework = ((GetFrameworkTests) message).getFramework();
                state = STATE.GET_TESTS;
            } else {
                throw new IllegalStateException("illegal message: " + message);
            }
            LOGGER.info("END send(...): state={}", state);
        }

        @Override
        public Message receive(Transport transport) throws TransportException {
            LOGGER.info("BEGIN receive(transport): state={}", state);
            Message result;
            switch (state) {
                case GET_FRAMEWORKS:
                    state = STATE.INIT;
                    result = new TestFrameworks(FRAMEWORKS);
                    break;
                case GET_TESTS:
                    state = STATE.INIT;
                    if (FRAMEWORK1.equals(framework)) {
                        result = new FrameworkTests(FRAMEWORK1_TESTS);
                    } else if (FRAMEWORK2.equals(framework)) {
                        result = new FrameworkTests(FRAMEWORK2_TESTS);
                    } else {
                        throw new IllegalStateException("unknown framework: " + framework);
                    }
                    break;
                case INIT:
                default:
                    throw new IllegalStateException("illegal state: " + state);
            }
            LOGGER.info("END receive(transport): state={} RETURN {}({})", new Object[]{state, result.getClass().getSimpleName(),
                    ((result instanceof TestFrameworks) ? ((TestFrameworks) result).getFrameworks() : ((FrameworkTests) result).getTests())});
            return result;
        }

        @Override
        public void stop(Transport transport) throws IOException {
            throw new IOException("stop() shouldn't be called");
        }
    }

    private GetFrameworkTests eqMessage(GetFrameworkTests getFrameworkTests) {
        return argThat(new GetFrameworkTestsMatcher(getFrameworkTests));
    }

    static class RequestComparator implements Comparator<Request> {
        @Override
        public int compare(Request o1, Request o2) {
            if (!o1.getPlatform().equals(o2.getPlatform())) {
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
            return otherRequest.getFramework().equals(request.getFramework());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("GetFrameworkTests(");
            description.appendText(request.getFramework()).appendText(")");
        }
    }
}