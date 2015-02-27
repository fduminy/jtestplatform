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
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MultiThreadJUnitTestReporterTest {
    private static final int NB_THREADS = 100;
    private static final int NB_REPEATS = 10;

    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public final ConcurrentRule concurrentRule = new ConcurrentRule();
    @Rule
    public final RepeatingRule repeatingRule = new RepeatingRule();

    private static MockTestReporter[] testReporters;

    private static AtomicInteger counter;

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

    @BeforeClass
    public static void beforeClass() throws IOException {
        counter = new AtomicInteger();
        testReporters = new MockTestReporter[Reporter.values().length];
        for (int i = 0; i < testReporters.length; i++) {
            JUnitXMLReportWriter writer = mock(JUnitXMLReportWriter.class);
            testReporters[i] = new MockTestReporter(folder.newFolder("reportFolder" + i), writer);
        }
    }

    @AfterClass
    public static void afterClass() {
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
                reporter.report(platform, testResult, millis(1));
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
                reporter.report(platform, testResult, millis(1));
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
                reporter.report(platform, testResult, millis(1));
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