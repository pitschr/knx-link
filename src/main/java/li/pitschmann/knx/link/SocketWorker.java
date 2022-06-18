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
import li.pitschmann.knx.core.datapoint.value.DataPointValue;
import li.pitschmann.knx.core.exceptions.KnxException;
import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.link.protocol.Header;
import li.pitschmann.knx.link.protocol.ReadRequestBody;
import li.pitschmann.knx.link.protocol.ResponseBody;
import li.pitschmann.knx.link.protocol.WriteRequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;

/**
 * Worker for Server
 */
public final class SocketWorker {
    private static final Logger LOG = LoggerFactory.getLogger(SocketWorker.class);
    private final KnxClient knxClient;

    SocketWorker(final KnxClient knxClient) {
        this.knxClient = Objects.requireNonNull(knxClient);
    }

    private static void writeToChannel(final SocketChannel channel, final Action action, final ResponseBody responseBody) {
        final var responseBytes = responseBody.getBytes();

        // Currently we only have Protocol V1 - so no special strategy implementation required
        final var headerBytes = Header.of(1, action, responseBytes.length).getBytes();
        final var bytes = new byte[headerBytes.length + responseBytes.length];
        System.arraycopy(headerBytes, 0, bytes, 0, headerBytes.length);
        System.arraycopy(responseBytes, 0, bytes, headerBytes.length, responseBytes.length);

        if (channel.isConnected()) {
            try {
                channel.write(ByteBuffer.wrap(bytes));
            } catch (final IOException e) {
                LOG.error("I/O Exception during replying to channel: {}", channel, e);
            }
        } else {
            LOG.warn("The channel ({}) seems not be open anymore and could not respond: {}",
                    channel,
                    bytes);
        }
    }

    /**
     * Reads and executes the command sequence specified in the {@link ChannelPacket}
     *
     * @param packet the channel packet that contains data from client; may not be null
     */
    public void execute(final ChannelPacket packet) {
        final var bytes = packet.getBytes();
        Preconditions.checkArgument(bytes != null && bytes.length > 0, "Bytes is required.");

        // Currently we only have Protocol V1 - so no special strategy implementation required
        final var header = Header.of(bytes[0], bytes[1], bytes[2]);
        Preconditions.checkArgument(header.getVersion() == 0x01,
                "Protocol Version '{}' is not supported: {}", header.getVersion(), ByteFormatter.formatHexAsString(bytes));

        switch (header.getAction()) {
            case READ_REQUEST:
                actionRead(packet);
                break;
            case WRITE_REQUEST:
                actionWrite(packet);
                break;
        }
    }

    /**
     * Performs READ REQUEST to the KNX Client
     *
     * @param packet the channel packet
     */
    private void actionRead(final ChannelPacket packet) {
        final var bytes = Arrays.copyOfRange(packet.getBytes(), 3, packet.getBytes().length);
        final var readRequest = ReadRequestBody.of(bytes);
        final var groupAddress = readRequest.getGroupAddress();
        final var channel = packet.getChannel();
        LOG.debug("Send read request to group address: {}", groupAddress);

        final var action = Action.READ_RESPONSE;
        knxClient.readRequest(groupAddress)
                .thenAccept(b -> {
                    if (b) {
                        // Success Request
                        LOG.debug("Read Request success for group address: {}", groupAddress);
                        writeToChannel(channel, action, ResponseBody.of(false, Status.SUCCESS));

                        // Try to get the KNX status
                        final var value = knxClient.getStatusPool().getStatusFor(groupAddress);
                        if (value == null) {
                            var message = String.format("Could not get read data for group address: %s", groupAddress.getAddressLevel3());
                            LOG.warn(message);
                            writeToChannel(channel, action, ResponseBody.of(true, Status.ERROR_TIMEOUT, message));
                            return;
                        }

                        // KNX status received, now try to translate it to Data Point Type
                        final var dpt = readRequest.getDataPointType();
                        DataPointValue dpv = null;
                        try {
                            dpv = dpt.of(value.getData());
                        } catch (final Exception e) {
                            var message = String.format("Could not parse the read data for group address '%s' and data point type '%s': %s",
                                    groupAddress.getAddressLevel3(), dpt.getId(), ByteFormatter.formatHexAsString(value.getData()));
                            LOG.warn(message);
                            writeToChannel(channel, action, ResponseBody.of(true, Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE, message));
                            return;
                        }

                        // Translation successful
                        final var message = dpv.toText() + dpt.getUnit();
                        LOG.debug("Forward text of read request to channel ({}) from {}: {}", channel, groupAddress, message);
                        writeToChannel(channel, action, ResponseBody.of(true, Status.SUCCESS, message));
                    } else {
                        // Request failed, No Acknowledge
                        LOG.warn("Read Request failed for group address: {}", groupAddress);
                        writeToChannel(channel, action, ResponseBody.of(true, Status.ERROR_REQUEST));
                    }
                });
    }

    /**
     * Performs WRITE REQUEST to the KNX Client
     *
     * @param packet the channel packet
     */
    private void actionWrite(final ChannelPacket packet) {
        final var action = Action.WRITE_RESPONSE;

        final var bytes = Arrays.copyOfRange(packet.getBytes(), 3, packet.getBytes().length);
        final var writeRequest = WriteRequestBody.of(bytes);
        LOG.debug("Write request: {}", writeRequest);
        final var dpt = writeRequest.getDataPointType();
        final var groupAddress = writeRequest.getGroupAddress();
        final var channel = packet.getChannel();

        final DataPointValue dpv;
        try {
            dpv = dpt.of(writeRequest.getArguments());
        } catch (final KnxException ex) {
            var message = String.format("I could not understand value for group address '%s' and data point type '%s': %s",
                    groupAddress.getAddressLevel3(), dpt.getId(), Arrays.toString(writeRequest.getArguments()));
            LOG.warn(message);
            writeToChannel(channel, action, ResponseBody.of(true, Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE, message));
            return;
        }

        knxClient.writeRequest(groupAddress, dpv)
                .thenAccept(b -> {
                    if (b) {
                        writeToChannel(channel, action, ResponseBody.of(true, Status.SUCCESS));
                        LOG.debug("Write Request was successful for: {}", groupAddress);
                    } else {
                        writeToChannel(channel, action, ResponseBody.of(true, Status.ERROR_REQUEST));
                        LOG.debug("Write Request was not successful for: {}", groupAddress);
                    }
                });
    }
}
