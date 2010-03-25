/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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
 * -
 */
package org.jtestplatform.client;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.FrameworkTests;
import org.jtestplatform.common.message.GetFrameworkTests;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;

public class DefaultTestManager implements TestManager {
    private static final Logger LOGGER = Logger.getLogger(DefaultTestManager.class);
    
    private final ExecutorService executor;
    private final TransportHelper transportHelper;
    private final ThreadGroup threadGroup;
    
    public DefaultTestManager(int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit) {
        transportHelper = new TransportHelper();
        
        threadGroup = new ThreadGroup("DefaultTestManager threads");
        
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new TestManagerThread(r);
            }
        };
        
        //executor = Executors.newSingleThreadExecutor();
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, unit,
                new LinkedBlockingQueue<Runnable>(), factory);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Future<TestResult> runTest(Message message,
            TransportProvider transportProvider, Platform platform) 
            throws Exception {
        return executor.submit(new TestCallable(message, transportProvider, platform));
    }

    /**
     * {@inheritDoc}
     * @throws TransportException 
     */
    @Override
    public List<String> getFrameworkTests(String testFramework, TransportProvider transportProvider, Platform platform) throws TransportException {
        //TODO we assume here that the available tests/test frameworks are all the same on each server. check it ?
        Transport transport = transportProvider.get(platform);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("call: transport=" + transport);
        }
        transportHelper.send(transport, new GetFrameworkTests());
        return ((FrameworkTests) transportHelper.receive(transport)).getTests();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOGGER.error("executor didn't terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
    
    private class TestCallable implements Callable<TestResult> {
        private final Message message;
        private final TransportProvider transportProvider;
        private final Platform platform;
        
        public TestCallable(Message message, TransportProvider transportProvider, Platform platform) {
            this.message = message;
            this.transportProvider = transportProvider;
            this.platform = platform;
        }
        
        @Override
        public TestResult call() throws Exception {
            Transport transport = transportProvider.get(platform);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("call: transport=" + transport);
            }
            transportHelper.send(transport, message);
            return (TestResult) transportHelper.receive(transport);
        }
    }
    
    private class TestManagerThread extends Thread {
        private TestManagerThread(Runnable r) {
            super(threadGroup, r);            
        }
    }
}
