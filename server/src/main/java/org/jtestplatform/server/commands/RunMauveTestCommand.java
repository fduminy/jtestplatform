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
package org.jtestplatform.server.commands;



import gnu.testlet.runner.RunResult;
import gnu.testlet.runner.XMLReportWriter;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.jtestplatform.common.message.MauveReport;
import org.jtestplatform.common.message.RunMauveTest;
import org.jtestplatform.server.TestFailureException;
import org.jtestplatform.server.TestServerCommand;

public class RunMauveTestCommand implements TestServerCommand<RunMauveTest, MauveReport> {
    private static final Logger LOGGER = Logger.getLogger(RunMauveTestCommand.class);
    
    @Override
    public MauveReport execute(RunMauveTest message) {
        String test = message.getTest();
        LOGGER.debug("running test " + test);
        
        MauveTestRunner runner = MauveTestRunner.getInstance();
        try {
            RunResult runResult = runner.runTest(test);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.write("<?xml version=\"1.0\" encoding=\"UTF-16\"?>");
            new XMLReportWriter(true).write(runResult, pw);
            
            String result = sw.getBuffer().toString();
            LOGGER.debug("result=" + result);
            return new MauveReport(test, result);
        } catch (TestFailureException e) {
            LOGGER.error("error in execute", e);            
        } catch (Throwable t) {
            LOGGER.error("error in execute", t);            
        }
        return null;
    }
}
