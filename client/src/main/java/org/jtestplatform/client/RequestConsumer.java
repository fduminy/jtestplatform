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
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.StopWatch;
import com.google.code.tempusfugit.temporal.Timer;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.RunTest;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Consumer of {@link org.jtestplatform.client.Request}s provided by a {@link java.util.concurrent.BlockingQueue}.
 * Each consumed request is sent for execution through a {@link org.jtestplatform.common.transport.Transport} provided by a
 * {@link org.jtestplatform.cloud.TransportProvider}. The result is sent to a {@link org.jtestplatform.client.TestReporter}.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class RequestConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestConsumer.class);

    private final BlockingQueue<Request> requests;
    private final Clock clock;

    public RequestConsumer(BlockingQueue<Request> requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    public void consume(TransportProvider transportProvider, TestReporter reporter) throws Exception {
        LOGGER.info("STARTED");
        TransportHelper transportHelper = createTransportHelper();
        Request request = null;
        while (request != Request.END) {
            while ((request = requests.poll(1, SECONDS)) != null) {
                LOGGER.info("consuming {}", request);

                if (request == Request.END) {
                    break;
                }
                Platform platform = request.getPlatform();
                Transport transport = transportProvider.get(platform);

                StopWatch stopWatch = new Timer(clock);
                TestResult testResult = runTest(transportHelper, request, transport);
                stopWatch.lap();
                Duration testDuration = stopWatch.elapsedTime();

                reporter.report(platform, testResult, testDuration);
            }
        }
        LOGGER.info("FINISHED");
    }

    protected TestResult runTest(TransportHelper transportHelper, Request request, Transport transport)
        throws TransportException {
        RunTest requestMessage = new RunTest(request.getTestFramework(), request.getTestName());
        return (TestResult) transportHelper.sendRequest(transport, requestMessage);
    }

    TransportHelper createTransportHelper() {
        return new TransportHelper();
    }
}
