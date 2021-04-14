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

import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.config.Config;
import li.pitschmann.knx.link.config.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The main class to start the KNX Link Server
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // load config from 'server.cfg' file, if not readable fall back to default one
        final var serverConfigPath = Paths.get("server.cfg");
        final Config config;
        if (Files.isReadable(serverConfigPath)) {
            LOG.info("Config file found at: {}", serverConfigPath.toAbsolutePath());
            config = ConfigReader.load(serverConfigPath);
        } else {
            LOG.info("Fall back to default config. No Config file found at: {}", serverConfigPath.toAbsolutePath());
            config = Config.useDefault();
        }

        // start the KNX Link server and try to keep it alive forever
        // in case of issue, the restart will be done by the systemd
        try (final var server = DefaultServer.createStarted(config)) {
            LOG.info("KNX Link Server started");
            while (server.isRunning()) {
                Sleeper.seconds(5);
            }
        } finally {
            LOG.info("KNX Link Server stopped");
        }
    }

}
