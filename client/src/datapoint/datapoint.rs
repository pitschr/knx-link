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

use regex::Regex;
use std::error::Error;
use std::fmt;

#[derive(Debug, PartialEq)]
pub struct DataPointError {
    message: String,
}

impl DataPointError {
    pub fn new(message: &str) -> Self {
        DataPointError { message: String::from(message) }
    }
}

impl Error for DataPointError {}

impl Display for DataPointError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "Data Point: {}", self.message)
    }
}

pub struct DataPoint {
    dpt: u16,
    dpst: u16,
}

impl Display for DataPoint {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        if self.dpst == 0 {
            write!(f, "dpt-{}", self.dpt)
        } else {
            write!(f, "dpst-{}-{}", self.dpt, self.dpst)
        }
    }
}

impl TryFrom<&str> for DataPoint {
    type Error = DataPointError;

    fn try_from(value: &str) -> Result<Self, Self::Error> {
        // format like "1.2", "14.1000"
        if value.contains(".") {
            return parse_with_dot(value);
        }
        // format like "dpt-1", "dpt-14"
        else if value.to_ascii_lowercase().starts_with("dpt-") {
            return parse_with_dpt(value);
        }
        // format like "dpst-1-1", "dpst-14-1000"
        else if value.to_ascii_lowercase().starts_with("dpst-") {
            return parse_with_dpst(value);
        }
        // format like "1", "14"
        else if value.chars().all(char::is_numeric) {
            return parse_with_numbers(value);
        }
        Err(DataPointError::new("Wrong data point format. Expected: #, #.#, dpt-# or dpst-#-#"))
    }
}

impl DataPoint {
    pub fn as_bytes(&self) -> [u8; 4] {
        [
            ((self.dpt & 0xFF00) >> 8) as u8,
            (self.dpt & 0xFF) as u8,
            ((self.dpst & 0xFF00) >> 8) as u8,
            (self.dpst & 0xFF) as u8,
        ]
    }
}

fn parse_with_dot(s: &str) -> Result<DataPoint, DataPointError> {
    let regex = Regex::new(r"^(\d+)\.(\d+)$").unwrap();
    match regex.captures(s) {
        Some(caps) => {
            // get data point type, the digit before "." character
            let dpt = match caps.get(1).unwrap().as_str().parse::<u16>() {
                Ok(x) => x,
                Err(_) => return Err(DataPointError::new("Main Data Point Type must be between 0 and 65535"))
            };
            // get data point sub type, the digits after "." character
            let dpst = match caps.get(2).unwrap().as_str().parse::<u16>() {
                Ok(x) => x,
                Err(_) => return Err(DataPointError::new("Sub Data Point Type must be between 0 and 65535"))
            };

            Ok(DataPoint { dpt, dpst })
        }
        None => {
            Err(DataPointError::new("Wrong data point format. Expected: #.#"))
        }
    }
}

fn parse_with_dpt(s: &str) -> Result<DataPoint, DataPointError> {
    let regex = Regex::new(r"^(?:dpt-)(\d+)$").unwrap();
    match regex.captures(s.to_ascii_lowercase().as_str()) {
        Some(caps) => {
            // get data point type, the digit after "dpt" or "dpt-" string
            match caps.get(1).unwrap().as_str().parse::<u16>() {
                Ok(x) => Ok(DataPoint { dpt: x, dpst: 0 }),
                Err(_) => Err(DataPointError::new("Data Point Type must be between 0 and 65535"))
            }
        }
        None => {
            Err(DataPointError::new("Wrong data point format. Expected: dpt-#"))
        }
    }
}

fn parse_with_dpst(s: &str) -> Result<DataPoint, DataPointError> {
    let regex = Regex::new(r"^(?:dpst-)(\d+)-(\d+)$").unwrap();
    match regex.captures(s.to_ascii_lowercase().as_str()) {
        Some(caps) => {
            // get data point type, the digit after "dpst" or "dpst-" string
            let dpt = match caps.get(1).unwrap().as_str().parse::<u16>() {
                Ok(x) => x,
                Err(_) => return Err(DataPointError::new("Main Data Point Type must be between 0 and 65535"))
            };
            // get data point sub type, the digits after "-" character
            let dpst = match caps.get(2).unwrap().as_str().parse::<u16>() {
                Ok(x) => x,
                Err(_) => return Err(DataPointError::new("Sub Data Point Type must be between 0 and 65535"))
            };

            Ok(DataPoint { dpt, dpst })
        }
        None => {
            Err(DataPointError::new("Wrong data point format. Expected: dpst-#-#"))
        }
    }
}

fn parse_with_numbers(s: &str) -> Result<DataPoint, DataPointError> {
    match u16::from_str(s) {
        Ok(dpt) => Ok(DataPoint { dpt, dpst: 0 }),
        Err(_) => Err(DataPointError::new("Data Point Type must be between 0 and 65535"))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_display() {
        // output: dpt-1
        assert_eq!(format!("{}", DataPoint { dpt: 0, dpst: 0 }), "dpt-0");
        assert_eq!(format!("{}", DataPoint { dpt: 1, dpst: 0 }), "dpt-1");
        assert_eq!(format!("{}", DataPoint { dpt: 13, dpst: 0 }), "dpt-13");
        assert_eq!(format!("{}", DataPoint { dpt: 32100, dpst: 0 }), "dpt-32100");
        // output: dpst-1-1
        assert_eq!(format!("{}", DataPoint { dpt: 0, dpst: 1 }), "dpst-0-1");
        assert_eq!(format!("{}", DataPoint { dpt: 13, dpst: 17 }), "dpst-13-17");
        assert_eq!(format!("{}", DataPoint { dpt: 1235, dpst: 8391 }), "dpst-1235-8391");
    }

    #[test]
    fn test_with_dot() {
        assert_eq!(DataPoint::try_from("1.2").unwrap().as_bytes(), [0x00, 0x01, 0x00, 0x02]);
        assert_eq!(DataPoint::try_from("4711.32109").unwrap().as_bytes(), [0x12, 0x67, 0x7D, 0x6D]);

        assert_eq!(DataPoint::try_from(".ILLEGAL").err(),
                   Some(DataPointError::new("Wrong data point format. Expected: #.#"))
        );
        assert!(DataPoint::try_from(".1").is_err());
        assert!(DataPoint::try_from("a.1").is_err());
        assert!(DataPoint::try_from("1.a").is_err());

        assert_eq!(DataPoint::try_from("9999999999.1").err(),
                   Some(DataPointError::new("Main Data Point Type must be between 0 and 65535"))
        );
        assert_eq!(DataPoint::try_from("1.9999999999").err(),
                   Some(DataPointError::new("Sub Data Point Type must be between 0 and 65535"))
        );

    }

    #[test]
    fn test_with_dpt() {
        assert_eq!(DataPoint::try_from("dpt-13").unwrap().as_bytes(), [0x00, 0x0D, 0x00, 0x00]);
        assert_eq!(DataPoint::try_from("DPT-1713").unwrap().as_bytes(), [0x06, 0xB1, 0x00, 0x00]);

        assert_eq!(DataPoint::try_from("dpt-ILLEGAL").err(),
                   Some(DataPointError::new("Wrong data point format. Expected: dpt-#"))
        );
        assert!(DataPoint::try_from("dpt--").is_err());
        assert!(DataPoint::try_from("dpt--1").is_err());
        assert!(DataPoint::try_from("dpt-a").is_err());

        assert_eq!(DataPoint::try_from("dpt-9999999999").err(),
                Some(DataPointError::new("Data Point Type must be between 0 and 65535"))
        );
    }

    #[test]
    fn test_with_dpst() {
        assert_eq!(DataPoint::try_from("dpst-21-17").unwrap().as_bytes(), [0x00, 0x15, 0x00, 0x11]);
        assert_eq!(DataPoint::try_from("DPST-1331-4719").unwrap().as_bytes(), [0x05, 0x33, 0x12, 0x6F]);

        assert_eq!(DataPoint::try_from("dpst-ILLEGAL").err(),
                   Some(DataPointError::new("Wrong data point format. Expected: dpst-#-#"))
        );
        assert!(DataPoint::try_from("dpst--").is_err());
        assert!(DataPoint::try_from("dpst--1").is_err());
        assert!(DataPoint::try_from("dpst-1-").is_err());
        assert!(DataPoint::try_from("dpst-1-a").is_err());
        assert!(DataPoint::try_from("dpst-a-1").is_err());

        assert_eq!(DataPoint::try_from("dpst-9999999999-1").err(),
                   Some(DataPointError::new("Main Data Point Type must be between 0 and 65535"))
        );
        assert_eq!(DataPoint::try_from("dpst-1-9999999999").err(),
                   Some(DataPointError::new("Sub Data Point Type must be between 0 and 65535"))
        );
    }

    #[test]
    fn test_with_numbers() {
        assert_eq!(DataPoint::try_from("1").unwrap().as_bytes(), [0x00, 0x01, 0x00, 0x00]);
        assert_eq!(DataPoint::try_from("4141").unwrap().as_bytes(), [0x10, 0x2D, 0x00, 0x00]);

        assert_eq!(DataPoint::try_from("99999").err(),
                   Some(DataPointError::new("Data Point Type must be between 0 and 65535"))
        );
    }

    #[test]
    fn test_error() {
        assert_eq!(DataPoint::try_from("ILLEGAL").err(),
                   Some(DataPointError::new("Wrong data point format. Expected: #, #.#, dpt-# or dpst-#-#"))
        );
    }
}