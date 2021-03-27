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
import li.pitschmann.knx.core.datapoint.DPT14;
import li.pitschmann.knx.core.datapoint.DPT19;
import li.pitschmann.knx.core.datapoint.DPT2;
import li.pitschmann.knx.core.datapoint.DPT28;
import li.pitschmann.knx.core.datapoint.value.DPT19Value;
import li.pitschmann.knx.core.datapoint.value.DataPointValue;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.test.SocketClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link Server}
 */
class ServerTest {
    private SocketClient client;
    private KnxClient knxClientMock;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        knxClientMock = mock(KnxClient.class);
        when(knxClientMock.readRequest(any(GroupAddress.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(knxClientMock.writeRequest(any(GroupAddress.class), any(DataPointValue.class))).thenReturn(CompletableFuture.completedFuture(true));
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Server(knxClientMock));
        client = SocketClient.createStarted(10222);
    }

    @AfterEach
    void tearDown() {
        client.close();
        executorService.shutdownNow();
    }

    @Test
    @DisplayName("Read Request")
    void test_Server_ReadRequest() {
        client.readRequest("8/3/250", "1.004");

        verify(knxClientMock, timeout(1000))
                .readRequest(
                        eq(GroupAddress.of(8,3,250))
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
                                new DPT19Value.Flags(new byte[]{ 0x03, (byte) 0x80})
                        ))
                );
    }

    @Test
    @DisplayName("Write Request: DPT 28.001 - UTF8 Text")
    void test_Server_WriteRequest_DPT28() {
        client.writeRequest("1/2/3", "28.001", "Hällö 読継 食う न्त्राल яуи δολ 123");

        verify(knxClientMock, timeout(1000))
                .writeRequest(
                        eq(GroupAddress.of(1,2,3)),
                        eq(DPT28.UTF_8.of("Hällö 読継 食う न्त्राल яуи δολ 123"))
                );
    }
}
