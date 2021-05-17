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
        final var body = ResponseBody.of(false, Status.SUCCESS);

        assertThat(body.isLastPacket()).isFalse();
        assertThat(body.getStatus()).isSameAs(Status.SUCCESS);
        assertThat(body.getData()).isEmpty();
        assertThat(body.getBytes()).containsExactly(0x00, 0x00);

        assertThat(body).hasToString("ResponseBody{lastPacket=false, status=SUCCESS, data=, data(String)=}");
    }

    @Test
    @DisplayName("Success: Last Packet, Data as ASCII String = 'Hello'")
    void test_Success_LastPacket_ASCII() {
        final var body = ResponseBody.of(true, Status.SUCCESS, "Hello");

        assertThat(body.isLastPacket()).isTrue();
        assertThat(body.getStatus()).isSameAs(Status.SUCCESS);
        assertThat(body.getData()).containsExactly('H', 'e', 'l', 'l', 'o');
        assertThat(body.getBytes()).containsExactly(0x80, 0x00, 'H', 'e', 'l', 'l', 'o');

        assertThat(body).hasToString(
                "ResponseBody{" +
                        "lastPacket=true, " +
                        "status=SUCCESS, " +
                        "data=0x48 65 6C 6C 6F, " +
                        "data(String)=Hello" +
                        "}");
    }

    @Test
    @DisplayName("Error: Not Last Packet, Data as UTF-8 String = 'ä漢и'")
    void test_Success_LastPacket_UTF8() {
        final var body = ResponseBody.of(true, Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE, "ä漢и");

        assertThat(body.isLastPacket()).isTrue();
        assertThat(body.getStatus()).isSameAs(Status.ERROR_INCOMPATIBLE_DATA_POINT_TYPE);
        assertThat(body.getData()).containsExactly(
                0xC3, 0xA4,         // ä
                0xE6, 0xBC, 0xA2,   // 漢
                0xD0, 0xB8          // cryllic N
        );
        assertThat(body.getBytes()).containsExactly(
                0x85,               // Last Packet + Error
                0x00,               // (Reserved)
                0xC3, 0xA4,         // ä
                0xE6, 0xBC, 0xA2,   // 漢
                0xD0, 0xB8          // cryllic N
        );

        assertThat(body).hasToString(
                "ResponseBody{" +
                        "lastPacket=true, " +
                        "status=ERROR_INCOMPATIBLE_DATA_POINT_TYPE, " +
                        "data=0xC3 A4 E6 BC A2 D0 B8, " +
                        "data(String)=ä漢и" +
                        "}");
    }

    @Test
    @DisplayName("Error: Not Last Packet, Data as byte array")
    void test_Success_LastPacket_ByteArray() {
        final var body = ResponseBody.of(false, Status.ERROR_TIMEOUT, new byte[]{(byte) 0x81, (byte) 0x83, (byte) 0x85});

        assertThat(body.isLastPacket()).isFalse();
        assertThat(body.getStatus()).isSameAs(Status.ERROR_TIMEOUT);
        assertThat(body.getData()).containsExactly(0x81, 0x83, 0x85);
        assertThat(body.getBytes()).containsExactly(
                0x03,               // Last Packet + Error
                0x00,               // (Reserved)
                0x81, 0x83, 0x85    // Byte Array
        );

        assertThat(body).hasToString(
                "ResponseBody{" +
                        "lastPacket=false, " +
                        "status=ERROR_TIMEOUT, " +
                        "data=0x81 83 85, " +
                        "data(String)=���" +
                        "}");
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
