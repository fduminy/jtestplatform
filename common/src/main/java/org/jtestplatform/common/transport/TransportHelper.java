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
package org.jtestplatform.common.transport;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jtestplatform.common.message.Message;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class TransportHelper {
    private static final Logger LOGGER = Logger.getLogger(TransportHelper.class);
    
    public void send(Transport transport, Message message) throws TransportException {
        transport.send(message.getClass().getName());
        
        if (message.getClass().isEnum()) {
            transport.send(((Enum<?>) message).name());
        }
        
        message.sendWith(transport);
    }
    
    public Message receive(Transport transport) throws TransportException {
        String className = transport.receive();
        
        try {
            Class<? extends Message> clazz = Class.forName(className).asSubclass(Message.class);
            
            Message message;
            if (clazz.isEnum()) {
                String enumValue = transport.receive();
                message = createEnum(clazz, enumValue);
            } else {
                message = clazz.newInstance();
                message.receiveFrom(transport);
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
    
    @SuppressWarnings("unchecked")
    private Message createEnum(Class<? extends Message> clazz, String enumValue) {
        return clazz.cast(Enum.valueOf(clazz.asSubclass(Enum.class), enumValue));
    }

    /**
     * @throws IOException 
     * 
     */
    public void stop(Transport transport) throws IOException {
        transport.close();
    }
}
