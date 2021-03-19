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

package li.pitschmann.knx.link.v1;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.datapoint.DPT1;
import li.pitschmann.knx.core.datapoint.DPT14;
import li.pitschmann.knx.core.exceptions.KnxDataPointTypeNotFoundException;
import li.pitschmann.knx.link.protocol.v1.PacketReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link PacketReader}
 */
class PacketReaderTest {

    @Test
    @DisplayName("#getVersion(byte[])")
    void test_getVersion() {
        var protocol = new PacketReader();

        // index: 0
        assertThat(protocol.getVersion(new byte[]{0x01})).isEqualTo(1);
        assertThat(protocol.getVersion(new byte[]{0x03})).isEqualTo(3);
    }

    @Test
    @DisplayName("#getAction(byte[])")
    void test_getAction() {
        var protocol = new PacketReader();

        // index: 1
        assertThat(protocol.getAction(new byte[]{0x00, 0x00})).isSameAs(PacketReader.Action.READ_REQUEST);
        assertThat(protocol.getAction(new byte[]{0x00, 0x01})).isSameAs(PacketReader.Action.WRITE_REQUEST);
    }

    @Test
    @DisplayName("#getGroupAddress(byte[])")
    void test_getGroupAddress() {
        var protocol = new PacketReader();

        // index: 2 + 3
        assertThat(
                protocol.getGroupAddress(new byte[]{0x00, 0x00, 0x63, 0x2D})
        ).isEqualTo(GroupAddress.of(12, 3, 45));

        assertThat(
                protocol.getGroupAddress(new byte[]{0x00, 0x00, 0x26, 0x1F})
        ).isEqualTo(GroupAddress.of(4, 1567));
    }

    @Test
    @DisplayName("#getDataPointType(byte[])")
    void test_getDataPointType() {
        var protocol = new PacketReader();

        // index: 4 + 5 for Data Point Type
        // index: 6 + 7 for Data Point Sub Type

        // DPT: 1.002
        assertThat(
                protocol.getDataPointType(new byte[]{0x00, 0x00, 0x00, 0x00,
                        0x00, 0x01, // Type: 1
                        0x00, 0x02  // Sub Type: 2
                })
        ).isSameAs(DPT1.BOOL);

        // DPT: 14.1201
        assertThat(
                protocol.getDataPointType(new byte[]{0x00, 0x00, 0x00, 0x00,
                        0x00, 0x0E,        // Type: 14
                        0x04, (byte) 0xB1  // Sub Type: 1201
                })
        ).isSameAs(DPT14.VOLUME_FLUX_LITER_PER_SECONDS);
    }

    @Test
    @DisplayName("#getDataPointType(byte[]) - fallback because of unknown Data Point Sub Type")
    void test_getDataPointType_Fallback() {
        var protocol = new PacketReader();

        // index: 4 + 5 for Data Point Type
        // index: 6 + 7 for Data Point Sub Type

        // Default of DPT1 is DPT1#SWITCH (1.001)
        assertThat(
                protocol.getDataPointType(new byte[]{0x00, 0x00, 0x00, 0x00,
                        0x00, 0x01, // Type: 1
                        0x04, 0x05  // Sub Type: 1029 (not exists)
                })
        ).isSameAs(DPT1.SWITCH);

        // Default of DPT14 is DPT14#ACCELERATION: 14.000
        assertThat(
                protocol.getDataPointType(new byte[]{0x00, 0x00, 0x00, 0x00,
                        0x00, 0x0E,               // Type: 14
                        (byte) 0xFF, (byte) 0xFF  // Sub Type: 65535
                })
        ).isSameAs(DPT14.ACCELERATION);
    }

    @Test
    @DisplayName("#getDataPointType(byte[]) - invalid/unsupported Data Point Type")
    void test_getDataPointType_Invalid() {
        var protocol = new PacketReader();

        // index: 4 + 5 for Data Point Type
        // index: 6 + 7 for Data Point Sub Type

        // Non-existing / Non-supported
        assertThatThrownBy(() ->
                protocol.getDataPointType(new byte[]{0x00, 0x00, 0x00, 0x00,
                        (byte) 0xFF, (byte) 0xFF, // Type: 65535
                        (byte) 0xFF, (byte) 0xFF  // Sub Type: 65535
                })
        ).isInstanceOf(KnxDataPointTypeNotFoundException.class);
    }

    @Test
    @DisplayName("#getArgs(byte[]) - No Arguments")
    void test_getArgs_NoArgs() {
        var protocol = new PacketReader();

        // index: 8 - terminated NULL
        assertThat(
                protocol.getArgs(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                        0x00 // Termination NULL
                })
        ).isEmpty();
    }

    @Test
    @DisplayName("#getArgs(byte[]) - No Arguments")
    void test_getArgs_NoTermination() {
        var protocol = new PacketReader();

        assertThatThrownBy(() ->
                protocol.getArgs(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01
                        // No Termination NULL
                })
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No NULL termination?: 0x00 00 00 00 00 01 00 01");
    }

    @Test
    @DisplayName("#getArgs(byte[]) - One Argument")
    void test_getArgs_OneArg() {
        var protocol = new PacketReader();

        // Argument: Hello  (0x48 0x65 0x6C 0x6C 0x6F)
        // index: 8 - 12 for text
        // index: 13 for terminated NULL
        assertThat(
                protocol.getArgs(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                        0x48 /*H*/, 0x65 /*e*/, 0x6C /*l*/, 0x6C /*l*/, 0x6F, /*o*/
                        0x00 // Termination NULL
                })
        ).containsExactly("Hello");
    }

    @Test
    @DisplayName("#getArgs(byte[]) - Two Arguments")
    void test_getArgs_TwoArgs() {
        var protocol = new PacketReader();

        // String: Hello äÖ!$€
        // 1st Argument: Hello  (0x48 0x65 0x6C 0x6C 0x6F)
        // 2nd Argument: äÖ!$€  (0xC3 0xA4 0xC3 0x96 0x21 0x24 0xE2 0x82 0xAC)
        // index: 8 - 12 for "Hello"
        // index: 13 for space
        // index: 14 - 22 for "äÖ!$€"
        // index: 23 for terminated NULL
        assertThat(
                protocol.getArgs(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                        0x48 /*H*/, 0x65 /*e*/, 0x6C /*l*/, 0x6C /*l*/, 0x6F, /*o*/
                        0x20 /*SPACE*/,
                        (byte) 0xC3, (byte) 0xA4 /*ä*/, (byte) 0xC3, (byte) 0x96 /*Ö*/,
                        0x21 /*!*/, 0x24 /*$*/,
                        (byte) 0xE2, (byte) 0x82, (byte) 0xAC /*€*/,
                        0x00 // Termination NULL
                })
        ).containsExactly("Hello", "äÖ!$€");
    }

    @Test
    @DisplayName("#getArgs(byte[]) - Three Arguments")
    void test_getArgs_ThreeArgs() {
        var protocol = new PacketReader();

        // String: ABC "DE F" GH
        // 1st Argument: ABC    (0x41 0x42 0x43), index: 8 - 10
        // Space, index: 11
        // Quote, index: 12
        // 2nd Argument: DE F  (0x44 0x45 0x20 0x46), index: 13 - 16
        // Quote, index: 17
        // Space, index: 18
        // 3rd Argument: GH     (0x47 0x48), index: 19 - 20
        // Terminated NULL, index: 21
        assertThat(
                protocol.getArgs(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                        0x41, 0x42, 0x43 /*ABC*/,
                        0x20 /*SPACE*/,
                        0x22 /*QUOTE*/,
                        0x44, 0x45 /*DE*/, 0x20 /*SPACE*/, 0x46 /*F*/,
                        0x22 /*QUOTE*/,
                        0x20 /*SPACE*/,
                        0x47, 0x48 /*GH*/,
                        0x00 /*NULL*/
                })
        ).containsExactly("ABC", "DE F", "GH");
    }

    @Test
    @DisplayName("#getArgs(byte[]) - Arguments With Escaped Quotes")
    void test_getArgs_EscapedQuote() {
        var protocol = new PacketReader();

        // Argument String: "IJK" "L\"MN" "OP"
        assertThat(
                protocol.getArgs(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                        0x22 /*QUOTE*/,                                                                     // index: 8
                        0x49, 0x4A, 0x4B /*IJK*/,                                                           // index: 9 - 11
                        0x22 /*QUOTE*/,                                                                     // index: 12

                        0x20 /*SPACE*/,                                                                     // index: 13

                        0x22 /*QUOTE*/,                                                                     // index: 14
                        0x4C /*L*/, 0x5C, 0x22 /*ESCAPED QUOTE*/, 0x4D /*M*/, 0x4E /*N*/,                   // index: 15 - 19
                        0x22 /*QUOTE*/,                                                                     // index: 20

                        0x20 /*SPACE*/,                                                                     // index: 21

                        0x22 /*QUOTE*/,                                                                     // index: 22
                        0x4F /*O*/, 0x50 /*P*/,                                                             // index: 23 - 24
                        0x22 /*QUOTE*/,                                                                     // index: 25

                        0x00 /*NULL*/                                                                       // index: 26
                })
        ).containsExactly("IJK", "L\"MN", "OP");
    }
}
