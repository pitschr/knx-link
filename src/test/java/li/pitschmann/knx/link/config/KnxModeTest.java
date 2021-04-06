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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link KnxMode}
 */
class KnxModeTest {

    @Test
    @DisplayName("Test #TUNNELING")
    void test_tunneling() {
        assertThat(KnxMode.TUNNELING.getMode()).isEqualTo("tunneling");
        assertThat(KnxMode.of("TuNNeliNG")).isSameAs(KnxMode.TUNNELING);
    }

    @Test
    @DisplayName("Test #ROUTING")
    void test_routing() {
        assertThat(KnxMode.ROUTING.getMode()).isEqualTo("routing");
        assertThat(KnxMode.of("RoutinG")).isSameAs(KnxMode.ROUTING);
    }

    @Test
    @DisplayName("Test unsupported KnxMode")
    void test_unsupportedMode() {
        assertThatThrownBy(() -> KnxMode.of("foobar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mode is not supported: foobar");
    }
}
