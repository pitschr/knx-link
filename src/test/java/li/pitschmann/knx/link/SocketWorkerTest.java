/*
 * Copyright (C) 2021 Pitschmann Christoph
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package li.pitschmann.knx.link;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.datapoint.DPT7;
import li.pitschmann.knx.core.exceptions.KnxEnumNotFoundException;
import li.pitschmann.knx.link.protocol.ResponseBody;
import li.pitschmann.knx.link.test.Helper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static li.pitschmann.knx.link.test.Helper.createChannelPacketMock;
import static li.pitschmann.knx.link.test.Helper.createKnxClientMock;
import static li.pitschmann.knx.link.test.Helper.createKnxStatusDataMock;
import static li.pitschmann.knx.link.test.Helper.verifyChannelPackets;
import static li.pitschmann.knx.link.test.Helper.verifyNoChannelPackets;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SocketWorker}
 */
class SocketWorkerTest {

    @Test
    @DisplayName("Worker without Knx Client")
    void test_NoKnxClient() {
        assertThatThrownBy(() -> new SocketWorker(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("#execute(ChannelPacket) with NULL")
    void test_execute_Null() {
        final var worker = new SocketWorker(createKnxClientMock());
        assertThatThrownBy(() -> worker.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("#execute(ChannelPacket) without bytes")
    void test_execute_NullBytes() {
        final var worker = new SocketWorker(createKnxClientMock());

        final var channelPacketMock = mock(ChannelPacket.class);
        when(channelPacketMock.getBytes()).thenReturn(null);

        assertThatThrownBy(() -> worker.execute(channelPacketMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bytes is required.");
    }

    @Test
    @DisplayName("#execute(ChannelPacket) with empty bytes")
    void test_execute_EmptyBytes() {
        final var worker = new SocketWorker(createKnxClientMock());

        final var channelPacketMock = mock(ChannelPacket.class);
        when(channelPacketMock.getBytes()).thenReturn(new byte[0]);

        assertThatThrownBy(() -> worker.execute(channelPacketMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bytes is required.");
    }

    @Test
    @DisplayName("#execute(ChannelPacket) with unsupported protocol version")
    void test_execute_UnsupportedProtocolVersion() {
        final var worker = new SocketWorker(createKnxClientMock());

        final var channelPacketMock = mock(ChannelPacket.class);
        when(channelPacketMock.getBytes()).thenReturn(new byte[]{(byte) 0xFF, 0x00, 0x00});

        assertThatThrownBy(() -> worker.execute(channelPacketMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Protocol Version '255' is not supported: 0xFF 00 00");
    }

    @Test
    @DisplayName("#execute(ChannelPacket) with unsupported action")
    void test_execute_UnsupportedAction() {
        final var worker = new SocketWorker(createKnxClientMock());

        final var channelPacketMock = mock(ChannelPacket.class);
        when(channelPacketMock.getBytes()).thenReturn(new byte[]{0x01, (byte) 0xEE, 0x00});

        assertThatThrownBy(() -> worker.execute(channelPacketMock))
                .isInstanceOf(KnxEnumNotFoundException.class);
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - READ REQUEST - Successful")
    void test_execute_ReadRequest() throws IOException {
        final var knxClientMock = createKnxClientMock();
        createKnxStatusDataMock(knxClientMock, DPT7.ABSOLUTE_COLOR_TEMPERATURE.of(4711));

        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.READ_REQUEST, "1/2/3", "7.600", null)
        );

        final var worker = new SocketWorker(knxClientMock);
        worker.execute(channelPacketMock);

        verifyChannelPackets(channelPacketMock,
                List.of(
                        ResponseBody.of(false, Status.SUCCESS),
                        ResponseBody.of(true, Status.SUCCESS, "4711K")
                )
        );
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - READ REQUEST - Read Request Failed")
    void test_execute_ReadRequest_Failure() throws IOException {
        final var knxClientMock = createKnxClientMock();
        when(knxClientMock.readRequest(any(GroupAddress.class))).thenReturn(CompletableFuture.completedFuture(false));

        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.READ_REQUEST, "1/2/3", "7.600", null)
        );

        final var worker = new SocketWorker(knxClientMock);
        worker.execute(channelPacketMock);

        verifyChannelPackets(channelPacketMock, List.of(
                ResponseBody.of(true, Status.ERROR_REQUEST))
        );
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - READ REQUEST - Channel is not connected")
    void test_execute_ReadRequest_ChannelNotConnected() throws IOException {
        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.READ_REQUEST, "1/2/3", "7.600", null)
        );
        when(channelPacketMock.getChannel().isConnected()).thenReturn(false); // HERE: not connected!

        final var worker = new SocketWorker(createKnxClientMock());
        worker.execute(channelPacketMock);

        verifyNoChannelPackets(channelPacketMock);
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - READ REQUEST - No response from KNX Router")
    void test_execute_ReadRequest_NoReadResponseFromKnx() throws IOException {
        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.READ_REQUEST, "1/2/3", "7.600", null)
        );

        final var worker = new SocketWorker(createKnxClientMock());
        worker.execute(channelPacketMock); // should be fine, we silently ignore I/O

        verifyChannelPackets(channelPacketMock, List.of(
                ResponseBody.of(false, Status.SUCCESS),
                ResponseBody.of(true, Status.ERROR_TIMEOUT, "Could not get read data for group address: 1/2/3")
        ));
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - READ REQUEST - I/O Exception during Channel Write")
    void test_execute_ReadRequest_ChannelIOException() throws IOException {
        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.READ_REQUEST, "1/2/3", "7.600", null)
        );
        final var channelMock = channelPacketMock.getChannel();
        doThrow(new IOException()).when(channelMock).write(any(ByteBuffer.class)); // HERE: I/O Exception!

        final var worker = new SocketWorker(createKnxClientMock());
        worker.execute(channelPacketMock); // should be fine, we silently ignore I/O
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - WRITE REQUEST")
    void test_execute_WriteRequest() throws IOException {
        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.WRITE_REQUEST, "1/2/3", "7.600", new String[]{"4711"})
        );

        final var worker = new SocketWorker(createKnxClientMock());
        worker.execute(channelPacketMock);

        verifyChannelPackets(channelPacketMock, List.of(
                ResponseBody.of(true, Status.SUCCESS)
        ));
    }

    @Test
    @DisplayName("#execute(ChannelPacket) - WRITE REQUEST - Incompatible value for DataPointType")
    void test_execute_WriteRequest_IncompatibleDataPointType() throws IOException {
        final var channelPacketMock = createChannelPacketMock(
                Helper.createProtocolV1Packet(Action.WRITE_REQUEST, "1/2/3", "1.001", new String[]{"foobar"})
        );

        final var worker = new SocketWorker(createKnxClientMock());
        worker.execute(channelPacketMock);

        verifyChannelPackets(channelPacketMock, List.of(
                ResponseBody.of(true, Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE,
                        "I could not understand value for group address '1/2/3' and data point type '1.001': [foobar]")
        ));
    }
}
