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

import li.pitschmann.knx.core.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extracts byte array of argument to a UTF-8 argument list
 *
 * <p>
 * <p> <strong><u>Example 1</u></strong>:
 * <pre>{@code
 * Input:
 *      Hoi "Hello World" Привет 你好 Grüezi
 * Output:
 *      Index 0: Hoi
 *      Index 1: Hello World
 *      Index 2: Привет
 *      Index 3: 你好
 *      Index 4: Grüezi
 * }</pre>
 * <p>
 * <p> <strong><u>Example 2</u></strong>:
 * <pre>{@code
 * Input:
 *      "I am saying: \"Hello World\"!"
 * Output:
 *      Index 0: I am saying: "Hello World"!
 * }</pre>
 * <p>
 * <p> <strong><u>Example 3</u></strong>:
 * <pre>{@code
 * Input:
 *      Escaped\ Character\ Also\ Works!
 * Output:
 *      Index 0: Escaped Character Also Works!
 * }</pre>
 */
public final class ArgumentParser {

    private ArgumentParser() {
        throw new AssertionError("Do not touch me!");
    }

    /**
     * Parses the given byte array to a UTF-8 string and splits the
     * arguments into a list of String
     *
     * @param bytes bytes to be parsed
     * @return a new list of arguments, empty list if {@code null} or byte array is empty
     */
    public static List<String> toList(final @Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }

        // convert byte array to UTF-8 character array
        final var argsAsChars = new String(bytes, StandardCharsets.UTF_8).toCharArray();

        final var args = new ArrayList<String>();

        char ch;
        boolean quote = false;
        boolean escaped = false;
        final var sb = new StringBuilder();
        for (var i = 0; i < argsAsChars.length; i++) {
            ch = argsAsChars[i];
            if (Character.isWhitespace(ch)) {
                // add white space when escaped by \ or when within quote
                if (escaped || quote) {
                    sb.append(ch);
                }
                // else consider whitespace as separator and add text to list
                // only when string builder is not empty
                else if (sb.length() > 0) {
                    var arg = sb.toString();
                    sb.setLength(0);
                    args.add(arg);
                }
            } else if (ch == '\\') {
                // inside quote: add escape character immediately, when next character is not quote
                if (quote && argsAsChars[i + 1] != '"') {
                    sb.append(ch);
                }
                // outside of quote: add escape character, when previously escaped
                else if (escaped) {
                    sb.append(ch);
                }
                // else set escaped flag
                else {
                    escaped = true;
                    continue;
                }
            } else if (ch == '"') {
                // inside and outside quote: add " only when already escaped
                if (escaped) {
                    sb.append(ch);
                } else {
                    quote = !quote;
                }
            } else {
                sb.append(ch);
            }
            escaped = false;
        }

        // more strings in buffer, but not added yet?
        // add was done on whitespace character
        if (sb.length() > 0) {
            args.add(sb.toString());
        }

        return Collections.unmodifiableList(args);
    }

}
