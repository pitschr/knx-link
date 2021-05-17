use std::net::IpAddr;
use std::process::exit;

use clap::Clap;

use client::client::Client;
use protocol::action::Action;
use protocol::v1::protocol::Protocol;

mod address;
mod datapoint;
mod client;
mod protocol;

#[derive(Clap)]
#[clap(name = "KNX Client", about = "A client to send/receive KNX commands to/from KNX-Link Server", version = "0.1")]
struct Opts {
    #[clap(short = 'h', long, default_value = "127.0.0.1")]
    #[clap(about = "Defines the hostname of the KNX-Link Server")]
    host: IpAddr,

    #[clap(short = 'p', long, default_value = "3672")]
    #[clap(about = "Defines the port of the KNX-Link Server")]
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
#[clap(about = "Sends a read request to KNX-Link Server")]
struct Read {
    #[clap(short = 'g', long)]
    #[clap(about = "The KNX Group Address that should receive the read request\n\
                    Supported formats: Free-Level (#), Two-Level (#/#) or Three-Level (#/#/#)\n\
                    Examples: 12345, 7/1234, 1/2/110")]
    group_address: String,

    #[clap(short = 'd', long)]
    #[clap(about = "Data Point Type that should be returned by KNX-Link Server\n\
                    Supported formats: #, #.#, dpt-#, dpst-#-#\n\
                    Examples: 1, 1.001, dpt-1, dpst-1-1")]
    data_point_type: String,
}

#[derive(Clap)]
#[clap(about = "Sends a write request to KNX-Link Server")]
struct Write {
    #[clap(short = 'g', long)]
    #[clap(about = "The KNX Group Address that should receive the write request\n\
                    Supported formats: Free-Level (#), Two-Level (#/#) or Three-Level (#/#/#)\n\
                    Examples: 12345, 7/1234, 1/2/110")]
    group_address: String,

    #[clap(short = 'd', long)]
    #[clap(about = "Data Point Type that should be interpreted by KNX-Link Server\n\
                    Supported formats: #, #.#, dpt-#, dpst-#-#\n\
                    Examples: 1, 1.001, dpt-1, dpst-1-1")]
    data_point_type: String,

    #[clap(short = 'v', long)]
    #[clap(about = "The value of Data Point Type to be sent to KNX-Link Server\n\
                    Supported formats depends on the given Data Point Type\n\n\
                    Examples for DPT-1:  on, off, true, false, 1, 0\n\
                    Examples for DPT-3:  stop, \"controlled stop\"\n\
                    Examples for DPT-5:  1, 10, 20\n\
                    Examples for DPT-9:  1.23, 123.456\n\
                    Examples for DPT-10: 00:00:00, 12:34:56\n\
                    Examples for DPT-11: 2000-01-02, 2050-03-04\n\
                    Examples for DPT-13: 1234, -1234\n\
                    Examples for DPT-16: \"Hello World\"")]
    value: String,
}


fn main() {
    let opts: Opts = Opts::parse();

    match opts.subcmd {
        SubCommand::Read(r) => {
            match Protocol::as_bytes(
                Action::ReadRequest,
                r.group_address.as_str(),
                r.data_point_type.as_str(),
                vec![],
            ) {
                Ok(bytes) => {
                    Client::send_bytes(opts.host, opts.port, bytes)
                }
                Err(err) => {
                    eprintln!("Error during read request. Error: {:?}", err);
                    exit(10);
                }
            }
        }
        SubCommand::Write(w) => {
            match Protocol::as_bytes(
                Action::WriteRequest,
                w.group_address.as_str(),
                w.data_point_type.as_str(),
                w.value.split(" ").collect(),
            ) {
                Ok(bytes) => {
                    Client::send_bytes(opts.host, opts.port, bytes)
                }
                Err(err) => {
                    eprintln!("Error during write request. Error: {:?}", err);
                    exit(20);
                }
            }
        }
    }

    ;
}
