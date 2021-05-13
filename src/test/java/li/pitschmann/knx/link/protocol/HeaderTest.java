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

import li.pitschmann.knx.link.Action;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Header}
 */
class HeaderTest {
    @Test
    @DisplayName("#of(int, Action) and #of(byte, byte): Version=1, Action=READ_REQUEST")
    void test_Version1_ReadRequest() {
        final var header = Header.of(1, Action.READ_REQUEST);
        final var header2 = Header.of((byte) 0x01, (byte) 0x00);

        assertThat(header.getVersion()).isEqualTo(1);
        assertThat(header.getAction()).isSameAs(Action.READ_REQUEST);

        assertThat(header).isEqualTo(header2);
        assertThat(header).hasSameHashCodeAs(header2);

        assertThat(header).hasToString("Header{version=1, action=READ_REQUEST}");
    }

    @Test
    @DisplayName("#of(int, Action) and #of(byte, byte): Version=7, Action=WRITE_REQUEST")
    void test_Version7_WriteRequest() {
        final var header = Header.of(7, Action.WRITE_REQUEST);
        final var header2 = Header.of((byte) 0x07, (byte) 0x01);

        assertThat(header.getVersion()).isEqualTo(7);
        assertThat(header.getAction()).isSameAs(Action.WRITE_REQUEST);

        assertThat(header).isEqualTo(header2);
        assertThat(header).hasSameHashCodeAs(header2);

        assertThat(header).hasToString("Header{version=7, action=WRITE_REQUEST}");
    }

    @Test
    @DisplayName("#equals() and #hashCode()")
    void testEqualsAndHashCode() {
        EqualsVerifier.forClass(Header.class).verify();
    }
}
