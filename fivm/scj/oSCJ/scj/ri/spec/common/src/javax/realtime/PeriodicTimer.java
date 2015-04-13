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


//TODO: the interval clock is not used? 
public class PeriodicTimer extends Timer {
//    /** used to keep track of the clock of our interval */
//    Clock intervalClock;
//
//    @SCJProtected
//    public PeriodicTimer(HighResolutionTime start, RelativeTime interval,
//            Clock cclock, AsyncEventHandler handler) {
//        // we always bind with the clock of the start time. The clock
//        // parameter only affects the interval
//        super(start, start == null ? null : start._clock, handler);
//
//        if (interval == null)
//            period = 0;
//        else if (interval.isNegative())
//            throw new IllegalArgumentException("negative time");
//        else
//            period = interval.toNanos();
//
//        // note we never look at interval.getClock here as our clock parameter
//        // always overrides it, even if null. If you want to use interval's
//        // clock then don't use this form of the constructor.
//        intervalClock = (clock != null ? clock : Clock.rtc);
//        periodic = true;
//        gotoState(currentState);
//    }
//
//    @SCJProtected
//    public PeriodicTimer(HighResolutionTime start, RelativeTime interval,
//            AsyncEventHandler handler) {
//        this(start, interval, interval == null ? null : interval._clock, handler);
//    }
}
