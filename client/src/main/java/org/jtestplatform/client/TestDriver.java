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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sourceforge.nanoxml.XMLParseException;

import org.apache.log4j.Logger;
import org.jtestplatform.client.domain.DomainManager;
import org.jtestplatform.client.utils.TestListRW;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.transport.TransportProvider;

public class TestDriver {
    private static final Logger LOGGER = Logger.getLogger(TestDriver.class);
    
    public static void main(String[] args) {
        TestDriver testDriver = null;
        
        try {
            ConfigReader reader = new ConfigReader();
            Config config = reader.read();
            testDriver = new TestDriver(config);
            
            testDriver.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start", e);
        }
    }
    
    private final Config config;
    private final TestListRW testListRW;
    private final TestManager testManager;
    private final DomainManager domainManager;
    private final String vmType;
    
    private TestDriver(Config config) throws Exception {
        this.config = config;
        testListRW = new TestListRW(config);
        domainManager = new DomainManager(config);
        TransportProvider transportProvider = domainManager;
        
        testManager = new DefaultTestManager(1, 1, 0L, TimeUnit.MILLISECONDS, transportProvider);
        vmType = config.getVMConfigs().get(0).getFactory().getType(); //TODO there might be multiple VMs (and so multiple types)
    }
    
    public void start() throws Exception {
        domainManager.start();
 
        try {
            Run latestRun = Run.getLatest(config);
            Run newRun = Run.create(config);
            
            TestHandler testHandler = new MauveTestHandler();
            
            RunResult runResult = runTests(null, newRun.getTimestampString(), testHandler);
            runResult.setSystemProperty("jtestserver.domain.type", vmType);
            
            writeReports(runResult, newRun.getReportXml());
            
            compareRuns(latestRun, newRun, runResult);
        } finally {        
            domainManager.stop();
            testManager.shutdown();
        }
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
    
    private List<Future<Message>> runTestsImpl(List<String> list, TestHandler testHandler) throws Exception {
        List<Message> messages = new ArrayList<Message>(list.size());
        for (String test : list) {
            messages.add(testHandler.createRequest(test));
        }
        
        return testManager.runTests(messages);
    }

    private List<String> readTests(File listFile) throws Exception {
        List<String> list;
        if ((listFile != null) && listFile.exists()) {
            list = testListRW.readList(listFile);
        } else {
            list = testListRW.readCompleteList();
        }
        return list;
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
    
    private RunResult runTests(File listFile, String timestamp, TestHandler testHandler)
        throws Exception {
        List<String> list = readTests(listFile);
        List<Future<Message>> replies = runTestsImpl(list, testHandler);
        return mergeResults(timestamp, testHandler, replies);
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
