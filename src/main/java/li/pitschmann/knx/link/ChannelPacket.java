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

import java.nio.channels.SocketChannel;
import java.util.Objects;

/**
 * Immutable implementation of bytes arrived from a channel
 *
 * <p> We keep the channel which allows us to respond something back to the channel
 *
 * @author PITSCHR
 */
public final class ChannelPacket {

    private final SocketChannel channel;
    private final byte[] bytes;

    ChannelPacket(final SocketChannel channel, final byte[] bytes) {
        this.channel = Objects.requireNonNull(channel);
        this.bytes = bytes.clone();
    }

    /**
     * The source channel where we got the byte array packet
     *
     * @return the {@link SocketChannel}
     */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
     * Byte array that were arrived at the {@link SocketChannel}
     *
     * @return byte array, defensively copied
     */
    public byte[] getBytes() {
        return bytes.clone();
    }
}
