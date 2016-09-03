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

import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;
import static java.net.InetAddress.getLocalHost;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jtestplatform.common.transport.UDPTransport.BYTE_ORDER;
import static org.jtestplatform.common.transport.UDPTransport.NULL_SIZE;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 *
 */
@RunWith(Theories.class)
public class UDPTransportTest {

    private static final int SERVER_PORT = 12345;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private DatagramSocket datagramSocket;

    @Theory
    public void testSend(StringMessage message) throws IOException, TransportException {
        assumeFalse(message.corrupted);
        // prepare
        InetAddress serverAddress = getLocalHost();
        InetSocketAddress socketAddress = spy(new InetSocketAddress(serverAddress, SERVER_PORT));
        when(datagramSocket.getRemoteSocketAddress()).thenReturn(socketAddress);
        final List<DatagramPacket> packets = new ArrayList<DatagramPacket>();
        doAnswer(captureSentPackets(packets)).when(datagramSocket).send(any(DatagramPacket.class));
        UDPTransport transport = new UDPTransportWithMockDatagramSocket(serverAddress, SERVER_PORT, 0);

        // test
        transport.send(message.value);

        // verify
        assertThat(transport.getAddress()).as("transport.address").isEqualTo(socketAddress.getAddress());
        assertThat(transport.getPort()).as("transport.port").isEqualTo(SERVER_PORT);

        verify(datagramSocket, times(message.getNbPackets())).send(any(DatagramPacket.class));
        verifyNoMoreInteractions(datagramSocket, socketAddress);
        verifyPackets(message, socketAddress, SERVER_PORT, packets);
    }

    @Theory
    public void testReceive(StringMessage message) throws IOException, TransportException {
        assumeFalse(message.corrupted);
        // prepare
        List<DatagramPacket> packets = new ArrayList<DatagramPacket>();
        doAnswer(simulateReceive(message, getLocalHost(), SERVER_PORT, packets)).when(datagramSocket)
                                                                                .receive(any(DatagramPacket.class));
        UDPTransport transport = new UDPTransportWithMockDatagramSocket(SERVER_PORT);

        // test
        String receivedMessage = transport.receive();

        // verify
        assertThat(receivedMessage).isEqualTo(message.value);
        assertThat(transport.getAddress()).as("transport.address").isEqualTo(getLocalHost());
        assertThat(transport.getPort()).as("transport.port").isEqualTo(SERVER_PORT);

        verify(datagramSocket, times(message.getNbPackets())).receive(any(DatagramPacket.class));
        verifyNoMoreInteractions(datagramSocket);
        verifyPackets(message, null, SERVER_PORT, packets);
    }

    @Theory
    public void testReceive_corruptedMessage(StringMessage message) throws IOException, TransportException {
        assumeTrue(message.corrupted);
        thrown.expect(TransportException.class);
        thrown.expectMessage("stream corrupted");
        doAnswer(simulateReceive(message, getLocalHost(), SERVER_PORT, null)).when(datagramSocket)
                                                                             .receive(any(DatagramPacket.class));

        new UDPTransportWithMockDatagramSocket(SERVER_PORT).receive();
    }

    private void verifyPackets(StringMessage message, InetSocketAddress socketAddress, int serverPort,
                               List<DatagramPacket> packets) {
        // first packet
        DatagramPacket packet1 = packets.get(0);
        assertThat(packet1).as("DatagramPacket").isNotNull();
        if (socketAddress != null) {
            assertThat(packet1.getSocketAddress()).as("serverAddress").isEqualTo(socketAddress);
            assertThat(packet1.getPort()).as("serverPort").isEqualTo(serverPort);
        }
        assertThat(packet1.getLength()).as("packet1.length").isEqualTo(4);
        assertThat(packet1.getData()).as("packet1.intValue").isEqualTo(message.getSizeAsBytes());

        if (message.getNbPackets() > 1) {
            // second packet
            DatagramPacket packet2 = packets.get(1);
            assertThat(packet2.getLength()).as("packet2.length").isEqualTo(message.value.length());
            assertThat(packet2.getData()).as("packet2.bytes").isEqualTo(message.value.getBytes());
        }
    }

    private Answer<Object> captureSentPackets(final List<DatagramPacket> packets) {
        return new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                DatagramPacket packet = (DatagramPacket) invocationOnMock.getArguments()[0];
                addClonedPacket(packets, packet);
                return null;
            }
        };
    }

    private Answer<Void> simulateReceive(final StringMessage message, final InetAddress serverAddress,
                                         final int serverPort, final List<DatagramPacket> packets) {
        return new Answer<Void>() {
            int callNumber = 0;

            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                callNumber++;

                DatagramPacket packet = (DatagramPacket) invocationOnMock.getArguments()[0];
                packet.setAddress(serverAddress);
                packet.setPort(serverPort);
                addClonedPacket(packets, packet);

                byte[] data;
                if (callNumber == 1) {
                    // receive the message size
                    data = message.getSizeAsBytes();
                    System.arraycopy(data, 0, packet.getData(), 0, data.length);
                    packet.setLength(data.length);
                } else if (callNumber == 2) {
                    // receive the message content
                    data = message.value.getBytes();
                    packet.setLength(min(data.length, packet.getData().length));
                    System.arraycopy(data, 0, packet.getData(), 0, packet.getLength());
                }
                return null;
            }
        };
    }

    private void addClonedPacket(List<DatagramPacket> packets, DatagramPacket packet) {
        if (packets != null) {
            DatagramPacket clonedPacket = new DatagramPacket(packet.getData(), packet.getOffset());
            clonedPacket.setAddress(packet.getAddress());
            clonedPacket.setPort(packet.getPort());
            clonedPacket.setLength(packet.getLength());
            packets.add(clonedPacket);
        }
    }

    public static enum StringMessage {
        MESSAGE("a value", false),
        NULL_MESSAGE(null, false) {
            @Override
            byte[] getSizeAsBytes() {
                return toBytes(NULL_SIZE);
            }

            @Override
            int getNbPackets() {
                return 1;
            }
        },
        EMPTY_MESSAGE("", false),
        NEGATIVE_MESSAGE_SIZE("", true) {
            @Override byte[] getSizeAsBytes() {
                return StringMessage.toBytes(-1);
            }
        },
        MESSAGE_SIZE_TOO_BIG("A", true) {
            @Override byte[] getSizeAsBytes() {
                return StringMessage.toBytes(this.value.length() + 1);
            }
        },
        MESSAGE_SIZE_CUT("", true) {
            @Override byte[] getSizeAsBytes() {
                return new byte[] { 1 }; // 1 byte instead of 4 bytes for size
            }
        },
        WRONG_BYTE_ORDER("A", true) {
            @Override byte[] getSizeAsBytes() {
                return toBytes(value.length(), (BYTE_ORDER == BIG_ENDIAN) ? LITTLE_ENDIAN : BIG_ENDIAN);
            }
        };
/*
       //TODO handle case of a big message
        A_BIG_MESSAGE(createBigMessage());

        private static String createBigMessage() {
        int size = 10 * 1024 * 1024;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append((char) i);
        }
            return sb.toString();
        }
*/

        protected final String value;
        private final boolean corrupted;

        StringMessage(String value, boolean corrupted) {
            this.value = value;
            this.corrupted = corrupted;
        }

        byte[] getSizeAsBytes() {
            return toBytes(value.length(), BYTE_ORDER);
        }

        int getNbPackets() {
            return 2;
        }

        private static byte[] toBytes(int value) {
            return toBytes(value, BYTE_ORDER);
        }

        private static byte[] toBytes(int value, ByteOrder byteOrder) {
            return ByteBuffer.allocate(4).order(byteOrder).putInt(value).array();
        }
    }

    private class UDPTransportWithMockDatagramSocket extends UDPTransport {
        private final boolean server;

        public UDPTransportWithMockDatagramSocket(InetAddress serverAddress, int serverPort, int timeout)
            throws TransportException {
            super(serverAddress, serverPort, timeout);
            server = false;
        }

        public UDPTransportWithMockDatagramSocket(int serverPort) throws TransportException {
            super(serverPort);
            server = true;
        }

        @Override
        DatagramSocket createDatagramSocket(int serverPort) throws SocketException {
            return datagramSocket;
        }

        @Override
        protected DatagramSocket createDatagramSocket() throws SocketException {
            return datagramSocket;
        }
    }
}
