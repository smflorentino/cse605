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

import java.io.Serializable;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * TBD: do we make this SCJAllowed? It may be that the restrictions put in place
 * for JSR 302 code will guarantee that this exception is never thrown. However,
 * such restrictions are not yet sufficiently defined to allow this
 * determination.
 */
@SCJAllowed
public class InaccessibleAreaException extends RuntimeException implements
        Serializable {

	/**
	   * Shall not copy "this" to any instance or
	   * static field.
	   * <p>
	   * Allocates an application- and implementation-dependent amount of
	   * memory in the current scope (to represent stack backtrace).
	   */
    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public InaccessibleAreaException() {
    }

    /**
     * Shall not copy "this" to any instance or
     * static field. The scope containing the msg argument must enclose the
     * scope containing "this". Otherwise, an IllegalAssignmentError will be
     * thrown.
     * <p>
     * Allocates an application- and implementation-dependent amount of
     * memory in the current scope (to represent stack backtrace).
     */
    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public InaccessibleAreaException(String description) {
        super(description);
    }
}
