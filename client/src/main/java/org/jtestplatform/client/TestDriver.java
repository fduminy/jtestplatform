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

import gnu.testlet.runner.ClassResult;
import gnu.testlet.runner.HTMLGenerator;
import gnu.testlet.runner.PackageResult;
import gnu.testlet.runner.RunResult;
import gnu.testlet.runner.TestResult;
import gnu.testlet.runner.XMLReportParser;
import gnu.testlet.runner.XMLReportWriter;
import gnu.testlet.runner.compare.ComparisonWriter;
import gnu.testlet.runner.compare.HTMLComparisonWriter;
import gnu.testlet.runner.compare.ReportComparator;
import gnu.testlet.runner.compare.RunComparison;
import gnu.testlet.runner.compare.TextComparisonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.cloud.domain.DomainManager;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.configuration.Configuration;

public class TestDriver {
    private static final Logger LOGGER = Logger.getLogger(TestDriver.class);
    
    public static void main(String[] args) {
        TestDriver testDriver = null;
        
        try {
            ConfigReader reader = new ConfigReader();
            Configuration config = reader.read();
            testDriver = new TestDriver(config);
            
            testDriver.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start", e);
        }
    }
    
    private final Configuration config;
    private final TestManager testManager;
    private final TestHandler testHandler;
    
    private TestDriver(Configuration config) throws Exception {
        this.config = config;        
        testManager = new DefaultTestManager(1, 1, 0L, TimeUnit.MILLISECONDS);
        testHandler = new MauveTestHandler(config);
    }
    
    public void start() throws Exception {
        DomainManager domainManager = null;
        try {
            File cloudConfig = new File(config.getWorkDir(), "cloud.xml");
            FileReader configReader = new FileReader(cloudConfig);
            domainManager = new DomainManager(configReader);
            domainManager.start();

            //TODO we should have our own list of platforms, not necessarily the domain manager's ones
            java.util.List<Platform> platforms = domainManager.getPlatforms();
            
            List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(platforms.size()); 
            for (Platform platform : platforms) {
                PlatformTask platformResults = new PlatformTask(platform, domainManager);
                tasks.add(platformResults);
            }

            // launch all tasks in parallel and wait for completion
            ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
            executor.invokeAll(tasks);
            executor.shutdown();
        } finally {
            if (domainManager != null) {
                domainManager.stop();
            }
            
            testManager.shutdown();
        }
    }

    private void compareRuns(Run latestRun, Run newRun, RunResult newRunResult) throws IOException {
        if ((latestRun != null) && latestRun.getReportXml().exists()) {
            // there was a previous run, let do the comparison !
            
            RunResult latestRunResult = new XMLReportParser().parse(latestRun.getReportXml());
            
            ReportComparator comparator = new ReportComparator(latestRunResult, newRunResult);
            RunComparison comparison = comparator.compare();
            
            // write comparison in html format
            ComparisonWriter writer = new HTMLComparisonWriter();
            writer.write(comparison, new File(newRun.getReportXml().getParentFile(), "comparison.html"));
            
            // write comparison in text format
            writer = new TextComparisonWriter();
            writer.write(comparison, new File(newRun.getReportXml().getParentFile(), "comparison.txt"));
        }
    }
    
    private void writeReports(RunResult result, File reportXml) throws IOException {
        XMLReportWriter rw = new XMLReportWriter(false);
        rw.write(result, reportXml);
        
        HTMLGenerator.createReport(result, reportXml.getParentFile());
    }
    
    private RunResult mergeResults(String timestamp, TestHandler testHandler,
            List<Future<Message>> replies) throws Exception {
        RunResult result = new RunResult(timestamp);
        boolean firstTest = true;
        for (Future<Message> reply : replies) {
            Result runnerResult = testHandler.parseResult(reply.get());
            RunResult delta = runnerResult.getRunResult();
            mergeResults(result, delta);

            if (firstTest && (delta != null)) {
                for (String name : delta.getSystemPropertyNames()) {
                    result.setSystemProperty(name, delta.getSystemProperty(name));
                }

                firstTest = false;
            }
        }

        return result;
    }
    
    private void mergeResults(RunResult target, RunResult source) {
        for (Iterator<?> itSourcePackage = source.getPackageIterator(); itSourcePackage.hasNext(); ) {
            PackageResult sourcePackage = (PackageResult) itSourcePackage.next();
            
            PackageResult targetPackage = target.getPackageResult(sourcePackage.getName());
            if (targetPackage == null) {
                target.add(sourcePackage);
            } else {            
                for (Iterator<?> itSourceClass = sourcePackage.getClassIterator(); itSourceClass.hasNext(); ) {
                    ClassResult sourceClass = (ClassResult) itSourceClass.next();
                    
                    ClassResult targetClass = targetPackage.getClassResult(sourceClass.getName());
                    if (targetClass == null) {
                        targetPackage.add(sourceClass);
                    } else {                                    
                        for (Iterator<?> itSourceTest = sourceClass.getTestIterator(); itSourceTest.hasNext(); ) {
                            TestResult sourceTest = (TestResult) itSourceTest.next();
                            
                            boolean hasTest = false;
                            for (Iterator<?> it = targetClass.getTestIterator(); it.hasNext(); ) {
                                TestResult tr = (TestResult) it.next();
                                if (tr.getName().equals(sourceTest.getName())) {
                                    hasTest = true;
                                    break;
                                }
                            }
                            
                            if (!hasTest) {
                                targetClass.add(sourceTest);
                            }
                            
                        }
                    }
                }
            }
        }
    }
    
    private class PlatformTask implements Callable<Object> {
        private final Platform platform;
        private final TransportProvider transportProvider; 
        
        public PlatformTask(Platform platform, TransportProvider transportProvider) {
            this.platform = platform;
            this.transportProvider = transportProvider;
        }

        @Override
        public Object call() throws Exception {
            StringBuilder platformStr = new StringBuilder(); 
            Map<String, String> platformProperties = getPlatformProperties(platform, platformStr);                    
            File workDir = new File(config.getWorkDir(), platformStr.toString());
            Run latestRun = Run.getLatest(workDir);
            Run newRun = Run.create(workDir);
            
            List<Future<Message>> replies = runTests(testHandler);
            RunResult runResult = mergeResults(newRun.getTimestampString(), testHandler, replies);
            
            for (String property : platformProperties.keySet()) {
                runResult.setSystemProperty(property, platformProperties.get(property));
            }
            
            writeReports(runResult, newRun.getReportXml());
            
            compareRuns(latestRun, newRun, runResult);

            return null;
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
        
        private List<Future<Message>> runTests(TestHandler testHandler) throws Exception {
            List<Future<Message>> results = new ArrayList<Future<Message>>();
            for (String test : testHandler.readTests(null)) {
                Message message = testHandler.createRequest(test); 
                Future<Message> result = testManager.runTest(message, transportProvider, platform);
                results.add(result);
            }
            return results;
        }
    }    
}
