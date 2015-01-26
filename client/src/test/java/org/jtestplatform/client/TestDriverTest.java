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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jtestplatform.cloud.TransportProvider;
import org.jtestplatform.cloud.domain.DefaultDomainManager;
import org.jtestplatform.cloud.domain.DomainException;
import org.jtestplatform.cloud.domain.DomainManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TestDriverTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCreateDomainManager() throws Exception {
        File cloudConfigFile = getCloudConfigFile();
        TestDriver testDriver = new TestDriver();

        DomainManager domainManager = testDriver.createDomainManager(cloudConfigFile);

        assertThat(domainManager).isInstanceOf(DefaultDomainManager.class);
    }

    @Test
    public void testCreateRequestQueue() throws Exception {
        TestDriver testDriver = new TestDriver();

        BlockingQueue<Request> requestQueue = testDriver.createRequestQueue();

        assertThat(requestQueue).isInstanceOf(BlockingQueue.class);
    }

    @Test
    public void testCreateRequestProducer() throws Exception {
        BlockingQueue<Request> requests = mock(BlockingQueue.class);
        TestDriver testDriver = new TestDriver();

        RequestProducer requestProducer = testDriver.createRequestProducer(requests);

        assertThat(requestProducer).isExactlyInstanceOf(RequestProducer.class);
    }

    @Test
    public void testCreateRequestConsumer() throws Exception {
        BlockingQueue<Request> requests = mock(BlockingQueue.class);
        TestDriver testDriver = new TestDriver();

        RequestConsumer requestConsumer = testDriver.createRequestConsumer(requests);

        assertThat(requestConsumer).isExactlyInstanceOf(RequestConsumer.class);
    }

    @Test(timeout = 60000)
    public void testRunTests() throws Exception {
        // preparation
        File reportDirectory = folder.getRoot();
        File cloudConfigFile = getCloudConfigFile();
        final BlockingQueue<Request> requests = mock(BlockingQueue.class);
        final RequestProducer requestProducer = mock(RequestProducer.class);
        final MutableObject<Thread> requestProducerThread = new MutableObject<Thread>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                requestProducerThread.setValue(Thread.currentThread());
                return null;
            }
        }).when(requestProducer).produce(any(DomainManager.class));
        final RequestConsumer requestConsumer = mock(RequestConsumer.class);
        final MutableObject<Thread> requestConsumerThread = new MutableObject<Thread>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                requestConsumerThread.setValue(Thread.currentThread());
                return null;
            }
        }).when(requestConsumer).consume(any(TransportProvider.class), any(TestReporter.class));
        MockTestDriver testDriver = new MockTestDriver(requests, requestProducer, requestConsumer);

        // test
        testDriver.runTests(cloudConfigFile, reportDirectory);

        // verifications
        ArgumentCaptor<DomainManager> domainManagerCaptor = ArgumentCaptor.forClass(DomainManager.class);
        ArgumentCaptor<DomainManager> domainManagerCaptor2 = ArgumentCaptor.forClass(DomainManager.class);
        InOrder inOrder = inOrder(requests, requestProducer, requestConsumer);
        inOrder.verify(requestConsumer, times(1)).consume(domainManagerCaptor2.capture(), any(TestReporter.class));
        inOrder.verify(requestProducer, times(1)).produce(domainManagerCaptor.capture());
        inOrder.verifyNoMoreInteractions();
        assertThat(testDriver.getActualCloudConfigFile()).as("cloudConfigFile").isEqualTo(cloudConfigFile);
        Thread mainThread = Thread.currentThread();
        assertThat(requestConsumerThread.getValue()).as("requestConsumerThread").isNotNull().isNotEqualTo(mainThread);
        assertThat(requestProducerThread.getValue()).as("requestProducerThread").isNotNull().isNotEqualTo(mainThread).isNotEqualTo(requestConsumerThread);
    }

    public static File copyStreamToFile(TemporaryFolder folder, String name) throws IOException {
        File outputFile = folder.newFile(name);
        final InputStream inputStream = TestDriverTest.class.getResourceAsStream(name);
        FileUtils.copyInputStreamToFile(inputStream, outputFile);
        return outputFile;
    }

    private File getCloudConfigFile() throws IOException {
        return copyStreamToFile(folder, "/cloud.xml");
    }

    private static class MockTestDriver extends TestDriver {
        private final BlockingQueue<Request> requests;
        private final RequestProducer requestProducer;
        private final RequestConsumer requestConsumer;
        private File actualCloudConfigFile;

        public MockTestDriver(BlockingQueue<Request> requests, RequestProducer requestProducer, RequestConsumer requestConsumer) {
            this.requests = requests;
            this.requestProducer = requestProducer;
            this.requestConsumer = requestConsumer;
        }

        @Override
        BlockingQueue<Request> createRequestQueue() {
            return requests;
        }

        @Override
        RequestProducer createRequestProducer(BlockingQueue<Request> requests) {
            return requestProducer;
        }

        @Override
        RequestConsumer createRequestConsumer(BlockingQueue<Request> requests) {
            return requestConsumer;
        }

        @Override
        protected DomainManager createDomainManager(File cloudConfigFile) throws FileNotFoundException, DomainException {
            this.actualCloudConfigFile = cloudConfigFile;
            return super.createDomainManager(cloudConfigFile);
        }

        public File getActualCloudConfigFile() {
            return actualCloudConfigFile;
        }
    }
}