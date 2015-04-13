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

import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public abstract class HighResolutionTime implements Comparable {

    /*
     * Note that the time is always kept in a normalized form, where both
     * components always have the same sign or are zero, and nanos is always in
     * the range -999999 <= nanoseconds <= 999999. While a time may hold a value
     * greater than that representable by a long nanosecond value, we convert to
     * such a value when we use this time for sleeps/waits etc.
     */
    Clock _clock;
    long _milliseconds;
    int _nanoseconds;
    static final boolean CHECK_OVERFLOW = true;
    static final int NANOS_PER_MILLI = 1000 * 1000;

    /*
     * We mirror all the constructor forms needed by the subclasses. This is
     * necessary because we want HRT to take full responsibility for clock
     * checking and normalization, but the subclasses have to check for null
     * parameters (and throw IllegalArgumentException) which means they can't
     * invoke super constructors unless the super constructor does the null
     * check - hence the super constructor must take the same parameter types
     */
    HighResolutionTime(long millis, long nanos, Clock clock) {
        if (!setNormalized(millis, nanos))
            throw new IllegalArgumentException("non-normalizable values");
        setClock(clock);
    }

    
    @SCJRestricted(maySelfSuspend = false)
    @SCJAllowed
    @RunsIn(CALLER)
    public int compareTo(@Scope(UNKNOWN) HighResolutionTime time) {
        if (time == null)
            throw new IllegalArgumentException("null parameter");
        if (getClass() != time.getClass())
            throw new ClassCastException();
        if (_clock != time._clock)
            throw new IllegalArgumentException("different clocks");
        if (_milliseconds > time._milliseconds)
            return 1;
        else if (_milliseconds < time._milliseconds)
            return -1;
        else
            return _nanoseconds - time._nanoseconds;
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public int compareTo(java.lang.Object object) {
        return compareTo((HighResolutionTime) object);
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public boolean equals(HighResolutionTime time) {
        return _nanoseconds == time._nanoseconds
                && _milliseconds == time._milliseconds && _clock == time._clock;
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    public boolean equals(java.lang.Object object) {
        if (object instanceof HighResolutionTime)
            return equals((HighResolutionTime) object);
        return false;
    }

    @SCJAllowed
    @SCJRestricted()
    @RunsIn(CALLER) @Scope(UNKNOWN)
    public Clock getClock() {
        return _clock;
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    @RunsIn(CALLER)
    public int hashCode() {
        // what would be a good hashcode?
        return (int) (_milliseconds ^ _nanoseconds ^ _clock.hashCode());
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    @RunsIn(CALLER)
    public final long getMilliseconds() {
        return _milliseconds;
    }

    @SCJAllowed
    @SCJRestricted(maySelfSuspend = false)
    @RunsIn(CALLER)
    public final int getNanoseconds() {
        return _nanoseconds;
    }

    @SCJAllowed
    @SCJRestricted()
    @RunsIn(CALLER)
    public void set(HighResolutionTime time) {
    	//TODO:
    }
    
    @SCJAllowed
    @SCJRestricted()
    @RunsIn(CALLER)
    public void set(long millis) {
    	//TODO:
    }
    
    @SCJAllowed
    @SCJRestricted()
    @RunsIn(CALLER)
    public void set(long millis, int nanos) {
    	//TODO:
    }
    
    /**
     * Behaves exactly like <code>target.wait()</code> but with the enhancement
     * that it waits with a precision of <code>HighResolutionTime</code>.
     */
    @SCJAllowed(LEVEL_2)
    @SCJRestricted()
    public static void waitForObject(java.lang.Object target,
            HighResolutionTime time) throws java.lang.InterruptedException {
        if (target == null)
            throw new NullPointerException("null target");

        if (time != null) {
            if (time._clock != Clock.rtc)
                throw new UnsupportedOperationException("Incompatible clock");

            if (time instanceof AbsoluteTime) {
                time = ((AbsoluteTime) time).subtract(Clock.rtc.getTime());
                target.wait(time.getMilliseconds(), time.getNanoseconds());
            } else {
                if (time.isNegative())
                    throw new IllegalArgumentException("negative relative time");
                else
                    target.wait(time.getMilliseconds(), time.getNanoseconds());
            }
        } else
            target.wait();
    }

    /**
     * Note: it is not "safe" to automatically convert from one clock basis to
     * another.
     */
    public abstract RelativeTime relative(Clock clock);

    /**
     * Convert this time to a pure nanos value for use with sleeps or waits. The
     * time value must be known to be positive and if it overflows a long then
     * set to <tt>Long.MAX_VALUE</tt> - we're not going to be able to wait 256
     * years (291 less 35 since epoch) to get the bug report.
     * 
     * @return the time value of this in nanoseconds, or <tt>Long.MAX_VALUE</tt>
     *         if this time value overflows a long
     */
    final long toNanos() {
        long nanos = _milliseconds * NANOS_PER_MILLI + _nanoseconds;
        return nanos < 0 ? Long.MAX_VALUE : nanos;
    }

    final boolean isNegative() {
        return _milliseconds < 0 || _nanoseconds < 0;
    }

    final boolean isZero() {
        return _milliseconds == 0 && _nanoseconds == 0;
    }

    /**
     * Normalize the given millis and nanos components and set them in this.
     * 
     * @return <tt>true</tt> if the normalized values could be set, and
     *         <tt>false</tt> if overflow would occur. If <tt>false</tt> is
     *         returned then the millisecond and nanosecond components of this
     *         are unchanged.
     */
    @RunsIn(CALLER)
    final boolean setNormalized(final long millis, final long nanos) {
        final long millis_in_nanos = nanos / NANOS_PER_MILLI;
        final int nanosleft = (int) (nanos % NANOS_PER_MILLI);
        if (millis > 0) {
            if (nanos < 0) { // no overflow possible
                _milliseconds = millis + millis_in_nanos;
                // ensure same sign
                if (_milliseconds > 0 && nanosleft != 0) {
                    _milliseconds--;
                    _nanoseconds = nanosleft + NANOS_PER_MILLI;
                } else {
                    _nanoseconds = nanosleft;
                }
            } else { // watch for overflow
                long tmp = millis + millis_in_nanos;
                if (tmp <= 0) {
                    return false;
                }
                _milliseconds = tmp;
                _nanoseconds = nanosleft;
            }
        } else if (millis < 0) {
            if (nanos < 0) { // watch for negative overflow
                long tmp = millis + millis_in_nanos;
                if (tmp >= 0) {
                    return false;
                }
                _milliseconds = tmp;
                _nanoseconds = nanosleft;
            } else { // no overflow possible
                _milliseconds = millis + millis_in_nanos;
                // ensure same sign
                if (_milliseconds < 0 && nanosleft != 0) {
                    _milliseconds++;
                    _nanoseconds = nanosleft - NANOS_PER_MILLI;
                } else {
                    _nanoseconds = nanosleft;
                }
            }
        } else { // millis == 0
            _milliseconds = millis_in_nanos;
            _nanoseconds = nanosleft;
        }

        return true;
    }

    final void setClock(Clock clock) {
        _clock = clock == null ? Clock.rtc : clock;
    }

    /**
     * Set the millis and nanos component of this to be the same as this given.
     * This is only called when we know the values are already normalized.
     */
    final void setDirect(long millis, int nanos) {
        _milliseconds = millis;
        _nanoseconds = nanos;
    }

    HighResolutionTime add(long millis, int nanos, HighResolutionTime dest) {
        assert dest != null;

        if (!dest.setNormalized(addSafe(_milliseconds, millis),
                ((long) _nanoseconds) + nanos))
            throw new ArithmeticException("non-normalizable result");

        dest.setClock(_clock);

        return dest;
    }

    /**
     * Adds the two given values together, returning their sum if there is no
     * overflow.
     */
    static long addSafe(long arg1, long arg2) {
        long sum = arg1 + arg2;
        if (CHECK_OVERFLOW)
            if ((arg1 > 0 && arg2 > 0 && sum <= 0)
                    || (arg1 < 0 && arg2 < 0 && sum >= 0))
                throw new ArithmeticException("overflow");

        return sum;
    }
    
    
}
