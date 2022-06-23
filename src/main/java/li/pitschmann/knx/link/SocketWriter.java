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

package li.pitschmann.knx.link;

import li.pitschmann.knx.link.protocol.Header;
import li.pitschmann.knx.link.protocol.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public final class SocketWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SocketWriter.class);

    private SocketWriter() {
        // NO-OP
    }

    /**
     * Creates a general message to the {@link SocketChannel} with {@link ResponseBody}
     * @param channel      the socket channel which should receive the packet; may not be null
     * @param responseBody the body of response of the packet; may not be null
     */
    public static void writeToChannel(final SocketChannel channel, final ResponseBody responseBody) {
        writeToChannel(channel, Action.GENERAL_MESSAGE, responseBody);
    }

    /**
     * Creates a message to the {@link SocketChannel} with given {@link Action}
     * and a response body
     *
     * @param channel      the socket channel which should receive the packet; may not be null
     * @param action       the type of action that should represent the packet; may not be null
     * @param responseBody the body of response of the packet; may not be null
     */
    public static void writeToChannel(final SocketChannel channel, final Action action, final ResponseBody responseBody) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(action);
        Objects.requireNonNull(responseBody);

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
}
