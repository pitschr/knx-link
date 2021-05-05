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
use std::fmt::{Display, Formatter};
use std::str::FromStr;

use crate::address::group_address_free_level::GroupAddressFreeLevel;
use crate::address::group_address_three_level::GroupAddressThreeLevel;
use crate::address::group_address_two_level::GroupAddressTwoLevel;

#[derive(Debug, PartialEq)]
pub struct GroupAddressError {
    message: String,
}

impl GroupAddressError {
    pub fn new(message: String) -> Self {
        GroupAddressError { message }
    }
}

impl Display for GroupAddressError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.message)
    }
}

pub trait GroupAddressBytes {
    fn as_bytes(&self) -> [u8; 2];
}

#[derive(Debug)]
pub struct GroupAddress {
    address: [u8; 2],
}

impl TryFrom<&str> for GroupAddress {
    type Error = GroupAddressError;

    fn try_from(value: &str) -> Result<Self, Self::Error> {
        let splitted: Vec<_> = value.split('/').collect();
        match splitted.len() {
            3 => {
                // Group Address is Three-level
                match GroupAddressThreeLevel::new(
                    u8::from_str(splitted[0]).unwrap(),
                    u8::from_str(splitted[1]).unwrap(),
                    u8::from_str(splitted[2]).unwrap(),
                ) {
                    Ok(ga) => Ok(GroupAddress { address: ga.as_bytes() }),
                    Err(e) => Err(e),
                }
            }
            2 => {
                // Group Address is Two-Level
                match GroupAddressTwoLevel::new(
                    u8::from_str(splitted[0]).unwrap(),
                    u16::from_str(splitted[1]).unwrap(),
                ) {
                    Ok(ga) => Ok(GroupAddress { address: ga.as_bytes() }),
                    Err(e) => Err(e),
                }
            }
            1 => {
                // Group Address is Free-Level
                match GroupAddressFreeLevel::new(
                    u16::from_str(splitted[0]).unwrap()
                ) {
                    Ok(ga) => Ok(GroupAddress { address: ga.as_bytes() }),
                    Err(e) => Err(e),
                }
            }
            _ => {
                Err(GroupAddressError::new(format!("Unsupported address format provided. Supported are: 0/0/0, 0/0 or 0: {}", value)))
            }
        }
    }
}

impl GroupAddressBytes for GroupAddress {
    fn as_bytes(&self) -> [u8; 2] {
        self.address
    }
}

#[cfg(test)]
mod tests {
    use std::convert::TryFrom;

    use crate::address::group_address::{GroupAddress, GroupAddressError};

    #[test]
    fn as_bytes_two_level() {
        // 20/1223 = 1010_0... ...._.... (A4, C7)
        //           ...._.100 ...._....
        //           ...._.... 1100_0111
        let group_address = GroupAddress::try_from("20/1223").unwrap();
        assert_eq!(group_address.address, [0xA4, 0xC7]);
    }

    #[test]
    fn as_bytes_three_level() {
        // 1/2/3   = 0000_1... ...._.... (0A, 03)
        //           ...._.010 ...._....
        //           ...._.... 0000_0011
        let group_address = GroupAddress::try_from("1/2/3").unwrap();
        assert_eq!(group_address.address, [0x0A, 0x03]);
    }

    #[test]
    fn as_bytes_error() {
        // Illegal Format
        assert_eq!(GroupAddress::try_from("1/2/3/4").err(),
                   Some(GroupAddressError::new(format!("Unsupported address format provided. Supported are: 0/0/0, 0/0 or 0: 1/2/3/4")))
        );
    }
}