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

import li.pitschmann.knx.core.utils.Networker;
import li.pitschmann.knx.link.SecurityAuditor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Config}
 */
class ConfigTest {

    @Test
    @DisplayName("Test #useDefault()")
    void testUseDefault() {
        final var config = Config.useDefault();

        assertThat(config.getServerPort()).isEqualTo(Config.DEFAULT_SERVER_PORT);
        assertThat(config.getSecurityAuditor().getAllowedAddresses()).containsExactlyElementsOf(Config.DEFAULT_SERVER_ALLOWED_ADDRESSES);

        final var knxConfig = config.getKnxClientConfig();
        assertThat(knxConfig.isRoutingEnabled()).isFalse();
        assertThat(knxConfig.isNatEnabled()).isEqualTo(Config.DEFAULT_KNX_NAT_ENABLED);
        assertThat(knxConfig.getRemoteControlAddress().isAnyLocalAddress()).isTrue();
        assertThat(knxConfig.getRemoteControlPort()).isEqualTo(Config.DEFAULT_KNX_PORT);

        assertThat(config).hasToString(
                "Config{" +
                        "knxMode=TUNNELING, " +
                        "knxNatEnabled=false, " +
                        "knxAddress=0.0.0.0, " +
                        "knxPort=3671, " +
                        "serverPort=3672, " +
                        "securityAuditor=SecurityAuditor{allowedAddresses=[127.0.0.1]}" +
                        "}"
        );
    }

    @Test
    @DisplayName("Test config with custom settings")
    void testCustomSettings() {
        final var config = new Config(
                1234,
                KnxMode.ROUTING,
                false,
                Networker.getByAddress(224, 6, 7, 8),
                9123,
                new SecurityAuditor(Set.of("10.0.1.2"))
        );

        assertThat(config.getServerPort()).isEqualTo(1234);
        assertThat(config.getSecurityAuditor().getAllowedAddresses()).containsExactly("10.0.1.2");

        final var knxConfig = config.getKnxClientConfig();
        assertThat(knxConfig.isRoutingEnabled()).isTrue();
        assertThat(knxConfig.isNatEnabled()).isFalse();
        assertThat(knxConfig.getRemoteControlAddress().getAddress()).containsExactly(224, 6, 7, 8);
        assertThat(knxConfig.getRemoteControlPort()).isEqualTo(9123);

        assertThat(config).hasToString(
                "Config{" +
                        "knxMode=ROUTING, " +
                        "knxNatEnabled=false, " +
                        "knxAddress=224.6.7.8, " +
                        "knxPort=9123, " +
                        "serverPort=1234, " +
                        "securityAuditor=SecurityAuditor{allowedAddresses=[10.0.1.2]}" +
                        "}"
        );
    }
}
