package com.fiji.realtime;


import com.fiji.fivm.ThreadPriority;
import javax.realtime.SchedulingParameters;
import javax.realtime.PriorityScheduler;

public class RRPriorityParameters extends SchedulingParameters {
   
    private int priority;
  

    private static boolean ValidateRTSJPriority(int priority){
	return (PriorityScheduler.MIN_PRIORITY <= priority && priority <= PriorityScheduler.MAX_PRIORITY);
    }

    
    private static int RTSJ2FijiRR(int priority){
	return priority + (ThreadPriority.priority(ThreadPriority.RR_MIN) - PriorityScheduler.MIN_PRIORITY);
    }

    private static int PriorityConversion(int priority){
	int prio;
	prio = RTSJ2FijiRR(priority) | ThreadPriority.RR;
	if (ThreadPriority.RR_MIN <= prio && prio <= ThreadPriority.RR_MAX){
	    try{
		ThreadPriority.validate(prio);
	    } catch(IllegalArgumentException e){
		throw new IllegalArgumentException("Priority: " + priority + " not supported by VM");
	    }
	    return prio;
	}
	throw new IllegalArgumentException("Priority: " + priority + " not supported by VM");
    }


    public RRPriorityParameters(){
    }

    public RRPriorityParameters(int priority) {
	if(ValidateRTSJPriority(priority)){
	    this.priority = PriorityConversion(priority);
	    
	    return;
	}
	throw new IllegalArgumentException("Priority: " + priority +  " is not a valid real time priority");
    }

    public synchronized int getPriority(){
	return priority;
    }

    public synchronized void setPriority(int priority){
	if(ValidateRTSJPriority(priority)){
	    int prio = PriorityConversion(priority);
	    this.priority = prio;
	}
	throw new IllegalArgumentException("Priority: " + priority +  " is not a valid real time priority");
	
    }

   
}
