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

pub struct Header {
    version: u8,
    action: Action,
    length: u8,
}

impl Header {
    pub fn new(action: Action, length: u8) -> Self {
        Header { version: 1, action, length }
    }

    pub fn as_bytes(&self) -> [u8; 3] {
        let mut bytes = [0u8; 3];
        bytes[0] = self.version;
        bytes[1] = self.action.into();
        bytes[2] = self.length;
        return bytes;
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_read_header() {
        assert_eq!(Header::new(Action::ReadRequest, 13).as_bytes(),
                   [
                       0x01,       // Version
                       0x00,       // Read Request
                       0x0D,       // Length
                   ]
        );
    }

    #[test]
    fn test_write_header() {
        assert_eq!(Header::new(Action::WriteRequest, 17).as_bytes(),
                   [
                       0x01,       // Version
                       0x01,       // Write Request
                       0x11,       // Length
                   ]
        );
    }
}