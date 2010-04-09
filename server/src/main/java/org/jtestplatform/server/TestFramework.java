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
package org.jtestplatform.server;

import java.util.Collection;
import java.util.Set;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public interface TestFramework {
    /**
     * {@inheritDoc}
     * @param testClass
     * @throws Exception
     */
    void addTestClass(Class<?> testClass) throws Exception;

    String getName();

    Collection<String> getTests();

    boolean runTest(String test) throws UnknownTestException;

    /**
     * @param testClass
     * @return a list of tests provided by the given class,
     * null if there is none or the class is not a valid
     * test class for the framework.
     */
    Set<String> getTests(Class<?> testClass);
}
