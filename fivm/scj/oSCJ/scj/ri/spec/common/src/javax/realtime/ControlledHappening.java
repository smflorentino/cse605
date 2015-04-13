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

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;

/**
 * TODO: implement it when necessary
 */
@SCJAllowed(LEVEL_2)
public class ControlledHappening extends EventHappening {
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public ControlledHappening() {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public ControlledHappening(int id) {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public ControlledHappening(int id, String name) {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public ControlledHappening(String name) {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public final void attach(AsyncEvent ae) {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	protected void process() {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public final void takeControl() {
	};
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	public final void takeControlInterruptible() {
	};
	
	//
	// /**
	// *
	// */
	@SCJAllowed(LEVEL_2)
	protected final Object visit(EventExaminer logic) {
	return null;
	};

}
