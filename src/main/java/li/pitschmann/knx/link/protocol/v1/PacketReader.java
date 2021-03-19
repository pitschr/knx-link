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

package li.pitschmann.knx.link.protocol.v1;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.datapoint.DataPointRegistry;
import li.pitschmann.knx.core.datapoint.DataPointType;
import li.pitschmann.knx.core.exceptions.KnxDataPointTypeNotFoundException;
import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.link.ArgumentParser;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Implementation for Protocol Version 1.
 *
 * <pre>
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 * Field Names | (Version)                     | (Action)                      |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Group Address)                                               |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Data Point Type)                                             |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Data Point Sub Type)                                         |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Arg Byte 1)                    (Arg Byte 2)                  |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Arg Byte 3)                    (Arg Byte 4)                  |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             |                    ... variable length ...                    |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Arg Byte N)                    (Terminated by NULL 0x00)     |
 *             +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * Format:     (U<sub>1</sub> U<sub>1</sub> U<sub>2</sub> U<sub>2</sub> U<sub>2</sub> U<sub>N</sub> 0x00)
 * Length:     minimum 9 octets incl. termination byte
 *             maximum 255 octets incl. termination byte
 * Fields:
 *             Version             (1 octet) : 0x01
 *             Action              (1 octet) : 0x00 = read request
 *                                             0x01 = write request
 *             Group Address       (2 octets): [1, 65535]
 *             Data Point Type     (2 octets): [0, 65535]
 *             Data Point Sub Type (2 octets): [0, 65535]
 *             Argument Bytes                : no bytes for 'read request'
 *                                             N bytes for 'write request' (encoded as UTF-8 String)
 *             Terminated by Null  (1 octet) : 0x00
 * </pre>
 *
 * <p> For action <strong>read request (0x00)</strong> the total length of 9 octets
 * is expected. If the length differs, then packet is considered to be dropped.
 *
 * <p> For action <strong>write request (0x00)</strong> the total length may be variable
 * but must comply with the <strong>data point type</strong>. The value is UTF-8 string
 * in a byte array and is considered to be sent as data for write request.
 *
 * <p> Other commands are not considered yet
 *
 * <p> The Argument Bytes may contain e.g. {@code 123 "456 78" 90} which provides a
 * string array with a length of 3: {@code new String[]{"123", "456 78", "90"} }
 *
 * @author PITSCHR
 */
public final class PacketReader {
    /**
     * The version of protocol implementation
     */
    public static final int PROTOCOL_VERSION = 1;

    /**
     * Parses the version of byte array
     *
     * @param bytes byte array to be parsed
     * @return the version as integer
     */
    public int getVersion(final byte[] bytes) {
        return Byte.toUnsignedInt(bytes[0]);
    }

    /**
     * Returns the command. Basd on the command number
     *
     * @param bytes byte array to be parsed
     * @return the {@link Action}
     */
    public Action getAction(final byte[] bytes) {
        final var commandInt = Byte.toUnsignedInt(bytes[1]);
        return Action.values()[commandInt];
    }

    /**
     * Parses the group address from byte array.
     * The group address is on 2nd and 3rd byte index.
     *
     * @param bytes byte array to be parsed
     * @return new {@link GroupAddress}
     */
    public GroupAddress getGroupAddress(final byte[] bytes) {
        return GroupAddress.of(
                (Byte.toUnsignedInt(bytes[2]) << 8) | Byte.toUnsignedInt(bytes[3])
        );
    }

    /**
     * Parses for the data point type from byte array.
     * <p> The data point type is on 4th and 5th index.
     * <p> The sub type of data point is on 6th and 7th index.
     *
     * @param bytes byte array to be parsed
     * @return a new {@link DataPointType}
     * @throws KnxDataPointTypeNotFoundException if no suitable data point type could be found
     */
    public DataPointType getDataPointType(final byte[] bytes) {
        final var dptInt = (Byte.toUnsignedInt(bytes[4]) << 8) | Byte.toUnsignedInt(bytes[5]);
        final var dpstInt = (Byte.toUnsignedInt(bytes[6]) << 8) | Byte.toUnsignedInt(bytes[7]);

        try {
            // first try with Data Point Sub Type - this allows us to
            return DataPointRegistry.getDataPointType("dpst-" + dptInt + "-" + dpstInt);
        } catch (final KnxDataPointTypeNotFoundException e) {
            // second try with Data Point Type
            return DataPointRegistry.getDataPointType("dpt-" + dptInt);
        }
    }

    /**
     * Parses the arguments from byte array for e.g. write request..
     *
     * <p> If the length of {@code bytes} is 9, then it is supposed that no
     * argument is present, therefore, an empty byte array is returned.
     *
     * @param bytes byte array to be parsed
     * @return new array of String as arguments; if none, then empty array
     */
    public String[] getArgs(final byte[] bytes) {
        Preconditions.checkArgument(bytes[bytes.length - 1] == 0x00,
                "No NULL termination?: {}",
                ByteFormatter.formatHexAsString(bytes));

        if (bytes.length == 9) {
            return new String[0];
        } else {
            final var valuesAsArray = Arrays.copyOfRange(bytes, 8, bytes.length - 1);
            return ArgumentParser.toList(valuesAsArray).toArray(new String[0]);
        }
    }

    public enum Action {
        READ_REQUEST, // 0x00
        WRITE_REQUEST // 0x01
    }
}
