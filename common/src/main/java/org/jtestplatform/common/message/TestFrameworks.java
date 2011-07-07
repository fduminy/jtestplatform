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
package org.jtestplatform.common.message;

import java.util.HashSet;
import java.util.Set;

import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TestFrameworks implements Message {
    private Set<String> frameworks;

    public TestFrameworks() {
        // nothing
    }

    public TestFrameworks(Set<String> frameworks) {
        this.frameworks = frameworks;
    }

    /**
     * @return the frameworks
     */
    public Set<String> getFrameworks() {
        return frameworks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWith(Transport transport) throws TransportException {
        transport.send(Integer.toString(frameworks.size()));
        for (String test : frameworks) {
            transport.send(test);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveFrom(Transport transport) throws TransportException {
        int nbFrameworks = Integer.getInteger(transport.receive());
        frameworks = new HashSet<String>(nbFrameworks);
        for (int i = 0; i < nbFrameworks; i++) {
            frameworks.add(transport.receive());
        }
    }
}
