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
import javax.safetycritical.annotate.SCJRestricted;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.LEVEL_1;

@SCJAllowed(LEVEL_2)
public class NoHeapRealtimeThread extends RealtimeThread {

    public NoHeapRealtimeThread(SchedulingParameters scheduling, MemoryArea area) {
        this(scheduling, null, null, area, null, null);
    }

    public NoHeapRealtimeThread(SchedulingParameters scheduling,
            ReleaseParameters release, MemoryArea area) {
        this(scheduling, release, null, area, null, null);
    }

    public NoHeapRealtimeThread(SchedulingParameters scheduling,
            ReleaseParameters release, MemoryParameters memory,
            MemoryArea area, ProcessingGroupParameters group, Runnable logic) {
        super(scheduling, release, memory, area, group, logic);
    }

    public String toString() {
        return "NoHeap" + super.toString();
    }
    
    @SCJAllowed(LEVEL_2)
	@SCJRestricted()
    public void start() {
    	
    }
}
