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
use std::fmt;
use std::str::FromStr;

use crate::address::group_address::{GroupAddressBytes, GroupAddressError};
use crate::address::group_address::GroupAddressErrorKind::*;

#[derive(Debug)]
pub struct GroupAddressTwoLevel {
    main: u8,
    sub: u16,
}

impl TryFrom<[u8; 2]> for GroupAddressTwoLevel {
    type Error = GroupAddressError;

    fn try_from(value: [u8; 2]) -> Result<Self, Self::Error> {
        if value[0] == 0 && value[1] == 0 {
            return Err(GroupAddressError::new(Invalid, "Address [0,0] is not allowed because it would lead to address 0/0"));
        }

        Ok(Self {
            main: value[0] >> 3,
            sub: (value[0] as u16 & 0b0000_0111) << 8
                | value[1] as u16,
        })
    }
}

impl fmt::Display for GroupAddressTwoLevel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}/{}", self.main, self.sub)
    }
}


impl FromStr for GroupAddressTwoLevel {
    type Err = GroupAddressError;

    fn from_str(s: &str) -> Result<Self, GroupAddressError> {
        let split: Vec<&str> = s.split('/').collect();

        if split.len() != 2 {
            return Err(GroupAddressError::new(Invalid, "Wrong format. Expected: #/#"));
        }

        let main = match split[0].parse::<u8>() {
            Ok(x) => x,
            Err(_) => return Err(GroupAddressError::new(MainOverflow, "Main must between 0 and 31")),
        };
        let sub = match split[1].parse::<u16>() {
            Ok(x) => x,
            Err(_) => return Err(GroupAddressError::new(SubOverflow, "Sub must between 0 and 2047")),
        };
        GroupAddressTwoLevel::new(main, sub)
    }
}


impl GroupAddressBytes for GroupAddressTwoLevel {
    fn as_bytes(&self) -> [u8; 2] {
        let main_as_u8 = (self.main() & 31) << 3;
        let middle_as_u8 = (self.sub() >> 8) as u8 & 7;
        let sub_as_u8 = (self.sub() & 0xFF) as u8;
        [main_as_u8 | middle_as_u8, sub_as_u8]
    }
}

impl GroupAddressTwoLevel {
    pub fn new(main: u8, sub: u16) -> Result<Self, GroupAddressError> {
        if main > 31 {
            return Err(GroupAddressError::new(MainOverflow, "Main must between 0 and 31"));
        }
        if sub > 2047 {
            return Err(GroupAddressError::new(SubOverflow, "Sub must between 0 and 2047"));
        }
        if main == 0 && sub == 0 {
            return Err(GroupAddressError::new(Invalid, "Address 0/0 is not allowed"));
        }

        Ok(Self { main, sub })
    }

    pub fn main(&self) -> u8 {
        self.main
    }

    pub fn sub(&self) -> u16 {
        self.sub
    }
}

#[cfg(test)]
mod tests {
    use std::convert::TryFrom;
    use std::str::FromStr;

    use crate::address::group_address::{GroupAddressBytes, GroupAddressError};
    use crate::address::group_address::GroupAddressErrorKind::*;
    use crate::address::group_address_two_level::GroupAddressTwoLevel;

    #[test]
    fn new_0_1() {
        let group_address = GroupAddressTwoLevel::new(0, 1).unwrap();
        assert_eq!(group_address.main(), 0);
        assert_eq!(group_address.sub(), 1);
        assert_eq!(group_address.as_bytes(), [0x00, 0x01]);
        assert_eq!(group_address.to_string(), "0/1");
    }

    #[test]
    fn new_7_1024() {
        let group_address = GroupAddressTwoLevel::new(7, 1024).unwrap();
        assert_eq!(group_address.main(), 7);
        assert_eq!(group_address.sub(), 1024);
        assert_eq!(group_address.as_bytes(), [0x3C, 0x00]);
        assert_eq!(group_address.to_string(), "7/1024");
    }

    #[test]
    fn new_31_2047() {
        let group_address = GroupAddressTwoLevel::new(31, 2047).unwrap();
        assert_eq!(group_address.main(), 31);
        assert_eq!(group_address.sub(), 2047);
        assert_eq!(group_address.as_bytes(), [0xFF, 0xFF]);
        assert_eq!(group_address.to_string(), "31/2047");
    }

    #[test]
    fn from_bytes_0001() {
        let group_address = GroupAddressTwoLevel::try_from([0x00, 0x01]).unwrap();
        assert_eq!(group_address.main(), 0);
        assert_eq!(group_address.sub(), 1);
        assert_eq!(group_address.as_bytes(), [0x00, 0x01]);
        assert_eq!(group_address.to_string(), "0/1");
    }

    #[test]
    fn from_bytes_3c00() {
        let group_address = GroupAddressTwoLevel::try_from([0x3C, 0x00]).unwrap();
        assert_eq!(group_address.main(), 7);
        assert_eq!(group_address.sub(), 1024);
        assert_eq!(group_address.as_bytes(), [0x3C, 0x00]);
        assert_eq!(group_address.to_string(), "7/1024");
    }

    #[test]
    fn from_bytes_ffff() {
        let group_address = GroupAddressTwoLevel::try_from([0xFF, 0xFF]).unwrap();
        assert_eq!(group_address.main(), 31);
        assert_eq!(group_address.sub(), 2047);
        assert_eq!(group_address.as_bytes(), [0xFF, 0xFF]);
        assert_eq!(group_address.to_string(), "31/2047");
    }

    #[test]
    fn new_err() {
        // Address 0/0 is not allowed
        assert_eq!(GroupAddressTwoLevel::new(0, 0).unwrap_err(),
                   GroupAddressError::new(Invalid, "Address 0/0 is not allowed"));

        // main should have only 0-31
        assert_eq!(GroupAddressTwoLevel::new(32, 0).unwrap_err(),
                   GroupAddressError::new(MainOverflow, "Main must between 0 and 31"));

        // sub should have only 0-2047
        assert_eq!(GroupAddressTwoLevel::new(0, 2048).unwrap_err(),
                   GroupAddressError::new(SubOverflow, "Sub must between 0 and 2047"));
    }

    #[test]
    fn from_bytes_err() {
        // Address 0/0 is not allowed
        assert_eq!(GroupAddressTwoLevel::try_from([0x00, 0x00]).unwrap_err(),
                   GroupAddressError::new(Invalid, "Address [0,0] is not allowed because it would lead to address 0/0"));
    }

    #[test]
    fn from_string() {
        let group_address = GroupAddressTwoLevel::from_str("14/1733").unwrap();
        assert_eq!(group_address.main(), 14);
        assert_eq!(group_address.sub(), 1733);
        assert_eq!(group_address.as_bytes(), [0x76, 0xC5]);
    }

    #[test]
    fn from_string_err() {
        assert_eq!(GroupAddressTwoLevel::from_str("0").unwrap_err(), GroupAddressError::new(Invalid, "Wrong format. Expected: #/#"));
        assert_eq!(GroupAddressTwoLevel::from_str("0/0/0").unwrap_err(), GroupAddressError::new(Invalid, "Wrong format. Expected: #/#"));

        assert_eq!(GroupAddressTwoLevel::from_str("0/0").unwrap_err(), GroupAddressError::new(Invalid, "Address 0/0 is not allowed"));

        assert_eq!(GroupAddressTwoLevel::from_str("32/0").unwrap_err(), GroupAddressError::new(MainOverflow, "Main must between 0 and 31"));
        assert_eq!(GroupAddressTwoLevel::from_str("99999/0").unwrap_err(), GroupAddressError::new(MainOverflow, "Main must between 0 and 31"));

        assert_eq!(GroupAddressTwoLevel::from_str("0/2048").unwrap_err(), GroupAddressError::new(SubOverflow, "Sub must between 0 and 2047"));
        assert_eq!(GroupAddressTwoLevel::from_str("0/99999").unwrap_err(), GroupAddressError::new(SubOverflow, "Sub must between 0 and 2047"));
    }
}