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
 *   along with oSCJ.  If not, see <http: www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

/**
 * 
 * Happenings: ----> in SCJ Specificaiton, happenings are still in flux due to
 * dependencies on JSR-282.
 * 
 */
@SCJAllowed(value = LEVEL_1, members = true)
public abstract class Happening {

    /**
	*
	*/
    @SCJAllowed
    public static Happening getHappening(String name) {
        return null;
    }

    /**
	*
	*/
    @SCJAllowed
    public final int getId() {
        return 1;
    }

    /**
	  *
	  */
    @SCJAllowed
    public static int getId(java.lang.String name) {
        return 1;
    }

    /**
	  *
	  */
    @SCJAllowed
    public final String getName() {
        return "Happ";
    }

    /**
	  *
	  */
    @SCJAllowed
    public static boolean isHappening(java.lang.String name) {
        return false;
    }

    /**
	  *
	  */
    @SCJAllowed
    public boolean isRegistered() {
        return false;
    }

    /**
	  *
	 */
    @SCJRestricted(INITIALIZATION)
    public final void register() {
    }

    /**
	  *
	  */
    @SCJAllowed
    public static final boolean trigger(int happeningId) {
        return true;
    }

    /**
	  *
	  */
    @SCJAllowed
    public final void unRegister() {
    }
}
