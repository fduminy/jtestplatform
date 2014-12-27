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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class UDPTransport implements Transport {
    private static final Logger LOGGER = LoggerFactory.getLogger(UDPTransport.class);

    private static final int NULL_SIZE = Integer.MIN_VALUE;
    private static final int MAX_SIZE = Integer.MAX_VALUE;

    /**
     *  size of an int in bytes.
     */
    private static final int INT_SIZE = Integer.SIZE / 4;

    private final DatagramSocket socket;

    /**
     *
     */
    public UDPTransport(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     *
     */
    @Override
    public void send(String message) throws TransportException {
        try {
            if (message == null) {
                // send a null message
                ByteBuffer byteBuffer = ByteBuffer.allocate(INT_SIZE).
                    putInt(NULL_SIZE);
                byte[] data = byteBuffer.array();
                SocketAddress remoteAddress = socket.getRemoteSocketAddress();
                DatagramPacket packet = new DatagramPacket(data, data.length,
                        remoteAddress);

                socket.send(packet);
            } else {
                final byte[] bytes = message.getBytes();

                // send size of data
                ByteBuffer byteBuffer = ByteBuffer.allocate(INT_SIZE).
                    putInt(bytes.length);
                byte[] data = byteBuffer.array();
                //remoteAddress = (remoteAddress == null) ? socket.getRemoteSocketAddress() : remoteAddress;
                SocketAddress remoteAddress = socket.getRemoteSocketAddress();
                DatagramPacket packet = new DatagramPacket(data, data.length,
                        remoteAddress);

                socket.send(packet);

                LOGGER.info("nb bytes sent : {}", bytes.length);

                // send data
                packet = new DatagramPacket(bytes, bytes.length, remoteAddress);
                socket.send(packet);

//                ByteBuffer bb = ByteBuffer.allocate(command.length() * CHAR_SIZE + INT_SIZE);
//                bb.putInt(command.length()).asCharBuffer().append(command);
//                socket.getChannel().send(bb, socket.getRemoteSocketAddress());
            }
        } catch (SocketTimeoutException e) {
            throw new TransportException("timeout in send", e);
        } catch (IOException e) {
            throw new TransportException("error in send", e);
        }
    }

    @Override
    public String receive() throws TransportException {
        try {
            // receive size of data
            byte[] data = new byte[INT_SIZE];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            int size = ByteBuffer.wrap(data).getInt();

            String message = null;
            if (size != NULL_SIZE) {
                LOGGER.info("nb bytes received : {}", size);
                if (size > MAX_SIZE) {
                    throw new TransportException(
                            "stream probably corrupted : received more than "
                            + MAX_SIZE + " bytes (" + size + ")");
                }

                // receive actual data
                data = new byte[size];
                packet = new DatagramPacket(data, data.length);
                socket.receive(packet);

                message = new String(packet.getData());
            }

            return message;

//            ByteBuffer bb = ByteBuffer.allocate(INT_SIZE);
//            socket.getChannel().read(bb);
//            int size = bb.getInt();
//            bb = ByteBuffer.allocate(size);
//            socket.getChannel().read(bb);
//
//            return bb.asCharBuffer().rewind().toString();
        } catch (SocketTimeoutException e) {
            throw new TransportException("timeout in receive", e);
        } catch (IOException e) {
            throw new TransportException("error in receive", e);
        }
    }

    /**
     *
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * {@inheritDoc}
     */
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
}
