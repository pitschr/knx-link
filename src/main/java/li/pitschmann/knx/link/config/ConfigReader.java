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

import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.config.CoreConfigs;
import li.pitschmann.knx.core.utils.Networker;
import li.pitschmann.knx.core.utils.Strings;
import li.pitschmann.knx.link.SecurityAuditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * Utility to read a configuration file
 *
 * <p> Call it by {@link ConfigReader#load(Path)}
 *
 * @author PITSCHR
 */
public final class ConfigReader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigReader.class);

    private ConfigReader() {
        throw new AssertionError("Do not call me!");
    }

    /**
     * Reads the configuration file from given {@link Path}
     *
     * @param path the configuration path, must exists and readable; may not be null
     * @return an immutable Config
     */
    public static Config load(final Path path) {
        if (Files.isReadable(path)) {
            final var properties = new Properties();
            try (final var fis = new FileInputStream(path.toFile())) {
                properties.load(fis);

                return new Config(
                        getServerPort(properties),
                        getKnxMode(properties),
                        getKnxNatEnabled(properties),
                        getKnxAddress(properties),
                        getKnxPort(properties),
                        getSecurityAuditor(properties)
                );
            } catch (IOException e) {
                LOG.error("I/O Exception during reading configuration", e);
            }
        } else {
            LOG.error("Configuration file not found or is not readable: {}", path);
        }
        throw new ConfigException("Could not read the configuration: " + path);
    }

    /**
     * Returns the server port from {@code server.port} configuration key.
     * Defaults to {@link Config#DEFAULT_SERVER_PORT} if not specified
     *
     * @param properties the loaded properties; may not be null
     * @return the server port
     */
    private static int getServerPort(final Properties properties) {
        final var serverPort = properties.getProperty("server.port");
        return Strings.isNullOrEmpty(serverPort) ? Config.DEFAULT_SERVER_PORT : Integer.parseInt(serverPort);
    }

    /**
     * Returns the KNX mode for communication from {@code knx.mode} configuration
     * key. Defaults to {@link Config#DEFAULT_KNX_MODE} if not specified
     *
     * @param properties the loaded properties; may not be null
     * @return the KNX Mode
     */
    private static KnxMode getKnxMode(final Properties properties) {
        final var knxMode = properties.getProperty("knx.mode");
        return Strings.isNullOrEmpty(knxMode) ? Config.DEFAULT_KNX_MODE : KnxMode.of(knxMode);
    }

    /**
     * Returns if the communication with KNX Net/IP device should be done via
     * NAT, read from {@code knx.nat} configuration key. Defaults to
     * {@link Config#DEFAULT_KNX_NAT_ENABLED}
     *
     * @param properties the loaded properties; may not be null
     * @return {@code true} if the NAT should be enabled, {@code false} otherwise
     */
    private static boolean getKnxNatEnabled(final Properties properties) {
        final var knxNatEnabled = properties.getProperty("knx.nat", "false");
        return Strings.isNullOrEmpty(knxNatEnabled) ? Config.DEFAULT_KNX_NAT_ENABLED : Boolean.parseBoolean(knxNatEnabled);
    }

    /**
     * Returns the address of KNX Net/IP device is specified by {@code knx.address}
     * configuration key. Defaults to {@code null}
     *
     * @param properties the loaded properties; may not be null
     * @return the {@link InetAddress} of KNX Net/IP device, {@code null}
     * if no address is specified which uses the auto-discovery
     */
    @Nullable
    private static InetAddress getKnxAddress(final Properties properties) {
        final var knxAddress = properties.getProperty("knx.address");
        return Strings.isNullOrEmpty(knxAddress) ? null : Networker.getByAddress(knxAddress);
    }

    /**
     * Returns the port of KNX Net/IP device and is specified by {@code knx.port}.
     * Defaults to {@link Config#DEFAULT_KNX_PORT}
     *
     * @param properties the loaded properties; may not be null
     * @return the port of KNX Net/IP device
     */
    private static int getKnxPort(final Properties properties) {
        final var knxPort = properties.getProperty("knx.port");
        return Strings.isNullOrEmpty(knxPort) ? CoreConfigs.KNX_PORT : Integer.parseInt(knxPort);
    }

    /**
     * Returns the {@link SecurityAuditor} that is used to protect the server
     * (e.g. accept requests only from allowed IP addresses)
     *
     * @param properties the loaded properties; may not be null
     * @return the {@link SecurityAuditor}
     */
    private static SecurityAuditor getSecurityAuditor(final Properties properties) {
        final var allowedAddresses = properties.getProperty("server.allowed.addresses", "").trim();
        if (Strings.isNullOrEmpty(allowedAddresses)) {
            return new SecurityAuditor();
        } else {
            final var securityClientsAsArray = allowedAddresses.trim().split("\\s*,\\s*");
            return new SecurityAuditor(Set.of(securityClientsAsArray));
        }
    }
}
