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
package javax.safetycritical;

import javax.safetycritical.annotate.Level;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(LEVEL_1)
public abstract class InterruptHandler {
	
	@SCJAllowed(LEVEL_1)
	public synchronized void handleInterrupt() {
	}
	
	@SCJAllowed(LEVEL_1)
	public static int getInterruptPriority(int i) {
		return 0;
	}
}
