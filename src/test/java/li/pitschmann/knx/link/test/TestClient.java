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
import li.pitschmann.knx.link.protocol.Header;
import li.pitschmann.knx.link.protocol.ResponseBody;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test Socket Client for communication tests
 */
public final class TestClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);
    private final int serverPort;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<String> receivedStrings = new ArrayList<>();
    private final List<ResponseBody> receivedResponses = new LinkedList<>();
    private Selector writeSelector;
    private Selector readSelector;

    private TestClient(final int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Creates a new {@link TestClient} listening to the {@code serverPort}.
     * For the client a random free port will be used.
     *
     * @param serverPort the port of server
     * @return a new instance of {@link TestClient}
     */
    public static TestClient createStarted(final int serverPort) {
        final var client = new TestClient(serverPort);
        client.start();
        return client;
    }

    /**
     * Verifies if the {@code expectedResponseBodies} has been received by
     * the current {@link TestClient} within 5 hardcoded-second timeout.
     *
     * @param expectedResponseBodies expected array of {@link ResponseBody}; may be empty
     * @throws AssertionError in case there was a mismatch or timeout occurred first
     */
    public void verifyReceivedResponses(ResponseBody... expectedResponseBodies) {
        var result = Sleeper.milliseconds(50, () -> {
            if (receivedResponses.size() == expectedResponseBodies.length) {
                for (var i = 0; i < expectedResponseBodies.length; i++) {
                    if (!expectedResponseBodies[i].equals(receivedResponses.get(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }, 5000);

        if (!result) {
            throw new AssertionError("Not expected responses received: " +
                    "expected=" + Arrays.toString(expectedResponseBodies) + ", actual=" + receivedResponses);
        }
    }

    /**
     * Verifies if the {@code expectedReceivedStrings} has been received by
     * the current {@link TestClient} within 5 hardcoded-second timeout.
     *
     * @param expectedReceivedStrings expected strings to be received; may be empty
     * @throws AssertionError in case there was a mismatch or timeout occurred first
     */
    public void verifyReceivedStrings(String... expectedReceivedStrings) {
        var result = Sleeper.milliseconds(50, () -> {
            if (receivedStrings.size() == expectedReceivedStrings.length) {
                for (var i = 0; i < expectedReceivedStrings.length; i++) {
                    if (!expectedReceivedStrings[i].equals(receivedStrings.get(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }, 5000);

        if (!result) {
            throw new AssertionError("Not expected received strings: " +
                    "expected=" + Arrays.toString(expectedReceivedStrings) + ", actual=" + receivedStrings);
        }
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

                final var headerBytes = Header.of(receivedBytes[0], receivedBytes[1]);
                final var responseBytes = ResponseBody.of(Arrays.copyOfRange(receivedBytes, 2, receivedBytes.length));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving packet: header={}, body={}", headerBytes, responseBytes);
                }

                receivedStrings.add(new String(receivedBytes, StandardCharsets.UTF_8));
                receivedResponses.add(responseBytes);
            } finally {
                byteBuffer.clear();
            }
        }
    }
}
