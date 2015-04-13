package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;


import edu.purdue.scj.VMSupport;

public class PreciseClock extends Clock {


	/** The resolution of this clock */
	static RelativeTime resolution = null;

	/** No construction allowed */
	private PreciseClock() {
	}

	/**
	 * Utility method for other javax.realtime classes to read the current
	 * time in nanoseconds.
	 */
	static long getCurrentTimeNanos() {
		return VMSupport.getCurrentTime();
	}
	
	static long getCurrentTimePreciseNanos() {
		//return VMSupport.getCurrentTime();
		return VMSupport.getCurrentTimePrecise();
	}

	static long getResolutionNanos() {
		return resolution.toNanos();
	}

	/** Initialize the RTC instance */
	private static PreciseClock instance() {
		long nanosR = VMSupport.getClockResolution();
		long millis = nanosR / HighResolutionTime.NANOS_PER_MILLI;
		int nanos = (int) (nanosR % HighResolutionTime.NANOS_PER_MILLI);
		PreciseClock c = new PreciseClock();
		resolution = new RelativeTime(millis, nanos, c);
		return c;
	}

	@Override
	public RelativeTime getResolution() {
		return new RelativeTime(resolution); // defensive copy
	}

	@Override
	public AbsoluteTime getTime() {
		return getTime(new AbsoluteTime(0, 0, this));
	}


	public AbsoluteTime getTimePrecise() {
		return getTimePrecise(new AbsoluteTime(0, 0, this));
	}
	
	@Override
	public void setResolution(RelativeTime resolution) {
		throw new UnsupportedOperationException();
	}

	public AbsoluteTime getTime(AbsoluteTime time) {
		if (time != null) {
			long nanos = getCurrentTimeNanos();
			long millis = nanos / HighResolutionTime.NANOS_PER_MILLI;
			nanos = (nanos % HighResolutionTime.NANOS_PER_MILLI);
			time.setDirect(millis, (int) nanos);
			time.setClock(this);
		}
		return time;
	}
	
	public AbsoluteTime getTimePrecise(AbsoluteTime time) {
		if (time != null) {
			long nanos = getCurrentTimePreciseNanos();
			long millis = nanos / HighResolutionTime.NANOS_PER_MILLI;
			nanos = (nanos % HighResolutionTime.NANOS_PER_MILLI);
			time.setDirect(millis, (int) nanos);
			time.setClock(this);
		}
		return time;
	}

	/**
	 * Spec 0.73 says: the relative time of the offset of the epoch of this
	 * clock from the Epoch. For the real-time clock it will return a
	 * RelativeTime value equal to 0. A newly allocated RelativeTime object
	 * in the current execution context with the offset past the Epoch for
	 * this clock.
	 * 
	 * The returned object is associated with this clock. -
	 * epoch.setClock(this);
	 * 
	 * @return For the real-time clock it will return a RelativeTime value
	 *         equal to 0.
	 * 
	 */
	public RelativeTime getEpochOffset() {
		RelativeTime epoch = new RelativeTime(0, 0);
		epoch.setClock(this);
		return epoch;
	}

	/**
	 * At Level 0, we do not trigger the eecution of events. Therefore, this
	 * clock is not able to trigger anything. --> we can say that this clock
	 * is "read-only".
	 * 
	 * @return true if and only if this Clock is able to trigger the
	 *         execution of time-driven activities. (Spec 0.73)
	 * 
	 */
	@Override
	@SCJAllowed
	protected boolean drivesEvents() {
		return false;
	}

	@Override
	@SCJAllowed(LEVEL_1)
	protected void registerCallBack(AbsoluteTime time,
			ClockCallBack clockEvent) {
		// this should not be called at Level 0
	}

	@Override
	@SCJAllowed(LEVEL_1)
	protected boolean resetTargetTime(AbsoluteTime time) {
		// this should not be called at Level 0
		return false;
	}

	@Override
	public RelativeTime getResolution(RelativeTime time) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
