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

import javax.realtime.AffinitySet;
import javax.realtime.HighResolutionTime;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.Scope;		
import javax.safetycritical.annotate.RunsIn;

/**
 * This class provides a collection of static helper methods.
 */
@SCJAllowed
public class Services {

	/**
	 * 
	 * @return Returns the default ceiling priority.
	 *  The default ceiling priority is the PrioritySched- uler.getMaxPriority.4
	 */
	@SCJAllowed(LEVEL_1)
	@RunsIn(CALLER)
	public static int getDefaultCeiling() {
		return 0;
	}
	
	
	/**
	 * Sets the ceiling priority of the first argument. The priority can be in the
	 *  software or hardware priority range. Ceiling priorities are immutable.
	 * @param o
	 * @param pri
	 */
	@SCJAllowed(LEVEL_1)
	@RunsIn(CALLER)
	public static void setCeiling(@Scope(UNKNOWN) Object o, int pri) {}
	
	/**
	 * Captures the stack back trace for the current thread into its
	 *  thread-local stack back trace buffer and remembers that the current 
	 *  contents of the stack back trace buffer is associated with the object 
	 *  represented by the association argument. The size of the stack back trace 
	 *  buffer is determined by the StorageParameters object that is passed as an
	 *  argument to the constructor of the corresponding Schedulable. If the stack
	 *   back trace buffer is not large enough to capture all of the stack back trace
     *   information,
	 *  the information is truncated in an implementation dependent manner.
	 * 
	 * 
	 * @param association
	 */
	@SCJAllowed 
	public static void captureBackTrace(Throwable association) {}
	
	/**
	 * This method is invoked by infrastructure to change the association
	 *  for the thread- local stack back trace buffer to the Class that represents 
	 *  a Throwable that has crossed its scope boundary, at the time that Throwable 
	 *  is replaced with a ThrowBoundary- Error.
	 */
	static void overwriteBackTraceAssociation(Class _class) {}

	/**
	 * Every interrupt has an implementation-defined integer id.
	 * @param InterruptId
	 * @return Returns The priority of the code that the first-level 
	 * interrupts code executes. The returned value is always greater
	 *	 than javax.safetycritical.PriorityScheduler.getMax- Priority().
	 *
	 *  @Throws	 IllegalArgumentException if unsupported InterruptId
	 * 
	 */
	@SCJAllowed(LEVEL_1)
	@RunsIn(CALLER)
	public static int getInterruptPriority(int InterruptId) {
		return 0;
	}
	
	/**
	 * TODO: this is commented out since OVM does not support annotations
	 * @return Returns the deployment level.
	 */
	//@SCJAllowed
	//public static Level getDeploymentLevel() {}
	
	/**
	 * This is like sleep except that it is not interruptible and it uses a HighResolutionTime.
	 * Parameter if delay is a RelativeTime type then it represents 
	 * the number of millisec- onds and nanoseconds to suspend. 
	 * If it is an AbsoluteTime type then delay is the absolute time at which the delay finishes. If delay is time in the past, the method returns immediately.
	 */
	@SCJAllowed(LEVEL_2)
	// TODO: @SCJMayBlock\issue{check} 
	public static void delay(HighResolutionTime delay) {}
	
	/**
	 * This is like sleep except that it is not interruptible and it 
	 * uses nanoseconds instead of milliseconds.
	 * Parameter if delay is the the number of nanoseconds to suspend.
	 * @param delay
	 */
	@SCJAllowed(LEVEL_2) 
	@RunsIn(CALLER)
	public static void delay(int delay) {}
	
	/**
	 * Busy wait spinning loop.
	 * Parameter if delay is a RelativeTime type then it represents 
	 * the number of milliseconds and nanoseconds to spin. 
	 * If it is an AbsoluteTime type then delay is the absolute time at which the spin finishes. If delay is time in the past, the method returns imme- diately.
	 * @param delay
	 */
	// TODO: @ICS
	@SCJAllowed(LEVEL_1)
	public static void spin(HighResolutionTime delay) {}
	
	
	@SCJAllowed(LEVEL_1) 
	@RunsIn(CALLER)
	public static void spin(int nanos) {}
	
	 @SCJAllowed
	public static AffinitySet createSchedulingDomain(java.util.BitSet bitSet) {
		return null;
	}

	 @SCJAllowed(LEVEL_1)
	public static void registerInterruptHandler(int i, InterruptHandler ihandler) {
		
	}

	@SCJAllowed
	@RunsIn(CALLER)
	public static Level getDeploymentLevel() {
		return Level.HIDDEN;
	}

	@SCJAllowed(LEVEL_1)
	@RunsIn(CALLER)
	public static void nanoSpin(int i) {
		
	}
}
