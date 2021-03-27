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

import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.core.datapoint.DPT1;
import li.pitschmann.knx.core.datapoint.DataPointRegistry;
import li.pitschmann.knx.core.utils.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Server implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    /**
     * The port that should be allocated by the server and connected by clients
     */
    private static final int SERVER_CHANNEL_PORT = 10222;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ServerWorker serverWorker;

    public Server(final KnxClient knxClient) {
        DataPointRegistry.getDataPointType(DPT1.SWITCH.getId()); // warm up!
        serverWorker = new ServerWorker(knxClient);
    }

    @Override
    public void run() {
        LOG.trace("*** START ***");

        try (final var serverChannel = ServerSocketChannel.open()) {

            final var serverSocketAddress = new InetSocketAddress(SERVER_CHANNEL_PORT);
            serverChannel.bind(serverSocketAddress);
            serverChannel.configureBlocking(false);

            final var serverChannelPort = serverChannel.socket().getLocalPort();
            LOG.debug("Server Channel created at port: {}", serverChannelPort);

            final var serverCommunicator = new ServerCommunicator(serverChannel);
            executorService.submit(serverCommunicator);

            while (!Thread.currentThread().isInterrupted()) {
                final var packet = serverCommunicator.nextPacket();
                serverWorker.execute(packet);
            }

        } catch (final InterruptedException ie) {
            LOG.debug("Interrupt signal caught");
            Thread.currentThread().interrupt();
        } catch (final IOException ioe) {
            LOG.error("I/O Exception thrown", ioe);
        } catch (final Exception e) {
            LOG.error("Other exception", e);
        } finally {
            Closeables.shutdownQuietly(executorService);
            LOG.trace("*** END ***");
        }
    }
}
