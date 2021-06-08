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

package li.pitschmann.knx.link.test;

import li.pitschmann.knx.core.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Server for manual testing only (not connected with real KNX Net/IP device)
 */
public class TestServerMain {
    public static final Logger LOG = LoggerFactory.getLogger(TestServerMain.class);

    public static void main(String[] args) {
        try (final var server = TestServer.createStarted()) {
            LOG.info("KNX Link Test Server started");
            while (server.isRunning()) {
                Sleeper.seconds(5);
            }
        } finally {
            LOG.info("KNX Link Test Server stopped");
        }
    }
}
