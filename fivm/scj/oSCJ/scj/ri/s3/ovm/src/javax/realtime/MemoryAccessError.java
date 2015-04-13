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

/**
 * This error is thrown on an attempt to refer to an object in
 * an inaccessible {@link MemoryArea}. For example this
 * will be thrown if logic in a {@link NoHeapRealtimeThread}
 * attempts to refer to an object in the traditional Java heap.
 *
 * @spec RTSJ 1.0.1 - from OVM/RTSJ
 */
public class MemoryAccessError extends Error {

    /**
     * A constructor for <tt>MemoryAccessError</tt>.
     */
    public MemoryAccessError() {}

    /**
     * A descriptive constructor for <tt>MemoryAccessError</tt>.
     *
     * @param description Description of the error.
     */
    public MemoryAccessError(String description) {
        super(description);
    }
}
