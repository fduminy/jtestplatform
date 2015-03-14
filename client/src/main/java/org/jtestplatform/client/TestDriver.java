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

import com.google.code.tempusfugit.temporal.Clock;
import com.google.code.tempusfugit.temporal.RealClock;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class TestDriver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDriver.class);
    protected final Clock clock;

    public TestDriver() {
        this(RealClock.now());
    }

    public TestDriver(Clock clock) {
        this.clock = clock;
    }

    public final void runTests(File cloudConfigFile, File reportDirectory) throws Exception {
        BlockingQueue<Request> requests = createRequestQueue();
        RequestConsumer requestConsumer = createRequestConsumer(requests);
        RequestProducer requestProducer = createRequestProducer(requests);
        DomainManager domainManager = createDomainManager(cloudConfigFile);
        TestReporter reporter = createTestReporter(reportDirectory);

        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("Uncaught error in {}: {}", t, e);
            }
        };

        ExecutorService consumerExecutor = newExecutor("client-consumer-%d", handler);
        consumerExecutor.submit(new ConsumerTask(requestConsumer, domainManager, reporter));

        ExecutorService producerExecutor = newExecutor("client-producer-%d", handler);
        producerExecutor.submit(new ProducerTask(requestProducer, domainManager));

        shutdown(4, TimeUnit.MINUTES, consumerExecutor, producerExecutor);

        reporter.saveReport();
    }

    private ExecutorService newExecutor(String nameFormat, Thread.UncaughtExceptionHandler handler) {
        ThreadFactory consumerFactory = new ThreadFactoryBuilder().setNameFormat(nameFormat).setUncaughtExceptionHandler(handler).build();
        return Executors.newSingleThreadExecutor(consumerFactory);
    }

    TestReporter createTestReporter(File reportDirectory) {
        return new JUnitTestReporter(reportDirectory);
    }

    protected DomainManager createDomainManager(File cloudConfigFile) throws FileNotFoundException, DomainException {
        return new DefaultDomainManager(new FileReader(cloudConfigFile));
    }

    BlockingQueue<Request> createRequestQueue() {
        return new LinkedBlockingQueue<Request>();
    }

    RequestProducer createRequestProducer(BlockingQueue<Request> requests) {
        return new RequestProducer(requests);
    }

    protected RequestConsumer createRequestConsumer(BlockingQueue<Request> requests) {
        return new RequestConsumer(requests, clock);
    }

    private void shutdown(long timeout, TimeUnit unit, ExecutorService... executors) {
        for (ExecutorService executor : executors) {
            MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit);
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
