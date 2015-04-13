package javax.realtime;

public class RelativeTime extends HighResolutionTime {


    public RelativeTime() {
	this.clock = SystemClock.getSystemClock();
	this.millis = 0;
	this.nanos = 0;
    }

    public RelativeTime(Clock clock) {
	this.clock = clock;
	this.millis = 0;
	this.nanos = 0;
    }

    public RelativeTime(long l, int i) {
	this.clock = SystemClock.getSystemClock();
        this.millis = l;
        this.nanos = i;
    }

    public RelativeTime(long l, int i, Clock clock) {
	this.clock = clock;
	this.millis = l;
        this.nanos = i;
    }

    public RelativeTime(RelativeTime relativeTime) {
	this.clock = SystemClock.getSystemClock();
    }

    public RelativeTime(RelativeTime relativeTime, Clock clock) {
	this.clock = clock;
    }

    public AbsoluteTime absolute(Clock clock) {
	if(clock == null){
	    return Clock.getRealtimeClock().getTime();
	}
	return clock.getTime();
    }

    public AbsoluteTime absolute(Clock clock, AbsoluteTime absoluteTime) { return null; }

    public RelativeTime add(long l, int i) {
	long newMillis = millis+l;
	int newNanos = nanos+i;
	if(millis < 0 || nanos < 0)
	    throw new ArithmeticException();
	//need to canonicalize
	return new RelativeTime(newMillis, newNanos); 
    }

    public RelativeTime add(long l, int i, RelativeTime relativeTime) { return null; }

    public RelativeTime add(RelativeTime relativeTime) { return null; }

    public RelativeTime add(RelativeTime relativeTime, RelativeTime relativeTime1) { return null; }

    /**
     * @deprecated
     */
    public void addInterarrivalTo(AbsoluteTime absoluteTime) {  }

    /**
     * @deprecated
     */
    public RelativeTime getInterarrivalTime() { return null; }

    /**
     * @deprecated
     */
    public RelativeTime getInterarrivalTime(RelativeTime relativeTime) { return null; }

    public RelativeTime relative(Clock clock) { return null; }

    public RelativeTime relative(Clock clock, RelativeTime relativeTime) { return null; }

    public RelativeTime subtract(RelativeTime relativeTime) { return null; }

    public RelativeTime subtract(RelativeTime relativeTime, RelativeTime relativeTime1) { return null; }

    public java.lang.String toString() { return null; }
}