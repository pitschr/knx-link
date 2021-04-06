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
import li.pitschmann.knx.core.datapoint.DPT1;
import li.pitschmann.knx.core.datapoint.DataPointRegistry;
import li.pitschmann.knx.core.utils.Closeables;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The KNX Link Server
 *
 * <p> Will create a server socket, listens to incoming socket channels
 * for requests and acts as a proxy between socket channel and the KNX
 * Net/IP device.
 */
public final class Server implements Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private final Config config;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final AtomicBoolean running = new AtomicBoolean();

    Server(final Config config) {
        this.config = Objects.requireNonNull(config);
        DataPointRegistry.getDataPointType(DPT1.SWITCH.getId()); // warm up!
    }

    /**
     * Creates a new KNX Link Server and starts
     *
     * @param config the configuration; may not be null
     * @return the KNX Link Server which has been started
     */
    public static Server createStarted(final Config config) {
        final var server = new Server(config);
        server.start();
        return server;
    }

    /**
     * Lazy initialization of {@link KnxClient}
     *
     * @return the {@link KnxClient}
     */
    KnxClient getKnxClient() {
        return DefaultKnxClient.createStarted(config.getKnxClientConfig());
    }

    /**
     * Starts the KNX Link Server
     */
    public void start() {
        executorService.submit(this);
        // wait until running state is true (up to 10 sec)
        if (Sleeper.milliseconds(100, this::isRunning, 10000)) {
            LOG.info("Server started up: {}", this);
        } else {
            LOG.error("Something went wrong starting up the server within 10 seconds. Please check logs.");
            close(); // call close() method to ensure that it is cleaned up!
        }
    }

    /**
     * Stops and closes the KNX Link Server
     */
    @Override
    public void close() {
        Closeables.shutdownQuietly(executorService);
    }

    /**
     * Indicates if the KNX Link Server is still running
     *
     * @return {@code true} if running, otherwise {@code false}
     */
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        if (running.getAndSet(true)) {
            LOG.warn("There is already a Server running. Do nothing.");
            return;
        }

        LOG.trace("*** START ***");

        try (final var knxClient = getKnxClient()) {

            final var serverCommunicator = new ServerCommunicator(config);
            executorService.submit(serverCommunicator);

            final var serverWorker = new ServerWorker(knxClient);

            while (!Thread.currentThread().isInterrupted() && knxClient.isRunning()) {
                final var packet = serverCommunicator.nextPacket();
                try {
                    serverWorker.execute(packet);
                } catch (final Exception e) {
                    LOG.error("An exception happened inside the worker", e);
                }
            }

        } catch (final InterruptedException ie) {
            LOG.debug("Interrupt signal caught");
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            LOG.error("An other exception occurred", e);
        } finally {
            Closeables.shutdownQuietly(executorService);
            running.set(false);
            LOG.trace("*** END ***");
        }
    }
}
