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
/**
 * 
 */
package org.jtestplatform.client;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.configuration.Platform;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.message.TestResult;
import org.jtestplatform.common.transport.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
@RunWith(Parameterized.class)
public class TestManagerTest {
    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
            {0, 1, 0L, 1},
        });
    }

    private final int corePoolSize;
    private final int maximumPoolSize;
    private final long keepAliveTime;
    private final TimeUnit unit = TimeUnit.MILLISECONDS; 
    private final int nbMessages;

    private TestManager testManager;
    private TransportProvider transportProvider;

    public TestManagerTest(Integer corePoolSize, Integer maximumPoolSize,
            Long keepAliveTime, Integer nbMessages) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.nbMessages = nbMessages;
    }

    @Before
    public void setUp() throws ConfigurationException {
        new ConfigReader().read(); // will initialize log4j

        transportProvider = mock(TransportProvider.class);

        testManager = new DefaultTestManager(corePoolSize,
                maximumPoolSize, keepAliveTime, unit);
    }

    @After
    public void tearDown() {
        testManager.shutdown();
    }

    @Test
    public void testRunTests() throws Exception {
        Transport transport = mock(Transport.class);
        when(transportProvider.get(any(Platform.class))).thenReturn(transport);
        
        //TODO do tests with multiple platforms and check they are actually run on the proper platform
        Platform platform = new Platform();

        for (int i = 0; i < nbMessages; i++) {
            Message message = mock(Message.class);
//            doReturn(null).when(message).sendWith((Transport) any());
//            doReturn(null).when(message).sendWith(any());

            Future<TestResult> result = testManager.runTest(message, transportProvider, platform);
            assertNotNull(result);
        }
    }
}
