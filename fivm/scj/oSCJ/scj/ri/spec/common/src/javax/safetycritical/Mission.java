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

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import edu.purdue.scj.utils.Utils;

@SCJAllowed
public abstract class Mission {

	boolean _terminateAll = false;
	volatile int _phase = Phase.INACTIVE;


	public static class Phase {
		public final static int INITIAL = 0;
		public final static int EXECUTE = 1;
		public final static int CLEANUP = 2;
		public final static int INACTIVE = -1;
		public final static int TERMINATED = -2;
	}

	@SCJAllowed
	@Scope(UNKNOWN) @RunsIn(CALLER) 
	public static Mission getCurrentMission() {
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
		if (!(mem instanceof ManagedMemory))
			throw new Error("Cannot get current mission in non-managed memory");
		return ((ManagedMemory) mem).getManager().getMission();
	}

	@SCJAllowed
	@RunsIn(CALLER)
	public void requestTermination() {
		if (_phase != Phase.TERMINATED) {
			_phase = Phase.TERMINATED;
		}
	}

	@SCJAllowed
	@RunsIn(CALLER)
	public void requestSequenceTermination() {
		_terminateAll = true;
		requestTermination();
	}

	@SCJAllowed
	public abstract long missionMemorySize();

	@SCJAllowed(INFRASTRUCTURE)
	final void run() {
	    //Utils.debugIndentIncrement("###[SCJ] Mission.run : ");
	    
		_terminateAll = false;
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
		if (!(mem instanceof MissionMemory)) { 
		    Utils.panic("Mission not run in mission memory"); 
		}

		MissionManager mngr = new MissionManager(this);
		((MissionMemory) mem).setManager(mngr);

		//Utils.debugPrintln("###[SCJ] Mission.run : INIT");
		_phase = Phase.INITIAL;
		initialize();
		
		//Utils.debugPrintln("###[SCJ] Mission.run : EXECUTE");
		_phase = Phase.EXECUTE;
		exec(mngr);
		
		//Utils.debugPrintln("###[SCJ] Mission.run : CLEAN-UP");
		_phase = Phase.CLEANUP;
		cleanUp();
		_phase = Phase.INACTIVE;
		
		//Utils.debugPrintln("###[SCJ] Mission.run : Mission INACTIVE");
		//Utils.decreaseIndent();
	}

	protected void exec(MissionManager manager) {
	}

	@SCJAllowed(SUPPORT)
	protected abstract void initialize();

	@SCJAllowed(SUPPORT)
	protected void cleanUp() {
	}
	
	@SCJAllowed
	public final boolean terminationPending() {
		return false;
	}
	
	@SCJAllowed
	public final boolean sequenceTerminationPending() {
		return false;
	}
}
