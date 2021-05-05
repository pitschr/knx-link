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

//use std::str::{FromStr};
use std::fmt;

//use std::convert::TryFrom;

// pub trait GroupAddressInt {
//     fn to_address(&self) -> u16;
// }

// #[derive(Debug)]
// pub struct GroupAddressFormatError {
//     // empty
// }
//
// impl Error for GroupAddressFormatError {}
//
// impl fmt::Display for GroupAddressFormatError {
//     fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
//         write!(f, "Invalid group address format provided, expected are '0/0/0', '0/0' or '0'")
//     }
// }
//
//
// pub struct GroupAddress {}
//
// impl GroupAddress {
//     pub fn parse(address: &str) -> Result<&dyn GroupAddressInt, &dyn Error> {
//         let splitted_address : Vec<_> = address.split('/').collect();
//
//         match splitted_address.len() {
//             3 => {
//                 // Group Address is Three-level
//                 Ok(&GroupAddressThreeLevel::new(
//                     u8::from_str(splitted_address[0]).unwrap(),
//                     u8::from_str(splitted_address[1]).unwrap(),
//                     u8::from_str(splitted_address[2]).unwrap(),
//                 ).unwrap().to_free_level())
//             },
// 2 => {
//     // Group Address is Two-Level
//     Ok(&GroupAddressTwoLevel::new(
//         u8::from_str(splitted_address[0]).unwrap(),
//         u16::from_str(splitted_address[1]).unwrap()
//     ).unwrap().to_free_level())
// },
// 1 => {
//     // Group Address is Free-Level
//     Ok(&GroupAddressFreeLevel::new(
//         u16::from_str(address).unwrap()
//     ))
// },
//             _ => {
//                 Err(&Box::try_from(GroupAddressFormatError).unwrap())
//             }
//         }
//     }
// }
//
// impl fmt::Display for dyn GroupAddressInt {
//     fn fmt(&self, f: &mut fmt::Formatter<'_>) -> Result<(), fmt::Error> {
//         write!(f, "Three-Level={}, Two-Level={}, Free-Level={}",
//                self.to_three_level(), self.to_two_level(), self.to_free_level())
//     }
// }
//
//
//
// #[derive(Debug)]
// pub struct GroupAddressFreeLevel {
//     address: u16
// }
//
// impl GroupAddressFreeLevel {
//     pub fn new(address: u16) -> Self {
//         if address == 0 {
//             Err(Error::new("Address 0 is not allowed"))
//         }
//
//         GroupAddressFreeLevel { address }
//     }
//
//     pub fn to_two_level(&self) -> GroupAddressTwoLevel {
//         GroupAddressTwoLevel {
//             main : ((self.address & 0b1111_1000_0000_0000) >> 11) as u8,
//             sub : self.address & 0b0000_0111_1111_11111,
//         }
//     }
//
//     pub fn to_three_level(&self) -> GroupAddressThreeLevel {
//         GroupAddressThreeLevel {
//             main: ((self.address & 0b1111_1000_0000_0000) >> 11) as u8,
//             middle: ((self.address & 0b0000_0111_0000_0000) >> 8) as u8,
//             sub: (self.address & 0b0000_0000_1111_11111) as u8,
//         }
//     }
// }
//
// impl GroupAddressInt for GroupAddressFreeLevel {
//     fn to_address(&self) -> u16 {
//         self.address
//     }
// }
//
// impl fmt::Display for GroupAddressFreeLevel {
//     fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
//         write!(f, "{}", self.address)
//     }
// }
//
//
//
