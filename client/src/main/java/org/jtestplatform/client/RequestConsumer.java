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

import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.RunTest;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Consumer of {@link org.jtestplatform.client.Request}s provided by a {@link java.util.concurrent.BlockingQueue}.
 * Each consumed request is sent for execution through a {@link org.jtestplatform.common.transport.Transport} provided by a
 * {@link org.jtestplatform.cloud.TransportProvider}. The result is sent to a {@link org.jtestplatform.client.TestReporter}.
 */
public class RequestConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestConsumer.class);

    private final BlockingQueue<Request> requests;

    public RequestConsumer(BlockingQueue<Request> requests) {
        this.requests = requests;
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
                transportHelper.send(transport, new RunTest(request.getTestFramework(), request.getTestName()));
                TestResult testResult = (TestResult) transportHelper.receive(transport);
                reporter.report(platform, testResult);
            }
        }
        LOGGER.info("FINISHED");
    }

    TransportHelper createTransportHelper() {
        return new TransportHelper();
    }
}
