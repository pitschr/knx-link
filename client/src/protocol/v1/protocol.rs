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

#[derive(Debug)]
pub struct ProtocolError;

pub struct Protocol;

impl Protocol {
    pub fn as_bytes(action: Action, group_address: &str, datapoint: &str, values: Vec<&str>) -> Result<Vec<u8>, ProtocolError> {
        //
        // Body
        //
        let mut body = Vec::<u8>::with_capacity(250);

        // Group Address (2 bytes)
        for i in GroupAddress::try_from(group_address).unwrap().as_bytes().iter() {
            body.push(*i);
        }

        // Data Point (4 bytes)
        for i in DataPoint::try_from(datapoint).unwrap().as_bytes().iter() {
            body.push(*i);
        }

        // Values
        // * Read = 0 bytes
        // * Write = N bytes
        if let Action::WriteRequest = action {
            for i in Protocol::convert_values_to_utf8_bytes(values).iter() {
                body.push(*i);
            }
        }

        //
        // Header
        //
        let header = Header::new(1, action, body.len() as u8).as_bytes();

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

    fn convert_values_to_utf8_bytes(values: Vec<&str>) -> Vec<u8> {
        let mut ve = Vec::with_capacity(255);
        let mut count = 0_u8;
        for value in values {
            if count > 0 {
                ve.push(0x20); // space
            }
            ve.push(0x22); // "
            for byte in value.bytes() {
                ve.push(byte);
            }
            ve.push(0x22); // "
            count += 1;
        }
        ve
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_read_request() {
        assert_eq!(Protocol::as_bytes(Action::ReadRequest, "1/2/3", "4711.32109", vec![]).unwrap(),
                   [
                       0x01,                       // Protocol Version
                       0x00,                       // Read Request
                       0x06,                       // Body Length (6 octets)
                       0x0A, 0x03,                 // Group Address
                       0x12, 0x67, 0x7D, 0x6D      // Data Point Type
                   ]
        );
    }

    #[test]
    fn test_write_request() {
        assert_eq!(Protocol::as_bytes(Action::WriteRequest, "1/2/3", "4711.32109", vec!["Hello", "World"]).unwrap(),
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

    #[test]
    fn test_convert_empty() {
        assert_eq!(Protocol::convert_values_to_utf8_bytes(vec![]), []);
    }

    #[test]
    fn test_convert_simple_ascii() {
        assert_eq!(Protocol::convert_values_to_utf8_bytes(vec!["abc"]),
                   [
                       b'"', b'a', b'b', b'c', b'"'
                   ]
        );
    }

    #[test]
    fn test_convert_simple_utf8() {
        assert_eq!(Protocol::convert_values_to_utf8_bytes(vec!["äÖß査д"]),
                   [
                       b'"',
                       0xC3, 0xA4,          // ä
                       0xC3, 0x96,          // Ö
                       0xC3, 0x9f,          // ß
                       0xE6, 0x9F, 0xBB,    // 査
                       0xD0, 0xB4,          // д
                       b'"',
                   ]
        );
    }

    #[test]
    fn test_convert_simple_ascii_multiple() {
        assert_eq!(Protocol::convert_values_to_utf8_bytes(vec!["abc", "123", "$%&"]),
                   [
                       b'"', b'a', b'b', b'c', b'"',   // abc
                       b' ',                           // space
                       b'"', b'1', b'2', b'3', b'"',   // 123
                       b' ',                           // space
                       b'"', b'$', b'%', b'&', b'"',   // $%&
                   ]
        );
    }

    #[test]
    fn test_convert_simple_utf8_multiple() {
        assert_eq!(Protocol::convert_values_to_utf8_bytes(vec!["äÄ", "示野", "сит", "😀😂"]),
                   [
                       b'"', 0xC3, 0xA4, 0xC3, 0x84, b'"',                          // äÄ
                       b' ',                                                        // space
                       b'"', 0xE7, 0xA4, 0xBA, 0xE9, 0x87, 0x8E, b'"',              // 示野
                       b' ',                                                        // space
                       b'"', 0xD1, 0x81, 0xD0, 0xB8, 0xD1, 0x82, b'"',              // сит
                       b' ',                                                        // space
                       b'"', 0xF0, 0x9F, 0x98, 0x80, 0xF0, 0x9F, 0x98, 0x82, b'"',  // 😀😂
                   ]
        );
    }
}
