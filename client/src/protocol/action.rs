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

#[derive(Copy, Clone)]
pub enum Action {
    ReadRequest,
    WriteRequest,
}

impl Into<u8> for Action {
    fn into(self) -> u8 {
        match self {
            Action::ReadRequest => 0x00,
            Action::WriteRequest => 0x01,
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_into_read() {
        let x : u8 = Action::ReadRequest.into();
        assert_eq!(x, 0_u8);
    }

    #[test]
    fn test_into_write() {
        let x : u8 = Action::WriteRequest.into();
        assert_eq!(x, 1_u8);
    }
}