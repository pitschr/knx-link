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
import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.link.config.Config;

/**
 * The KNX Link Server. Default implementation of {@link AbstractServer}
 *
 * <p> Will create a server socket, listens to incoming socket channels
 * for requests and acts as a proxy between socket channel and the KNX
 * Net/IP device.
 */
public final class DefaultServer extends AbstractServer {

    private DefaultServer(final Config config) {
        super(config);
    }

    /**
     * Creates a new KNX Link Server and starts
     *
     * @param config the configuration; may not be null
     * @return the KNX Link Server which has been started
     */
    public static DefaultServer createStarted(final Config config) {
        final var server = createUnstarted(config);
        server.start();
        return server;
    }

    /**
     * Creates a new KNX Link Server unstarted. To start the server call {@link #start()}.
     *
     * @param config the configuration; may not be null
     * @return the KNX Link Server which has *not* been started
     */
    public static DefaultServer createUnstarted(final Config config) {
        return new DefaultServer(config);
    }

    /**
     * Lazy initialization of {@link KnxClient}
     *
     * @return the {@link KnxClient}
     */
    protected KnxClient getKnxClient() {
        return DefaultKnxClient.createStarted(config.getKnxClientConfig());
    }
}
