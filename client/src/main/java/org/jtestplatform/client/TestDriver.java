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
import org.jtestplatform.cloud.domain.DefaultDomainManager;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MINUTES;

public class TestDriver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDriver.class);

    TestDriver() {
    }

    public void runTests(File cloudConfigFile, File reportDirectory) throws Exception {
        BlockingQueue<Request> requests = createRequestQueue();
        RequestConsumer requestConsumer = createRequestConsumer(requests);
        RequestProducer requestProducer = createRequestProducer(requests);
        DomainManager domainManager = createDomainManager(cloudConfigFile);
        final JUnitTestReporter reporter = new JUnitTestReporter(reportDirectory);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(new ConsumerTask(requestConsumer, domainManager, reporter));
        executor.submit(new ProducerTask(requestProducer, domainManager));
        shutdown(executor);
    }

    DomainManager createDomainManager(File cloudConfigFile) throws FileNotFoundException, DomainException {
        return new DefaultDomainManager(new FileReader(cloudConfigFile));
    }

    BlockingQueue<Request> createRequestQueue() {
        return new LinkedBlockingDeque<Request>();
    }

    RequestProducer createRequestProducer(BlockingQueue<Request> requests) {
        return new RequestProducer(requests);
    }

    RequestConsumer createRequestConsumer(BlockingQueue<Request> requests) {
        return new RequestConsumer(requests);
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(10, MINUTES)) { //TODO this should be a parameter
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(1, MINUTES)) {
                    LOGGER.error("executor didn't terminate properly");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private static class ConsumerTask implements Callable<Void> {
        private final RequestConsumer requestConsumer;
        private final TransportProvider transportProvider;
        private final TestReporter reporter;

        private ConsumerTask(RequestConsumer requestConsumer, TransportProvider transportProvider, TestReporter reporter) {
            this.requestConsumer = requestConsumer;
            this.transportProvider = transportProvider;
            this.reporter = reporter;
        }

        @Override
        public Void call() throws Exception {
            requestConsumer.consume(transportProvider, reporter);
            return null;
        }
    }

    private static class ProducerTask implements Callable<Void> {
        private final RequestProducer requestProducer;
        private final DomainManager domainManager;

        private ProducerTask(RequestProducer requestProducer, DomainManager domainManager) {
            this.requestProducer = requestProducer;
            this.domainManager = domainManager;
        }

        @Override
        public Void call() throws Exception {
            requestProducer.produce(domainManager);
            return null;
        }
    }
}
