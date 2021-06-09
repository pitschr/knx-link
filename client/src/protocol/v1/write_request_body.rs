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

use crate::address::group_address::{GroupAddress, GroupAddressBytes};
use crate::datapoint::datapoint::DataPoint;
use crate::protocol::action::Action;
use crate::protocol::header::Header;
use crate::protocol::v1::protocol::{Protocol, ProtocolError};
use std::error::Error;

pub struct WriteRequestBody {}

impl WriteRequestBody {
    pub fn as_bytes(group_address: &str, datapoint: &str, values: Vec<&str>) -> Result<Vec<u8>, Box<dyn Error>> {
        //
        // Body
        //
        let mut body = Vec::<u8>::with_capacity(250);

        // Group Address (2 bytes)
        match GroupAddress::try_from(group_address) {
            Ok(ga) => {
                for i in ga.as_bytes().iter() {
                    body.push(*i);
                }
            }
            Err(e) => {
                return Err(Box::new(e));
            }
        }

        // Data Point (4 bytes)
        match DataPoint::try_from(datapoint) {
            Ok(dpt) => {
                for i in dpt.as_bytes().iter() {
                    body.push(*i);
                }
            }
            Err(e) => {
                return Err(Box::new(e));
            }
        }

        // Values
        for i in Protocol::convert_values_to_utf8_bytes(values).iter() {
            body.push(*i);
        }

        //
        // Header
        //
        let header = Header::new(1, Action::WriteRequest, body.len() as u8).as_bytes();

        //
        // Glue Header + Body
        //
        let mut ve = Vec::<u8>::with_capacity(255);
        for i in &header {
            ve.push(*i);
        }
        for i in &body {
            ve.push(*i);
        }

        Ok(ve)
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_write_request() {
        assert_eq!(WriteRequestBody::as_bytes("1/2/3", "4711.32109", vec!["Hello", "World"]).unwrap(),
                   [
                       0x01,                                           // Protocol Version
                       0x01,                                           // Write Request
                       0x15,                                           // Body Length (21 octets)
                       0x0A, 0x03,                                     // Group Address
                       0x12, 0x67, 0x7D, 0x6D,                         // Data Point Type
                       b'"', b'H', b'e', b'l', b'l', b'o', b'"',       // Values (within double-quote characters)
                       b' ',                                           // (Space)
                       b'"', b'W', b'o', b'r', b'l', b'd', b'"',       // Values, continued
                   ]
        );
    }
}
