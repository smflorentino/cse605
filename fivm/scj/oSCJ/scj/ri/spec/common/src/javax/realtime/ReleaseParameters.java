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

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * All analysis on safety critical software is performed off line. Although the
 * rtsj allows on-line schedulability analysis, scj assumes any such analysis is
 * performed off line and that the on line environment is predictable.
 * Consequently, the failure hypothesis is that deadlines should not be missed.
 * scj, therefore, provides no mechanisms for coping with cost overruns or
 * deadlines misses. In scj, the support for these mechanism is removed. This is
 * reflected in the ReleaseParameters class hierarchy. In order to constrain the
 * attributes of release parameters, the rtsj classes are restricted.
 * 
 * Note also the absence of an SporadicParameters class.
 * 
 * TODO: implement this (currently we don't need this parameter at all)
 */
@SCJAllowed(LEVEL_0)
public abstract class ReleaseParameters {

//    @SCJProtected
//    protected ReleaseParameters(RelativeTime cost, RelativeTime deadline,
//            AsyncEventHandler overrunHandler, AsyncEventHandler missHandler) {
//    }
//
//    
//    public RelativeTime getDeadline() {
//        return null;
//    }
	
	 
	  protected ReleaseParameters(RelativeTime cost, RelativeTime deadline,
	                              AsyncEventHandler overrunHandler,
	                              AsyncEventHandler missHandler)
	  { }

	  @SCJAllowed
	  protected ReleaseParameters()
	  { }

	  @SCJAllowed(LEVEL_1)
	  protected ReleaseParameters(RelativeTime deadline,
	                              AsyncEventHandler missHandler)
	  { }

	  @SCJAllowed(LEVEL_1)
	  public Object clone()
	  {
	    return null;
	  }

	  @SCJAllowed(LEVEL_1)
	  public AsyncEventHandler getDeadlineMissHandler()
	  {
	    return null;
	  }

	  /**
	   * TBD: whether SCJ makes any use of deadlines or tries to detect
	   * deadline overruns.
	   * <p>
	   * No allocation because RelativeTime is immutable.
	   */ 
	  @SCJRestricted(maySelfSuspend = false)
	  @SCJAllowed(LEVEL_1)
	  public RelativeTime getDeadline() {
	    return null;
	  }
	
}
