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

import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.core.datapoint.DPT1;
import li.pitschmann.knx.core.datapoint.DPT11;
import li.pitschmann.knx.core.datapoint.DPT14;
import li.pitschmann.knx.core.datapoint.DPT19;
import li.pitschmann.knx.core.datapoint.DPT2;
import li.pitschmann.knx.core.datapoint.DPT28;
import li.pitschmann.knx.core.datapoint.DPTRaw;
import li.pitschmann.knx.core.datapoint.value.DPT19Value;
import li.pitschmann.knx.link.config.Config;
import li.pitschmann.knx.link.protocol.ResponseBody;
import li.pitschmann.knx.link.test.TestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static li.pitschmann.knx.link.test.Helper.createConfigMock;
import static li.pitschmann.knx.link.test.Helper.createKnxClientMock;
import static li.pitschmann.knx.link.test.Helper.createKnxStatusDataMock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link DefaultServer}
 */
class DefaultServerTest {
    private TestClient client;
    private KnxClient knxClientMock;
    private DefaultServer server;

    @BeforeEach
    void setUp() {
        knxClientMock = createKnxClientMock();
        server = spy(DefaultServer.createUnstarted(createConfigMock()));
        // as we don't want to communicate with real KNX Net/IP device
        doReturn(knxClientMock).when(server).getKnxClient();
        server.start();
        client = spy(TestClient.createStarted(Config.DEFAULT_SERVER_PORT));
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.close();
    }

    @Test
    @DisplayName("Read Request with DPT 1.004 - Ramp")
    void test_Server_ReadRequest_DPT1() {
        createKnxStatusDataMock(knxClientMock, DPT1.RAMP.of(true));

        client.readRequest("8/3/250", "1.004");

        final var expectedGroupAddress = GroupAddress.of(8, 3, 250);
        verify(knxClientMock, timeout(1000))
                .readRequest(expectedGroupAddress);
        verify(knxClientMock.getStatusPool(), timeout(1000))
                .getStatusFor(expectedGroupAddress);

        client.verifyReceivedResponses(
                ResponseBody.of(false, Status.SUCCESS),
                ResponseBody.of(true, Status.SUCCESS, "Ramp")
        );
    }

    @Test
    @DisplayName("Read Request with DPT 11.001 - Date")
    void test_Server_ReadRequest_DPT11() {
        createKnxStatusDataMock(knxClientMock, DPT11.DATE.of(LocalDate.of(2021, 3, 28)));

        client.readRequest("1/2/3", "11.001");

        final var expectedGroupAddress = GroupAddress.of(1, 2, 3);
        verify(knxClientMock, timeout(1000))
                .readRequest(expectedGroupAddress);
        verify(knxClientMock.getStatusPool(), timeout(1000))
                .getStatusFor(expectedGroupAddress);

        client.verifyReceivedResponses(
                ResponseBody.of(false, Status.SUCCESS),
                ResponseBody.of(true, Status.SUCCESS, "2021-03-28")
        );
    }

    @Test
    @DisplayName("Read Request with unsupported DPT")
    void test_Server_ReadRequest_UnsupportedDPT() {
        createKnxStatusDataMock(knxClientMock, DPTRaw.VALUE.of((byte) 0x12, (byte) 0x67));

        client.readRequest("1/2/3", "99.9999");

        final var expectedGroupAddress = GroupAddress.of(1, 2, 3);
        verify(knxClientMock, timeout(1000))
                .readRequest(expectedGroupAddress);
        verify(knxClientMock.getStatusPool(), timeout(1000))
                .getStatusFor(expectedGroupAddress);

        client.verifyReceivedResponses(
                ResponseBody.of(false, Status.SUCCESS),
                ResponseBody.of(true, Status.SUCCESS, "0x12 67")
        );
    }

    @Test
    @DisplayName("Write Request: DPT 1.001 - Switch 'On'")
    void test_Server_WriteRequest_DPT1() {
        client.writeRequest("1/2/150", "1.001", "on");

        verify(knxClientMock, timeout(1000))
                .writeRequest(
                        eq(GroupAddress.of(1, 2, 150)),
                        eq(DPT1.SWITCH.of(true))
                );

        client.verifyReceivedResponses(
                ResponseBody.of(true, Status.SUCCESS)
        );
    }

    @Test
    @DisplayName("Write Request: DPT 2.001 - Alarm Control 'No Alarm, Not Controlled'")
    void test_Server_WriteRequest_DPT2() {
        client.writeRequest("12/1/33", "2.005", "No Alarm", "Controlled");

        verify(knxClientMock, timeout(1000))
                .writeRequest(
                        eq(GroupAddress.of(12, 1, 33)),
                        eq(DPT2.ALARM_CONTROL.of(true, false))
                );

        client.verifyReceivedResponses(
                ResponseBody.of(true, Status.SUCCESS)
        );
    }

    @Test
    @DisplayName("Write Request: DPT 14.1200 - '1234.567'")
    void test_Server_WriteRequest_DPT14() {
        client.writeRequest("13/3/170", "14.1200", "1234.567");

        verify(knxClientMock, timeout(1000))
                .writeRequest(
                        eq(GroupAddress.of(13, 3, 170)),
                        eq(DPT14.VOLUME_FLUX_METER.of(1234.567d))
                );

        client.verifyReceivedResponses(
                ResponseBody.of(true, Status.SUCCESS)
        );
    }

    @Test
    @DisplayName("Write Request: DPT 19.001 - Date & Time")
    void test_Server_WriteRequest_DPT19() {
        client.writeRequest("14/2/90", "19.001", "21:04:51", "SuNDaY", "2021-03-25", "0x0380");

        verify(knxClientMock, timeout(1000))
                .writeRequest(
                        eq(GroupAddress.of(14, 2, 90)),
                        eq(DPT19.DATE_TIME.of(
                                DayOfWeek.SUNDAY,
                                LocalDate.of(2021, 3, 25),
                                LocalTime.of(21, 4, 51),
                                new DPT19Value.Flags(new byte[]{0x03, (byte) 0x80})
                        ))
                );

        client.verifyReceivedResponses(
                ResponseBody.of(true, Status.SUCCESS)
        );
    }

    @Test
    @DisplayName("Write Request: DPT 28.001 - UTF8 Text")
    void test_Server_WriteRequest_DPT28() {
        client.writeRequest("1/2/3", "28.001", "Hällö 読継 食う न्त्राल яуи δολ 123");

        verify(knxClientMock, timeout(1000))
                .writeRequest(
                        GroupAddress.of(1, 2, 3),
                        DPT28.UTF_8.of("Hällö 読継 食う न्त्राल яуи δολ 123")
                );

        client.verifyReceivedResponses(
                ResponseBody.of(true, Status.SUCCESS)
        );
    }
}
