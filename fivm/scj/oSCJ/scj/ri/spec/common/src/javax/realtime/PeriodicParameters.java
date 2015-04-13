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

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public class PeriodicParameters extends ReleaseParameters {

    protected HighResolutionTime _start;
    protected final RelativeTime _period;

    @SCJAllowed
    public PeriodicParameters(HighResolutionTime start, RelativeTime period) {
        if (start == null)
            _start = new AbsoluteTime(0, 0);
        else
            _start = start;

        if (period == null || period.isNegative())
            throw new IllegalArgumentException("Need a period > 0");
        else
            _period = new RelativeTime(period);
    }

    @SCJAllowed
    @SCJRestricted()
    public HighResolutionTime getStart() {
        return _start;
    }

    @SCJAllowed(LEVEL_1)
    @SCJRestricted()
    @RunsIn(CALLER) @Scope(THIS)
    public RelativeTime getPeriod() {
        return _period;
    }

    // avoid defensive copy
    @RunsIn(CALLER) 
    long getPeriodNanos() {
        return _period.toNanos();
    }
}
