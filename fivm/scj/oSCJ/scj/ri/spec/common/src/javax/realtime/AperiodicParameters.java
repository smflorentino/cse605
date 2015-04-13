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

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * 
 * Construct a new object within the current memory area.
 * 
 * @author plsek
 *
 */
@SCJAllowed(LEVEL_1)
public class AperiodicParameters extends ReleaseParameters {
    //
    // // JAN: FIX
	// @SCJAllowed(LEVEL_1)
    // protected AperiodicParameters(RelativeTime cost, RelativeTime deadline,
    // AsyncEventHandler overrunHandler, AsyncEventHandler missHandler) {
    // super(cost, deadline, overrunHandler, missHandler);
    // }
	
	
	@SCJAllowed(LEVEL_1)
	public AperiodicParameters(RelativeTime deadline, AsyncEventHandler missHandler)
	{
	  // TODO:	
	  //super(null, null, null, null);
	}
}
