package javax.realtime;

public class AperiodicParameters extends ReleaseParameters {

    public static final String arrivalTimeQueueOverflowExcept = "EXCEPT";
    public static final String arrivalTimeQueueOverflowIgnore = "IGNORE";
    public static final String arrivalTimeQueueOverflowReplace = "REPLACE";
    public static final String arrivalTimeQueueOverflowSave = "SAVE";
    
    private RelativeTime cost;
    private RelativeTime deadline;
    private AsyncEventHandler overrunHandler;
    private AsyncEventHandler missHandler;
    private String queueOverflowPolicy;
    private int queueLength;

    public AperiodicParameters(){
	this.cost = null;
	this.deadline = null;
	this.overrunHandler = null;
	this.missHandler = null;
	this.queueOverflowPolicy = arrivalTimeQueueOverflowIgnore;
	this.queueLength = 0;
    }
    
    public AperiodicParameters(RelativeTime cost,
			       RelativeTime deadline,
			       AsyncEventHandler overrunHandler,
			       AsyncEventHandler missHandler){
	if(cost == null)
	    this.cost = new RelativeTime(0,0); 
	else{
	    if((cost.getMilliseconds()*1000000 + cost.getNanoseconds()) < 0)
		throw new IllegalArgumentException("Negative cost in AperiodicParameters");
	    this.cost = cost;
	}
	if(deadline == null)
	    this.deadline = new RelativeTime(Long.MAX_VALUE,999999); 
	else{
	    if((deadline.getMilliseconds()*1000000 + deadline.getNanoseconds()) <= 0)
		throw new IllegalArgumentException("Negative deadline in AperiodicParameters");
	    this.deadline = deadline;
	}
	this.overrunHandler = overrunHandler;
	this.missHandler = missHandler;
	this.queueOverflowPolicy = arrivalTimeQueueOverflowIgnore;
	this.queueLength = 0;
    }

    public String getArrivalTimeQueueOverflowBehavoir(){
	return queueOverflowPolicy;
    }

    public int getInitialArrivalTimeQueueLenght(){
	return queueLength;
    }

    public void serArrivalTimeQueueOverflowBehavoir(String behavior){
	if(behavior == arrivalTimeQueueOverflowExcept ||
           behavior == arrivalTimeQueueOverflowIgnore ||
	   behavior == arrivalTimeQueueOverflowReplace ||
	   behavior == arrivalTimeQueueOverflowSave)
	    this.queueOverflowPolicy = behavior;
	else
	   throw new IllegalArgumentException("Invalid arrivalTimeQueueOverflow behavoir in AperiodicParameters"); 
    }

    public void setDeadline(RelativeTime deadline){
	if((deadline.getMilliseconds()*1000000 + deadline.getNanoseconds()) <= 0)
		throw new IllegalArgumentException("Negative deadline in AperiodicParameters");
	this.deadline = deadline;
    }

    public boolean setIfFeasible(RelativeTime cost,
				 RelativeTime deadline){
	if(cost == null)
	    this.cost = new RelativeTime(0,0); 
	else{
	    if((cost.getMilliseconds()*1000000 + cost.getNanoseconds()) < 0)
		throw new IllegalArgumentException("Negative cost in AperiodicParameters");
	    this.cost = cost;
	}
	if(deadline == null)
	    this.deadline = new RelativeTime(Long.MAX_VALUE,999999); 
	else{
	    if((deadline.getMilliseconds()*1000000 + deadline.getNanoseconds()) <= 0)
		throw new IllegalArgumentException("Negative deadline in AperiodicParameters");
	    this.deadline = deadline;
	}
	return false;
    }
    
    public void setInitialArrivalTimeQueueLenght(int initial){
	this.queueLength = initial;
    }
}