/**
 * JTestPlatform is a client/server framework for testing any JVM implementation.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sourceforge.nanoxml.XMLParseException;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.DomainManager;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.transport.TransportProvider;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.Platform;

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
    
    private TestDriver(Configuration config) throws Exception {
        this.config = config;        
        testManager = new DefaultTestManager(1, 1, 0L, TimeUnit.MILLISECONDS);
    }
    
    public void start() throws Exception {
        try {            
            for (Platform platform : config.getPlatforms()) {
                //TODO run domain managers in parallel 
                DomainManager domainManager = new DomainManager(config, platform);
                domainManager.start();
         
                try {
                    File workDir = new File(config.getWorkDir(), toString(platform));
                    Run latestRun = Run.getLatest(workDir);
                    Run newRun = Run.create(workDir);
                    
                    TestHandler testHandler = new MauveTestHandler(config);
        
                    List<Future<Message>> replies = runTests(testHandler, domainManager);
                    RunResult runResult = mergeResults(newRun.getTimestampString(), testHandler, replies);
                    //runResult.setSystemProperty("jtestplatform.domain.type", vmType); //TODO add system properties (virtualization type, ...)
                    
                    writeReports(runResult, newRun.getReportXml());
                    
                    compareRuns(latestRun, newRun, runResult);
                } finally {        
                    domainManager.stop();
                }
            }
        } finally {        
            testManager.shutdown();
        }
    }
    
    private String toString(Platform platform) {
        return platform.getCpu() + '_' + platform.getWordSize() + "bits_x" + platform.getNbCores(); 
    }
    
    private void compareRuns(Run latestRun, Run newRun, RunResult newRunResult) throws XMLParseException, IOException {
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
    
    private List<Future<Message>> runTests(TestHandler testHandler, TransportProvider transportProvider) throws Exception {
        final List<String> list = testHandler.readTests(null);

        List<Message> messages = new ArrayList<Message>(list.size());
        for (String test : list) {
            messages.add(testHandler.createRequest(test));
        }
        
        return testManager.runTests(messages, transportProvider);
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
    
}
