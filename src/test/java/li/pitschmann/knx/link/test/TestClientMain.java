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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.link.config.Config;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Test Client for testing manual testing only
 */
public class TestClientMain {
    static {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);
    }

    private static void printHelp() {
        System.out.println("" +
                "---------------------------------------------------" + System.lineSeparator() +
                "Pre-defined Actions:" + System.lineSeparator() +
                "---------------------------------------------------" + System.lineSeparator() +
                "1) 1/2/150 1.001 On                       [WRITE]" + System.lineSeparator() +
                "2) 1/2/150 1.001 Off                      [WRITE]" + System.lineSeparator() +
                "3) 1/2/152 5.001 10                       [WRITE]" + System.lineSeparator() +
                "4) 1/2/152 5.001 50                       [WRITE]" + System.lineSeparator() +
                System.lineSeparator() +
                "5) 1/2/110 1.001 On                       [WRITE]" + System.lineSeparator() +
                "6) 1/2/110 1.001 Off                      [WRITE]" + System.lineSeparator() +
                "7) 1/2/113 1.001                          [READ]" + System.lineSeparator() +
                "8) 1/2/113 99.999                         [READ/RAW]" + System.lineSeparator() +
                System.lineSeparator() +
                "9) 11/3/15 14.056                         [READ]" + System.lineSeparator() +
                "10) 11/3/15 99.999                        [READ/RAW]" + System.lineSeparator() +
                System.lineSeparator() +
                "11) 11/3/10 9.021                         [READ]" + System.lineSeparator() +
                "12) 11/3/0 9.001                          [READ]" + System.lineSeparator() +

                "0) Quit" + System.lineSeparator() +
                "---------------------------------------------------");
    }

    public static void main(String[] args) {
        printHelp();
        final var scanner = new Scanner(System.in);
        try (final var client = TestClient.createStarted(Config.DEFAULT_SERVER_PORT)) {
            loop:
            while (true) {
                Sleeper.seconds(1);
                System.out.print("Enter: ");
                final var line = scanner.nextLine();
                switch (line) {
                    case "1":
                        client.writeRequest("1/2/150", "1.001", "On");
                        break;
                    case "2":
                        client.writeRequest("1/2/150", "1.001", "Off");
                        break;
                    case "3":
                        client.writeRequest("1/2/152", "5.001", "10");
                        break;
                    case "4":
                        client.writeRequest("1/2/152", "5.001", "50");
                        break;
                    case "5":
                        client.writeRequest("1/2/110", "1.001", "On");
                        break;
                    case "6":
                        client.writeRequest("1/2/110", "1.001", "Off");
                        break;
                    case "7":
                        client.readRequest("1/2/113", "1.001");
                        break;
                    case "8":
                        client.readRequest("1/2/113", "99.999");
                        break;
                    case "9":
                        client.readRequest("11/3/15", "14.056");
                        break;
                    case "10":
                        client.readRequest("11/3/15", "99.999");
                        break;
                    case "11":
                        client.readRequest("11/3/10", "9.021");
                        break;
                    case "12":
                        client.readRequest("11/3/0", "9.001");
                        break;
                    case "0":
                        break loop;
                    default:
                        final var splitted = line.split("\\s+");
                        if (splitted.length == 2) {
                            // read request
                            client.readRequest(splitted[0], splitted[1]);
                        } else if (splitted.length == 3) {
                            // write request
                            client.writeRequest(splitted[0], splitted[1], splitted[2]);
                        }
                }
            }
        }
    }
}
