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

import li.pitschmann.knx.core.config.ConfigBuilder;
import li.pitschmann.knx.core.config.CoreConfigs;
import li.pitschmann.knx.core.utils.Networker;
import li.pitschmann.knx.core.utils.Strings;
import li.pitschmann.knx.link.SecurityAuditor;

import java.net.InetAddress;
import java.util.Set;

/**
 * Immutable {@link Config} for KNX Link Server
 *
 * @author PITSCHR
 */
public final class Config {
    public static final int DEFAULT_SERVER_PORT = 3672;
    public static final Set<String> DEFAULT_SERVER_ALLOWED_ADDRESSES = Set.of("127.0.0.1");
    public static final KnxMode DEFAULT_KNX_MODE = KnxMode.TUNNELING;
    public static final boolean DEFAULT_KNX_NAT_ENABLED = false;
    public static final int DEFAULT_KNX_PORT = CoreConfigs.KNX_PORT;
    public static final InetAddress DEFAULT_KNX_ADDRESS = Networker.getAddressUnbound();

    private final KnxMode knxMode;
    private final boolean knxNatEnabled;
    private final InetAddress knxAddress;
    private final int knxPort;
    private final int serverPort;
    private final SecurityAuditor securityAuditor;

    Config(
            final int serverPort,
            final KnxMode knxMode,
            final boolean knxNatEnabled,
            final InetAddress knxAddress,
            final int knxPort,
            final SecurityAuditor securityAuditor) {

        this.serverPort = serverPort;
        this.knxMode = knxMode;
        this.knxNatEnabled = knxNatEnabled;
        this.knxAddress = knxAddress;
        this.knxPort = knxPort;
        this.securityAuditor = securityAuditor;
    }

    public static final Config useDefault() {
        return new Config(
                DEFAULT_SERVER_PORT,
                DEFAULT_KNX_MODE,
                DEFAULT_KNX_NAT_ENABLED,
                DEFAULT_KNX_ADDRESS,
                DEFAULT_KNX_PORT,
                new SecurityAuditor(DEFAULT_SERVER_ALLOWED_ADDRESSES)
        );
    }

    public SecurityAuditor getSecurityAuditor() {
        return securityAuditor;
    }

    public int getServerPort() {
        return serverPort;
    }

    /**
     * Returns the configuration for {@link li.pitschmann.knx.core.communication.KnxClient}
     *
     * @return immutable {@link li.pitschmann.knx.core.communication.KnxClient} configuration
     */
    public li.pitschmann.knx.core.config.Config getKnxClientConfig() {
        if (KnxMode.TUNNELING.equals(knxMode)) {
            if (knxAddress.isAnyLocalAddress()) {
                // auto-discovery to be used
                return ConfigBuilder.tunneling(knxNatEnabled).build();
            } else {
                return ConfigBuilder.tunneling(
                        knxAddress,
                        knxPort,
                        knxNatEnabled
                ).build();
            }
        } else if (KnxMode.ROUTING.equals(knxMode)) {
            if (knxAddress.isAnyLocalAddress()) {
                // auto-discovery to be used
                return ConfigBuilder.routing().build();
            } else {
                return ConfigBuilder.routing(
                        knxAddress,
                        knxPort
                ).build();
            }
        } else {
            throw new AssertionError("Unsupported KNX communication mode selected: " + knxMode);
        }
    }

    @Override
    public String toString() {
        return Strings.toStringHelper(this)
                .add("knxMode", knxMode.name())
                .add("knxNatEnabled", knxNatEnabled)
                .add("knxAddress", knxAddress.getHostAddress())
                .add("knxPort", knxPort)
                .add("serverPort", serverPort)
                .add("securityAuditor", securityAuditor)
                .toString();
    }
}
