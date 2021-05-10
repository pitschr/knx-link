[![Build Status](https://github.com/pitschr/knx-link/workflows/build/badge.svg?branch=main)](https://github.com/pitschr/knx-link/actions)
[![Coverage Status](https://coveralls.io/repos/github/pitschr/knx-link/badge.svg?branch=main)](https://coveralls.io/github/pitschr/knx-link?branch=main)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

# KNX Link

The KNX Link establish a connection to your KNX Net/IP device using the [knx-core](https://github.com/pitschr/knx-core) 
library and acts as a KNX gateway. The KNX Link project is based on two components: **KNX Link Server** and 
**KNX Link Client**.

The purpose of the *KNX Link Server* is to serve the requests from *KNX Link Client* and communicates
with the KNX Net/IP device with almost no delay. This is achieved by avoiding establishing a connection
to the KNX Net/IP device as a long-running `systemd` service and keeps the connection established. In 
this way we can avoid spending up to few seconds per connection ramp ups and tear downs.

The *KNX Link Client* is a lightweight commandline tool with a short life-cycle and comes with a much 
simplified protocol which has been designed for communication between the *KNX Link Server* and the
*KNX Link Client*. The communication between those parties are done via TCP socket as illustrated below.

![Architecture](./assets/readme_architecture.png)

## Prerequisites

* **Host**
    * A dedicated machine based on Linux (e.g. CentOS)
    * [curl](https://man7.org/linux/man-pages/man1/curl.1.html) for downloading the files for installation
    * [systemctl](https://man7.org/linux/man-pages/man1/systemctl.1.html) for setup for systemd service
* **Java 11+**
    * Make sure that you have Java 11+ installed and running as Java 11+

## KNX Link Server

#### How to install KNX Link Server?

Execute and follow the instructions:
```
bash <(curl -s https://raw.githubusercontent.com/pitschr/knx-link/main-initial-commit/install.sh)
```

What it is doing? It is downloading the file [install.sh](./install.sh) I prepared for you and will prompt
guide you through the installation and asking some questions which you may reply with 'yes' or 'no'. 

In nutshell, the steps of [install.sh](./install.sh) script are:
1. Creating a technical user `knx` for systemd service
1. Downloading the `knx-link-<version>.jar` file to folder `/opt/knx-link-<version>`
1. Install `knx.service` to start and keep KNX Link server running (e.g. after reboot)
1. (Optional) Create a `knx` firewall rule for your `firewalld`, which opens the UDP port `3671` which is 
   registered for KNX communication. Opens UDP ports `40001` (for Description Channel), `40002` 
   (for Control Channel) and `40003` (for Data Channel) which are required for communication without NAT.
1. Check if the systemd service is running 
1. (Optional) Check if the port of KNX Link server is open

#### How to start, stop or restart KNX Link Server?

After installation, the *KNX Link Server* is installed as a fully managed service by the `systemd` daemon 
and executed as technical user (`knx`). It is a long-running process and in normal reasons there is no 
need to start or stop it by yourself. However, in special cases you need to start/stop/restart the 
*KNX Link Server*; i.e. when you change the server configuration file (`server.cfg`, see below) and apply 
the change immediately.

| Action      | Command |
| ----------- | -------------------------------------- |
| **Start**   | `systemctl start knx.service`          |
| **Stop**    | `systemctl stop knx.service`           |
| **Restart** | `systemctl restart knx.service`        |

For further info, please check out the [systemctl man page](https://man7.org/linux/man-pages/man1/systemctl.1.html).

#### KNX Link Server Configuration

The configuration for KNX Link server is `server.cfg` and in folder `/opt/knx-link-<version>/server.cfg`. 

By default, no `server.cfg` file exists which means the KNX Link Server will use default values. 
This is suitable for many cases, however, if a different configuration is required, then please 
go to folder `/opt/knx-link-<version>/server.cfg` and create a file `server.cfg` and apply the 
config key and values according your needs. 

| Config Key                 | Default&nbsp;Value                 | Description |
| -------------------------- | ---------------------------------- | ----------- |
| `knx.mode`                 | `tunneling`                        | Defines the mode of communication how the KNX Link server should communicate with the KNX Net/IP device.<br><br>**Allowed values:**<br>`tunneling` or `routing` |
| `knx.nat`                  | `false`                            | Defines if the Network Address Translation (NAT) should be used. NAT is can be used for `tunneling` mode only. If `routing` mode is used then it has no effect and is ignored.<br><br>**Allowed values:**<br>`false` or `true` |
| `knx.address`              | `0.0.0.0`<br>(Auto&nbsp;Discovery) | If your KNX Net/IP device has a static IP address you can set the IP address that allows a faster start-up as it will skip the auto-discovery process. This setting might be also useful if you have more than one KNX Net/IP device and you want to specific one, otherwise the auto-discovery will choose a KNX Net/IP device in behalf of you otherwise.<br><br>**Allowed Pattern:**<br>`[0-255].[0-255].[0-255].[0-255]` | 
| `knx.port`                 | `3671`                             | Set only if your KNX Net/IP device is using a port number that differs from the officially registered KNX port `3671` at IANA.<br>If 'knx.address' is 'auto', then KNX port has no effect and will be ignored.<br><br>**Allowed Port Range:**<br>`1024 - 65535` | 
| `server.port`              | `3672`                             | Set only if you want to use a different port for your KNX Link server that opens a server socket channel for your clients. This is not the port communicating with your KNX Net/IP device.<br><br>**Allowed Port Range:**<br>`1024 - 65535` |
| `server.allowed.addresses` | `127.0.0.1`<br>(localhost)         | This setting is used to accept requests from your clients that are from a trusted IP address. Default is `127.0.0.1` which means it will only accept requests which are originated from the same machine. You can define multiple IP addresses, define them comma-separated. Example: `10.0.1.2,192.168.1.4,192.168.2.8`.<br><br>**Allowed Pattern:**<br>`[0-255].[0-255].[0-255].[0-255]` |


## How to install the KNX Link Client?

TODO

