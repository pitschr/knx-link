/*
 * Copyright (C) 2022 Pitschmann Christoph
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

package li.pitschmann.knx.link.test;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.core.communication.KnxStatusData;
import li.pitschmann.knx.core.communication.KnxStatusPool;
import li.pitschmann.knx.core.datapoint.value.DataPointValue;
import li.pitschmann.knx.link.Action;
import li.pitschmann.knx.link.ChannelPacket;
import li.pitschmann.knx.link.SecurityAuditor;
import li.pitschmann.knx.link.config.Config;
import li.pitschmann.knx.link.protocol.ResponseBody;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Helper Class for Test Scenarios
 */
public final class Helper {

    /**
     * Create a mock {@link Config} that is suitable for testing purposes only.
     * <p>
     * The server port is always {@link Config#DEFAULT_SERVER_PORT} by default.
     * The {@link SecurityAuditor#isRemoteAddressValid(SocketChannel)} returns always {@code true} by default.
     *
     * @return mocked {@link Config}
     */
    public static Config createConfigMock() {
        final var configMock = mock(Config.class);

        when(configMock.getServerPort()).thenReturn(Config.DEFAULT_SERVER_PORT);

        final var securityAuditor = mock(SecurityAuditor.class);
        when(securityAuditor.isRemoteAddressValid(any(SocketChannel.class))).thenReturn(true);
        when(configMock.getSecurityAuditor()).thenReturn(securityAuditor);

        when(configMock.getKnxClientConfig()).thenReturn(mock(li.pitschmann.knx.core.config.Config.class));

        return configMock;
    }

    /**
     * Creates a mock {@link KnxClient} whereas the {@link KnxClient#readRequest(GroupAddress)}
     * and {@link KnxClient#writeRequest(GroupAddress, DataPointValue)} is simulated with {@code true}.
     *
     * @return mocked {@link KnxClient}
     */
    public static KnxClient createKnxClientMock() {
        final var knxClientMock = mock(KnxClient.class);
        when(knxClientMock.isRunning()).thenReturn(true);
        when(knxClientMock.readRequest(any(GroupAddress.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(knxClientMock.writeRequest(any(GroupAddress.class), any(DataPointValue.class))).thenReturn(CompletableFuture.completedFuture(true));
        final var knxStatusPoolMock = mock(KnxStatusPool.class);
        when(knxClientMock.getStatusPool()).thenReturn(knxStatusPoolMock);
        return knxClientMock;
    }

    /**
     * Creates a mocked {@link KnxStatusData} to simulate a response from a KNX Net/IP device
     * @param knxClientMock the mocked {@link KnxClient}; may not be null
     * @param dpv the {@link DataPointValue} that should be returned for testing purpose; may not be null
     * @return mocked {@link KnxStatusData}
     */
    public static KnxStatusData createKnxStatusDataMock(final KnxClient knxClientMock, final DataPointValue dpv) {
        final var knxStatusDataMock = mock(KnxStatusData.class);
        when(knxStatusDataMock.getData()).thenReturn(dpv.toByteArray());
        when(knxClientMock.getStatusPool().getStatusFor(any(GroupAddress.class))).thenReturn(knxStatusDataMock);
        return knxStatusDataMock;
    }

    /**
     * Creates a mock {@link ChannelPacket} with mock {@link SocketChannel}
     * and given {@code bytes} as byte array for {@link ChannelPacket#getBytes()}
     *
     * @param bytes the byte array containing data for read/write request operation
     * @return mocked {@link ChannelPacket}
     */
    public static ChannelPacket createChannelPacketMock(final byte[] bytes) {
        final var inetAddressMock = mock(InetAddress.class);
        when(inetAddressMock.getHostAddress()).thenReturn("127.0.0.1");

        final var socketAddressMock = mock(InetSocketAddress.class);
        when(socketAddressMock.getAddress()).thenReturn(inetAddressMock);

        final var channelMock = mock(SocketChannel.class);
        when(channelMock.isConnected()).thenReturn(true);
        try {
            when(channelMock.getRemoteAddress()).thenReturn(socketAddressMock);
        } catch (IOException e) {
            throw new AssertionError("Should never happen with mock!");
        }

        final var channelPacketMock = mock(ChannelPacket.class);
        when(channelPacketMock.getChannel()).thenReturn(channelMock);
        when(channelPacketMock.getBytes()).thenReturn(bytes);

        return channelPacketMock;
    }

    /**
     * Creates a mock {@link ChannelPacket} with default values
     *
     * @return mocked {@link ChannelPacket}
     */
    public static ChannelPacket createChannelPacketMock() {
        return createChannelPacketMock(new byte[0]);
    }

    /**
     * Verifies that given list of {@link ResponseBody} was returned by the KNX Link Server
     *
     * @param channelPacketMock the channel packet to be inspected; may not be null
     * @param expectedResponses list of {@link ResponseBody} to be expected; may not be null
     */
    public static void verifyChannelPackets(final ChannelPacket channelPacketMock, final List<ResponseBody> expectedResponses) {
        final var argCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
        try {
            verify(channelPacketMock.getChannel(), timeout(5000).times(expectedResponses.size()))
                    .write(argCaptor.capture());
        } catch (IOException e) {
            throw new AssertionError("Should not happen!");
        }

        // first 3 bytes will be skipped:
        // Byte 0: Protocol Version
        // Byte 1: Type of response
        // Byte 2: Length of response byte array
        assertThat(argCaptor.getAllValues().stream().map(ByteBuffer::array)
                .map(a -> ResponseBody.of(Arrays.copyOfRange(a, 3, a.length)))
        ).containsExactlyElementsOf(expectedResponses);
    }

    /**
     * Verifies that no channel packets have been sent to the {@link ChannelPacket}
     *
     * @param channelPacketMock the channel packet to be inspected; may not be null
     */
    public static void verifyNoChannelPackets(final ChannelPacket channelPacketMock) {
        try {
            verify(channelPacketMock.getChannel(), never()).write(any(ByteBuffer.class));
        } catch (IOException e) {
            throw new AssertionError("Should not happen!");
        }
    }

    /**
     * Returns bytes for a message in Protocol Version 1
     *
     * @param action       the action to be requested; may not be null
     * @param groupAddress the group address in free-level (1), two-level (1/2)
     *                     or three-level (1/2/3) format; may not be null
     * @param dpt          the data point type in "x.y" format (e.g. 1.001); may be null (defaults to 0.000)
     * @param arguments    array of string that represents the arguemnt to be sent; may be null for read (defaults to new String[0])
     * @return new byte array
     */
    public static byte[] createProtocolV1Packet(final Action action,
                                                final String groupAddress,
                                                final @Nullable String dpt,
                                                final @Nullable String[] arguments) {
        final var bytes = new byte[256];
        int i = 0;

        // Protocol Version 1
        bytes[i++] = 0x01;
        // Action (read, write)
        bytes[i++] = action.getByte();
        // Length (will be temporarily set here!)
        bytes[i++] = 0x00;
        // Group Address
        final var groupAddressAsBytes = GroupAddress.of(groupAddress).toByteArray();
        bytes[i++] = groupAddressAsBytes[0];
        bytes[i++] = groupAddressAsBytes[1];
        // Data Point type
        if (dpt != null) {
            final var dataPointType = dpt.split("\\.");
            final var type = Integer.parseInt(dataPointType[0]);
            final var subType = Integer.parseInt(dataPointType[1]);
            bytes[i++] = (byte) ((type & 0xFF00) >> 8);
            bytes[i++] = (byte) (type & 0xFF);
            bytes[i++] = (byte) ((subType & 0xFF00) >> 8);
            bytes[i++] = (byte) (subType & 0xFF);
        } else {
            i += 4; // skip 4 bytes and leave them as 0x00
        }
        // Arguments
        if (arguments != null) {
            for (int a = 0; a < arguments.length; a++) {
                if (a != 0) {
                    bytes[i++] = ' '; // space
                }
                bytes[i++] = '"'; // quote
                final var argAsBytes = arguments[a].getBytes(StandardCharsets.UTF_8);
                System.arraycopy(argAsBytes, 0, bytes, i, argAsBytes.length);
                i += argAsBytes.length;
                bytes[i++] = '"'; // quote
            }
        }

        // update (Header.length) with the correct body length
        bytes[2] = (byte) (i - 3);

        return Arrays.copyOf(bytes, i);
    }
}
