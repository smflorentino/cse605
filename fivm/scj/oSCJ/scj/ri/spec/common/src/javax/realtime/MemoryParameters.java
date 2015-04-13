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

package javax.realtime;

// this class is not existing in SCJ spec
public class MemoryParameters {

	public static final long NO_MAX = -1;

	public MemoryParameters(long maxMemoryArea, long maxImmortal)
	throws IllegalArgumentException
	{
		this(maxMemoryArea, maxImmortal,NO_MAX);
	}

	public MemoryParameters(long maxMemoryArea,
			long maxImmortal,
			long allocationRate)
	throws IllegalArgumentException
	{
	}

	public long getAllocationRate() { return 0L; }

	public long getMaxImmortal()    { return 0L; }

	public long getMaxMemoryArea()  { return 0L; }

	public void setAllocationRate(long allocationRate) {}

	public boolean setMaxImmortalIfFeasible(long maximum) { return false; }

	public boolean setMaxMemoryAreaIfFeasible(long maximum) { return false; }

	public boolean setAllocationRateIfFeasible(long allocationRate)
	{
		return false;
	}

	boolean setMaxImmortal(long maximum) { return false; }

	boolean setMaxMemoryArea(long maximum) { return false; }

	public Object clone() { return null; }
}
