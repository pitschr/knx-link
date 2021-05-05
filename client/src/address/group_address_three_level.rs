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
pub struct GroupAddressThreeLevel {
    main: u8,
    middle: u8,
    sub: u8,
}

impl TryFrom<[u8; 2]> for GroupAddressThreeLevel {
    type Error = GroupAddressError;

    fn try_from(value: [u8; 2]) -> Result<Self, Self::Error> {
        if value[0] == 0 && value[1] == 0 {
            Err(GroupAddressError::new(format!("Address [0,0] is not allowed because it would lead to address 0/0/0")))
        } else {
            Ok(Self {
                main: value[0] >> 3,
                middle: value[0] & 0b0000_0111,
                sub: value[1],
            })
        }
    }
}

impl fmt::Display for GroupAddressThreeLevel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}/{}/{}", self.main, self.middle, self.sub)
    }
}

impl FromStr for GroupAddressThreeLevel {
    type Err = GroupAddressError;

    fn from_str(s: &str) -> Result<Self, GroupAddressError> {
        let split: Vec<&str> = s.split('/').collect();
        if split.len() != 3 {
            return Err(GroupAddressError::new(format!("Unsupported format. Expected '1/1/1', but got: {}", s)));
        }

        let main;
        let middle;
        let sub;
        match split[0].parse::<u8>() {
            Ok(value) => main = value,
            Err(_) => return Err(GroupAddressError::new(format!("Main must between 0 and 31 but was: {}", split[0]))),
        }
        match split[1].parse::<u8>() {
            Ok(value) => middle = value,
            Err(_) => return Err(GroupAddressError::new(format!("Middle must between 0 and 7 but was: {}", split[1]))),
        }
        match split[2].parse::<u8>() {
            Ok(value) => sub = value,
            Err(_) => return Err(GroupAddressError::new(format!("Sub must between 0 and 255 but was: {}", split[2]))),
        }
        Self::new(main, middle, sub)
    }
}

impl GroupAddressBytes for GroupAddressThreeLevel {
    fn as_bytes(&self) -> [u8; 2] {
        let main_as_u8 = (self.main() & 31) << 3;
        [main_as_u8 | self.middle(), self.sub()]
    }
}

impl GroupAddressThreeLevel {
    pub fn new(main: u8, middle: u8, sub: u8) -> Result<Self, GroupAddressError> {
        if main > 31 {
            return Err(GroupAddressError::new(format!("Main must between 0 and 31 but was: {}", main)));
        }
        if middle > 7 {
            return Err(GroupAddressError::new(format!("Middle must between 0 and 7 but was: {}", middle)));
        }
        if main == 0 && middle == 0 && sub == 0 {
            return Err(GroupAddressError::new(format!("Address 0/0/0 is not allowed!")));
        }

        Ok(Self { main, middle, sub })
    }

    pub fn main(&self) -> u8 {
        self.main
    }

    pub fn middle(&self) -> u8 {
        self.middle
    }

    pub fn sub(&self) -> u8 {
        self.sub
    }
}

#[cfg(test)]
mod tests {
    use std::convert::TryFrom;
    use std::str::FromStr;

    use crate::address::group_address::GroupAddressError;
    use crate::address::group_address_three_level::{GroupAddressBytes, GroupAddressThreeLevel};

    #[test]
    fn new_0_0_1() {
        let group_address = GroupAddressThreeLevel::new(0, 0, 1).unwrap();
        assert_eq!(group_address.main(), 0);
        assert_eq!(group_address.middle(), 0);
        assert_eq!(group_address.sub(), 1);
        assert_eq!(group_address.as_bytes(), [0x00, 0x01]);
        assert_eq!(group_address.to_string(), "0/0/1");
    }

    #[test]
    fn new_7_3_128() {
        let group_address = GroupAddressThreeLevel::new(7, 3, 128).unwrap();
        assert_eq!(group_address.main(), 7);
        assert_eq!(group_address.middle(), 3);
        assert_eq!(group_address.sub(), 128);
        assert_eq!(group_address.as_bytes(), [0x3B, 0x80]);
        assert_eq!(group_address.to_string(), "7/3/128");
    }

    #[test]
    fn new_31_7_255() {
        let group_address = GroupAddressThreeLevel::new(31, 7, 255).unwrap();
        assert_eq!(group_address.main(), 31);
        assert_eq!(group_address.middle(), 7);
        assert_eq!(group_address.sub(), 255);
        assert_eq!(group_address.as_bytes(), [0xFF, 0xFF]);
        assert_eq!(group_address.to_string(), "31/7/255");
    }

    #[test]
    fn from_bytes_0001() {
        let group_address = GroupAddressThreeLevel::try_from([0x00, 0x01]).unwrap();
        assert_eq!(group_address.main(), 0);
        assert_eq!(group_address.middle(), 0);
        assert_eq!(group_address.sub(), 1);
        assert_eq!(group_address.as_bytes(), [0x00, 0x01]);
        assert_eq!(group_address.to_string(), "0/0/1");
    }

    #[test]
    fn from_bytes_3b80() {
        let group_address = GroupAddressThreeLevel::try_from([0x3B, 0x80]).unwrap();
        assert_eq!(group_address.main(), 7);
        assert_eq!(group_address.middle(), 3);
        assert_eq!(group_address.sub(), 128);
        assert_eq!(group_address.as_bytes(), [0x3B, 0x80]);
        assert_eq!(group_address.to_string(), "7/3/128");
    }

    #[test]
    fn from_bytes_ffff() {
        let group_address = GroupAddressThreeLevel::try_from([0xFF, 0xFF]).unwrap();
        assert_eq!(group_address.main(), 31);
        assert_eq!(group_address.middle(), 7);
        assert_eq!(group_address.sub(), 255);
        assert_eq!(group_address.as_bytes(), [0xFF, 0xFF]);
        assert_eq!(group_address.to_string(), "31/7/255");
    }

    #[test]
    fn new_err() {
        // Address 0/0 is not allowed
        assert_eq!(GroupAddressThreeLevel::new(0, 0, 0).err(),
                   Some(GroupAddressError::new(format!("Address 0/0/0 is not allowed!"))));

        // main should have only 0-31
        assert_eq!(GroupAddressThreeLevel::new(32, 0, 0).err(),
                   Some(GroupAddressError::new(format!("Main must between 0 and 31 but was: 32"))));

        // middle should have only 0-7
        assert_eq!(GroupAddressThreeLevel::new(0, 8, 0).err(),
                   Some(GroupAddressError::new(format!("Middle must between 0 and 7 but was: 8"))));
    }

    #[test]
    fn from_bytes_err() {
        // Address 0/0/0 is not allowed
        assert_eq!(GroupAddressThreeLevel::try_from([0x00, 0x00]).err(),
                   Some(GroupAddressError::new(format!("Address [0,0] is not allowed because it would lead to address 0/0/0"))));
    }

    #[test]
    fn from_string() {
        let group_address = GroupAddressThreeLevel::from_str("1/2/3").unwrap();
        assert_eq!(group_address.main(), 1);
        assert_eq!(group_address.middle(), 2);
        assert_eq!(group_address.sub(), 3);
        assert_eq!(group_address.as_bytes(), [0x0A, 0x03]);
    }

    #[test]
    fn from_string_err() {
        assert_eq!(GroupAddressThreeLevel::from_str("0/0/0").err(),
                   Some(GroupAddressError::new(format!("Address 0/0/0 is not allowed!")))
        );
        assert_eq!(GroupAddressThreeLevel::from_str("99999/0/0").err(),
                   Some(GroupAddressError::new(format!("Main must between 0 and 31 but was: 99999")))
        );
        assert_eq!(GroupAddressThreeLevel::from_str("0/99999/0").err(),
                   Some(GroupAddressError::new(format!("Middle must between 0 and 7 but was: 99999")))
        );
        assert_eq!(GroupAddressThreeLevel::from_str("0/0/99999").err(),
                   Some(GroupAddressError::new(format!("Sub must between 0 and 255 but was: 99999")))
        );
        assert_eq!(GroupAddressThreeLevel::from_str("32/0/0").err(),
                   Some(GroupAddressError::new(format!("Main must between 0 and 31 but was: 32")))
        );
        assert_eq!(GroupAddressThreeLevel::from_str("0/8/0").err(),
                   Some(GroupAddressError::new(format!("Middle must between 0 and 7 but was: 8")))
        );
        assert_eq!(GroupAddressThreeLevel::from_str("0/0/256").err(),
                   Some(GroupAddressError::new(format!("Sub must between 0 and 255 but was: 256")))
        );
    }
}