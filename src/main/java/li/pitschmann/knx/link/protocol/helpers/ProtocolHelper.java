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

package li.pitschmann.knx.link.protocol.helpers;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.datapoint.DPTRaw;
import li.pitschmann.knx.core.datapoint.DataPointRegistry;
import li.pitschmann.knx.core.datapoint.DataPointType;
import li.pitschmann.knx.core.exceptions.KnxDataPointTypeNotFoundException;

/**
 * Helper for Protocol
 *
 * @author PITSCHR
 */
public final class ProtocolHelper {
    private ProtocolHelper() {
        throw new AssertionError("Do not touch me!");
    }

    /**
     * Parses the group address from two byte array.
     *
     * @param bytes byte array to be parsed
     * @return new {@link GroupAddress}
     */
    public static GroupAddress parseGroupAddress(final byte[] bytes) {
        return GroupAddress.of(
                (Byte.toUnsignedInt(bytes[0]) << 8) | Byte.toUnsignedInt(bytes[1])
        );
    }

    /**
     * Parses for the data point type from four byte array.
     * <p> The data point type is on 1st and 2nd index.
     * <p> The sub type of data point is on 3rd and 4th index.
     *
     * @param bytes byte array to be parsed
     * @return a new {@link DataPointType}, {@link DPTRaw} if no suitable could be found
     */
    public static DataPointType parseDataPointType(final byte[] bytes) {
        final var dptInt = (Byte.toUnsignedInt(bytes[0]) << 8) | Byte.toUnsignedInt(bytes[1]);
        final var dpstInt = (Byte.toUnsignedInt(bytes[2]) << 8) | Byte.toUnsignedInt(bytes[3]);

        try {
            // first try with Data Point Sub Type - this allows us to use finer values like
            // "on", "off", "true", "false", ...
            return DataPointRegistry.getDataPointType("dpst-" + dptInt + "-" + dpstInt);
        } catch (final KnxDataPointTypeNotFoundException e) {
            // second try with Data Point Type
            try {
                return DataPointRegistry.getDataPointType("dpt-" + dptInt);
            } catch (final KnxDataPointTypeNotFoundException ee) {
                return DPTRaw.VALUE;
            }
        }
    }

    /**
     * Parses the arguments from byte array for e.g. write request..
     *
     * <p> If the {@code bytes} is {@code null} or length of {@code bytes} is {@code 0},
     * then it is supposed that no argument is present, therefore, an empty byte array is returned.
     *
     * @param bytes byte array to be parsed
     * @return new array of String as arguments; if none, then empty array
     */
    public static String[] parseArguments(final @Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new String[0];
        } else {
            return ArgumentHelper.toList(bytes).toArray(new String[0]);
        }
    }

}
