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
package org.jtestplatform.server;

import org.jtestplatform.common.message.*;
import org.jtestplatform.common.message.Shutdown;
import org.jtestplatform.common.transport.*;
import org.jtestplatform.server.commands.GetFrameworkTestsCommand;
import org.jtestplatform.server.commands.GetTestFrameworksCommand;
import org.jtestplatform.server.commands.RunTestCommand;
import org.jtestplatform.server.commands.ShutdownCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class TestServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestServer.class);

    public static void main(String[] args) {
        try {
            TestServer server = new TestServer();
            server.start();
        } catch (IOException e) {
            LOGGER.error("unable to read config", e);
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("unable to create server", e);
            System.exit(2);
        }
    }

    private static final int SERVER_PORT = 10000;

    private final Map<Class<? extends Message>, TestServerCommand<? extends Message, ? extends Message>> messageClassToCommand;
    private final TransportHelper transportManager;
    private final TransportFactory transportFactory;
    private Transport transport;

    public TestServer() throws Exception {
        this(null);
    }

    public TestServer(TransportFactory transportFactory) throws Exception {
        messageClassToCommand = new HashMap<Class<? extends Message>, TestServerCommand<? extends Message, ? extends Message>>();

        addCommand(RunTest.class, new RunTestCommand());
        addCommand(Shutdown.class, new ShutdownCommand(this));
        addCommand(GetTestFrameworks.class, new GetTestFrameworksCommand());
        addCommand(GetFrameworkTests.class, new GetFrameworkTestsCommand());

        if (transportFactory == null) {
            LOGGER.warn("no TransportFactory specified. Using default one (UDPTransport on port {})", SERVER_PORT);
            this.transportFactory = new TransportFactory() {
                @Override
                public Transport create() throws TransportException {
                    return new UDPTransport(SERVER_PORT);
                }
            };
        } else {
            this.transportFactory = transportFactory;
        }
        transportManager = new TransportHelper();
    }

    <TM extends Message> void addCommand(Class<TM> messageClass, TestServerCommand<TM, ? extends Message> command) {
        messageClassToCommand.put(messageClass, command);
    }

    public void start() throws Exception {
        LOGGER.info("server started");

        transport = transportFactory.create();
        while (true) {
            processCommand(transport);
        }
    }

    @SuppressWarnings("unchecked")
    void processCommand(Transport transport) throws TransportException {
        Message message = transportManager.receive(transport);
        TestServerCommand command = messageClassToCommand.get(message.getClass());

        if (command != null) {
            Message result = null;
            try {
                result = command.execute(message);
            } catch (Exception e) {
                handleError(transport, "Error in " + command.getClass().getSimpleName() + " : " + e.getMessage(), e);
            }
            if (result != null) {
                try {
                    transportManager.send(transport, result);
                } catch (TransportException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } else {
            handleError(transport, "No command for message : " + message.getClass().getName(), null);
        }
    }

    private void handleError(Transport transport, String message, Exception e) throws TransportException {
        if (e == null) {
            LOGGER.error(message);
        } else {
            LOGGER.error(message, e);
        }
        transportManager.send(transport, new ErrorMessage(message));
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
