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

/**
 * Implementation for Read Request. This class is immutable.
 *
 * <pre>
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             |                                                               |
 *             |                       HEADER (2 octets)                       |
 *             |                (Protocol Version, Action Type)                |
 *             |                                                               |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 * Field Names | (Group Address)                                               |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Data Point Type)                                             |
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 *             | (Data Point Sub Type)                                         |
 *             +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *
 * Length:     6 octets
 * Fields:
 *             Group Address       (2 octets): [1, 65535]
 *             Data Point Type     (2 octets): [0, 65535]
 *             Data Point Sub Type (2 octets): [0, 65535]
 * </pre>
 *
 * <p> For action <strong>read request (0x00)</strong> the total length of 6 octets
 * is expected. If the length differs, then packet is considered as corrupted and
 * shall be dropped with a warning.
 *
 * @author PITSCHR
 */
public final class ReadRequestBody {
    private static final int STRUCTURE_LENGTH = 6;
    private final GroupAddress groupAddress;
    private final DataPointType dataPointType;

    private ReadRequestBody(final byte[] bytes) {
        groupAddress = ProtocolHelper.parseGroupAddress(new byte[]{bytes[0], bytes[1]});
        dataPointType = ProtocolHelper.parseDataPointType(new byte[]{bytes[2], bytes[3], bytes[4], bytes[5]});
    }

    public static ReadRequestBody of(final byte[] bytes) {
        Preconditions.checkArgument(bytes.length == STRUCTURE_LENGTH,
                "Wrong structure length: {}", ByteFormatter.formatHexAsString(bytes));
        return new ReadRequestBody(bytes);
    }

    public GroupAddress getGroupAddress() {
        return groupAddress;
    }

    public DataPointType getDataPointType() {
        return dataPointType;
    }
}
