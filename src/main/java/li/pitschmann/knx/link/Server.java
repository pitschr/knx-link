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
import li.pitschmann.knx.core.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Server implements Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    /**
     * The port that should be allocated by the server and connected by clients
     */
    private static final int SERVER_CHANNEL_PORT = 10222;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ServerWorker serverWorker;
    private boolean running;

    public Server(final KnxClient knxClient) {
        DataPointRegistry.getDataPointType(DPT1.SWITCH.getId()); // warm up!
        serverWorker = new ServerWorker(knxClient);
    }

    public static final Server createStarted(final KnxClient knxClient) {
        final var server = new Server(knxClient);
        server.start();
        return server;
    }

    private void start() {
        executorService.submit(this);
        // wait until running state is true (up to 10 sec)
        if (Sleeper.milliseconds(100, this::isRunning, 10000)) {
            LOG.info("Server started up: {}", this);
        } else {
            LOG.error("Something went wrong starting up the server within 10 seconds. Please check logs.");
            close(); // call close() method to ensure that it is cleaned up!
        }
    }

    @Override
    public void close() {
        Closeables.shutdownQuietly(executorService);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        running = true;
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
                try {
                    serverWorker.execute(packet);
                } catch (final Exception e) {
                    LOG.error("Error inside ", e);
                    packet.getChannel().write(ByteBuffer.wrap("ERROR".getBytes(StandardCharsets.UTF_8)));
                }
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
            running = false;
        }
    }
}
