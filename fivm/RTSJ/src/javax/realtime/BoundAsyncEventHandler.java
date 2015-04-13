package javax.realtime;

public class BoundAsyncEventHandler extends AsyncEventHandler {

    public BoundAsyncEventHandler(){
	super();
	this.t.start();
	this.attachCount = false;
    }

    public BoundAsyncEventHandler(SchedulingParameters scheduling,
			     ReleaseParameters release,
			     MemoryParameters memory,
			     MemoryArea area,
			     ProcessingGroupParameters group,
			     boolean nonheap,
			     Runnable logic){
	super(scheduling, release, memory, area, group, nonheap, logic);
	this.t.start();
	this.attachCount = false;
    }
}