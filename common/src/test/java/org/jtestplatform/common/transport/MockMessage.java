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
package org.jtestplatform.common.transport;

import org.jtestplatform.common.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class MockMessage implements Message {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    static final int PART_COUNT = 4;
    private static final String PATTERN = "%d_PART%d";

    private final int id = NEXT_ID.getAndIncrement();
    private final List<String> messageParts;

    public MockMessage() {
        messageParts = new ArrayList<String>(1 + PART_COUNT);
        messageParts.add(getClass().getName());
        for (int i = 0; i < PART_COUNT; i++) {
            messageParts.add(String.format(PATTERN, id, i));
        }
    }

    @Override
    public void sendWith(Transport t) throws TransportException {
        for (int i = 1; i < messageParts.size(); i++) {
            String messagePart = messageParts.get(i);
            t.send(messagePart);
        }
    }

    @Override
    public void receiveFrom(Transport t) throws TransportException {
        //        for (int i = 1; i < messageParts.size(); i++) {
        //            String messagePart = messageParts.get(i);
        //            t.send(messagePart);
        //        }
    }

    public List<String> getMessageParts() {
        return messageParts;
    }
}
