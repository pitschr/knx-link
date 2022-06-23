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

/// Status enumeration that should indicate if the body payload
/// is a `success` or an `error`; if error which type of error.
#[derive(Debug, Copy, Clone, PartialEq)]
pub enum Status {
    Success,
    Error,
    ErrorRequest,
    ErrorTimeout,
    ErrorGroupAddress,
    ErrorDataPointType,
    ErrorClientNotAuthorized,
}

/// Error in case no suitable [`Status`] could be found
#[derive(Debug)]
pub struct UnknownStatusError;

impl TryFrom<u8> for Status {
    type Error = UnknownStatusError;

    /// Converts a byte to [`Status`]. Returns [`UnknownStatusError`] if the
    /// value of byte is not registered.
    fn try_from(value: u8) -> Result<Self, UnknownStatusError> {
        match value {
            0 => Ok(Status::Success),
            1 => Ok(Status::Error),
            2 => Ok(Status::ErrorRequest),
            3 => Ok(Status::ErrorTimeout),
            4 => Ok(Status::ErrorGroupAddress),
            5 => Ok(Status::ErrorDataPointType),
            6 => Ok(Status::ErrorClientNotAuthorized),
            _ => Err(UnknownStatusError)
        }
    }
}

impl From<Status> for u8 {
    fn from(value: Status) -> Self {
        match value {
            Status::Success => 0,
            Status::Error => 1,
            Status::ErrorRequest => 2,
            Status::ErrorTimeout => 3,
            Status::ErrorGroupAddress => 4,
            Status::ErrorDataPointType => 5,
            Status::ErrorClientNotAuthorized => 6,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_byte_representation() {
        assert_eq!(u8::from(Status::Success), 0);
        assert_eq!(u8::from(Status::Error), 1);
        assert_eq!(u8::from(Status::ErrorRequest), 2);
        assert_eq!(u8::from(Status::ErrorTimeout), 3);
        assert_eq!(u8::from(Status::ErrorGroupAddress), 4);
        assert_eq!(u8::from(Status::ErrorDataPointType), 5);
        assert_eq!(u8::from(Status::ErrorClientNotAuthorized), 6);
    }

    #[test]
    fn test_conversion() {
        assert_eq!(Status::Success, Status::try_from(u8::from(Status::Success)).unwrap());
        assert_eq!(Status::Error, Status::try_from(u8::from(Status::Error)).unwrap());
        assert_eq!(Status::ErrorRequest, Status::try_from(u8::from(Status::ErrorRequest)).unwrap());
        assert_eq!(Status::ErrorTimeout, Status::try_from(u8::from(Status::ErrorTimeout)).unwrap());
        assert_eq!(Status::ErrorGroupAddress, Status::try_from(u8::from(Status::ErrorGroupAddress)).unwrap());
        assert_eq!(Status::ErrorDataPointType, Status::try_from(u8::from(Status::ErrorDataPointType)).unwrap());
        assert_eq!(Status::ErrorClientNotAuthorized, Status::try_from(u8::from(Status::ErrorClientNotAuthorized)).unwrap());
    }

    #[test]
    fn test_try_from_err() {
        assert!(Status::try_from(255).is_err())
    }
}