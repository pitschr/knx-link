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

import li.pitschmann.knx.core.utils.Strings;
import li.pitschmann.knx.link.Action;

import java.util.Objects;

/**
 * Implementation for Header. This class is immutable
 *
 * <pre>
 *             +-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+-7-+-6-+-5-+-4-+-3-+-2-+-1-+-0-+
 * Field Names | (Version)                     | (Action)                      |
 *             +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * Format:     (U<sub>1</sub> U<sub>1</sub> U<sub>1</sub>)
 * Length:     3 octets
 *
 * Fields:
 *             Version             (1 octet) : the version of protocol packet
 *             Action              (1 octet) : 0x00 = read request
 *                                             0x01 = write request
 *             Length              (1 octet) : the length of body packet
 * </pre>
 *
 * @author PITSCHR
 */
public final class Header {
    private final int version;
    private final Action action;
    private final int length;

    private Header(final int version, final Action action, final int length) {
        this.version = version;
        this.action = action;
        this.length = length;
    }

    public static Header of(final int version, final Action action, final int length) {
        return new Header(version, action, length);
    }

    public static Header of(final byte versionAsByte, final byte actionAsByte, final byte lengthAsByte) {
        return of(
                Byte.toUnsignedInt(versionAsByte),
                Action.of(Byte.toUnsignedInt(actionAsByte)),
                Byte.toUnsignedInt(lengthAsByte)
        );
    }

    public int getVersion() {
        return version;
    }

    public Action getAction() {
        return action;
    }

    public byte[] getBytes() {
        return new byte[]{(byte) version, action.getByte(), (byte) length};
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var header = (Header) o;
        return version == header.version && action == header.action && length == header.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, action, length);
    }

    @Override
    public String toString() {
        return Strings.toStringHelper(this)
                .add("version", version)
                .add("action", action.name())
                .add("length", length)
                .toString();
    }
}
