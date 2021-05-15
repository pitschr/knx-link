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

import li.pitschmann.knx.link.Status;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ResponseBody}
 */
class ResponseBodyTest {
    @Test
    @DisplayName("Success: Not Last Packet, No Message")
    void test_Success_NotLastPacket_NoMessage() {
        final var body = ResponseBody.of(false, Status.SUCCESS, null);

        assertThat(body.isLastPacket()).isFalse();
        assertThat(body.getStatus()).isSameAs(Status.SUCCESS);
        assertThat(body.getMessage()).isEmpty();
        assertThat(body.getBytes()).containsExactly(0x00, 0x00);

        assertThat(body).hasToString("ResponseBody{lastPacket=false, status=SUCCESS, message=}");
    }

    @Test
    @DisplayName("Success: Last Packet, Message = 'Hello'")
    void test_Success_LastPacket_MessageAscii() {
        final var body = ResponseBody.of(true, Status.SUCCESS, "Hello");

        assertThat(body.isLastPacket()).isTrue();
        assertThat(body.getStatus()).isSameAs(Status.SUCCESS);
        assertThat(body.getMessage()).isEqualTo("Hello");
        assertThat(body.getBytes()).containsExactly(0x80, 0x00, 'H', 'e', 'l', 'l', 'o');

        assertThat(body).hasToString("ResponseBody{lastPacket=true, status=SUCCESS, message=Hello}");
    }

    @Test
    @DisplayName("Error: Not Last Packet, Message = 'ä漢и'")
    void test_Success_LastPacket_MessageUTF8() {
        final var body = ResponseBody.of(true, Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE, "ä漢и");

        assertThat(body.isLastPacket()).isTrue();
        assertThat(body.getStatus()).isSameAs(Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE);
        assertThat(body.getMessage()).isEqualTo("ä漢и");
        assertThat(body.getBytes()).containsExactly(
                0x85,               // Last Packet + Error
                0x00,               // (Reserved)
                0xC3, 0xA4,         // ä
                0xE6, 0xBC, 0xA2,   // 漢
                0xD0, 0xB8          // cryllic N
        );

        assertThat(body).hasToString("ResponseBody{lastPacket=true, status=ERROR_INCOMPATIBLE_DATA_POINT_TYPE, message=ä漢и}");
    }

    @Test
    @DisplayName("#(bytes) with wrong length")
    void test_Bytes_Ctor_Wrong_Length() {
        assertThatThrownBy(() -> ResponseBody.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bytes must not be null and minimum 2 bytes: <null>");
        assertThatThrownBy(() -> ResponseBody.of(new byte[1]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bytes must not be null and minimum 2 bytes: 0x00");
    }

    @Test
    @DisplayName("#equals() and #hashCode()")
    void testEqualsAndHashCode() {
        EqualsVerifier.forClass(ResponseBody.class).verify();
    }
}
