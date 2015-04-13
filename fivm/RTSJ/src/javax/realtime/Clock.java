package javax.realtime;

public abstract class Clock {

    public static Clock getRealtimeClock() {
	return (Clock) SystemClock.getSystemClock();
    }
    
    public abstract RelativeTime getResolution();
    
    public abstract AbsoluteTime getTime();
    
    public abstract AbsoluteTime getTime(AbsoluteTime dest);
    
    public abstract RelativeTime getEpochOffset();
    
    public abstract void setResolution(RelativeTime resolution);
    
    
    
}
