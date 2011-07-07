/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2011  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.common.message;

import java.util.ArrayList;
import java.util.Collection;

import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class FrameworkTests implements Message {
    private Collection<String> tests;

    public FrameworkTests() {
        // nothing
    }

    public FrameworkTests(Collection<String> tests) {
        this.tests = tests;
    }

    /**
     * @return the tests
     */
    public Collection<String> getTests() {
        return tests;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWith(Transport transport) throws TransportException {
        transport.send(Integer.toString(tests.size()));
        for (String test : tests) {
            transport.send(test);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveFrom(Transport transport) throws TransportException {
        int nbTests = Integer.getInteger(transport.receive());
        tests = new ArrayList<String>(nbTests);
        for (int i = 0; i < nbTests; i++) {
            tests.add(transport.receive());
        }
    }
}
