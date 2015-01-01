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

import org.jtestplatform.client.configuration.Configuration;
import org.jtestplatform.client.configuration.io.dom4j.ConfigurationDom4jReader;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainManager;
import org.jtestplatform.common.message.GetTestFrameworks;
import org.jtestplatform.common.message.RunTest;
import org.jtestplatform.common.message.TestFrameworks;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class TestDriver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDriver.class);

    TestDriver() throws Exception {
    }

    public void runTests(File configFile, File cloudConfigFile, File reportDirectory) throws Exception {
        DomainManager domainManager = null;
        TestManager testManager = null;
        try {
            testManager = new TestManager(1, 1, 0L, TimeUnit.MILLISECONDS);
            //TestReporter testReporter = new JUnitTestReporter(reportDirectory);
            TestReporter testReporter = new TestReporter() {

                @Override
                public void report(Platform platform, TestResult testResult) throws IOException {
                    //TODO implement this.
                }
            };

            LOGGER.info("Reading config file {}", configFile);
            ConfigurationDom4jReader reader = new ConfigurationDom4jReader();
            Configuration config = reader.read(new FileReader(configFile));

            LOGGER.info("Reading cloud config file {}", cloudConfigFile);
            FileReader configReader = new FileReader(cloudConfigFile);
            domainManager = new DomainManager(configReader);

            LOGGER.info("Starting cloud");
            domainManager.start();

            TransportProvider transportProvider = domainManager;
            TransportHelper transportHelper = new TransportHelper();

            java.util.List<Platform> platforms = domainManager.getPlatforms();

            List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(platforms.size());
            for (Platform platform : platforms) {
                Transport transport = transportProvider.get(platform);

                //TODO we assume that all frameworks are available on each server. check it ?
                transportHelper.send(transport, new GetTestFrameworks());
                TestFrameworks testFrameworks = (TestFrameworks) transportHelper.receive(transport);

                for (String testFramework : testFrameworks.getFrameworks()) {
                    PlatformTask platformResults = new PlatformTask(testFramework, platform, domainManager, testManager, testReporter);
                    tasks.add(platformResults);
                }
            }

            // launch all tasks in parallel and wait for completion
            ExecutorService executor = Executors.newFixedThreadPool(platforms.size());
            executor.invokeAll(tasks);
            executor.shutdown();
        } finally {
            if (domainManager != null) {
                domainManager.stop();
            }

            testManager.shutdown();
        }
    }

    private class PlatformTask implements Callable<Object> {
        private final String testFramework;
        private final Platform platform;
        private final TransportProvider transportProvider;
        private final TestReporter reporter;
        private final TestManager testManager;

        public PlatformTask(String testFramework, Platform platform, TransportProvider transportProvider, TestManager testManager, TestReporter reporter) {
            this.testFramework = testFramework;
            this.platform = platform;
            this.transportProvider = transportProvider;
            this.reporter = reporter;
            this.testManager = testManager;
        }

        @Override
        public Object call() throws Exception {
            for (String test : testManager.getFrameworkTests(testFramework, transportProvider, platform)) {
                RunTest message = new RunTest(testFramework, test);
                TestResult testResult = testManager.runTest(message, transportProvider, platform).get();
                reporter.report(platform, testResult);
            }
            return null;
        }

        private String getPlatformKey(Platform platform) {
            StringBuilder platformKey = new StringBuilder();
            platformKey.append(platform.getCpu());
            platformKey.append('_').append(platform.getWordSize()).append("bits");
            platformKey.append('x').append(platform.getNbCores());
            return platformKey.toString();
        }
    }
}
