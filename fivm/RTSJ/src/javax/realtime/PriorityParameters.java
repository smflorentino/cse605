package javax.realtime;


import com.fiji.fivm.ThreadPriority;

public class PriorityParameters extends SchedulingParameters {
   
    private int priority;
    private int priorityLowLevel;

    private static boolean ValidateRTSJPriority(int priority){
	return (PriorityScheduler.MIN_PRIORITY <= priority && priority <= PriorityScheduler.MAX_PRIORITY);
    }

    private static int RTSJ2FijiFIFO(int priority){
	return priority + (ThreadPriority.priority(ThreadPriority.FIFO_MIN) - PriorityScheduler.MIN_PRIORITY);
    }
    
    private static int PriorityConversion(int priority){
	int prio;
	prio = RTSJ2FijiFIFO(priority) | ThreadPriority.FIFO;	
	if (ThreadPriority.FIFO_MIN <= prio && prio <= ThreadPriority.FIFO_MAX){	   
	    try{
		ThreadPriority.validate(prio);
	    } catch(IllegalArgumentException e){
		throw new IllegalArgumentException("Priority: " + priority + " not supported by VM");
	    }
	    return prio;
	}
	throw new IllegalArgumentException("Priority: " + priority + " not supported by VM");
    }

    public PriorityParameters(){
    }

    public PriorityParameters(int priority) {
	if(ValidateRTSJPriority(priority)){
		this.priorityLowLevel = PriorityConversion(priority);
		this.priority = priority;
		return;
	}
	throw new IllegalArgumentException("Priority: " + priority +  " is not a valid real time priority");
    }

    protected synchronized int getPriorityLowLevel(){
	return priorityLowLevel;
    }

    public synchronized int getPriority(){
	return priority;
    }

    public synchronized void setPriority(int priority){
	if(ValidateRTSJPriority(priority)){
	    this.priorityLowLevel = PriorityConversion(priority);
	    this.priority = priority;
	}
	throw new IllegalArgumentException("Priority: " + priority +  " is not a valid real time priority");
	
    }

   
}
