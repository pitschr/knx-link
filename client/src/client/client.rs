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

use std::io::{Read, Write};
use std::net::{IpAddr, SocketAddr, TcpStream};
use std::process::exit;
use std::str::from_utf8;
use std::time::Duration;
use crate::protocol::header::Header;
use std::convert::TryFrom;

pub struct Client;

impl Client {
    pub fn send_bytes(host: IpAddr, port: u16, bytes: Vec<u8>) {
        match TcpStream::connect(SocketAddr::new(host, port)) {
            Ok(mut stream) => {
                stream.set_read_timeout(Some(Duration::from_secs(3))).expect("Could not set read timeout.");
                stream.set_write_timeout(Some(Duration::from_secs(3))).expect("Could not set write timeout.");

                match stream.write(bytes.as_slice()) {
                    Ok(_) => {}
                    Err(e) => {
                        eprintln!("Failed to send data: {:?}", e.kind());
                        exit(30);
                    }
                }

                let mut data = [0 as u8; 255];
                match stream.read(&mut data) {
                    Ok(_) => {
                        let header = Header::try_from(&data)
                            .expect(format!("Could not parse Header from: {:?}", &data).as_str());

                        if header.version() == 1 {
                            // Valid version
                            let body = ResponseBody::try_from(&data[3..3 + header.length()])
                                .expect(format!("Could not parse Response from: {:?}", &data).as_str());

                            body.lastPacket();
                            body.status();
                            body.message();
                            let text = from_utf8(&data).unwrap();
                            if text.starts_with("FAILED") {
                                eprintln!("Got ERR Reply from KNX-Link Server: {}", text);
                                exit(33);
                            } else {
                                println!("Got Reply from KNX-Link Server: '{}'", text);
                            }

                        } else {
                            panic!("Invalid version ({}) received for: {:?}", header.version(), &data);
                        }
                    }
                    Err(e) => {
                        eprintln!("Failed to receive data: {:?}", e.kind());
                        exit(31);
                    }
                }
            }
            Err(e) => {
                eprintln!("Failed to connect to the server: {}", e);
                exit(32);
            }
        }
    }
}
