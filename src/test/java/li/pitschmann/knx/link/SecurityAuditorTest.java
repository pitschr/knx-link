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

import li.pitschmann.knx.link.protocol.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;

import static li.pitschmann.knx.link.test.Helper.createChannelPacketMock;
import static li.pitschmann.knx.link.test.Helper.verifyChannelPackets;
import static li.pitschmann.knx.link.test.Helper.verifyNoChannelPackets;
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
        final var auditor = new SecurityAuditor(Set.of());

        // 127.0.0.1 should be always whitelisted
        final var socketChannelPacketMock = createChannelPacketMock();
        when(((InetSocketAddress) socketChannelPacketMock.getChannel().getRemoteAddress()).getAddress().getHostAddress())
                .thenReturn("127.0.0.1"); // simulate the request coming from client with ip address 10.0.0.1

        assertThat(auditor.isRemoteAddressValid(socketChannelPacketMock.getChannel())).isTrue();
        verifyNoChannelPackets(socketChannelPacketMock);
    }

    @Test
    @DisplayName("#isRemoteAddressValid(SocketChannel): 198.168.0.1, in whitelist")
    void test_remoteAddress_198_168_0_1() throws IOException {
        final var auditor = new SecurityAuditor(Set.of("198.168.0.1"));

        // 127.0.0.1 should be always whitelisted
        final var socketChannelPacketMock = createChannelPacketMock();
        when(((InetSocketAddress) socketChannelPacketMock.getChannel().getRemoteAddress()).getAddress().getHostAddress())
                .thenReturn("198.168.0.1");

        assertThat(auditor.isRemoteAddressValid(socketChannelPacketMock.getChannel())).isTrue();
        verifyNoChannelPackets(socketChannelPacketMock);
    }

    @Test
    @DisplayName("#isRemoteAddressValid(SocketChannel): 10.0.0.1, not in whitelist")
    void test_remoteAddress_10_0_0_1() throws IOException {
        final var auditor = new SecurityAuditor(Set.of("192.168.0.111"));

        final var socketChannelPacketMock = createChannelPacketMock();
        when(((InetSocketAddress) socketChannelPacketMock.getChannel().getRemoteAddress()).getAddress().getHostAddress())
                .thenReturn("10.0.0.1"); // simulate the request coming from client with ip address 10.0.0.1

        assertThat(auditor.isRemoteAddressValid(socketChannelPacketMock.getChannel())).isFalse();

        final var expectedResponseBodies = List.of(
                ResponseBody.of(true, Status.ERROR_CLIENT_NOT_AUTHORIZED,
                        "Your IP Address '10.0.0.1' is not whitelisted. To add your IP Address to whitelist, add " +
                                "it to the property 'server.allowed.addresses' of your 'server.cfg' file.")
        );
        verifyChannelPackets(socketChannelPacketMock, expectedResponseBodies);
    }

    @Test
    @DisplayName("#isRemoteAddressValid(SocketChannel): IOException")
    void test_remoteAddress_IOException() throws IOException {
        final var auditor = new SecurityAuditor(Set.of());

        final var socketChannelMock = mock(SocketChannel.class);
        when(socketChannelMock.getRemoteAddress()).thenThrow(new IOException());

        assertThat(auditor.isRemoteAddressValid(socketChannelMock)).isFalse();
    }

    @Test
    @DisplayName("#toString()")
    void testToString() {
        final var auditor = new SecurityAuditor(Set.of("1.2.3.4"));

        assertThat(auditor).hasToString(
                "SecurityAuditor{allowedAddresses=[1.2.3.4]}"
        );
    }
}
