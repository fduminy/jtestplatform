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
/**
 * 
 */
package org.jtestplatform.client;

import gnu.testlet.runner.RunResult;
import gnu.testlet.runner.XMLReportParser;

import java.io.IOException;
import java.io.StringReader;

import net.sourceforge.nanoxml.XMLParseException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jtestplatform.common.message.MauveReport;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.message.RunMauveTest;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class MauveTestHandler implements TestHandler {
    private static final Logger LOGGER = Logger.getLogger(DefaultTestManager.class);

    /* (non-Javadoc)
     * @see org.jtestplatform.client.TestHandler#createRequest(java.lang.String)
     */
    @Override
    public Message createRequest(String test) {
        return new RunMauveTest(test);
    }

    /* (non-Javadoc)
     * @see org.jtestplatform.client.TestHandler#parseResult(org.jtestplatform.common.message.Message)
     */
    @Override
    public Result parseResult(Message reply) {
        Result result = null;
        
        if (reply instanceof MauveReport) {
            MauveReport r = (MauveReport) reply;
            RunResult runResult = parseMauveReport(r.getReport());
            if (runResult != null) {
                result = new Result(r.getTest(), runResult);
            }
        }
        
        return result;
    }

    protected RunResult parseMauveReport(String report) {
        XMLReportParser parser = new XMLReportParser();
        StringReader sr = new StringReader(report);
        RunResult result = null;
        
        try {
            LOGGER.log(Level.INFO, "xml report: " + report);
            
            result = parser.parse(sr);
        } catch (XMLParseException e) {
            LOGGER.error("invalid XML answer", e);
        } catch (IOException e) {
            LOGGER.error("I/O error", e);
        }
        return result;
    }    
}
