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

import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.protocol.Header;
import li.pitschmann.knx.link.protocol.ReadRequestBody;
import li.pitschmann.knx.link.protocol.WriteRequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Worker for Server
 */
public class ServerWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ServerWorker.class);
    private final KnxClient knxClient;

    public ServerWorker(final KnxClient knxClient) {
        this.knxClient = Objects.requireNonNull(knxClient);
    }

    private static void writeToChannel(final SocketChannel channel, final ByteBuffer bb) {
        try {
            if (channel.isOpen() && channel.isConnected()) {
                channel.write(bb);
                LOG.debug("Written to channel ({}): {}", channel, ByteFormatter.formatHex(bb.array()));
            } else {
                LOG.warn("The channel ({}) seems not be open anymore and could not respond: {}", channel, bb);
            }
        } catch (final IOException e) {
            LOG.error("I/O Exception during replying to channel: {}", channel, e);
        }
    }

    public void execute(final ChannelPacket packet) {
        final var bytes = packet.getBytes();
        Preconditions.checkArgument(bytes != null && bytes.length > 0, "Bytes is required.");

        // Currently we only have Protocol V1 - so no special strategy implementation required
        final var header = Header.of(new byte[]{bytes[0], bytes[1]});
        Preconditions.checkArgument(header.getVersion() == 0x01,
                "Bytes is not supported for this protocol implementation '{}': {}",
                getClass(), ByteFormatter.formatHexAsString(bytes));

        switch (header.getAction()) {
            case READ_REQUEST:
                actionRead(packet);
                break;
            case WRITE_REQUEST:
                actionWrite(packet);
                break;
            default:
                throw new AssertionError(); // should never happen!
        }
    }

    /**
     * Performs READ REQUEST to the KNX Client
     *
     * @param packet the channel packet
     */
    private void actionRead(final ChannelPacket packet) {
        final var bytes = Arrays.copyOfRange(packet.getBytes(), 2, packet.getBytes().length);
        final var readRequest = ReadRequestBody.of(bytes);

        final var groupAddress = readRequest.getGroupAddress();
        final var channel = packet.getChannel();

        LOG.debug("Send read request to group address: {}", groupAddress);
        knxClient.readRequest(groupAddress)
                .thenAccept(b -> {
                    final var status = b ? "SUCCESS" : "FAILED";
                    final var statusAsBytes = ByteBuffer.wrap(status.getBytes(StandardCharsets.UTF_8));
                    LOG.debug("Read Request was: {}", status);

                    writeToChannel(channel, statusAsBytes);
                })
                .thenAccept(x -> {
                    final var dpt = readRequest.getDataPointType();
                    final var dpv = knxClient.getStatusPool().getValue(groupAddress, dpt);
                    LOG.debug("Forward value of read request to channel ({}): {}", channel, dpv);

                    final var responseBytes = ByteBuffer.wrap(dpv.toByteArray());
                    writeToChannel(channel, responseBytes);
                })
                .thenAccept(x -> Sleeper.seconds(1));
    }

    /**
     * Performs WRITE REQUEST to the KNX Client
     *
     * @param packet the channel packet
     */
    private void actionWrite(final ChannelPacket packet) {
        final var bytes = Arrays.copyOfRange(packet.getBytes(), 2, packet.getBytes().length);
        final var writeRequest = WriteRequestBody.of(bytes);
        final var dpt = writeRequest.getDataPointType();
        final var dpv = dpt.of(writeRequest.getArguments());
        final var groupAddress = writeRequest.getGroupAddress();
        final var channel = packet.getChannel();
        LOG.debug("Write request: {}", writeRequest);

        knxClient.writeRequest(groupAddress, dpv)
                .thenAccept(b -> {
                    final var str = b ? "SUCCESS" : "FAILED";
                    final var strAsBytes = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));
                    writeToChannel(channel, strAsBytes);
                })
                .thenAccept(x -> Sleeper.seconds(1));
    }
}
