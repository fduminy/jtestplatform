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

import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.google.code.tempusfugit.concurrency.annotations.Repeating;
import org.jtestplatform.client.JUnitTestReporterTest.MockTestReporter;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.junitxmlreport.JUnitXMLReportWriter;
import org.jtestplatform.junitxmlreport.Testsuite;
import org.jtestplatform.junitxmlreport.Testsuites;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MultiThreadJUnitTestReporterTest {
    @Rule
    public final ConcurrentRule concurrentRule = new ConcurrentRule();
    @Rule
    public final RepeatingRule repeatingRule = new RepeatingRule();

    private static final int NB_THREADS = 100;
    private static final int NB_REPEATS = 10;

    private static final MockTestReporter[] testReporters = new MockTestReporter[Reporter.values().length];

    static {
        try {
            File tempFolder = File.createTempFile("test", "");
            for (int i = 0; i < testReporters.length; i++) {
                JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
                testReporters[i] = new MockTestReporter(tempFolder, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final AtomicInteger counter = new AtomicInteger();

    @Test
    @Concurrent(count = NB_THREADS)
    @Repeating(repetition = NB_REPEATS)
    public void testReport_multipleThreads_Framework() throws Exception {
        Reporter.FRAMEWORK.report(testReporters[Reporter.FRAMEWORK.ordinal()], counter.getAndIncrement());
    }

    @Test
    @Concurrent(count = NB_THREADS)
    @Repeating(repetition = NB_REPEATS)
    public void testReport_multipleThreads_Platform() throws Exception {
        Reporter.PLATFORM.report(testReporters[Reporter.PLATFORM.ordinal()], counter.getAndIncrement());
    }

    @Test
    @Concurrent(count = NB_THREADS)
    @Repeating(repetition = NB_REPEATS)
    public void testReport_multipleThreads_TestCase() throws Exception {
        Reporter.TESTCASE.report(testReporters[Reporter.TESTCASE.ordinal()], counter.getAndIncrement());
    }

    @AfterClass
    public static void afterTest() {
        for (int i = 0; i < testReporters.length; i++) {
            Testsuites suites = testReporters[i].getSuites();
            Reporter reporter = Reporter.values()[i];
            reporter.assertResult(suites, NB_THREADS * NB_REPEATS);
        }
    }

    private static enum Reporter {
        PLATFORM() {
            @Override
            void report(MockTestReporter reporter, int index) throws Exception {
                Platform platform = Utils.createPlatform("CPU" + index, 32, 2);
                TestResult testResult = new TestResult("framework", "testCase", true);
                reporter.report(platform, testResult);
            }

            @Override
            public void assertResult(Testsuites suites, int nbThreads) {
                assertThat(suites.getTestsuite()).as("number of suites").hasSize(nbThreads);
                for (Testsuite suite : suites.getTestsuite()) {
                    assertThat(suite.getTestcase()).hasSize(1);
                }
            }
        },
        FRAMEWORK() {
            @Override
            void report(MockTestReporter reporter, int index) throws Exception {
                Platform platform = Utils.createPlatform("CPU", 32, 2);
                TestResult testResult = new TestResult("framework" + index, "testCase", true);
                reporter.report(platform, testResult);
            }

            @Override
            public void assertResult(Testsuites suites, int nbThreads) {
                PLATFORM.assertResult(suites, nbThreads);
            }
        },
        TESTCASE() {
            @Override
            void report(MockTestReporter reporter, int index) throws Exception {
                Platform platform = Utils.createPlatform("CPU", 32, 2);
                TestResult testResult = new TestResult("framework", "testCase" + index, true);
                reporter.report(platform, testResult);
            }

            @Override
            public void assertResult(Testsuites suites, int nbThreads) {
                assertThat(suites.getTestsuite()).as("number of suites").hasSize(1);
                Testsuite suite = suites.getTestsuite().get(0);
                assertThat(suite.getTestcase()).hasSize(nbThreads);
            }
        };

        abstract void report(MockTestReporter reporter, int index) throws Exception;

        public abstract void assertResult(Testsuites suites, int nbThreads);
    }
}