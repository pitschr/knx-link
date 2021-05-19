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

/// Action enumeration that should represent the body payload
#[derive(Debug, Copy, Clone, PartialEq)]
pub enum Action {
    ReadRequest,
    WriteRequest,
    ReadResponse,
    WriteResponse,
}

/// Error in case no suitable [`Action`] could be found
#[derive(Debug)]
pub struct UnknownActionError;

impl TryFrom<u8> for Action {
    type Error = UnknownActionError;

    /// Converts a byte to [`Action`]. Returns [`UnknownActionError`] if the
    /// value of byte is not registered.
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0 => Ok(Action::ReadRequest),
            1 => Ok(Action::WriteRequest),
            2 => Ok(Action::ReadResponse),
            3 => Ok(Action::WriteResponse),
            _ => Err(UnknownActionError {})
        }
    }
}

impl From<Action> for u8 {
    fn from(action: Action) -> Self {
        match action {
            Action::ReadRequest => 0,
            Action::WriteRequest => 1,
            Action::ReadResponse => 2,
            Action::WriteResponse => 3,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_byte_representation() {
        assert_eq!(u8::from(Action::ReadRequest), 0);
        assert_eq!(u8::from(Action::WriteRequest), 1);
        assert_eq!(u8::from(Action::ReadResponse), 2);
        assert_eq!(u8::from(Action::WriteResponse), 3);
    }

    #[test]
    fn test_conversion() {
        assert_eq!(Action::ReadRequest, Action::try_from(u8::from(Action::ReadRequest)).unwrap());
        assert_eq!(Action::WriteRequest, Action::try_from(u8::from(Action::WriteRequest)).unwrap());
        assert_eq!(Action::ReadResponse, Action::try_from(u8::from(Action::ReadResponse)).unwrap());
        assert_eq!(Action::WriteResponse, Action::try_from(u8::from(Action::WriteResponse)).unwrap());
    }

    #[test]
    fn test_try_from_err() {
        assert!(Action::try_from(255).is_err())
    }
}