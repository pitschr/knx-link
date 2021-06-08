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

package li.pitschmann.knx.link.protocol.helpers;

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.datapoint.DPT1;
import li.pitschmann.knx.core.datapoint.DPT14;
import li.pitschmann.knx.core.datapoint.DPTRaw;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ProtocolHelper}
 */
class ProtocolHelperTest {

    @Test
    @DisplayName("#parseGroupAddress(byte[])")
    void test_getGroupAddress() {
        // index: 2 + 3
        assertThat(
                ProtocolHelper.parseGroupAddress(new byte[]{0x63, 0x2D})
        ).isEqualTo(GroupAddress.of(12, 3, 45));

        assertThat(
                ProtocolHelper.parseGroupAddress(new byte[]{0x26, 0x1F})
        ).isEqualTo(GroupAddress.of(4, 1567));
    }

    @Test
    @DisplayName("#parseDataPointType(byte[])")
    void test_getDataPointType() {
        // index: 4 + 5 for Data Point Type
        // index: 6 + 7 for Data Point Sub Type

        // DPT: 1.002
        assertThat(
                ProtocolHelper.parseDataPointType(new byte[]{
                        0x00, 0x01, // Type: 1
                        0x00, 0x02  // Sub Type: 2
                })
        ).isSameAs(DPT1.BOOL);

        // DPT: 14.1201
        assertThat(
                ProtocolHelper.parseDataPointType(new byte[]{
                        0x00, 0x0E,        // Type: 14
                        0x04, (byte) 0xB1  // Sub Type: 1201
                })
        ).isSameAs(DPT14.VOLUME_FLUX_LITER_PER_SECONDS);
    }

    @Test
    @DisplayName("#parseDataPointType(byte[]) - fallback because of unknown Data Point Sub Type")
    void test_getDataPointType_Fallback() {
        // index: 4 + 5 for Data Point Type
        // index: 6 + 7 for Data Point Sub Type

        // Default of DPT1 is DPT1#SWITCH (1.001)
        assertThat(
                ProtocolHelper.parseDataPointType(new byte[]{
                        0x00, 0x01, // Type: 1
                        0x04, 0x05  // Sub Type: 1029 (not exists)
                })
        ).isSameAs(DPT1.SWITCH);

        // Default of DPT14 is DPT14#ACCELERATION: 14.000
        assertThat(
                ProtocolHelper.parseDataPointType(new byte[]{
                        0x00, 0x0E,               // Type: 14
                        (byte) 0xFF, (byte) 0xFF  // Sub Type: 65535
                })
        ).isSameAs(DPT14.ACCELERATION);
    }

    @Test
    @DisplayName("#parseDataPointType(byte[]) - unsupported Data Point Type, fall back to DPT Raw")
    void test_getDataPointType_Invalid() {
        // index: 4 + 5 for Data Point Type
        // index: 6 + 7 for Data Point Sub Type

        // Non-existing / Non-supported
        assertThat(
                ProtocolHelper.parseDataPointType(new byte[]{
                        (byte) 0xFF, (byte) 0xFF, // Type: 65535
                        (byte) 0xFF, (byte) 0xFF  // Sub Type: 65535
                })
        ).isInstanceOf(DPTRaw.class);
    }

    @Test
    @DisplayName("#parseArguments(byte[]) - No Arguments")
    void test_getArgs_NoArgs() {
        assertThat(
                ProtocolHelper.parseArguments(null)
        ).isEmpty();
        assertThat(
                ProtocolHelper.parseArguments(new byte[0])
        ).isEmpty();
    }

    @Test
    @DisplayName("#parseArguments(byte[]) - One Argument")
    void test_getArguments_One() {
        // Arguments: Hello
        assertThat(
                ProtocolHelper.parseArguments(new byte[]{
                        0x48 /*H*/, 0x65 /*e*/, 0x6C /*l*/, 0x6C /*l*/, 0x6F /*o*/              // index: 1 - 5
                })
        ).containsExactly("Hello");
    }

    @Test
    @DisplayName("#parseArguments(byte[]) - Two Arguments")
    void test_getArguments_Two() {
        // Arguments: Hello äÖ!$€
        assertThat(
                ProtocolHelper.parseArguments(new byte[]{
                        0x48 /*H*/, 0x65 /*e*/, 0x6C /*l*/, 0x6C /*l*/, 0x6F, /*o*/             // index: 1 - 5

                        0x20 /*SPACE*/,                                                         // index: 6

                        (byte) 0xC3, (byte) 0xA4 /*ä*/, (byte) 0xC3, (byte) 0x96 /*Ö*/,         // index: 7 - 10
                        0x21 /*!*/, 0x24 /*$*/,                                                 // index: 11 - 12
                        (byte) 0xE2, (byte) 0x82, (byte) 0xAC /*€*/                             // index: 13 - 15
                })
        ).containsExactly("Hello", "äÖ!$€");
    }

    @Test
    @DisplayName("#parseArguments(byte[]) - Three Arguments")
    void test_getArguments_Three() {
        // Arguments: ABC "DE F" GH
        assertThat(
                ProtocolHelper.parseArguments(new byte[]{
                        0x41, 0x42, 0x43 /*ABC*/,                               // index: 1 - 3

                        0x20 /*SPACE*/,                                         // index: 4

                        0x22 /*QUOTE*/,                                         // index: 5
                        0x44, 0x45 /*DE*/, 0x20 /*SPACE*/, 0x46 /*F*/,          // index: 6 - 9
                        0x22 /*QUOTE*/,                                         // index: 10

                        0x20 /*SPACE*/,                                         // index: 11

                        0x47, 0x48 /*GH*/                                       // index: 12 - 13
                })
        ).containsExactly("ABC", "DE F", "GH");
    }

    @Test
    @DisplayName("#parseArguments(byte[]) - Arguments With Escaped Quotes")
    void test_getArguments_EscapedQuote() {
        // Arguments: "IJK" "L\"MN" "OP"
        assertThat(
                ProtocolHelper.parseArguments(new byte[]{
                        0x22 /*QUOTE*/,                                                   // index: 1
                        0x49, 0x4A, 0x4B /*IJK*/,                                         // index: 2 - 4
                        0x22 /*QUOTE*/,                                                   // index: 5

                        0x20 /*SPACE*/,                                                   // index: 6

                        0x22 /*QUOTE*/,                                                   // index: 7
                        0x4C /*L*/, 0x5C, 0x22 /*ESCAPED QUOTE*/, 0x4D /*M*/, 0x4E /*N*/, // index: 8 - 12
                        0x22 /*QUOTE*/,                                                   // index: 13

                        0x20 /*SPACE*/,                                                   // index: 14

                        0x22 /*QUOTE*/,                                                   // index: 15
                        0x4F /*O*/, 0x50 /*P*/,                                           // index: 16 - 17
                        0x22 /*QUOTE*/                                                    // index: 18
                })
        ).containsExactly("IJK", "L\"MN", "OP");
    }
}
