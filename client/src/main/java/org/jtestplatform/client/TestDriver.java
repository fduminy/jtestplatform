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

import org.jtestplatform.client.report.PlatformReport;
import org.jtestplatform.client.report.RunReport;
import org.jtestplatform.client.report.TestFrameworkReport;
import org.jtestplatform.client.report.TestReport;
import org.jtestplatform.client.report.io.dom4j.ReportDom4jWriter;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainManager;
import org.jtestplatform.common.message.GetTestFrameworks;
import org.jtestplatform.common.message.RunTest;
import org.jtestplatform.common.message.TestFrameworks;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportHelper;
import org.jtestplatform.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public final class TestDriver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDriver.class);

    public static void main(String[] args) {
        TestDriver testDriver = null;

        try {
            ConfigReader reader = new ConfigReader();
            Configuration config = reader.read();
            testDriver = new TestDriver(config);

            testDriver.run();
        } catch (Exception e) {
            LOGGER.error("Failed to start", e);
        }
    }

    private final Configuration config;
    private final TestManager testManager;

    private TestDriver(Configuration config) throws Exception {
        this.config = config;
        testManager = new DefaultTestManager(1, 1, 0L, TimeUnit.MILLISECONDS);
    }

    public void run() throws Exception {
        RunReport runReport = runTests();
        writeReport(runReport);
    }

    protected DomainManager createDomainManager() throws Exception {
        File cloudConfig = new File(config.getWorkDir(), "cloud.xml");
        FileReader configReader = new FileReader(cloudConfig);
        return new DomainManager(configReader);
    }

    protected RunReport runTests() throws Exception {
        DomainManager domainManager = null;
        try {
            domainManager = createDomainManager();
            domainManager.start();

            TransportProvider transportProvider = domainManager;
            TransportHelper transportHelper = new TransportHelper();

            //TODO we should have our own list of platforms, not necessarily the domain manager's ones
            java.util.List<Platform> platforms = domainManager.getPlatforms();
            RunReport runReport = new RunReport();

            List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(platforms.size());
            for (Platform platform : platforms) {
                Transport transport = transportProvider.get(platform);

                //TODO we assume that all frameworks are available on each server. check it ?
                transportHelper.send(transport, new GetTestFrameworks());
                TestFrameworks testFrameworks = (TestFrameworks) transportHelper.receive(transport);

                for (String testFramework : testFrameworks.getFrameworks()) {
                    PlatformTask platformResults = new PlatformTask(testFramework, platform, domainManager, runReport);
                    tasks.add(platformResults);
                }
            }

            // launch all tasks in parallel and wait for completion
            ExecutorService executor = Executors.newFixedThreadPool(platforms.size());
            executor.invokeAll(tasks);
            executor.shutdown();

            return runReport;
        } finally {
            if (domainManager != null) {
                domainManager.stop();
            }

            testManager.shutdown();
        }
    }

    protected void writeReport(RunReport runReport) throws Exception {
        // write the report
        File workDir = new File(config.getWorkDir());
        Run newRun = Run.create(workDir);
        File f = new File(workDir, newRun.getTimestampString());
        new ReportDom4jWriter().write(new FileWriter(f), runReport);
    }

    private class PlatformTask implements Callable<Object> {
        private final String testFramework;
        private final Platform platform;
        private final TransportProvider transportProvider;
        private final RunReport runReport;

        public PlatformTask(String testFramework, Platform platform, TransportProvider transportProvider, RunReport runReport) {
            this.testFramework = testFramework;
            this.platform = platform;
            this.transportProvider = transportProvider;
            this.runReport = runReport;
        }

        @Override
        public Object call() throws Exception {
            TestFrameworkReport testFrameworkReport = new TestFrameworkReport();
            testFrameworkReport.setName(testFramework);

            List<Future<TestResult>> replies = runTests();
            for (Future<TestResult> reply : replies) {
                TestResult testResult = reply.get();

                TestReport testReport = new TestReport();
                testReport.setName(testResult.getTest());
                testReport.setSuccess(testResult.isSuccess());
                testFrameworkReport.addTestReport(testReport);
            }

            synchronized (runReport) {
                for (PlatformReport platformReport : runReport.getPlatformReports()) {
                    if (samePlatform(platform, platformReport.getPlatform())) {
                        platformReport.addTestFrameworkReport(testFrameworkReport);
                    }
                }
            }

            return null;
        }

        private boolean samePlatform(Platform platform, org.jtestplatform.client.report.Platform reportPlatform) {
            boolean result = true;

            result = result && ((platform.getCdrom() == null) ? (reportPlatform.getCdrom() == null) : platform.getCdrom().equals(reportPlatform.getCdrom()));
            result = result && ((platform.getCpu() == null) ? (reportPlatform.getCpu() == null) : platform.getCpu().equals(reportPlatform.getCpu()));
            result = result && platform.getNbCores() == reportPlatform.getNbCores();
            result = result && platform.getWordSize() == reportPlatform.getWordSize();
            result = result && platform.getMemory() == reportPlatform.getMemory();

            return result;
        }

        private Map<String, String> getPlatformProperties(Platform platform, StringBuilder asString) {
            Map<String, String> result = new HashMap<String, String>();
            String prefix = "jtestplatform.platform.";

            result.put(prefix + "cpu", platform.getCpu());
            asString.append(platform.getCpu());

            asString.append('_').append(platform.getWordSize()).append("bits");
            result.put(prefix + "wordSize", Integer.toString(platform.getWordSize()));

            asString.append('x').append(platform.getNbCores());
            result.put(prefix + "nbCores", Integer.toString(platform.getNbCores()));

            return result;
        }

        private List<Future<TestResult>> runTests() throws Exception {
            List<Future<TestResult>> results = new ArrayList<Future<TestResult>>();
            for (String test : testManager.getFrameworkTests(testFramework, transportProvider, platform)) {
                RunTest message = new RunTest(testFramework, test);
                Future<TestResult> result = testManager.runTest(message, transportProvider, platform);
                results.add(result);
            }
            return results;
        }
    }
}
