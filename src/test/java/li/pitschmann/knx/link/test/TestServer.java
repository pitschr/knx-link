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

import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.link.AbstractServer;
import li.pitschmann.knx.link.config.Config;

/**
 * KNX Link Server for testing purposes only, uses mocked {@link KnxClient}
 */
public final class TestServer extends AbstractServer {
    private TestServer(final Config config) {
        super(config);
    }

    /**
     * Creates a new KNX Link Test Server and starts
     *
     * @return the KNX Link Server which has been started, only for testing purposes
     */
    public static TestServer createStarted() {
        final var server = new TestServer(Config.useDefault());
        server.start();
        return server;
    }

    @Override
    protected KnxClient getKnxClient() {
        return Helper.createKnxClientMock();
    }
}
