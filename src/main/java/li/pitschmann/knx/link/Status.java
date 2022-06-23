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
 * Status for KNX Link
 */
public enum Status {
    /**
     * Success Message (General)
     */
    SUCCESS(0x00),
    /**
     * Failure Message / Unknown Error Message (General)
     */
    ERROR(0x01),
    /**
     * Error: Request Failed, No Acknowledge received from KNX Net/IP
     */
    ERROR_REQUEST(0x02),
    /**
     * Error: Timeout, No Response received from KNX Net/IP
     */
    ERROR_TIMEOUT(0x03),
    /**
     * Error: Illegal Group Address (e.g. 0/0/0)
     */
    ERROR_GROUP_ADDRESS(0x04),
    /**
     * Error: Incompatible Data Point Type
     * (e.g. byte array cannot be translated into String for given Data Point Type)
     * (e.g. string cannot be translated into byte array for given Data Point Type)
     */
    ERROR_INCOMPATIBLE_DATA_POINT_TYPE(0x05),
    /**
     * Error: The client is not authorized
     */
    ERROR_CLIENT_NOT_AUTHORIZED(0x06);

    private final int code;

    Status(final int code) {
        this.code = code;
    }

    public static Status of(final int code) {
        return Arrays.stream(values())
                .filter(x -> x.code == code)
                .findFirst()
                .orElseThrow(() -> new KnxEnumNotFoundException(Status.class, code));
    }

    public byte getByte() {
        return (byte) (code % 0xFF);
    }
}
