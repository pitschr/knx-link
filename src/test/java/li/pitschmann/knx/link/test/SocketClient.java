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

import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Closeables;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test Socket Client for communication tests
 */
public final class SocketClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SocketClient.class);
    private final int serverPort;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<String> receivedStrings = new ArrayList<>();
    private Selector writeSelector;
    private Selector readSelector;

    private SocketClient(final int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Creates a new {@link SocketClient} listening to the {@code serverPort}.
     * For the client a random free port will be used.
     *
     * @param serverPort the port of server
     * @return a new instance of {@link SocketClient}
     */
    public static SocketClient createStarted(final int serverPort) {
        final var client = new SocketClient(serverPort);
        client.start();
        return client;
    }

    public List<String> getReceivedStrings() {
        return receivedStrings;
    }

    public void readRequest(final String groupAddress, final String dataPointType) {
        send(
                Helper.createProtocolV1Packet(Action.READ_REQUEST, groupAddress, dataPointType, null)
        );
    }

    public void writeRequest(final String groupAddress, final String dataPointType, final String... values) {
        send(
                Helper.createProtocolV1Packet(Action.WRITE_REQUEST, groupAddress, dataPointType, values)
        );
    }


    private void start() {
        try {
            writeSelector = Selector.open();
            readSelector = Selector.open();

            createAndRegisterChannel();
        } catch (final IOException e) {
            LOG.error("I/O Exception during open()", e);
        }

        executorService.submit(new SocketListener());
    }

    private void createAndRegisterChannel() throws IOException {
        var connected = false;
        do {
            try {
                final var channel = SocketChannel.open(new InetSocketAddress("localhost", serverPort));
                channel.configureBlocking(false);
                // prepare channel for non-blocking and register to selector
                channel.register(writeSelector, SelectionKey.OP_WRITE);
                channel.register(readSelector, SelectionKey.OP_READ);
                LOG.debug("Socket Client (port: {}) is connected: {}",
                        channel.getLocalAddress(), channel.isConnected());

                connected = channel.isConnected();
            } catch (final ConnectException ce) {
                Sleeper.seconds(3); // try again in 3 seconds
            }
        } while (!connected);
    }

    @Override
    public void close() {
        Closeables.closeQuietly(writeSelector);
        Closeables.closeQuietly(readSelector);
        Closeables.shutdownQuietly(executorService);
        LOG.debug("*** END ({}) ***", this);
    }

    private void send(final byte[] bytes) {
        try {
            if (writeSelector.select() > 0) {
                final var selectedKeys = writeSelector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    final var key = selectedKeys.next();
                    selectedKeys.remove();

                    if (key.isValid() && key.isWritable()) {
                        final var bytesToSend = ByteBuffer.wrap(bytes);
                        ((SocketChannel) key.channel()).write(bytesToSend);
                        LOG.debug("Write bytes: {}", (Object) ByteFormatter.formatHex(bytesToSend.array()));
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Exception thrown for Socket Client ({})", this, e);
        }
    }

    private class SocketListener implements Runnable {
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(512);

        @Override
        public void run() {
            LOG.debug("*** START ({}) ***", this);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    readSelector.select();
                    final var selectedKeys = readSelector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        final var key = selectedKeys.next();
                        selectedKeys.remove();
                        if (key.isValid() && key.isReadable()) {
                            read(key);
                        }
                    }
                }
            } catch (final Exception e) {
                LOG.error("Exception thrown for Socket Client ({})", this, e);
            } finally {
                LOG.debug("*** END ({}) ***", this);
            }
        }

        private void read(final SelectionKey key) throws IOException {
            try {
                final var channel = (SocketChannel) key.channel();
                int length = channel.read(byteBuffer);
                if (length < 0) {
                    // Channel is closed
                    LOG.warn("Channel is closed. Reconnect in progress.");
                    channel.close();
                    createAndRegisterChannel();
                    return;
                } else {
                    LOG.debug("channel read: {}", length);
                }

                byteBuffer.flip();
                final var receivedBytes = new byte[byteBuffer.limit()];
                byteBuffer.get(receivedBytes);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving packet: {}", ByteFormatter.formatHexAsString(receivedBytes));
                }
                System.out.println("Receiving packet: " + new String(receivedBytes, StandardCharsets.UTF_8));

                receivedStrings.add(new String(receivedBytes, StandardCharsets.UTF_8));
            } finally {
                byteBuffer.clear();
            }
        }
    }
}
