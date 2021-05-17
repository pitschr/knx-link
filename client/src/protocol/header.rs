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

use crate::protocol::action::Action;
use std::convert::TryFrom;

#[derive(Debug, PartialEq)]
pub struct Header {
    version: u8,
    action: Action,
    length: u8,
}

#[derive(Debug)]
pub struct HeaderError;

impl Header {
    pub fn new(version: u8, action: Action, length: u8) -> Self {
        Header { version, action, length }
    }

    pub fn as_bytes(&self) -> [u8; 3] {
        let mut bytes = [0u8; 3];
        bytes[0] = self.version;
        bytes[1] = self.action.into();
        bytes[2] = self.length;
        return bytes;
    }

    pub fn version(&self) -> u8 {
        self.version
    }

    pub fn action(&self) -> Action {
        self.action
    }

    pub fn length(&self) -> u8 {
        self.length
    }
}

impl <const N: usize> TryFrom<&[u8; N]> for Header {
    type Error = HeaderError;

    fn try_from(value: &[u8; N]) -> Result<Self, Self::Error> {
        if N >= 3 {
            Ok(Header::new(
                value[0],
                Action::try_from(value[1])
                    .expect(format!("Action with number not found: {}", value[1]).as_str()),
                value[2])
            )
        } else {
            Err(HeaderError)
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_read_header() {
        let header = Header::new(1, Action::ReadRequest, 13);

        assert_eq!(header.version(), 1);
        assert_eq!(header.action(), Action::ReadRequest);
        assert_eq!(header.length(), 13);
        assert_eq!(header.as_bytes(),
                   [
                       0x01,       // Version
                       0x00,       // Read Request
                       0x0D,       // Length
                   ]
        );
    }

    #[test]
    fn test_write_header() {
        let header = Header::new(2, Action::WriteRequest, 17);

        assert_eq!(header.version(), 2);
        assert_eq!(header.action(), Action::WriteRequest);
        assert_eq!(header.length(), 17);
        assert_eq!(header.as_bytes(),
                   [
                       0x02,       // Version
                       0x01,       // Write Request
                       0x11,       // Length
                   ]
        );
    }

    #[test]
    fn test_header() {
        let header = Header::try_from(&[0x07, 0x01, 0x08, 0xFF, 0xFF]).unwrap();
        assert_eq!(header, Header::new(7, Action::WriteRequest, 8));
    }
}