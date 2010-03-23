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
 * -
 */
package org.jtestplatform.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jtestplatform.common.message.GetStatus;
import org.jtestplatform.common.message.Message;
import org.jtestplatform.common.message.RunTest;
import org.jtestplatform.common.message.Shutdown;
import org.jtestplatform.common.transport.Transport;
import org.jtestplatform.common.transport.TransportException;
import org.jtestplatform.common.transport.TransportFactory;
import org.jtestplatform.common.transport.TransportHelper;
import org.jtestplatform.common.transport.UDPTransport;
import org.jtestplatform.server.commands.GetStatusCommand;
import org.jtestplatform.server.commands.RunTestCommand;
import org.jtestplatform.server.commands.ShutdownCommand;

public class TestServer<T extends Message> {
    private static final Logger LOGGER = Logger.getLogger(TestServer.class);
        
    public static void main(String[] args) {
        try {
            TestServer<Message> server = new TestServer<Message>();
            server.start();
        } catch (IOException e) {
            LOGGER.fatal("unable to read config", e);
            System.exit(1);
        } catch (Exception e) {
            LOGGER.fatal("unable to create server", e);
            System.exit(2);
        }
    }
    
    private final Map<Class<? extends Message>, TestServerCommand<? extends Message, ? extends Message>> messageClassToCommand;
    private final Map<String, TestFramework> testFrameworks;
    private final Config config;
    private final TransportHelper transportManager;
    private final TransportFactory transportFactory;
    private Transport transport;
    
    public TestServer() throws Exception {
        messageClassToCommand = new HashMap<Class<? extends Message>, TestServerCommand<? extends Message, ? extends Message>>();
        
        addCommand(RunTest.class, new RunTestCommand(this));
        addCommand(Shutdown.class, new ShutdownCommand(this));
        addCommand(GetStatus.class, new GetStatusCommand());

        config = Config.read(); //TODO remove it ?

        transportFactory = new TransportFactory() {
            @Override
            public Transport create() throws TransportException {
                try {
                    return new UDPTransport(new DatagramSocket(config.getPort()));
                } catch (SocketException e) {
                    throw new TransportException("unable to create a transport", e);
                }
            }
        };
        transportManager = new TransportHelper();

        testFrameworks = new HashMap<String, TestFramework>();
        addTestFramework(new JUnitTestFramework());
        addTestFramework(new MauveTestFramework());
    }

    public void addTestFramework(TestFramework framework) {
        testFrameworks.put(framework.getName(), framework);
    }

    /**
     * @param framework
     * @return
     */
    public TestFramework getTestFramework(String framework) {
        return testFrameworks.get(framework);
    }

    /**
     * @param framework
     * @return
     */
    public Set<String> getTestFrameworks() {
        return testFrameworks.keySet();
    }

    private <TM extends Message> void addCommand(Class<TM> messageClass, TestServerCommand<TM, ? extends Message> command) {
        messageClassToCommand.put(messageClass, command);
    }

    public void start() throws Exception {
        LOGGER.info("server started");

        transport = transportFactory.create();
        while (true) {
            Message message = transportManager.receive(transport);
            TestServerCommand<T, ?> command = (TestServerCommand<T, ?>) messageClassToCommand.get(message.getClass());
            
            if (command != null) {
                try {
                    Message result = command.execute((T) message);
                    if (result != null) {
                        transportManager.send(transport, result);
                    }
                } catch (Throwable t) {
                    LOGGER.error("error in command", t);
                }
            }
        }
    }
    
    private void shutdown() {
        if (transport != null) {
            try {
                transportManager.stop(transport);
                LOGGER.info("Server has shutdown");
            } catch (IOException e) {
                LOGGER.error("An error happened while shutting down", e);
            }
        }
    }
    
    public void requestShutdown() {
        LOGGER.info("shutdown requested");
        shutdown();    
    }
}
