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
package org.jtestplatform.common.transport;

import org.jtestplatform.common.message.ErrorMessage;
import org.jtestplatform.common.message.GetTestFrameworks;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.message.Shutdown;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TransportHelper {

    static final String TRUE = "1";
    static final String FALSE = "0";

    public void send(Transport transport, Message message) throws TransportException {
        transport.send(message.getClass().getName());
        message.sendWith(transport);
    }

    public Message receive(Transport transport) throws TransportException {
        String className = transport.receive();

        try {
            Class<? extends Message> clazz = Class.forName(className).asSubclass(Message.class);
            Message message = createMessage(clazz);

            message.receiveFrom(transport);
            if (message instanceof ErrorMessage) {
                throw new TransportException((ErrorMessage) message);
            }
            return message;
        } catch (ClassNotFoundException e) {
            throw new TransportException("can't find message of type " + className, e);
        } catch (InstantiationException e) {
            throw new TransportException("can't create message of type " + className, e);
        } catch (IllegalAccessException e) {
            throw new TransportException("can't create message of type " + className, e);
        } catch (ClassCastException cce) {
            throw new TransportException(className + " is not a message type", cce);
        }
    }

    /**
     * @throws IOException
     *
     */
    public void stop(Transport transport) throws IOException {
        transport.close();
    }

    Message createMessage(Class<? extends Message> clazz) throws InstantiationException, IllegalAccessException {
        if (GetTestFrameworks.class.equals(clazz)) {
            return GetTestFrameworks.INSTANCE;
        }
        if (Shutdown.class.equals(clazz)) {
            return Shutdown.INSTANCE;
        }
        return clazz.newInstance();
    }

    public static Collection<String> receiveList(Transport transport) throws TransportException {
        int nbFrameworks = Integer.parseInt(transport.receive());
        List<String> frameworks = new ArrayList<String>(nbFrameworks);
        for (int i = 0; i < nbFrameworks; i++) {
            frameworks.add(transport.receive());
        }
        return frameworks;
    }

    public static void sendList(Transport transport, Collection<String> items) throws TransportException {
        transport.send(Integer.toString(items.size()));
        for (String item : items) {
            transport.send(item);
        }
    }

    public static boolean receiveBoolean(Transport transport) throws TransportException {
        return TRUE.equals(transport.receive());
    }

    public static void sendBoolean(Transport transport, boolean value) throws TransportException {
        transport.send(value ? TRUE : FALSE);
    }
}
