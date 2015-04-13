
/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.safetycritical.annotate;

@SCJAllowed
public enum Level {
	@SCJAllowed
	LEVEL_0 {
		@Override
		public int value() {
			return 0;
		}
	},

	@SCJAllowed
	LEVEL_1 {
		@Override
		public int value() {
			return 1;
		}
	},

	@SCJAllowed
	LEVEL_2 {
		@Override
		public int value() {
			return 2;
		}
	};

	public abstract int value();

	public static Level getLevel(String value) {
		if ("0".equals(value))
			return LEVEL_0;
		else if ("1".equals(value))
			return LEVEL_1;
		else if ("2".equals(value))
			return LEVEL_2;
		else
			throw new IllegalArgumentException("The value" + value
					+ " is not a legal level.");
	}
}
