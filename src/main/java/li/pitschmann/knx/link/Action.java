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

import li.pitschmann.knx.core.exceptions.KnxEnumNotFoundException;

import java.util.Arrays;

/**
 * General Action for packet
 */
public enum Action {
    /**
     * READ Request. Here we will send a READ Request to KNX Net/IP device.
     * See: {@link li.pitschmann.knx.link.protocol.ReadRequestBody}
     */
    READ_REQUEST(0x00),
    /**
     * WRITE Request. Here we will send a WRITE Request to KNX Net/IP device.
     * See: {@link li.pitschmann.knx.link.protocol.WriteRequestBody}
     */
    WRITE_REQUEST(0x01);

    private final int code;

    Action(final int code) {
        this.code = code;
    }

    public static Action of(final int code) {
        return Arrays.stream(values())
                .filter(x -> x.code == code)
                .findFirst()
                .orElseThrow(() -> new KnxEnumNotFoundException(Action.class, code));
    }

    public byte getByte() {
        return (byte) (code % 0xFF);
    }
}
