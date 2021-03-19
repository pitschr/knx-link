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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ArgumentParser}
 */
class ArgumentParserTest {

    @Test
    @DisplayName("Null bytes")
    void test_NullBytes() {
        assertThat(ArgumentParser.toList(null)).isEmpty();
    }

    @Test
    @DisplayName("Empty bytes")
    void test_EmptyBytes() {
        assertThat(ArgumentParser.toList(new byte[0])).isEmpty();
    }

    @Test
    @DisplayName("One Argument: ASCII")
    void test_OneArg_Ascii() {
        assertThat(ArgumentParser.toList(
                new byte[]{
                        0x41, 0x42, 0x43, /*ABC*/
                        0x31, 0x32, 0x33  /*123*/
                }
        )).containsExactly("ABC123");
    }

    @Test
    @DisplayName("One Argument: ISO-8859-1")
    void test_OneArg_ISO_8859_1() {
        assertThat(ArgumentParser.toList(
                new byte[]{
                        (byte) 0xC3, (byte) 0xA4, (byte) 0xC3, (byte) 0xB6, (byte) 0xC3, (byte) 0xBC, /*äöü*/
                        (byte) 0xC3, (byte) 0x84, (byte) 0xC3, (byte) 0x96, (byte) 0xC3, (byte) 0x9C  /*ÄÖÜ*/
                }
        )).containsExactly("äöüÄÖÜ");
    }

    @Test
    @DisplayName("One Argument: Russian")
    void test_OneArg_Russian() {
        assertThat(ArgumentParser.toList(
                new byte[]{
                        (byte) 0xD0, (byte) 0x9B /*Л*/,
                        (byte) 0xD0, (byte) 0xBE /*о*/,
                        (byte) 0xD1, (byte) 0x80 /*р*/,
                        (byte) 0xD0, (byte) 0xB5 /*е*/,
                        (byte) 0xD0, (byte) 0xBC /*м*/
                }
        )).containsExactly("Лорем");
    }

    @Test
    @DisplayName("One Argument: Chinese")
    void test_OneArg_Chinese() {
        assertThat(ArgumentParser.toList(
                new byte[]{
                        (byte) 0xE5, (byte) 0xBF, (byte) 0x85 /*必*/,
                        (byte) 0xE8, (byte) 0xBB, (byte) 0xBD /*軽*/,
                        (byte) 0xE9, (byte) 0x87, (byte) 0x8E /*野*/,
                        (byte) 0xE3, (byte) 0x80, (byte) 0x82 /*。*/
                }
        )).containsExactly("必軽野。");
    }

    @Test
    @DisplayName("Arguments without quotes")
    void test_Args_WithoutQuotes() {
        // ABC DEF GHI
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC DEF GHI"
        ))).containsExactly("ABC", "DEF", "GHI");
    }

    @Test
    @DisplayName("Arguments within quotes only")
    void test_Args_WithQuotes() {
        // "ABC DEF" "GHI JKL" "MNO PQR"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC DEF\" \"GHI JKL\" \"MNO PQR\""
        ))).containsExactly("ABC DEF", "GHI JKL", "MNO PQR");
    }

    @Test
    @DisplayName("Arguments with and without quotes")
    void test_Args_WithAndWithoutQuotes() {
        // ABC DEF "GHI JKL" MNO PQR
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC DEF \"GHI JKL\" MNO PQR"
        ))).containsExactly("ABC", "DEF", "GHI JKL", "MNO", "PQR");

        // "ABC DEF" GHI "JKL MNO"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC DEF\" GHI \"JKL MNO\""
        ))).containsExactly("ABC DEF", "GHI", "JKL MNO");
    }

    @Test
    @DisplayName("Arguments separated by different whitespaces")
    void test_DifferentWhitespaces() {
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC abc\t123\r456"
        ))).containsExactly("ABC", "abc", "123", "456");
    }

    @Test
    @DisplayName("Arguments with repeated whitespaces without quotes")
    void test_RepeatedWhitespaces_NoQuotes() {
        // ABC    abc\t \t \t123 \t\r456
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC    abc\t \t \t123 \t\r456"
        ))).containsExactly("ABC", "abc", "123", "456");
    }

    @Test
    @DisplayName("Arguments with multiple whitespaces within quotes")
    void test_RepeatedWhitespaces_WithinQuotes() {
        // ABC "abc def  ghi" 123 "456    789"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC \"abc def  ghi\" 123 \"456    789\""
        ))).containsExactly("ABC", "abc def  ghi", "123", "456    789");
    }

    @Test
    @DisplayName("Arguments with escape characters without quotes")
    void test_EscapeCharacters_NoQuotes() {
        // \
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\\ "
        ))).containsExactly(" ");

        // \\
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\\\\"
        ))).containsExactly("\\");

        // ABC\ DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\ DEF"
        ))).containsExactly("ABC DEF");

        // ABC\ \ DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\ \\ DEF"
        ))).containsExactly("ABC  DEF");

        // ABC\\ DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\\ DEF"
        ))).containsExactly("ABC\\", "DEF");

        // ABC\\\ DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\\\\ DEF"
        ))).containsExactly("ABC\\ DEF");

        // ABC\\\\ DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\\\\\\ DEF"
        ))).containsExactly("ABC\\\\", "DEF");
    }

    @Test
    @DisplayName("Arguments with escape characters within quotes")
    void test_EscapeCharacters_WithinQuotes() {
        // "\ "
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"\\ \""
        ))).containsExactly("\\ ");

        // "ABC\ DEF"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC\\ DEF\""
        ))).containsExactly("ABC\\ DEF");

        // "ABC\ \ DEF"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC\\ \\ DEF\""
        ))).containsExactly("ABC\\ \\ DEF");
    }

    @Test
    @DisplayName("Arguments with quote characters without quotes")
    void test_QuoteCharacters_NoQuotes() {
        // \"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\\\""
        ))).containsExactly("\"");

        // \"ABC
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\\\"ABC"
        ))).containsExactly("\"ABC");

        // ABC\"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\""
        ))).containsExactly("ABC\"");

        // ABC\"DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\"DEF"
        ))).containsExactly("ABC\"DEF");

        // ABC\"\"DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\"\\\"DEF"
        ))).containsExactly("ABC\"\"DEF");

        // ABC\"\"\"DEF
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "ABC\\\"\\\"\\\"DEF"
        ))).containsExactly("ABC\"\"\"DEF");
    }

    @Test
    @DisplayName("Arguments with quote characters within quotes")
    void test_QuoteCharacters_WithinQuotes() {
        // "\""
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"\\\"\""
        ))).containsExactly("\"");

        // "\"ABC"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"\\\"ABC\""
        ))).containsExactly("\"ABC");

        // "ABC\""
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC\\\"\""
        ))).containsExactly("ABC\"");

        // "ABC\"DEF"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC\\\"DEF\""
        ))).containsExactly("ABC\"DEF");

        // "ABC\"\"DEF"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC\\\"\\\"DEF\""
        ))).containsExactly("ABC\"\"DEF");

        // "ABC\"\"\"DEF"
        assertThat(ArgumentParser.toList(toUTF8Bytes(
                "\"ABC\\\"\\\"\\\"DEF\""
        ))).containsExactly("ABC\"\"\"DEF");
    }

    private byte[] toUTF8Bytes(final String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
