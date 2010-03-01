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

import gnu.testlet.runner.CheckResult;
import gnu.testlet.runner.Mauve;
import gnu.testlet.runner.RunResult;

import java.util.Locale;

import org.jtestplatform.common.message.Status;
import org.jtestplatform.server.Config;
import org.jtestplatform.server.TestFailureException;

public class MauveTestRunner implements TestRunner<RunResult> {
    private static final MauveTestRunner INSTANCE = new MauveTestRunner();
    
    public static final MauveTestRunner getInstance() {
        return INSTANCE;
    }
    
    private Status status = Status.READY;
//    private RunnerThread thread = new RunnerThread();
    private Config config;
    
    private MauveTestRunner() {        
    }
    
    public void setConfig(Config config) {
        this.config = config;        
    }
    
    @Override
    public RunResult runTest(String test) throws TestFailureException {
        status = Status.RUNNING;
        JTSMauve m = new JTSMauve();
        RunResult result = m.runTest(test);
        status = Status.READY;
        
        return result;
    }

    public Status getStatus() {
        return status;
    }

    private class JTSMauve extends Mauve {
        public RunResult runTest(String testName) {
            // save the default locale, some tests change the default
            Locale savedLocale = Locale.getDefault();
            
            result = new RunResult("Mauve Test Run");
            addSystemProperties(result);
            currentCheck = new CheckResult(0, false);

            executeLine("", testName);
            
            // restore the default locale
            Locale.setDefault(savedLocale);
            
            return getResult();
        }
    }
}
