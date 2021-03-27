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

import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.link.Action;

/**
 * Implementation for Header. This class is immutable
 *
 * <pre>
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 * Field Names | (Version)                     | (Action)                      |
 *             +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * Format:     (U<sub>1</sub> U<sub>1</sub>)
 * Length:     2 octets
 *
 * Fields:
 *             Version             (1 octet) : 0x01
 *             Action              (1 octet) : 0x00 = read request
 *                                             0x01 = write request
 * </pre>
 *
 * @author PITSCHR
 */
public final class Header {
    public static final int STRUCTURE_LENGTH = 2;
    private final int version;
    private final Action action;

    private Header(final byte versionAsByte, final byte actionAsByte) {
        version = Byte.toUnsignedInt(versionAsByte);
        action = parseAction(actionAsByte);
    }

    public static Header of(final byte[] bytes) {
        Preconditions.checkArgument(bytes.length == STRUCTURE_LENGTH);
        return new Header(bytes[0], bytes[1]);
    }

    private Action parseAction(final byte actionAsByte) {
        final var commandInt = Byte.toUnsignedInt(actionAsByte);
        return Action.values()[commandInt];
    }

    public int getVersion() {
        return version;
    }

    public Action getAction() {
        return action;
    }
}
