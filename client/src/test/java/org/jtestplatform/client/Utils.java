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

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.MovableClock;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.*;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class Utils {
    static final Platform PLATFORM1 = createPlatform("Athlon", 32, 2);
    static final Platform PLATFORM2 = createPlatform("Pentium", 32, 1);
    static final List<Platform> PLATFORMS = Collections.unmodifiableList(Arrays.asList(PLATFORM1, PLATFORM2));

    static final String FRAMEWORK1 = "framework1";
    static final String FRAMEWORK2 = "framework2";
    static final Set<String> FRAMEWORKS = new HashSet<String>(Arrays.asList(FRAMEWORK1, FRAMEWORK2));
    static final String TEST1 = "test1";
    static final String TEST2 = "test2";
    static final String TEST3 = "test3";
    static final String TEST4 = "test4";
    static final String TEST5 = "test5";
    static final Set<String> FRAMEWORK1_TESTS = new HashSet<String>(Arrays.asList(TEST1, TEST2));
    static final Set<String> FRAMEWORK2_TESTS = new HashSet<String>(Arrays.asList(TEST3, TEST4, TEST5));

    static {
        PLATFORM1.setCdrom("myCDROM");
        PLATFORM1.setMemory(123L);

        PLATFORM2.setCdrom("myCDROM2");
        PLATFORM2.setMemory(456L);
    }

    static Platform createPlatform(String cpuName, int wordSize, int nbCores) {
        Platform platform = new Platform();
        platform.setCpu(cpuName);
        platform.setWordSize(wordSize);
        platform.setNbCores(nbCores);
        return platform;
    }

    static class MockTransportHelper extends TransportHelper {
        private static final Logger LOGGER = LoggerFactory.getLogger(MockTransportHelper.class);
        private TransportException sendError;
        private TransportException receiveError;

        private static enum STATE {
            INIT,
            GET_FRAMEWORKS,
            GET_TESTS,
            RUN_TEST
        }

        private STATE state = STATE.INIT;
        private String framework;
        private String test;
        private final List<Transport> expectedTransports;
        private final List<TestResult> testResults = new ArrayList<TestResult>();

        private MovableClock clock;
        private Duration[] testDurations;
        private int testNum;

        MockTransportHelper(Transport... expectedTransports) {
            this.expectedTransports = Arrays.asList(expectedTransports);
        }

        public void setTestDurations(MovableClock clock, Duration... durations) {
            this.clock = clock;
            this.testDurations = durations;
            testNum = 0;
        }

        public void setSendError(TransportException sendError) {
            this.sendError = sendError;
        }

        public void setReceiveError(TransportException receiveError) {
            this.receiveError = receiveError;
        }

        @Override
        protected void sendImpl(Transport transport, Message message) throws TransportException {
            LOGGER.info("BEGIN send(transport, {}): state={}", message.getClass().getSimpleName(), state);
            if (state != STATE.INIT) {
                throw new IllegalStateException("illegal state: " + state);
            }
            if (!expectedTransports.contains(transport)) {
                throw new IllegalArgumentException("illegal transport: " + transport);
            }

            if (sendError != null) {
                throw sendError;
            }

            if (message instanceof GetTestFrameworks) {
                state = STATE.GET_FRAMEWORKS;
                framework = null;
            } else if (message instanceof GetFrameworkTests) {
                framework = ((GetFrameworkTests) message).getFramework();
                state = STATE.GET_TESTS;
            } else if (message instanceof RunTest) {
                framework = ((RunTest) message).getFramework();
                test = ((RunTest) message).getTest();
                state = STATE.RUN_TEST;

                if (testNum < testDurations.length) {
                    clock.incrementBy(testDurations[testNum]);
                    testNum++;
                }
            } else {
                throw new IllegalStateException("illegal message: " + message);
            }
            LOGGER.info("END send(...): state={}", state);
        }

        @Override
        protected Message receiveImpl(Transport transport) throws TransportException {
            LOGGER.info("BEGIN receive(transport): state={}", state);

            if (receiveError != null) {
                throw receiveError;
            }

            Message result;
            switch (state) {
                case GET_FRAMEWORKS:
                    state = STATE.INIT;
                    result = new TestFrameworks(FRAMEWORKS);
                    break;
                case GET_TESTS:
                    state = STATE.INIT;
                    if (FRAMEWORK1.equals(framework)) {
                        result = new FrameworkTests(FRAMEWORK1_TESTS);
                    } else if (FRAMEWORK2.equals(framework)) {
                        result = new FrameworkTests(FRAMEWORK2_TESTS);
                    } else {
                        throw new IllegalStateException("unknown framework: " + framework);
                    }
                    break;
                case RUN_TEST:
                    state = STATE.INIT;
                    result = new TestResult(framework, test, true);
                    testResults.add((TestResult) result);
                    break;
                case INIT:
                default:
                    throw new IllegalStateException("illegal state: " + state);
            }
            LOGGER.info("END receive(transport): state={} RETURN {}({})", state, result.getClass().getSimpleName(),
                    toString(result));
            return result;
        }

        public List<TestResult> getTestResults() {
            return testResults;
        }

        private Object toString(Message result) {
            if (result instanceof TestFrameworks) {
                return ((TestFrameworks) result).getFrameworks();
            } else if (result instanceof TestResult) {
                final TestResult testResult = (TestResult) result;
                return testResult.getFramework() + ',' + testResult.getTest() + ',' + testResult.isSuccess();
            } else {
                return ((FrameworkTests) result).getTests();
            }
        }

        @Override
        public void stop(Transport transport) throws IOException {
            throw new IOException("stop() shouldn't be called");
        }
    }
}
