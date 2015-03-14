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

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class UDPTransport implements Transport {
    static final int NULL_SIZE = Integer.MIN_VALUE;
    private static final int MAX_SIZE = Integer.MAX_VALUE;

    /**
     *  size of an int in bytes.
     */
    private static final int INT_SIZE = Integer.SIZE / 8;

    private InetAddress address;
    private int port;
    private final DatagramSocket socket;

    public UDPTransport(int serverPort) throws SocketException {
        this.socket = createDatagramSocket(serverPort);
        port = serverPort;
    }

    public UDPTransport(InetAddress serverAddress, int serverPort, int timeout) throws SocketException {
        this.socket = createDatagramSocket();
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }

        address = serverAddress;
        port = serverPort;
    }

    protected DatagramSocket createDatagramSocket() throws SocketException {
        return new DatagramSocket();
    }

    DatagramSocket createDatagramSocket(int serverPort) throws SocketException {
        return new DatagramSocket(serverPort);
    }

    @Override
    public void send(String message) throws TransportException {
        try {
            if (message == null) {
                sendInt(NULL_SIZE);
            } else {
                sendInt(message.length());
                sendString(message);
            }
        } catch (SocketTimeoutException e) {
            throw new TransportException("timeout in receive", e);
        } catch (IOException e) {
            throw new TransportException("error in receive", e);
        }
    }

    @Override
    public String receive() throws TransportException {
        try {
            int size = receiveInt();

            String message = null;
            if (size != NULL_SIZE) {
                if (size > MAX_SIZE) {
                    throw new TransportException(
                            "stream probably corrupted : received more than "
                            + MAX_SIZE + " bytes (" + size + ")");
                }

                message = receiveString(size);
            }

            return message;
        } catch (SocketTimeoutException e) {
            throw new TransportException("timeout in receive", e);
        } catch (IOException e) {
            throw new TransportException("error in receive", e);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("UDPTransport[");
        if (socket.isConnected()) {
            sb.append(socket.getInetAddress()).append(":");
            sb.append(socket.getPort());
        } else {
            sb.append("not connected");
        }
        sb.append(']');
        return sb.toString();
    }

    private void sendInt(int integer) throws IOException {
        sendBytes(ByteBuffer.allocate(INT_SIZE).putInt(integer).array());
    }

    private void sendString(String message) throws IOException {
        sendBytes(message.getBytes());
    }

    private void sendBytes(byte[] buffer) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    private int receiveInt() throws IOException {
        return ByteBuffer.wrap(receiveBytes(INT_SIZE)).getInt();
    }

    private String receiveString(int length) throws IOException {
        return new String(receiveBytes(length));
    }

    private byte[] receiveBytes(int length) throws IOException {
        byte[] buffer = new byte[length];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        if (address == null) {
            address = packet.getAddress();
            port = packet.getPort();
        }
        return buffer;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
