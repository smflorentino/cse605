package javax.realtime;

public class PeriodicParameters extends ReleaseParameters {

    private HighResolutionTime start;
    private RelativeTime period;

    public PeriodicParameters(RelativeTime period){
	this.period = period;
    }
    
    public PeriodicParameters(HighResolutionTime start, 
			      RelativeTime period) {
        this.start = start;
        this.period = period;
    }

    public PeriodicParameters(HighResolutionTime start, 
			      RelativeTime period, 
			      RelativeTime cost, 
			      RelativeTime deadline, 
			      AsyncEventHandler overrunHandler, 
			      AsyncEventHandler missHandler) {
	super(cost, deadline, overrunHandler, missHandler);
        this.start = start;
        this.period = period;
    }

    public RelativeTime getPeriod(){
	return period; 
    }

    public HighResolutionTime getStart() {
	return start;
    }

    public void setPeriod(RelativeTime period){
	this.period = period;
    }

    public void setStart(HighResolutionTime start){
	this.start = start;
    }

    public boolean setIfFeasible(RelativeTime period,
				 RelativeTime cost,
				 RelativeTime deadline){ 
	return false; 
    }
}