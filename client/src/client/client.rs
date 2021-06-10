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

use colored::Colorize;

use crate::protocol::header::Header;
use crate::protocol::status::Status;
use crate::protocol::v1 as protocol_v1;

pub struct Client;

impl Client {
    fn stream_write(stream: &mut TcpStream, bytes: &[u8]) {
        match stream.write(bytes) {
            Ok(_) => {}
            Err(e) => {
                eprintln!("{}",
                          format!("{} Could not send data to {}. Error: {}", "[ERROR]".bold(), stream.peer_addr().unwrap(), e).as_str().red()
                );
                exit(30);
            }
        }
    }

    fn stream_read(stream: &mut TcpStream) {
        let mut data = [0 as u8; 255];
        match stream.read(&mut data) {
            Ok(size) => {
                let header;
                match Header::try_from(&data) {
                    Ok(h) => {
                        if h.version() != 1 {
                            eprintln!("{}",
                                      format!("{} Unsupported header '{}'. Data: {:?}", "[ERROR]".bold(), h.version(), &data[..size]).as_str().red()
                            );
                            exit(40);
                        }
                        header = h
                    }
                    Err(_) => {
                        eprintln!("{}",
                                  format!("{} Could not parse header. Data: {:?}", "[ERROR]".bold(), &data[..size]).as_str().red()
                        );
                        exit(41);
                    }
                }

                // Convert to ResponseBody V1
                let body_data = &data[3..3 + header.length() as usize];
                let body;
                match protocol_v1::response_body::ResponseBody::try_from(body_data) {
                    Ok(b) => {
                        if b.status() != Status::Success {
                            match b.message() {
                                Ok(m) => {
                                    let message = if m.is_empty() { "<no message>" } else { m };
                                    eprintln!("{}",
                                              String::from(
                                                  format!("{} ({:?}): {}", "[ERROR]".bold(), b.status(), message).as_str()
                                              ).red()
                                    );
                                    exit(42);
                                }
                                Err(_) => {
                                    eprintln!("{}",
                                              String::from(
                                                  format!("{} ({:?}): Error decoding failure message. Data: {:?}", "[ERROR]".bold(), b.status(), &body_data).as_str()
                                              ).red()
                                    );
                                    exit(43);
                                }
                            }
                        }
                        body = b
                    }
                    Err(_) => {
                        eprintln!("{}",
                                  format!("{} Could not parse response. Data: {:?}", "[ERROR]".bold(), &data[..size]).as_str().red()
                        );
                        exit(44);
                    }
                }

                match body.message() {
                    Ok(m) => {
                        if !m.is_empty() {
                            println!("{} {}", "[SUCCESS]".green().bold(), m);
                        }
                    }
                    Err(_) => {
                        eprintln!("{}",
                                  format!("{} Could not decode success message. Data: {:?}", "[ERROR]".bold(), &data[..size]).as_str().red()
                        );
                        exit(45);
                    }
                }

                if !body.last_packet() {
                    Self::stream_read(stream);
                }
            }
            Err(e) => {
                eprintln!("Failed to receive data: {:?}", e.kind());
                exit(46);
            }
        }
    }

    pub fn send_bytes(host: IpAddr, port: u16, bytes: Vec<u8>) {
        let remote_addr = SocketAddr::new(host, port);
        match TcpStream::connect(&remote_addr) {
            Ok(mut stream) => {
                stream.set_read_timeout(Some(Duration::from_secs(5))).expect("Could not set read timeout.");
                stream.set_write_timeout(Some(Duration::from_secs(5))).expect("Could not set write timeout.");

                // send packets to KNX Link Server
                Self::stream_write(&mut stream, bytes.as_slice());
                // read packets from KNX Link Server
                Self::stream_read(&mut stream);
            }
            Err(e) => {
                let mut err_msg = String::from("[ERROR] ".bold().to_string());
                if e.kind() == std::io::ErrorKind::ConnectionRefused {
                    err_msg.push_str(format!("Failed to connect to KNX Link Server. Is KNX Link Server alive at {}?", &remote_addr).as_str());
                    eprintln!("{}", err_msg.as_str().red());
                    exit(50);
                } else {
                    err_msg.push_str(format!("Failed to connect to KNX Link Server. Error: {}", e).as_str());
                    eprintln!("{}", err_msg.as_str().red());
                    exit(51);
                }
            }
        }
    }
}
