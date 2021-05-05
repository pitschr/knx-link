use std::collections::HashMap;
use std::convert::TryFrom;
use std::net::IpAddr;
use std::sync::{Arc, Mutex};

use clap::Clap;

use address::group_address::GroupAddress;

mod address;
mod datapoint;
mod protocol_v1;

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

    match opts.subcmd {
        SubCommand::Read(r) => println!("Read: -g {}", r.group_address),
        SubCommand::Write(w) => {
            match w.data {
                Some(data) => println!("Write: -g {} -d {}", w.group_address, data),
                None => println!("Write: -g {}", w.group_address),
            }
        }
    }

    // Gets a value for config if supplied by user, or defaults to "default.conf"
    // println!("Value for group_address: {}", opts.group_address);

    let ga = GroupAddress::try_from("2/8").expect("something went wrong");
    println!("Group Address: {:?}", ga);

    //let version: u8 = 1;
    //let action: u8 = 0; // 0 = read, 1 = write

    //
    // println!("Hello, world!");
    // let start = Instant::now();
    //
    // match TcpStream::connect("localhost:3672") {
    //     Ok(mut stream) => {
    //         eprintln!("Successfully connected to the server");
    //
    //         let msg = b"hello";
    //
    //         stream.write(msg).unwrap();
    //         println!("Sent message, awaiting reply... ");
    //
    //         let mut data = [0 as u8; 255];
    //         match stream.read_exact(&mut data) {
    //             Ok(_) => {
    //                 let text = from_utf8(&data).unwrap();
    //                 println!("Unexpected reply: {}", text);
    //             }
    //             Err(e) => {
    //                 eprintln!("Failed to receive data: {}", e);
    //             }
    //         }
    //     }
    //     Err(e) => {
    //         eprintln!("Failed to connect to the server: {}", e);
    //     }
    // }
    //
    // println!("terminated: {} ms", Instant::now().duration_since(start).as_millis());
}
