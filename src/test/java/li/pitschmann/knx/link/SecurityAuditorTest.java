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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SecurityAuditor}
 */
class SecurityAuditorTest {

    @Test
    @DisplayName("#isRemoteAddressValid(SocketChannel): 127.0.0.1, in whitelist")
    void test_remoteAddress_127_0_0_1() throws IOException {
        final var guarder = new SecurityAuditor();

        final var socketChannelMock = mock(SocketChannel.class);
        final var socketAddressMock = mock(InetSocketAddress.class);
        final var inetAddressMock = mock(InetAddress.class);

        when(socketChannelMock.getRemoteAddress()).thenReturn(socketAddressMock);
        when(socketAddressMock.getAddress()).thenReturn(inetAddressMock);
        when(inetAddressMock.getHostAddress()).thenReturn("127.0.0.1");

        assertThat(guarder.isRemoteAddressValid(socketChannelMock)).isTrue();
    }

    @Test
    @DisplayName("#isRemoteAddressValid(SocketChannel): 10.0.0.1, not in whitelist")
    void test_remoteAddress_10_0_0_1() throws IOException {
        final var guarder = new SecurityAuditor();

        final var socketChannelMock = mock(SocketChannel.class);
        final var socketAddressMock = mock(InetSocketAddress.class);
        final var inetAddressMock = mock(InetAddress.class);

        when(socketChannelMock.getRemoteAddress()).thenReturn(socketAddressMock);
        when(socketAddressMock.getAddress()).thenReturn(inetAddressMock);
        when(inetAddressMock.getHostAddress()).thenReturn("10.0.0.1");

        assertThat(guarder.isRemoteAddressValid(socketChannelMock)).isFalse();
    }

    @Test
    @DisplayName("#isRemoteAddressValid(SocketChannel): IOException")
    void test_remoteAddress_IOException() throws IOException {
        final var guarder = new SecurityAuditor();

        final var socketChannelMock = mock(SocketChannel.class);
        when(socketChannelMock.getRemoteAddress()).thenThrow(new IOException());

        assertThat(guarder.isRemoteAddressValid(socketChannelMock)).isFalse();
    }
}
