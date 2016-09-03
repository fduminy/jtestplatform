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

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

import static java.lang.String.format;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
public class UDPTransport implements Transport {
    static final int NULL_SIZE = Integer.MIN_VALUE;

    /**
     *  size of an int in bytes.
     */
    private static final int INT_SIZE = Integer.SIZE / 8;

    private InetAddress address;
    private int port;
    private final DatagramSocket socket;
    private DatagramPacket packet;

    public UDPTransport(int serverPort) throws TransportException {
        try {
            this.socket = createDatagramSocket(serverPort);
        } catch (SocketException e) {
            throw new TransportException(e.getMessage(), e);
        }
        port = serverPort;
    }

    public UDPTransport(InetAddress serverAddress, int serverPort, int timeout) throws TransportException {
        try {
            this.socket = createDatagramSocket();
            if (timeout > 0) {
                socket.setSoTimeout(timeout);
            }
        } catch (SocketException e) {
            throw new TransportException(e.getMessage(), e);
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
                if (size < 0) {
                    throw new TransportException(
                        format("stream corrupted : received negative message size (%d)", size));
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
        DatagramPacket packet = getDatagramPacket(buffer);
        packet.setAddress(address);
        packet.setPort(port);
        socket.send(packet);
    }

    private int receiveInt() throws IOException, TransportException {
        return ByteBuffer.wrap(receiveBytes(INT_SIZE)).getInt();
    }

    private String receiveString(int length) throws IOException, TransportException {
        return new String(receiveBytes(length));
    }

    private byte[] receiveBytes(int length) throws IOException, TransportException {
        byte[] buffer = new byte[length];
        DatagramPacket packet = getDatagramPacket(buffer);
        socket.receive(packet);
        if (address == null) {
            address = packet.getAddress();
            port = packet.getPort();
        }
        if (packet.getLength() < buffer.length) {
            throw new TransportException(
                format("stream corrupted : expected %d bytes but only %s bytes were received", buffer.length,
                       packet.getLength()));
        }
        return buffer;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    private DatagramPacket getDatagramPacket(byte[] buffer) {
        if (packet == null) {
            packet = new DatagramPacket(buffer, buffer.length);
        } else {
            packet.setData(buffer);
        }
        return packet;
    }
}
