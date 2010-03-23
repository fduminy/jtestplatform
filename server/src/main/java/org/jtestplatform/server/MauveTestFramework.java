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
 */
/**
 * 
 */
package org.jtestplatform.server;

import gnu.testlet.runner.CheckResult;
import gnu.testlet.runner.Mauve;
import gnu.testlet.runner.RunResult;

import java.util.List;
import java.util.Locale;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class MauveTestFramework implements TestFramework {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "mauve";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getTests() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String runTest(String test) {
        JTSMauve m = new JTSMauve();
        RunResult result = m.runTest(test);
        return ""; //TODO
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
