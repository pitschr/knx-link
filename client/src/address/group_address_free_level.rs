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

#[derive(Debug)]
pub struct GroupAddressFreeLevel {
    address: u16,
}

impl TryFrom<[u8; 2]> for GroupAddressFreeLevel {
    type Error = GroupAddressError;

    fn try_from(value: [u8; 2]) -> Result<Self, Self::Error> {
        if value[0] == 0 && value[1] == 0 {
            return Err(GroupAddressError::new(format!("Address [0,0] is not allowed because it would lead to address 0")));
        }

        Ok(Self {
            address: (value[0] as u16) << 8 | value[1] as u16
        })
    }
}

impl fmt::Display for GroupAddressFreeLevel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.address)
    }
}

impl GroupAddressBytes for GroupAddressFreeLevel {
    fn as_bytes(&self) -> [u8; 2] {
        [(self.address >> 8) as u8, (self.address & 0xFF) as u8]
    }
}

impl FromStr for GroupAddressFreeLevel {
    type Err = GroupAddressError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.parse::<u16>() {
            Ok(value) => Self::new(value),
            Err(_) => Err(GroupAddressError::new(format!("Address must be between [1-65535], but got: {}", s))),
        }
    }
}

impl GroupAddressFreeLevel {
    pub fn new(address: u16) -> Result<Self, GroupAddressError> {
        if address == 0 {
            return Err(GroupAddressError::new(format!("Address 0 is not allowed!")));
        }

        Ok(Self { address })
    }

    pub fn address(&self) -> u16 {
        self.address
    }
}

#[cfg(test)]
mod tests {
    use std::convert::TryFrom;
    use std::str::FromStr;

    use crate::address::group_address::{GroupAddressBytes, GroupAddressError};
    use crate::address::group_address_free_level::GroupAddressFreeLevel;

    #[test]
    fn new_1() {
        let group_address = GroupAddressFreeLevel::new(1).unwrap();
        assert_eq!(group_address.address(), 1);
        assert_eq!(group_address.as_bytes(), [0x00, 0x01]);
        assert_eq!(group_address.to_string(), "1");
    }

    #[test]
    fn new_4711() {
        let group_address = GroupAddressFreeLevel::new(4711).unwrap();
        assert_eq!(group_address.address(), 4711);
        assert_eq!(group_address.as_bytes(), [0x12, 0x67]);
        assert_eq!(group_address.to_string(), "4711");
    }

    #[test]
    fn new_65535() {
        let group_address = GroupAddressFreeLevel::new(65535).unwrap();
        assert_eq!(group_address.address(), 65535);
        assert_eq!(group_address.as_bytes(), [0xFF, 0xFF]);
        assert_eq!(group_address.to_string(), "65535");
    }

    #[test]
    fn from_bytes_0001() {
        let group_address = GroupAddressFreeLevel::try_from([0x00, 0x01]).unwrap();
        assert_eq!(group_address.address(), 1);
        assert_eq!(group_address.as_bytes(), [0x00, 0x01]);
        assert_eq!(group_address.to_string(), "1");
    }

    #[test]
    fn from_bytes_3c4f() {
        let group_address = GroupAddressFreeLevel::try_from([0x3C, 0x4F]).unwrap();
        assert_eq!(group_address.address(), 15439);
        assert_eq!(group_address.as_bytes(), [0x3C, 0x4F]);
        assert_eq!(group_address.to_string(), "15439");
    }

    #[test]
    fn from_bytes_ffff() {
        let group_address = GroupAddressFreeLevel::try_from([0xFF, 0xFF]).unwrap();
        assert_eq!(group_address.address(), 65535);
        assert_eq!(group_address.as_bytes(), [0xFF, 0xFF]);
        assert_eq!(group_address.to_string(), "65535");
    }

    #[test]
    fn new_err() {
        // Address 0 is not allowed
        assert_eq!(GroupAddressFreeLevel::new(0).err(),
                   Some(GroupAddressError::new(format!("Address 0 is not allowed!"))));
    }

    #[test]
    fn from_bytes_err() {
        // Address 0/0 is not allowed
        assert_eq!(GroupAddressFreeLevel::try_from([0x00, 0x00]).err(),
                   Some(GroupAddressError::new(format!("Address [0,0] is not allowed because it would lead to address 0"))));
    }

    #[test]
    fn from_string() {
        let group_address = GroupAddressFreeLevel::from_str("41733").unwrap();
        assert_eq!(group_address.address(), 41733);
        assert_eq!(group_address.as_bytes(), [0xA3, 0x05]);
    }

    #[test]
    fn from_string_err() {
        assert_eq!(GroupAddressFreeLevel::from_str("0").err(),
                   Some(GroupAddressError::new(format!("Address 0 is not allowed!")))
        );
        assert_eq!(GroupAddressFreeLevel::from_str("99999").err(),
                   Some(GroupAddressError::new(format!("Address must be between [1-65535], but got: 99999")))
        );
    }
}