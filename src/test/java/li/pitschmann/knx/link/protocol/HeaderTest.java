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
    void test_ReadRequest() {
        final var header = Header.of(1, Action.READ_REQUEST, 13);
        final var header2 = Header.of((byte) 0x01, (byte) 0x00, (byte) 0x0D);

        assertThat(header.getVersion()).isEqualTo(1);
        assertThat(header.getAction()).isSameAs(Action.READ_REQUEST);
        assertThat(header.getLength()).isSameAs(13);

        assertThat(header).isEqualTo(header2);
        assertThat(header).hasSameHashCodeAs(header2);

        assertThat(header).hasToString("Header{version=1, action=READ_REQUEST, length=13}");
    }

    @Test
    @DisplayName("#of(int, Action) and #of(byte, byte): Version=7, Action=WRITE_REQUEST, Length=11")
    void test_WriteRequest() {
        final var header = Header.of(7, Action.WRITE_REQUEST, 17);
        final var header2 = Header.of((byte) 0x07, (byte) 0x01, (byte) 0x11);

        assertThat(header.getVersion()).isEqualTo(7);
        assertThat(header.getAction()).isSameAs(Action.WRITE_REQUEST);
        assertThat(header.getLength()).isSameAs(17);

        assertThat(header).isEqualTo(header2);
        assertThat(header).hasSameHashCodeAs(header2);

        assertThat(header).hasToString("Header{version=7, action=WRITE_REQUEST, length=17}");
    }

    @Test
    @DisplayName("#equals() and #hashCode()")
    void testEqualsAndHashCode() {
        EqualsVerifier.forClass(Header.class).verify();
    }
}
