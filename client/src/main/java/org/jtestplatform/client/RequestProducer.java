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

import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainManager;
import org.jtestplatform.common.message.FrameworkTests;
import org.jtestplatform.common.message.GetFrameworkTests;
import org.jtestplatform.common.message.GetTestFrameworks;
import org.jtestplatform.common.message.TestFrameworks;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Producer of {@link org.jtestplatform.client.Request}s from a {@link org.jtestplatform.cloud.domain.DomainManager}.
 * Each produced request is added to a {@link java.util.concurrent.BlockingQueue}, which could be used by consumers.
 *
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class RequestProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestProducer.class);

    private final BlockingQueue<Request> requests;

    public RequestProducer(BlockingQueue<Request> requests) {
        this.requests = requests;
    }

    public void produce(DomainManager domainManager) throws Exception {
        LOGGER.info("STARTED");
        try {
            TransportProvider transportProvider = domainManager;
            TransportHelper transportHelper = createTransportHelper();

            List<Platform> platforms = domainManager.getPlatforms();

            for (Platform platform : platforms) {
                Transport transport = transportProvider.get(platform);

                //TODO we assume that all frameworks are available on each server. check it ?
                TestFrameworks testFrameworks = (TestFrameworks) transportHelper
                    .sendRequest(transport, GetTestFrameworks.INSTANCE);

                for (String testFramework : testFrameworks.getFrameworks()) {
                    FrameworkTests tests = getFrameworkTests(transportHelper, transport, testFramework);
                    for (String test : tests.getTests()) {
                        final Request request = new Request(platform, testFramework, test);
                        LOGGER.info("producing {}", request);
                        requests.put(request);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } finally {
            requests.put(Request.END);
            LOGGER.info("FINISHED");
        }
    }

    private FrameworkTests getFrameworkTests(TransportHelper transportHelper, Transport transport, String testFramework)
        throws TransportException {
        final GetFrameworkTests requestMessage = new GetFrameworkTests(testFramework);
        return (FrameworkTests) transportHelper.sendRequest(transport, requestMessage);
    }

    TransportHelper createTransportHelper() {
        return new TransportHelper();
    }
}
