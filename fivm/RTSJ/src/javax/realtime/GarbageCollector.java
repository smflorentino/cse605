package javax.realtime;

public abstract class GarbageCollector{

    public GarbageCollector(){}

    public abstract RelativeTime getPreemptionLatency();

}