package javax.realtime;

public class SystemClock extends Clock {

    private static SystemClock singleton = new SystemClock();

    private SystemClock(){
    }
    
    protected static SystemClock getSystemClock(){
	return singleton;
    }

    public RelativeTime getResolution() {
	return new RelativeTime();
    }
    
    public AbsoluteTime getTime() {
        AbsoluteTime result=new AbsoluteTime();
        long time=System.nanoTime();
        result.nanos=(int)(time%1000000);
        result.millis=time/1000000;
        return result;
    }
    
    public AbsoluteTime getTime(AbsoluteTime dest) {
        long time=System.nanoTime();
        dest.nanos=(int)(time%1000000);
        dest.millis=time/1000000;
        return dest;
    }
    
    public RelativeTime getEpochOffset() {
	return new RelativeTime();
    }
    
    public void setResolution(RelativeTime resolution) { } 
    
    
    
}
