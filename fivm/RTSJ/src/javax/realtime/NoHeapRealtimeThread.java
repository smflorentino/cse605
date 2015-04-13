package javax.realtime;

public class NoHeapRealtimeThread extends RealtimeThread {

    public NoHeapRealtimeThread(Runnable logic){
	super(logic);
    }
    public NoHeapRealtimeThread(SchedulingParameters scheduling, 
                                MemoryArea area) {
        super(scheduling, area);
    }

    public NoHeapRealtimeThread(SchedulingParameters scheduling,
				ReleaseParameters release,
				MemoryArea area){
	super(scheduling, release, area);
    }

    public NoHeapRealtimeThread(SchedulingParameters scheduling,
				ReleaseParameters release,
				MemoryParameters memory,
				MemoryArea area,
				ProcessingGroupParameters group,
			       java.lang.Runnable logic){
        super(scheduling, release, memory, area, group, logic);
   }
 
}
