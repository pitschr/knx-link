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

package li.pitschmann.knx.link.config;

/**
 * Returns the mode of KNX communication between
 * the server and the KNX Net/IP device.
 */
public enum KnxMode {
    TUNNELING("tunneling"),
    ROUTING("routing");

    private final String mode;

    KnxMode(final String mode) {
        this.mode = mode;
    }

    public static KnxMode of(final String mode) {
        if (TUNNELING.mode.equalsIgnoreCase(mode)) {
            return TUNNELING;
        } else if (ROUTING.mode.equalsIgnoreCase(mode)) {
            return ROUTING;
        } else {
            throw new IllegalArgumentException("Mode is not supported: " + mode);
        }
    }

    public String getMode() {
        return mode;
    }
}
