package javax.realtime;

public class ReleaseParameters extends ParameterBase {

    private RelativeTime cost;
    private RelativeTime deadline;
    private AsyncEventHandler overrunHandler;
    private AsyncEventHandler missHandler;


    protected ReleaseParameters() {
	this.cost = new RelativeTime(0,0);
    }

    protected ReleaseParameters(RelativeTime cost, 
				RelativeTime deadline, 
				AsyncEventHandler overrunHandler, 
				AsyncEventHandler missHandler) {
	if(cost == null)
	   this.cost = new RelativeTime(0,0); 
	else
	    this.cost = cost;
	this.deadline = deadline;
	this.overrunHandler = overrunHandler;
	this.missHandler = missHandler;
    }

    public RelativeTime getCost() { 
	return cost; 
    }

    public AsyncEventHandler getCostOverrunHandler() { 
	return overrunHandler; 
    }

    public RelativeTime getDeadline() { 
	return deadline; 
    }

    public AsyncEventHandler getDeadlineMissHandler(){
	return missHandler;
    }

    public void setCost(RelativeTime cost) {
	this.cost = cost;
    }

    public void setCostOverrunHandler(AsyncEventHandler overrunHandler){
	this.overrunHandler = overrunHandler;
    }

    public void setDeadline(RelativeTime deadline){  
	this.deadline = deadline;
    }

    public void setDeadlineMissHandler(AsyncEventHandler missHandler){
	this.missHandler = missHandler;
    }

    public boolean setIfFeasible(RelativeTime relativeTime, RelativeTime relativeTime1) { 
	return true; 
    }
}