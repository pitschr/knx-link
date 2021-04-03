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

package li.pitschmann.knx.link.test;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.core.communication.KnxStatusPool;
import li.pitschmann.knx.core.datapoint.value.DataPointValue;
import li.pitschmann.knx.link.Action;
import li.pitschmann.knx.link.ChannelPacket;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class Helper {

    /**
     * Creates a mock {@link KnxClient} whereas the {@link KnxClient#readRequest(GroupAddress)}
     * and {@link KnxClient#writeRequest(GroupAddress, DataPointValue)} is simulated with {@code true}.
     *
     * @return mocked {@link KnxClient}
     */
    public static KnxClient createKnxClientMock() {
        final var knxClientMock = mock(KnxClient.class);
        when(knxClientMock.readRequest(any(GroupAddress.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(knxClientMock.writeRequest(any(GroupAddress.class), any(DataPointValue.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(knxClientMock.getStatusPool()).thenReturn(mock(KnxStatusPool.class));
        return knxClientMock;
    }

    /**
     * Creates a mock {@link ChannelPacket} with mock {@link SocketChannel}
     * and given {@code bytes} as byte array for {@link ChannelPacket#getBytes()}
     *
     * @param bytes the byte array containing data for read/write request operation
     * @return mocked {@link ChannelPacket}
     */
    public static ChannelPacket createChannelPacketMock(final byte[] bytes) {
        final var channelMock = mock(SocketChannel.class);
        when(channelMock.isConnected()).thenReturn(true);

        final var channelPacketMock = mock(ChannelPacket.class);
        when(channelPacketMock.getChannel()).thenReturn(channelMock);
        when(channelPacketMock.getBytes()).thenReturn(bytes);
        return channelPacketMock;
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
        final var bytes = new byte[500];
        int i = 0;

        // Protocol Version 1
        bytes[i++] = 0x01;
        // Action (read, write)
        bytes[i++] = action.getByte();
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

        if (action == Action.WRITE_REQUEST) {
            bytes[i++] = 0x00; // +1 for NULL termination
        }

        return Arrays.copyOf(bytes, i);
    }
}
