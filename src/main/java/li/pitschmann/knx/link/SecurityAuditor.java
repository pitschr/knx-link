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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Objects;

/**
 * Security Auditor
 *
 * <p> Guards the KNX server with some actions like if connected clients are acceptable.
 */
public final class SecurityAuditor {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityAuditor.class);
    private static final String[] DEFAULT_IP_ADDRESS_WHITELIST = new String[]{"127.0.0.1"};
    private final String[] ipAddressWhitelist;

    public SecurityAuditor() {
        this(DEFAULT_IP_ADDRESS_WHITELIST);
    }

    public SecurityAuditor(final String[] ipAddressWhitelist) {
        this.ipAddressWhitelist = Objects.requireNonNull(ipAddressWhitelist);
        LOG.info("Whitelist IP Addresses: {}", (Object) this.ipAddressWhitelist); // pass as an object, not as an array to vargs
    }

    /**
     * Checks if the {@link SocketChannel} is coming from a valid remote address.
     * The remote address from {@link SocketChannel} will be compared against the
     * {@link #DEFAULT_IP_ADDRESS_WHITELIST}.
     *
     * <p> If the remote address could not be read for any reasons
     * (e.g. due {@link IOException}), then {@code false} is returned.
     *
     * @param socketChannel the socket channel to be checked; may not be null
     * @return {@code true} if the socket channel is valid, {@code false} otherwise
     */
    public boolean isRemoteAddressValid(final SocketChannel socketChannel) {
        try {
            final var socketAddress = socketChannel.getRemoteAddress();
            if (socketAddress instanceof InetSocketAddress) {
                final var inetSocketAddress = (InetSocketAddress) socketAddress;
                final var ipAddress = inetSocketAddress.getAddress().getHostAddress();

                for (var validAddress : ipAddressWhitelist) {
                    if (validAddress.equals(ipAddress)) {
                        LOG.debug("Socket channel IP address found in whitelist: {}", ipAddress);
                        return true;
                    }
                }
                LOG.warn("Socket channel IP address not found in whitelist: {}", ipAddress);
            }
        } catch (final IOException e) {
            LOG.error("Could not validate the IP address of socket channel: {}", socketChannel, e);
        }
        return false;
    }
}
