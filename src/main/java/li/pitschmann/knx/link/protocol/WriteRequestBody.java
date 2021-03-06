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

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.datapoint.DataPointType;
import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.link.protocol.helpers.ProtocolHelper;

import java.util.Arrays;

/**
 * Implementation for Write Request. This class is immutable.
 *
 * <pre>
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             |                                                               |
 *             |                       HEADER (3 octets)                       |
 *             |            (Protocol Version, Action Type, Length)            |
 *             |                                                               |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 * Field Names | (Group Address)                                               |
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
 *             |                                 (Arg Byte N)                  |
 *             +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *
 * Length:     Minimum 6 octets
 * Fields:
 *             Group Address       (2 octets): [1, 65535]
 *             Data Point Type     (2 octets): [0, 65535]
 *             Data Point Sub Type (2 octets): [0, 65535]
 *             Argument Bytes                : no bytes for 'read request'
 *                                             N bytes for 'write request' (encoded as UTF-8 String)
 * </pre>
 *
 * <p> For action <strong>write request</strong> the total length may be variable
 * but must comply with the <strong>data point type</strong>. The value is UTF-8 string
 * in a byte array and is considered to be sent as data for write request.
 *
 * @author PITSCHR
 */
public final class WriteRequestBody {
    private static final int MIN_STRUCTURE_LENGTH = 6;
    private final GroupAddress groupAddress;
    private final DataPointType dataPointType;
    private final String[] arguments;

    private WriteRequestBody(final byte[] bytes) {
        groupAddress = ProtocolHelper.parseGroupAddress(new byte[]{bytes[0], bytes[1]});
        dataPointType = ProtocolHelper.parseDataPointType(new byte[]{bytes[2], bytes[3], bytes[4], bytes[5]});
        arguments = ProtocolHelper.parseArguments(Arrays.copyOfRange(bytes, 6, bytes.length));
    }

    public static WriteRequestBody of(final byte[] bytes) {
        Preconditions.checkArgument(bytes.length >= MIN_STRUCTURE_LENGTH,
                "Wrong structure length (min): {}", ByteFormatter.formatHexAsString(bytes));
        return new WriteRequestBody(bytes);
    }

    public GroupAddress getGroupAddress() {
        return groupAddress;
    }

    public DataPointType getDataPointType() {
        return dataPointType;
    }

    public String[] getArguments() {
        return arguments.clone();
    }
}