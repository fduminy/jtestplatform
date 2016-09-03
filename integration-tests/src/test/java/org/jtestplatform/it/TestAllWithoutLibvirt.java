/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.it;

import com.google.code.tempusfugit.temporal.MovableClock;
import org.jtestplatform.server.*;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.client.TestDriverTest.copyStreamToFile;
import static org.jtestplatform.junitxmlreport.JUnitXMLReportWriterTest.*;
import static org.junit.Assert.fail;

/**
 * This class contains integration tests.
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class TestAllWithoutLibvirt {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() throws Exception {
        MauveTestFramework testFramework = (MauveTestFramework) TestFrameworkManager.getInstance()
                                                                                    .getTestFramework("mauve");
        MauveTestFrameworkTest.addTestsTo(testFramework);
        JUnitTestFramework testFramework2 = (JUnitTestFramework) TestFrameworkManager.getInstance()
                                                                                     .getTestFramework("junit");
        JUnitTestFrameworkTest.addTestsTo(testFramework2);
    }

/*
    //TODO find a way to get .iso files with a running TestServer
    @Test
    public void testAllInLibVirt() throws Exception {
        File reportDirectory = folder.getRoot();
        File cloudConfigFile = TestDriverTest.getCloudConfigFile(folder);

        TestDriver testDriver = new TestDriver();

        testDriver.runTests(cloudConfigFile, reportDirectory);
    }
*/

    @Test
    public void testAllInJVM_UDPTransport() throws Exception {
        File reportDirectory = folder.getRoot();
        File cloudConfigFile = copyStreamToFile(folder, "/cloud-it.xml");

        MovableClock clock = new MovableClock();
        UDPTransportChannelFactory channelFactory = UDPTransportChannelFactory.INSTANCE;
        InJVMTestDriver testDriver = new InJVMTestDriver<UDPTransportChannel>(clock, channelFactory);
        testDriver.runTests(cloudConfigFile, reportDirectory);

        verifyXMLReport(cloudConfigFile, "/AllInJVM_noNetworkTransport.xml");
    }

    @Test
    public void testAllInJVM_noNetworkTransport() throws Exception {
        File reportDirectory = folder.getRoot();
        File cloudConfigFile = copyStreamToFile(folder, "/cloud-it.xml");

        MovableClock clock = new MovableClock();
        NoNetworkTransportChannelFactory channelFactory = NoNetworkTransportChannelFactory.INSTANCE;
        InJVMTestDriver testDriver = new InJVMTestDriver<NoNetworkTransportChannel>(clock, channelFactory);
        testDriver.runTests(cloudConfigFile, reportDirectory);

        verifyXMLReport(cloudConfigFile, "/AllInJVM_noNetworkTransport.xml");
    }

    private void verifyXMLReport(File cloudConfigFile, String expectedContentResource)
        throws IOException, SAXException {
        File xmlReportFile = null;
        final File[] files = folder.getRoot().listFiles();
        if (files == null) {
            fail("No files in temporary directory");
        }

        for (File file : files) {
            if (!cloudConfigFile.equals(file)) {
                if (xmlReportFile != null) {
                    fail("Too many files in temporary directory");
                }
                xmlReportFile = file;
            }
        }

        assertThat(xmlReportFile).as("xml report file").isNotNull();
        if (xmlReportFile != null) {
            String actualContent = readFile(xmlReportFile.getAbsolutePath(), true);
            String expectedContent = readResource(expectedContentResource, true);
            compareXML(expectedContent, actualContent);
        }
    }
}
