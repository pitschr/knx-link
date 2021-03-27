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

import li.pitschmann.knx.core.communication.DefaultKnxClient;
import li.pitschmann.knx.core.utils.Sleeper;

/**
 * The main class to start the KNX Link Server
 */
public class Main {

    public static void main(String[] args) {

        // TODO:
        // 1: Find server.cfg
        // 2: Load and parses server.cfg and create an immutable ServerConfig instance
        //       Validations:
        //       'nat' only available for tunneling
        //       'address': multicast address available for routing only, rest for tunneling
        //       'address': must be in (byte).(byte).(byte).(byte) format -> see: li.pitschmann.knx.core.utils.Networker.getByAddress(java.lang.String)
        //       'port': 1024 - 65535
        // 3: Prepare Config for KNX Client based on ServerConfig
        // 4: Start KNX Client, Accepting the Client connections

        try (final var knxClient = DefaultKnxClient.createStarted()) {
            final var server = new Server(knxClient);
            while (knxClient.isRunning()) {
                Sleeper.seconds(60);
            }
        }
    }

}
