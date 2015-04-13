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

import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * 
 * The BoundAsyncEventHandler class is not used directly by the application.
 * Hence none of its methods or constructors are publicly available.
 * 
 * @author plsek
 * 
 */
// TODO: Compared to AEH, this guy should be really made bounding to a thread
@SCJAllowed
public class BoundAsyncEventHandler extends AsyncEventHandler {

    public BoundAsyncEventHandler(SchedulingParameters scheduling,
            ReleaseParameters release, MemoryParameters memory,
            MemoryArea area, ProcessingGroupParameters group, boolean nonheap,
            Runnable logic) {
        super(scheduling, release, memory, area, group, logic);
    }

    // TODO:
    public BoundAsyncEventHandler(PriorityParameters priority, Object object,
            StorageParameters storage, String name) {
        super(null, null, null, null, null, null);
    }
}
