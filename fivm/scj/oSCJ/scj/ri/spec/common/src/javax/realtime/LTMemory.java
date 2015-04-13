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


import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Scope.CALLER;
import javax.safetycritical.annotate.RunsIn;


import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;


@SCJAllowed
public class LTMemory extends ScopedMemory {

    @SCJAllowed(INFRASTRUCTURE)
    public LTMemory(long size) {
        super(size);
    }

    @SCJAllowed(INFRASTRUCTURE)
    public LTMemory(SizeEstimator estimator) {
        super(estimator);
    }

	//@Override
    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    @RunsIn(CALLER)
	public void resize(long size) throws IllegalStateException {
		// TODO: resizing Scoped Memory, should we implement this?		
	}
    
    
}
