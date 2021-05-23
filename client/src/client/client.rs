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

use std::convert::TryFrom;
use std::io::{Read, Write};
use std::net::{IpAddr, SocketAddr, TcpStream};
use std::process::exit;
use std::time::Duration;

use crate::protocol::header::Header;
use crate::protocol::status::Status;
use crate::protocol::v1 as protocol_v1;

pub struct Client;

impl Client {
    fn stream_write(stream: &mut TcpStream, bytes: &[u8]) {
        match stream.write(bytes) {
            Ok(_) => {}
            Err(e) => {
                eprintln!("Failed to send data: {:?}", e.kind());
                exit(30);
            }
        }
    }

    fn stream_read(stream: &mut TcpStream) {
        let mut data = [0 as u8; 255];
        match stream.read(&mut data) {
            Ok(_) => {
                let header = Header::try_from(&data)
                    .expect(format!("Could not parse Header from: {:?}", &data).as_str());

                if header.version() == 1 {
                    // Convert to ResponseBody V1
                    let body = protocol_v1::response_body::ResponseBody::try_from(&data[3..3 + header.length() as usize])
                        .expect(format!("Could not parse Response from: {:?}", &data).as_str());

                    if body.status() == Status::Success {
                        println!("body.lastPacket(): {}", body.last_packet());
                        println!("body.status(): {:?}", body.status());
                        println!("body.data(): {:?}", body.data());
                        println!("body.message(): {:?}", body.message());
                    }

                    if !body.last_packet() {
                        Self::stream_read(stream);
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

    pub fn send_bytes(host: IpAddr, port: u16, bytes: Vec<u8>) {
        println!("PITSCHR: {:?}", bytes);
        match TcpStream::connect(SocketAddr::new(host, port)) {
            Ok(mut stream) => {
                stream.set_read_timeout(Some(Duration::from_secs(3))).expect("Could not set read timeout.");
                stream.set_write_timeout(Some(Duration::from_secs(3))).expect("Could not set write timeout.");

                Self::stream_write(&mut stream, bytes.as_slice());

                Self::stream_read(&mut stream);
            }
            Err(e) => {
                eprintln!("Failed to connect to the server: {}", e);
                exit(32);
            }
        }
    }
}
