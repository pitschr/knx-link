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

package li.pitschmann.knx.link.protocol;

import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.core.utils.Strings;
import li.pitschmann.knx.link.Status;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Implementation for Header Response. This class is immutable
 *
 * <pre>
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             |                                                               |
 *             |                       HEADER (2 octets)                       |
 *             |                (Protocol Version, Action Type)                |
 *             |                                                               |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 * Field Names | B   r   r   r   N   N   N   N   r   r   r   r   r   r   r   r |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Message Byte 1)                (Message Byte 2)              |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Message Byte 3)                (Message Byte 4)              |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             |                    ... variable length ...                    |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Message Byte N)                (Terminated by NULL 0x00)     |
 *             +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *
 * Length:     Minimum 3 octets (2 octets for response body + 1 octet for NULL)
 * Fields:
 *             Byte 1, Bit 7       (1 bit)  :
 *                                             0 = Not Last Packet
 *                                                 More Packets to be expected (KNX Client should not close connection immediately)
 *                                             1 = Last Packet
 *                                                 No More Packets to be expected (KNX Client may close connection immediately)
 *             Byte 1, Bit 6-4     (reserved)
 *             Byte 1, Bit 3-0     (4 bits) :  Status object
 *             Byte 2              (reserved)
 *             Argument Bytes                : no bytes for 'no response message'
 *                                             N bytes for 'response message' (encoded as UTF-8 String)
 *             Terminated by Null  (1 octet) : 0x00
 *
 * </pre>
 *
 * @author PITSCHR
 */
public final class ResponseBody {
    private static final int MIN_STRUCTURE_LENGTH = 3;
    private final boolean lastPacket;
    private final Status status;
    private final String message;

    private ResponseBody(final boolean lastPacket, final Status status, final @Nullable String message) {
        this.lastPacket = lastPacket;
        this.status = Objects.requireNonNull(status);
        this.message = message == null ? "" : message;
    }

    private ResponseBody(final byte[] bytes) {
        this.lastPacket = (bytes[0] & 0x80) == 0x80;
        this.status = Status.of(bytes[0] & 0xF);
        if (bytes.length == 3) {
            this.message = "";
        } else {
            this.message = new String(Arrays.copyOfRange(bytes, 2, bytes.length - 1), StandardCharsets.UTF_8);
        }
    }

    public static ResponseBody of(final boolean lastPacket, final Status status) {
        return of(lastPacket, status, null);
    }

    public static ResponseBody of(final boolean lastPacket, final Status status, final @Nullable String message) {
        return new ResponseBody(lastPacket, status, message);
    }

    public static ResponseBody of(final byte[] bytes) {
        Preconditions.checkArgument(bytes != null && bytes.length >= MIN_STRUCTURE_LENGTH,
                "Bytes must not be null and minimum 3 bytes: {}", bytes);
        Preconditions.checkArgument(bytes[bytes.length - 1] == 0,
                "No Termination NULL?: {}", bytes);
        return new ResponseBody(bytes);
    }

    public boolean isLastPacket() {
        return lastPacket;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    private byte getByte1() {
        var byte1 = lastPacket ? (byte) 0x80 : (byte) 0x00;
        byte1 |= status.getByte();
        return byte1;
    }

    private byte getByte2() {
        return 0x00;
    }

    public byte[] getBytes() {
        final var bytes = getMessage().getBytes(StandardCharsets.UTF_8);
        final var newBytes = new byte[bytes.length + 3]; // 2 bytes from ResponseBody + 1 NULL Termination
        newBytes[0] = getByte1();
        newBytes[1] = getByte2();
        System.arraycopy(bytes, 0, newBytes, 2, bytes.length);
        return newBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseBody that = (ResponseBody) o;
        return lastPacket == that.lastPacket && status == that.status && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastPacket, status, message);
    }

    @Override
    public String toString() {
        return Strings.toStringHelper(this)
                .add("lastPacket", lastPacket)
                .add("status", status.name())
                .add("message", message)
                .toString();
    }
}