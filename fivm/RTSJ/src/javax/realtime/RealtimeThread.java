package javax.realtime;

import com.fiji.fivm.r1.*;
import com.fiji.fivm.*;

public class RealtimeThread extends Thread implements Schedulable {
    
    private static ThreadStartHook tsh=
        new ThreadStartHook() {
            public void go(Object vmThread,
                           Thread javaThread) {
                RealtimeThread rtt=(RealtimeThread)javaThread;
                rtt.mogrifyReleaseParameters();
            }
        };
    
    private SchedulingParameters scheduling;
    private ReleaseParameters release;
    private MemoryParameters memory;
    private MemoryArea area;
    private ProcessingGroupParameters group;
    private long initialRelease = -1;
    protected Pointer[] scopeStack;
    

    public RealtimeThread() {
    }

    public RealtimeThread(SchedulingParameters scheduling){
	this.scheduling = scheduling;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());	    
	}
    }

    protected RealtimeThread(SchedulingParameters scheduling,
			     MemoryArea area){
	this.scheduling = scheduling;
	this.area = area;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());
	    
	}
    }

    protected RealtimeThread(SchedulingParameters scheduling,
                             ReleaseParameters release,
                             MemoryArea area){
	this.scheduling = scheduling;
	this.release = release;
	this.area = area;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());
	    
	}
    }
    
    public RealtimeThread(SchedulingParameters scheduling,
                          ReleaseParameters release){
	this.scheduling = scheduling;
	this.release = release;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());
	    
	}
    }
    
    public RealtimeThread(SchedulingParameters scheduling,
                          ReleaseParameters release,
                          MemoryParameters memory,
                          MemoryArea area,
                          ProcessingGroupParameters group,
                          Runnable logic){
	super(logic);
	this.scheduling = scheduling;
	this.release = release;
	this.memory = memory;
	this.area = area;
	this.group = group;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());
	    
	}
    }


    public RealtimeThread(Runnable logic){
        super(logic);
    }

    public boolean addIfFeasible(){
	return true;
    }

    public boolean addToFeasibility(){
	return true;
    }

    private void mogrifyReleaseParameters() {
	if(release != null  && release instanceof PeriodicParameters){
	    PeriodicParameters periodic = (PeriodicParameters)release;
	    HighResolutionTime start = periodic.getStart();
	    if (start == null) {
		initialRelease = System.nanoTime();
	    } else if (start instanceof RelativeTime) {
		initialRelease = System.nanoTime() + start.getMilliseconds() * 1000000 + start.getNanoseconds();
		Time.sleepAbsolute(initialRelease);
	    } else if (start instanceof AbsoluteTime) {
		initialRelease = start.getMilliseconds() * 1000000 + start.getNanoseconds();
		long now = System.nanoTime();
		if (initialRelease < now) {
		    initialRelease = now;
		} else {
		    Time.sleepAbsolute(initialRelease);
		}
	    }
	}
    }

    public void start() {
        fivmSupport.registerThreadStartHook(this,tsh);
	super.start();
    }

    public static RealtimeThread currentRealtimeThread() {
	return (RealtimeThread)Thread.currentThread();
    }

    public void deschedulePeriodic(){
    }

    
    // public static MemoryArea getCurrentMemoryArea() {
    //    
    //}

    public static int getInitialMemoryAreaIndex(){
	return 0;
    }

    public MemoryArea getMemoryArea(){
	return area;
    }

    public static int getMemoryAreaStackDepth(){
	return 0;
    }

    public MemoryParameters getMemoryParameters(){
	return memory;
    }
    
    public ProcessingGroupParameters getProcessingGroupParameters(){
	return group;
    }

    public ReleaseParameters getReleaseParameters(){
	return release;
    }

    public SchedulingParameters getSchedulingParameters(){
	return scheduling;
    }
     
    public boolean waitForNextPeriod() {
	if(release != null && release instanceof PeriodicParameters){
	    RelativeTime period = ((PeriodicParameters)release).getPeriod();
	    long periodL = period.getMilliseconds()*1000000 + period.getNanoseconds();
	    long time = System.nanoTime() - initialRelease;
	    long periodCount = (time + periodL - 1) / periodL;
	    long nextPeriod = periodCount * periodL;
	    long nextTime = nextPeriod + initialRelease;
	    while(System.nanoTime() < nextTime){
		Time.sleepAbsolute(nextTime);
		Thread.interrupted();
	    }
	    RelativeTime deadline = release.getDeadline();
	    if(deadline != null){
	    	long currentTime = System.nanoTime();
	    	if(currentTime > deadline.getMilliseconds()*1000000 + deadline.getNanoseconds())
	    	    release.getDeadlineMissHandler().handleAsyncEvent();
	    }
	    return true;
	   
	}
	throw new IllegalThreadStateException();
    }

    public boolean waitForNextPeriodInterruptable() throws InterruptedException{

	if(release != null && release instanceof PeriodicParameters){
	    RelativeTime period = ((PeriodicParameters)release).getPeriod();
	    long periodL = period.getMilliseconds()*1000000 + period.getNanoseconds();
	    long time = System.nanoTime() - initialRelease;
	    long periodCount = (time + periodL - 1) / periodL;
	    long nextPeriod = periodCount * periodL;
	    long nextTime = nextPeriod + initialRelease;
	    Time.sleepAbsolute(nextTime);
	    RelativeTime deadline = release.getDeadline();
	    if(deadline != null){
		long currentTime = System.nanoTime();
		if(currentTime > deadline.getMilliseconds()*1000000 + deadline.getNanoseconds())
		    release.getDeadlineMissHandler().handleAsyncEvent();
	    }
	}
	if (Thread.interrupted())
	    throw new InterruptedException();
	return true;

    }

     public boolean setIfFeasible(ReleaseParameters release,
				 MemoryParameters memory){
	this.release = release;
	this.memory = memory;
	return true;
    }

    public boolean setIfFeasible(ReleaseParameters release,
				 MemoryParameters memory,
				 ProcessingGroupParameters group){
	this.release = release;
	this.memory = memory;
	this.group = group;
	return true;
    }

    public boolean setIfFeasible(ReleaseParameters release,
				 ProcessingGroupParameters group){
	this.release = release;
	this.group = group;
	return true;
    }

    public boolean setIfFeasible(SchedulingParameters scheduling,
				 ReleaseParameters release,
				 MemoryParameters memory){
	this.scheduling = scheduling;
	this.release = release;
	this.memory = memory;
	return true;
    }

    public boolean setIfFeasible(SchedulingParameters scheduling,
				 ReleaseParameters release,
				 MemoryParameters memory,
				 ProcessingGroupParameters group){
	this.scheduling = scheduling;
	this.release = release;
	this.memory = memory;
	this.group = group;
	return true;
    }

    public void setMemoryParameters(MemoryParameters memory){
	this.memory = memory;
    }

    public boolean setMemoryParametersIfFeasible(MemoryParameters memory){
	this.memory = memory;
	return true;
    }

    public void setProcessingGroupParameters(ProcessingGroupParameters group){
	this.group = group;
    }

    public boolean setProcessingGroupParametersIfFeasible(ProcessingGroupParameters group){
	this.group = group;
	return true;
    }

    public void setReleaseParameters(ReleaseParameters release){
	this.release = release;
    }

    public boolean setReleaseParametersIfFeasible(ReleaseParameters release){
	this.release = release;
	return true;
    }

    public void setScheduler(Scheduler scheduler){
    }

    public void setScheduler(Scheduler scheduler,
			     SchedulingParameters scheduling,
			     ReleaseParameters release,
			     MemoryParameters memory,
			     ProcessingGroupParameters group){
	this.scheduling = scheduling;
	this.release = release;
	this.memory = memory;
	this.group = group;
    }

    public void setSchedulingParameters(SchedulingParameters scheduling){
	this.scheduling = scheduling;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());
	}
    }
    
    public boolean setSchedulingParametersIfFeasible(SchedulingParameters scheduling){
	this.scheduling = scheduling;
	if(scheduling instanceof PriorityParameters){
	    this.setPriority(((PriorityParameters)scheduling).getPriorityLowLevel());
	}
	return true;
    }

   
   
}
