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
use std::str::{from_utf8, Utf8Error};

use crate::protocol::status::Status;

#[derive(Debug)]
pub struct ResponseBody {
    last_packet: bool,
    status: Status,
    data: Vec<u8>,
}

impl ResponseBody {
    pub fn last_packet(&self) -> bool {
        self.last_packet
    }

    pub fn status(&self) -> Status {
        self.status
    }

    pub fn data(&self) -> Vec<u8> {
        self.data.clone()
    }

    pub fn message(&self) -> Result<&str, Utf8Error> {
        from_utf8(self.data.as_slice())
    }
}

#[derive(Debug)]
pub struct ResponseBodyError;

impl TryFrom<&[u8]> for ResponseBody {
    type Error = ResponseBodyError;

    fn try_from(value: &[u8]) -> Result<Self, Self::Error> {
        Ok(
            ResponseBody {
                last_packet: value[0] & 0x80 > 0,
                status: Status::try_from(value[1])
                    .expect(format!("Status not found: {}", value[1]).as_str()),
                data: value[2..].to_vec(),
            }
        )
    }
}