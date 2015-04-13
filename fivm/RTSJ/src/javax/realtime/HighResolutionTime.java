package javax.realtime;

public abstract class HighResolutionTime implements java.lang.Cloneable {

    protected long millis;
    protected int nanos;
    protected Clock clock;

    protected HighResolutionTime() {}

    public abstract AbsoluteTime absolute(Clock clock);

    public abstract AbsoluteTime absolute(Clock clock, AbsoluteTime absoluteTime);

    public Object clone() {
        return null;
    }

    public int compareTo(HighResolutionTime highResolutionTime) {
	if(highResolutionTime == null) 
	    throw new IllegalArgumentException();
	if(!(highResolutionTime.getClass().equals(this.getClass())))
	    throw new ClassCastException();
	if(this.clock != highResolutionTime.clock)
	    throw new IllegalArgumentException();
	if(this.millis < highResolutionTime.millis)
	    return -1;
	if(this.millis > highResolutionTime.millis)
	    return 1;
	if(this.nanos < highResolutionTime.nanos)
	    return -1;
	if(this.nanos > highResolutionTime.nanos)
	    return 1;
	return 0;
    }

    public int compareTo(Object object) {
	if(object == null)
	    throw new IllegalArgumentException();
        if(!(object instanceof HighResolutionTime))
	    throw new ClassCastException();
	HighResolutionTime highResolutionTime = (HighResolutionTime) object;
	if(this.clock != highResolutionTime.clock)
	    throw new IllegalArgumentException();
	if(this.millis < highResolutionTime.millis)
	    return -1;
	if(this.millis > highResolutionTime.millis)
	    return 1;
	if(this.nanos < highResolutionTime.nanos)
	    return -1;
	if(this.nanos > highResolutionTime.nanos)
	    return 1;
	return 0;
    }

    public boolean equals(HighResolutionTime highResolutionTime) {
        if(this.millis == highResolutionTime.millis &&
           this.nanos == highResolutionTime.millis &&
           this.clock == highResolutionTime.clock)
	    return true;
	return false;
    }

    public boolean equals(Object object) {
	if(this.getClass().equals(object.getClass())){
	    HighResolutionTime highResolutionTime = (HighResolutionTime) object;
	    return this.equals(highResolutionTime);
	}
        return false;
    }

    public Clock getClock() {
        return clock;
    }

    public final long getMilliseconds() {
        return millis;
    }


    public final int getNanoseconds() {
        return nanos;
    }

    public int hashCode() {
        return 0;
    }

    public abstract RelativeTime relative(Clock clock);

    public abstract RelativeTime relative(Clock clock, RelativeTime relativeTime);

    public void set(HighResolutionTime highResolutionTime) {
	this.millis = highResolutionTime.millis;
        this.nanos = highResolutionTime.nanos;
	this.clock = highResolutionTime.clock;
    }

    public void set(long l) {
	this.millis = l;
	this.nanos = 0;
    }

    public void set(long l, int i) {
	this.millis = l;
	this.nanos = i;
    }

    public static void waitForObject(Object object, HighResolutionTime highResolutionTime) throws InterruptedException {
    }
}