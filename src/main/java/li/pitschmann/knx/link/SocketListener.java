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

import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Closeables;
import li.pitschmann.knx.link.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TCP socket listener for {@link AbstractServer}. Accepts channel and reads bytes
 * from channel. The read bytes are added to {@link BlockingQueue}
 * as a {@link ChannelPacket}.
 */
public final class SocketListener implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SocketListener.class);
    private final Config config;
    private final ByteBuffer buff = ByteBuffer.allocate(512);
    private final BlockingQueue<ChannelPacket> queue = new LinkedBlockingQueue<>();

    SocketListener(final Config config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Returns the next {@link ChannelPacket}. This method is blocking until
     * the next {@link ChannelPacket} is available.
     *
     * @return the {@link ChannelPacket}
     * @throws InterruptedException if the thread has been interrupted
     */
    public ChannelPacket nextPacket() throws InterruptedException {
        return this.queue.take();
    }

    /**
     * Opens a {@link Selector} and configures the {@link ServerSocketChannel} to
     * listen on the port defined by {@link Config#getServerPort()}
     *
     * @param channel a {@link ServerSocketChannel}; may not be null
     * @return a new {@link Selector}
     * @throws IOException if an I/O exception occurred
     */
    private Selector createSelector(final ServerSocketChannel channel) throws IOException {
        final var serverSocketAddress = new InetSocketAddress(config.getServerPort());
        channel.bind(serverSocketAddress);
        channel.configureBlocking(false);
        LOG.debug("Server Channel opened at port: {}", serverSocketAddress.getPort());

        final var selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);
        LOG.debug("Selector created, now ready to accept connections at channel: {}", channel);
        return selector;
    }

    @Override
    public void run() {
        LOG.trace("*** START ***");
        try (final var serverSocketChannel = ServerSocketChannel.open();
             final var selector = createSelector(serverSocketChannel)) {
            while (!Thread.interrupted()) {
                selector.select();
                final var selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    final var key = selectedKeys.next();
                    selectedKeys.remove();

                    // new client to join?
                    if (key.isValid() && key.isAcceptable()) {
                        acceptAndRegister(key);
                    }

                    // new request from client?
                    if (key.isValid() && key.isReadable()) {
                        read(key);
                    }
                }
            }
        } catch (final IOException ioe) {
            LOG.error("I/O Exception", ioe);
        } finally {
            LOG.trace("*** END ***");
        }
    }

    /**
     * Accepts the channel from {@link SelectionKey} and registers
     * it for the read operation.
     *
     * @param key the selection key that contains channel to be accepted; may not be null.
     */
    private void acceptAndRegister(final SelectionKey key) {
        try {
            final var client = ((ServerSocketChannel) key.channel()).accept();

            // check if client passes the security checks
            if (config.getSecurityAuditor().isRemoteAddressValid(client)) {
                client.configureBlocking(false);
                client.register(key.selector(), SelectionKey.OP_READ);
                LOG.debug("Client accepted: {}", client.getRemoteAddress());
            } else {
                Closeables.closeQuietly(client);
                LOG.warn("Client rejected: {}", client);
            }
        } catch (final IOException ioe) {
            LOG.error("Could not accept the client due I/O Exception", ioe);
        }
    }

    /**
     * Reads the byte array stream from {@link SelectionKey}
     *
     * @param key the selection key that contains the channel where we want
     *            to read the data from; may not be null
     */
    private void read(final SelectionKey key) {
        final var channel = (SocketChannel) key.channel();

        try {
            int read = channel.read(buff);

            if (read < 0) {
                key.cancel();
                LOG.debug("Client says bye! {}", channel.getRemoteAddress());
                key.channel().close();
                return;
            } else {
                LOG.debug("Receiving packet.");
            }

            this.buff.flip();
            final byte[] receivedBytes = new byte[this.buff.limit()];
            this.buff.get(receivedBytes);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Receiving packet: {}", ByteFormatter.formatHexAsString(receivedBytes));
            }

            queue.add(new ChannelPacket(channel, receivedBytes));
        } catch (final IOException ioe) {
            LOG.error("Could not read the stream from channel: {}", channel, ioe);
        } finally {
            buff.clear();
        }
    }

}

