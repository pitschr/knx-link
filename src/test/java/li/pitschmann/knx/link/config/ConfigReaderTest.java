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

import li.pitschmann.knx.core.config.CoreConfigs;
import li.pitschmann.knx.core.utils.Networker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ConfigReader}
 */
class ConfigReaderTest {

    @Test
    @DisplayName("Test blank configuration file")
    void testBlankConfig() {
        final var config = ConfigReader.load(Paths.get("src/test/resources/config_blank.cfg"));

        assertThat(config.getServerPort()).isEqualTo(Config.DEFAULT_SERVER_PORT);
        assertThat(config.getSecurityAuditor().getAllowedAddresses()).containsExactly("127.0.0.1");

        final var knxClientConfig = config.getKnxClientConfig();
        assertThat(knxClientConfig.getRemoteControlAddress().isAnyLocalAddress()).isTrue();
        assertThat(knxClientConfig.getRemoteControlPort()).isEqualTo(Config.DEFAULT_KNX_PORT);
        assertThat(knxClientConfig.isNatEnabled()).isEqualTo(Config.DEFAULT_KNX_NAT_ENABLED);
        assertThat(knxClientConfig.isRoutingEnabled()).isFalse();
    }

    @Test
    @DisplayName("Test configuration file with empty values")
    void testConfigEmptyValues() {
        final var config = ConfigReader.load(Paths.get("src/test/resources/config_emptyValues.cfg"));

        assertThat(config.getServerPort()).isEqualTo(Config.DEFAULT_SERVER_PORT);
        assertThat(config.getSecurityAuditor().getAllowedAddresses()).containsExactly("127.0.0.1");

        final var knxClientConfig = config.getKnxClientConfig();
        assertThat(knxClientConfig.getRemoteControlAddress().isAnyLocalAddress()).isTrue();
        assertThat(knxClientConfig.getRemoteControlPort()).isEqualTo(Config.DEFAULT_KNX_PORT);
        assertThat(knxClientConfig.isNatEnabled()).isEqualTo(Config.DEFAULT_KNX_NAT_ENABLED);
        assertThat(knxClientConfig.isRoutingEnabled()).isFalse();
    }

    @Test
    @DisplayName("Test configuration with tunneling setup (without NAT)")
    void testConfigTunneling() {
        final var config = ConfigReader.load(Paths.get("src/test/resources/config_tunneling.cfg"));

        assertThat(config.getServerPort()).isEqualTo(4567);
        assertThat(config.getSecurityAuditor().getAllowedAddresses())
                .containsExactlyInAnyOrder("192.168.3.6", "192.168.3.8", "192.168.3.14");

        final var knxClientConfig = config.getKnxClientConfig();
        assertThat(knxClientConfig.getRemoteControlAddress()).isEqualTo(Networker.getByAddress("192.168.1.2"));
        assertThat(knxClientConfig.getRemoteControlPort()).isEqualTo(1234);
        assertThat(knxClientConfig.isNatEnabled()).isFalse();
        assertThat(knxClientConfig.isRoutingEnabled()).isFalse();
    }

    @Test
    @DisplayName("Test configuration with tunneling setup (with NAT)")
    void testConfigTunnelingWithNAT() {
        final var config = ConfigReader.load(Paths.get("src/test/resources/config_tunneling_withNAT.cfg"));

        assertThat(config.getServerPort()).isEqualTo(8332);
        assertThat(config.getSecurityAuditor().getAllowedAddresses()).containsExactly("192.168.5.93");

        final var knxClientConfig = config.getKnxClientConfig();
        assertThat(knxClientConfig.getRemoteControlAddress()).isEqualTo(Networker.getByAddress("10.12.1.3"));
        assertThat(knxClientConfig.getRemoteControlPort()).isEqualTo(27333);
        assertThat(knxClientConfig.isNatEnabled()).isTrue();
        assertThat(knxClientConfig.isRoutingEnabled()).isFalse();
    }

    @Test
    @DisplayName("Test configuration with routing setup")
    void testConfigRouting() {
        final var config = ConfigReader.load(Paths.get("src/test/resources/config_routing.cfg"));

        assertThat(config.getServerPort()).isEqualTo(11292);
        assertThat(config.getSecurityAuditor().getAllowedAddresses())
                .containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2");

        final var knxClientConfig = config.getKnxClientConfig();
        assertThat(knxClientConfig.getRemoteControlAddress()).isEqualTo(Networker.getByAddress("224.0.23.11"));
        assertThat(knxClientConfig.getRemoteControlPort()).isEqualTo(3323);
        assertThat(knxClientConfig.isNatEnabled()).isFalse();
        assertThat(knxClientConfig.isRoutingEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test configuration with routing setup without address")
    void testConfigRoutingNoAddress() {
        final var config = ConfigReader.load(Paths.get("src/test/resources/config_routing_noAddress.cfg"));

        assertThat(config.getServerPort()).isEqualTo(Config.DEFAULT_SERVER_PORT);
        assertThat(config.getSecurityAuditor().getAllowedAddresses()).containsExactly("127.0.0.1");

        final var knxClientConfig = config.getKnxClientConfig();
        assertThat(knxClientConfig.getRemoteControlAddress()).isEqualTo(CoreConfigs.MULTICAST_ADDRESS);
        assertThat(knxClientConfig.getRemoteControlPort()).isEqualTo(CoreConfigs.KNX_PORT);
        assertThat(knxClientConfig.isNatEnabled()).isFalse();
        assertThat(knxClientConfig.isRoutingEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test with an non-existing configuration file")
    void testNonExistingConfig() {
        assertThatThrownBy(() -> ConfigReader.load(Paths.get("src/test/resources/loremIpsum.cfg")))
                .isInstanceOf(ConfigException.class)
                .hasMessage("Could not read the configuration: src/test/resources/loremIpsum.cfg");
    }
}
