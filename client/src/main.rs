use std::collections::HashMap;
use std::convert::{TryFrom, TryInto};
use std::net::{IpAddr, TcpStream};
use std::sync::{Arc, Mutex};

use clap::Clap;

use address::group_address::GroupAddress;
use crate::protocol_v1::action::Action;
use crate::protocol_v1::protocol::{Protocol, ProtocolError};
use crate::protocol_v1::header::Header;
use std::str::from_utf8;
use std::time::{Instant, Duration};
use std::io::{Write as Wr, Read as Re, Error};
use std::borrow::Borrow;

mod address;
mod datapoint;
pub mod protocol_v1;

#[derive(Clap)]
struct Opts {
    #[clap(short = 'h', long, default_value = "127.0.0.1")]
    host: IpAddr,

    #[clap(short = 'p', long, default_value = "3672")]
    port: u16,

    #[clap(subcommand)]
    subcmd: SubCommand,
}

#[derive(Clap)]
enum SubCommand {
    Read(Read),
    Write(Write),
}

#[derive(Clap)]
struct Read {
    #[clap(short = 'g', long)]
    group_address: String,
}

#[derive(Clap)]
struct Write {
    #[clap(short = 'g', long)]
    group_address: String,

    #[clap(short = 'd', long)]
    data: Option<String>,
}


fn main() {
    let opts: Opts = Opts::parse();
    println!("Host & Port: {}:{}", opts.host, opts.port);

    let mut bytes = vec![];
    match opts.subcmd {
        SubCommand::Read(r) => {
            println!("Read: -g {}", r.group_address);
            match Protocol::as_bytes(Header::new(Action::ReadRequest), r.group_address.as_str(), "1.001", vec![]) {
                Ok(o) => { bytes = o }
                Err(_) => {}
            }
        },
        SubCommand::Write(w) => {
            match w.data {
                Some(data) => {
                    let values = data.split(" ").collect();
                    match Protocol::as_bytes(Header::new(Action::WriteRequest), w.group_address.as_str(), "1.001", values) {
                        Ok(o) => { bytes = o }
                        Err(_) => {}
                    }
                    println!("Write: -g {} -d {}", w.group_address, data)
                },
                None => println!("Write: -g {}", w.group_address),
            }
        }
    }

    if bytes.len() > 0 {
        println!("Bytes: {:?}", bytes);
    } else {
        println!("No Bytes!")
    }

    match TcpStream::connect("localhost:3672") {
        Ok(mut stream) => {
            stream.set_read_timeout(Some(Duration::from_secs(3)));
            stream.set_write_timeout(Some(Duration::from_secs(3)));
            eprintln!("Successfully connected to the server");

            match stream.write(bytes.as_slice()) {
                Ok(_) => println!("Sent message, awaiting reply... "),
                Err(e) => {
                    eprintln!("Failed to send data: {:?}", e.kind());
                    panic!("Failed")
                },
            }


            let mut data = [0 as u8; 255];
            match stream.read(&mut data) {
                Ok(_) => {
                    let text = from_utf8(&data).unwrap();
                    println!("Reply: '{}'", text);
                }
                Err(e) => {
                    eprintln!("Failed to receive data: {:?}", e.kind());
                    panic!("Failed")
                }
            }
        }
        Err(e) => {
            eprintln!("Failed to connect to the server: {}", e);
        }
    }
}
