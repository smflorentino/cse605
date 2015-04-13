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

import edu.purdue.scj.VMSupport;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
/**
 * TBD: we need additional methods to allow SizeEstimation of thread stacks. In
 * particular, we need to be able to reserve memory for backing store. Perhaps
 * this belongs in a javax.safetycritical variant of SizeEstimator.
 * 
 * SIZE OF THE MONITOR issue:
 *  Lei: can we let VM decide the size of monitor for us?
 *  	Ales: VM will decide this for us. 
 *  
 */
@SCJAllowed
public final class SizeEstimator {

    /** the number of bytes we've reserved so far */
    private long size = 0;

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public SizeEstimator() {
    }

    /**
     * JSR 302 tightens the semantic requirements on the implementation of
     * getEstimate. For compliance with JSR 302, getEstimate() must return a
     * conservative upper bound on the amount of memory required to represent
     * all of the memory reservations associated with this SizeEstimator object.
     */

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public long getEstimate() {
        return size;
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public void reserve(Class clazz, int num) {
        if (clazz != null)
            size += num * VMSupport.sizeOf(clazz);
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public void reserve(SizeEstimator estimator) {
        // behavior on null not specified
        reserve(estimator, 1);
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public void reserve(SizeEstimator estimator, int num) {
        // behavior on null not specified
        if (estimator != null)
            size += num * estimator.size;
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public void reserveArray(int length) {
        if (length < 0)
            throw new IllegalArgumentException("negative length");
        else
            size += VMSupport.sizeOfReferenceArray(length);
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public void reserveArray(int length, Class clazz) {
        if (length < 0)
            throw new IllegalArgumentException("negative length");
        else if (clazz == null || !clazz.isPrimitive())
            throw new IllegalArgumentException("type is not a primitive type");
        else
            size += VMSupport.sizeOfPrimitiveArray(length, clazz);
    }
}
