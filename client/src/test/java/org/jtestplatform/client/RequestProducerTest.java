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
import org.jtestplatform.common.transport.TransportHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.Utils.*;
import static org.mockito.Mockito.*;

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
}