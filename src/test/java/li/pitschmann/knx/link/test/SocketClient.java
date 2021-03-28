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

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Closeables;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test Socket Client for communication tests
 */
public final class SocketClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SocketClient.class);
    private final int serverPort;
    private Selector selector;
    private Selector selector2;
    private ExecutorService executorService;

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
        client.open();
        return client;
    }

    public void readRequest(final String groupAddress, final String dataPointType) {
        sendProtocolV1(Action.READ_REQUEST, groupAddress, dataPointType, null);
    }

    public void writeRequest(final String groupAddress, final String dataPointType, final String... values) {
        sendProtocolV1(Action.WRITE_REQUEST, groupAddress, dataPointType, values);
    }

    /**
     * Send a message in Protocol Version 1
     *
     * @param action       the action to be requested; may not be null
     * @param groupAddress the group address in free-level (1), two-level (1/2)
     *                     or three-level (1/2/3) format; may not be null
     * @param dpt          the data point type in "x.y" format (e.g. 1.001); may be null (defaults to 0.000)
     * @param arguments    array of string that represents the arguemnt to be sent; may be null for read (defaults to new String[0])
     */
    private void sendProtocolV1(final Action action,
                                final String groupAddress,
                                final @Nullable String dpt,
                                final @Nullable String[] arguments) {
        final var bytes = new byte[100];
        int i = 0;

        // Protocol Version 1
        bytes[i++] = 0x01;
        // Action (read, write)
        bytes[i++] = action.getByte();
        // Group Address
        final var groupAddressAsBytes = GroupAddress.of(groupAddress).toByteArray();
        bytes[i++] = groupAddressAsBytes[0];
        bytes[i++] = groupAddressAsBytes[1];
        // Data Point type
        if (dpt != null) {
            final var dataPointType = dpt.split("\\.");
            final var type = Integer.parseInt(dataPointType[0]);
            final var subType = Integer.parseInt(dataPointType[1]);
            bytes[i++] = (byte) ((type & 0xFF00) >> 8);
            bytes[i++] = (byte) (type & 0xFF);
            bytes[i++] = (byte) ((subType & 0xFF00) >> 8);
            bytes[i++] = (byte) (subType & 0xFF);
        } else {
            i += 4; // skip 4 bytes and leave them as 0x00
        }
        // Arguments
        if (arguments != null) {
            for (int a = 0; a < arguments.length; a++) {
                if (a != 0) {
                    bytes[i++] = ' '; // space
                }
                bytes[i++] = '"'; // quote
                final var argAsBytes = arguments[a].getBytes(StandardCharsets.UTF_8);
                System.arraycopy(argAsBytes, 0, bytes, i, argAsBytes.length);
                i += argAsBytes.length;
                bytes[i++] = '"'; // quote
            }
        }

        if (action == Action.WRITE_REQUEST) {
            bytes[i++] = 0x00; // +1 for NULL termination
        }

        send(Arrays.copyOf(bytes, i));
    }

    private void open() {
        LOG.debug("*** START ({}) ***", this);
        try {
            final var channel = SocketChannel.open(new InetSocketAddress("localhost", serverPort));
            selector = Selector.open();
            selector2 = Selector.open();
            channel.configureBlocking(false);
            // prepare channel for non-blocking and register to selector
            channel.register(selector, SelectionKey.OP_WRITE);
            channel.register(selector2, SelectionKey.OP_READ);

            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new SocketListener());

            LOG.debug("Socket Client (port: {}) is connected: {}",
                    channel.getLocalAddress(), channel.isConnected());
        } catch (final IOException e) {
            LOG.error("I/O Exception during open()", e);
        }
    }

    @Override
    public void close() {
        Closeables.closeQuietly(selector);
        Closeables.closeQuietly(selector2);
        Closeables.shutdownQuietly(executorService);
        LOG.debug("*** END ({}) ***", this);
    }

    private class SocketListener implements Runnable {
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && selector2.select() > 0) {
                    final var selectedKeys = selector2.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        final var key = selectedKeys.next();
                        selectedKeys.remove();

                        if (key.isValid() && key.isReadable()) {
                            final var socketChannel = (SocketChannel) key.channel();
                            final var receivedBytes = readAsArray(socketChannel);
                            LOG.debug("Read bytes: {}", (Object) ByteFormatter.formatHex(receivedBytes));
                        }
                    }
                }
            } catch (final Exception e) {
                LOG.error("Exception thrown for Socket Client ({})", this, e);
            }
        }

        private byte[] readAsArray(final SocketChannel channel) throws IOException {
            try {
                channel.read(byteBuffer);
                byteBuffer.flip();
                final var receivedBytes = new byte[byteBuffer.limit()];
                byteBuffer.get(receivedBytes);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Receiving packet: {}", ByteFormatter.formatHexAsString(receivedBytes));
                }
                return receivedBytes;
            } finally {
                byteBuffer.clear();
            }
        }
    }

    private void send(final byte[] bytes) {
        try {
            selector.select();
            final var selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                final var key = selectedKeys.next();
                selectedKeys.remove();

                if (key.isValid() && key.isWritable()) {
                    final var bytesToSend = ByteBuffer.wrap(bytes);
                    ((SocketChannel) key.channel()).write(bytesToSend);
                    LOG.debug("Write bytes: {}", (Object) ByteFormatter.formatHex(bytesToSend.array()));
                }
            }
        } catch (final Exception e) {
            LOG.error("Exception thrown for Socket Client ({})", this, e);
        }
    }
}
