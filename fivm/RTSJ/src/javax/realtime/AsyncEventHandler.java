package javax.realtime;


public class AsyncEventHandler implements Schedulable, Runnable {

    protected RealtimeThread t;
    private int fireCount;
    private Runnable logic;
    protected boolean attachCount;
    private int priority;

    public AsyncEventHandler() {
	this.t = new RealtimeThread(this);
	this.fireCount = 0;
	this.logic = null;
	this.attachCount = true;
	this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }
    
    public AsyncEventHandler(boolean nonheap) {
	if(nonheap)
	    this.t = new NoHeapRealtimeThread(this);
	else
	    this.t = new RealtimeThread(this);
	this.fireCount = 0;
	this.logic = null;
	this.attachCount = true;
	this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }
    
    public AsyncEventHandler(boolean nonheap, Runnable logic) {
	if(nonheap)
	    this.t = new NoHeapRealtimeThread(this);
	else
	    this.t = new RealtimeThread(this);
	this.fireCount = 0;
	this.logic = logic;
	this.attachCount = true;
	this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }

    public AsyncEventHandler(Runnable logic) {
	this.t = new RealtimeThread(this);	
	this.fireCount = 0;
	this.logic = logic;
	this.attachCount = true;
	this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }
    
    public AsyncEventHandler(SchedulingParameters scheduling,
			     ReleaseParameters release,
			     MemoryParameters memory,
			     MemoryArea area,
			     ProcessingGroupParameters group,
 			     boolean nonheap){
	RealtimeThread rt = RealtimeThread.currentRealtimeThread();
	SchedulingParameters sched;
	MemoryArea mem;
	ReleaseParameters rel;
	if(scheduling == null)
	    sched = rt.getSchedulingParameters();
	else
	    sched = scheduling;
	if(release == null)
	    rel = new ReleaseParameters();
	else
	    rel = release;
	if(area == null)
	    mem = rt.getMemoryArea();
	else
	    mem = area;
	if(nonheap)
	    this.t = new NoHeapRealtimeThread(sched, rel, memory, mem, group, this);
	else
	    this.t = new RealtimeThread(sched, rel, memory, mem, group, this);
	this.fireCount = 0;
	this.logic = null;
	//add error check for nonheap
	this.attachCount = true;
	if(sched instanceof PriorityParameters)
	    this.priority = ((PriorityParameters) sched).getPriority();
        else
	    this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }

    public AsyncEventHandler(SchedulingParameters scheduling,
			     ReleaseParameters release,
			     MemoryParameters memory,
			     MemoryArea area,
			     ProcessingGroupParameters group,
			     boolean nonheap,
			     Runnable logic){
	RealtimeThread rt = RealtimeThread.currentRealtimeThread();
	SchedulingParameters sched;
	MemoryArea mem;
	ReleaseParameters rel;
	if(scheduling == null)
	    sched = rt.getSchedulingParameters();
	else
	    sched = scheduling;
	if(release == null)
	    rel = new ReleaseParameters();
	else
	    rel = release;
	if(area == null)
	    mem = rt.getMemoryArea();
	else
	    mem = area;
	if(nonheap)
	    this.t = new NoHeapRealtimeThread(sched, rel, memory, mem, group, this);
	else
	    this.t = new RealtimeThread(sched, rel, memory, mem, group, this);
	this.fireCount = 0;
	this.logic = logic;
	//add error check for nonheap
	this.attachCount = true;
	if(sched instanceof PriorityParameters)
	    this.priority = ((PriorityParameters) sched).getPriority();
        else
	    this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }

    public AsyncEventHandler(SchedulingParameters scheduling,
			     ReleaseParameters release,
			     MemoryParameters memory,
			     MemoryArea area,
			     ProcessingGroupParameters group,
			     Runnable logic){
	RealtimeThread rt = RealtimeThread.currentRealtimeThread();
	SchedulingParameters sched;
	MemoryArea mem;
	ReleaseParameters rel;
	if(scheduling == null)
	    sched = rt.getSchedulingParameters();
	else
	    sched = scheduling;
	if(release == null)
	    rel = new ReleaseParameters();
	else
	    rel = release;
	if(area == null)
	    mem = rt.getMemoryArea();
	else
	    mem = area;
	this.t = new RealtimeThread(sched, rel, memory, mem, group, this);
	this.fireCount = 0;
	this.logic = logic;
	this.attachCount = true;
	if(sched instanceof PriorityParameters){
	    this.priority = ((PriorityParameters) sched).getPriority();
	}
        else
	    this.priority = PriorityScheduler.instance().getMinPriority();
	this.t.setDaemon(true);
    }

    protected synchronized void attach(){
	if(attachCount){
	    this.t.start();
	    this.attachCount = false;
	}
    }

    public boolean addIfFeasible(){
	return true;
    }

    public boolean addToFeasibility(){
	return true;
    }

    protected synchronized int getAndClearPendingFireCount(){
	int numberOfFires = this.fireCount;
	this.fireCount = 0;
	return numberOfFires;
    }

    protected synchronized int getAndDecrementPendingFireCount(){
	int numberOfFires = this.fireCount;
	if(numberOfFires > 0)
	    this.fireCount = numberOfFires - 1;
	return numberOfFires;
    }

    protected synchronized int getAndIncrementPendingFireCount(){
	int numberOfFires = this.fireCount;
	this.fireCount = numberOfFires + 1;
	return numberOfFires;
    }

    public MemoryArea getMemoryArea(){
	return t.getMemoryArea();
    }

    public MemoryParameters getMemoryParameters(){
	return t.getMemoryParameters();
    }

    protected synchronized int getPendingFireCount(){
	return fireCount;
    }

    protected int getPriority(){
	return priority;
    }

    public ProcessingGroupParameters getProcessingGroupParameters(){
	return t.getProcessingGroupParameters();
    }

    public ReleaseParameters getReleaseParameters(){
	return t.getReleaseParameters();
    }

    public Scheduler getScheduler(){
	return PriorityScheduler.instance();
    }

    public SchedulingParameters getSchedulingParameters(){
	return t.getSchedulingParameters();
    }

    public void  handleAsyncEvent() {
	if(this.logic != null)
	    logic.run();
    }

    public boolean isDaemon(){
	return this.t.isDaemon();
    }

    public boolean removeFromFeasibility(){
	return false;
    }

    public final void run() {
	boolean fire = false;
        while(fireCount >= 0){
	    synchronized(this){
		if(fireCount > 0){
		    this.fireCount = fireCount - 1;
		    fire = true;
		}
		else{
		    try{  
			wait();
		    } catch(InterruptedException e){}
		}
	    }
	    try{
		if(fire){
		    handleAsyncEvent();
		    fire = false;
		}
	    } catch (Throwable th){
		th.printStackTrace();
	    }
	}
    }

    public void setDaemon(boolean on){
	this.t.setDaemon(on);
    }

    public boolean setIfFeasible(ReleaseParameters release,
				 MemoryParameters memory){
	return t.setIfFeasible(release, memory);
    }

    public boolean setIfFeasible(ReleaseParameters release,
				 MemoryParameters memory,
				 ProcessingGroupParameters group){
	return t.setIfFeasible(release, memory, group);
    }

    public boolean setIfFeasible(ReleaseParameters release,
				 ProcessingGroupParameters group){
	return t.setIfFeasible(release, group);
    }

    public boolean setIfFeasible(SchedulingParameters scheduling,
				 ReleaseParameters release,
				 MemoryParameters memory){
	return t.setIfFeasible(scheduling, release, memory);
    }

    public boolean setIfFeasible(SchedulingParameters scheduling,
				 ReleaseParameters release,
				 MemoryParameters memory,
				 ProcessingGroupParameters group){
	return t.setIfFeasible(scheduling, release, memory, group);
    }

    public void setMemoryParameters(MemoryParameters memory){
	t.setMemoryParameters(memory);
    }

    public boolean setMemoryParametersIfFeasible(MemoryParameters memory){
	t.setMemoryParameters(memory);
	return true;
    }

    public void setProcessingGroupParameters(ProcessingGroupParameters group){
	t.setProcessingGroupParameters(group);
    }

    public boolean setProcessingGroupParametersIfFeasible(ProcessingGroupParameters group){
	t.setProcessingGroupParameters(group);
	return true;
    }

    public void setReleaseParameters(ReleaseParameters release){
	t.setReleaseParameters(release);
    }

    public boolean setReleaseParametersIfFeasible(ReleaseParameters release){
	t.setReleaseParameters(release);
	return true;
    }

    public void setScheduler(Scheduler scheduler){
	t.setScheduler(scheduler);
    }

    public void setScheduler(Scheduler scheduler,
			     SchedulingParameters scheduling,
			     ReleaseParameters release,
			     MemoryParameters memory,
			     ProcessingGroupParameters group){
	t.setScheduler(scheduler, scheduling, release, memory, group);
    }

    public void setSchedulingParameters(SchedulingParameters scheduling){
	t.setSchedulingParameters(scheduling);
    }
    
    public boolean setSchedulingParametersIfFeasible(SchedulingParameters scheduling){
	t.setSchedulingParameters(scheduling);
	return true;
    }
}
