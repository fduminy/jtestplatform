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
package org.jtestplatform.common.transport;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test a client and a server using {@link org.jtestplatform.common.transport.UDPTransport}.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class UDPClientServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UDPClientServerTest.class);

    private final String REQUEST = "REQUEST";
    private final String ANSWER = "ANSWER";

    private final int SERVER_PORT = 12000;
    private final int TIMEOUT = 1000;

    @Test
    public void testClientAndServer() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        MutableObject<String> actualRequest = new MutableObject<String>();
        executor.submit(createServer(actualRequest));
        MutableObject<String> actualAnswer = new MutableObject<String>();
        executor.submit(createClient(actualAnswer));

        MoreExecutors.shutdownAndAwaitTermination(executor, 10, MINUTES);

        assertThat(actualRequest.getValue()).as("request").isEqualTo(REQUEST);
        assertThat(actualAnswer.getValue()).as("answer").isEqualTo(ANSWER);
    }

    private Callable<Object> createServer(final MutableObject<String> actualRequest) {
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                LOGGER.info("SERVER: receiving request");
                UDPTransport transport = new UDPTransport(SERVER_PORT);
                String request = transport.receive();
                actualRequest.setValue(request);

                LOGGER.info("SERVER: sending answer");
                transport.send(ANSWER);

                LOGGER.info("SERVER: finished");
                return null;
            }
        };
    }

    private Callable<Object> createClient(final MutableObject<String> actualAnswer) {
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                LOGGER.info("CLIENT: sending request");
                UDPTransport transport = new UDPTransport(InetAddress.getLocalHost(), SERVER_PORT, TIMEOUT);
                transport.send(REQUEST);

                LOGGER.info("CLIENT: receiving answer");
                String answer = transport.receive();
                actualAnswer.setValue(answer);

                LOGGER.info("CLIENT: finished");
                return null;
            }
        };
    }
}
